package cn.ztuo.bitrade.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.math.BigDecimal;

/**
 * @description: MemberGrade
 * 会员等级设置
 * @author: MrGao
 * @create: 2019/04/25 15:41
 */
@Entity
@Data
public class MemberGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 等级名称
     */
    private String gradeName ;
    /**
     * 等级code
     */
    private String gradeCode ;
    /**
     * 每日提币数量限制
     */
    private BigDecimal withdrawCoinAmount ;
    /**
     * 每日提币笔数
     */
    private Integer dayWithdrawCount ;
    /**
     * 手续费比例
     */
    private BigDecimal exchangeFeeRate ;
    /**
     * 等级界限 积分的多少 设置成会员等级
     */
    private Integer gradeBound;



}
