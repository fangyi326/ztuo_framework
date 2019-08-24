package cn.ztuo.bitrade.model.screen;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import cn.ztuo.bitrade.constant.RewardRecordType;
import cn.ztuo.bitrade.entity.QRewardRecord;
import cn.ztuo.bitrade.util.PredicateUtils;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 奖励记录查询条件
 */
@Data
public class RewardRecordScreen extends AccountScreen{

    /**
     * 奖励币种单位
     */
    private String unit ;

    /**
     * 奖励类型
     */
    private RewardRecordType type ;


    public Predicate getPredicate() {
        List<BooleanExpression> booleanExpressions = new ArrayList<>();
        if(!StringUtils.isEmpty(unit)){
            booleanExpressions.add(QRewardRecord.rewardRecord.coin.unit.eq(unit));
        }
        if(type!=null){
            booleanExpressions.add(QRewardRecord.rewardRecord.type.eq(type));
        }
        if(!StringUtils.isEmpty(account)){
            booleanExpressions.add(QRewardRecord.rewardRecord.member.username.like("%"+account+"%")
                    .or(QRewardRecord.rewardRecord.member.mobilePhone.like(account+"%"))
                    .or(QRewardRecord.rewardRecord.member.email.like(account+"%"))
                    .or(QRewardRecord.rewardRecord.member.realName.like("%"+account+"%")));
        }
        return PredicateUtils.getPredicate(booleanExpressions);
    }
}
