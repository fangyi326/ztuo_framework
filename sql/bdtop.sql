

DROP TABLE IF EXISTS `member_grade`;
CREATE TABLE `member_grade` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `grade_name` varchar(255) DEFAULT NULL,
  `grade_code` varchar(255) DEFAULT NULL,
  `withdraw_coin_amount` decimal(18,6) DEFAULT NULL COMMENT '提币数量',
  `day_withdraw_count` int(11) DEFAULT NULL,
  `grade_bound` int(11) DEFAULT NULL,
  `exchange_fee_rate` decimal(10,6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `bitrade`.`member`
ADD COLUMN `integration` bigint(20) NOT NULL COMMENT '会员积分' AFTER `local`;

ALTER TABLE `bitrade`.`member`
ADD COLUMN `member_grade_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '等级id' AFTER `integration`;

ALTER TABLE `bitrade`.`exchange_order`
ADD COLUMN `trigger_price` decimal(18, 8) NULL AFTER `type`;


DROP TABLE IF EXISTS `integration_record`;
CREATE TABLE `integration_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` bigint(20) NOT NULL,
  `type` int(11) DEFAULT NULL,
  `amount` bigint(20) NOT NULL COMMENT '积分赠送数量',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `bitrade`.`member_wallet`
ADD COLUMN `release_balance` decimal(18, 8) NULL COMMENT '待释放余额' AFTER `frozen_balance`;

ALTER TABLE `bitrade`.`exchange_order`
ADD COLUMN `member_trade` int(11) NULL DEFAULT 1 AFTER `margin_trade`;

DROP TABLE IF EXISTS `robot_transaction`;
CREATE TABLE `robot_transaction` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `amount` decimal(18,8) DEFAULT NULL COMMENT '充币金额',
  `create_time` datetime DEFAULT NULL,
  `fee` decimal(19,8) DEFAULT NULL,
  `member_id` bigint(20) DEFAULT NULL,
  `symbol` varchar(255) DEFAULT NULL,
  `type` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

ALTER TABLE `bitrade`.`sys_help`
ADD COLUMN `is_top` varchar(1) NULL DEFAULT 1 COMMENT '是否置顶默认非置顶' AFTER `title`;

DROP TABLE IF EXISTS `member_api_key`;
CREATE TABLE `member_api_key` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` bigint(20) NOT NULL ,
  `api_key` varchar(255) NOT NULL,
  `secret_key`  varchar(255) NOT NULL,
  `bind_ip`  varchar(1024) default NULL,
  `create_time` datetime DEFAULT NULL,
  `api_name`  varchar(255) NOT NULL,
  `remark`  varchar(255) NOT NULL,
  `expire_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `bitrade`.`exchange_order`
DROP COLUMN `member_trade`;

ALTER TABLE `bitrade`.`exchange_order`
ADD COLUMN `order_resource` int(11) NULL DEFAULT 1 AFTER `trigger_price`;

ALTER TABLE `bitrade`.`lever_wallet`
ADD COLUMN `mobile_phone` varchar(255) NULL AFTER `lever_coin_id`,
ADD COLUMN `email` varchar(255) NULL AFTER `mobile_phone`;