package com.darkvoice1.devcompass.search.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.darkvoice1.devcompass.search.dto.SearchItemResponse;
import com.darkvoice1.devcompass.search.dto.SearchItemType;
import com.darkvoice1.devcompass.search.dto.SearchQueryRequest;
import com.darkvoice1.devcompass.search.dto.SearchResponse;
import com.darkvoice1.devcompass.search.repository.SearchMapper;

/**
 * 按关键字搜索项目和任务。
 */
@Service
public class SearchService {

    /**
     * 搜索摘要最长字符数。
     */
    public static final int SUMMARY_MAX_LENGTH = 120;

    private final SearchMapper searchMapper;

    /**
     * 创建搜索服务。
     *
     * @param searchMapper 搜索数据访问对象
     */
    public SearchService(SearchMapper searchMapper) {
        this.searchMapper = searchMapper;
    }

    /**
     * 按关键字搜索项目和任务，可选限定类型。
     *
     * @param request 搜索参数
     * @return 按更新时间倒序的搜索结果
     */
    public SearchResponse search(SearchQueryRequest request) {
        String keyword = request.getKeyword().trim();
        String pattern = toLikePattern(keyword);
        List<SearchItemResponse> items = new ArrayList<>();
        if (request.getType() == null || request.getType() == SearchItemType.PROJECT) {
            items.addAll(searchMapper.selectProjects(pattern));
        }
        if (request.getType() == null || request.getType() == SearchItemType.TASK) {
            items.addAll(searchMapper.selectTasks(pattern));
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
}
