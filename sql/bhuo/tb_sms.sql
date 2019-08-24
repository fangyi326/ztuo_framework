/*
Navicat MySQL Data Transfer

Source Server         : 交易
Source Server Version : 50640
Source Host           : 39.104.97.192:3306
Source Database       : bitrade

Target Server Type    : MYSQL
Target Server Version : 50640
File Encoding         : 65001

Date: 2018-07-02 21:18:54
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for tb_sms
-- ----------------------------
DROP TABLE IF EXISTS `tb_sms`;
CREATE TABLE `tb_sms` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `key_id` varchar(32) DEFAULT NULL COMMENT 'keyId',
  `key_secret` varchar(128) DEFAULT NULL COMMENT '秘钥',
  `sign_id` varchar(10) NOT NULL COMMENT '签名id',
  `template_id` varchar(10) NOT NULL COMMENT '模板id',
  `sms_status` varchar(5) NOT NULL COMMENT '状态 ',
  `sms_name` varchar(100) NOT NULL COMMENT '短信介入公司名称',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

INSERT INTO `bitrade`.`tb_sms` (`id`, `key_id`, `key_secret`, `sign_id`, `template_id`, `sms_status`, `sms_name`) VALUES ('1', 'nR1SzkQ0EYVUFBHQ', 'nVLt2c1Q2U3QQGuS7Up6NvpCuiVoNPPp', '4334', '6299', '0', 'chuangrui');

