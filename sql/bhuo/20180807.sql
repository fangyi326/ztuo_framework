CREATE TABLE `wealth_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` bigint(20) NOT NULL COMMENT '用户ID',
  `member_name` varchar(128) DEFAULT NULL COMMENT '用户名字',
  `member_mobile` varchar(32) DEFAULT NULL COMMENT '用户手机号',
  `inviter_id` bigint(20) DEFAULT NULL COMMENT '推荐人ID',
  `member_rate` varchar(255) DEFAULT NULL COMMENT '会员等级',
  `give_bhb` decimal(18,8) DEFAULT NULL COMMENT '赠送BHB',
  `accumulated_mine` decimal(18,8) DEFAULT NULL COMMENT '累计挖矿',
  `release_rate` varchar(8) DEFAULT NULL COMMENT '挖矿释放比例',
  `fee_amount` decimal(18,8) DEFAULT NULL COMMENT '免手续费累计',
  `promotion_time` datetime DEFAULT NULL COMMENT '会员创建时间',
  `over_time` datetime DEFAULT NULL COMMENT '超级合伙人过期时间',
  `release_amount` decimal(18,8) DEFAULT NULL COMMENT '待释放分红',
  `bonus_amount` decimal(18,8) DEFAULT NULL COMMENT '累计分红',
  PRIMARY KEY (`id`),
  KEY `index_member_id` (`member_id`) USING BTREE
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;



