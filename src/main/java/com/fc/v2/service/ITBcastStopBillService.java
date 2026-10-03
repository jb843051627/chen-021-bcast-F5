package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TBcastStopBill;
import com.fc.v2.model.custom.BcastStopBillView;
import com.fc.v2.model.custom.BcastStopRoster;

/**
 * 停播检修核准单 Service接口（approval-chain 形状：三道关口按次序走）。
 *
 * <p>这张检修单走到哪一档、下一档收不收它，两处结论都只出自服务层那一个落名出口
 * {@link #driveStopBill}；递上来的档号只当意图看待。省局点齐之后还添不添得进人、
 * 越档先落的那一笔作不作数、退一档时本档这一轮的名清不清，三样一律听这个出口的回话。</p>
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITBcastStopBillService {

    /** 按主键回查单据（签名照旧，进什么样出什么样，不添新参；档号以出口数出来的为准） */
    TBcastStopBill selectTBcastStopBillById(Long id);

    /** 台账列查：逐张按落名明细把档口、已落名数回数到实处 */
    List<TBcastStopBill> selectTBcastStopBillList(QueryWrapper<TBcastStopBill> queryWrapper);

    /** 开单：从台站那一档起，在核；档号、应点、已点各栏系统落笔 */
    TBcastStopBill fileStopBill(String billNo, String remark);

    /**
     * 唯一落名出口。action：sign 落名 / veto 否了 / back 退回上一档。
     * intentNodeNo 是屏上递来的档号，只算意图——与出口数出来的不合，按回话这一份走。
     * 被拒（越档、已排定、已了断、人没给、退到头）返回 null。
     */
    BcastStopBillView driveStopBill(Long id, Integer intentNodeNo, String signer, String comment, String action);

    /** 整面回查：三块、一块四栏，已记几人顺着名次一笔一笔点出来 */
    BcastStopBillView viewBill(Long id);

    /** 检修排期名册：只对已排定的单出，与核准结果同一次算、同一凭据 */
    BcastStopRoster rosterOf(Long id);
}
