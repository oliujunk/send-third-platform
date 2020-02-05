package com.whxph.sendthirdplatform.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * @author liujun
 * @description
 * @create 2019-12-14 08:42
 */
public class Xphapi {

    public static String updateToken(RestTemplate restTemplate) {
        JSONObject authData = new JSONObject();
        authData.put("username", "test");
        authData.put("password", "123456");
        JSONObject data = restTemplate.postForObject("http://47.105.215.208:8005/login",
            authData, JSONObject.class);
        assert data != null;
        return data.getString("token");
    }

    public static Set<Integer> updateDevice(RestTemplate restTemplate, String username, String token) {
        Set<Integer> deviceSet = new HashSet<>();
        HttpHeaders headers = new HttpHeaders();
        headers.add("token", token);
        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
        ResponseEntity<JSONObject> resEntity = restTemplate.exchange(
            String.format("http://47.105.215.208:8005/user/%s", username),
            HttpMethod.GET, requestEntity, JSONObject.class);
        JSONArray devices = Objects.requireNonNull(resEntity.getBody()).getJSONArray("devices");
        for (int i = 0; i < devices.size(); i++) {
            deviceSet.add(devices.getJSONObject(i).getInteger("facId"));
        }
        return deviceSet;
    }
}
