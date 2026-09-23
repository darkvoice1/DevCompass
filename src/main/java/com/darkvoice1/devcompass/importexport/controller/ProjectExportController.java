package com.darkvoice1.devcompass.importexport.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.darkvoice1.devcompass.importexport.dto.ProjectExportFile;
import com.darkvoice1.devcompass.importexport.service.ProjectExportService;

/**
 * 提供项目 JSON 导出接口。
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectExportController {

    private final ProjectExportService projectExportService;

    /**
     * 创建项目导出控制器。
     *
     * @param projectExportService 项目导出服务
     */
    public ProjectExportController(ProjectExportService projectExportService) {
        this.projectExportService = projectExportService;
    }

    /**
     * 把一个项目导出成 JSON 文件。
     *
     * @param projectId 项目主键
     * @return JSON 文件
     */
    @GetMapping("/{projectId}/export")
    public ResponseEntity<ProjectExportFile> exportProject(@PathVariable Long projectId) {
        ProjectExportFile file = projectExportService.exportProject(projectId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"project-" + projectId + ".json\"")
                .body(file);
    }
}
