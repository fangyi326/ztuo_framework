package cn.ztuo.bitrade.controller;


import cn.ztuo.bitrade.core.DataException;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import cn.ztuo.bitrade.coin.CoinExchangeFactory;
import cn.ztuo.bitrade.constant.*;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.entity.transform.*;
import cn.ztuo.bitrade.exception.InformationExpiredException;
import cn.ztuo.bitrade.model.screen.AdvertiseScreen;
import cn.ztuo.bitrade.service.*;
import cn.ztuo.bitrade.util.BigDecimalUtils;
import cn.ztuo.bitrade.util.BindingResultUtil;
import cn.ztuo.bitrade.util.Md5;
import cn.ztuo.bitrade.util.MessageResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

import static cn.ztuo.bitrade.constant.PayMode.*;
import static cn.ztuo.bitrade.constant.SysConstant.SESSION_MEMBER;
import static cn.ztuo.bitrade.util.BigDecimalUtils.compare;
import static org.springframework.util.Assert.isTrue;


/**
 * @author GuoShuai
 * @date 2017年12月08日
 */
@RestController
@RequestMapping("/advertise")
@Slf4j
public class AdvertiseController extends BaseController {

    @Autowired
    private AdvertiseService advertiseService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private OtcCoinService otcCoinService;
    @Autowired
    private OtcWalletService otcWalletService;
    @Autowired
    private CoinExchangeFactory coins;
    @Autowired
    private LocaleMessageSourceService msService;
    @Autowired
    private CountryService countryService;
    @Value("${bdtop.system.advertise:0}")
    private int allow;


    /**
     * 创建广告
     *
     * @param advertise 广告{@link Advertise}
     * @return
     */
    @RequestMapping(value = "create")
    @Transactional(rollbackFor = Exception.class)
    public MessageResult create(@Valid Advertise advertise, BindingResult bindingResult,
                                @SessionAttribute(SESSION_MEMBER) AuthMember member,
                                @RequestParam(value = "pay[]") String[] pay, String jyPassword) throws Exception {
        MessageResult result = BindingResultUtil.validate(bindingResult);
        if (result != null) {
            return result;
        }
        if(advertise.getMaxLimit()!=null&&advertise.getMinLimit()!=null
                &&advertise.getMinLimit().compareTo(advertise.getMaxLimit())>0){
            return MessageResult.error(msService.getMessage("MIN_LIMIT_TOO_LARGE"));
        }
        Assert.notEmpty(pay, msService.getMessage("MISSING_PAY"));
        Assert.hasText(jyPassword, msService.getMessage("MISSING_JYPASSWORD"));
        Member member1 = memberService.findOne(member.getId());
        if(member1.getTransactionStatus().equals(BooleanEnum.IS_FALSE)){
            return MessageResult.error(500,msService.getMessage("CANNOT_TRADE"));
        }
        if(member1.getPublishAdvertise().equals(BooleanEnum.IS_FALSE)){
            return MessageResult.error(msService.getMessage("CANNOT_PUBLISH_ADVERTISE"));
        }
        if(advertise.getPriceType()==PriceType.REGULAR&&advertise.getPrice()==null){
            return MessageResult.error(500,msService.getMessage("NEET_PRICE"));
        }
        Assert.isTrue(member1.getIdNumber() != null, msService.getMessage("NO_REALNAME"));
        if (allow == 1) {
            //allow是1的时候，必须是认证商家才能发布广告
            Assert.isTrue(member1.getMemberLevel().equals(MemberLevelEnum.IDENTIFICATION), msService.getMessage("NO_BUSINESS"));
        }
        String mbPassword = member1.getJyPassword();
        Assert.hasText(mbPassword, msService.getMessage("NO_SET_JYPASSWORD"));
        Assert.isTrue(Md5.md5Digest(jyPassword + member1.getSalt()).toLowerCase().equals(mbPassword), msService.getMessage("ERROR_JYPASSWORD"));
        AdvertiseType advertiseType = advertise.getAdvertiseType();
        StringBuffer payMode = checkPayMode(pay, advertiseType, member1);
        advertise.setPayMode(payMode.toString());
        OtcCoin otcCoin = otcCoinService.findOne(advertise.getCoin().getId());
        OtcWallet memberWallet=otcWalletService.findByOtcCoinAndMemberId(member.getId(),otcCoin);
        if (memberWallet == null){
            return error("请先划转法币账户");
        }
        if(memberWallet.getIsLock() == 1){
            return MessageResult.error("钱包已被锁定");
        }
        checkAmount(advertiseType, advertise, otcCoin, member1);
        advertise.setLevel(AdvertiseLevel.ORDINARY);
        advertise.setRemainAmount(advertise.getNumber());
        Member mb = new Member();
        mb.setId(member.getId());
        advertise.setMember(mb);
        Advertise ad = advertiseService.saveAdvertise(advertise);
        if (ad != null) {
            return MessageResult.success(msService.getMessage("CREATE_SUCCESS"));
        } else {
            return MessageResult.error(msService.getMessage("CREATE_FAILED"));
        }
    }

