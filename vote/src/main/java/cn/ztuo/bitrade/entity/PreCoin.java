package cn.ztuo.bitrade.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.persistence.*;

/**
 * @author GuoShuai
 * @date 2018年03月26日
 */
@Entity
@Data
public class PreCoin{
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private String name;

    private String unit;

    private String remark;

    private int amount=0;

    @Column(columnDefinition = "varchar(256) comment '详情链接'")
    private String link;
    @JsonIgnore
    @ManyToOne(targetEntity = Vote.class)
    private Vote vote;
    @JsonIgnore
    @Version
    private long version;
}
