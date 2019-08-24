package cn.ztuo.bitrade.model.update;

import cn.ztuo.bitrade.constant.BooleanEnum;
import cn.ztuo.bitrade.constant.CommonStatus;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import java.math.BigDecimal;

@Data
public class CoinUpdate {


    @NotBlank(message = "name不得为空")
    private String name;
    /**
     * 中文
     */
    @NotBlank(message = "中文名称不得为空")
    private String nameCn;
    /**
     * 缩写
     */
    @NotBlank(message = "单位不得为空")
    private String unit;
    /**
     * 状态
     */
    private CommonStatus status = CommonStatus.NORMAL;

    /**
     * 最小提币手续费
     */
    private Double minTxFee;
    /**
     * 对人民币汇率
     */
    private double cnyRate;
    /**
     * 最大提币手续费
     */
    private Double maxTxFee;
    /**
     * 对美元汇率
     */
    private double usdRate;
    /**
     * 是否支持rpc接口
     */
    private BooleanEnum enableRpc = BooleanEnum.IS_TRUE;

    /**
     * 是否能提币
     */
    private BooleanEnum canWithdraw;

    /**
     * 是否能充币
     */
    private BooleanEnum canRecharge;


    /**
     * 是否能自动提币
     */
    private BooleanEnum canAutoWithdraw;

    /**
     * 提币阈值
     */
    private BigDecimal withdrawThreshold;
    private BigDecimal minWithdrawAmount;
    private BigDecimal maxWithdrawAmount;


    private int withdrawScale;
    /**
     * 提现手续费改为一个值
     */
    private Double txFee;
    /**
     * 最小充币量
     */
    private BigDecimal minRechargeAmount;
    /**
     * 矿工费
     */
    private BigDecimal minerFee;

    private int sort;

    private BigDecimal maxDailyWithdrawRate;
}
