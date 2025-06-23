package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.Tag;
import com.example.demo.entity.jdbc.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基于JDBC的标签仓库实现
 */
@Repository
public class TagRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO tags (tag_name, tag_type, description, created_at, created_by_user_id, created_change_log_id) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE tags SET tag_name=?, tag_type=?, description=? WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM tags WHERE id=? AND deleted_at IS NULL";
    
    private static final String SQL_FIND_BY_TAG_NAME = 
            "SELECT * FROM tags WHERE tag_name=? AND deleted_at IS NULL";
    
    private static final String SQL_EXISTS_BY_TAG_NAME = 
            "SELECT COUNT(*) FROM tags WHERE tag_name=? AND deleted_at IS NULL";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM tags WHERE deleted_at IS NULL";
    
    private static final String SQL_SOFT_DELETE = 
            "UPDATE tags SET deleted_at=? WHERE id=?";

    @Autowired
    public TagRepository(JdbcTemplate jdbcTemplate, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
    }

    /**
     * 保存标签
     *
     * @param tag 标签对象
     * @return 带有ID的标签对象
     */
    public Tag save(Tag tag) {
        if (tag.getId() == null) {
            return insert(tag);
        } else {
            return update(tag);
        }
    }

    /**
     * 插入新标签
     *
     * @param tag 标签对象
     * @return 带有ID的标签对象
     */
    private Tag insert(Tag tag) {
        if (tag.getCreatedAt() == null) {
            tag.setCreatedAt(LocalDateTime.now());
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, tag.getTagName());
            ps.setString(2, tag.getTagType());
            
            // 处理描述字段
            if (tag.getDescription() != null) {
                ps.setString(3, tag.getDescription());
            } else {
                ps.setNull(3, java.sql.Types.VARCHAR);
            }
            
            ps.setTimestamp(4, Timestamp.valueOf(tag.getCreatedAt()));
            
            // 处理可能为null的外键
            if (tag.getCreatedByUser() != null && tag.getCreatedByUser().getId() != null) {
                ps.setLong(5, tag.getCreatedByUser().getId());
            } else {
                ps.setNull(5, java.sql.Types.BIGINT);
            }
            
            if (tag.getCreatedChangeLog() != null && tag.getCreatedChangeLog().getId() != null) {
                ps.setLong(6, tag.getCreatedChangeLog().getId());
            } else {
                ps.setNull(6, java.sql.Types.BIGINT);
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            tag.setId(key.longValue());
        }
        return tag;
    }

    /**
     * 更新标签
     *
     * @param tag 标签对象
     * @return 更新后的标签对象
     */
    private Tag update(Tag tag) {
        jdbcTemplate.update(SQL_UPDATE,
                tag.getTagName(),
                tag.getTagType(),
                tag.getDescription(),
                tag.getId());

        return tag;
    }

    /**
     * 根据ID查找标签
     *
     * @param id 标签ID
     * @return 标签的Optional包装
     */
    public Optional<Tag> findById(Long id) {
        try {
            Tag tag = jdbcTemplate.queryForObject(SQL_FIND_BY_ID, new Object[]{id}, new TagRowMapper());
            return Optional.ofNullable(tag);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据标签名查找标签
     *
     * @param tagName 标签名
     * @return 标签的Optional包装
     */
    public Optional<Tag> findByTagName(String tagName) {
        try {
            Tag tag = jdbcTemplate.queryForObject(SQL_FIND_BY_TAG_NAME, new Object[]{tagName}, new TagRowMapper());
            return Optional.ofNullable(tag);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 检查标签名是否已存在
     *
     * @param tagName 标签名
     * @return 是否存在
     */
    public boolean existsByTagName(String tagName) {
        Integer count = jdbcTemplate.queryForObject(SQL_EXISTS_BY_TAG_NAME, Integer.class, tagName);
        return count != null && count > 0;
    }

    /**
     * 查找所有标签
     *
     * @return 标签列表
     */
    public List<Tag> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new TagRowMapper());
    }

    /**
     * 软删除标签
     *
     * @param id 标签ID
     * @return 是否成功
     */
    public boolean softDelete(Long id) {
        int affected = jdbcTemplate.update(SQL_SOFT_DELETE, Timestamp.valueOf(LocalDateTime.now()), id);
        return affected > 0;
    }

    /**
     * 标签行映射器
     */
    private class TagRowMapper implements RowMapper<Tag> {
        @Override
        public Tag mapRow(ResultSet rs, int rowNum) throws SQLException {
            Tag tag = new Tag();
            tag.setId(rs.getLong("id"));
            tag.setTagName(rs.getString("tag_name"));
            tag.setTagType(rs.getString("tag_type"));
            tag.setDescription(rs.getString("description"));
            
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                tag.setCreatedAt(createdAt.toLocalDateTime());
            }
            
            Timestamp deletedAt = rs.getTimestamp("deleted_at");
            if (deletedAt != null) {
                tag.setDeletedAt(deletedAt.toLocalDateTime());
            }
            
            // 设置创建者用户
            Long createdByUserId = rs.getLong("created_by_user_id");
            if (!rs.wasNull()) {
                userRepository.findById(createdByUserId).ifPresent(user -> tag.setCreatedByUser(user));
            }
            
            // 注意：这里没有处理created_change_log_id，因为会导致循环依赖
            // 如果需要，可以在服务层处理这种关系
            
            return tag;
        }
    }
} 
