package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fc.v2.model.auto.TBcastStopSign;

/**
 * 核准单上按档分块的一块，四栏齐：
 * 该点几人（{@link #needCount}）、按什么算法算齐（{@link #ruleText}）、
 * 如今记了几人（{@link #signedCount}）、整单此刻落到第几块（{@link #billNodeNo}）。
 *
 * <p>"如今记了几人"不许手填：它是服务层顺着这张单的名次，把明细表上这一档
 * 当前趟的在册条目一笔一笔点出来的条数，{@link #currentSigns} 就是点给他看的
 * 那几笔。屏上那个数只当展示用，真齐没齐只认条数与应点人数两边持平。</p>
 *
 * @author fuce
 * @date 2026-10-03
 */
public class BcastStopNodeBlock implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 档号 0台站 1市广电局 2省局 */
    private int nodeNo;

    /** 档名 */
    private String nodeName;

    /** 该点几人 */
    private int needCount;

    /** 按什么算法算齐（台站头一名落地便算过；市里、省局两笔点齐，缺一笔仍挂候点） */
    private String ruleText;

    /** 如今记了几人：明细表当前趟在册条点出来的条数，不是谁往格子里填的 */
    private int signedCount;

    /** 整单此刻落到第几块（三块回显同一个数：落名出口回话的那一份） */
    private int billNodeNo;

    /** 这一档现在走到第几趟（头一趟为1；退过一次重来便加一趟） */
    private int roundNo;

    /** 条数与应点人数两边持平才算齐 */
    private boolean quotaFilled;

    /** 整单此刻正落在这一块 */
    private boolean atBillNode;

    /** 当前趟在册的那几笔，顺着名次点给他看 */
    private List<TBcastStopSign> currentSigns = new ArrayList<TBcastStopSign>();

    /** 旧册各趟留下的底：退档清零的、再走压旧的都在这，新旧两拨分开放，各留各的底 */
    private List<TBcastStopSign> priorSigns = new ArrayList<TBcastStopSign>();

    public int getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(int nodeNo) {
        this.nodeNo = nodeNo;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public int getNeedCount() {
        return needCount;
    }

    public void setNeedCount(int needCount) {
        this.needCount = needCount;
    }

    public String getRuleText() {
        return ruleText;
    }

    public void setRuleText(String ruleText) {
        this.ruleText = ruleText;
    }

    public int getSignedCount() {
        return signedCount;
    }

    public void setSignedCount(int signedCount) {
        this.signedCount = signedCount;
    }

    public int getBillNodeNo() {
        return billNodeNo;
    }

    public void setBillNodeNo(int billNodeNo) {
        this.billNodeNo = billNodeNo;
    }

    public int getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(int roundNo) {
        this.roundNo = roundNo;
    }

    public boolean isQuotaFilled() {
        return quotaFilled;
    }

    public void setQuotaFilled(boolean quotaFilled) {
        this.quotaFilled = quotaFilled;
    }

    public boolean isAtBillNode() {
        return atBillNode;
    }

    public void setAtBillNode(boolean atBillNode) {
        this.atBillNode = atBillNode;
    }

    public List<TBcastStopSign> getCurrentSigns() {
        return currentSigns;
    }

    public void setCurrentSigns(List<TBcastStopSign> currentSigns) {
        this.currentSigns = currentSigns;
    }

    public List<TBcastStopSign> getPriorSigns() {
        return priorSigns;
    }

    public void setPriorSigns(List<TBcastStopSign> priorSigns) {
        this.priorSigns = priorSigns;
    }
}
