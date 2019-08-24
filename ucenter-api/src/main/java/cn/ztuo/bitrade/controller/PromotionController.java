package cn.ztuo.bitrade.controller;

import com.querydsl.core.types.Predicate;
import cn.ztuo.bitrade.constant.PageModel;
import cn.ztuo.bitrade.constant.PromotionLevel;
import cn.ztuo.bitrade.constant.RewardRecordType;
import cn.ztuo.bitrade.constant.TransactionType;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.entity.transform.AuthMember;
import cn.ztuo.bitrade.pagination.PageResult;
import cn.ztuo.bitrade.service.MemberService;
import cn.ztuo.bitrade.service.MemberTransactionService;
import cn.ztuo.bitrade.service.RewardRecordService;
import cn.ztuo.bitrade.util.MessageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.ztuo.bitrade.constant.SysConstant.SESSION_MEMBER;

/**
 * 推广
 *
 * @author GuoShuai
 * @date 2018年03月19日
 */
@RestController
@RequestMapping(value = "/promotion")
public class PromotionController {
    @Autowired
    private MemberService memberService;
    @Autowired
    private RewardRecordService rewardRecordService;
    @Autowired
    private MemberTransactionService transactionService;

    /**
     * 推广记录查询
     *
     * @param member
     * @return
     */
    @RequestMapping(value = "/record")
    public MessageResult promotionRecord(
            PageModel pageModel,
            @SessionAttribute(SESSION_MEMBER) AuthMember member) {

        Predicate predicate = QMember.member.inviterId.eq(member.getId());
        Page<Member> page = memberService.findAll(predicate,pageModel.getPageable());

        List<Member> list = page.getContent() ;

        List<PromotionMember> list1 = list.stream().map(x ->
                PromotionMember.builder().createTime(x.getRegistrationTime())
                        .level(PromotionLevel.ONE)
                        .username(x.getUsername())
                        .build()
        ).collect(Collectors.toList());
        if (list.size() > 0) {
            list.stream().forEach(x -> {
                if (x.getPromotionCode() != null) {
                    list1.addAll(memberService.findPromotionMember(x.getId()).stream()
                            .map(y ->
                                    PromotionMember.builder().createTime(y.getRegistrationTime())
                                            .level(PromotionLevel.TWO)
                                            .username(y.getUsername())
                                            .build()
                            ).collect(Collectors.toList()));
                }
            });
        }

        MessageResult messageResult = MessageResult.success();
        PageResult<PromotionMember> pageResult = new PageResult<>(list1.stream().sorted((x, y) -> {
            if (x.getCreateTime().after(y.getCreateTime())) {
                return -1;
            } else {
                return 1;
            }
        }).collect(Collectors.toList()) ,pageModel.getPageNo()+1,page.getSize(),page.getTotalElements());
        messageResult.setData(pageResult);
        return messageResult;
    }

    /**
     * 推广奖励记录
     *
     * @param member
     * @return
     */
    @RequestMapping(value = "/reward/record")
    public MessageResult rewardRecord(
            PageModel pageModel,
            @SessionAttribute(SESSION_MEMBER) AuthMember member) {
        Predicate predicate = QRewardRecord.rewardRecord.member.id.eq(member.getId()).and(QRewardRecord.rewardRecord.type.eq(RewardRecordType.PROMOTION));

        Page<RewardRecord> page = rewardRecordService.findAll(predicate,pageModel);

        List<RewardRecord> list = page.getContent() ;

        MessageResult result = MessageResult.success();

        PageResult<PromotionRewardRecord> pageResult = new PageResult<>(list.stream().map(x ->
                PromotionRewardRecord.builder().amount(x.getAmount())
                        .createTime(x.getCreateTime())
                        .remark(x.getRemark())
                        .symbol(x.getCoin().getUnit())
                        .build()
        ).collect(Collectors.toList()),pageModel.getPageNo()+1,page.getSize(),page.getTotalElements());
        result.setData(pageResult);
        return result;
    }

    /**
     * 获取推广统计数据
     * @param member
     * @return
     */
    @RequestMapping(value = "/summary")
    public  Map<String,Object> summary(@SessionAttribute(SESSION_MEMBER) AuthMember member){
        Map<String,Object> map = new HashMap<>();
        Predicate predicate = QRewardRecord.rewardRecord.member.id.eq(member.getId()).and(QRewardRecord.rewardRecord.type.eq(RewardRecordType.PROMOTION));
        map.put("count",rewardRecordService.findCount(predicate));
        map.put("amount",transactionService.findTransactionSum(member.getId(), TransactionType.PROMOTION_AWARD));
        return map;
    }
}
