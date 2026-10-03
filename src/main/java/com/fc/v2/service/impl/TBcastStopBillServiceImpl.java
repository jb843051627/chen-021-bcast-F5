package com.fc.v2.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TBcastStopBillMapper;
import com.fc.v2.mapper.auto.TBcastStopSignMapper;
import com.fc.v2.model.auto.TBcastStopBill;
import com.fc.v2.model.auto.TBcastStopSign;
import com.fc.v2.model.custom.BcastStopBillView;
import com.fc.v2.model.custom.BcastStopNodeBlock;
import com.fc.v2.model.custom.BcastStopRoster;
import com.fc.v2.model.custom.BcastStopRosterEntry;
import com.fc.v2.service.ITBcastStopBillService;

/**
 * 停播检修核准单 Service业务层处理（approval-chain 形状：三道关口按次序走）。
 *
 * <p>前年河湾区那张单在台站、市广电局、省局之间来回退了两趟：退回来重走时先前各档
 * 记下的名被一并抹净，台站等于把同一件事点了两遍；三道上的记录谁也归不到谁名下。
 * 今年这一张由系统一路盯到底，所有结论只出在 {@link #driveStopBill} 这一个落名出口：</p>
 * <ul>
 *   <li>走到哪一档：不由屏上递的档号定，由出口顺着这张单的名次，从落名明细上
 *       把已点齐的档数数出来；屏上带出的档号与回话不合，按回话那一份走。</li>
 *   <li>三档算法各算各的：台站一人即够，头一名落地便算过；市里、省局各要两人，
 *       缺一人在窗口里仍挂候点。已记几人不许手填——当前趟在册落名条数与应点人数
 *       两边持平才算数。</li>
 *   <li>往前只挪到紧挨着的下一档：前一档缺名未齐，下一档记上名也不作数；绕开市里
 *       直接由省局落名，单依然卡在市里。</li>
 *   <li>往回只退一格：整张落回紧挨着的上一档，本档这一轮已点下的名当场清零留底
 *       （打上退档标记，老册照翻，不抹掉），早先各档的旧名一笔不动；重走另起一趟，
 *       新旧两拨分开放。</li>
 *   <li>哪一档否了，全单就此了断，往后各档不得再记名。</li>
 *   <li>省局点齐即排定封卷：谁也不能再添名，卷面上的字也动不了；同时落下同算凭据，
 *       排期名册与核准结果出自同一次算。</li>
 * </ul>
 *
 * <p>不继承 ServiceImpl：IService 那一套 save/updateById/removeById 是绕得过去的
 * 第二道口子，收口规矩不许它开张。</p>
 *
 * @author fuce
 * @date 2026-10-03
 */
@Service
public class TBcastStopBillServiceImpl implements ITBcastStopBillService {

    private static final Logger log = LoggerFactory.getLogger(TBcastStopBillServiceImpl.class);

    /** 第一道：台站 */
    private static final int NODE_SITE = 0;
    /** 第二道：市广电局 */
    private static final int NODE_CITY = 1;
    /** 第三道：省局 */
    private static final int NODE_PROV = 2;
    private static final int MAX_NODE = NODE_PROV;
    private static final int NODE_COUNT = 3;
    private static final String[] NODE_NAMES = {"台站", "市广电局", "省局"};

    /** 各档该点几人：台站一人，市里、省局各两人——三档算法各算各的 */
    private static final int[] NEED_COUNT = {1, 2, 2};
    /** 各档算齐说法 */
    private static final String[] RULE_TEXT = {
            "一人即够，头一名落地这一档便算过",
            "两人点齐，缺一人在窗口里仍挂候点",
            "两人点齐，缺一人在窗口里仍挂候点"};
    /** 同档点齐办法：台站一笔即可，市里、省局两笔点齐（只作展示） */
    private static final int[] SIGN_MODE = {0, 1, 1};

    /** 核准情形：在核 */
    private static final int STATUS_RUNNING = 0;
    /** 核准情形：已排定（省局点齐，封卷） */
    private static final int STATUS_FINAL = 1;
    /** 核准情形：已了断（被某一档否了） */
    private static final int STATUS_VETO = 2;

    /** 落名在册（当前趟） */
    private static final int SIGN_ACTIVE = 0;
    /** 退档清零留下的底：退档那档本趟的名当场清零，不抹掉，老册照翻 */
    private static final int SIGN_BACKED = 1;
    /** 否决留底 */
    private static final int SIGN_VETOED = 2;
    /** 单子退到本档、重新起趟时，上一趟的旧名压作旧册：新旧两拨分开放，各留各的底 */
    private static final int SIGN_PRIOR = 3;

