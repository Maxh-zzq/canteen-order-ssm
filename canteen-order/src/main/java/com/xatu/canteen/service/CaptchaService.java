package com.xatu.canteen.service;

import com.xatu.canteen.common.CanteenProperties;
import com.xatu.canteen.common.RedisKeys;
import com.xatu.canteen.vo.CaptchaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 图形验证码。口令存 Redis，一次性使用；Redis 故障时降级放行（验证码默认关闭）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;

    private final StringRedisTemplate redis;
    private final CanteenProperties properties;

    public CaptchaVO generate() {
        String code = randomCode(4);
        String key = UUID.randomUUID().toString().replace("-", "");
        try {
            redis.opsForValue().set(RedisKeys.captcha(key), code,
                    Duration.ofSeconds(properties.getCaptcha().getExpireSeconds()));
        } catch (Exception e) {
            log.error("验证码写入 Redis 失败: {}", e.getMessage());
        }
        return new CaptchaVO(key, "data:image/png;base64," + render(code));
    }

    public boolean verify(String key, String input) {
        if (!properties.getCaptcha().isEnabled()) {
            return true;
        }
        if (!StringUtils.hasText(key) || !StringUtils.hasText(input)) {
            return false;
        }
        try {
            String code = redis.opsForValue().get(RedisKeys.captcha(key));
            redis.delete(RedisKeys.captcha(key));
            return code != null && code.equalsIgnoreCase(input.trim());
        } catch (Exception e) {
            log.error("验证码校验异常，降级放行: {}", e.getMessage());
            return true;
        }
    }

    private String randomCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(CHARS.charAt(ThreadLocalRandom.current().nextInt(CHARS.length())));
        }
        return builder.toString();
    }

    private String render(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(245, 247, 250));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            for (int i = 0; i < 8; i++) {
                g.setColor(randomColor(150, 220));
                int x1 = ThreadLocalRandom.current().nextInt(WIDTH);
                int y1 = ThreadLocalRandom.current().nextInt(HEIGHT);
                int x2 = ThreadLocalRandom.current().nextInt(WIDTH);
                int y2 = ThreadLocalRandom.current().nextInt(HEIGHT);
                g.drawLine(x1, y1, x2, y2);
            }
            g.setFont(new Font("Arial", Font.BOLD, 26));
            for (int i = 0; i < code.length(); i++) {
                g.setColor(randomColor(30, 120));
                g.drawString(String.valueOf(code.charAt(i)), 18 + i * 25, 30 + ThreadLocalRandom.current().nextInt(5));
            }
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("验证码生成失败", e);
        }
    }

    private Color randomColor(int min, int max) {
        int bound = max - min;
        return new Color(
                min + ThreadLocalRandom.current().nextInt(bound),
                min + ThreadLocalRandom.current().nextInt(bound),
                min + ThreadLocalRandom.current().nextInt(bound));
    }
}
