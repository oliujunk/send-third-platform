package com.whxph.sendthirdplatform.changzhi;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.whxph.sendthirdplatform.changzhi.Changzhi.getCrc16;
import static com.whxph.sendthirdplatform.changzhi.Changzhi.onlineMap;

/**
 * @author liujun
 * @description
 * @create 2019-11-26 15:05
 */
@Component
@ChannelHandler.Sharable
public class CZReceiveHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CZReceiveHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        byte[] receiveByte = Unpooled.copiedBuffer((ByteBuf) msg).array();
        ChannelData channelData = onlineMap.get(ctx.channel());
        Integer deviceId = channelData.getDeviceId();
        String pw = channelData.getPw();
        String mn = channelData.getMn();
        String receiveMsg = new String(receiveByte);
        LOGGER.info("[{}]: 接收数据 {}", deviceId, receiveMsg);
        String qn = receiveMsg.substring(9, 26);
        String cn = receiveMsg.substring(36, 40);
        switch (cn) {
            case "1011":
                StringBuilder stringBuilder = new StringBuilder("QN=");
                stringBuilder.append(qn);
                stringBuilder.append(";ST=91;");
                stringBuilder.append("CN=9011;MN=");
                stringBuilder.append(mn);
                stringBuilder.append(";PW=");
                stringBuilder.append(pw);
                stringBuilder.append(";Flag=4;CP=&&QnRtn=1&&");
                DecimalFormat decimalFormat = new DecimalFormat("0000");
                String dataLenStr = decimalFormat.format(stringBuilder.length());
                String crcStr = getCrc16(stringBuilder.toString().getBytes());
                String message = "##" + dataLenStr + stringBuilder.toString() + crcStr + "\r\n";
                ctx.channel().writeAndFlush(Unpooled.copiedBuffer(message.getBytes()));
                LOGGER.info("[{}]: 发送数据 {}", deviceId, message);

                stringBuilder = new StringBuilder("QN=");
                stringBuilder.append(qn);
                stringBuilder.append(";ST=22;");
                stringBuilder.append("CN=1011;MN=");
                stringBuilder.append(mn);
                stringBuilder.append(";PW=");
                stringBuilder.append(pw);
                stringBuilder.append(";Flag=4;CP=&&SystemTime=");
                SimpleDateFormat s = new SimpleDateFormat("yyyyMMddHHmmss");
                stringBuilder.append(s.format(new Date()));
                stringBuilder.append("&&");
                dataLenStr = decimalFormat.format(stringBuilder.length());
                crcStr = getCrc16(stringBuilder.toString().getBytes());
                message = "##" + dataLenStr + stringBuilder.toString() + crcStr + "\r\n";
                ctx.channel().writeAndFlush(Unpooled.copiedBuffer(message.getBytes()));
                LOGGER.info("[{}]: 发送数据 {}", deviceId, message);

                stringBuilder = new StringBuilder("QN=");
                stringBuilder.append(qn);
                stringBuilder.append(";ST=91;");
                stringBuilder.append("CN=9012;MN=");
                stringBuilder.append(mn);
                stringBuilder.append(";PW=");
                stringBuilder.append(pw);
                stringBuilder.append(";Flag=4;CP=&&ExeRtn=1&&");
                dataLenStr = decimalFormat.format(stringBuilder.length());
                crcStr = getCrc16(stringBuilder.toString().getBytes());
                message = "##" + dataLenStr + stringBuilder.toString() + crcStr + "\r\n";
                ctx.channel().writeAndFlush(Unpooled.copiedBuffer(message.getBytes()));
                LOGGER.info("[{}]: 发送数据 {}提取时间完成", deviceId, message);
                break;
            case "1100":
                StringBuilder stringBuilder1 = new StringBuilder("QN=");
                stringBuilder1.append(qn);
                stringBuilder1.append(";ST=91;");
                stringBuilder1.append("CN=9011;MN=");
                stringBuilder1.append(mn);
                stringBuilder1.append(";PW=");
                stringBuilder1.append(pw);
                stringBuilder1.append(";Flag=4;CP=&&QnRtn=1&&");
                DecimalFormat decimalFormat1 = new DecimalFormat("0000");
                String dataLenStr1 = decimalFormat1.format(stringBuilder1.length());
                String crcStr1 = getCrc16(stringBuilder1.toString().getBytes());
                String message1 = "##" + dataLenStr1 + stringBuilder1.toString() + crcStr1 + "\r\n";
                ctx.channel().writeAndFlush(Unpooled.copiedBuffer(message1.getBytes()));
                LOGGER.info("[{}]: 发送数据 {}心跳", deviceId, message1);
                break;
            default:
                break;
        }
    }
}
