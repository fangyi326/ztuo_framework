package cn.ztuo.bitrade.entity;

public enum ExchangeOrderType {
    /**
     * 市价
     */
    MARKET_PRICE,
    /**
     * 限价
     */
    LIMIT_PRICE,
    /**
     * 止盈止损
     */
    CHECK_FULL_STOP;
}