    /** 落名出口动作 */
    private static final String ACTION_SIGN = "sign";
    private static final String ACTION_VETO = "veto";
    private static final String ACTION_BACK = "back";

    @javax.annotation.Resource
    private TBcastStopBillMapper bcastStopBillMapper;

    @javax.annotation.Resource
    private TBcastStopSignMapper bcastStopSignMapper;

    /**
     * 按主键回查：老方法签名一概不碰，进什么样出什么样，不塞新参。
     * 只是把档口、应点、已点三档展示栏按落名明细回数到实处——卷面旧值与数出来的
     * 对不上时，以数出来的为准。
     */
    @Override
    public TBcastStopBill selectTBcastStopBillById(Long id) {
        if (id == null) {
            return null;
        }
        TBcastStopBill bill = this.bcastStopBillMapper.selectById(id);
        if (bill == null) {
            return null;
        }
        return heal(bill, signsOf(id));
    }

    /** 台账列查：每张单的落名一次取齐，逐张把展示栏回数到实处，不分两回数 */
    @Override
    public List<TBcastStopBill> selectTBcastStopBillList(QueryWrapper<TBcastStopBill> queryWrapper) {
        QueryWrapper<TBcastStopBill> wrapper = queryWrapper == null ? new QueryWrapper<TBcastStopBill>() : queryWrapper;
        wrapper.eq("del_flag", 0);
        List<TBcastStopBill> rows = this.bcastStopBillMapper.selectList(wrapper);
        if (rows.isEmpty()) {
            return rows;
        }
        List<Object> ids = new ArrayList<Object>();
        for (TBcastStopBill r : rows) {
            ids.add(r.getId());
        }
        List<TBcastStopSign> all = this.bcastStopSignMapper.selectList(new QueryWrapper<TBcastStopSign>()
                .in("bill_id", ids));
        Map<Long, List<TBcastStopSign>> byBill = new HashMap<Long, List<TBcastStopSign>>();
        for (TBcastStopSign s : all) {
            List<TBcastStopSign> list = byBill.get(s.getBillId());
            if (list == null) {
                list = new ArrayList<TBcastStopSign>();
                byBill.put(s.getBillId(), list);
            }
            list.add(s);
        }
        for (TBcastStopBill r : rows) {
            List<TBcastStopSign> signs = byBill.get(r.getId());
            heal(r, signs == null ? Collections.<TBcastStopSign>emptyList() : signs);
        }
        return rows;
    }

