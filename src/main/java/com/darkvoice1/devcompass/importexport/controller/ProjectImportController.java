package com.darkvoice1.devcompass.importexport.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.darkvoice1.devcompass.common.web.ApiResponse;
import com.darkvoice1.devcompass.importexport.dto.ProjectExportFile;
import com.darkvoice1.devcompass.importexport.dto.ProjectImportResponse;
import com.darkvoice1.devcompass.importexport.service.ProjectImportService;

/**
 * 提供项目 JSON 导入接口。
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectImportController {

    private final ProjectImportService projectImportService;

    /**
     * 创建项目导入控制器。
     *
     * @param projectImportService 项目导入服务
     */
    public ProjectImportController(ProjectImportService projectImportService) {
        this.projectImportService = projectImportService;
    }

    /**
     * 导入一份项目 JSON。请求体就是导出文件的内容。
     *
     * @param file 导出文件内容
     * @return 新项目编号和跳过的附件数量
     */
    @PostMapping("/import")
    public ApiResponse<ProjectImportResponse> importProject(@RequestBody ProjectExportFile file) {
        return ApiResponse.success(projectImportService.importProject(file));
    }
}
