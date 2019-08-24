ALTER TABLE `bitrade`.`exchange_coin`
ADD COLUMN `zone` int(11) NULL COMMENT '交易区域' AFTER `min_turnover`,
ADD COLUMN `min_volume` decimal(18, 4) NULL COMMENT '最小下单量' AFTER `zone`,
ADD COLUMN `max_volume` decimal(18, 4) NULL COMMENT '最大下单量' AFTER `min_volume`;