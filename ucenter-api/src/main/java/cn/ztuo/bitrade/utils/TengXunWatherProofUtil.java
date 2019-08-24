package cn.ztuo.bitrade.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * @Description:
 * @Author: GuoShuai
 * @Date: 2019/4/25 3:12 PM
 */
@Configuration
@Slf4j
public class TengXunWatherProofUtil {



    private static String appId;

    private static String appSecretKey;

    private static final String url = "https://ssl.captcha.qq.com/ticket/verify";

    private static MultiThreadedHttpConnectionManager connectionManager = null;

    private static int connectionTimeOut = 15000;

    private static int socketTimeOut = 15000;

    private static int maxConnectionPerHost = 500;

    private static int maxTotalConnections = 500;


    private static HttpClient client;

    static {
        connectionManager = new MultiThreadedHttpConnectionManager();
        connectionManager.getParams().setConnectionTimeout(connectionTimeOut);
        connectionManager.getParams().setSoTimeout(socketTimeOut);
        connectionManager.getParams().setDefaultMaxConnectionsPerHost(maxConnectionPerHost);
        connectionManager.getParams().setMaxTotalConnections(maxTotalConnections);
        client = new HttpClient(connectionManager);
    }

    @Value("${water.proof.app.id}")
    public void setAppId(String appId) {
        TengXunWatherProofUtil.appId = appId;
    }

    @Value("${water.proof.app.secret.key}")
    public void setAppSecretKey(String appSecretKey) {
        TengXunWatherProofUtil.appSecretKey = appSecretKey;
    }


    /**
     * @param ticket
     * @param randStr
     * @param ip
     * @return 腾讯防水验证
     * @throws Exception
     */
    public static Boolean watherProof(String ticket, String randStr, String ip) throws Exception {
        String response = null;
        GetMethod getMethod = null;
        Boolean responseBool = false;
        try {
            log.info("watherProof>>>>>start>>>ip>>>" + ip);
            StringBuilder sb = new StringBuilder();
            sb.append(url).append("?aid=").append(appId)
                    .append("&AppSecretKey=").append(appSecretKey)
                    .append("&Ticket=").append(ticket).
                    append("&Randstr=").append(randStr).
                    append("&UserIP=").append(ip);
            log.info("防水请求：" + sb.toString());
            getMethod = new GetMethod(sb.toString());
            int code = client.executeMethod(getMethod);
            if (code == 200) {
                response = getMethod.getResponseBodyAsString();
            } else {
                log.info("状态响应码为>>>>>>" + code);
            }
        } catch (HttpException e) {
            log.info("发生致命的异常，可能是协议不对或者返回的内容有问题", e);
        } catch (IOException e) {
            log.error("发生网络异常", e);
        } finally {
            if (getMethod != null) {
                getMethod.releaseConnection();
                getMethod = null;
            }
        }
        log.info(">>>>>>>>发送校验结果响应为>>>>>>" + response);
        if (!StringUtils.isEmpty(response)) {
            JSONObject responseJson = JSONObject.parseObject(response);
            String code = responseJson.getString("response");
            if ("1".equals(code)) {
                responseBool = true;
            }
        }
        return responseBool;
    }
}
