package cn.ztuo.bitrade.dao;

import cn.ztuo.bitrade.entity.ExchangeCoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExchangeCoinRepository extends JpaRepository<ExchangeCoin, String>, JpaSpecificationExecutor<ExchangeCoin>, QueryDslPredicateExecutor<ExchangeCoin> {
    ExchangeCoin findBySymbol(String symbol);

    @Query("select distinct a.baseSymbol from  ExchangeCoin a where a.enable = 1")
    List<String> findBaseSymbol();

    @Query("select distinct a.coinSymbol from  ExchangeCoin a where a.enable = 1 and a.baseSymbol = :baseSymbol")
    List<String> findCoinSymbol(@Param("baseSymbol") String baseSymbol);

    @Query("select distinct a.coinSymbol from  ExchangeCoin a where a.enable = 1")
    List<String> findAllCoinSymbol();

    ExchangeCoin findExchangeCoinByDefaultSymbol(String defaultSymbol);

    /**
     * 修改交易对儿为非默认
     * @return
     */
    @Query("update ExchangeCoin set defaultSymbol='0'")
    Integer updateDefaultSymbol();
}
