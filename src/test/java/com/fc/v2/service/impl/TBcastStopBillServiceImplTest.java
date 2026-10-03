package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TBcastStopBillMapper;
import com.fc.v2.mapper.auto.TBcastStopSignMapper;
import com.fc.v2.model.auto.TBcastStopBill;
import com.fc.v2.model.auto.TBcastStopSign;
import com.fc.v2.model.custom.BcastStopBillView;
import com.fc.v2.model.custom.BcastStopNodeBlock;
import com.fc.v2.model.custom.BcastStopRoster;
import com.fc.v2.model.custom.BcastStopRosterEntry;

/**
 * 停播检修核准单：唯一落名出口的单测。库用两本账顶替（主单账、落名账），
 * 落名、清零、封卷都当着面在账上过。三道关口台站1人、市里2人、省局2人。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TBcastStopBillServiceImplTest {

    @Mock
    private TBcastStopBillMapper billMapper;

    @Mock
    private TBcastStopSignMapper signMapper;

    @org.mockito.InjectMocks
    private TBcastStopBillServiceImpl service;

    private final Map<Long, TBcastStopBill> bills = new LinkedHashMap<Long, TBcastStopBill>();
    private final Map<Long, TBcastStopSign> signs = new LinkedHashMap<Long, TBcastStopSign>();
    private long billSeq = 1000L;
    private long signSeq = 5000L;

    // 落名标记
    private static final int ACTIVE = 0;
    private static final int BACKED = 1;
    private static final int VETOED = 2;
    private static final int PRIOR = 3;

    @BeforeEach
    void mockDb() {
        bills.clear();
        signs.clear();
        billSeq = 1000L;
        signSeq = 5000L;

        when(billMapper.selectById(any())).thenAnswer(inv -> bills.get((Long) inv.getArgument(0)));
        when(billMapper.selectList(any())).thenAnswer(inv -> {
            QueryWrapper<?> w = inv.getArgument(0);
            w.getSqlSegment();
            String billNo = null;
            for (Object v : w.getParamNameValuePairs().values()) {
                if (v instanceof String) {
                    billNo = (String) v;
                }
            }
            List<TBcastStopBill> list = new ArrayList<TBcastStopBill>();
            for (TBcastStopBill b : bills.values()) {
                if (b.getDelFlag() != null && b.getDelFlag().intValue() == 0
                        && (billNo == null || (b.getBillNo() != null && b.getBillNo().contains(billNo)))) {
                    list.add(b);
                }
            }
            return list;
        });
        when(billMapper.selectCount(any())).thenAnswer(inv -> {
            QueryWrapper<?> w = inv.getArgument(0);
            w.getSqlSegment();
            String billNo = null;
            for (Object v : w.getParamNameValuePairs().values()) {
                if (v instanceof String) {
                    billNo = (String) v;
                }
            }
            int n = 0;
            for (TBcastStopBill b : bills.values()) {
                if (b.getDelFlag() != null && b.getDelFlag().intValue() == 0
                        && b.getStatus() != null && b.getStatus().intValue() == 0
                        && (billNo == null || billNo.equals(b.getBillNo()))) {
                    n++;
                }
            }
            return Integer.valueOf(n);
        });
        when(billMapper.insert(any())).thenAnswer(inv -> {
            TBcastStopBill b = inv.getArgument(0);
            b.setId(Long.valueOf(++billSeq));
            bills.put(b.getId(), b);
            return Integer.valueOf(1);
        });
        when(billMapper.updateById(any())).thenAnswer(inv -> {
            TBcastStopBill b = inv.getArgument(0);
            bills.put(b.getId(), b);
            return Integer.valueOf(1);
        });

        when(signMapper.selectList(any())).thenAnswer(inv -> {
            QueryWrapper<?> w = inv.getArgument(0);
            w.getSqlSegment();
            Long billId = null;
            for (Object v : w.getParamNameValuePairs().values()) {
                if (v instanceof Long) {
                    billId = (Long) v;
                }
            }
            List<TBcastStopSign> list = new ArrayList<TBcastStopSign>();
            for (TBcastStopSign s : signs.values()) {
                if (billId == null || billId.equals(s.getBillId())) {
                    list.add(s);
                }
            }
            list.sort((a, b) -> {
                int r = a.getNodeNo().compareTo(b.getNodeNo());
                if (r != 0) {
                    return r;
                }
                int r2 = a.getRoundNo().compareTo(b.getRoundNo());
                if (r2 != 0) {
                    return r2;
                }
                int t = a.getSignTime().compareTo(b.getSignTime());
                return t != 0 ? t : a.getId().compareTo(b.getId());
            });
            return list;
        });
        when(signMapper.insert(any())).thenAnswer(inv -> {
            TBcastStopSign s = inv.getArgument(0);
            s.setId(Long.valueOf(++signSeq));
            signs.put(s.getId(), s);
            return Integer.valueOf(1);
        });
        when(signMapper.updateById(any())).thenAnswer(inv -> {
            TBcastStopSign s = inv.getArgument(0);
            signs.put(s.getId(), s);
            return Integer.valueOf(1);
        });
    }

    // ================= 家什 =================

    private TBcastStopBill 开单(String no) {
        return service.fileStopBill(no, null);
    }

    private BcastStopBillView 落名(Long id, Integer intent, String signer) {
        return service.driveStopBill(id, intent, signer, null, "sign");
    }

    private BcastStopBillView 否决(Long id, Integer intent) {
        return service.driveStopBill(id, intent, null, "不批", "veto");
    }

    private BcastStopBillView 退回(Long id, Integer intent) {
        return service.driveStopBill(id, intent, null, "材料重补", "back");
    }

    private BcastStopNodeBlock 块(BcastStopBillView v, int node) {
        return v.getBlocks().get(node);
    }

    /** 点齐台站：1人 */
    private void 台站点齐(Long id, String name) {
        BcastStopBillView v = 落名(id, 0, name);
        assertNotNull(v);
    }

    /** 点齐市里：2人 */
    private void 市里点齐(Long id, String n1, String n2) {
        assertNotNull(落名(id, 1, n1));
        assertNotNull(落名(id, 1, n2));
    }

    /** 点齐省局：2人；返回排定后的整面 */
    private BcastStopBillView 省局点齐(Long id, String n1, String n2) {
        assertNotNull(落名(id, 2, n1));
        return 落名(id, 2, n2);
    }

    /** 一路走通：台站1→市里2→省局2，落5个名 */
    private BcastStopBillView 走通(Long id) {
        台站点齐(id, "台长老方");
        市里点齐(id, "市一科老钱", "市二科老孙");
        return 省局点齐(id, "省一处老李", "省二处老周");
    }

    private List<TBcastStopSign> 该档在册(Long billId, int node) {
        List<TBcastStopSign> list = new ArrayList<TBcastStopSign>();
        for (TBcastStopSign s : signs.values()) {
            if (s.getBillId().equals(billId) && s.getNodeNo().intValue() == node
                    && s.getDelFlag().intValue() == ACTIVE) {
                list.add(s);
            }
        }
        return list;
    }

    // ================= 开单 =================

    @Test
    void 开单_系统落笔_从台站档起() {
        TBcastStopBill b = 开单(" JX-01 ");
        assertEquals("JX-01", b.getBillNo());
        assertEquals(Integer.valueOf(0), b.getNodeNo());
        assertEquals(Integer.valueOf(1), b.getNeedCount());
        assertEquals(Integer.valueOf(0), b.getSignCount());
        assertEquals(Integer.valueOf(0), b.getStatus());
        assertNull(b.getBizKey());
        // 空代号立不起来；同号还有一张在核，也立不起来
        assertNull(service.fileStopBill("  ", null));
        assertNull(service.fileStopBill("JX-01", null));
    }

    // ================= 三档算法：1 / 2 / 2 =================

    @Test
    void 台站一人即够_头一名落地便算过() {
        TBcastStopBill b = 开单("JX-10");
        BcastStopBillView v = 落名(b.getId(), 0, "台长老方");
        assertEquals(1, v.getDerivedNodeNo(), "台站头一名落地，单挪到市里");
        assertTrue(块(v, 0).isQuotaFilled());
        assertTrue(块(v, 1).isAtBillNode());
        assertEquals(1, 块(v, 0).getSignedCount(), "如今记了几人是点条数点出来的");
    }

    @Test
    void 市里两人_缺一人仍挂候点() {
        TBcastStopBill b = 开单("JX-11");
        台站点齐(b.getId(), "台长老方");
        BcastStopBillView one = 落名(b.getId(), 1, "市一科老钱");
        assertEquals(1, one.getDerivedNodeNo(), "市里才1人，单还卡在市里");
        assertFalse(块(one, 1).isQuotaFilled());
        assertEquals(1, 块(one, 1).getSignedCount());
        BcastStopBillView two = 落名(b.getId(), 1, "市二科老孙");
        assertEquals(2, two.getDerivedNodeNo(), "市里两人点齐，挪到省局");
        assertTrue(块(two, 1).isQuotaFilled());
    }

    @Test
    void 省局两人点齐_才算排定() {
        TBcastStopBill b = 开单("JX-12");
        台站点齐(b.getId(), "台长老方");
        市里点齐(b.getId(), "市一科老钱", "市二科老孙");
        BcastStopBillView one = 落名(b.getId(), 2, "省一处老李");
        assertEquals(2, one.getDerivedNodeNo(), "省局才1人，仍挂候点");
        assertFalse(one.isFinalized());
        BcastStopBillView done = 落名(b.getId(), 2, "省二处老周");
        assertTrue(done.isFinalized());
        assertEquals(2, done.getDerivedNodeNo());
        assertNotNull(done.getBill().getBizKey(), "排定那一刻落下同算凭据");
    }

    @Test
    void 同一个人本趟再点_不添条也凑不齐两人() {
        TBcastStopBill b = 开单("JX-13");
        台站点齐(b.getId(), "台长老方");
        assertNotNull(落名(b.getId(), 1, "市一科老钱"));
        assertNull(落名(b.getId(), 1, "市一科老钱"), "同一名字再点一遍不添条");
        assertEquals(1, 该档在册(b.getId(), 1).size());
        assertEquals(1, service.viewBill(b.getId()).getBlocks().get(1).getSignedCount());
    }

    // ================= 越档不作数 =================

    @Test
    void 绕开市里直接省局落名_单依然卡在市里() {
        TBcastStopBill b = 开单("JX-20");
        台站点齐(b.getId(), "台长老方");
        assertNull(落名(b.getId(), 2, "省一处老李"), "省局先落的这一笔不作数");
        assertNull(落名(b.getId(), 2, "省二处老周"));
        BcastStopBillView v = service.viewBill(b.getId());
        assertEquals(1, v.getDerivedNodeNo(), "单依然卡在市里那道");
        assertTrue(该档在册(b.getId(), 2).isEmpty(), "越档那两笔一条也不许落");
        // 市里两人补齐，才收得到省局
        市里点齐(b.getId(), "市一科老钱", "市二科老孙");
        assertNotNull(落名(b.getId(), 2, "省一处老李"));
    }

    @Test
    void 台站没点齐_市里先落也不作数() {
        TBcastStopBill b = 开单("JX-21");
        assertNull(落名(b.getId(), 1, "市一科老钱"), "前一档缺名未齐，下一档记名不作数");
        assertEquals(0, service.viewBill(b.getId()).getDerivedNodeNo());
        assertTrue(该档在册(b.getId(), 1).isEmpty());
    }

    // ================= 退一格：清本档、旧名分开放 =================

    @Test
    void 市里退回台站_市里本趟清零_台站旧名压旧册另起趟() {
        TBcastStopBill b = 开单("JX-30");
        台站点齐(b.getId(), "台长老方");
        assertNotNull(落名(b.getId(), 1, "市一科老钱"), "市里先落1人，还差1人挂候点");
        // 市里把单退回台站：只退一格
        BcastStopBillView back = 退回(b.getId(), 1);
        assertEquals(0, back.getDerivedNodeNo(), "整张落回紧挨着的上一档——台站");
        // 本档（市里）这一轮已经点下的名当场清零：行还在，打上退档底，不计在册
        assertEquals(0, 该档在册(b.getId(), 1).size());
        TBcastStopSign cityOld = null;
        for (TBcastStopSign s : signs.values()) {
            if (s.getNodeNo().intValue() == 1) {
                cityOld = s;
            }
        }
        assertNotNull(cityOld);
        assertEquals(BACKED, cityOld.getDelFlag().intValue(), "清零不是抹掉，留底");
        // 台站那一档上一拨的名压作旧册，重新起趟；新旧两拨分开放
        assertEquals(0, 该档在册(b.getId(), 0).size(), "台站重走，当前趟空着候点");
        BcastStopBillView v0 = service.viewBill(b.getId());
        assertEquals(1, 块(v0, 0).getPriorSigns().size());
        assertEquals(1, 块(v0, 0).getPriorSigns().get(0).getRoundNo().intValue());
        // 台站重新点（第二拨人），名次另起一趟
        BcastStopBillView again = 落名(b.getId(), 0, "台长老方");
        assertEquals(1, again.getDerivedNodeNo());
        assertEquals(2, 块(again, 0).getRoundNo(), "重走是第2趟");
        assertEquals(1, 块(again, 0).getSignedCount());
        assertEquals(1, 块(again, 0).getPriorSigns().size(), "头一趟那笔旧名一笔没动，压在旧册");
    }

    @Test
    void 省局退回市里_省局已落的清零_台站更早的旧名一笔不动() {
        TBcastStopBill b = 开单("JX-31");
        台站点齐(b.getId(), "台长老方");
        市里点齐(b.getId(), "市一科老钱", "市二科老孙");
        assertNotNull(落名(b.getId(), 2, "省一处老李"), "省局先落1人");
        BcastStopBillView back = 退回(b.getId(), 2);
        assertEquals(1, back.getDerivedNodeNo(), "退回市里");
        // 省局本档这一轮那1笔当场清零留底
        assertEquals(0, 该档在册(b.getId(), 2).size());
        // 市里上一拨两人压作旧册另起趟，台站那1笔更早，一笔不动仍在册
        assertEquals(1, 该档在册(b.getId(), 0).size(), "更早各档的旧名一笔不动");
        assertEquals(0, 该档在册(b.getId(), 1).size(), "被退回的市里重新起趟");
        // 重走：市里第2拨两人、省局两笔，再次排定
        市里点齐(b.getId(), "市一科小赵", "市二科小钱");
        BcastStopBillView done = 省局点齐(b.getId(), "省一处老李", "省二处老周");
        assertTrue(done.isFinalized());
        BcastStopRoster roster = service.rosterOf(b.getId());
        assertEquals(5, roster.getTotalCount(), "名册只点当前趟：台站1+市里2+省局2");
        assertEquals("台长老方", roster.getEntries().get(4).getSigner(), "台站头一趟那笔沿用");
    }

    @Test
    void 退到头_台站档不再往后收() {
        TBcastStopBill b = 开单("JX-32");
        assertNull(退回(b.getId(), 0));
        assertEquals(0, service.viewBill(b.getId()).getDerivedNodeNo());
    }

    // ================= 否决了断 =================

    @Test
    void 哪一档否了_全单了断_往后不得再记名() {
        TBcastStopBill b = 开单("JX-40");
        台站点齐(b.getId(), "台长老方");
        assertNotNull(落名(b.getId(), 1, "市一科老钱"));
        BcastStopBillView veto = 否决(b.getId(), 1);
        assertTrue(veto.isVetoed());
        assertEquals(Integer.valueOf(2), veto.getBill().getStatus());
        // 了断之后落名、退回、再否都不作数
        assertNull(落名(b.getId(), 1, "市二科老孙"));
        assertNull(退回(b.getId(), 1));
        assertNull(否决(b.getId(), 1));
        // 已落下的名转作否决留底，老册照翻
        for (TBcastStopSign s : signs.values()) {
            assertEquals(VETOED, s.getDelFlag().intValue());
        }
    }

    @Test
    void 别的档号递来否不了() {
        TBcastStopBill b = 开单("JX-41");
        台站点齐(b.getId(), "台长老方");
        assertNull(否决(b.getId(), 2), "单在市里，省局否不了");
        assertEquals(Integer.valueOf(0), bills.get(b.getId()).getStatus());
    }

    // ================= 排定封卷 =================

    @Test
    void 省局点齐排定后_添不进人也退不动否不动() {
        TBcastStopBill b = 开单("JX-50");
        BcastStopBillView done = 走通(b.getId());
        assertTrue(done.isFinalized());
        int names = signs.size();
        assertNull(落名(b.getId(), 2, "省里又来一人"), "排定后添不进人");
        assertNull(落名(b.getId(), 0, "台站补一人"));
        assertNull(退回(b.getId(), 2));
        assertNull(否决(b.getId(), 2));
        assertEquals(names, signs.size(), "一条名都没多落");
    }

    // ================= 排期名册：同一次算、一笔不差 =================

    @Test
    void 名册与核准同一次算_自省局往回收数到台站() {
        TBcastStopBill b = 开单("JX-60");
        BcastStopBillView done = 走通(b.getId());
        String key = done.getBill().getBizKey();
        BcastStopRoster roster = service.rosterOf(b.getId());
        assertNotNull(roster);
        assertTrue(roster.isBalanced());
        // 笔数与当日窗口那份排期名单一笔不差：1+2+2=5
        assertEquals(5, roster.getTotalCount());
        assertArrayEqualsL(new int[]{1, 2, 2}, roster.getNodeCounts(), "任取一块单看也经得起复核");
        // 次序：自省局往回收数到台站
        List<BcastStopRosterEntry> es = roster.getEntries();
        assertEquals(2, es.get(0).getNodeNo());
        assertEquals(2, es.get(1).getNodeNo());
        assertEquals(1, es.get(2).getNodeNo());
        assertEquals(1, es.get(3).getNodeNo());
        assertEquals(0, es.get(4).getNodeNo());
        assertEquals("台长老方", es.get(4).getSigner());
        // 两边出自同一次算：同一个凭据，逐笔也带着，不许一处取数一处手填
        assertEquals(key, roster.getBizKey());
        for (BcastStopRosterEntry e : es) {
            assertEquals(key, e.getBizKey());
            assertEquals(b.getBillNo(), e.getBillNo());
        }
    }

    @Test
    void 没排定的单_名册不出() {
        TBcastStopBill b = 开单("JX-61");
        台站点齐(b.getId(), "台长老方");
        assertNull(service.rosterOf(b.getId()));
    }

    // ================= 屏上的档号与数只作展示 =================

    @Test
    void 卷面档号已记人数与出口数出来的不合_按回话走() {
        TBcastStopBill b = 开单("JX-70");
        台站点齐(b.getId(), "台长老方");
        // 屏上/卷面被人改花：档号写成省局、已记写成9
        b.setNodeNo(Integer.valueOf(2));
        b.setSignCount(Integer.valueOf(9));
        BcastStopBillView v = service.viewBill(b.getId());
        assertEquals(1, v.getDerivedNodeNo(), "出口顺着名次数：台站已齐、市里没动，单在市里");
        for (BcastStopNodeBlock blk : v.getBlocks()) {
            assertEquals(1, blk.getBillNodeNo(), "三块回显的都是出口的档号");
        }
        // 按主键回查老签名没改：照样把档口、已点数回数到实处
        TBcastStopBill reread = service.selectTBcastStopBillById(b.getId());
        assertEquals(Integer.valueOf(1), reread.getNodeNo());
        assertEquals(Integer.valueOf(0), reread.getSignCount(), "市里当前趟0人，不信卷面的9");
        // 屏上带着省局档号来落名，按回话这一份：越档不作数
        assertNull(落名(b.getId(), 2, "省一处老李"));
        assertNotNull(落名(b.getId(), 1, "市一科老钱"));
    }

    // ================= 来回两趟：三拨记录各归各名 =================

    @Test
    void 来回退两趟_新旧分开放_台站不重复点同一件事() {
        TBcastStopBill b = 开单("JX-80");
        // 头一趟：台站1、市里2，省局落1人时被退回市里
        台站点齐(b.getId(), "台长老方");
        市里点齐(b.getId(), "市甲", "市乙");
        assertNotNull(落名(b.getId(), 2, "省甲"));
        assertNotNull(退回(b.getId(), 2));
        // 第二趟：市里换人点齐，省局又落1人，再被退回市里
        市里点齐(b.getId(), "市丙", "市丁");
        assertNotNull(落名(b.getId(), 2, "省乙"));
        assertNotNull(退回(b.getId(), 2));
        // 第三趟：市里再换一拨，省局两人点齐，排定
        市里点齐(b.getId(), "市戊", "市己");
        BcastStopBillView done = 省局点齐(b.getId(), "省丙", "省丁");
        assertTrue(done.isFinalized());

        // 台站始终只点了头一趟那1笔：没把同一件事点两遍
        List<TBcastStopSign> siteAll = new ArrayList<TBcastStopSign>();
        int siteRounds = 0;
        for (TBcastStopSign s : signs.values()) {
            if (s.getNodeNo().intValue() == 0) {
                siteAll.add(s);
                assertEquals(ACTIVE, s.getDelFlag().intValue());
                siteRounds = Math.max(siteRounds, s.getRoundNo().intValue());
            }
        }
        assertEquals(1, siteAll.size());
        assertEquals(1, siteRounds, "退的是市/省，台站不另起趟");

        // 市里留着三拨：头两拨压旧册（各2笔），第3拨2笔在册；省局头两笔清零留底、末两笔在册
        int cityPrior = 0, cityActive = 0;
        for (TBcastStopSign s : signs.values()) {
            if (s.getNodeNo().intValue() == 1 && s.getDelFlag().intValue() == PRIOR) {
                cityPrior++;
            }
            if (s.getNodeNo().intValue() == 1 && s.getDelFlag().intValue() == ACTIVE) {
                cityActive++;
            }
        }
        assertEquals(4, cityPrior, "新旧两拨（其实两拨旧的）分开放");
        assertEquals(2, cityActive);

        // 名册只点当前趟这5笔，哪日补的各留各的底
        BcastStopRoster roster = service.rosterOf(b.getId());
        assertEquals(5, roster.getTotalCount());
        assertArrayEqualsL(new int[]{1, 2, 2}, roster.getNodeCounts(), "退两趟后各块仍经得起复核");
        List<String> cityNow = new ArrayList<String>();
        for (BcastStopRosterEntry e : roster.getEntries()) {
            if (e.getNodeNo() == 1) {
                cityNow.add(e.getSigner());
            }
        }
        assertTrue(cityNow.contains("市戊"));
        assertTrue(cityNow.contains("市己"));
        // 各块单看：当前趟、旧册趟次都点得清
        BcastStopBillView v = service.viewBill(b.getId());
        assertEquals(4, 块(v, 1).getPriorSigns().size());
        assertEquals(3, 块(v, 1).getRoundNo());
        assertFalse(blockHas(块(v, 1), "市甲"), "旧册的人不混进当前趟");
        assertTrue(blockHas(块(v, 1), "市戊"));
    }

    private boolean blockHas(BcastStopNodeBlock block, String signer) {
        for (TBcastStopSign s : block.getCurrentSigns()) {
            if (signer.equals(s.getSigner())) {
                return true;
            }
        }
        return false;
    }

    private static void assertArrayEqualsL(int[] expected, int[] actual, String msg) {
        assertEquals(expected.length, actual.length, msg);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], msg + "（第" + i + "块）");
        }
    }
}