    /**
     * 个人所有广告
     *
     * @param shiroUser
     * @return
     */
    @RequestMapping(value = "all")
    public MessageResult allNormal(
            PageModel pageModel,
            @SessionAttribute(SESSION_MEMBER) AuthMember shiroUser, HttpServletRequest request) {
        BooleanExpression eq = QAdvertise.advertise.member.id.eq(shiroUser.getId()).
                and(QAdvertise.advertise.status.ne(AdvertiseControlStatus.TURNOFF));
        ;
        if (request.getParameter("status") != null) {
            eq.and(QAdvertise.advertise.status.eq(AdvertiseControlStatus.valueOf(request.getParameter("status"))));
        }
        Page<Advertise> all = advertiseService.findAll(eq, pageModel.getPageable());
        return success(all);
    }

    /**
     * 个人所有广告
     *
     * @param
     * @return
     */
    @RequestMapping(value = "self/all")
    public MessageResult self(
            AdvertiseScreen screen,
            PageModel pageModel,
            @SessionAttribute(SESSION_MEMBER) AuthMember shiroUser) {
        //添加 指定用户条件
        Predicate predicate = screen.getPredicate(QAdvertise.advertise.member.id.eq(shiroUser.getId()));
        Page<Advertise> all = advertiseService.findAll(predicate, pageModel.getPageable());
        return success(all);
    }

