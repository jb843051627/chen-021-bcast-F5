package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.Date;

/**
 * 检修排期名册上的一笔：核准单上落定的一个名。
 *
 * <p>一笔对应明细表当前趟在册的一行，不许多抄、不许漏抄。自省局往回收数到台站，
 * 名册就按这个次序排：省局两笔在前、市里两笔居中、台站一笔压尾。</p>
 *
 * @author fuce
 * @date 2026-10-03
 */
public class BcastStopRosterEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 核准单代号 */
    private String billNo;

    /** 同算凭据：与核准结果出自同一次算 */
    private String bizKey;

    /** 档号 0台站 1市广电局 2省局 */
    private int nodeNo;

    /** 档名 */
    private String nodeName;

    /** 这一档的第几趟（头一趟为1） */
    private int roundNo;

    /** 落名人 */
    private String signer;

    /** 落名时刻（哪日补的各留各的底） */
    private Date signTime;

    public BcastStopRosterEntry() {
    }

    public BcastStopRosterEntry(String billNo, String bizKey, int nodeNo, String nodeName,
                                int roundNo, String signer, Date signTime) {
        this.billNo = billNo;
        this.bizKey = bizKey;
        this.nodeNo = nodeNo;
        this.nodeName = nodeName;
        this.roundNo = roundNo;
        this.signer = signer;
        this.signTime = signTime;
    }

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

    public int getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(int roundNo) {
        this.roundNo = roundNo;
    }

    public String getSigner() {
        return signer;
    }

    public void setSigner(String signer) {
        this.signer = signer;
    }

    public Date getSignTime() {
        return signTime;
    }

    public void setSignTime(Date signTime) {
        this.signTime = signTime;
    }
}
