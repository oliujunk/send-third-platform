package com.whxph.sendthirdplatform;

import com.whxph.sendthirdplatform.changzhi.Changzhi;
import com.whxph.sendthirdplatform.henan.*;
import com.whxph.sendthirdplatform.jinanjiaotongwei.JinanJiaotongwei;
import javax.annotation.Resource;

import com.whxph.sendthirdplatform.qingdaojiutian.Qingdaojiutian;
import com.whxph.sendthirdplatform.zhonghuanongye.Zhonghuanongye;
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
    private Changzhi changzhi;

    @Resource
    private Qingdaojiutian qingdaojiutian;

    @Resource
    private UpdateStandrdData updateStandrdData;

    @Resource
    private Zhonghuanongye zhonghuanongye;

    public static void main(String[] args) {
        SpringApplication.run(SendThirdPlatformApplication.class, args);
    }

    public static StandardData standardData;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        updateStandrdData.updateStandrdData();
        jinanJiaotongwei.start();
        changzhi.start();
        qingdaojiutian.start();
        zhonghuanongye.start();
    }
}
