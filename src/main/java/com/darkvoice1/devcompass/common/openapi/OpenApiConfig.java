package com.darkvoice1.devcompass.common.openapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * 配置 DevCompass 的 OpenAPI 基础信息。
 */
@Configuration
public class OpenApiConfig {

    /**
     * 创建 OpenAPI 文档元数据。
     *
     * @return OpenAPI 配置对象
     */
    @Bean
    public OpenAPI devCompassOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("DevCompass API")
                .version("v1")
                .description("个人多项目研发管理平台接口文档"));
    }
}
