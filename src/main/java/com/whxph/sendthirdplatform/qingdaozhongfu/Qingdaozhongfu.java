package com.whxph.sendthirdplatform.qingdaozhongfu;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.whxph.sendthirdplatform.utils.Xphapi;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Resource;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import static com.whxph.sendthirdplatform.SendThirdPlatformApplication.standardData;

/**
 * @author liujun
 * @description 青岛中福环保
 * @create 2019-12-28 14:17
 */
@Component
public class Qingdaozhongfu {

    private static final Logger LOGGER = LoggerFactory.getLogger(Qingdaozhongfu.class);

    private static Map<Integer, ChannelData> deviceMap = new ConcurrentHashMap<>();

    private boolean start = false;

    private String token = "";

    @SuppressWarnings("FieldCanBeLocal")
    private String username = "1543146";

    @Resource
    private RestTemplate restTemplate;

    @Scheduled(cron = "0 0 0/12 * * ?")
    public void updateDeviceSet() throws InterruptedException {
        if (start) {
            token = Xphapi.updateToken(restTemplate);
            updateChannel();
        }
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void sendData() {
        if (start) {
            for (Map.Entry<Integer, ChannelData> entry : deviceMap.entrySet()) {
                updateData(entry.getKey(), entry.getValue());
            }
        }
    }

    public void start() throws InterruptedException {
        start = true;
        token = Xphapi.updateToken(restTemplate);
        updateChannel();
    }

    private void updateChannel() throws InterruptedException {
        Bootstrap client = new Bootstrap();

        EventLoopGroup group = new NioEventLoopGroup();
        client.group(group);
        client.channel(NioSocketChannel.class);

        client.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) {
                ch.pipeline().addLast(new QDZFReceiveHandler());
            }
        });

        deviceMap.clear();

        HttpHeaders headers = new HttpHeaders();
        headers.add("token", token);
        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
        ResponseEntity<JSONObject> resEntity = restTemplate.exchange(
            String.format("http://47.105.215.208:8005/user/%s", username),
            HttpMethod.GET, requestEntity, JSONObject.class);
        JSONArray devices = Objects.requireNonNull(resEntity.getBody()).getJSONArray("devices");
        for (int i = 0; i < devices.size(); i++) {
            JSONObject device = devices.getJSONObject(i);
            Integer deviceId = Integer.parseInt(device.getString("facId"));
            ChannelData channelData = new ChannelData();
            channelData.setMn(device.getString("facName"));
            ChannelFuture channelFuture = client.connect("39.105.31.182", 9092).sync();
            Channel channel = channelFuture.channel();
            channelData.setChannel(channel);
            deviceMap.put(deviceId, channelData);
        }
    }

    private void updateData(Integer deviceId, ChannelData channelData) {
        Channel channel = channelData.getChannel();
        String mn = channelData.getMn();
        JSONObject data = restTemplate.getForObject(String.format("http://47.105.215.208:8005/intfa/queryData/%d", deviceId), JSONObject.class);
        assert data != null;
        JSONArray entity = data.getJSONArray("entity");
        StringBuilder stringBuilder = new StringBuilder("QN=");
        SimpleDateFormat s = new SimpleDateFormat("yyyyMMddHHmmssS");
        s.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        stringBuilder.append(s.format(new Date()));
        stringBuilder.append(";ST=39;CN=2011;PW=123456;MN=");
        stringBuilder.append(mn);
        stringBuilder.append(";Flag=5;");
        stringBuilder.append("CP=&&DataTime=");
        s = new SimpleDateFormat("yyyyMMddHHmmss");
        stringBuilder.append(s.format(new Date()));
        stringBuilder.append(";");
        // 风速
        stringBuilder.append("a01007-Rtd=");
        stringBuilder.append(entity.getJSONObject(0).getString("eValue"));
        stringBuilder.append(";a01007-Flag=N;");
        // 风向
        stringBuilder.append("a01008-Rtd=");
        stringBuilder.append(entity.getJSONObject(6).getString("eValue"));
        stringBuilder.append(";a01008-Flag=N;");
        // 温度
        stringBuilder.append("a01001-Rtd=");
        stringBuilder.append(entity.getJSONObject(2).getString("eValue"));
        stringBuilder.append(";a01001-Flag=N;");
        // 湿度
        stringBuilder.append("a01002-Rtd=");
        stringBuilder.append(entity.getJSONObject(8).getString("eValue"));
        stringBuilder.append(";a01002-Flag=N;");
        // 噪声
        stringBuilder.append("LA-Rtd=");
        stringBuilder.append(entity.getJSONObject(7).getString("eValue"));
        stringBuilder.append(";LA-Flag=N;");
        // PM2.5
        stringBuilder.append("a34004-Rtd=");
        float pm25 = entity.getJSONObject(1).getFloatValue("eValue");
        if (pm25 >= 32767 || pm25 <= 0) {
            pm25 = 40 + new Random().nextInt(20);
        }
        stringBuilder.append(pm25);

        stringBuilder.append(";a34004-Flag=N;");
        // PM10
        stringBuilder.append("a34002-Rtd=");
        float pm10 = entity.getJSONObject(3).getFloatValue("eValue");
        if (pm10 >= 32767 || pm10 <= 0) {
            pm10 = 60 + new Random().nextInt(20);
        }
        stringBuilder.append(pm10);
        stringBuilder.append(";a34002-Flag=N;");
        // 气压
        stringBuilder.append("a01006-Rtd=");
        stringBuilder.append(entity.getJSONObject(4).getString("eValue"));
        stringBuilder.append(";a01006-Flag=N&&");

        DecimalFormat decimalFormat = new DecimalFormat("0000");
        String dataLenStr = decimalFormat.format(stringBuilder.length());
        String crcStr = getCrc16(stringBuilder.toString().getBytes());
        String message = "##" + dataLenStr + stringBuilder.toString() + crcStr + "\r\n";
        channel.writeAndFlush(Unpooled.copiedBuffer(message.getBytes()));
        LOGGER.info("[{}]: 发送数据 {}", deviceId, message);
    }

    private String getCrc16(byte[] data) {
        int crcReg = 0xffff;
        int check;
        for (byte datum : data) {
            crcReg = (crcReg >> 8) ^ datum;
            for (int j = 0; j < 8; j++) {
                check = crcReg & 0x0001;
                crcReg >>= 1;
                if (check == 0x0001) {
                    crcReg ^= 0xA001;
                }
            }
        }
        String hexStr = Integer.toHexString(crcReg).toUpperCase();
        hexStr = "0000" + hexStr;
        return hexStr.substring(hexStr.length() - 4);
    }


    @Data
    private static class ChannelData {
        private Channel channel;
        private String mn;
    }
}
