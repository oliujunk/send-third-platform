package com.whxph.sendthirdplatform;

import com.whxph.sendthirdplatform.henan.*;
import com.whxph.sendthirdplatform.jinanjiaotongwei.JinanJiaotongwei;
import com.whxph.sendthirdplatform.qingdaojiutian.Qingdaojiutian;
import com.whxph.sendthirdplatform.qingdaozhongfu.Qingdaozhongfu;
import javax.annotation.Resource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author liujun
 */
@EnableScheduling
@SpringBootApplication
public class SendThirdPlatformApplication implements ApplicationRunner {

    @Resource
    private JinanJiaotongwei jinanJiaotongwei;

    @Resource
    private Qingdaojiutian qingdaojiutian;

    @Resource
    private Qingdaozhongfu qingdaozhongfu;

    @Resource
    private UpdateStandrdData updateStandrdData;

    @Resource
    private Guokong guokong;

    @Resource
    private Gkgridding gkgridding;

    @Resource
    private Hnhebi hnhebi;

    @Resource
    private Kaifeng kaifeng;

    @Resource
    private Nanyang nanyang;

    @Resource
    private Pingdingshan pingdingshan;

    @Resource
    private Nanyang2019 nanyang2019;

    @Resource
    private Zhumadian zhumadian;

    @Resource
    private Zhatucang zhatucang;

    public static void main(String[] args) {
        SpringApplication.run(SendThirdPlatformApplication.class, args);
    }

    public static StandardData standardData;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        updateStandrdData.updateStandrdData();
        jinanJiaotongwei.start();
        qingdaojiutian.start();
        qingdaozhongfu.start();

        guokong.start();
        gkgridding.start();
        hnhebi.start();
        kaifeng.start();
        nanyang.start();
        nanyang2019.start();
        pingdingshan.start();
        zhumadian.start();
        zhatucang.start();
    }
}
