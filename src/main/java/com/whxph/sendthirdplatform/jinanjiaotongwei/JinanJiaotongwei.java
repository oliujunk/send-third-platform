package com.whxph.sendthirdplatform.jinanjiaotongwei;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.whxph.sendthirdplatform.utils.HexBinDecOct;
import com.whxph.sendthirdplatform.utils.ModbusCrc16;
import com.whxph.sendthirdplatform.utils.NettyUtils;
import com.whxph.sendthirdplatform.utils.OldXphapi;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @author liujun
 * @description 济南交通委
 * @create 2019-11-25 17:41
 */
@Component
public class JinanJiaotongwei {

    private static final Logger LOGGER = LoggerFactory.getLogger(JinanJiaotongwei.class);

    private static Map<Integer, Channel> deviceMap = new ConcurrentHashMap<>();

    static Map<Channel, ChannelData> onlineMap = new ConcurrentHashMap<>();

    private boolean start = false;

    private String token = "";

    private String username = "jiaotongwei";

    private Set<Integer> deviceSet = new HashSet<>();

    @Resource
    private RestTemplate restTemplate;

    @Scheduled(cron = "50 0/10 * * * ?")
    public void updateDeviceList() throws InterruptedException {
        if (start) {
            token = OldXphapi.updateToken(restTemplate);
            deviceSet = OldXphapi.updateDevice(restTemplate, username, token);
            updateChannel();
        }
    }

    @Scheduled(cron = "30 0/2 * * * ?")
    public void sendHeartBeat() {
        if (start) {
            for (Map.Entry<Channel, ChannelData> entry : onlineMap.entrySet()) {
                heartBeat(entry.getValue().getDeviceId());
                LOGGER.info(String.format("[%d]: sendHeartBeat", entry.getValue().getDeviceId()));
            }
        }
    }

    @Scheduled(cron = "0 0/5 * * * ?")
    public void sendData() {
        if (start) {
            for (Map.Entry<Channel, ChannelData> entry : onlineMap.entrySet()) {
                monitorData(entry.getValue().getDeviceId());
                LOGGER.info(String.format("[%d]: sendData", entry.getValue().getDeviceId()));
            }
        }
    }

    public void start() throws InterruptedException {
        start = true;
        token = OldXphapi.updateToken(restTemplate);
        deviceSet = OldXphapi.updateDevice(restTemplate, username, token);
        updateChannel();
    }

    private void login(Integer deviceId) {

        Channel channel = deviceMap.get(deviceId);
        ChannelData channelData = onlineMap.get(channel);

        List<Byte> dataList = new ArrayList<>(64);
        // 报文头
        dataList.add((byte)0x68);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0x68);

