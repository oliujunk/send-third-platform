package com.whxph.sendthirdplatform.qingdaozhongfu;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author liujun
 * @description
 * @create 2019-11-26 15:05
 */
@Component
@ChannelHandler.Sharable
public class QDZFReceiveHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        QDZFReceiveHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        byte[] receiveByte = Unpooled.copiedBuffer((ByteBuf) msg).array();
        LOGGER.info(new String(receiveByte));
    }
}
