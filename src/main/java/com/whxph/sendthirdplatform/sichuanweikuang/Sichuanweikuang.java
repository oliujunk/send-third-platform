package com.whxph.sendthirdplatform.sichuanweikuang;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.whxph.sendthirdplatform.utils.OldXphapi;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * @author liujun
 */
@Component
public class Sichuanweikuang {

    private static final Logger LOGGER = LoggerFactory.getLogger(Sichuanweikuang.class);

    private boolean start = false;

    private String username = "3264676";

    private String[] urls = {
            "http://223.71.251.217:7001/wkkDataAccess/api/wkkss/addDrybeachequip", // 干滩
            "http://223.71.251.217:7001/wkkDataAccess/api/wkkss/addReserwaterlevel", // 水位
            "http://223.71.251.217:7001/wkkDataAccess/api/wkkss/addDisplacement", // 坝体位移
            "http://223.71.251.217:7001/wkkDataAccess/api/wkkss/addSaturationline", // 浸润线
            "http://223.71.251.217:7001/wkkDataAccess/api/wkkss/addRainfall", // 降雨量
            "http://223.71.251.217:7001/wkkDataAccess/api/wkkss/addInclinometer", // 内部测斜
    };

    private String password = "aa7889d3-435b-4ed2-99f9-08035661eda9";

    @Resource
    private RestTemplate restTemplate;

    @Scheduled(cron = "0 0/5 * * * ?")
    public void update() throws Exception {
        if (start) {
            JSONObject data = restTemplate
                    .getForObject(String.format("http://47.105.215.208:8005/intfa/queryData/%d", 16068992),
                                  JSONObject.class);
            assert data != null;
            JSONObject rain = data.getJSONArray("entity").getJSONObject(0);
            JSONObject postData = new JSONObject();
            JSONArray records = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("sensorno", "51342500040601"); // 雨量1
            record.put("collectdate", rain.getString("datetime"));
            record.put("value", rain.getString("eValue"));
            records.add(record);
            postData.put("RECORDS", records);
            doPost(urls[4], AESUtil.encrypt(postData.toJSONString().getBytes(StandardCharsets.UTF_8), password));
            Thread.sleep(1000);

            data = restTemplate
                    .getForObject(String.format("http://47.105.215.208:8005/intfa/queryData/%d", 16069029),
                                  JSONObject.class);
            assert data != null;
            JSONObject displacement = data.getJSONArray("entity").getJSONObject(1);
            postData.clear();
            records.clear();
            record.clear();
            record.put("sensorno", "51342500040301"); // 表面位移
            record.put("collectdate", displacement.getString("datetime"));
            record.put("xvalue", displacement.getString("eValue"));
            record.put("yvalue", displacement.getString("eValue"));
            record.put("zvalue", displacement.getString("eValue"));
            records.add(record);
            postData.put("RECORDS", records);
            doPost(urls[2], AESUtil.encrypt(postData.toJSONString().getBytes(StandardCharsets.UTF_8), password));
            Thread.sleep(1000);

            data = restTemplate
                    .getForObject(String.format("http://47.105.215.208:8005/intfa/queryData/%d", 16069044),
                                  JSONObject.class);
            assert data != null;
            JSONObject rain2 = data.getJSONArray("entity").getJSONObject(1);
            postData.clear();
            records.clear();
            record.clear();
            record.put("sensorno", "51342500040602"); // 雨量2
            record.put("collectdate", rain2.getString("datetime"));
            record.put("value", rain2.getString("eValue"));
            records.add(record);
            postData.put("RECORDS", records);
            doPost(urls[4], AESUtil.encrypt(postData.toJSONString().getBytes(StandardCharsets.UTF_8), password));
            Thread.sleep(1000);

            JSONObject waterLevel = data.getJSONArray("entity").getJSONObject(3);
            postData.clear();
            records.clear();
            record.clear();
            record.put("sensorno", "51342500040201"); // 水位
            record.put("collectdate", waterLevel.getString("datetime"));
            record.put("value", "0");
            records.add(record);
            postData.put("RECORDS", records);
            doPost(urls[1], AESUtil.encrypt(postData.toJSONString().getBytes(StandardCharsets.UTF_8), password));
            Thread.sleep(1000);

            postData.clear();
            records.clear();
            record.clear();
            record.put("sensorno", "51342500040101"); // 干滩
            record.put("collectdate", waterLevel.getString("datetime"));
            record.put("value", "450");
            records.add(record);
            postData.put("RECORDS", records);
            doPost(urls[0], AESUtil.encrypt(postData.toJSONString().getBytes(StandardCharsets.UTF_8), password));
            Thread.sleep(1000);

            postData.clear();
            records.clear();
            record.clear();
            record.put("sensorno", "51342500040501"); // 浸润线
            record.put("collectdate", waterLevel.getString("datetime"));
            record.put("value", "3");
            records.add(record);
            postData.put("RECORDS", records);
            doPost(urls[3], AESUtil.encrypt(postData.toJSONString().getBytes(StandardCharsets.UTF_8), password));
        }
    }

    public void start() throws Exception {
        this.start = true;
    }

    public void doPost(String url, byte[] bytes) {
        // 创建Httpclient对象
        HttpClient client = new HttpClient();
        PostMethod httpPost = new PostMethod(url);
        //设置header
        httpPost.addRequestHeader("Content-Type", "text/plain");
        httpPost.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "utf-8");
        InputStream inputStream = new ByteArrayInputStream(bytes);
        httpPost.setRequestBody(inputStream);
        try {
            int code = client.executeMethod(httpPost);
            if (code == 200) {
                String res = httpPost.getResponseBodyAsString();
                LOGGER.info("发送成功: {}", res);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
