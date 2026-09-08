package com.darkvoice1.devcompass.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.project.dto.CreateProjectPhaseRequest;
import com.darkvoice1.devcompass.project.dto.UpdateProjectPhaseRequest;
import com.darkvoice1.devcompass.project.dto.UpdateProjectPhaseSortOrderRequest;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.entity.ProjectPhase;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.project.repository.ProjectPhaseMapper;
import com.darkvoice1.devcompass.project.service.ProjectPhaseService;

/**
 * 验证项目阶段服务的核心业务逻辑。
 */
class ProjectPhaseServiceTest {

    private ProjectMapper projectMapper;

    private ProjectPhaseMapper projectPhaseMapper;

    private ProjectPhaseService projectPhaseService;

    /**
     * 初始化项目阶段服务及 Mapper 模拟对象。
     */
    @BeforeEach
    void setUp() {
        projectMapper = mock(ProjectMapper.class);
        projectPhaseMapper = mock(ProjectPhaseMapper.class);
        projectPhaseService = new ProjectPhaseService(projectMapper, projectPhaseMapper);
    }

    /**
     * 验证新阶段默认排在项目已有阶段之后。
     */
    @Test
    void shouldCreateProjectPhaseAtEnd() {
        when(projectMapper.selectById(1L)).thenReturn(project(1L));
        when(projectPhaseMapper.selectCount(any())).thenReturn(2L);
        doAnswer(invocation -> {
            invocation.<ProjectPhase>getArgument(0).setId(3L);
            return 1;
        }).when(projectPhaseMapper).insert(any(ProjectPhase.class));

        CreateProjectPhaseRequest request = new CreateProjectPhaseRequest();
        request.setName("开发实现");
        request.setDescription("完成后端功能开发");

        var response = projectPhaseService.createProjectPhase(1L, request);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getProjectId()).isEqualTo(1L);
        assertThat(response.getSortOrder()).isEqualTo(2);
        assertThat(response.getName()).isEqualTo("开发实现");
    }

    /**
     * 验证项目不存在时不能创建阶段。
     */
    @Test
    void shouldRejectCreatingPhaseForMissingProject() {
        when(projectMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> projectPhaseService.createProjectPhase(99L,
                new CreateProjectPhaseRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("项目不存在");
    }

    /**
     * 验证查询项目阶段时按排序序号返回。
     */
    @Test
    void shouldQueryProjectPhasesBySortOrder() {
        when(projectMapper.selectById(1L)).thenReturn(project(1L));
        when(projectPhaseMapper.selectList(any())).thenReturn(List.of(
                phase(1L, 1L, "需求分析", 0), phase(2L, 1L, "开发实现", 1)));

        var responses = projectPhaseService.getProjectPhases(1L);

        assertThat(responses).extracting(response -> response.getSortOrder())
                .containsExactly(0, 1);
    }

    /**
     * 验证可以编辑当前项目中的阶段。
     */
    @Test
    void shouldUpdateProjectPhase() {
        ProjectPhase stored = phase(1L, 1L, "旧阶段名称", 0);
        when(projectMapper.selectById(1L)).thenReturn(project(1L));
        when(projectPhaseMapper.selectById(1L)).thenReturn(stored);

        UpdateProjectPhaseRequest request = new UpdateProjectPhaseRequest();
        request.setName("需求分析");
        request.setDescription("补充需求说明");

        var response = projectPhaseService.updateProjectPhase(1L, 1L, request);

        assertThat(response.getName()).isEqualTo("需求分析");
        assertThat(response.getDescription()).isEqualTo("补充需求说明");
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(projectPhaseMapper).updateById(stored);
    }

    /**
     * 验证不能通过其他项目操作阶段。
     */
    @Test
    void shouldRejectPhaseFromAnotherProject() {
        when(projectMapper.selectById(1L)).thenReturn(project(1L));
        when(projectPhaseMapper.selectById(2L)).thenReturn(phase(2L, 9L, "其他项目阶段", 0));

        assertThatThrownBy(() -> projectPhaseService.updateProjectPhase(1L, 2L,
                new UpdateProjectPhaseRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("项目阶段不属于当前项目");
    }

    /**
     * 验证调整阶段顺序后会重新设置连续序号。
     */
    @Test
    void shouldReorderProjectPhases() {
        ProjectPhase analysis = phase(1L, 1L, "需求分析", 0);
        ProjectPhase development = phase(2L, 1L, "开发实现", 1);
        ProjectPhase testing = phase(3L, 1L, "测试验收", 2);
        when(projectMapper.selectById(1L)).thenReturn(project(1L));
        when(projectPhaseMapper.selectById(3L)).thenReturn(testing);
        when(projectPhaseMapper.selectList(any())).thenReturn(List.of(analysis, development, testing));

        UpdateProjectPhaseSortOrderRequest request = new UpdateProjectPhaseSortOrderRequest();
        request.setSortOrder(0);
        var response = projectPhaseService.updateProjectPhaseSortOrder(1L, 3L, request);

        assertThat(response.getSortOrder()).isZero();
        assertThat(analysis.getSortOrder()).isEqualTo(1);
        assertThat(development.getSortOrder()).isEqualTo(2);
        verify(projectPhaseMapper).updateById(testing);
        verify(projectPhaseMapper).updateById(analysis);
        verify(projectPhaseMapper).updateById(development);
    }

    /**
     * 创建用于模拟项目存在的项目实体。
     */
    private Project project(Long id) {
        Project project = new Project();
        project.setId(id);
        return project;
    }

    /**
     * 创建用于测试的项目阶段实体。
     */
    private ProjectPhase phase(Long id, Long projectId, String name, int sortOrder) {
        ProjectPhase phase = new ProjectPhase();
        phase.setId(id);
        phase.setProjectId(projectId);
        phase.setName(name);
        phase.setSortOrder(sortOrder);
        return phase;
    }
}
