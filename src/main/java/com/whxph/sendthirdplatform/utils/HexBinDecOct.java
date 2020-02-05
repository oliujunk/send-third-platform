package com.whxph.sendthirdplatform.utils;

/**
 * @author liujun
 * @description 进制转换
 * @create 2019-11-26 14:10
 */
public class HexBinDecOct {

    /**
     * HEX码转成BCD码 例如 16 转成 0x16
     * @param hex HEX码
     * @return BCD码
     */
    public static byte hex2bcd(byte hex) {
        return (byte)(hex / 10 * 16 + hex % 10);
    }

}
