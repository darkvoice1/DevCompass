package com.darkvoice1.devcompass.importexport.dto;

/**
 * 项目 JSON 导入后的结果。
 */
public class ProjectImportResponse {

    private Long projectId;

    private String projectName;

    private int skippedAttachmentCount;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public int getSkippedAttachmentCount() {
        return skippedAttachmentCount;
    }

    public void setSkippedAttachmentCount(int skippedAttachmentCount) {
        this.skippedAttachmentCount = skippedAttachmentCount;
    }
}
