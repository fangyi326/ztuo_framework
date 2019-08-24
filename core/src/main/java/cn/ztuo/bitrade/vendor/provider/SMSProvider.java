package cn.ztuo.bitrade.vendor.provider;


import cn.ztuo.bitrade.util.MessageResult;

public interface SMSProvider {
    /**
     * 发送单条短信
     *
     * @param mobile  手机号
     * @param content 短信内容
     * @return
     * @throws Exception
     */
    MessageResult sendSingleMessage(String mobile, String content) throws Exception;

    /**
     * 发送验证码短信
     *
     * @param mobile     手机号
     * @param verifyCode 验证码
     * @return
     * @throws Exception
     */
    default MessageResult sendVerifyMessage(String mobile, String verifyCode) throws Exception {
        return sendSingleMessage(mobile, formatVerifyCode(verifyCode));
    }

    /**
     * 获取验证码信息格式
     *
     * @param code
     * @return
     */
    default String formatVerifyCode(String code) {
        return String.format("您的验证码为%s，十分钟内有效，如非本人操作，请忽略", code);
    }

    /**
     * 发送国际短信
     *
     * @param content
     * @param phone
     * @return
     */
    default MessageResult sendInternationalMessage(String content, String phone) throws Exception {
        return null;
    }

    default String sendLoginMessage(String ip){
        return String.format("您已经登录，登录IP：%s",ip);
    }

    public MessageResult sendLoginMessage(String ip, String phone) throws Exception;
}
