package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 停播检修核准单落名明细对象 t_bcast_stop_sign
 *
 * <p>顺着核准单的名次一笔一笔记在这：已记几人不许手填，全凭这张明细表上
 * 当前趟的在册条数点出来。退一档时本档这一轮已经点下的名当场清零——行不抹掉，
 * 打上删除标记留底；早先各档的旧名一笔不动。重走另起一趟，新旧两拨分开放。</p>
 *
 * @author fuce
 * @date 2026-10-03
 */
@TableName("t_bcast_stop_sign")
@ApiModel(value = "TBcastStopSign", description = "停播检修核准单落名明细")
public class TBcastStopSign implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所属停播检修核准单 */
    @TableField("bill_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属停播检修核准单")
    private Long billId;

    /** 所属核准单代号（冗余留底） */
    @TableField("bill_no")
    @ApiModelProperty(value = "所属核准单代号")
    private String billNo;

    /** 落名档口 0台站 1市广电局 2省局 */
    @TableField("node_no")
    @ApiModelProperty(value = "落名档口 0台站 1市广电局 2省局")
    private Integer nodeNo;

    /** 该档第几趟落名（头一趟为1；退一次重来加一趟） */
    @TableField("round_no")
    @ApiModelProperty(value = "该档第几趟落名")
    private Integer roundNo;

    /** 落名人 */
    @TableField("signer")
    @ApiModelProperty(value = "落名人")
    private String signer;

    /** 落名时留下的话 */
    @TableField("sign_note")
    @ApiModelProperty(value = "落名时留下的话")
    private String signNote;

    /** 落名时刻 */
    @TableField("sign_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "落名时刻")
    private Date signTime;

    /** 删除标记 0在册（当前趟）1退档清零留下的底 2否决留底 3再走时压作旧册的上一趟底 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0当前趟在册 1退档清零底 2否决留底 3旧趟册底")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public String getSigner() {
        return signer;
    }

    public void setSigner(String signer) {
        this.signer = signer;
    }

    public String getSignNote() {
        return signNote;
    }

    public void setSignNote(String signNote) {
        this.signNote = signNote;
    }

    public Date getSignTime() {
        return signTime;
    }

    public void setSignTime(Date signTime) {
        this.signTime = signTime;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