    /** 开单：从台站那一档起、在核；代号之外的档号、应点、已点、凭据各栏系统落笔 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TBcastStopBill fileStopBill(String billNo, String remark) {
        String no = billNo == null ? "" : billNo.trim();
        if (no.isEmpty()) {
            return null;
        }
        Integer inflight = this.bcastStopBillMapper.selectCount(new QueryWrapper<TBcastStopBill>()
                .eq("bill_no", no)
                .eq("del_flag", 0)
                .eq("status", STATUS_RUNNING));
        if (inflight != null && inflight.intValue() > 0) {
            log.info("核准单{}还在核，同号的单立不起来", no);
            return null;
        }
        TBcastStopBill bill = new TBcastStopBill();
        bill.setBillNo(no);
        bill.setBizKey(null);
        bill.setNodeNo(Integer.valueOf(NODE_SITE));
        bill.setSignMode(Integer.valueOf(SIGN_MODE[NODE_SITE]));
        bill.setNeedCount(Integer.valueOf(NEED_COUNT[NODE_SITE]));
        bill.setSignCount(Integer.valueOf(0));
        bill.setStatus(Integer.valueOf(STATUS_RUNNING));
        bill.setLastAction("开单：落在台站档，候点");
        bill.setDelFlag(Integer.valueOf(0));
        bill.setRemark(remark);
        this.bcastStopBillMapper.insert(bill);
        return bill;
    }

    /**
     * 唯一落名出口。屏上走到哪一档、下一档收不收，两处结论全出在这一处。
     * 递上来的 intentNodeNo 只当意图：与出口顺着名次数出来的档口不合，按回话这一份走，
     * 越档先落的那一笔不作数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BcastStopBillView driveStopBill(Long id, Integer intentNodeNo, String signer, String comment, String action) {
        if (id == null || action == null) {
            return null;
        }
        TBcastStopBill bill = this.bcastStopBillMapper.selectById(id);
        if (bill == null) {
            return null;
        }
        int status = bill.getStatus() == null ? STATUS_RUNNING : bill.getStatus().intValue();
        // 已排定的封了卷、已了断的止了步：往后什么名都落不进来
        if (status == STATUS_FINAL) {
            log.info("核准单{}省局点齐已排定，添不进人，{}不作数", id, action);
            return null;
        }
        if (status == STATUS_VETO) {
            log.info("核准单{}已被否了断，{}不作数", id, action);
            return null;
        }

        List<TBcastStopSign> signs = signsOf(id);
        int atNode = currentNode(signs);

        if (ACTION_SIGN.equals(action)) {
            return doSign(bill, signs, atNode, intentNodeNo, signer, comment);
        }
        if (ACTION_VETO.equals(action)) {
            // 否了它的只应是正落着单的这一档；递个别的档号来，否不了
            if (intentNodeNo != null && intentNodeNo.intValue() != atNode) {
                log.info("核准单{}此刻落在{}档，{}档否不了", id, NODE_NAMES[atNode],
                        nodeText(intentNodeNo.intValue()));
                return null;
            }
            // 全单了断：到这一步各档已落下的名一律转作否决留底，老册照翻，不再算在册
            for (TBcastStopSign s : signs) {
                if (s.getDelFlag() != null && s.getDelFlag().intValue() == SIGN_ACTIVE) {
                    s.setDelFlag(Integer.valueOf(SIGN_VETOED));
                    this.bcastStopSignMapper.updateById(s);
                }
            }
            bill.setStatus(Integer.valueOf(STATUS_VETO));
            bill.setLastAction(NODE_NAMES[atNode] + "档否决，全单了断，往后各档不得再记名");
            persistDisplay(bill, signsOf(id), atNode);
            this.bcastStopBillMapper.updateById(bill);
            return buildView(bill, signsOf(id));
        }
        if (ACTION_BACK.equals(action)) {
            return doBack(bill, signs, atNode, intentNodeNo, comment);
        }
        log.info("核准单{}：认不得的动作{}", id, action);
        return null;
    }

    /** 整面回查：三块、一块四栏，如今记了几人顺着名次一笔一笔点出来 */
    @Override
    public BcastStopBillView viewBill(Long id) {
        if (id == null) {
            return null;
        }
        TBcastStopBill bill = this.bcastStopBillMapper.selectById(id);
        if (bill == null) {
            return null;
        }
        return buildView(bill, signsOf(id));
    }

    /**
     * 检修排期名册：只对已排定的单出。名册与核准结果出自同一次算——同样是这张单
     * 当前趟在册落名点上来的，凭据用的是排定那一刻落下的同一个；这里没有一处手填。
     * 没排定、或哪一块笔数对不上应点人数的，不出名册。
     */
    @Override
    public BcastStopRoster rosterOf(Long id) {
        if (id == null) {
            return null;
        }
        TBcastStopBill bill = this.bcastStopBillMapper.selectById(id);
        if (bill == null || bill.getStatus() == null || bill.getStatus().intValue() != STATUS_FINAL) {
            return null;
        }
        List<TBcastStopSign> signs = signsOf(id);
        int[] counts = new int[NODE_COUNT];
        List<BcastStopRosterEntry> entries = new ArrayList<BcastStopRosterEntry>();
        // 自省局往回收数到台站：档号从大到小；同档内按落名名次
        for (int node = MAX_NODE; node >= NODE_SITE; node--) {
            List<TBcastStopSign> inNode = activeInNode(signs, node);
            counts[node] = inNode.size();
            for (TBcastStopSign s : inNode) {
                entries.add(new BcastStopRosterEntry(bill.getBillNo(), bill.getBizKey(),
                        node, NODE_NAMES[node], s.getRoundNo().intValue(), s.getSigner(), s.getSignTime()));
            }
        }
        boolean balanced = true;
        int total = 0;
        for (int node = NODE_SITE; node <= MAX_NODE; node++) {
            total += counts[node];
            if (counts[node] != NEED_COUNT[node]) {
                balanced = false;
            }
        }
        if (!balanced) {
            // 排了定却点不齐，账本身对不上，不许出一份糊弄的名册
            log.info("核准单{}三块笔数对不上应点人数，名册不出", id);
            return null;
        }
        BcastStopRoster roster = new BcastStopRoster();
        roster.setBillNo(bill.getBillNo());
        roster.setBizKey(bill.getBizKey());
        roster.setEntries(entries);
        roster.setNodeCounts(counts);
        roster.setTotalCount(total);
        roster.setBalanced(true);
        return roster;
    }

