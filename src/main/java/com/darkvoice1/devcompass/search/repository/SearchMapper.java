package com.darkvoice1.devcompass.search.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.darkvoice1.devcompass.search.dto.SearchItemResponse;

/**
 * 全局搜索数据访问接口。
 */
@Mapper
public interface SearchMapper {

    /**
     * 按关键字搜索未归档、未删除项目。
     *
     * @param keyword 已转义并带通配符的匹配模式
     * @return 匹配的项目结果
     */
    @Select("""
            SELECT p.id, 'PROJECT' AS type, p.name AS title, p.description AS summary,
                   p.updated_at, p.id AS project_id, p.name AS project_name
            FROM project p
            WHERE p.deleted_at IS NULL
              AND p.archived = FALSE
              AND (
                    p.name ILIKE #{keyword} ESCAPE '\\'
                    OR p.description ILIKE #{keyword} ESCAPE '\\'
                    OR p.tags ILIKE #{keyword} ESCAPE '\\'
                  )
            ORDER BY p.updated_at DESC, p.id DESC
            """)
    List<SearchItemResponse> selectProjects(@Param("keyword") String keyword);

    /**
     * 按关键字搜索未删除任务，所属项目必须未归档、未删除。
     *
     * @param keyword 已转义并带通配符的匹配模式
     * @return 匹配的任务结果
     */
    @Select("""
            SELECT t.id, 'TASK' AS type, t.title, t.description AS summary,
                   t.updated_at, t.project_id, p.name AS project_name
            FROM task t
            INNER JOIN project p ON p.id = t.project_id
            WHERE t.deleted_at IS NULL
              AND p.deleted_at IS NULL
              AND p.archived = FALSE
              AND (
                    t.title ILIKE #{keyword} ESCAPE '\\'
                    OR t.description ILIKE #{keyword} ESCAPE '\\'
                  )
            ORDER BY t.updated_at DESC, t.id DESC
            """)
    List<SearchItemResponse> selectTasks(@Param("keyword") String keyword);
}