        // 消息头
        // 消息ID
        dataList.add((byte)1);
        dataList.add((byte)0);
        // 消息体属性
        dataList.add((byte)0);
        dataList.add((byte)0);
        // 终端ID
        dataList.add((byte)0x03);
        dataList.add((byte)0x86);
        dataList.add(HexBinDecOct.hex2bcd((byte)(deviceId / 1000000)));
        dataList.add(HexBinDecOct.hex2bcd((byte)(deviceId / 10000 % 100)));
        dataList.add(HexBinDecOct.hex2bcd((byte)(deviceId / 100 % 100)));
        dataList.add(HexBinDecOct.hex2bcd((byte)(deviceId % 100)));
        // 消息流水号
        dataList.add((byte)(channelData.getSerialNumber() & 0x00FF));
        dataList.add((byte)(channelData.getSerialNumber() >> 8));
        channelData.setSerialNumber((short)(channelData.getSerialNumber() + 1));
        onlineMap.put(channel, channelData);
        // 消息体
        // 省域ID
        dataList.add((byte)37);
        dataList.add((byte)0);
        // 市县域ID
        dataList.add((byte)112);
        dataList.add((byte)0);
        // 制造商ID
        dataList.add((byte)1);
        dataList.add((byte)2);
        dataList.add((byte)3);
        dataList.add((byte)4);
        dataList.add((byte)5);
        // 终端型号
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        // 终端手机号码
        dataList.add((byte)0x01);
        dataList.add((byte)0x88);
        dataList.add((byte)0x53);
        dataList.add((byte)0x10);
        dataList.add((byte)0x72);
        dataList.add((byte)0x58);
        int dataListSize = dataList.size();
        byte[] sendData = new byte[dataListSize + 3];
        int i = 0;
        for (Byte data : dataList) {
            sendData[i] = data;
            i++;
        }
        sendData[1] = (byte)(dataListSize - 4);
        sendData[2] = (byte)((dataListSize - 4) >> 8);
        int checkSum = ModbusCrc16.crc16(sendData, 4, dataListSize - 4);
        sendData[dataListSize] = (byte)checkSum;
        sendData[dataListSize + 1] = (byte)(checkSum >> 8);
        sendData[dataListSize + 2] = (byte)0x16;
        NettyUtils.sendDataToChannel(sendData, channel);
    }

    private void heartBeat(Integer deviceId) {
        Channel channel = deviceMap.get(deviceId);
        ChannelData channelData = onlineMap.get(channel);

        List<Byte> dataList = new ArrayList<>(64);
        // 报文头
        dataList.add((byte)0x68);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0x68);

        // 消息头
        // 消息ID
        dataList.add((byte)2);
        dataList.add((byte)0);
        // 消息体属性
        dataList.add((byte)0x20);
        dataList.add((byte)0);
        // 终端ID
        dataList.add((byte)0x03);
        dataList.add((byte)0x86);
        dataList.add(HexBinDecOct.hex2bcd((byte)(deviceId / 1000000)));
        dataList.add(HexBinDecOct.hex2bcd((byte)(deviceId / 10000 % 100)));
        dataList.add(HexBinDecOct.hex2bcd((byte)(deviceId / 100 % 100)));
        dataList.add(HexBinDecOct.hex2bcd((byte)(deviceId % 100)));
        // 消息流水号
        dataList.add((byte)(channelData.getSerialNumber() & 0x00FF));
        dataList.add((byte)(channelData.getSerialNumber() >> 8));
        channelData.setSerialNumber((short)(channelData.getSerialNumber() + 1));
        onlineMap.put(channel, channelData);
        // 时间标签Tp
        LocalDateTime localDateTime = LocalDateTime.now();
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getYear() - 2000)));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getMonthValue())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getDayOfMonth())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getHour())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getMinute())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getSecond())));
        // 消息体
        // 故障标识
        dataList.add((byte)0);
        // 电池电压
        dataList.add((byte)0);
        dataList.add((byte)0);
        // 供电方式
        dataList.add((byte)0);
        int dataListSize = dataList.size();
        byte[] sendData = new byte[dataListSize + 3];
        int i = 0;
        for (Byte data : dataList) {
            sendData[i] = data;
            i++;
        }
        sendData[1] = (byte)(dataListSize - 4);
        sendData[2] = (byte)((dataListSize - 4) >> 8);
        int checkSum = ModbusCrc16.crc16(sendData, 4, dataListSize - 4);
        sendData[dataListSize] = (byte)checkSum;
        sendData[dataListSize + 1] = (byte)(checkSum >> 8);
        sendData[dataListSize + 2] = (byte)0x16;
        NettyUtils.sendDataToChannel(sendData, deviceMap.get(deviceId));
    }

    private void monitorData(Integer deviceId) {
        Channel channel = deviceMap.get(deviceId);
        ChannelData channelData = onlineMap.get(channel);
        JSONObject data = restTemplate.getForObject(String.format("http://115.28.187.9:8005/intfa/queryData/%d", deviceId), JSONObject.class);
        assert data != null;
        JSONArray entity = data.getJSONArray("entity");

        List<Byte> dataList = new ArrayList<>(128);
        // 报文头
        dataList.add((byte)0x68);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0x68);

        // 消息头
        // 消息ID
        dataList.add((byte)0x09);
        dataList.add((byte)0x45);
        // 消息体属性
        dataList.add((byte)0);
        dataList.add((byte)0x20);
        // 终端ID
        dataList.add((byte)0x03);
        dataList.add((byte)0x86);
        dataList.add(HexBinDecOct.hex2bcd((byte)(deviceId / 1000000)));
        dataList.add(HexBinDecOct.hex2bcd((byte)(deviceId / 10000 % 100)));
        dataList.add(HexBinDecOct.hex2bcd((byte)(deviceId / 100 % 100)));
        dataList.add(HexBinDecOct.hex2bcd((byte)(deviceId % 100)));
        // 消息流水号
        dataList.add((byte)(channelData.getSerialNumber() & 0x00FF));
        dataList.add((byte)(channelData.getSerialNumber() >> 8));
        channelData.setSerialNumber((short)(channelData.getSerialNumber() + 1));
        onlineMap.put(channel, channelData);
        // 时间标签Tp
        LocalDateTime localDateTime = LocalDateTime.now();
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getYear() - 2000)));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getMonthValue())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getDayOfMonth())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getHour())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getMinute())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getSecond())));
        // 消息体
        // 应答流水号
        dataList.add((byte)0);
        dataList.add((byte)0);
        // 设备时间
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getYear() - 2000)));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getMonthValue())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getDayOfMonth())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getHour())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getMinute())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getSecond())));
        // 主被动标志
        dataList.add((byte)1);
        // 保留
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        // token
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        // TSP
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        // PM2.5
        int temp = (int)(entity.getJSONObject(5).getFloatValue("eValue") * 10.0);
        dataList.add((byte)temp);
        dataList.add((byte)(temp >> 8));
        // PM10
        temp = (int)(entity.getJSONObject(6).getFloatValue("eValue") * 10.0);
        dataList.add((byte)temp);
        dataList.add((byte)(temp >> 8));
        // 噪声
        temp = (int)(entity.getJSONObject(4).getFloatValue("eValue") * 10.0);
        dataList.add((byte)temp);
        dataList.add((byte)(temp >> 8));
        // 风向
        temp = (int)(entity.getJSONObject(1).getFloatValue("eValue") * 10.0);
        dataList.add((byte)temp);
        dataList.add((byte)(temp >> 8));
        // 风速
        temp = (int)(entity.getJSONObject(0).getFloatValue("eValue") * 10.0);
        dataList.add((byte)temp);
        dataList.add((byte)(temp >> 8));
        // 温度
        temp = (int)(entity.getJSONObject(2).getFloatValue("eValue") * 10.0);
        if (temp >= 0) {
            dataList.add((byte)temp);
            dataList.add((byte)(temp >> 8));
        } else {
            temp = -temp;
            dataList.add((byte)temp);
            dataList.add((byte)((byte)(temp >> 8) + 0x80));
        }
        // 湿度
        temp = (int)(entity.getJSONObject(3).getFloatValue("eValue") * 10.0);
        dataList.add((byte)temp);
        dataList.add((byte)(temp >> 8));

        // 经纬度
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        // 采集起始时间
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.minusMinutes(5).getYear() - 2000)));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.minusMinutes(5).getMonthValue())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.minusMinutes(5).getDayOfMonth())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.minusMinutes(5).getHour())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.minusMinutes(5).getMinute())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.minusMinutes(5).getSecond())));
        // 采集终止时间
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getYear() - 2000)));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getMonthValue())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getDayOfMonth())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getHour())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getMinute())));
        dataList.add(HexBinDecOct.hex2bcd((byte)(localDateTime.getSecond())));
        // 保留
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        dataList.add((byte)0);
        int dataListSize = dataList.size();
        byte[] sendData = new byte[dataListSize + 3];
        int i = 0;
        for (Byte dataByte : dataList) {
            sendData[i] = dataByte;
            i++;
        }
        sendData[1] = (byte)(dataListSize - 4);
        sendData[2] = (byte)((dataListSize - 4) >> 8);
        int checkSum = ModbusCrc16.crc16(sendData, 4, dataListSize - 4);
        sendData[dataListSize] = (byte)checkSum;
        sendData[dataListSize + 1] = (byte)(checkSum >> 8);
        sendData[dataListSize + 2] = (byte)0x16;
        NettyUtils.sendDataToChannel(sendData, deviceMap.get(deviceId));
    }

    private void updateChannel() throws InterruptedException {
        Bootstrap client = new Bootstrap();

        EventLoopGroup group = new NioEventLoopGroup();
        client.group(group);
        client.channel(NioSocketChannel.class);

        client.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) {
                ch.pipeline().addLast(new ReceiveHandler());
            }
        });

        deviceMap.clear();
        onlineMap.clear();
        for (Integer deviceId : deviceSet) {
            ChannelFuture channelFuture = client.connect("221.214.51.19", 39105).sync();
            Channel channel = channelFuture.channel();
            deviceMap.put(deviceId, channel);
            ChannelData channelData = new ChannelData(deviceId, channel, (short)0, false);
            onlineMap.put(channel, channelData);
            login(deviceId);
        }
    }
}