    // ================= 落名出口的三样回话 =================

    /** 落名：只许落在正收单的这一档，条数攒齐才进紧挨的下一档 */
    private BcastStopBillView doSign(TBcastStopBill bill, List<TBcastStopSign> signs, int atNode,
                                     Integer intentNodeNo, String signer, String comment) {
        Long id = bill.getId();
        // 屏上递的档号只当意图：与数出来的不合，按回话这一份走——这一笔不作数
        if (intentNodeNo != null && intentNodeNo.intValue() != atNode) {
            log.info("核准单{}此刻卡在{}档，越档落到{}档的这一笔不作数",
                    id, NODE_NAMES[atNode], nodeText(intentNodeNo.intValue()));
            return null;
        }
        String name = signer == null ? "" : signer.trim();
        if (name.isEmpty()) {
            log.info("核准单{}落名没给人，不作数", id);
            return null;
        }
        List<TBcastStopSign> inNode = activeInNode(signs, atNode);
        // 一个人在这一档这一趟只落一笔：同一名字再点一遍不添条，也不能拿它凑两人
        for (TBcastStopSign s : inNode) {
            if (name.equals(s.getSigner())) {
                log.info("核准单{}{}档本趟{}已落过名，再点不添条", id, NODE_NAMES[atNode], name);
                return null;
            }
        }
        if (inNode.size() >= NEED_COUNT[atNode]) {
            // 出口数出来这一档已齐，窗口里不该再收；真走到这，以不添条为准
            log.info("核准单{}{}档已点齐{}人，多落的不作数", id, NODE_NAMES[atNode], NEED_COUNT[atNode]);
            return null;
        }

        TBcastStopSign row = new TBcastStopSign();
        row.setBillId(id);
        row.setBillNo(bill.getBillNo());
        row.setNodeNo(Integer.valueOf(atNode));
        row.setRoundNo(Integer.valueOf(currentRound(signs, atNode)));
        row.setSigner(name);
        row.setSignNote(comment);
        row.setSignTime(new Date());
        row.setDelFlag(Integer.valueOf(SIGN_ACTIVE));
        this.bcastStopSignMapper.insert(row);
        signs.add(row);

        int nowHave = inNode.size() + 1;
        if (nowHave >= NEED_COUNT[atNode]) {
            // 本档条数与应点人数两边持平：算齐
            if (atNode == NODE_PROV) {
                // 省局点齐：这张单排定，落下同算凭据，当场封卷
                bill.setStatus(Integer.valueOf(STATUS_FINAL));
                bill.setBizKey(genBizKey());
                bill.setNodeNo(Integer.valueOf(MAX_NODE));
                bill.setLastAction("省局点齐，核准排定，封卷；名册凭据已落");
            } else {
                int next = atNode + 1;
                bill.setNodeNo(Integer.valueOf(next));
                bill.setLastAction(NODE_NAMES[atNode] + "档点齐，挪到" + NODE_NAMES[next] + "档候点");
            }
        } else {
            bill.setNodeNo(Integer.valueOf(atNode));
            bill.setLastAction(NODE_NAMES[atNode] + "档落名1笔，还差" + (NEED_COUNT[atNode] - nowHave) + "人，挂候点");
        }
        persistDisplay(bill, signs, bill.getStatus().intValue() == STATUS_FINAL ? MAX_NODE : bill.getNodeNo().intValue());
        this.bcastStopBillMapper.updateById(bill);
        return buildView(bill, signsOf(id));
    }

