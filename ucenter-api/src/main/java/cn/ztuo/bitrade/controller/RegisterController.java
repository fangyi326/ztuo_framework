package cn.ztuo.bitrade.controller;

import cn.ztuo.bitrade.constant.BooleanEnum;
import cn.ztuo.bitrade.constant.MemberLevelEnum;
import cn.ztuo.bitrade.constant.SysConstant;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.entity.transform.AuthMember;
import cn.ztuo.bitrade.event.MemberEvent;
import cn.ztuo.bitrade.service.CountryService;
import cn.ztuo.bitrade.service.LocaleMessageSourceService;
import cn.ztuo.bitrade.service.MemberService;

import cn.ztuo.bitrade.util.*;
import cn.ztuo.bitrade.utils.TengXunWatherProofUtil;
import cn.ztuo.bitrade.utils.ValidateUtils;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static cn.ztuo.bitrade.constant.SysConstant.*;
import static cn.ztuo.bitrade.util.MessageResult.error;
import static cn.ztuo.bitrade.util.MessageResult.success;
import static org.springframework.util.Assert.hasText;
import static org.springframework.util.Assert.isTrue;
import static org.springframework.util.Assert.notNull;

/**
 * 会员注册
 *
 * @author GuoShuai
 * @date 2017年12月29日
 */
@Controller
@Slf4j
public class RegisterController extends BaseController{

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String from;
    @Value("${bdtop.system.host}")
    private String host;
    @Value("${bdtop.system.name}")
    private String company;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private MemberService memberService;

    @Autowired
    private IdWorkByTwitter idWorkByTwitter;

    @Autowired
    private MemberEvent memberEvent;

    @Autowired
    private CountryService countryService;

    @Autowired
    private LocaleMessageSourceService msService;

    @Resource
    private LocaleMessageSourceService localeMessageSourceService;

    private String userNameFormat = "U%06d";

    /**
     * 注册支持的国家
     *
     * @return
     */
    @RequestMapping(value = "/support/country", method = RequestMethod.POST)
    @ResponseBody
    public MessageResult allCountry() {
        MessageResult result = success();
        List<Country> list = countryService.getAllCountry();
        result.setData(list);
        return result;
    }

    /**
     * 检查用户名是否重复
     *
     * @param username
     * @return
     */
    @RequestMapping(value = "/register/check/username")
    @ResponseBody
    public MessageResult checkUsername(String username) {
        MessageResult result = success();
        if (memberService.usernameIsExist(username)) {
            result.setCode(500);
            result.setMessage(localeMessageSourceService.getMessage("ACTIVATION_FAILS_USERNAME"));
        }
        return result;
    }


