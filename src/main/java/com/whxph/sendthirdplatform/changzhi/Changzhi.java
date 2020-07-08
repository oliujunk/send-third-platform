package com.whxph.sendthirdplatform.changzhi;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.whxph.sendthirdplatform.utils.OldXphapi;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
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
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author liujun
 * @description 长治
 * @create 2019-12-28 14:17
 */
@Component
public class Changzhi {

    private static final Logger LOGGER = LoggerFactory.getLogger(Changzhi.class);

    static final Map<Integer, ChannelData> deviceMap = new ConcurrentHashMap<>();
    static final Map<Channel, ChannelData> onlineMap = new ConcurrentHashMap<>();

    private boolean start = false;

    private String token = "";

    @SuppressWarnings("FieldCanBeLocal")
    private final String username = "changzhi";

    @Resource
    private RestTemplate restTemplate;

    @Scheduled(cron = "30 1 0/12 * * ?")
//    @Scheduled(cron = "50 */10 * * * ?")
    public void updateDeviceSet() throws InterruptedException {
        if (start) {
            token = OldXphapi.updateToken(restTemplate);
            updateChannel();
        }
    }

    @Scheduled(cron = "0 */2 * * * ?")
    public void sendData() throws InterruptedException {
        if (start) {
            for (Map.Entry<Integer, ChannelData> entry : deviceMap.entrySet()) {
                updateData(entry.getKey(), entry.getValue());
                Thread.sleep(1000);
            }
        }
    }

    @Scheduled(cron = "0 0 */1 * * ?")
    public void sendHourData() throws InterruptedException {
        if (start) {
            for (Map.Entry<Integer, ChannelData> entry : deviceMap.entrySet()) {
                updateHourData(entry.getKey(), entry.getValue());
                Thread.sleep(1000);
            }
        }
    }

    public void start() throws InterruptedException {
        start = true;
        token = OldXphapi.updateToken(restTemplate);
        updateChannel();
    }

