package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.EvaluationPromptAssemblyConfig;
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
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基于JDBC的评测场景prompt组装配置仓库实现
 */
@Repository
public class EvaluationPromptAssemblyConfigRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository UserRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO evaluation_prompt_assembly_configs " +
            "(name, description, is_active, base_system_prompt, created_by_user_id, " +
            "created_at, updated_at, tag_prompts_section_header, subjective_section_header, " +
            "tag_prompt_separator, section_separator, final_instruction) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE evaluation_prompt_assembly_configs SET " +
            "name=?, description=?, is_active=?, base_system_prompt=?, created_by_user_id=?, " +
            "created_at=?, updated_at=?, tag_prompts_section_header=?, subjective_section_header=?, " +
            "tag_prompt_separator=?, section_separator=?, final_instruction=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM evaluation_prompt_assembly_configs WHERE id=?";
    
    private static final String SQL_FIND_BY_IS_ACTIVE_TRUE = 
            "SELECT * FROM evaluation_prompt_assembly_configs WHERE is_active=true";
    
    private static final String SQL_FIND_BY_NAME = 
            "SELECT * FROM evaluation_prompt_assembly_configs WHERE name=?";
    
    private static final String SQL_FIND_BY_CREATED_BY_USER_ID = 
            "SELECT * FROM evaluation_prompt_assembly_configs WHERE created_by_user_id=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM evaluation_prompt_assembly_configs";
    
    private static final String SQL_DELETE = 
            "DELETE FROM evaluation_prompt_assembly_configs WHERE id=?";

    @Autowired
    public EvaluationPromptAssemblyConfigRepository(JdbcTemplate jdbcTemplate, UserRepository UserRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.UserRepository = UserRepository;
    }

    /**
     * 保存配置
     *
     * @param config 配置对象
     * @return 带有ID的配置对?
     */
    public EvaluationPromptAssemblyConfig save(EvaluationPromptAssemblyConfig config) {
        if (config.getId() == null) {
            return insert(config);
        } else {
            return update(config);
        }
    }

    /**
     * 插入新配?
     *
     * @param config 配置对象
     * @return 带有ID的配置对?
     */
    private EvaluationPromptAssemblyConfig insert(EvaluationPromptAssemblyConfig config) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        // 确保创建时间和最后修改时间已设置
        if (config.getCreatedAt() == null) {
            config.setCreatedAt(LocalDateTime.now());
        }
        if (config.getUpdatedAt() == null) {
            config.setUpdatedAt(config.getCreatedAt());
        }

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置名称
            ps.setString(1, config.getName());
            
            // 设置描述
            if (config.getDescription() != null) {
                ps.setString(2, config.getDescription());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            
            // 设置是否激?
            ps.setBoolean(3, config.getIsActive());
            
            // 设置基础系统提示
            ps.setString(4, config.getBaseSystemPrompt());
            
            // 设置创建者ID
            if (config.getCreatedByUser() != null && config.getCreatedByUser().getId() != null) {
                ps.setLong(5, config.getCreatedByUser().getId());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            
            // 设置创建时间
            ps.setTimestamp(6, Timestamp.valueOf(config.getCreatedAt()));
            
            // 设置更新时间
            ps.setTimestamp(7, Timestamp.valueOf(config.getUpdatedAt()));
            
            // 设置标签提示部分标题
            ps.setString(8, config.getTagPromptsSectionHeader());
            
            // 设置主观题部分标?
            ps.setString(9, config.getSubjectiveSectionHeader());
            
            // 设置标签提示分隔?
            ps.setString(10, config.getTagPromptSeparator());
            
            // 设置部分分隔?
            ps.setString(11, config.getSectionSeparator());
            
            // 设置最终指?
            ps.setString(12, config.getFinalInstruction());
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            config.setId(key.longValue());
        }

        return config;
    }

    /**
     * 更新配置
     *
     * @param config 配置对象
     * @return 更新后的配置对象
     */
    private EvaluationPromptAssemblyConfig update(EvaluationPromptAssemblyConfig config) {
        // 更新最后修改时?
        config.setUpdatedAt(LocalDateTime.now());

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_UPDATE);
            
            // 设置名称
            ps.setString(1, config.getName());
            
            // 设置描述
            if (config.getDescription() != null) {
                ps.setString(2, config.getDescription());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            
            // 设置是否激?
            ps.setBoolean(3, config.getIsActive());
            
            // 设置基础系统提示
            ps.setString(4, config.getBaseSystemPrompt());
            
            // 设置创建者ID
            if (config.getCreatedByUser() != null && config.getCreatedByUser().getId() != null) {
                ps.setLong(5, config.getCreatedByUser().getId());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            
            // 设置创建时间
            ps.setTimestamp(6, Timestamp.valueOf(config.getCreatedAt()));
            
            // 设置更新时间
            ps.setTimestamp(7, Timestamp.valueOf(config.getUpdatedAt()));
            
            // 设置标签提示部分标题
            ps.setString(8, config.getTagPromptsSectionHeader());
            
            // 设置主观题部分标?
            ps.setString(9, config.getSubjectiveSectionHeader());
            
            // 设置标签提示分隔?
            ps.setString(10, config.getTagPromptSeparator());
            
            // 设置部分分隔?
            ps.setString(11, config.getSectionSeparator());
            
            // 设置最终指?
            ps.setString(12, config.getFinalInstruction());
            
            // 设置ID
            ps.setLong(13, config.getId());
            
            return ps;
        });

        return config;
    }

    /**
     * 根据ID查找配置
     *
     * @param id 配置ID
     * @return 配置对象
     */
    public Optional<EvaluationPromptAssemblyConfig> findById(Long id) {
        try {
            EvaluationPromptAssemblyConfig config = jdbcTemplate.queryForObject(
                SQL_FIND_BY_ID,
                new EvaluationPromptAssemblyConfigRowMapper(),
                id
            );
            return Optional.ofNullable(config);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 查找所有激活状态的配置
     * 
     * @return 激活状态的配置列表
     */
    public List<EvaluationPromptAssemblyConfig> findByIsActiveTrue() {
        return jdbcTemplate.query(SQL_FIND_BY_IS_ACTIVE_TRUE, new EvaluationPromptAssemblyConfigRowMapper());
    }
    
    /**
     * 根据名称查找配置
     * 
     * @param name 配置名称
     * @return 匹配的配置列?
     */
    public List<EvaluationPromptAssemblyConfig> findByName(String name) {
        return jdbcTemplate.query(SQL_FIND_BY_NAME, new EvaluationPromptAssemblyConfigRowMapper(), name);
    }
    
    /**
     * 根据创建者ID查找配置
     * 
     * @param userId 用户ID
     * @return 该用户创建的配置列表
     */
    public List<EvaluationPromptAssemblyConfig> findByCreatedByUserId(Long userId) {
        return jdbcTemplate.query(SQL_FIND_BY_CREATED_BY_USER_ID, new EvaluationPromptAssemblyConfigRowMapper(), userId);
    }
    
    /**
     * 查找所有配?
     *
     * @return 所有配置列?
     */
    public List<EvaluationPromptAssemblyConfig> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new EvaluationPromptAssemblyConfigRowMapper());
    }
    
    /**
     * 删除配置
     *
     * @param config 配置对象
     */
    public void delete(EvaluationPromptAssemblyConfig config) {
        jdbcTemplate.update(SQL_DELETE, config.getId());
    }

    /**
     * 配置行映射器
     */
    private class EvaluationPromptAssemblyConfigRowMapper implements RowMapper<EvaluationPromptAssemblyConfig> {
        @Override
        public EvaluationPromptAssemblyConfig mapRow(ResultSet rs, int rowNum) throws SQLException {
            EvaluationPromptAssemblyConfig config = new EvaluationPromptAssemblyConfig();
            
            // 设置ID和基本属?
            config.setId(rs.getLong("id"));
            config.setName(rs.getString("name"));
            config.setDescription(rs.getString("description"));
            config.setIsActive(rs.getBoolean("is_active"));
            config.setBaseSystemPrompt(rs.getString("base_system_prompt"));
            
            // 设置创建?
            Long createdByUserId = rs.getLong("created_by_user_id");
            if (!rs.wasNull()) {
                UserRepository.findById(createdByUserId).ifPresent(config::setCreatedByUser);
            }
            
            // 设置时间
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                config.setCreatedAt(createdAt.toLocalDateTime());
            }
            
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                config.setUpdatedAt(updatedAt.toLocalDateTime());
            }
            
            // 设置其他属?
            config.setTagPromptsSectionHeader(rs.getString("tag_prompts_section_header"));
            config.setSubjectiveSectionHeader(rs.getString("subjective_section_header"));
            config.setTagPromptSeparator(rs.getString("tag_prompt_separator"));
            config.setSectionSeparator(rs.getString("section_separator"));
            config.setFinalInstruction(rs.getString("final_instruction"));
            
            return config;
        }
    }
} 
