package com.whxph.sendthirdplatform.utils;

/**
 * @author liujun
 * @description
 * @create 2019-11-26 14:04
 */
public class ModbusCrc16 {

    public static final int BYTE_MASK = 0xFF;

    private static final int CRC_STRING_LENGTH = 4;

    private static final int BTYE_BIT = 8;

    private static final int MIN_FRAME_LENGTH = 3;

    public static String getCrc16(byte[] buf) {
        byte[] pcrc = new byte[2];

        int crc = crc16(buf, 0, buf.length);

        pcrc[0] = (byte)(crc & 0x00FF);
        pcrc[1] = (byte)(crc >> 8);
        //字节低前高后组合
        String retCrc = Integer.toHexString((int)pcrc[0] & 0x000000ff) + Integer.toHexString((int)pcrc[1] & 0x000000ff);
        if (retCrc.length() != CRC_STRING_LENGTH){
            retCrc = fillCrc16(pcrc);
        }

        return retCrc.toUpperCase();
    }

    public static int crc16(byte[] buf, int offset, int len) {
        int crc = 0xFFFF;
        int polynomial = 0xA001;

        if (len == 0) { return 0; }

        for (int i = offset; i < len + offset; i++) {
            crc ^= ((int) buf[i] & 0x00FF);
            for (int j = 0; j < BTYE_BIT; j++) {
                if ((crc & 0x0001) != 0) {
                    crc >>= 1;
                    crc ^= polynomial;
                } else {
                    crc >>= 1;
                }
            }
        }

        return crc;
    }

    public static boolean testFrame(byte[] buf, int len) {
        int checkSum;
        if (len < MIN_FRAME_LENGTH) {
            return false;
        }
        checkSum = crc16(buf, 0, len - 2);

        return (buf[buf.length-2] & BYTE_MASK) == ( checkSum & 0x00FF) && (buf[buf.length-1] & BYTE_MASK) == (checkSum >> 8);
    }

    private static String fillCrc16(byte[] pcrc) {
        String crc1 = Integer.toHexString((int)pcrc[0] & 0x000000ff);
        String crc2 = Integer.toHexString((int)pcrc[1] & 0x000000ff);
        if (crc1.length() == 1) {
            crc1 = "0" + crc1;
        }

        if (crc2.length() == 1) {
            crc2 = "0" + crc2;
        }

        return crc1 + crc2;
    }

}
