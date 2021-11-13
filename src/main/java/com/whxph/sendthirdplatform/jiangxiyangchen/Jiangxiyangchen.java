package com.whxph.sendthirdplatform.jiangxiyangchen;

import com.alibaba.fastjson.JSONObject;
import com.whxph.sendthirdplatform.henan.Gkgridding;
import com.whxph.sendthirdplatform.utils.OldXphapi;
import org.apache.activemq.command.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.HashSet;
import java.util.Set;

@Component
public class Jiangxiyangchen {

    private static final Logger LOGGER = LoggerFactory.getLogger(Jiangxiyangchen.class);

    private boolean start = false;

    private String token = "";

    private String username = "39614141";

    private Set<Integer> deviceSet = new HashSet<>();

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private JmsMessagingTemplate jmsTemplate;

    @Scheduled(cron = "0 0 0/12 * * ?")
    public void updateDeviceSet() {
        if (start) {
            token = OldXphapi.updateToken(restTemplate);
            deviceSet = OldXphapi.updateDevice(restTemplate, username, token);
        }
    }

    @Scheduled(cron = "0 0 0/12 * * ?")
    public void updateToken() {
        if (start) {
            token = OldXphapi.updateToken(restTemplate);
        }
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void pushData() {
        if (start) {
            for (Integer deviceId : deviceSet) {
                JSONObject data = restTemplate.getForObject(String.format("http://115.28.187.9:8005/intfa/queryData/%d", deviceId), JSONObject.class);
                assert data != null;
                Destination destination = new ActiveMQQueue("qsh_building_alarm_queue");
                jmsTemplate.convertAndSend(destination, data.toJSONString());
                LOGGER.info(data.toJSONString());
            }
        }
    }

    public void start() {
        this.start = true;
        token = OldXphapi.updateToken(restTemplate);
        deviceSet = OldXphapi.updateDevice(restTemplate, username, token);
    }
}
