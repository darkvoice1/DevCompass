package com.darkvoice1.devcompass.task.dto;

import java.util.List;

import com.darkvoice1.devcompass.task.entity.TaskStatus;

/**
 * 任务看板中的单个状态列。
 */
public class TaskBoardColumnResponse {

    private TaskStatus status;
    private List<TaskDetailResponse> tasks;
    private int count;

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public List<TaskDetailResponse> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskDetailResponse> tasks) {
        this.tasks = tasks;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
