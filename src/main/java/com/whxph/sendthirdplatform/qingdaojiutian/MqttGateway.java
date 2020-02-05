package com.whxph.sendthirdplatform.qingdaojiutian;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;

/**
 * @author liujun
 * @description
 * @create 2019-12-13 17:09
 */
@MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
public interface MqttGateway {

    /**
     * 发送消息
     * @param data 数据
     * @param topic 主题
     */
    void sendToMqtt(String data, @Header(MqttHeaders.TOPIC)String topic);

}