CREATE TABLE `bhb_change` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `change_num` decimal(18,8) NOT NULL COMMENT '本次变化量',
  `sum_num` decimal(18,8) NOT NULL COMMENT '当前总量',
  `member_id` bigint(20) DEFAULT NULL COMMENT '人员ID，关联人员表',
  `busi_type` varchar(20) DEFAULT NULL COMMENT '业务类型',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `version` int(9) NOT NULL COMMENT '版本号，唯一',
  `remark` varchar(20) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `INX_VERSION` (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


//公告添加是否置顶
alter table announcement add is_top VARCHAR(10) DEFAULT '1';

alter table sys_help add is_top VARCHAR(10) DEFAULT '1';






CREATE TABLE `financial_order` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ORDER_NO` bigint(20) NOT NULL COMMENT '理财订单号，格式：14位时间戳+6位随机数',
  `MEMBER_ID` bigint(20) NOT NULL COMMENT '人员ID',
  `ITEM_ID` varchar(40) NOT NULL COMMENT '理财项目ID',
  `COIN_NAME` varchar(40) NOT NULL COMMENT '币种名称',
  `CREATE_TIME` datetime NOT NULL COMMENT '定投时间',
  `COIN_NUM` decimal(18,8) NOT NULL COMMENT '定投数量',
  `ORDER_STATE` int(1) NOT NULL COMMENT '订单状态，0-锁仓中（默认），1-已结束',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '订单更新时间',
  `FROZEN_DAYS` int(4) NOT NULL COMMENT '锁仓天数',
  `ORDER_USDT_RATE` double NOT NULL COMMENT '定投时对USDT的汇率',
  `PLAN_REVENUE_TIME` datetime NOT NULL COMMENT '计划收益时间',
  `REAL_INCOME` decimal(18,8) DEFAULT NULL COMMENT '实际收益',
  PRIMARY KEY (`ID`),
  KEY `INX_MEMER_ID` (`MEMBER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;




CREATE TABLE `financial_item` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ITEM_ID` varchar(40) NOT NULL COMMENT '项目ID,后端自动生成',
  `ITEM_NAME` varchar(255) NOT NULL COMMENT '项目名称',
  `ITEM_DESC` varchar(255) DEFAULT NULL COMMENT '项目描述',
  `YIELD` double(5,4) NOT NULL COMMENT '收益率，格式：0.12',
  `DEADLINE` int(4) NOT NULL COMMENT '项目定投期限，单位：天',
  `COIN_MINNUM` decimal(18,8) NOT NULL COMMENT '最少定投数量',
  `COIN_NAME` varchar(40) NOT NULL COMMENT '币种名称',
  `CREATE_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `ITEM_STATE` int(1) NOT NULL COMMENT '状态，0-有效，1-无效',
  PRIMARY KEY (`ID`),
  KEY `INX_TERM_ID` (`ITEM_ID`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;




insert into financial_item (ID,ITEM_ID,ITEM_NAME,ITEM_DESC ,YIELD,DEADLINE,COIN_MINNUM ,COIN_NAME,CREATE_TIME,UPDATE_TIME,ITEM_STATE) values
(1,'BHB001','30天BHB理财','30天BHB理财描述',0.10,30,50000,'BHB',now(),null,0),
(2,'BHB002','60天BHB理财','60天BHB理财描述',0.12,60,100000,'BHB',now(),null,0),
(3,'BHB003','90天BHB理财','90天BHB理财描述',0.14,90,500000,'BHB',now(),null,0);



1.
CREATE TABLE `member_copy` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `ali_no` varchar(255) DEFAULT NULL,
  `qr_code_url` varchar(255) DEFAULT NULL,
  `appeal_success_times` int(11) NOT NULL,
  `appeal_times` int(11) NOT NULL,
  `application_time` datetime DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `bank` varchar(255) DEFAULT NULL,
  `branch` varchar(255) DEFAULT NULL,
  `card_no` varchar(255) DEFAULT NULL,
  `certified_business_apply_time` datetime DEFAULT NULL,
  `certified_business_check_time` datetime DEFAULT NULL,
  `certified_business_status` int(11) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `first_level` int(11) NOT NULL,
  `google_date` datetime DEFAULT NULL,
  `google_key` varchar(255) DEFAULT NULL,
  `google_state` int(11) DEFAULT NULL,
  `id_number` varchar(255) DEFAULT NULL,
  `inviter_id` bigint(20) DEFAULT NULL,
  `jy_password` varchar(255) DEFAULT NULL,
  `last_login_time` datetime DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `district` varchar(255) DEFAULT NULL,
  `province` varchar(255) DEFAULT NULL,
  `login_count` int(11) NOT NULL,
  `margin` varchar(255) DEFAULT NULL,
  `member_level` int(11) DEFAULT NULL,
  `mobile_phone` varchar(255) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `promotion_code` varchar(255) DEFAULT NULL,
  `publish_advertise` int(11) DEFAULT NULL,
  `real_name` varchar(255) DEFAULT NULL,
  `real_name_status` int(11) DEFAULT NULL,
  `registration_time` datetime DEFAULT NULL,
  `salt` varchar(255) DEFAULT NULL,
  `second_level` int(11) NOT NULL,
  `sign_in_ability` bit(1) NOT NULL DEFAULT b'1',
  `status` int(11) DEFAULT NULL,
  `super_partner` varchar(255) DEFAULT NULL,
  `third_level` int(11) NOT NULL,
  `token` varchar(255) DEFAULT NULL,
  `token_expire_time` datetime DEFAULT NULL,
  `transaction_status` int(11) DEFAULT NULL,
  `transactions` int(11) NOT NULL,
  `username` varchar(255) NOT NULL,
  `qr_we_code_url` varchar(255) DEFAULT NULL,
  `wechat` varchar(255) DEFAULT NULL,
  `local` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_gc3jmn7c2abyo3wf6syln5t2i` (`username`),
  UNIQUE KEY `UK_mbmcqelty0fbrvxp1q58dn57t` (`email`),
  UNIQUE KEY `UK_10ixebfiyeqolglpuye0qb49u` (`mobile_phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

2.INSERT INTO member_copy SELECT * FROM member;

select count(1) from member_copy;

==========SET FOREIGN_KEY_CHECKS = 0;

3.TRUNCATE TABLE member;

4.INSERT INTO member SELECT * from member_copy where id_number is NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;

select count(1) from member;




1.
CREATE TABLE `member_wallet_copy` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `address` varchar(255) DEFAULT NULL,
  `balance` decimal(26,16) DEFAULT NULL,
  `frozen_balance` decimal(26,16) DEFAULT NULL,
  `is_lock` int(11) DEFAULT '0' COMMENT '钱包不是锁定',
  `member_id` bigint(20) DEFAULT NULL,
  `version` int(11) NOT NULL,
  `coin_id` varchar(255) DEFAULT NULL,
  `to_released` decimal(18,8) DEFAULT NULL COMMENT '待释放总量',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKm68bscpof0bpnxocxl4qdnvbe` (`member_id`,`coin_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


2.
INSERT INTO member_wallet_copy SELECT * FROM member_wallet;

3
CREATE TABLE `member_wallet_copy_1` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `address` varchar(255) DEFAULT NULL,
  `balance` decimal(26,16) DEFAULT NULL,
  `frozen_balance` decimal(26,16) DEFAULT NULL,
  `is_lock` int(11) DEFAULT '0' COMMENT '钱包不是锁定',
  `member_id` bigint(20) DEFAULT NULL,
  `version` int(11) NOT NULL,
  `coin_id` varchar(255) DEFAULT NULL,
  `to_released` decimal(18,8) DEFAULT NULL COMMENT '待释放总量',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKm68bscpof0bpnxocxl4qdnvbe` (`member_id`,`coin_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

====================================
4.
INSERT INTO member_wallet_copy_1 SELECT a.* FROM member_wallet a RIGHT JOIN member b on a.member_id=b.id;

5.减去BHB；

6.
TRUNCATE TABLE member_wallet;

7.
INSERT INTO member_wallet SELECT * from member_wallet_copy_1;

8.
select count(1) from member_wallet group by member_id;

117870000
18477090.7
3039780

139386870.7

INSERT INTO `bhb_change`(`change_num`, `sum_num`, `member_id`, `busi_type`, `create_time`, `version`, `remark`) VALUES ( 139386870.7, 139386870.7, 0, '初始化', '2018-08-7 20:23:45', 1, '初始化');



53700743.4538706836021964 - 35223652.81662956

3039780.00000000


66946,65859,13029,55



70136,70138,70139,70137

SELECT id,count(id) counts from member WHERE id_number is NULL GROUP BY inviter_id ORDER BY counts desc limit 0,10;


SELECT id,count(id) counts from member WHERE id_number is NULL GROUP BY inviter_id ORDER BY counts desc limit 301,100;


 SELECT id,count(id) counts from member WHERE id_number is NULL GROUP BY inviter_id;


 update member_wallet set to_released=to_released-counts*60 where member_id =id AND coin_id='BHB';



