package com.xatu.canteen.config;

import lombok.extern.slf4j.Slf4j;
import com.xatu.canteen.common.CanteenProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静态资源映射：把本地上传目录暴露为 /uploads/**。
 */
@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CanteenProperties properties;

    public WebMvcConfig(CanteenProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = properties.getFile().getUploadDir();
        Path root = Paths.get(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            log.error("创建上传目录失败: {}", root, e);
        }
        String location = "file:" + root.toString().replace("\\", "/") + "/";
        String pattern = properties.getFile().getAccessPrefix();
        if (!pattern.endsWith("/")) {
            pattern = pattern + "/";
        }
        registry.addResourceHandler(pattern + "**").addResourceLocations(location);
        log.info("上传目录映射: {} -> {}", pattern + "**", location);
    }
}
