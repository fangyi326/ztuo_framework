package cn.ztuo.bitrade.entity;

public enum ExchangeOrderStatus {
    /**
     * 交易中
     */
    TRADING,
    /**
     * 完成
     */
    COMPLETED,
    /**
     * 取消
     */
    CANCELED,
    /**
     * 超时
     */
    OVERTIMED,
    /**
     *等待触发
     */
    WAITING_TRIGGER
    ;
}
