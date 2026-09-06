package com.darkvoice1.devcompass.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.task.dto.CreateTaskRequest;
import com.darkvoice1.devcompass.task.dto.UpdateTaskRequest;
import com.darkvoice1.devcompass.task.entity.Task;
import com.darkvoice1.devcompass.task.entity.TaskPriority;
import com.darkvoice1.devcompass.task.entity.TaskStatus;
import com.darkvoice1.devcompass.task.repository.TaskMapper;
import com.darkvoice1.devcompass.task.service.TaskService;

/**
 * 验证任务创建和编辑业务。
 */
class TaskServiceTest {

    private TaskMapper taskMapper;
    private ProjectMapper projectMapper;
    private TaskService taskService;

    /**
     * 初始化任务服务及 Mapper 模拟对象。
     */
    @BeforeEach
    void setUp() {
        taskMapper = mock(TaskMapper.class);
        projectMapper = mock(ProjectMapper.class);
        taskService = new TaskService(taskMapper, projectMapper);
    }

    /**
     * 验证创建任务时使用默认状态和优先级。
     */
    @Test
    void shouldCreateTaskWithDefaultStatusAndPriority() {
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        doAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(10L);
            return 1;
        }).when(taskMapper).insert(any(Task.class));

        CreateTaskRequest request = new CreateTaskRequest();
        request.setProjectId(1L);
        request.setTitle("实现任务接口");

        var response = taskService.createTask(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(response.getPriority()).isEqualTo(TaskPriority.MEDIUM);
    }

    /**
     * 验证编辑任务字段并刷新更新时间。
     */
    @Test
    void shouldUpdateTaskFields() {
        Task task = new Task();
        task.setId(10L);
        task.setTitle("旧标题");
        when(taskMapper.selectById(10L)).thenReturn(task);

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("新标题");
        request.setStatus(TaskStatus.COMPLETED);

        var response = taskService.updateTask(10L, request);

        assertThat(response.getTitle()).isEqualTo("新标题");
        assertThat(response.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(taskMapper).updateById(task);
    }

}
