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

CREATE TABLE init_plate (id bigint(20) NOT NULL AUTO_INCREMENT, init_price varchar(20) NOT NULL COMMENT '初始价',final_price varchar(20) DEFAULT NULL COMMENT '最终价',init_time datetime DEFAULT NULL COMMENT '拉盘时间',final_time datetime DEFAULT NULL COMMENT '最终时间',rise_fall_state varchar(255) DEFAULT NULL COMMENT '涨跌状态',symbol varchar(255) DEFAULT NULL COMMENT '交易对',PRIMARY KEY (id)) ENGINE=InnoDB  DEFAULT CHARSET=utf8;


INSERT INTO init_plate(`id`, `init_price`, `final_price`, `init_time`, `final_time`, `rise_fall_state`, `symbol`) VALUES (1, '0.013', '0.05', '2018-08-11 18:57:59', '2018-08-11 23:56:08', '0', 'BHB/USDT');
