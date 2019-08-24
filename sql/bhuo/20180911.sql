
CREATE TABLE `quiz_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` bigint(20) NOT NULL COMMENT '用户ID',
  `member_name` varchar(255) NOT NULL COMMENT '用户名字',
  `order_no` varchar(32) NOT NULL COMMENT '订单号，格式:时间戳+随机数',
  `period_no` varchar(11) NOT NULL COMMENT '期次（001）',
  `quiz_type_id` bigint(20) NOT NULL COMMENT '竞猜类型id',
  `bet_num` int(11) NOT NULL COMMENT '下注数,一次多少注，默认1',
  `bet_time` datetime(3) DEFAULT NULL COMMENT '下注时间',
  `winning_level` char(1) DEFAULT NULL COMMENT '中奖等级',
  `winning_num` decimal(18,8) DEFAULT NULL COMMENT '中奖数量',
  `winning_coin` varchar(8) DEFAULT NULL COMMENT '中奖币种',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `bet_no` int(6) DEFAULT NULL COMMENT '下注号',
  `or_grand` varchar(2) DEFAULT '0' COMMENT '奖励是否发放',
  `quiz_type` varchar(32) DEFAULT NULL COMMENT '竞猜类型',
  `bet_amount` varchar(5) DEFAULT NULL COMMENT '每注数量',
  `lottery_time` datetime DEFAULT NULL COMMENT '开奖时间',
  PRIMARY KEY (`id`),
  KEY `inx_quiz_type_id` (`quiz_type_id`) USING BTREE COMMENT '竞猜类型Id',
  KEY `inx_period_no` (`period_no`) USING BTREE COMMENT '期次索引'
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for quiz_summary
-- ----------------------------
DROP TABLE IF EXISTS `quiz_summary`;
CREATE TABLE `quiz_summary` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `period_no` varchar(11) NOT NULL COMMENT '期次,001',
  `quiz_type_id` bigint(20) NOT NULL COMMENT '竞猜类型id',
  `lottery_no` varchar(5) DEFAULT NULL COMMENT '开奖号码',
  `lottery_time` datetime DEFAULT '0000-00-00 00:00:00' COMMENT '开奖时间',
  `bet_max_sum` int(4) NOT NULL COMMENT '总注数',
  `bet_sum` int(4) DEFAULT NULL COMMENT '投注总数',
  `bet_member_sum` int(4) DEFAULT NULL COMMENT '投注人数',
  `create_time` datetime NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `item_address` varchar(2048) DEFAULT NULL COMMENT '开奖细则地址',
  `num_for_lottery` varchar(10) DEFAULT NULL COMMENT '体彩期数',
  `quiz_type` varchar(32) DEFAULT NULL COMMENT '竞猜类型',
  `bet_amount` varchar(5) DEFAULT NULL COMMENT '每注数量',
  `join_price` decimal(18,5) DEFAULT NULL COMMENT '参与奖每注数量',
  `quiz_coin_instant_price` decimal(18,5) DEFAULT NULL COMMENT '该竞猜类型的瞬时价',
  `bonus_coin_instant_price` decimal(18,5) DEFAULT NULL COMMENT '阳光普照奖励瞬时价',
  `period_state` char(1) DEFAULT NULL COMMENT '期次状态0-未开奖1-待发奖2-已发奖3-已归集',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for quiz_type
-- ----------------------------
DROP TABLE IF EXISTS `quiz_type`;
CREATE TABLE `quiz_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `quiz_currency` varchar(5) NOT NULL COMMENT '竞猜币种',
  `bet_currency` varchar(5) NOT NULL COMMENT '投注币种',
  `bet_amount` double(5,3) NOT NULL COMMENT '用户手机号',
  `full_quota` double(5,2) NOT NULL COMMENT '满额度,例0.85',
  `deadline` varchar(8) NOT NULL COMMENT '投注截止时间 例20:00:00',
  `state` char(1) NOT NULL COMMENT '有效状态0-有效 1-无效',
  `first_prize` decimal(18,8) DEFAULT NULL COMMENT '一等奖数量',
  `second_prize` decimal(8,8) DEFAULT NULL COMMENT '二等奖数量',
  `third_prize` decimal(18,8) DEFAULT NULL COMMENT '三等奖数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `first_amount` int(5) DEFAULT NULL COMMENT '一等奖人数',
  `second_amount` int(5) DEFAULT NULL COMMENT '二等奖人数',
  `third_amount` int(5) DEFAULT NULL COMMENT '三等奖人数',
  `poundage_fee` double(5,3) DEFAULT NULL COMMENT '手续费',
  `premium_rate` double(5,3) DEFAULT NULL COMMENT '溢价率',
  `giving_bhb_rate` double(5,3) DEFAULT NULL COMMENT '赠送BHB比例',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

INSERT INTO `quiz_type`(`id`, `quiz_currency`, `bet_currency`, `bet_amount`, `full_quota`, `deadline`, `state`, `first_prize`, `second_prize`, `third_prize`, `create_time`, `first_amount`, `second_amount`, `third_amount`, `poundage_fee`, `premium_rate`, `giving_bhb_rate`) VALUES (1, 'BTC', 'USDT', 10.000, 0.85, '20:00:00', '0', 0.50000000, 0.02500000, 0.01250000, '2018-09-10 15:24:21', 1, 10, 20, 0.100, 0.200, 1.000);
INSERT INTO `quiz_type`(`id`, `quiz_currency`, `bet_currency`, `bet_amount`, `full_quota`, `deadline`, `state`, `first_prize`, `second_prize`, `third_prize`, `create_time`, `first_amount`, `second_amount`, `third_amount`, `poundage_fee`, `premium_rate`, `giving_bhb_rate`) VALUES (2, 'ETH', 'USDT', 1.000, 0.85, '20:00:00', '0', 1.00000000, 0.02500000, 0.01250000, '2018-09-10 15:18:47', 1, 0, 0, 0.100, 0.200, 1.000);
