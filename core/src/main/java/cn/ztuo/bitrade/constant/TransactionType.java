package cn.ztuo.bitrade.constant;

import com.fasterxml.jackson.annotation.JsonValue;
import cn.ztuo.bitrade.core.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TransactionType implements BaseEnum {
    RECHARGE("充值"),//0
    WITHDRAW("提现"),//1
    TRANSFER_ACCOUNTS("转账"),//2
    EXCHANGE("币币交易"),//3
    OTC_BUY("法币买入"),//4
    OTC_SELL("法币卖出"),//5
    ACTIVITY_AWARD("活动奖励"),//6
    PROMOTION_AWARD("推广奖励"),//7
    DIVIDEND("分红"),//8
    VOTE("投票"),//9
    ADMIN_RECHARGE("人工充值"),//10
    MATCH("配对"),//11
    DEPOSIT("缴纳商家认证保证金"),//12
    GET_BACK_DEPOSIT("退回商家认证保证金"),//13
    LEGAL_RECHARGE("法币充值"),//14
    ASSET_EXCHANGE("币币兑换"),//15
    CHANNEL_AWARD("渠道推广"),//16
    TRANSFER_INTO_LEVER("划转入杠杆钱包"),//17
    TRANSFER_OUT_LEVER("从杠杆钱包划转出"),//18
    MANUAL_AIRDROP("钱包空投"),//19
    LOCK_POSITION("锁仓"),//20
    UNLOCK_POSITION("解锁"),//21
    THIRD_PARTY_TRANSFER("第三方转入"),//22
    THIRD_PARTY_TURN_OUT("第三方转出"),//23
    COIN_TWO_OTC("币币转入法币"),//24
    OTC_TWO_COIN("法币转入币币"),//25
    LOAN_RECORD("借贷流水"),//26
    REPAYMENT_LOAN("还款流水");//27

    private String cnName;
    @Override
    @JsonValue
    public int getOrdinal() {
        return this.ordinal();
    }

    public static TransactionType valueOfOrdinal(int ordinal){
        switch (ordinal){
            case 0:return RECHARGE;
            case 1:return WITHDRAW;
            case 2:return TRANSFER_ACCOUNTS;
            case 3:return EXCHANGE;
            case 4:return OTC_BUY;
            case 5:return OTC_SELL;
            case 6:return ACTIVITY_AWARD;
            case 7:return PROMOTION_AWARD;
            case 8:return DIVIDEND;
            case 9:return VOTE;
            case 10:return ADMIN_RECHARGE;
        }
        return null;
    }
    public static int parseOrdinal(TransactionType ordinal) {
        if (TransactionType.RECHARGE.equals(ordinal)) {
            return 0;
        } else if (TransactionType.WITHDRAW.equals(ordinal)) {
            return 1;
        } else if (TransactionType.TRANSFER_ACCOUNTS.equals(ordinal)) {
            return 2;
        } else if (TransactionType.EXCHANGE.equals(ordinal)) {
            return 3;
        } else if (TransactionType.OTC_BUY.equals(ordinal)) {
            return 4;
        } else if (TransactionType.OTC_SELL.equals(ordinal)) {
            return 5;
        } else if (TransactionType.ACTIVITY_AWARD.equals(ordinal)) {
            return 6;
        }else if (TransactionType.PROMOTION_AWARD.equals(ordinal)) {
            return 7;
        }else if (TransactionType.DIVIDEND.equals(ordinal)) {
            return 8;
        }else if (TransactionType.VOTE.equals(ordinal)) {
            return 9;
        }else if (TransactionType.ADMIN_RECHARGE.equals(ordinal)) {
            return 10;
        }else {
            return  20;
        }
    }

}
