package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fc.v2.model.auto.TBcastStopBill;

/**
 * 停播检修核准单整面：主单三栏作展示，三块按档排开，一块四栏。
 *
 * <p>这一面上的"落到第几块、各块记了几人"全由服务层从落名明细当场点出来，
 * 屏上回传的档号与这面不合时，以这面（落名出口的回话）为准。省局点齐之后
 * 这一面即封卷，谁也不能再往里添名。</p>
 *
 * @author fuce
 * @date 2026-10-03
 */
public class BcastStopBillView implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主单（need_count/sign_count 等栏只当展示） */
    private TBcastStopBill bill;

    /** 三道关口三块，按台站→市广电局→省局次序 */
    private List<BcastStopNodeBlock> blocks = new ArrayList<BcastStopNodeBlock>();

    /** 已点齐的档数（出口顺着名次数出来的，主单 node_no 与它不一致时以它为准） */
    private int derivedNodeNo;

    /** 已排定（省局点齐，封卷） */
    private boolean finalized;

    /** 已了断（被某一档否了，留底不再记名） */
    private boolean vetoed;

    /** 三块当前趟在册落名加起来的笔数：与排期名册那本名册同一次点出来 */
    private int currentNameCount;

    public TBcastStopBill getBill() {
        return bill;
    }

    public void setBill(TBcastStopBill bill) {
        this.bill = bill;
    }

    public List<BcastStopNodeBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<BcastStopNodeBlock> blocks) {
        this.blocks = blocks;
    }

    public int getDerivedNodeNo() {
        return derivedNodeNo;
    }

    public void setDerivedNodeNo(int derivedNodeNo) {
        this.derivedNodeNo = derivedNodeNo;
    }

    public boolean isFinalized() {
        return finalized;
    }

    public void setFinalized(boolean finalized) {
        this.finalized = finalized;
    }

    public boolean isVetoed() {
        return vetoed;
    }

    public void setVetoed(boolean vetoed) {
        this.vetoed = vetoed;
    }

    public int getCurrentNameCount() {
        return currentNameCount;
    }

    public void setCurrentNameCount(int currentNameCount) {
        this.currentNameCount = currentNameCount;
    }
}
