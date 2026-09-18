package com.darkvoice1.devcompass.common.time;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 提供应用统一时区和时钟。
 */
@Configuration
@EnableConfigurationProperties(DevCompassProperties.class)
public class DateTimeConfig {

    /**
     * 根据配置创建时区，日期边界都按这个时区计算。
     *
     * @param properties 应用自定义配置
     * @return 应用时区
     */
    @Bean
    public ZoneId appZoneId(DevCompassProperties properties) {
        return ZoneId.of(properties.getTimezone());
    }

    /**
     * 创建跟随应用时区的时钟，便于业务和测试使用同一套“今天”。
     *
     * @param appZoneId 应用时区
     * @return 应用时钟
     */
    @Bean
    public Clock appClock(ZoneId appZoneId) {
        return Clock.system(appZoneId);
    }
}