    /** 退回：只退一格，本档这一轮的名当场清零留底，早先各档旧名一笔不动 */
    private BcastStopBillView doBack(TBcastStopBill bill, List<TBcastStopSign> signs, int atNode,
                                     Integer intentNodeNo, String comment) {
        Long id = bill.getId();
        if (intentNodeNo != null && intentNodeNo.intValue() != atNode) {
            log.info("核准单{}此刻落在{}档，{}档退不了它", id, NODE_NAMES[atNode], nodeText(intentNodeNo.intValue()));
            return null;
        }
        if (atNode <= NODE_SITE) {
            log.info("核准单{}已在头一道台站档，往后没处退", id);
            return null;
        }
        int round = currentRound(signs, atNode);
        List<TBcastStopSign> inNode = activeInNode(signs, atNode);
        int cleared = inNode.size();
        // 本档（退档那档）当前趟的在册名当场清零：不抹行，打上退档标记留底
        for (TBcastStopSign s : inNode) {
            s.setDelFlag(Integer.valueOf(SIGN_BACKED));
            this.bcastStopSignMapper.updateById(s);
        }

        int backTo = atNode - 1;
        // 整张落回紧挨着的上一档：那一档上一拨已点的名压作旧册（flag3），重新起趟。
        // 早先更前各档的旧名一笔不动；新旧两拨分开放，哪日补的各留各的底
        List<TBcastStopSign> priorOfBackTo = activeInNode(signs, backTo);
        for (TBcastStopSign s : priorOfBackTo) {
            s.setDelFlag(Integer.valueOf(SIGN_PRIOR));
            this.bcastStopSignMapper.updateById(s);
        }

        bill.setNodeNo(Integer.valueOf(backTo));
        bill.setStatus(Integer.valueOf(STATUS_RUNNING));
        bill.setLastAction(NODE_NAMES[atNode] + "档退回" + NODE_NAMES[backTo] + "档；" + NODE_NAMES[atNode]
                + "档第" + round + "趟" + cleared + "笔清零留底，" + NODE_NAMES[backTo]
                + "档上一拨" + priorOfBackTo.size() + "笔压旧册另起趟，更早各档不动");
        if (comment != null) {
            bill.setRemark(comment);
        }
        List<TBcastStopSign> fresh = signsOf(id);
        persistDisplay(bill, fresh, backTo);
        this.bcastStopBillMapper.updateById(bill);
        return buildView(bill, fresh);
    }

    // ================= 顺着名次数档口 =================

    /** 这张单此刻落在第几块：头一块没点齐的档便是；三块都齐，停在省局（排定） */
    private int currentNode(List<TBcastStopSign> signs) {
        for (int node = NODE_SITE; node <= MAX_NODE; node++) {
            if (activeInNode(signs, node).size() < NEED_COUNT[node]) {
                return node;
            }
        }
        return MAX_NODE;
    }

    /** 该档当前趟在册落名，按落名名次（时刻、主键）排 */
    private List<TBcastStopSign> activeInNode(List<TBcastStopSign> signs, int node) {
        List<TBcastStopSign> list = new ArrayList<TBcastStopSign>();
        for (TBcastStopSign s : signs) {
            if (s.getDelFlag() != null && s.getDelFlag().intValue() == SIGN_ACTIVE
                    && s.getNodeNo() != null && s.getNodeNo().intValue() == node) {
                list.add(s);
            }
        }
        Collections.sort(list, new Comparator<TBcastStopSign>() {
            @Override
            public int compare(TBcastStopSign a, TBcastStopSign b) {
                int t = a.getSignTime().compareTo(b.getSignTime());
                if (t != 0) {
                    return t;
                }
                return a.getId().compareTo(b.getId());
            }
        });
        return list;
    }

    /**
     * 这一档走到第几趟：当前趟有在册名的用在册名的趟号；没有在册名的，
     * 看各趟留底（含退档的底）最大趟号再加一趟；从没记过为头一趟。
     */
    private int currentRound(List<TBcastStopSign> signs, int node) {
        int maxRound = 0;
        boolean hasActive = false;
        for (TBcastStopSign s : signs) {
            if (s.getNodeNo() == null || s.getNodeNo().intValue() != node || s.getRoundNo() == null) {
                continue;
            }
            if (s.getDelFlag() != null && s.getDelFlag().intValue() == SIGN_ACTIVE) {
                hasActive = true;
                maxRound = Math.max(maxRound, s.getRoundNo().intValue());
            } else {
                maxRound = Math.max(maxRound, s.getRoundNo().intValue());
            }
        }
        return hasActive ? maxRound : maxRound + 1;
    }

    /** 一张单的落名全册（含退档留底的，老册照翻） */
    private List<TBcastStopSign> signsOf(Long billId) {
        return this.bcastStopSignMapper.selectList(new QueryWrapper<TBcastStopSign>()
                .eq("bill_id", billId)
                .orderByAsc("node_no", "round_no", "sign_time", "id"));
    }

