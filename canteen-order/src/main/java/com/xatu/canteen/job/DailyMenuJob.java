package com.xatu.canteen.job;

import com.xatu.canteen.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每天 00:10 自动为全部上架菜品生成当日菜单（已存在则跳过）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyMenuJob {

    private final MenuService menuService;

    @Scheduled(cron = "0 10 0 * * ?")
    public void initTodayMenu() {
        log.info("开始初始化今日菜单");
        menuService.initTodayIfAbsent();
    }
}
