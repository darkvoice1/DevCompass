package com.darkvoice1.devcompass.search.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
import com.darkvoice1.devcompass.project.entity.ProjectStatus;
import com.darkvoice1.devcompass.search.dto.SearchItemResponse;
import com.darkvoice1.devcompass.search.dto.SearchItemType;
import com.darkvoice1.devcompass.search.dto.SearchQueryRequest;
import com.darkvoice1.devcompass.search.dto.SearchResponse;
import com.darkvoice1.devcompass.search.repository.SearchMapper;
import com.darkvoice1.devcompass.task.entity.TaskStatus;

/**
 * 按关键字和筛选条件搜索项目和任务。
 */
@Service
public class SearchService {

    /**
     * 搜索摘要最长字符数。
     */
    public static final int SUMMARY_MAX_LENGTH = 120;

    private final SearchMapper searchMapper;

    private final Clock clock;

    /**
     * 创建搜索服务。
     *
     * @param searchMapper 搜索数据访问对象
     * @param clock 应用时钟，用于按统一时区解释日期
     */
    public SearchService(SearchMapper searchMapper, Clock clock) {
        this.searchMapper = searchMapper;
        this.clock = clock;
    }

    /**
     * 按关键字和可选筛选条件搜索项目和任务。
     *
     * @param request 搜索参数
     * @return 按更新时间倒序的搜索结果
     */
    public SearchResponse search(SearchQueryRequest request) {
        validateDateRange(request.getDateFrom(), request.getDateTo());
        String keyword = normalizeKeyword(request.getKeyword());
        String pattern = keyword == null ? null : toLikePattern(keyword);
        ProjectStatus projectStatus = parseProjectStatus(request.getStatus());
        TaskStatus taskStatus = parseTaskStatus(request.getStatus());
        boolean searchProjects = request.getType() == null
                || request.getType() == SearchItemType.PROJECT;
        boolean searchTasks = request.getType() == null || request.getType() == SearchItemType.TASK;
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            if (projectStatus == null && taskStatus == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "不支持的状态");
            }
            if (projectStatus == null) {
                searchProjects = false;
            }
            if (taskStatus == null) {
                searchTasks = false;
            }
        }
        Instant updatedFrom = startOfDay(request.getDateFrom());
        Instant updatedTo = startOfNextDay(request.getDateTo());

        List<SearchItemResponse> items = new ArrayList<>();
        if (searchProjects) {
            items.addAll(searchMapper.selectProjects(
                    pattern, request.getProjectId(), projectStatus, updatedFrom, updatedTo));
        }
        if (searchTasks) {
            items.addAll(searchMapper.selectTasks(
                    pattern, request.getProjectId(), taskStatus, updatedFrom, updatedTo));
        }
        items.forEach(item -> item.setSummary(toSummary(item.getSummary())));
        items.sort(Comparator
                .comparing((SearchItemResponse item) -> item.getUpdatedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(item -> item.getId(),
                        Comparator.nullsLast(Comparator.reverseOrder())));

        SearchResponse response = new SearchResponse();
        response.setKeyword(keyword);
        response.setType(request.getType());
        response.setItems(items);
        return response;
    }

    /**
     * 去掉关键字两侧空白，空白关键字按未筛选处理。
     */
    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    /**
     * 把用户关键字转成可安全用于 LIKE 的模式，并转义 %、_ 和反斜杠。
     */
    private String toLikePattern(String keyword) {
        String escaped = keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    /**
     * 把描述压成短摘要，方便列表展示。
     */
    private String toSummary(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.trim();
        if (normalized.length() <= SUMMARY_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, SUMMARY_MAX_LENGTH);
    }

    /**
     * 开始日期不能晚于结束日期。
     */
    private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "更新日期范围不合法");
        }
    }

    /**
     * 把日历日转换成该时区当天 0 点对应的时间。
     */
    private Instant startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay(clock.getZone()).toInstant();
    }

    /**
     * 把日历日转换成次日 0 点，用作含当天的开区间终点。
     */
    private Instant startOfNextDay(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
    }

    /**
     * 尝试把状态解析成项目状态，无法解析时返回空。
     */
    private ProjectStatus parseProjectStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ProjectStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /**
     * 尝试把状态解析成任务状态，无法解析时返回空。
     */
    private TaskStatus parseTaskStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return TaskStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
