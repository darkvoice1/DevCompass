package com.darkvoice1.devcompass.importexport.controller;

import java.nio.charset.StandardCharsets;

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
 * 提供项目 JSON 和任务 CSV 导出接口。
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}")
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
    @GetMapping("/export")
    public ResponseEntity<ProjectExportFile> exportProject(@PathVariable Long projectId) {
        ProjectExportFile file = projectExportService.exportProject(projectId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"project-" + projectId + ".json\"")
                .body(file);
    }

    /**
     * 把一个项目的任务导出成 CSV 文件。
     *
     * @param projectId 项目主键
     * @return CSV 文件
     */
    @GetMapping("/tasks/export")
    public ResponseEntity<String> exportTasks(@PathVariable Long projectId) {
        String csv = projectExportService.exportTasksCsv(projectId);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"project-" + projectId + "-tasks.csv\"")
                .body(csv);
    }
}
