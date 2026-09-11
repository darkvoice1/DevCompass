package com.darkvoice1.devcompass.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.entity.ProjectPhase;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.project.repository.ProjectPhaseMapper;
import com.darkvoice1.devcompass.task.dto.CreateTaskRequest;
import com.darkvoice1.devcompass.task.dto.ChangeTaskStatusRequest;
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
    private ProjectPhaseMapper projectPhaseMapper;
    private TaskService taskService;

    /**
     * 初始化任务服务及 Mapper 模拟对象。
     */
    @BeforeEach
    void setUp() {
        taskMapper = mock(TaskMapper.class);
        projectMapper = mock(ProjectMapper.class);
        projectPhaseMapper = mock(ProjectPhaseMapper.class);
        taskService = new TaskService(taskMapper, projectMapper, projectPhaseMapper);
    }

    /**
     * 验证新建任务固定使用待办状态，并使用默认优先级。
     */
    @Test
    void shouldCreateTaskWithTodoStatusAndDefaultPriority() {
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        when(projectPhaseMapper.selectById(2L)).thenReturn(phase(2L, 1L, "开发实现"));
        doAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(10L);
            return 1;
        }).when(taskMapper).insert(any(Task.class));

        CreateTaskRequest request = new CreateTaskRequest();
        request.setProjectId(1L);
        request.setPhaseId(2L);
        request.setTitle("实现任务接口");

        var response = taskService.createTask(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(response.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(response.getPhaseId()).isEqualTo(2L);
        assertThat(response.getPhaseName()).isEqualTo("开发实现");
    }

    /**
     * 验证编辑任务字段并刷新更新时间。
     */
    @Test
    void shouldUpdateTaskFields() {
        Task task = new Task();
        task.setId(10L);
        task.setProjectId(1L);
        task.setPhaseId(2L);
        task.setTitle("旧标题");
        task.setStatus(TaskStatus.TODO);
        when(taskMapper.selectById(10L)).thenReturn(task);
        when(projectPhaseMapper.selectById(2L)).thenReturn(phase(2L, 1L, "开发实现"));

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("新标题");

        var response = taskService.updateTask(10L, request);

        assertThat(response.getTitle()).isEqualTo("新标题");
        assertThat(response.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(taskMapper).updateById(task);
    }

    /**
     * 验证合法状态流转会更新状态和更新时间。
     */
    @Test
    void shouldChangeTaskStatus() {
        Task task = task(10L, TaskStatus.TODO);
        when(taskMapper.selectById(10L)).thenReturn(task);
        when(projectPhaseMapper.selectById(2L)).thenReturn(phase(2L, 1L, "开发实现"));
        when(taskMapper.updateStatusIfCurrent(any(), any(), any(), any())).thenReturn(1);

        ChangeTaskStatusRequest request = new ChangeTaskStatusRequest();
        request.setTargetStatus(TaskStatus.IN_PROGRESS);

        var response = taskService.changeTaskStatus(10L, request);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(taskMapper).updateStatusIfCurrent(
                10L, TaskStatus.TODO, TaskStatus.IN_PROGRESS, response.getUpdatedAt());
    }

    /**
     * 验证重复提交相同状态时直接返回，不更新数据库。
     */
    @Test
    void shouldTreatSameStatusChangeAsIdempotent() {
        Task task = task(10L, TaskStatus.TODO);
        when(taskMapper.selectById(10L)).thenReturn(task);
        when(projectPhaseMapper.selectById(2L)).thenReturn(phase(2L, 1L, "开发实现"));

        ChangeTaskStatusRequest request = new ChangeTaskStatusRequest();
        request.setTargetStatus(TaskStatus.TODO);

        var response = taskService.changeTaskStatus(10L, request);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.TODO);
        verifyNoInteractions(projectMapper);
        org.mockito.Mockito.verify(taskMapper, org.mockito.Mockito.never())
                .updateStatusIfCurrent(any(), any(), any(), any());
    }

    /**
     * 验证非法状态流转会被拒绝。
     */
    @Test
    void shouldRejectIllegalTaskStatusChange() {
        Task task = task(10L, TaskStatus.COMPLETED);
        when(taskMapper.selectById(10L)).thenReturn(task);

        ChangeTaskStatusRequest request = new ChangeTaskStatusRequest();
        request.setTargetStatus(TaskStatus.IN_PROGRESS);

        assertThatThrownBy(() -> taskService.changeTaskStatus(10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("任务状态不能从 COMPLETED 流转到 IN_PROGRESS");
        org.mockito.Mockito.verify(taskMapper, org.mockito.Mockito.never())
                .updateStatusIfCurrent(any(), any(), any(), any());
    }

    /**
     * 验证并发状态变化会拒绝覆盖其他请求的更新。
     */
    @Test
    void shouldRejectConcurrentStatusChange() {
        Task task = task(10L, TaskStatus.TODO);
        when(taskMapper.selectById(10L)).thenReturn(task);
        when(taskMapper.updateStatusIfCurrent(any(), any(), any(), any())).thenReturn(0);

        ChangeTaskStatusRequest request = new ChangeTaskStatusRequest();
        request.setTargetStatus(TaskStatus.IN_PROGRESS);

        assertThatThrownBy(() -> taskService.changeTaskStatus(10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("任务状态已发生变化，请重试");
    }

    /**
     * 验证可以按项目、状态、优先级和标题关键字查询任务。
     */
    @Test
    void shouldQueryTasksByConditions() {
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        Task task = new Task();
        task.setId(10L);
        task.setProjectId(1L);
        task.setPhaseId(2L);
        task.setTitle("实现任务查询");
        task.setStatus(TaskStatus.TODO);
        task.setPriority(TaskPriority.HIGH);
        when(taskMapper.selectList(any())).thenReturn(List.of(task));
        when(projectPhaseMapper.selectById(2L)).thenReturn(phase(2L, 1L, "开发实现"));

        var responses = taskService.queryTasks(1L, TaskStatus.TODO, TaskPriority.HIGH, "查询");

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getTitle()).isEqualTo("实现任务查询");
        assertThat(responses.getFirst().getPhaseName()).isEqualTo("开发实现");
    }

    /**
     * 验证不能将任务创建到其他项目的阶段中。
     */
    @Test
    void shouldRejectPhaseFromAnotherProject() {
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        when(projectPhaseMapper.selectById(2L)).thenReturn(phase(2L, 9L, "其他项目阶段"));

        CreateTaskRequest request = new CreateTaskRequest();
        request.setProjectId(1L);
        request.setPhaseId(2L);
        request.setTitle("实现任务接口");

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("任务阶段不属于当前项目");
    }

    /**
     * 验证任务阶段不存在时不能创建任务。
     */
    @Test
    void shouldRejectCreatingTaskWithMissingPhase() {
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        when(projectPhaseMapper.selectById(99L)).thenReturn(null);

        CreateTaskRequest request = new CreateTaskRequest();
        request.setProjectId(1L);
        request.setPhaseId(99L);
        request.setTitle("实现任务接口");

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("项目阶段不存在");
    }

    /**
     * 验证可以软删除任务。
     */
    @Test
    void shouldSoftDeleteTask() {
        when(taskMapper.softDeleteById(10L)).thenReturn(1);

        taskService.deleteTask(10L);

        verify(taskMapper).softDeleteById(10L);
    }

    /**
     * 验证可以恢复已删除任务。
     */
    @Test
    void shouldRestoreDeletedTask() {
        when(taskMapper.restoreById(10L)).thenReturn(1);

        taskService.restoreDeletedTask(10L);

        verify(taskMapper).restoreById(10L);
    }

    /**
     * 创建用于测试的项目阶段实体。
     */
    private ProjectPhase phase(Long id, Long projectId, String name) {
        ProjectPhase phase = new ProjectPhase();
        phase.setId(id);
        phase.setProjectId(projectId);
        phase.setName(name);
        return phase;
    }

    /**
     * 创建用于测试的任务实体。
     */
    private Task task(Long id, TaskStatus status) {
        Task task = new Task();
        task.setId(id);
        task.setProjectId(1L);
        task.setPhaseId(2L);
        task.setTitle("测试任务");
        task.setStatus(status);
        task.setPriority(TaskPriority.MEDIUM);
        return task;
    }

}