    /**
     * 广告详情
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "detail")
    public MessageResult detail(Long id, @SessionAttribute(SESSION_MEMBER) AuthMember shiroUser) {
        MemberAdvertiseDetail advertise = advertiseService.findOne(id, shiroUser.getId());
        advertise.setMarketPrice(coins.getLegalCurrencyRate(advertise.getCountry().getLocalCurrency(), advertise.getCoinUnit()));
        MessageResult result = MessageResult.success();
        result.setData(advertise);
        return result;
    }

    /**
     * 修改广告
     *
     * @param advertise 广告{@link Advertise}
     * @return {@link MessageResult}
     */
    @RequestMapping(value = "update")
    @Transactional(rollbackFor = Exception.class)
    public MessageResult update(
            @Valid Advertise advertise,
            BindingResult bindingResult,
            @SessionAttribute(SESSION_MEMBER) AuthMember shiroUser,
            @RequestParam(value = "pay[]") String[] pay, String jyPassword) throws Exception {
        MessageResult result = BindingResultUtil.validate(bindingResult);
        if (result != null) return result;

        if(advertise.getMaxLimit()!=null&&advertise.getMinLimit()!=null
                &&advertise.getMinLimit().compareTo(advertise.getMaxLimit())>0){
            return MessageResult.error(msService.getMessage("MIN_LIMIT_TOO_LARGE"));
        }
        Assert.notEmpty(pay, msService.getMessage("MISSING_PAY"));
        Assert.notNull(advertise.getId(), msService.getMessage("UPDATE_FAILED"));
        Assert.hasText(jyPassword, msService.getMessage("MISSING_JYPASSWORD"));

        Member member = memberService.findOne(shiroUser.getId());

        Assert.isTrue(Md5.md5Digest(jyPassword + member.getSalt()).toLowerCase().equals(member.getJyPassword()), msService.getMessage("ERROR_JYPASSWORD"));
        if(advertise.getPriceType()==PriceType.REGULAR&&advertise.getPrice()==null){
            return MessageResult.error(500,msService.getMessage("NEET_PRICE"));
        }
        AdvertiseType advertiseType = advertise.getAdvertiseType();

        StringBuffer payMode = checkPayMode(pay, advertiseType, member);

        advertise.setPayMode(payMode.toString());
        Advertise old = advertiseService.findOne(advertise.getId());
        Assert.notNull(old, msService.getMessage("UPDATE_FAILED"));
        Assert.isTrue(old.getStatus().equals(AdvertiseControlStatus.PUT_OFF_SHELVES), msService.getMessage("AFTER_OFF_SHELVES"));
        OtcCoin otcCoin = otcCoinService.findOne(old.getCoin().getId());
        checkAmount(old.getAdvertiseType(), advertise, otcCoin, member);
        Country country = countryService.findOne(advertise.getCountry().getZhName());
        old.setCountry(country);
        Advertise ad = advertiseService.modifyAdvertise(advertise, old);
        if (ad != null) {
            return MessageResult.success(msService.getMessage("UPDATE_SUCCESS"));
        } else {
            return MessageResult.error(msService.getMessage("UPDATE_FAILED"));
        }
    }


