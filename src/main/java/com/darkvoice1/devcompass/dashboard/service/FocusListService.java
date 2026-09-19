package com.darkvoice1.devcompass.dashboard.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.darkvoice1.devcompass.dashboard.dto.FocusListItemResponse;
import com.darkvoice1.devcompass.dashboard.dto.FocusListQueryRequest;
import com.darkvoice1.devcompass.dashboard.dto.FocusListResponse;
import com.darkvoice1.devcompass.dashboard.dto.FocusListType;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.task.repository.TaskMapper;

/**
 * 查询首页需要优先处理的任务和延期项目清单。
 */
@Service
public class FocusListService {

    private final TaskMapper taskMapper;

    private final ProjectMapper projectMapper;

    private final Clock clock;

    /**
     * 创建焦点清单服务。
     *
     * @param taskMapper 任务数据访问对象
     * @param projectMapper 项目数据访问对象
     * @param clock 应用时钟，用于按统一时区计算今天
     */
    public FocusListService(TaskMapper taskMapper, ProjectMapper projectMapper, Clock clock) {
        this.taskMapper = taskMapper;
        this.projectMapper = projectMapper;
        this.clock = clock;
    }

    /**
     * 按清单类型查询本周、逾期、即将到期或阻塞事项。
     *
     * @param request 清单查询参数
     * @return 日期范围和清单项
     */
    public FocusListResponse getFocusList(FocusListQueryRequest request) {
        LocalDate today = LocalDate.now(clock);
        return switch (request.getType()) {
            case THIS_WEEK -> thisWeek(today);
            case OVERDUE -> overdue(today);
            case DUE_SOON -> dueSoon(today);
            case BLOCKED -> blocked();
        };
    }

    /**
     * 查询本周一到周日到期的未完成任务。
     */
    private FocusListResponse thisWeek(LocalDate today) {
        LocalDate fromDate = today.with(DayOfWeek.MONDAY);
        LocalDate toDate = today.with(DayOfWeek.SUNDAY);
        return toResponse(FocusListType.THIS_WEEK, fromDate, toDate,
                taskMapper.selectFocusListTasks(fromDate, toDate));
    }

    /**
     * 查询截止日期早于今天的任务，以及目标日期已过的未完成项目。
     */
    private FocusListResponse overdue(LocalDate today) {
        LocalDate toDate = today.minusDays(1);
        List<FocusListItemResponse> items = new ArrayList<>(
                taskMapper.selectFocusListTasks(null, toDate));
        items.addAll(projectMapper.selectOverdueFocusProjects(today));
        return toResponse(FocusListType.OVERDUE, null, toDate, items);
    }

    /**
     * 查询今天起 7 天内到期的未完成任务，包含今天和第 7 天。
     */
    private FocusListResponse dueSoon(LocalDate today) {
        LocalDate toDate = today.plusDays(DashboardService.DUE_SOON_DAYS);
        return toResponse(FocusListType.DUE_SOON, today, toDate,
                taskMapper.selectFocusListTasks(today, toDate));
    }

    /**
     * 查询已标记阻塞且尚未完成的任务。
     */
    private FocusListResponse blocked() {
        return toResponse(FocusListType.BLOCKED, null, null,
                taskMapper.selectBlockedFocusListTasks());
    }

    /**
     * 组装清单响应。
     */
    private FocusListResponse toResponse(FocusListType type, LocalDate fromDate, LocalDate toDate,
            List<FocusListItemResponse> items) {
        FocusListResponse response = new FocusListResponse();
        response.setType(type);
        response.setFromDate(fromDate);
        response.setToDate(toDate);
        response.setItems(items);
        return response;
    }
}
