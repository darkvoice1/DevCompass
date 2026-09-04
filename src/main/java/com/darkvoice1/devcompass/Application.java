package com.darkvoice1.devcompass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DevCompass 的 Spring Boot 启动类。
 */
@SpringBootApplication
public class Application {

    /**
     * 启动 DevCompass 应用。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
