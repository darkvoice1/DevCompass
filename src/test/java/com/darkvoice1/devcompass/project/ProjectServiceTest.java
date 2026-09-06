package com.darkvoice1.devcompass.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.project.dto.CreateProjectRequest;
import com.darkvoice1.devcompass.project.dto.ProjectDetailResponse;
import com.darkvoice1.devcompass.project.dto.UpdateProjectRequest;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.entity.ProjectStatus;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.project.service.ProjectService;

/**
 * 验证项目创建和查询服务逻辑。
 */
class ProjectServiceTest {

    private ProjectMapper projectMapper;

    private ProjectService projectService;

    /**
     * 初始化项目服务及 Mapper 模拟对象。
     */
    @BeforeEach
    void setUp() {
        projectMapper = mock(ProjectMapper.class);
        projectService = new ProjectService(projectMapper);
    }

    /**
     * 验证未传状态时默认使用计划中状态。
     */
    @Test
    void shouldUsePlannedStatusByDefault() {
        Project stored = new Project();
        stored.setId(1L);
        stored.setName("研发罗盘");
        stored.setStatus(ProjectStatus.PLANNED);
        doAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(1L);
            return 1;
        }).when(projectMapper).insert(any(Project.class));
        when(projectMapper.selectById(1L)).thenReturn(stored);

        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("研发罗盘");

        ProjectDetailResponse response = projectService.createProject(request);

        assertThat(response.getStatus()).isEqualTo(ProjectStatus.PLANNED);
    }

    /**
     * 验证查询不存在的项目时抛出业务异常。
     */
    @Test
    void shouldThrowBusinessExceptionWhenProjectDoesNotExist() {
        when(projectMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> projectService.getProjectDetail(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("项目不存在");
    }

    /**
     * 验证编辑项目时更新名称、描述和状态。
     */
    @Test
    void shouldUpdateEditableProjectFields() {
        Project stored = new Project();
        stored.setId(1L);
        stored.setName("旧项目名称");
        stored.setDescription("旧项目描述");
        stored.setStatus(ProjectStatus.PLANNED);
        when(projectMapper.selectById(1L)).thenReturn(stored);

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setName("新项目名称");
        request.setDescription("新项目描述");
        request.setStatus(ProjectStatus.COMPLETED);

        ProjectDetailResponse response = projectService.updateProject(1L, request);

        assertThat(response.getName()).isEqualTo("新项目名称");
        assertThat(response.getDescription()).isEqualTo("新项目描述");
        assertThat(response.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    /**
     * 验证编辑不存在的项目时抛出业务异常。
     */
    @Test
    void shouldThrowBusinessExceptionWhenUpdatingMissingProject() {
        when(projectMapper.selectById(99L)).thenReturn(null);

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setName("新项目名称");

        assertThatThrownBy(() -> projectService.updateProject(99L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("项目不存在");
    }
}
