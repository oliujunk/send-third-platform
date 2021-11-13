package com.whxph.sendthirdplatform.jinancj;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.whxph.sendthirdplatform.jinancj.Jinancj.onlineMap;
import static com.whxph.sendthirdplatform.utils.ModbusCrc16.BYTE_MASK;

/**
 * @author liujun
 * @description
 * @create 2019-11-26 15:05
 */
@Component
@ChannelHandler.Sharable
public class CJReceiveHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CJReceiveHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        byte[] receiveByte = Unpooled.copiedBuffer((ByteBuf) msg).array();
        ChannelData channelData = onlineMap.get(ctx.channel());
        StringBuilder str = new StringBuilder();
        for (byte b : receiveByte) {
            str.append(String.format("%02X", b));
        }
        LOGGER.info(str.toString());

        if ((receiveByte[4] & BYTE_MASK) == 0x01 && (receiveByte[5] & BYTE_MASK) == 0x80) {
            // 登录应答
            if ((receiveByte[8] & BYTE_MASK) == 0 || (receiveByte[8] & BYTE_MASK) == 1 || (receiveByte[8] & BYTE_MASK) == 3) {
                channelData.setOnline(true);
                onlineMap.put(ctx.channel(), channelData);
                LOGGER.info(String.format("[%d]: 登陆成功！", channelData.getDeviceId()));
            }

        } else if ((receiveByte[4] & BYTE_MASK) == 0x00 && (receiveByte[5] & BYTE_MASK) == 0x80) {
            // 通用应答
            LOGGER.info(String.format("[%d]: 心跳或数据上报成功！", channelData.getDeviceId()));
        }
        super.channelRead(ctx, msg);
    }
}
