CREATE TABLE `quiz_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `quiz_currency` VARCHAR (5) NOT NULL COMMENT '竞猜币种',
  `bet_currency` varchar(5) NOT NULL COMMENT '投注币种',
  `bet_amount` DOUBLE(5,3) NOT NULL COMMENT '每注数量',
  `full_quota` DOUBLE(5,2) NOT NULL COMMENT '满额度,例0.85',
  `deadline` varchar(8) NOT NULL COMMENT '投注截止时间 例20:00:00',
  `state` CHAR(1) NOT NULL COMMENT '有效状态0-有效 1-无效',
  `first_prize` DECIMAL(18,8) DEFAULT NULL COMMENT '一等奖数量',
  `second_prize` DECIMAL(8,8) DEFAULT NULL COMMENT '二等奖数量',
  `third_prize` DECIMAL(18,8) DEFAULT NULL COMMENT '三等奖数量',
  `join_prize` DECIMAL(18,8) DEFAULT NULL COMMENT '参与奖',
  `first_amount` INT(5) DEFAULT NULL COMMENT '一等奖人数',
  `second_amount` INT(5) DEFAULT NULL COMMENT '二等奖人数',
  `third_amount` INT(5) DEFAULT NULL COMMENT '三等奖人数',
  `join_amount` INT(5) DEFAULT NULL COMMENT '参与奖人数',
  `create_time` TIMESTAMP NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;


alter TABLE quiz_type add COLUMN join_prize DECIMAL(18,8) DEFAULT NULL COMMENT '参与奖';

alter TABLE quiz_type add COLUMN first_amount INT(5) DEFAULT NULL COMMENT '一等奖人数';

alter TABLE quiz_type add COLUMN second_amount INT(5) DEFAULT NULL COMMENT '二等奖人数';

alter TABLE quiz_type add COLUMN third_amount INT(5) DEFAULT NULL COMMENT '三等奖人数';

alter TABLE quiz_type add COLUMN join_amount INT(5) DEFAULT NULL COMMENT '参与奖人数';


alter TABLE quiz_order add COLUMN quiz_type VARCHAR (10) DEFAULT NULL COMMENT '竞猜类型';

alter TABLE quiz_summary add COLUMN quiz_type VARCHAR (10) DEFAULT NULL COMMENT '竞猜类型';












CREATE TABLE `quiz_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` bigint(20) NOT NULL COMMENT '用户ID',
  `member_name` varchar(255) NOT NULL COMMENT '用户名字',
  `order_no` varchar(32) NOT NULL COMMENT '订单号，格式:时间戳+随机数',
  `period_no` varchar(11) NOT NULL COMMENT '期次（001）',
  `quiz_type_id` BIGINT(20) NOT NULL COMMENT '竞猜类型id',
  `bet_num` int NOT NULL COMMENT '下注数,一次多少注，默认1',
  `bet_time` TIMESTAMP NOT NULL COMMENT '下注时间',
  `winning_level` CHAR(1) DEFAULT NULL COMMENT '中奖等级',
  `winning_num` decimal(18,8) DEFAULT NULL COMMENT '中奖数量',
  `winning_coin` VARCHAR(8) NOT NULL COMMENT '中奖币种',
  `update_time` TIMESTAMP DEFAULT '0000-00-00 00.00.00' COMMENT '更新时间',
  `bet_no` INT(6) DEFAULT NULL COMMENT '下注号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;



 CREATE TABLE `quiz_summary` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `period_no` VARCHAR (11) NOT NULL COMMENT '期次,001',
  `quiz_type_id` BIGINT(20) NOT NULL COMMENT '竞猜类型id',
  `num_for_lottery` VARCHAR(10) DEFAULT NULL COMMENT '体彩期数',
  `lottery_no` INT(5) DEFAULT NULL COMMENT '开奖号码',
  `yield` TIMESTAMP DEFAULT '0000-00-00 00.00.00' COMMENT '开奖时间',
  `bet_max_sum` INT(4) NOT NULL COMMENT '总注数',
  `bet_sum` INT(4) DEFAULT NULL COMMENT '投注总数',
  `bet_member_sum` INT(4) DEFAULT NULL COMMENT '投注人数',
  `create_time` TIMESTAMP NOT NULL COMMENT '创建时间',
  `update_time` TIMESTAMP DEFAULT '0000-00-00 00.00.00' COMMENT '更新时间',
  `item_address` VARCHAR(2048) DEFAULT NULL COMMENT '开奖细则地址',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 ;


