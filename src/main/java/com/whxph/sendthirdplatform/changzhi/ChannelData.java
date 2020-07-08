package com.whxph.sendthirdplatform.changzhi;

import io.netty.channel.Channel;
import lombok.Data;

@Data
public class ChannelData {
    private Channel channel;
    private String mn;
    private String pw;
    private Integer deviceId;
}
