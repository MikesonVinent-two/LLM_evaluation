package com.example.demo.repository.jdbc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.example.demo.entity.jdbc.Tag;

@Repository
public class StandardQuestionTagsRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 根据问题ID查询关联的标签
     * 
     * @param questionId 问题ID
     * @return 关联的标签列表
     */
    public List<Tag> findTagsByQuestionId(Long questionId) {
        String sql = "SELECT t.* FROM tags t " +
                     "JOIN standard_question_tags sqt ON t.id = sqt.tag_id " +
                     "WHERE sqt.standard_question_id = ? AND t.deleted_at IS NULL";
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Tag tag = new Tag();
            tag.setId(rs.getLong("id"));
            tag.setTagName(rs.getString("tag_name"));
            tag.setTagType(rs.getString("tag_type"));
            tag.setDescription(rs.getString("description"));
            // 可以设置其他必要的字段
            return tag;
        }, questionId);
    }
} 