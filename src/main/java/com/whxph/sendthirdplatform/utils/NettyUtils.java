package com.whxph.sendthirdplatform.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

/**
 * @author liujun
 * @description
 * @create 2019-11-26 14:39
 */
public class NettyUtils {
    public static void sendDataToChannel(byte[] data, Channel channel) {
        ByteBuf out = Unpooled.buffer(data.length);
        String msg = ByteBufUtil.hexDump(data);
        out.writeBytes(ByteBufUtil.decodeHexDump(msg));
        channel.writeAndFlush(out);
    }
}