    /**
     * 激活邮件
     *
     * @param key
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/register/active")
    @Transactional(rollbackFor = Exception.class)
    public String activate(String key, HttpServletRequest request) throws Exception {
        if (StringUtils.isEmpty(key)) {
            request.setAttribute("result", localeMessageSourceService.getMessage("INVALID_LINK"));
            return "registeredResult";
        }
        Object info = redisUtil.get(key);
        LoginByEmail loginByEmail = null;
        if (info instanceof LoginByEmail) {
            loginByEmail = (LoginByEmail) info;
        }
        if (loginByEmail == null) {
            request.setAttribute("result", localeMessageSourceService.getMessage("INVALID_LINK"));
            return "registeredResult";
        }
        if (memberService.emailIsExist(loginByEmail.getEmail())) {
            request.setAttribute("result", localeMessageSourceService.getMessage("ACTIVATION_FAILS_EMAIL"));
            return "registeredResult";
        } else if (memberService.usernameIsExist(loginByEmail.getUsername())) {
            request.setAttribute("result", localeMessageSourceService.getMessage("ACTIVATION_FAILS_USERNAME"));
            return "registeredResult";
        }
        //删除redis里存的键值
        redisUtil.delete(key);
        redisUtil.delete(loginByEmail.getEmail());
        //不可重复随机数
        String loginNo = String.valueOf(idWorkByTwitter.nextId());
        //盐
        String credentialsSalt = ByteSource.Util.bytes(loginNo).toHex();
        //生成密码
        String password = Md5.md5Digest(loginByEmail.getPassword() + credentialsSalt).toLowerCase();
        Member member = new Member();
        member.setMemberLevel(MemberLevelEnum.GENERAL);
        Location location = new Location();
        location.setCountry(loginByEmail.getCountry());
        member.setLocation(location);
        Country country = new Country();
        country.setZhName(loginByEmail.getCountry());
        member.setCountry(country);
        member.setUsername(loginByEmail.getUsername());
        member.setPassword(password);
        member.setEmail(loginByEmail.getEmail());
        member.setSalt(credentialsSalt);
        member.setKycStatus(0);
        Member member1 = memberService.save(member);
        if (member1 != null) {
            member1.setPromotionCode(String.format(userNameFormat, member1.getId()) + GeneratorUtil.getNonceString(2));
            memberEvent.onRegisterSuccess(member1, loginByEmail.getPromotion());
            request.setAttribute("result", localeMessageSourceService.getMessage("ACTIVATION_SUCCESSFUL"));
        } else {
            request.setAttribute("result", localeMessageSourceService.getMessage("ACTIVATION_FAILS"));
        }
        return "registeredResult";
    }

    /**
     * 邮箱注册
     *
     * @param loginByEmail
     * @param bindingResult
     * @return
     */
    @RequestMapping("/register/email")
    @ResponseBody
    public MessageResult registerByEmail(@Valid LoginByEmail loginByEmail, HttpServletRequest request,BindingResult
            bindingResult) throws Exception{
        MessageResult result = BindingResultUtil.validate(bindingResult);
        if (result != null) {
            return result;
        }
        String ip = getRemoteIp(request);
        //防水验证
//        boolean resultProof = TengXunWatherProofUtil.watherProof(loginByEmail.getTicket(), loginByEmail.getRandStr(), ip);
//        if (!resultProof) {
//            return error("验证码错误");
//        }
        String email = loginByEmail.getEmail();
        isTrue(!memberService.emailIsExist(email), localeMessageSourceService.getMessage("EMAIL_ALREADY_BOUND"));
        isTrue(!memberService.usernameIsExist(loginByEmail.getUsername()), localeMessageSourceService.getMessage("USERNAME_ALREADY_EXISTS"));
        if (redisUtil.get(email) != null) {
            return error(localeMessageSourceService.getMessage("LOGIN_EMAIL_ALREADY_SEND"));
        }
        try {
            log.info("send==================================");
            sentEmail( loginByEmail, email);
            log.info("success===============================");
        } catch (Exception e) {
            e.printStackTrace();
            return error(localeMessageSourceService.getMessage("REGISTRATION_FAILED"));
        }
        return success(localeMessageSourceService.getMessage("SEND_LOGIN_EMAIL_SUCCESS"));
    }