    /**
     * 广告上架
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/on/shelves")
    @Transactional(rollbackFor = Exception.class)
    public MessageResult putOnShelves(long id, @SessionAttribute(SESSION_MEMBER) AuthMember authMember) throws InformationExpiredException {
        Member member=memberService.findOne(authMember.getId());
        if(member.getTransactionStatus().equals(BooleanEnum.IS_FALSE)){
            return MessageResult.error(500,msService.getMessage("CANNOT_TRADE"));
        }
        if(member.getPublishAdvertise().equals(BooleanEnum.IS_FALSE)){
            return MessageResult.error(msService.getMessage("CANNOT_PUBLISH_ADVERTISE"));
        }
        if (allow == 1) {
            //allow是1的时候，必须是认证商家才能上架广告
            Assert.isTrue(member.getMemberLevel().equals(MemberLevelEnum.IDENTIFICATION), msService.getMessage("NO_BUSINESS"));
        }
        Advertise advertise = advertiseService.find(id, authMember.getId());
        if(advertise.getMaxLimit()!=null&&advertise.getMinLimit()!=null
                &&advertise.getMinLimit().compareTo(advertise.getMaxLimit())>0){
            return MessageResult.error(msService.getMessage("MIN_LIMIT_TOO_LARGE"));
        }
        Assert.isTrue(advertise != null, msService.getMessage("PUT_ON_SHELVES_FAILED"));
        Assert.isTrue(advertise.getStatus().equals(AdvertiseControlStatus.PUT_OFF_SHELVES), msService.getMessage("PUT_ON_SHELVES_FAILED"));
        OtcCoin otcCoin = advertise.getCoin();
        OtcWallet memberWallet = otcWalletService.findByOtcCoinAndMemberId(authMember.getId(),otcCoin);
        if(memberWallet.getIsLock() == 1){
            return MessageResult.error("钱包已被锁定");
        }
        if (advertise.getAdvertiseType().equals(AdvertiseType.SELL)) {
            isTrue(memberWallet.getIsLock() == 0,msService.getMessage("WALLET_IS_LOCK"));
            Assert.isTrue(compare(memberWallet.getBalance(), advertise.getNumber()), msService.getMessage("INSUFFICIENT_BALANCE"));
            Assert.isTrue(advertise.getNumber().compareTo(otcCoin.getSellMinAmount()) >= 0, msService.getMessage("SELL_NUMBER_MIN") + otcCoin.getSellMinAmount());
            MessageResult result = otcWalletService.freezeBalance(memberWallet, advertise.getNumber());
            if (result.getCode() != 0) {
                throw new InformationExpiredException("Information Expired");
            }
        } else {
            Assert.isTrue(advertise.getNumber().compareTo(otcCoin.getBuyMinAmount()) >= 0, msService.getMessage("BUY_NUMBER_MIN") + otcCoin.getBuyMinAmount());
        }
        advertise.setRemainAmount(advertise.getNumber());
        advertise.setStatus(AdvertiseControlStatus.PUT_ON_SHELVES);
        return MessageResult.success(msService.getMessage("PUT_ON_SHELVES_SUCCESS"));
    }

    /**
     * 广告下架
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/off/shelves")
    @Transactional(rollbackFor = Exception.class)
    public MessageResult putOffShelves(long id, @SessionAttribute(SESSION_MEMBER) AuthMember authMember) throws InformationExpiredException {
        Advertise advertise = advertiseService.find(id, authMember.getId());
        Assert.isTrue(advertise != null, msService.getMessage("PUT_OFF_SHELVES_FAILED"));
        Assert.isTrue(advertise.getStatus().equals(AdvertiseControlStatus.PUT_ON_SHELVES), msService.getMessage("PUT_OFF_SHELVES_FAILED"));
        OtcCoin otcCoin = advertise.getCoin();
        if (advertise.getAdvertiseType().equals(AdvertiseType.SELL)) {
            OtcWallet memberWallet = otcWalletService.findByOtcCoinAndMemberId(authMember.getId(),otcCoin);
            MessageResult result = otcWalletService.thawBalance(memberWallet, advertise.getRemainAmount());
            if (result.getCode() != 0) {
                throw new InformationExpiredException("Information Expired");
            }
        }
        int ret = advertiseService.putOffShelves(advertise.getId(), advertise.getRemainAmount());
        if (!(ret > 0)) {
            throw new InformationExpiredException("Information Expired");
        }
       /* advertise.setNumber(BigDecimal.ZERO);
        advertise.setRemainAmount(BigDecimal.ZERO);
        advertise.setStatus(AdvertiseControlStatus.PUT_OFF_SHELVES);*/
        return MessageResult.success(msService.getMessage("PUT_OFF_SHELVES_SUCCESS"));
    }


    /**
     * 删除广告
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "delete")
    @Transactional(rollbackFor = Exception.class)
    public MessageResult delete(Long id, @SessionAttribute(SESSION_MEMBER) AuthMember shiroUser) {
        Advertise advertise = advertiseService.find(id, shiroUser.getId());
        Assert.notNull(advertise, msService.getMessage("DELETE_ADVERTISE_FAILED"));
        Assert.isTrue(advertise.getStatus().equals(AdvertiseControlStatus.PUT_OFF_SHELVES), msService.getMessage("DELETE_AFTER_OFF_SHELVES"));
        advertise.setStatus(AdvertiseControlStatus.TURNOFF);
        return MessageResult.success(msService.getMessage("DELETE_ADVERTISE_SUCCESS"));
    }


    /**
     * 查询优质广告
     *
     * @return
     */
    @RequestMapping(value = "excellent")
    public MessageResult allExcellentAdvertise(AdvertiseType advertiseType, @RequestParam(value = "legalCurrency", defaultValue = "NoSupport") String legalCurrency) throws Exception {
        List<Map<String, String>> marketPrices = new ArrayList<>();
        List<Map<String, String>> otcCoins = otcCoinService.getAllNormalCoin();
        //即默认全部是人民币
        MessageResult messageResult = MessageResult.success();
        if (legalCurrency.equalsIgnoreCase("NoSupport")) {
            otcCoins.stream().forEachOrdered(x -> {
                Map<String, String> map = new HashMap<>(2);
                map.put("name", x.get("unit"));
                map.put("price", coins.getCny(x.get("unit")).toString());
                marketPrices.add(map);
            });
            List<ScanAdvertise> list = advertiseService.getAllExcellentAdvertise(advertiseType, marketPrices);
            messageResult.setData(list);
        } else {
            List<Country> countrys = countryService.findByLegalCurrency(legalCurrency);
            if (countrys.size() <= 0) {
                return error("country no support");
            }
            Country country = countrys.get(0);
            otcCoins.stream().forEachOrdered(x -> {
                Map<String, String> map = new HashMap<>(2);
                map.put("name", x.get("unit"));
                map.put("price", coins.getLegalCurrencyRate(legalCurrency, x.get("unit")).toString());
                marketPrices.add(map);
            });
            List<ScanAdvertise> list = advertiseService.getAllExcellentAdvertise(advertiseType, marketPrices, country);
            messageResult.setData(list);
        }
        return messageResult;
    }

    /**
     * 分页查询广告
     *
     * @param pageNo
     * @param pageSize
     * @return
     */
    @RequestMapping(value = "page")
    public MessageResult queryPageAdvertise(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
                                            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                            Long id, AdvertiseType advertiseType,
                                            @RequestParam(value = "isCertified", defaultValue = "0") Integer isCertified,
                                            @RequestParam(value = "paymode",required = false) String[] payModes,
                                            @RequestParam(value = "legalCurrency", defaultValue = "NoSupport") String legalCurrency) throws SQLException, DataException {
        OtcCoin otcCoin = otcCoinService.findOne(id);
        MessageResult messageResult = MessageResult.success();
        List<Country> countrys = countryService.findByLegalCurrency(legalCurrency.equalsIgnoreCase("NoSupport")?"CNY":legalCurrency);
        if (countrys.size() <= 0) {
            return error("country no support");
        }
        Country country = countrys.get(0);
        double marketPrice = coins.getLegalCurrencyRate(country.getLocalCurrency(),otcCoin.getUnit()).doubleValue();
        if (payModes!=null&&payModes.length>0&&!StringUtils.hasText(payModes[0])){
            payModes=null;
        }
            SpecialPage<ScanAdvertise> page = advertiseService.paginationAdvertise(pageNo, pageSize, otcCoin, advertiseType, marketPrice, isCertified,country,payModes);
            messageResult.setData(page);

        return messageResult;
    }

    @RequestMapping(value = "page-by-unit")
    public MessageResult queryPageAdvertiseByUnit(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
                                                  @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                  String unit, AdvertiseType advertiseType,
                                                  @RequestParam(value = "isCertified", defaultValue = "0") Integer isCertified,
                                                  @RequestParam(value = "paymode",required = false) String[] payModes,
                                                  @RequestParam(value = "legalCurrency", defaultValue = "CNY",required = false) String legalCurrency) throws SQLException, DataException {
        OtcCoin otcCoin = otcCoinService.findByUnit(unit);
        Assert.notNull(otcCoin, "validate otcCoin unit!");
        MessageResult messageResult = MessageResult.success();
        List<Country> countrys = countryService.findByLegalCurrency(legalCurrency);
        if (countrys.size() == 0) {
            return error("country no support");
        }
        Country country = countrys.get(0);
        BigDecimal exchangePrice = coins.getLegalCurrencyRate(country.getLocalCurrency(),otcCoin.getUnit());
        double marketPrice = 0.00;
        if(exchangePrice != null){
            marketPrice = exchangePrice.doubleValue();
        }
        if (payModes!=null && (payModes.length==0 || payModes.length > 0 &&!StringUtils.hasText(payModes[0]))){
            payModes=null;
        }
        SpecialPage<ScanAdvertise> page = advertiseService.paginationAdvertise(pageNo, pageSize, otcCoin, advertiseType, marketPrice, isCertified,country,payModes);
        messageResult.setData(page);
        return messageResult;
    }

    @RequestMapping(value = "member", method = RequestMethod.POST)
    public MessageResult memberAdvertises(String name) {
        Member member = memberService.findByUsername(name);
        if (member != null) {
            MemberAdvertiseInfo memberAdvertise = advertiseService.getMemberAdvertise(member, coins.getCoins("CNY"));
            MessageResult result = MessageResult.success();
            result.setData(memberAdvertise);
            return result;
        } else {
            return MessageResult.error(msService.getMessage("MEMBER_NOT_EXISTS"));
        }
    }

    private StringBuffer checkPayMode(String[] pay, AdvertiseType advertiseType, Member member) {
        StringBuffer payMode = new StringBuffer();
        Arrays.stream(pay).forEach(x -> {
            if (advertiseType.equals(AdvertiseType.SELL)) {
                if (ALIPAY.getCnName().equals(x)) {
                    Assert.isTrue(member.getAlipay() != null, msService.getMessage("NO_ALI"));
                } else if (WECHAT.getCnName().equals(x)) {
                    Assert.isTrue(member.getWechatPay() != null, msService.getMessage("NO_WECHAT"));
                } else if (BANK.getCnName().equals(x)) {
                    Assert.isTrue(member.getBankInfo() != null, msService.getMessage("NO_BANK"));
                } else {
                    throw new IllegalArgumentException("pay parameter error");
                }
            }
            payMode.append(x + ",");
        });
        return payMode.deleteCharAt(payMode.length() - 1);
    }

    private void checkAmount(AdvertiseType advertiseType, Advertise advertise, OtcCoin otcCoin, Member member) {
        if (advertiseType.equals(AdvertiseType.SELL)) {
            Assert.isTrue(compare(advertise.getNumber(), otcCoin.getSellMinAmount()), msService.getMessage("SELL_NUMBER_MIN") + otcCoin.getSellMinAmount());
            OtcWallet memberWallet = otcWalletService.findByOtcCoinAndMemberId(member.getId(),otcCoin);
            isTrue(memberWallet.getIsLock() == 0,msService.getMessage("WALLET_IS_LOCK"));
            Assert.isTrue(compare(memberWallet.getBalance(), advertise.getNumber()), msService.getMessage("INSUFFICIENT_BALANCE"));
        } else {
            Assert.isTrue(compare(advertise.getNumber(), otcCoin.getBuyMinAmount()), msService.getMessage("BUY_NUMBER_MIN") + otcCoin.getBuyMinAmount());
        }
    }

    /**
     * 查询最新十条广告
     * TODO 查询提速
     */
    @RequestMapping(value = "newest")
    public MessageResult queryNewest() throws Exception {
        Special<ScanAdvertise> list = advertiseService.getLatestAdvertise();
        OtcCoin otcCoin;
        double finalPrice;

        //空指针
        if (list == null || list.getContext() == null) return success("data null!");

        for (ScanAdvertise adv : list.getContext()) {
            if (null != adv) {
                otcCoin = otcCoinService.findOne(adv.getCoinId());
                if (null == otcCoin) {
                    continue;
                }
                finalPrice = coins.getCny(otcCoin.getUnit()).doubleValue();
                if (null != adv.getPremiseRate()) {
                    //pricetype = 0 ? price : 计算价格
                    adv.setPrice(BigDecimalUtils.round(((adv.getPremiseRate().doubleValue() + 100) / 100) * finalPrice, 2));
                }
                adv.setUnit(otcCoin.getUnit());
                adv.setCoinName(otcCoin.getName());
                adv.setCoinNameCn(otcCoin.getNameCn());
            }
        }
        MessageResult messageResult = MessageResult.success();
        messageResult.setData(list);
        return messageResult;
    }
}
