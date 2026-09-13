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
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.entity.ProjectPhase;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.project.repository.ProjectPhaseMapper;
import com.darkvoice1.devcompass.task.dto.CreateTaskRequest;
import com.darkvoice1.devcompass.task.dto.ChangeTaskStatusRequest;
import com.darkvoice1.devcompass.task.dto.TaskDetailResponse;
import com.darkvoice1.devcompass.task.dto.TaskBoardResponse;
import com.darkvoice1.devcompass.task.dto.TaskPageQueryRequest;
import com.darkvoice1.devcompass.task.dto.TaskPageResponse;
import com.darkvoice1.devcompass.task.dto.UpdateTaskRequest;
import com.darkvoice1.devcompass.task.entity.Task;
import com.darkvoice1.devcompass.task.entity.TaskPriority;
import com.darkvoice1.devcompass.task.entity.TaskStatus;
import com.darkvoice1.devcompass.task.repository.TaskMapper;
import com.darkvoice1.devcompass.task.service.TaskService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.mockito.ArgumentCaptor;

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
     * 验证分页查询返回当前页、总数和总页数。
     */
    @Test
    void shouldQueryTasksByPage() {
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        Task task = task(10L, TaskStatus.TODO);
        task.setTitle("分页任务");
        Page<Task> result = new Page<>(2, 2);
        result.setTotal(5);
        result.setRecords(List.of(task));
        when(taskMapper.selectPage(any(), any())).thenReturn(result);
        when(projectPhaseMapper.selectById(2L)).thenReturn(phase(2L, 1L, "开发实现"));

        TaskPageQueryRequest request = new TaskPageQueryRequest();
        request.setProjectId(1L);
        request.setPage(2L);
        request.setPageSize(2);
        request.setStatus(TaskStatus.TODO);

        TaskPageResponse response = taskService.queryTasksPage(request);

        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getPageSize()).isEqualTo(2);
        assertThat(response.getTotal()).isEqualTo(5);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.getRecords()).hasSize(1);
        assertThat(response.getRecords().getFirst().getTitle()).isEqualTo("分页任务");
    }

    /**
     * 验证服务层拒绝超出上限的每页数量。
     */
    @Test
    void shouldRejectInvalidPageSize() {
        TaskPageQueryRequest request = new TaskPageQueryRequest();
        request.setProjectId(1L);
        request.setPage(1L);
        request.setPageSize(101);

        assertThatThrownBy(() -> taskService.queryTasksPage(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("分页参数不合法");
    }

    /**
     * 验证分页查询支持阶段、日期范围和标题关键字组合筛选。
     */
    @Test
    void shouldApplyCombinedTaskFilters() {
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        Page<Task> result = new Page<>(1, 20);
        result.setTotal(0);
        result.setRecords(List.of());
        when(taskMapper.selectPage(any(), any())).thenReturn(result);

        TaskPageQueryRequest request = new TaskPageQueryRequest();
        request.setProjectId(1L);
        request.setPhaseId(2L);
        request.setStatus(TaskStatus.TODO);
        request.setPriority(TaskPriority.HIGH);
        request.setDueDateFrom(LocalDate.of(2026, 1, 1));
        request.setDueDateTo(LocalDate.of(2026, 12, 31));
        request.setKeyword(" 接口 ");

        taskService.queryTasksPage(request);

        ArgumentCaptor<QueryWrapper<Task>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(taskMapper).selectPage(any(), captor.capture());
        assertThat(captor.getValue().getSqlSegment())
                .contains("project_id", "phase_id", "status", "priority", "due_date", "title");
        assertThat(captor.getValue().getParamNameValuePairs().values())
                .contains(1L, 2L, TaskStatus.TODO, TaskPriority.HIGH,
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "%接口%");
    }

    /**
     * 验证截止日期开始时间晚于结束时间时拒绝查询。
     */
    @Test
    void shouldRejectInvalidDueDateRange() {
        TaskPageQueryRequest request = new TaskPageQueryRequest();
        request.setProjectId(1L);
        request.setDueDateFrom(LocalDate.of(2026, 12, 31));
        request.setDueDateTo(LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> taskService.queryTasksPage(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("截止日期范围不合法");
    }

    /**
     * 验证分页查询可以使用白名单字段和降序排序。
     */
    @Test
    void shouldApplyRequestedSort() {
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        Page<Task> result = new Page<>(1, 20);
        result.setRecords(List.of());
        when(taskMapper.selectPage(any(), any())).thenReturn(result);

        TaskPageQueryRequest request = new TaskPageQueryRequest();
        request.setProjectId(1L);
        request.setSortBy("updatedAt");
        request.setSortDirection("DESC");

        taskService.queryTasksPage(request);

        ArgumentCaptor<QueryWrapper<Task>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(taskMapper).selectPage(any(), captor.capture());
        assertThat(captor.getValue().getSqlSegment())
                .contains("ORDER BY updated_at DESC", "id ASC");
    }

    /**
     * 验证非法排序字段不会进入 SQL。
     */
    @Test
    void shouldRejectUnsupportedSortField() {
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        TaskPageQueryRequest request = new TaskPageQueryRequest();
        request.setProjectId(1L);
        request.setSortBy("deletedAt");

        assertThatThrownBy(() -> taskService.queryTasksPage(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不支持的排序字段: deletedAt");
    }

    /**
     * 验证非法排序方向会被拒绝。
     */
    @Test
    void shouldRejectUnsupportedSortDirection() {
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        TaskPageQueryRequest request = new TaskPageQueryRequest();
        request.setProjectId(1L);
        request.setSortBy("dueDate");
        request.setSortDirection("random");

        assertThatThrownBy(() -> taskService.queryTasksPage(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("排序方向只能是 asc 或 desc");
    }

    /**
     * 验证任务看板包含全部状态列，并按状态统计任务数量。
     */
    @Test
    void shouldBuildTaskBoardWithAllStatusColumns() {
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        when(projectPhaseMapper.selectList(any())).thenReturn(List.of(
                phase(2L, 1L, "开发实现")));
        Task todo = task(10L, TaskStatus.TODO);
        todo.setTitle("待办任务");
        Task completed = task(11L, TaskStatus.COMPLETED);
        completed.setTitle("已完成任务");
        when(taskMapper.selectList(any())).thenReturn(List.of(todo, completed));
        when(projectPhaseMapper.selectById(2L)).thenReturn(phase(2L, 1L, "开发实现"));

        TaskBoardResponse response = taskService.getTaskBoard(1L);

        assertThat(response.getProjectId()).isEqualTo(1L);
        assertThat(response.getColumns()).hasSize(4);
        assertThat(response.getColumns().get(0).getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(response.getColumns().get(0).getCount()).isEqualTo(1);
        assertThat(response.getColumns().get(0).getTasks().getFirst().getPhaseName())
                .isEqualTo("开发实现");
        assertThat(response.getColumns().get(1).getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.getColumns().get(1).getCount()).isZero();
        assertThat(response.getColumns().get(2).getCount()).isEqualTo(1);
        assertThat(response.getColumns().get(3).getCount()).isZero();
    }

    /**
     * 验证看板排除软删除任务和已软删除阶段下的任务。
     */
    @Test
    void shouldExcludeDeletedTasksAndDeletedPhasesFromBoard() {
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        ProjectPhase activePhase = phase(2L, 1L, "开发实现");
        ProjectPhase deletedPhase = phase(3L, 1L, "已删除阶段");
        deletedPhase.setDeletedAt(java.time.Instant.now());
        when(projectPhaseMapper.selectList(any())).thenReturn(List.of(activePhase, deletedPhase));
        Task activeTask = task(10L, TaskStatus.TODO);
        Task deletedTask = task(11L, TaskStatus.TODO);
        deletedTask.setDeletedAt(java.time.Instant.now());
        Task taskInDeletedPhase = task(12L, TaskStatus.TODO);
        taskInDeletedPhase.setPhaseId(3L);
        when(taskMapper.selectList(any())).thenReturn(List.of(activeTask, deletedTask, taskInDeletedPhase));
        when(projectPhaseMapper.selectById(2L)).thenReturn(activePhase);

        TaskBoardResponse response = taskService.getTaskBoard(1L);

        assertThat(response.getColumns().getFirst().getCount()).isEqualTo(1);
        assertThat(response.getColumns().getFirst().getTasks().getFirst().getId()).isEqualTo(10L);
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
