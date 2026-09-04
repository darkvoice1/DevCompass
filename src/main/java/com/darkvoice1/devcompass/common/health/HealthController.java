package com.darkvoice1.devcompass.common.health;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供应用基础健康检查接口。
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    /**
     * 返回应用当前可用状态。
     *
     * @return 包含应用状态的响应
     */
    @GetMapping
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
