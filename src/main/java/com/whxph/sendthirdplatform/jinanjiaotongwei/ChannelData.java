package com.whxph.sendthirdplatform.jinanjiaotongwei;

import io.netty.channel.Channel;
import lombok.Data;

/**
 * @author liujun
 * @description
 * @create 2019-11-26 16:34
 */
@Data
class ChannelData {
    private Integer deviceId;
    private Channel channel;
    private Short serialNumber;
    private boolean online;
    private String deviceName;

    ChannelData(Integer deviceId, Channel channel, Short serialNumber, boolean online, String deviceName) {
        this.deviceId = deviceId;
        this.channel = channel;
        this.serialNumber = serialNumber;
        this.online = online;
        this.deviceName = deviceName;
    }
}
