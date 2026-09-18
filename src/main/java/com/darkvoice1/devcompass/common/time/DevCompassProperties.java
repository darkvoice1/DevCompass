package com.darkvoice1.devcompass.common.time;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用自定义配置。
 */
@ConfigurationProperties(prefix = "devcompass")
public class DevCompassProperties {

    /**
     * 计算日期边界时使用的时区。
     */
    private String timezone = "Asia/Shanghai";

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
