package com.whxph.sendthirdplatform.henan;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.whxph.sendthirdplatform.utils.OldXphapi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.*;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static com.whxph.sendthirdplatform.SendThirdPlatformApplication.standardData;

/**
 * @author liujun
 */
@Component
public class Gkgridding {

    private static final Logger LOGGER = LoggerFactory.getLogger(Gkgridding.class);

    private boolean start = false;

    private String token = "";

    private String username = "gkgridding";

    private Set<Integer> deviceSet = new HashSet<>();

    @Resource
    private RestTemplate restTemplate;

    @Scheduled(cron = "0 0 0/12 * * ?")
    public void updateDeviceSet() {
        if (start) {
            token = OldXphapi.updateToken(restTemplate);
            deviceSet = OldXphapi.updateDevice(restTemplate, username, token);
        }
    }

    @Scheduled(cron = "30 0/8 * * * ?")
    public void update() throws InterruptedException {
        if (start) {
            for (Integer deviceId : deviceSet) {
                JSONObject data = restTemplate.getForObject(String.format("http://115.28.187.9:8005/intfa/queryData/%d", deviceId), JSONObject.class);
                assert data != null;
                JSONArray entity = data.getJSONArray("entity");
                Socket socket = null;
                try {
                    String facId = deviceId.toString();
                    String dateTime = entity.getJSONObject(0).getString("datetime");
                    DecimalFormat decimalFormat = new DecimalFormat("0.00");
                    float humi = entity.getJSONObject(3).getFloatValue("eValue");
                    float temp = entity.getJSONObject(2).getFloatValue("eValue");
                    float pre = entity.getJSONObject(7).getFloatValue("eValue");
                    float windd = entity.getJSONObject(1).getFloatValue("eValue");
                    float winds = entity.getJSONObject(0).getFloatValue("eValue");
                    float noise = entity.getJSONObject(4).getFloatValue("eValue");
                    float pm25 = entity.getJSONObject(5).getFloatValue("eValue");
                    float pm10 = entity.getJSONObject(6).getFloatValue("eValue");
                    float co = entity.getJSONObject(8).getFloatValue("eValue");
                    float o3 = entity.getJSONObject(9).getFloatValue("eValue");
                    float so2 = entity.getJSONObject(10).getFloatValue("eValue");
                    float no2 = entity.getJSONObject(11).getFloatValue("eValue");
                    humi = humi >= 3276 || humi <= 0 ? standardData.getHumi() : humi;
                    temp = temp >= 3276 ? standardData.getTemp() : temp;
                    pre = pre >= 3276 || pre <= 0 ? standardData.getPre() : pre;
                    windd = windd >= 32767 || windd <= 0 ? standardData.getWindd() : windd;
                    winds = winds >= 3276 || winds <= 0 ? standardData.getWinds() : winds;
                    noise = noise >= 3276 || noise <= 0 ? standardData.getNoise() : noise;
                    pm25 = pm25 >= 32767 || pm25 <= 0 ? standardData.getPm25() + new Random().nextInt(5) : pm25;
                    pm10 = pm10 >= 32767 || pm10 <= 0 ? standardData.getPm10() + new Random().nextInt(10) : pm10;
                    co = co >= 327 || co <= 0 ? standardData.getCo() : co;
                    o3 = o3 >= 32767 || o3 <= 0 ? standardData.getO3() : o3;
                    so2 = so2 >= 32767 || so2 <= 0 ? standardData.getSo2() : so2;
                    no2 = no2 >= 32767 || no2 <= 0 ? standardData.getNo2() : no2;
                    String content = "DevID:" + facId +
                            "|Time:" + dateTime +
                            "|HUMI:" + decimalFormat.format(humi) +
                            "|TEMP:" + decimalFormat.format(temp) +
                            "|PRE:" + decimalFormat.format(pre) +
                            "|WINDD:" + decimalFormat.format(windd) +
                            "|WINDS:" + decimalFormat.format(winds) +
                            "|NOISE:" + decimalFormat.format(noise) +
                            "|PM25:" + decimalFormat.format(pm25) +
                            "|PM10:" + decimalFormat.format(pm10) +
                            "|TSP:-1" +
                            "|CO:" + decimalFormat.format(co) +
                            "|O3:" + decimalFormat.format(o3) +
                            "|SO2:" + decimalFormat.format(so2) +
                            "|NO2:" + decimalFormat.format(no2) +
                            "|XX1:1|XX2:1|XX3:1";
                    LOGGER.info("[{}]: {}", deviceId, content);
                    socket = new Socket("116.255.182.245", 9001);
                    OutputStream out = socket.getOutputStream();
                    out.write(content.getBytes());
                    out.flush();
                    InputStream in = socket.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    LOGGER.info("{}", br.readLine());
                    out.close();
                    br.close();
                    in.close();
                } catch (Exception e) {
                    LOGGER.error("接口请求异常");
                } finally {
                    if (socket != null){
                        try {
                            socket.close();
                        } catch (IOException e) {
                            LOGGER.error("socket关闭异常", e);
                        }
                    }
                }
                Thread.sleep(500);
            }
        }
    }

    public void start() {
        this.start = true;
        token = OldXphapi.updateToken(restTemplate);
        deviceSet = OldXphapi.updateDevice(restTemplate, username, token);
    }
}
