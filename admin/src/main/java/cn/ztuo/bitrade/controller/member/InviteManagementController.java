package cn.ztuo.bitrade.controller.member;

import cn.ztuo.bitrade.annotation.AccessLog;
import cn.ztuo.bitrade.constant.AdminModule;
import cn.ztuo.bitrade.controller.BaseController;
import cn.ztuo.bitrade.entity.Member;
import cn.ztuo.bitrade.service.InviteManagementService;
import cn.ztuo.bitrade.util.MessageResult;
import cn.ztuo.bitrade.vo.InviteManagementVO;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@Slf4j
@RequestMapping("invite/management")
public class InviteManagementController extends BaseController {

    @Autowired
    private InviteManagementService inviteManagementService;

    /**
     * 邀请管理默认查询所有的用户
     *
     * @return
     */
    @RequiresPermissions("invite/management:query")
    @AccessLog(module = AdminModule.CMS, operation = "邀请管理默认查询所有的用户")
    @RequestMapping(value = "look", method = RequestMethod.POST)
    public MessageResult lookAll(@RequestBody InviteManagementVO inviteManagementVO) {
        log.info("默认查询所有的用户 lookAll ={}", inviteManagementVO);
        Page<Member> page = inviteManagementService.lookAll(inviteManagementVO);
//        List<Member> content = page.getContent();
//        return successDataAndTotal(content, page.getTotalElements());
        return success(page);
    }

    /**
     * 条件查询
     */
    @AccessLog(module = AdminModule.CMS, operation = "邀请管理多条件查询")
    @RequestMapping(value = "query", method = RequestMethod.POST)
    public MessageResult queryCondition(@RequestBody InviteManagementVO inviteManagementVO) {
        log.info("默认查询所有的用户 QueryCondition ={}", inviteManagementVO);
        Page<Member> page = inviteManagementService.queryCondition(inviteManagementVO);
//        List<Member> content = page.getContent();
//        return successDataAndTotal(content, page.getTotalElements());
        return success(page);
    }

    /**
     * 根据id查询一级二级用户
     */
    @AccessLog(module = AdminModule.CMS, operation = "根据id查询一级二级用户")
    @RequestMapping(value = "info", method = RequestMethod.POST)
    public MessageResult queryId(@RequestBody InviteManagementVO inviteManagementVO) {
        log.info("根据id查询一级二级用户 queryById={}", inviteManagementVO);
        Page<Member> page = inviteManagementService.queryId(inviteManagementVO);
//        List<Member> content = page.getContent();
//        return successDataAndTotal(content,page.getTotalElements() );
        return success(page);
    }


}
