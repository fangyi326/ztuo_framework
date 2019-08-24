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
###########删除挖矿表字段 邀请人状态 邀请人手机号 邀请人姓名 返还状态
SET FOREIGN_KEY_CHECKS=0;


ALTER TABLE `exchange_order_transaction_mine`
DROP COLUMN `bouns_state`,
DROP COLUMN `inviter_state`,
DROP COLUMN `inviter_mobile`,
DROP COLUMN `inviter_name`;

##########添加交易明细字段########
ALTER TABLE `member_transaction`
ADD COLUMN `real_fee` varchar(255) NULL COMMENT '实际手续费' ,
ADD COLUMN `discount_fee` varchar(255) default '0' COMMENT '折扣手续费';

update member_transaction set real_fee=fee;
########添加订单中使用折扣字段
ALTER TABLE `exchange_order` ADD COLUMN `use_discount` varchar(1) default '0' COMMENT '折扣手续费';