    @Async
    public void sentEmail( LoginByEmail loginByEmail, String email) throws MessagingException, IOException, TemplateException {
        //缓存邮箱和注册信息
        String token = UUID.randomUUID().toString().replace("-", "");
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = null;
        helper = new MimeMessageHelper(mimeMessage, true);
        helper.setFrom(from);
        helper.setTo(email);
        helper.setSubject(company);
        Map<String, Object> model = new HashMap<>(16);
        model.put("username", loginByEmail.getUsername());
        model.put("token", token);
        model.put("host", host);
        model.put("name", company);
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_26);
        cfg.setClassForTemplateLoading(this.getClass(), "/templates");
        Template template = cfg.getTemplate("activateEmail.ftl");
        String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
        helper.setText(html, true);
        //发送邮件
        javaMailSender.send(mimeMessage);
        redisUtil.set(token, loginByEmail, 12, TimeUnit.HOURS);
        redisUtil.set(email, "", 12, TimeUnit.HOURS);
    }

    /**
     * 手机注册
     *
     * @param loginByPhone
     * @param bindingResult
     * @return
     * @throws Exception
     */
    @RequestMapping("/register/phone")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public MessageResult loginByPhone(
            @Valid LoginByPhone loginByPhone,
            BindingResult bindingResult,HttpServletRequest request) throws Exception {
        MessageResult result = BindingResultUtil.validate(bindingResult);
        if (result != null) {
            return result;
        }

        String ip = getRemoteIp(request);
        //防水验证
//        boolean resultProof = TengXunWatherProofUtil.watherProof(loginByPhone.getTicket(), loginByPhone.getRandStr(), ip);
//        if (!resultProof) {
//            return error("验证码验证失败,请重新获取验证码");
//        }
        if (loginByPhone.getCountry().equals("中国")) {
            Assert.isTrue(ValidateUtil.isMobilePhone(loginByPhone.getPhone().trim()), localeMessageSourceService.getMessage("PHONE_EMPTY_OR_INCORRECT"));
        }

        String phone = loginByPhone.getPhone();

        Object code = redisUtil.get(SysConstant.PHONE_REG_CODE_PREFIX + phone);

        isTrue(!memberService.phoneIsExist(phone), localeMessageSourceService.getMessage("PHONE_ALREADY_EXISTS"));
        isTrue(!memberService.usernameIsExist(loginByPhone.getUsername()), localeMessageSourceService.getMessage("USERNAME_ALREADY_EXISTS"));
        notNull(code, localeMessageSourceService.getMessage("VERIFICATION_CODE_NOT_EXISTS"));
        if (!code.toString().equals(loginByPhone.getCode())) {
            return error(localeMessageSourceService.getMessage("VERIFICATION_CODE_INCORRECT"));
        } else {
            redisUtil.delete(SysConstant.PHONE_REG_CODE_PREFIX + phone);
        }
        //不可重复随机数
        String loginNo = String.valueOf(idWorkByTwitter.nextId());
        //盐
        String credentialsSalt = ByteSource.Util.bytes(loginNo).toHex();
        //生成密码
        String password = Md5.md5Digest(loginByPhone.getPassword() + credentialsSalt).toLowerCase();
        Member member = new Member();
        member.setMemberLevel(MemberLevelEnum.GENERAL);
        Location location = new Location();
        location.setCountry(loginByPhone.getCountry());
        Country country = new Country();
        country.setZhName(loginByPhone.getCountry());
        member.setCountry(country);
        member.setLocation(location);
        member.setUsername(loginByPhone.getUsername());
        member.setPassword(password);
        member.setMobilePhone(phone);
        member.setSalt(credentialsSalt);
        member.setKycStatus(0);
        Member member1 = memberService.save(member);
        if (member1 != null) {
            member1.setPromotionCode(String.format(userNameFormat, member1.getId()) + GeneratorUtil.getNonceString(2));
            memberEvent.onRegisterSuccess(member1, loginByPhone.getPromotion());
            return success(localeMessageSourceService.getMessage("REGISTRATION_SUCCESS"));
        } else {
            return error(localeMessageSourceService.getMessage("REGISTRATION_FAILED"));
        }
    }

    /**
     * 发送绑定邮箱验证码
     *
     * @param email
     * @param user
     * @return
     */
    @RequestMapping("/bind/email/code")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public MessageResult sendBindEmail(String email, @SessionAttribute(SESSION_MEMBER) AuthMember user) {
        Assert.isTrue(ValidateUtil.isEmail(email), localeMessageSourceService.getMessage("WRONG_EMAIL"));
        Member member = memberService.findOne(user.getId());
        Assert.isNull(member.getEmail(), localeMessageSourceService.getMessage("BIND_EMAIL_REPEAT"));
        Assert.isTrue(!memberService.emailIsExist(email), localeMessageSourceService.getMessage("EMAIL_ALREADY_BOUND"));
        String code = String.valueOf(GeneratorUtil.getRandomNumber(100000, 999999));
        if (redisUtil.get(EMAIL_BIND_CODE_PREFIX + email) != null) {
            return error(localeMessageSourceService.getMessage("EMAIL_ALREADY_SEND"));
        }
        try {
            sentEmailCode( email, code);
        } catch (Exception e) {
            e.printStackTrace();
            return error(localeMessageSourceService.getMessage("SEND_FAILED"));
        }
        return success(localeMessageSourceService.getMessage("SENT_SUCCESS_TEN"));
    }

    @Async
    public void sentEmailCode(String email, String code) throws MessagingException, IOException, TemplateException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = null;
        helper = new MimeMessageHelper(mimeMessage, true);
        helper.setFrom(from);
        helper.setTo(email);
        helper.setSubject(company);
        Map<String, Object> model = new HashMap<>(16);
        model.put("code", code);
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_26);
        cfg.setClassForTemplateLoading(this.getClass(), "/templates");
        Template template = cfg.getTemplate("bindCodeEmail.ftl");
        String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
        helper.setText(html, true);

        //发送邮件
        javaMailSender.send(mimeMessage);
        log.info("send email for {},content:{}", email, html);
        redisUtil.set(EMAIL_BIND_CODE_PREFIX + email, code, 10, TimeUnit.MINUTES);
    }

    /**
     * 增加提币地址验证码
     *
     * @param user
     * @return
     */
    @RequestMapping("/add/address/code")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public MessageResult sendAddAddress(@SessionAttribute(SESSION_MEMBER) AuthMember user) {
        String code = String.valueOf(GeneratorUtil.getRandomNumber(100000, 999999));
        Member member = memberService.findOne(user.getId());
        String email = member.getEmail();
        if (email == null) {
            return error(localeMessageSourceService.getMessage("NOT_BIND_EMAIL"));
        }
        if (redisUtil.get(ADD_ADDRESS_CODE_PREFIX + email) != null) {
            return error(localeMessageSourceService.getMessage("EMAIL_ALREADY_SEND"));
        }
        try {
            sentEmailAddCode( email, code);
        } catch (Exception e) {
            e.printStackTrace();
            return error(localeMessageSourceService.getMessage("SEND_FAILED"));
        }
        return success(localeMessageSourceService.getMessage("SENT_SUCCESS_TEN"));
    }

    @Async
    public void sentEmailAddCode( String email, String code) throws MessagingException, IOException, TemplateException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = null;
        helper = new MimeMessageHelper(mimeMessage, true);
        helper.setFrom(from);
        helper.setTo(email);
        helper.setSubject(company);
        Map<String, Object> model = new HashMap<>(16);
        model.put("code", code);
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_26);
        cfg.setClassForTemplateLoading(this.getClass(), "/templates");
        Template template = cfg.getTemplate("addAddressCodeEmail.ftl");
        String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
        helper.setText(html, true);
        //发送邮件
        javaMailSender.send(mimeMessage);
        redisUtil.set(ADD_ADDRESS_CODE_PREFIX + email, code, 10, TimeUnit.MINUTES);
    }

    @RequestMapping("/reset/email/code")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public MessageResult sendResetPasswordCode(String account) {
        Member member = memberService.findByEmail(account);
        Assert.notNull(member, localeMessageSourceService.getMessage("MEMBER_NOT_EXISTS"));
        if (redisUtil.get(RESET_PASSWORD_CODE_PREFIX + account) != null) {
            return error(localeMessageSourceService.getMessage("EMAIL_ALREADY_SEND"));
        }
        try {
            String code = String.valueOf(GeneratorUtil.getRandomNumber(100000, 999999));
            sentResetPassword( account, code,RESET_PASSWORD_CODE_PREFIX);
        } catch (Exception e) {
            e.printStackTrace();
            return error(localeMessageSourceService.getMessage("SEND_FAILED"));
        }
        return success(localeMessageSourceService.getMessage("SENT_SUCCESS_TEN"));
    }

    @Async
    public void sentResetPassword(String email, String code,String prefix) throws MessagingException, IOException, TemplateException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper;
        helper = new MimeMessageHelper(mimeMessage, true);
        helper.setFrom(from);
        helper.setTo(email);
        helper.setSubject(company);
        Map<String, Object> model = new HashMap<>(16);
        model.put("code", code);
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_26);
        cfg.setClassForTemplateLoading(this.getClass(), "/templates");
        Template template = cfg.getTemplate("resetPasswordCodeEmail.ftl");
        String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
        helper.setText(html, true);
        //发送邮件
        javaMailSender.send(mimeMessage);
        redisUtil.set(prefix + email, code, 10, TimeUnit.MINUTES);
    }

    /**
     * 忘记密码后重置密码
     *
     * @param mode     0为手机验证，1为邮箱验证
     * @param account  手机或邮箱
     * @param code     验证码
     * @param password 新密码
     * @return
     */
    @RequestMapping(value = "/reset/login/password", method = RequestMethod.POST)
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public MessageResult forgetPassword(int mode, String account, String code, String password) throws Exception {
        isTrue(ValidateUtils.validatePassword(password), msService.getMessage("PASSWORD_LENGTH_ILLEGAL"));
        Member member = null;
        Object redisCode = redisUtil.get(SysConstant.RESET_PASSWORD_CODE_PREFIX + account);
        notNull(redisCode, localeMessageSourceService.getMessage("VERIFICATION_CODE_NOT_EXISTS"));
        if (mode == 0) {
            member = memberService.findByPhone(account);
        } else if (mode == 1) {
            member = memberService.findByEmail(account);
        }
        isTrue(password.length() >= 6 && password.length() <= 20, localeMessageSourceService.getMessage("PASSWORD_LENGTH_ILLEGAL"));
        notNull(member, localeMessageSourceService.getMessage("MEMBER_NOT_EXISTS"));
        if (!code.equals(redisCode.toString())) {
            return error(localeMessageSourceService.getMessage("VERIFICATION_CODE_INCORRECT"));
        } else {
            redisUtil.delete(SysConstant.RESET_PASSWORD_CODE_PREFIX + account);
        }
        //生成密码
        String newPassword = Md5.md5Digest(password + member.getSalt()).toLowerCase();
        member.setPassword(newPassword);
        member.setLoginLock(BooleanEnum.IS_FALSE);
        return success();
    }

    /**
     * 发送解绑旧邮箱验证码
     *
     * @param user
     * @return
     */
    @PostMapping("/untie/email/code")
    @ResponseBody
    public MessageResult untieEmailCode(@SessionAttribute(SESSION_MEMBER) AuthMember user){
        Member member = memberService.findOne(user.getId());
        isTrue(member.getEmail()!=null,msService.getMessage("NOT_BIND_EMAIL"));
        Object cache = redisUtil.get(SysConstant.EMAIL_UNTIE_CODE_PREFIX + member.getEmail());
        if(cache!=null){
            return error(localeMessageSourceService.getMessage("EMAIL_ALREADY_SEND"));
        }
        String code = String.valueOf(GeneratorUtil.getRandomNumber(100000, 999999));
        try {
            sentResetPassword( member.getEmail(), code,EMAIL_UNTIE_CODE_PREFIX);
        } catch (MessagingException | IOException | TemplateException e) {
            e.printStackTrace();
        }
        return success();
    }

    /**
     * 发送新邮箱验证码
     *
     * @param user
     * @return
     */
    @PostMapping("/update/email/code")
    @ResponseBody
    public MessageResult updateEmailCode(@SessionAttribute(SESSION_MEMBER) AuthMember user,String email){
        if(memberService.emailIsExist(email)){
            return MessageResult.error(msService.getMessage("REPEAT_EMAIL_REQUEST"));
        }
        Member member = memberService.findOne(user.getId());
        isTrue(member.getEmail()!=null,msService.getMessage("NOT_BIND_EMAIL"));
        Object cache = redisUtil.get(SysConstant.EMAIL_UPDATE_CODE_PREFIX + email);
        if(cache!=null){
            return error(localeMessageSourceService.getMessage("EMAIL_ALREADY_SEND"));
        }
        String code = String.valueOf(GeneratorUtil.getRandomNumber(100000, 999999));
        try {
            sentResetPassword( email, code,EMAIL_UPDATE_CODE_PREFIX);
        } catch (MessagingException | IOException | TemplateException e) {
            e.printStackTrace();
        }
        return success();
    }


}
