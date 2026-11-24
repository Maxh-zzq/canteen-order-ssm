package com.xatu.canteen.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 业务配置，全部集中在 application.yml 的 canteen 节点下。
 */
@Data
@Component
@ConfigurationProperties(prefix = "canteen")
public class CanteenProperties {

    private Jwt jwt = new Jwt();
    private Order order = new Order();
    private Captcha captcha = new Captcha();
    private FileConfig file = new FileConfig();
    private Cors cors = new Cors();
    private Pay pay = new Pay();
    private Long workerId = 1L;

    @Data
    public static class Jwt {
        private String secret;
        private long accessTokenSeconds = 1800L;
        private long refreshTokenSeconds = 604800L;
        private boolean singleLogin = false;
    }

    @Data
    public static class Order {
        private int timeoutMinutes = 15;
    }

    @Data
    public static class Captcha {
        private boolean enabled = false;
        private int expireSeconds = 120;
    }

    @Data
    public static class FileConfig {
        private String uploadDir = "./uploads";
        private String accessPrefix = "/uploads/";
        private String allowedExtensions = "jpg,jpeg,png,webp";
        private int maxSizeMb = 5;
    }

    @Data
    public static class Cors {
        private String allowedOrigins = "*";
    }

    @Data
    public static class Pay {
        /** 模拟网关签名密钥 */
        private String secret = "canteen-pay-secret";
    }
}