    /** 主单三档展示栏回数到实处：在核的数当前趟在册条数；排定停省局、了断信库存档口 */
    private TBcastStopBill heal(TBcastStopBill bill, List<TBcastStopSign> signs) {
        int st = bill.getStatus() == null ? STATUS_RUNNING : bill.getStatus().intValue();
        int at;
        if (st == STATUS_FINAL) {
            at = MAX_NODE;
        } else if (st == STATUS_VETO) {
            at = bill.getNodeNo() == null ? NODE_SITE : bill.getNodeNo().intValue();
        } else {
            at = currentNode(signs);
        }
        bill.setNodeNo(Integer.valueOf(at));
        bill.setSignMode(Integer.valueOf(SIGN_MODE[at]));
        bill.setNeedCount(Integer.valueOf(NEED_COUNT[at]));
        bill.setSignCount(Integer.valueOf(activeInNode(signs, at).size()));
        return bill;
    }

    /** 落名出口落笔后，把主单上那三档展示栏按数出来的刷一遍 */
    private void persistDisplay(TBcastStopBill bill, List<TBcastStopSign> signs, int atNode) {
        bill.setSignMode(Integer.valueOf(SIGN_MODE[atNode]));
        bill.setNeedCount(Integer.valueOf(NEED_COUNT[atNode]));
        bill.setSignCount(Integer.valueOf(activeInNode(signs, atNode).size()));
    }

    /** 整面：三块，一块四栏；已记几人一笔一笔点出来给他看，旧册各趟另列 */
    private BcastStopBillView buildView(TBcastStopBill bill, List<TBcastStopSign> signs) {
        int st = bill.getStatus() == null ? STATUS_RUNNING : bill.getStatus().intValue();
        boolean finalized = st == STATUS_FINAL;
        boolean vetoed = st == STATUS_VETO;
        int at;
        if (finalized) {
            at = MAX_NODE;
        } else if (vetoed) {
            at = bill.getNodeNo() == null ? NODE_SITE : bill.getNodeNo().intValue();
        } else {
            at = currentNode(signs);
        }
        heal(bill, signs);

        BcastStopBillView view = new BcastStopBillView();
        view.setBill(bill);
        view.setDerivedNodeNo(at);
        view.setFinalized(finalized);
        view.setVetoed(vetoed);
        int totalCurrent = 0;
        List<BcastStopNodeBlock> blocks = new ArrayList<BcastStopNodeBlock>();
        for (int node = NODE_SITE; node <= MAX_NODE; node++) {
            List<TBcastStopSign> inNode = activeInNode(signs, node);
            List<TBcastStopSign> prior = priorInNode(signs, node);
            BcastStopNodeBlock block = new BcastStopNodeBlock();
            block.setNodeNo(node);
            block.setNodeName(NODE_NAMES[node]);
            block.setNeedCount(NEED_COUNT[node]);
            block.setRuleText(RULE_TEXT[node]);
            block.setSignedCount(inNode.size());
            block.setBillNodeNo(at);
            block.setRoundNo(currentRound(signs, node));
            block.setQuotaFilled(inNode.size() == NEED_COUNT[node]);
            block.setAtBillNode(node == at && !finalized && !vetoed);
            block.setCurrentSigns(inNode);
            block.setPriorSigns(prior);
            blocks.add(block);
            totalCurrent += inNode.size();
        }
        view.setBlocks(blocks);
        view.setCurrentNameCount(totalCurrent);
        return view;
    }

    /** 该档旧册各趟留下的底（退档清零的、再走压旧的、否决留底的），按趟与名次排 */
    private List<TBcastStopSign> priorInNode(List<TBcastStopSign> signs, int node) {
        List<TBcastStopSign> list = new ArrayList<TBcastStopSign>();
        for (TBcastStopSign s : signs) {
            if (s.getDelFlag() != null && s.getDelFlag().intValue() != SIGN_ACTIVE
                    && s.getNodeNo() != null && s.getNodeNo().intValue() == node) {
                list.add(s);
            }
        }
        Collections.sort(list, new Comparator<TBcastStopSign>() {
            @Override
            public int compare(TBcastStopSign a, TBcastStopSign b) {
                int r = a.getRoundNo().compareTo(b.getRoundNo());
                if (r != 0) {
                    return r;
                }
                int t = a.getSignTime().compareTo(b.getSignTime());
                return t != 0 ? t : a.getId().compareTo(b.getId());
            }
        });
        return list;
    }

    private String nodeText(int node) {
        return node >= 0 && node < NODE_COUNT ? NODE_NAMES[node] : ("第" + node + "档");
    }

    /** 同算凭据：省局点齐那一刻落一次，核准结果与排期名册共用 */
    private String genBizKey() {
        return "JX" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
