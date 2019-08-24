package cn.ztuo.bitrade.dao;

import cn.ztuo.bitrade.entity.Airdrop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

public interface AirdropDao  extends JpaRepository<Airdrop, String>, JpaSpecificationExecutor<Airdrop>,
        QueryDslPredicateExecutor<Airdrop> {
    Airdrop findById(Long id);
}
