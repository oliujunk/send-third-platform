package com.whxph.sendthirdplatform.qingdaojiutian;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.whxph.sendthirdplatform.utils.Xphapi;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
 * @description 青岛九天
 * @create 2019-12-13 17:07
 */
@Component
public class Qingdaojiutian {

    private static final Logger LOGGER = LoggerFactory.getLogger(Qingdaojiutian.class);

    private boolean start = false;

    private Set<Integer> deviceSet = new HashSet<>();

    private String token = "";

    private String username = "test";

//    private String pk = "4fcf1483a4fa4cd0869921668caab37a";
    private String pk = "75b644204a06489ab8a83091c2137456";

    @Resource
    private MqttGateway mqttGateway;

    @Resource
    private RestTemplate restTemplate;

    @Scheduled(cron = "0 0 0/12 * * ?")
    public void updateDeviceSet() {
        if (start) {
            token = Xphapi.updateToken(restTemplate);
//            deviceSet = Xphapi.updateDevice(restTemplate, username, token);
            deviceSet.add(16064260);
        }
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void sendToMqtt() {
        if (start) {
            for (Integer deviceId : deviceSet) {
                JSONObject xphData = restTemplate.getForObject(String.format("http://47.105.215.208:8005/intfa/queryData/%d", deviceId),
                    JSONObject.class);
                assert xphData != null;
                JSONArray entity = xphData.getJSONArray("entity");
                JSONObject sendData = new JSONObject();
                sendData.put("id", String.format("JT00%d", deviceId));
                sendData.put("method", "thing.event.property.post");
                ZoneOffset zoneOffset = ZoneOffset.ofHours(8);
                LocalDateTime localDateTime = LocalDateTime.now();
                sendData.put("timeStamp", localDateTime.toEpochSecond(zoneOffset));
                JSONObject params = new JSONObject();
                params.put("status", 1);
                JSONObject data = new JSONObject();
//                data.put("04003", entity.getJSONObject(3).getString("eValue"));
//                data.put("04004", entity.getJSONObject(4).getString("eValue"));
//                data.put("04005", entity.getJSONObject(0).getString("eValue"));
//                data.put("04006", entity.getJSONObject(1).getString("eValue"));
//                data.put("04007", entity.getJSONObject(2).getString("eValue"));
//                data.put("04012", entity.getJSONObject(5).getString("eValue"));
//                data.put("04010", entity.getJSONObject(6).getString("eValue"));
//                data.put("04011", entity.getJSONObject(7).getString("eValue"));
                data.put("04014", entity.getJSONObject(0).getString("eValue"));
                data.put("04015", entity.getJSONObject(1).getString("eValue"));
                data.put("04017", entity.getJSONObject(2).getString("eValue"));
                data.put("04029", entity.getJSONObject(3).getString("eValue"));
                data.put("04028", entity.getJSONObject(4).getString("eValue"));
                data.put("04016", entity.getJSONObject(5).getString("eValue"));
                data.put("04027", entity.getJSONObject(6).getString("eValue"));
                data.put("04030", entity.getJSONObject(7).getString("eValue"));
                params.put("data", data);
                sendData.put("params", params);
                sendData.put("version", "1.0");
                mqttGateway.sendToMqtt(sendData.toJSONString(), String.format("sys/%s/JT00%d/thing/event/property/post", pk, deviceId));
                LOGGER.info("Send to MQTT");
                LOGGER.info(sendData.toJSONString());
            }
        }
    }

    public void start() {
        start = true;
        token = Xphapi.updateToken(restTemplate);
//        deviceSet = Xphapi.updateDevice(restTemplate, username, token);
        deviceSet.add(16064260);
    }

}
