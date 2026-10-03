package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 检修排期名册：一张已排定核准单换出的那本名册。
 *
 * <p>核准结果与名册两边出自同一次算——同一次从落名明细点上来，落下同一个
 * {@link #bizKey}，不许一处取数一处手填。事后翻查，自省局往回收数到台站，
 * 一块一块清点：{@link #entries} 的笔数与当日窗口那份排期名单一笔不差，
 * 任取一块单看（{@link #nodeCounts}）也经得起复核。</p>
 *
 * @author fuce
 * @date 2026-10-03
 */
public class BcastStopRoster implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 核准单代号 */
    private String billNo;

    /** 同算凭据：与核准结果同一次算落同一凭据 */
    private String bizKey;

    /** 名册条目，自省局往回收数到台站 */
    private List<BcastStopRosterEntry> entries = new ArrayList<BcastStopRosterEntry>();

    /** 各块笔数：下标即档号 0台站 1市广电局 2省局，任取一块单看也经得起复核 */
    private int[] nodeCounts = new int[3];

    /** 总笔数：三块加起来，台站1+市里2+省局2=5 */
    private int totalCount;

    /** 与当日窗口那份排期名单是否一笔不差（三块笔数各自对得上应点人数） */
    private boolean balanced;

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getBizKey() {
        return bizKey;
    }

    public void setBizKey(String bizKey) {
        this.bizKey = bizKey;
    }

    public List<BcastStopRosterEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<BcastStopRosterEntry> entries) {
        this.entries = entries;
    }

    public int[] getNodeCounts() {
        return nodeCounts;
    }

    public void setNodeCounts(int[] nodeCounts) {
        this.nodeCounts = nodeCounts;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public void setBalanced(boolean balanced) {
        this.balanced = balanced;
    }
}
