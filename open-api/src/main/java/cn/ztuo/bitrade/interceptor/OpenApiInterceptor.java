package cn.ztuo.bitrade.interceptor;


import cn.ztuo.bitrade.util.HmacSHA256Signer;
import cn.ztuo.bitrade.util.HttpRequestUtil;
import com.alibaba.fastjson.JSONObject;
import cn.ztuo.bitrade.entity.MemberApiKey;
import cn.ztuo.bitrade.service.MemberApiKeyService;

import cn.ztuo.bitrade.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @description: OpenApiInterceptor
 * api拦截器
 * @author: MrGao
 * @create: 2019/05/06 14:11
 */
@Slf4j
public class OpenApiInterceptor implements HandlerInterceptor {


    private static final String API_HOST = "39.100.79.158";
    private static final String SIGNATURE_METHOD = "HmacSHA256";
    private static final String SIGNATURE_VERSION = "2";

    private static final ZoneId ZONE_GMT = ZoneId.of("Z");

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object object) throws Exception {
        //获取请求参数
        Map<String, String[]> parameterMap = request.getParameterMap();
        log.info("A={}",JSONObject.toJSONString(parameterMap));
        Map<String,String> params = new TreeMap<>();
        for (Iterator iterator = parameterMap.keySet().iterator(); iterator.hasNext();) {
            String name = (String) iterator.next();
            String[] values =  parameterMap.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用。如果mysign和sign不相等也可以使用这段代码转化
            //valueStr = new String(valueStr.getBytes("ISO-8859-1"), "gbk");
            params.put(name, valueStr);
        }
        String accessKeyId = params.get("accessKeyId");
        String apiKey ="";
        if(accessKeyId!=null){
            apiKey = accessKeyId;
        }
        String signature = params.get("signature");
        if(StringUtils.isEmpty(signature)){
            MemberInterceptor.ajaxReturn(response,3002,"签名有误，请核实");
            return false;
        }
        if(StringUtils.isEmpty(apiKey)){
            MemberInterceptor.ajaxReturn(response,3000,"参数有误，请核实");
            return false;
        }
        String timestamp = params.get("timestamp");
        if(StringUtils.isEmpty(timestamp)){
            MemberInterceptor.ajaxReturn(response,3003,"时间戳有误，请核实");
            return false;
        }

        //根据apiKey获取用户信息
        //解决service为null无法注入问题
        BeanFactory factory = WebApplicationContextUtils.getRequiredWebApplicationContext(request.getServletContext());
        MemberApiKeyService apiKeyService =(MemberApiKeyService)factory.getBean("memberApiKeyService");
        MemberApiKey memberApiKey = apiKeyService.findMemberApiKeyByApiKey(apiKey);
        //判断ip
        String remoteIp = request.getHeader("X-Real-IP");
        //限制ip调用次数 10s 一次
        RedisUtil redisUtil = (RedisUtil) factory.getBean("redisUtil");
        Object o = redisUtil.get(remoteIp);
        if(o!=null){
            MemberInterceptor.ajaxReturn(response,3005,"调用频繁，请稍后");
            return false;
        }
        redisUtil.set(remoteIp,"limit",10,TimeUnit.SECONDS);
        String ips = memberApiKey.getBindIp();
        if(StringUtils.isNotEmpty(ips)){
            String[] split = ips.split(",");
            List<String> ipList = Arrays.asList(split);
            if(!ipList.contains(remoteIp)){
                MemberInterceptor.ajaxReturn(response,3004,"IP有误，请核实");
                return false;
            }
        }
        if(memberApiKey==null){
            MemberInterceptor.ajaxReturn(response,3001,"apiKey有误，请核实");
            return false;
        }
        //创建签名
        String method = request.getMethod();
        String path = request.getRequestURI();

        //移除重复
        params.remove("accessKeyId");
        params.remove("timestamp");
        params.remove("signature");
        params.remove("signatureMethod");
        params.remove("signatureVersion");


        String  sign=  createSignature(method,path,apiKey,timestamp,params,memberApiKey.getSecretKey());
        if(!signature.equals(sign)){
            MemberInterceptor.ajaxReturn(response,3002,"签名有误，请核实");
            return false;
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {

    }

    private static String createSignature(String method, String path,String apiKey, String timeStamp,
                                   Map map,String secretKey) {
        StringBuilder sb = new StringBuilder(1024);
        // GET
        sb.append(method.toUpperCase()).append('\n')
                // Host
                .append(API_HOST.toLowerCase()).append('\n')
                // path
                .append(path).append('\n');



        StringJoiner joiner = new StringJoiner("&");
        joiner.add("accessKeyId=" + apiKey)
                .add("signatureMethod=" + SIGNATURE_METHOD)
                .add("signatureVersion=" + SIGNATURE_VERSION)
                .add("timestamp=" + encode(timeStamp));


        //拼接 遍历map
        Iterator<Map.Entry<String, String>> entries = map.entrySet().iterator();
        while (entries.hasNext()){
            Map.Entry<String, String> entry = entries.next();
            joiner.add(entry.getKey()+"="+entry.getValue());
        }
        String sign = HmacSHA256Signer.sign(sb.toString() + joiner.toString(), secretKey);
        log.info("sb={},joiner={},sign={}",sb.toString(),joiner.toString(),sign);
        return sign;
    }

    private static String encode(String code) {
        try {
            return URLEncoder.encode(code, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args)throws Exception {

        String s = Instant.now().atZone(ZONE_GMT).format(DT_FORMAT);
        String timeStamp = URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20");
        System.out.println( timeStamp);
        Map map = new TreeMap();
//        map.put("memberId","9");
        String sin = createSignature("get","/open-api/user/get/account",
                "cdgs9k03f3-9230e45d-5bf1d983-fe287",timeStamp,
                map,
                "3d53b8ea-4ea4d893-8f27a188-9648a");
        System.out.println(encode(sin));
        System.out.println(sin);
        Map<String, String> params = new TreeMap<>() ;
        params.put("accessKeyId","cdgs9k03f3-9230e45d-5bf1d983-fe287");
        params.put("signatureMethod",SIGNATURE_METHOD);
        params.put("signatureVersion",SIGNATURE_VERSION);
        params.put("timestamp",timeStamp);
        params.put("signature",sin);
        String ss = HttpRequestUtil.URLGet("http://39.100.79.158/open-api/user/get/account",params,"utf-8");
        System.out.println(ss);
        System.out.println(encode("bVgQnXsmFi5jBdDNuHBqB7qk6Umh/d+xTiOIewpB6oU="));

    }
}
