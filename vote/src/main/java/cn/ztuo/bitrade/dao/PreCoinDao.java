package cn.ztuo.bitrade.dao;

import cn.ztuo.bitrade.entity.PreCoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @author GuoShuai
 * @date 2018年03月26日
 */
public interface PreCoinDao extends JpaRepository<PreCoin,Long>,JpaSpecificationExecutor<PreCoin> {

}
