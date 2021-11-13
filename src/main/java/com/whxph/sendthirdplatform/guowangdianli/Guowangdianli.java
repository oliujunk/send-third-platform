package com.whxph.sendthirdplatform.guowangdianli;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@Slf4j
@Component
public class Guowangdianli {

    private boolean start = false;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;


    public void start() throws InterruptedException {
        start = true;
    }

    @Scheduled(cron = "0 */1 * * * ?")
    public void sendData() {
        if (start) {
            JSONObject data = restTemplate.getForObject(String.format("http://47.105.215.208:8005/intfa/queryData/%d", 15112501), JSONObject.class);
            assert data != null;
            log.info("{}", data);
            JSONArray entity = data.getJSONArray("entity");
            JSONObject msg = new JSONObject();
            msg.put("apNo", "123456");
            JSONObject device = new JSONObject();
            device.put("deviceId", "32601013");
            device.put("dataType", "1");
            device.put("dateReprotTime", String.valueOf(System.currentTimeMillis() / 1000));
            device.put("dateCollectTime", String.valueOf(System.currentTimeMillis() / 1000));
            JSONObject da1 = new JSONObject();
            da1.put("Temperature", "15.6");
            da1.put("Humidity", "56.7");
            da1.put("SupWh", "1.2");
            device.put("data", da1);
            JSONArray devices = new JSONArray();
            devices.add(device);
            msg.put("devices", devices);
            log.info(msg.toJSONString());
            kafkaTemplate.send("wktest", msg.toJSONString()).addCallback(success -> {
                // 消息发送到的topic
                assert success != null;
                String topic = success.getRecordMetadata().topic();
                // 消息发送到的分区
                int partition = success.getRecordMetadata().partition();
                // 消息在分区内的offset
                long offset = success.getRecordMetadata().offset();

                log.info("发送消息成功:" + topic + "-" + partition + "-" + offset);
            }, failure -> {
                log.error(failure.getMessage());
                log.error("发送消息失败:" + failure.getMessage());
            });
        }
    }
}
