package com.whxph.sendthirdplatform;

import com.whxph.sendthirdplatform.changzhi.Changzhi;
import com.whxph.sendthirdplatform.guowangdianli.Guowangdianli;
import com.whxph.sendthirdplatform.henan.*;
import com.whxph.sendthirdplatform.jiangxiyangchen.Jiangxiyangchen;
import com.whxph.sendthirdplatform.jinancj.Jinancj;
import com.whxph.sendthirdplatform.jinanjiaotongwei.JinanJiaotongwei;

import javax.annotation.Resource;

import com.whxph.sendthirdplatform.sichuanweikuang.Sichuanweikuang;
import com.whxph.sendthirdplatform.zhonghuanongye.Zhonghuanongye;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author liujun
 */
@EnableScheduling
@SpringBootApplication
@EnableJms
public class SendThirdPlatformApplication implements ApplicationRunner {

    @Resource
    private JinanJiaotongwei jinanJiaotongwei;

    @Resource
    private Jinancj jinancj;

    @Resource
    private UpdateStandrdData updateStandrdData;

    @Resource
    private Zhonghuanongye zhonghuanongye;

    @Resource
    private Sichuanweikuang sichuanweikuang;

    @Resource
    private Changzhi changzhi;

    @Resource
    private Jiangxiyangchen jiangxiyangchen;

    public static void main(String[] args) {
        SpringApplication.run(SendThirdPlatformApplication.class, args);
    }

    public static StandardData standardData;

    @Resource
    private Guowangdianli guowangdianli;

    @Override
    public void run(ApplicationArguments args) throws Exception {
//        updateStandrdData.updateStandrdData();
//
//        sichuanweikuang.start();
//
//        changzhi.start();

//        new Thread(() -> {
//            try {
//                jinanJiaotongwei.start();
//                jinancj.start();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }).start();
//
//        new Thread(() -> zhonghuanongye.start()).start();
//
//        jiangxiyangchen.start();

        guowangdianli.start();
    }
}
