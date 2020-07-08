package com.whxph.sendthirdplatform.zhonghuanongye;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.whxph.sendthirdplatform.utils.Xphapi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author liujun
 * @description 中化农业
 * @create 2020-06-06 16:11
 */
@Component
public class Zhonghuanongye {

    private static final Logger LOGGER = LoggerFactory.getLogger(Zhonghuanongye.class);

    private boolean start = false;

    private String token = "";

    @SuppressWarnings("FieldCanBeLocal")
    private final String username = "2181185";

    @Resource
    private RestTemplate restTemplate;

    private JSONArray devices;

    @Scheduled(cron = "0 0 */12 * * ?")
    public void updateDeviceList() {
        if (start) {
            token = Xphapi.updateToken(restTemplate);
            updateDevice();
        }
    }

    @Scheduled(cron = "0 */15 * * * ?")
    public void sendData() {
        if (start) {
            for (int i = 0; i < devices.size(); i++) {
                updateData(devices.getJSONObject(i).getInteger("facId"));
            }
        }
    }

    public void start() {
        start = true;
        token = Xphapi.updateToken(restTemplate);
        updateDevice();
    }

    private void updateDevice() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("token", token);
        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
        ResponseEntity<JSONObject> resEntity = restTemplate.exchange(
            String.format("http://47.105.215.208:8005/user/%s", username),
            HttpMethod.GET, requestEntity, JSONObject.class);
        devices = Objects.requireNonNull(resEntity.getBody()).getJSONArray("devices");
    }

    private void updateData(Integer deviceId) {
        JSONObject data = restTemplate.getForObject(String.format("http://47.105.215.208:8005/intfa/queryData/%d", deviceId), JSONObject.class);
        assert data != null;
        JSONArray entity = data.getJSONArray("entity");
        JSONObject dataInfo = new JSONObject();
        for (int i = 0; i < entity.size(); i++) {
            JSONObject item = entity.getJSONObject(i);
            dataInfo.put(item.getString("eName"), item.getString("eValue"));
        }
        dataInfo.put("equipmentId", data.getString("deviceName"));

        JSONObject verify = new JSONObject();
        verify.put("appId", "3eff34a5946a4cd793c2487f908e1860");
        verify.put("secretKey", "790490f735d84455b889c095e651a561");

        JSONObject message = new JSONObject();
        message.put("verify", verify);
        message.put("dataInfo", dataInfo);
        LOGGER.info("[{}]: {}", deviceId, message);

        JSONObject result = restTemplate.postForObject("http://117.78.48.161:8181/api/push/equipmentPushData", message, JSONObject.class);
        LOGGER.info("[{}]: {}", deviceId, result);
    }
}
