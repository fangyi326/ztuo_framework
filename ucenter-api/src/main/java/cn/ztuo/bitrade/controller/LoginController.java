package cn.ztuo.bitrade.controller;

import cn.ztuo.bitrade.constant.SysConstant;
import cn.ztuo.bitrade.entity.LoginInfo;
import cn.ztuo.bitrade.entity.Member;
import cn.ztuo.bitrade.entity.Sign;
import cn.ztuo.bitrade.entity.transform.AuthMember;
import cn.ztuo.bitrade.event.MemberEvent;
import cn.ztuo.bitrade.exception.AuthenticationException;
import cn.ztuo.bitrade.service.LocaleMessageSourceService;
import cn.ztuo.bitrade.service.MemberService;
import cn.ztuo.bitrade.service.SignService;
import cn.ztuo.bitrade.system.GeetestLib;
import cn.ztuo.bitrade.util.GoogleAuthenticatorUtil;
import cn.ztuo.bitrade.util.MessageResult;
import cn.ztuo.bitrade.util.RedisUtil;
import cn.ztuo.bitrade.util.RequestUtil;
import cn.ztuo.bitrade.vendor.provider.SMSProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static cn.ztuo.bitrade.constant.SysConstant.SESSION_MEMBER;

/**
 * @author GuoShuai
 * @date 2018年01月10日
 */
@RestController
@Slf4j
public class LoginController extends BaseController {

    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberEvent memberEvent;
    @Autowired
    private LocaleMessageSourceService messageSourceService;
    @Autowired
    private LocaleMessageSourceService msService;
    @Autowired
    private GeetestLib gtSdk;
    @Autowired
    private SignService signService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private SMSProvider smsProvider;

    @Value("${person.promote.prefix:}")
    private String promotePrefix;
    @Value("${system.ip138.api:}")
    private String ip138ApiUrl;
    @Value("${system.ip138.key:}")
    private String ip138Key;
    @Value("${system.ip138.value:}")
    private String ip138Value;
    @Value("${sms.driver}")
    private String driverName;
    @Value("${system.login.sms:0}")
    private Integer loginSms;

    @RequestMapping(value = "login")
    @Transactional(rollbackFor = Exception.class)
    public MessageResult login(HttpServletRequest request, @RequestParam("username") String username,
                               @RequestParam("password") String password
            , @RequestParam(value = "code", required = false) String code
            , @RequestParam(value = "ticket", required = false) String ticket
            , @RequestParam(value = "randStr", required = false) String randStr) throws Exception {
        Assert.hasText(username, messageSourceService.getMessage("MISSING_USERNAME"));
        Assert.hasText(password, messageSourceService.getMessage("MISSING_PASSWORD"));
        String ip = getRemoteIp(request);
        Member member = memberService.findByPhoneOrEmail(username);
        String host = RequestUtil.remoteIp(request);
        log.info("host:" + host);

        //防水验证
//        boolean result = TengXunWatherProofUtil.watherProof(ticket, randStr, ip);
//        if (!result) {
//            return error("验证失败");
//        }

        if (member == null){
            return error("用户不存在");
        }
        if (member.getGoogleState() == 1) {
            //谷歌验证
            if (StringUtils.isNotEmpty(code)) {
                long googleCode = Long.parseLong(code);
                long t = System.currentTimeMillis();
                GoogleAuthenticatorUtil ga = new GoogleAuthenticatorUtil();
                //  ga.setWindowSize(0); // should give 5 * 30 seconds of grace...
                boolean r = ga.check_code(member.getGoogleKey(), googleCode, t);
                if (!r) {
                    return MessageResult.error("谷歌验证失败");
                }

            }else {
                return MessageResult.error("请输入谷歌验证码");
            }
        }
        //从session中获取userid
        String userid = (String) request.getSession().getAttribute("userid");
        //自定义参数,可选择添加
        HashMap<String, String> param = new HashMap<String, String>();
        param.put("user_id", userid); //网站用户id
        param.put("client_type", "web"); //web:电脑上的浏览器；h5:手机上的浏览器，包括移动应用内完全内置的web_view；native：通过原生SDK植入APP应用的方式
        param.put("ip_address", ip); //传输用户请求验证时所携带的IP

        try {
            LoginInfo loginInfo = getLoginInfo(username, password, ip, request);

            return success(loginInfo);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }


    private LoginInfo getLoginInfo(String username, String password, String ip, HttpServletRequest request) throws
            Exception {
        Member member = memberService.login(username, password);
        if (member == null) {
            String key = SysConstant.LOGIN_LOCK + username;
            Object code = redisUtil.get(key);
            if (code == null) {
                code = 0;
            }
            Integer codeNum = (Integer) code;
            codeNum++;
            if (codeNum < 10) {
                redisUtil.set(key, codeNum, 3, TimeUnit.MINUTES);
            } else {
                memberService.lock(username);
            }
            throw new AuthenticationException("账号或密码错误");
        }
        memberEvent.onLoginSuccess(member, ip);
        request.getSession().setAttribute(SysConstant.SESSION_MEMBER, AuthMember.toAuthMember(member));
        String token = request.getHeader("access-auth-token");
        if (!StringUtils.isBlank(token) && token.equals(request.getSession().getId())) {
            member.setToken(token);
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR_OF_DAY, 24 * 7);
            member.setTokenExpireTime(calendar.getTime());
        }else {
            member.setToken(request.getSession().getId());
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR_OF_DAY, 24 * 7);
            member.setTokenExpireTime(calendar.getTime());
        }
        // 签到活动是否进行
        Sign sign = signService.fetchUnderway();
        LoginInfo loginInfo;
        if (sign == null) {
            loginInfo = LoginInfo.getLoginInfo(member, request.getSession().getId(), false, promotePrefix);
        } else {
            loginInfo = LoginInfo.getLoginInfo(member, request.getSession().getId(), true, promotePrefix);
        }

