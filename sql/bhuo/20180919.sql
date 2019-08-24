-- 新增ETH竞猜类型
insert into quiz_type (quiz_currency,bet_currency,bet_amount,full_quota,deadline,state,first_prize,second_prize,third_prize,create_time,first_amount,second_amount,third_amount,poundage_fee,premium_rate,giving_bhb_rate) 
values ('ETH','USDT',1.000,0.85,'20:25:00',0,0.4, 0.06, 0,now(),1,10,0,0.100,0.200,1.000);
-- 新建手续费折合表
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for exchange_order_transaction_mine
-- ----------------------------
DROP TABLE IF EXISTS `poundage_convert_eth`;
CREATE TABLE `poundage_convert_eth` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `exchange_order_id` varchar(255) DEFAULT NULL COMMENT '订单id',
  `member_id` varchar(255) DEFAULT NULL COMMENT '会员id',
  `mine_amount` decimal(18,8) NOT NULL COMMENT '挖矿个数BHB',
  `poundage_amount` decimal(18,8) NOT NULL COMMENT '手续费',
  `poundage_amount_Eth` decimal(18,8) NOT NULL COMMENT '折成以太坊手续费',
  `coin_id` varchar(255) NOT NULL COMMENT '该手续费币种id',
  `transaction_time` datetime DEFAULT NULL COMMENT '交易时间',
  `type` varchar(255) DEFAULT NULL COMMENT '交易类型',
  `symbol` varchar(255) DEFAULT NULL COMMENT '交易对',
  `direction` varchar(255) DEFAULT NULL COMMENT '交易方向',
  `usdt_rate` varchar(255) DEFAULT NULL COMMENT '手续费对USDT费率',
  `eth_usdt_rate` varchar(255) DEFAULT NULL COMMENT 'ETH对USDT费率',
  PRIMARY KEY (`id`),
  KEY `idx_transaction_time` (`transaction_time`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;