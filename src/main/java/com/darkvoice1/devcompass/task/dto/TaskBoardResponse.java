package com.darkvoice1.devcompass.task.dto;

import java.util.List;

/**
 * 项目任务看板响应数据。
 */
public class TaskBoardResponse {

    private Long projectId;
    private List<TaskBoardColumnResponse> columns;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public List<TaskBoardColumnResponse> getColumns() {
        return columns;
    }

    public void setColumns(List<TaskBoardColumnResponse> columns) {
        this.columns = columns;
    }
}
