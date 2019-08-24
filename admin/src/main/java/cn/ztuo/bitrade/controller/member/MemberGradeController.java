package cn.ztuo.bitrade.controller.member;

import cn.ztuo.bitrade.annotation.AccessLog;
import cn.ztuo.bitrade.constant.AdminModule;
import cn.ztuo.bitrade.controller.BaseController;
import cn.ztuo.bitrade.entity.MemberGrade;
import cn.ztuo.bitrade.service.MemberGradeService;
import cn.ztuo.bitrade.util.MessageResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description: MemberGradeController
 * @author: MrGao
 * @create: 2019/04/25 16:12
 */
@Slf4j
@RestController
@RequestMapping("member/grade")
public class MemberGradeController extends BaseController {

    @Autowired
    private MemberGradeService gradeService ;

    @RequiresPermissions("member:member-grade:all")
    @RequestMapping(value = "all",method = RequestMethod.GET)
    @AccessLog(module = AdminModule.MEMBER, operation = "所有会员等级")
    public MessageResult findAll() {
        List<MemberGrade> memberGrades = gradeService.findAll();
        return success(memberGrades);
    }

    @RequiresPermissions("member:member-grade:update")
    @PostMapping("update")
    @AccessLog(module = AdminModule.MEMBER, operation = "更新会员等级")
    @Transactional(rollbackFor = Exception.class)
    public MessageResult update(@RequestBody MemberGrade memberGrade) throws Exception {

        if (memberGrade.getId() == null) {
            return error("主键不得为空");
        }
        MemberGrade one = gradeService.findOne(memberGrade.getId());
        if (one == null) {
            return error("修改对象不存在");
        }
        if(memberGrade.getId()==1){
           MemberGrade grade = gradeService.findOne(2L);
           if(memberGrade.getGradeBound()>grade.getGradeBound()){
               return error("V1积分边界不允许大于V2积分边界");
           }
        }else if(memberGrade.getId()==2) {
            MemberGrade maxGrade = gradeService.findOne(3L);
            MemberGrade minGrade = gradeService.findOne(1L);
            if(memberGrade.getGradeBound()<minGrade.getGradeBound()){
                return error("V2积分边界不允许小于V1积分边界");
            }
            if(memberGrade.getGradeBound()>maxGrade.getGradeBound()){
                return error("V2积分边界不允许大于V3积分边界");
            }
        }else if(memberGrade.getId()==3) {
            MemberGrade maxGrade = gradeService.findOne(4L);
            MemberGrade minGrade = gradeService.findOne(2L);
            if(memberGrade.getGradeBound()<minGrade.getGradeBound()){
                return error("V3积分边界不允许小于V2积分边界");
            }
            if(memberGrade.getGradeBound()>maxGrade.getGradeBound()){
                return error("V3积分边界不允许大于V4积分边界");
            }
        }else if(memberGrade.getId()==4){
            MemberGrade maxGrade = gradeService.findOne(5L);
            MemberGrade minGrade = gradeService.findOne(3L);
            if(memberGrade.getGradeBound()<minGrade.getGradeBound()){
                return error("V4积分边界不允许小于V3积分边界");
            }
            if(memberGrade.getGradeBound()>maxGrade.getGradeBound()){
                return error("V4积分边界不允许大于V5积分边界");
            }
        }else if(memberGrade.getId()==5){
            MemberGrade minGrade = gradeService.findOne(4L);
            if(memberGrade.getGradeBound()<minGrade.getGradeBound()){
                return error("V5积分边界不允许小于V4积分边界");
            }
        }
        MemberGrade save = gradeService.save(memberGrade);
        return success(save);
    }


}
