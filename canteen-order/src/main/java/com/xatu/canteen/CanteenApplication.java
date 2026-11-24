package com.xatu.canteen;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园食堂网上订餐系统启动类。
 *
 * @author XATU
 */
@EnableScheduling
@MapperScan("com.xatu.canteen.mapper")
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CanteenApplication {

    public static void main(String[] args) {
        SpringApplication.run(CanteenApplication.class, args);
    }
}