        if (loginSms == 1 && member.getMobilePhone() != null) {
            String phone = member.getMobilePhone();
            //253国际短信，可以发国内号码，都要加上区域号
            if (driverName.equalsIgnoreCase("two_five_three")) {
                smsProvider.sendLoginMessage(ip, member.getCountry().getAreaCode() + phone);
            } else {
                if (member.getCountry().getAreaCode().equals("86")) {
                    smsProvider.sendLoginMessage(ip, phone);
                } else {
                    smsProvider.sendLoginMessage(ip, member.getCountry().getAreaCode() + phone);
                }
            }
        }
        return loginInfo;
    }



    /**
     * 登出
     *
     * @return
     */
    @RequestMapping(value = "/loginout")
    @Transactional(rollbackFor = Exception.class)
    public MessageResult loginOut2(HttpServletRequest request, @SessionAttribute(SESSION_MEMBER) AuthMember user) {
        MessageResult messageResult = new MessageResult();
        log.info(">>>>>退出登陆接口开始>>>>>");
        try {
            request.getSession().removeAttribute(SysConstant.SESSION_MEMBER);
            Member member = memberService.findOne(user.getId());
            member.setToken(null);
            messageResult= request.getSession().getAttribute(SysConstant.SESSION_MEMBER) != null ? error(messageSourceService.getMessage("LOGOUT_FAILED")) : success(messageSourceService.getMessage("LOGOUT_SUCCESS"));
        } catch (Exception e) {
            e.printStackTrace();
            log.info(">>>>>登出失败>>>>>"+e);
        }
        log.info(">>>>>退出登陆接口结束>>>>>");
        return messageResult;
    }


    /**
     * 检查是否登录
     *
     * @param request
     * @return
     */
    @RequestMapping("/check/login")
    public MessageResult checkLogin(HttpServletRequest request) {
        AuthMember authMember = (AuthMember) request.getSession().getAttribute(SESSION_MEMBER);
        MessageResult result = MessageResult.success();
        if (authMember != null) {
            result.setData(true);
        } else {
            result.setData(false);
        }
        return result;
    }


    /**
     * 获取用户状态
     *
     * @param mobile
     * @return
     * @throws Exception
     */

    @RequestMapping("/get/user")
    public MessageResult checkLogin(@RequestParam("mobile") String mobile) throws Exception {
        Member member = memberService.findByPhone(mobile);
        if (member != null) {
            return success(member.getGoogleState());
        }
        Member emailMember = memberService.findByEmail(mobile);
        if (emailMember != null){
            return success(emailMember.getGoogleState());
        }
        return error("用户名错误");
    }
}
