/*
Navicat MySQL Data Transfer

Source Server         : 交易
Source Server Version : 50640
Source Host           : 39.104.97.192:3306
Source Database       : bitrade

Target Server Type    : MYSQL
Target Server Version : 50640
File Encoding         : 65001

Date: 2018-07-02 21:17:28
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for exchange_order_transaction_mine
-- ----------------------------
DROP TABLE IF EXISTS `exchange_order_transaction_mine`;
CREATE TABLE `exchange_order_transaction_mine` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `exchange_order_id` varchar(255) DEFAULT NULL COMMENT '订单id',
  `member_id` varchar(255) DEFAULT NULL COMMENT '会员id',
  `mine_amount` decimal(18,8) NOT NULL COMMENT '挖矿个数BHB',
  `poundage_amount` decimal(18,8) NOT NULL COMMENT '手续费',
  `poundage_amount_Eth` decimal(18,8) NOT NULL COMMENT '折成以太坊手续费',
  `bouns_state` varchar(1) DEFAULT NULL COMMENT '分红返还状态 0 未返还 1 已返还',
  `coin_id` varchar(255) NOT NULL COMMENT '该手续费币种id',
  `transaction_time` datetime DEFAULT NULL COMMENT '交易时间',
  `inviter_state` varchar(100) DEFAULT NULL COMMENT '是否是邀请人挖矿 0 否 本人挖矿 1 是 给邀请人挖矿',
  `inviterMobile` varchar(100) DEFAULT NULL COMMENT '被推荐人手机号',
  `inviterName` varchar(100) DEFAULT NULL COMMENT '被推荐人姓名',
  `symbol` varchar(100) DEFAULT NULL COMMENT '交易对符号',
  `type` varchar(100) DEFAULT NULL COMMENT '0市价 1限价',
  `direction` varchar(100) DEFAULT NULL COMMENT '方向 0 买 1卖',
  PRIMARY KEY (`id`),
  KEY `idx_inviter_state` (`inviter_state`),
  KEY `idx_transaction_time` (`transaction_time`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;
