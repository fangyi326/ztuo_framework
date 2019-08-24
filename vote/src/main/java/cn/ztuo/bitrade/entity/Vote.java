package cn.ztuo.bitrade.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import cn.ztuo.bitrade.constant.BooleanEnum;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author GuoShuai
 * @date 2018年03月26日
 */
@Entity
@Data
public class Vote{
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    /**
     * 是否是开启
     */
    @Enumerated(EnumType.ORDINAL)
    private BooleanEnum status = BooleanEnum.IS_FALSE;

    @Column(columnDefinition = "decimal(18,2) comment '每次投票消耗的平台币数量'")
    private BigDecimal amount;

    /** 本轮投票个人总投票限制 */
    private int voteLimit=0;

    @OrderBy("id")
    @OneToMany(targetEntity = PreCoin.class,cascade = CascadeType.ALL)
    @JoinColumn(name="vote_id")
    private List<PreCoin> preCoins;

}
