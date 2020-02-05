package com.whxph.sendthirdplatform.heertai;

import com.alibaba.fastjson.JSONObject;
import com.whxph.sendthirdplatform.utils.Xphapi;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @author liujun
 * @description 和而泰农业
 * @create 2019-12-09 06:47
 */
@Component
public class Heertai {

    private static final Logger LOGGER = LoggerFactory.getLogger(Heertai.class);

    private Set<Integer> deviceSet = new HashSet<>();

    private String token = "";

    private String username = "4693048";

    private boolean start = false;

//    private String url = "https://open.api.clife.cn/apigateway/clink/api/open/clink/open/api/push/messages";
    private String url = "https://dp.clife.net/apigateway/open/clink/open/api/push/messages";

    @Resource
    private RestTemplate restTemplate;

    @Scheduled(cron = "0 0 0/12 * * ?")
    public void updateDeviceSet() {
        if (start) {
            token = Xphapi.updateToken(restTemplate);
            deviceSet = Xphapi.updateDevice(restTemplate, username, token);
        }
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void sendData() {
        if (start) {
            for (Integer deviceId : deviceSet) {
                LOGGER.info(String.format("[%d]: 开始推送...", deviceId));
                JSONObject data = restTemplate.getForObject(String.format("http://47.105.215.208:8005/intfa/queryData/%d", deviceId),
                    JSONObject.class);
                JSONObject sendResult = new JSONObject();
                try {
                    sendResult = restTemplate.postForObject(
                        url,
                        data, JSONObject.class);
                    assert sendResult != null;
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                } finally {
                    assert sendResult != null;
                    LOGGER.info(sendResult.toJSONString());
                }
            }
        }
    }

    public void start() {
        start = true;
        token = Xphapi.updateToken(restTemplate);
        deviceSet = Xphapi.updateDevice(restTemplate, username, token);
    }
}