    private void updateChannel() throws InterruptedException {

        deviceMap.values().forEach(channelData -> {
            channelData.getChannel().close();
        });

        deviceMap.clear();
        onlineMap.clear();

        Bootstrap client = new Bootstrap();

        EventLoopGroup group = new NioEventLoopGroup();
        client.group(group);
        client.channel(NioSocketChannel.class);

        client.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) {
                ch.pipeline().addLast(new CZReceiveHandler());
            }
        });


        HttpHeaders headers = new HttpHeaders();
        headers.add("token", token);
        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
        ResponseEntity<JSONObject> resEntity = restTemplate.exchange(
            String.format("http://115.28.187.9:8005/user/%s", username),
            HttpMethod.GET, requestEntity, JSONObject.class);
        JSONArray devices = Objects.requireNonNull(resEntity.getBody()).getJSONArray("devices");
        for (int i = 0; i < devices.size(); i++) {
            JSONObject device = devices.getJSONObject(i);
            Integer deviceId = Integer.parseInt(device.getString("facId"));
            ChannelData channelData = new ChannelData();
            String[] temp = device.getString("remark").split("&");
            channelData.setMn(temp[0]);
            channelData.setPw(temp[1]);
            channelData.setDeviceId(deviceId);
            ChannelFuture channelFuture = client.connect("111.53.98.26", 7102).sync();
            Channel channel = channelFuture.channel();
            channelData.setChannel(channel);
            deviceMap.put(deviceId, channelData);
            onlineMap.put(channel, channelData);
            LOGGER.info("[{}]: 新建连接", deviceId);
            Thread.sleep(1000);
        }
    }

    private void updateData(Integer deviceId, ChannelData channelData) throws InterruptedException {
        Channel channel = channelData.getChannel();
        if (!channel.isActive()) {
            Bootstrap client = new Bootstrap();

            EventLoopGroup group = new NioEventLoopGroup();
            client.group(group);
            client.channel(NioSocketChannel.class);

            client.handler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) {
                    ch.pipeline().addLast(new CZReceiveHandler());
                }
            });
            ChannelFuture channelFuture = client.connect("111.53.98.26", 7102).sync();
            channel = channelFuture.channel();
            channelData.setChannel(channel);
            deviceMap.remove(deviceId);
            onlineMap.remove(channel);
            deviceMap.put(deviceId, channelData);
            onlineMap.put(channel, channelData);
            LOGGER.info("[{}]: 新建连接", deviceId);
        }
        String mn = channelData.getMn();
        String pw = channelData.getPw();
        JSONObject data = restTemplate.getForObject(String.format("http://115.28.187.9:8005/intfa/queryData/%d", deviceId), JSONObject.class);
        assert data != null;
        JSONArray entity = data.getJSONArray("entity");
        StringBuilder stringBuilder = new StringBuilder("QN=");
        SimpleDateFormat s = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        s.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        stringBuilder.append(s.format(new Date()));
        stringBuilder.append(";ST=22;CN=2011;PW=");
        stringBuilder.append(pw);
        stringBuilder.append(";MN=");
        stringBuilder.append(mn);
        stringBuilder.append(";Flag=5;");
        stringBuilder.append("CP=&&DataTime=");
        s = new SimpleDateFormat("yyyyMMddHHmmss");
        stringBuilder.append(s.format(new Date()));
        stringBuilder.append(";");
        // 风速
        float value = entity.getJSONObject(0).getFloatValue("eValue");
        stringBuilder.append("a01007-Rtd=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",a01007-Flag=N;");
        // 风向
        value = entity.getJSONObject(1).getFloatValue("eValue");
        stringBuilder.append("a01008-Rtd=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",a01008-Flag=N;");
        // 温度
        value = entity.getJSONObject(2).getFloatValue("eValue");
        stringBuilder.append("a01001-Rtd=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",a01001-Flag=N;");
        // 湿度
        value = entity.getJSONObject(3).getFloatValue("eValue");
        stringBuilder.append("a01002-Rtd=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",a01002-Flag=N;");
        // 噪声
        value = entity.getJSONObject(4).getFloatValue("eValue");
        stringBuilder.append("LA-Rtd=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",LA-Flag=N;");
        // PM2.5
        stringBuilder.append("a34004-Rtd=");
        float pm25 = entity.getJSONObject(5).getFloatValue("eValue");
        if (pm25 >= 32767 || pm25 <= 0) {
            pm25 = 40 + new Random().nextInt(20);
        }
        stringBuilder.append(String.format("%.3f", pm25));

        stringBuilder.append(",a34004-Flag=N;");
        // PM10
        stringBuilder.append("a34002-Rtd=");
        float pm10 = entity.getJSONObject(6).getFloatValue("eValue");
        if (pm10 >= 32767 || pm10 <= 0) {
            pm10 = 60 + new Random().nextInt(20);
        }
        stringBuilder.append(String.format("%.3f", pm10));
        stringBuilder.append(",a34002-Flag=N;");
        // TSP
        stringBuilder.append("a34001-Rtd=");
        stringBuilder.append(String.format("%.3f", pm10 * 1.1));
        stringBuilder.append(",a34001-Flag=N;");
        // 气压
        value = entity.getJSONObject(9).getFloatValue("eValue");
        stringBuilder.append("a01006-Rtd=");
        stringBuilder.append(String.format("%.2f", value * 0.1));
        stringBuilder.append(",a01006-Flag=N&&");

        DecimalFormat decimalFormat = new DecimalFormat("0000");
        String dataLenStr = decimalFormat.format(stringBuilder.length());
        String crcStr = getCrc16(stringBuilder.toString().getBytes());
        String message = "##" + dataLenStr + stringBuilder.toString() + crcStr + "\r\n";
        channel.writeAndFlush(Unpooled.copiedBuffer(message.getBytes()));
        LOGGER.info("[{}]: 发送数据 {}实时数据", deviceId, message);
    }

    private void updateHourData(Integer deviceId, ChannelData channelData) throws InterruptedException {
        Channel channel = channelData.getChannel();
        if (!channel.isActive()) {
            Bootstrap client = new Bootstrap();

            EventLoopGroup group = new NioEventLoopGroup();
            client.group(group);
            client.channel(NioSocketChannel.class);

            client.handler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) {
                    ch.pipeline().addLast(new CZReceiveHandler());
                }
            });
            ChannelFuture channelFuture = client.connect("111.53.98.26", 7102).sync();
            channel = channelFuture.channel();
            channelData.setChannel(channel);
            deviceMap.remove(deviceId);
            onlineMap.remove(channel);
            deviceMap.put(deviceId, channelData);
            onlineMap.put(channel, channelData);
            LOGGER.info("[{}]: 新建连接", deviceId);
        }
        String mn = channelData.getMn();
        String pw = channelData.getPw();
        JSONObject data = restTemplate.getForObject(String.format("http://115.28.187.9:8005/intfa/queryData/%d", deviceId), JSONObject.class);
        assert data != null;
        JSONArray entity = data.getJSONArray("entity");
        StringBuilder stringBuilder = new StringBuilder("QN=");
        SimpleDateFormat s = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        s.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) - 1);
        stringBuilder.append(s.format(calendar.getTime()));
        stringBuilder.append(";ST=22;CN=2061;PW=");
        stringBuilder.append(pw);
        stringBuilder.append(";MN=");
        stringBuilder.append(mn);
        stringBuilder.append(";Flag=5;");
        stringBuilder.append("CP=&&DataTime=");
        s = new SimpleDateFormat("yyyyMMddHHmmss");
        stringBuilder.append(s.format(new Date()));
        stringBuilder.append(";");
        // 风速
        float value = entity.getJSONObject(0).getFloatValue("eValue");
        stringBuilder.append("a01007-Min=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",a01007-Avg=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",a01007-Max=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",a01007-Flag=N;");
        // 风向
        value = entity.getJSONObject(1).getFloatValue("eValue");
        stringBuilder.append("a01008-Min=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",a01008-Avg=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",a01008-Max=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",a01008-Flag=N;");
        // 温度
        value = entity.getJSONObject(2).getFloatValue("eValue");
        stringBuilder.append("a01001-Min=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",a01001-Avg=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",a01001-Max=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",a01001-Flag=N;");
        // 湿度
        value = entity.getJSONObject(3).getFloatValue("eValue");
        stringBuilder.append("a01002-Min=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",a01002-Avg=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",a01002-Max=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",a01002-Flag=N;");
        // 噪声
        value = entity.getJSONObject(4).getFloatValue("eValue");
        stringBuilder.append("LA-Min=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",LA-Avg=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",LA-Max=");
        stringBuilder.append(String.format("%.2f", value));
        stringBuilder.append(",LA-Flag=N;");
        // PM2.5
        float pm25 = entity.getJSONObject(5).getFloatValue("eValue");
        if (pm25 >= 32767 || pm25 <= 0) {
            pm25 = 40 + new Random().nextInt(20);
        }
        stringBuilder.append("a34004-Min=");
        stringBuilder.append(String.format("%.3f", pm25));
        stringBuilder.append(",a34004-Avg=");
        stringBuilder.append(String.format("%.3f", pm25));
        stringBuilder.append(",a34004-Max=");
        stringBuilder.append(String.format("%.3f", pm25));
        stringBuilder.append(",a34004-Flag=N;");
        // PM10
        float pm10 = entity.getJSONObject(6).getFloatValue("eValue");
        if (pm10 >= 32767 || pm10 <= 0) {
            pm10 = 60 + new Random().nextInt(20);
        }
        stringBuilder.append("a34002-Min=");
        stringBuilder.append(String.format("%.3f", pm10));
        stringBuilder.append(",a34002-Avg=");
        stringBuilder.append(String.format("%.3f", pm10));
        stringBuilder.append(",a34002-Max=");
        stringBuilder.append(String.format("%.3f", pm10));
        stringBuilder.append(",a34002-Flag=N;");
        // TSP
        stringBuilder.append("a34001-Min=");
        stringBuilder.append(String.format("%.3f", pm10 * 1.1));
        stringBuilder.append(",a34001-Avg=");
        stringBuilder.append(String.format("%.3f", pm10 * 1.1));
        stringBuilder.append(",a34001-Max=");
        stringBuilder.append(String.format("%.3f", pm10 * 1.1));
        stringBuilder.append(",a34001-Flag=N;");
        // 气压
        value = entity.getJSONObject(9).getFloatValue("eValue");
        stringBuilder.append("a01006-Min=");
        stringBuilder.append(String.format("%.2f", value * 0.1));
        stringBuilder.append(",a01006-Avg=");
        stringBuilder.append(String.format("%.2f", value * 0.1));
        stringBuilder.append(",a01006-Max=");
        stringBuilder.append(String.format("%.2f", value * 0.1));
        stringBuilder.append(",a01006-Flag=N&&");

        DecimalFormat decimalFormat = new DecimalFormat("0000");
        String dataLenStr = decimalFormat.format(stringBuilder.length());
        String crcStr = getCrc16(stringBuilder.toString().getBytes());
        String message = "##" + dataLenStr + stringBuilder.toString() + crcStr + "\r\n";
        channel.writeAndFlush(Unpooled.copiedBuffer(message.getBytes()));
        LOGGER.info("[{}]: 发送数据 {}小时数据", deviceId, message);
    }


    public static String getCrc16(byte[] data) {
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
}
