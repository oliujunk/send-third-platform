package com.whxph.sendthirdplatform.henan;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

import static com.whxph.sendthirdplatform.SendThirdPlatformApplication.standardData;


/**
 * @author liujun
 */
@Component
public class UpdateStandrdData {

    @Resource
    private RestTemplate restTemplate;

    @Scheduled(cron = "0 0/5 * * * ?")
    public void updateStandrdData() {
        standardData = new StandardData();
        JSONObject data = restTemplate.getForObject(String.format("http://115.28.187.9:8005/intfa/queryData/%d", 16054169), JSONObject.class);
        assert data != null;
        JSONArray entity = data.getJSONArray("entity");
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
        standardData.setHumi(humi >= 3276 ? 0 : humi);
        standardData.setTemp(temp >= 3276 ? 0 : temp);
        standardData.setPre(pre >= 3276 ? 0 : pre);
        standardData.setWindd(windd >= 32767 ? 0 : windd);
        standardData.setWinds(winds >= 3276 ? 0 : winds);
        standardData.setNoise(noise >= 3276 ? 0 : noise);
        standardData.setPm25(pm25 >= 32767 ? 0 : pm25);
        standardData.setPm10(pm10 >= 32767 ? 0 : pm10);
        standardData.setCo(co >= 327 ? 0 : co);
        standardData.setO3(o3 >= 32767 ? 0 : o3);
        standardData.setSo2(so2 >= 32767 ? 0 : so2);
        standardData.setNo2(no2 >= 32767 ? 0 : no2);
    }
}
