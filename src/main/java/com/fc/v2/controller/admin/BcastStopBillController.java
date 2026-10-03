package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TBcastStopBill;
import com.fc.v2.model.custom.BcastStopBillView;
import com.fc.v2.model.custom.BcastStopRoster;
import com.fc.v2.service.ITBcastStopBillService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 停播检修核准单 Controller（approval-chain 形状：台站→市广电局→省局，三道按次序走）。
 *
 * <p>屏上只有一处把手：落名、否决、退回都递到服务层那一个落名出口
 * {@code /sign}（动作不同而已）。屏上带出的档号只当意图随这一处递进去，
 * 这里不留第二处能把档号直接改定的把手，也不替服务层断定收不收。
 * 省局点齐之后添不添得进人、越档先落的作不作数、退一档本档这一轮的名清不清，
 * 三样一律看出口回面上的话；回话与屏上档号不合，按回话那一份走。</p>
 *
 * @author fuce
 * @date 2026-10-03
 */
@Api(value = "停播检修核准单")
@Controller
@RequestMapping("/bcastStopBill")
public class BcastStopBillController extends BaseController {

    private final String prefix = "admin/bcastStopBill";

    @Autowired
    private ITBcastStopBillService bcastStopBillService;

    @ApiOperation(value = "核准台账跳转", notes = "核准台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("bcastStopBill:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "停播检修核准台账", action = "list")
    @ApiOperation(value = "台账", notes = "台账：各单落到第几块、记了几人按落名明细回数")
    @GetMapping("/list")
    @RequiresPermissions("bcastStopBill:list")
    @ResponseBody
    public ResultTable list(TBcastStopBill record) {
        QueryWrapper<TBcastStopBill> queryWrapper = new QueryWrapper<TBcastStopBill>();
        if (record.getBillNo() != null && !record.getBillNo().trim().isEmpty()) {
            queryWrapper.like("bill_no", record.getBillNo().trim());
        }
        queryWrapper.orderByDesc("create_time");
        startPage();
        com.github.pagehelper.PageInfo<TBcastStopBill> page =
                new com.github.pagehelper.PageInfo<TBcastStopBill>(
                        bcastStopBillService.selectTBcastStopBillList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "停播检修核准单开单", action = "file")
    @ApiOperation(value = "开单", notes = "从台站档起、在核；档号应点已点各栏系统落笔")
    @PostMapping("/file")
    @RequiresPermissions("bcastStopBill:file")
    @ResponseBody
    public AjaxResult file(String billNo, String remark) {
        TBcastStopBill bill = bcastStopBillService.fileStopBill(billNo, remark);
        return bill == null ? AjaxResult.error("代号空着，或同号还有一张在核：单立不起来")
                : AjaxResult.successData(200, bill);
    }

    @Log(title = "停播检修落名", action = "sign")
    @ApiOperation(value = "落名", notes = "唯一落名出口；intentNodeNo 只算意图，越档先落不作数")
    @PostMapping("/sign")
    @RequiresPermissions("bcastStopBill:sign")
    @ResponseBody
    public AjaxResult sign(Long id, Integer intentNodeNo, String signer, String comment) {
        BcastStopBillView view = bcastStopBillService.driveStopBill(id, intentNodeNo, signer, comment, "sign");
        return view == null ? AjaxResult.error("这一笔没落下：档口不对、缺人、已排定或已了断，看整面出口那一份")
                : AjaxResult.successData(200, view);
    }

    @Log(title = "停播检修核准否决", action = "veto")
    @ApiOperation(value = "否决", notes = "哪一档否了全单了断，往后各档不得再记名")
    @PostMapping("/veto")
    @RequiresPermissions("bcastStopBill:veto")
    @ResponseBody
    public AjaxResult veto(Long id, Integer intentNodeNo, String comment) {
        BcastStopBillView view = bcastStopBillService.driveStopBill(id, intentNodeNo, null, comment, "veto");
        return view == null ? AjaxResult.error("否不了：单不在这个档口，或已排定、已了断")
                : AjaxResult.successData(200, view);
    }

    @Log(title = "停播检修退回上一档", action = "back")
    @ApiOperation(value = "退回上一档", notes = "只退一格；本档这一轮的名当场清零留底，旧档各名不动")
    @PostMapping("/back")
    @RequiresPermissions("bcastStopBill:back")
    @ResponseBody
    public AjaxResult back(Long id, Integer intentNodeNo, String comment) {
        BcastStopBillView view = bcastStopBillService.driveStopBill(id, intentNodeNo, null, comment, "back");
        return view == null ? AjaxResult.error("退不了：已在头一道、档口不对，或已排定、已了断")
                : AjaxResult.successData(200, view);
    }

    @Log(title = "停播检修核准单面", action = "detail")
    @ApiOperation(value = "整面回查", notes = "三块，一块四栏；已记几人顺着名次一笔一笔点出来")
    @GetMapping("/detail")
    @RequiresPermissions("bcastStopBill:view")
    @ResponseBody
    public AjaxResult detail(Long id) {
        BcastStopBillView view = bcastStopBillService.viewBill(id);
        return view == null ? AjaxResult.error("查无此单") : AjaxResult.successData(200, view);
    }

    @Log(title = "检修排期名册", action = "roster")
    @ApiOperation(value = "检修排期名册", notes = "只对已排定的单出；与核准结果同一次算、同一凭据")
    @GetMapping("/roster")
    @RequiresPermissions("bcastStopBill:roster")
    @ResponseBody
    public AjaxResult roster(Long id) {
        BcastStopRoster roster = bcastStopBillService.rosterOf(id);
        return roster == null ? AjaxResult.error("单还没排定，或三块笔数对不上：名册不出")
                : AjaxResult.successData(200, roster);
    }
}
