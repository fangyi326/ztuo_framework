
alter table exchange_order MODIFY traded_amount DECIMAL(26,16);

alter table exchange_order MODIFY turnover DECIMAL(26,16);


alter table member_wallet MODIFY balance DECIMAL(26,16);


alter table member_wallet MODIFY frozen_balance DECIMAL(26,16);



alter table member_wallet_history MODIFY before_balance DECIMAL(26,16);

alter table member_wallet_history MODIFY after_balance DECIMAL(26,16);

alter table member_wallet_history MODIFY before_frozen_balance DECIMAL(26,16);

alter table member_wallet_history MODIFY after_frozen_balance DECIMAL(26,16);


alter table member_transaction MODIFY amount DECIMAL(26,16);

alter table member_transaction MODIFY fee DECIMAL(26,16);




