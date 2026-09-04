package com.darkvoice1.devcompass.common.health;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.darkvoice1.devcompass.common.web.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 提供应用基础健康检查接口。
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "系统健康", description = "应用基础健康检查接口")
public class HealthController {

    /**
     * 返回应用当前可用状态。
     *
     * @return 包含应用状态的响应
     */
    @GetMapping
    @Operation(summary = "检查应用健康状态", description = "返回应用当前是否可用")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "UP"));
    }
}
