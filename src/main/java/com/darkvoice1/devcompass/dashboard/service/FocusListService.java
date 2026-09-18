package com.darkvoice1.devcompass.dashboard.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.darkvoice1.devcompass.dashboard.dto.FocusListItemResponse;
import com.darkvoice1.devcompass.dashboard.dto.FocusListQueryRequest;
import com.darkvoice1.devcompass.dashboard.dto.FocusListResponse;
import com.darkvoice1.devcompass.task.repository.TaskMapper;

/**
 * 查询首页需要优先处理的任务清单。
 */
@Service
public class FocusListService {

    private final TaskMapper taskMapper;

    private final Clock clock;

    /**
     * 创建焦点清单服务。
     *
     * @param taskMapper 任务数据访问对象
     * @param clock 应用时钟，用于按统一时区计算今天
     */
    public FocusListService(TaskMapper taskMapper, Clock clock) {
        this.taskMapper = taskMapper;
        this.clock = clock;
    }

    /**
     * 按清单类型查询任务。第一部分只支持本周任务。
     *
     * @param request 清单查询参数
     * @return 本周日期范围和任务列表
     */
    public FocusListResponse getFocusList(FocusListQueryRequest request) {
        LocalDate today = LocalDate.now(clock);
        LocalDate fromDate = today.with(DayOfWeek.MONDAY);
        LocalDate toDate = today.with(DayOfWeek.SUNDAY);
        List<FocusListItemResponse> items = taskMapper.selectFocusListTasks(fromDate, toDate);

        FocusListResponse response = new FocusListResponse();
        response.setType(request.getType());
        response.setFromDate(fromDate);
        response.setToDate(toDate);
        response.setItems(items);
        return response;
    }
}
