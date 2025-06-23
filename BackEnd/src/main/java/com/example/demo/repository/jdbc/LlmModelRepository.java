package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.LlmModel;
import com.example.demo.entity.jdbc.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于JDBC的LLM模型仓库实现
 */
@Repository
public class LlmModelRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository UserRepository;
    private final ObjectMapper objectMapper;

    private static final String SQL_INSERT = 
            "INSERT INTO llm_models (name, provider, version, description, api_url, api_key, api_type, model_parameters, created_at, created_by_user_id, created_change_log_id, deleted_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE llm_models SET name=?, provider=?, version=?, description=?, api_url=?, api_key=?, api_type=?, model_parameters=?, created_by_user_id=?, created_change_log_id=?, deleted_at=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM llm_models WHERE id=?";
    
    private static final String SQL_FIND_BY_NAME = 
            "SELECT * FROM llm_models WHERE name=?";
    
    private static final String SQL_FIND_BY_PROVIDER = 
            "SELECT * FROM llm_models WHERE provider=?";
    
    private static final String SQL_FIND_BY_DELETED_AT_IS_NULL = 
            "SELECT * FROM llm_models WHERE deleted_at IS NULL";
    
    private static final String SQL_FIND_BY_PROVIDER_AND_VERSION = 
            "SELECT * FROM llm_models WHERE provider=? AND version=?";
    
    private static final String SQL_EXISTS_BY_NAME_AND_API_URL = 
            "SELECT COUNT(*) FROM llm_models WHERE name=? AND api_url=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM llm_models";
    
    private static final String SQL_SOFT_DELETE = 
            "UPDATE llm_models SET deleted_at=? WHERE id=?";

    @Autowired
    public LlmModelRepository(JdbcTemplate jdbcTemplate, UserRepository UserRepository, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.UserRepository = UserRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存LLM模型
     *
     * @param model LLM模型对象
     * @return 带有ID的LLM模型对象
     */
    public LlmModel save(LlmModel model) {
        if (model.getId() == null) {
            return insert(model);
        } else {
            return update(model);
        }
    }

    /**
     * 插入新LLM模型
     *
     * @param model LLM模型对象
     * @return 带有ID的LLM模型对象
     */
    private LlmModel insert(LlmModel model) {
        if (model.getCreatedAt() == null) {
            model.setCreatedAt(LocalDateTime.now());
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            ps.setString(1, model.getName());
            ps.setString(2, model.getProvider());
            ps.setString(3, model.getVersion());
            ps.setString(4, model.getDescription());
            ps.setString(5, model.getApiUrl());
            ps.setString(6, model.getApiKey());
            ps.setString(7, model.getApiType());
            
            // 将模型参数转换为JSON字符串
            if (model.getModelParameters() != null) {
                try {
                    ps.setString(8, objectMapper.writeValueAsString(model.getModelParameters()));
                } catch (JsonProcessingException e) {
                    ps.setString(8, "{}");
                }
            } else {
                ps.setString(8, "{}");
            }
            
            ps.setTimestamp(9, Timestamp.valueOf(model.getCreatedAt()));
            
            // 设置创建用户ID
            if (model.getCreatedByUser() != null && model.getCreatedByUser().getId() != null) {
                ps.setLong(10, model.getCreatedByUser().getId());
            } else {
                ps.setNull(10, java.sql.Types.BIGINT);
            }
            
            // 设置创建变更日志ID
            if (model.getCreatedChangeLog() != null && model.getCreatedChangeLog().getId() != null) {
                ps.setLong(11, model.getCreatedChangeLog().getId());
            } else {
                ps.setNull(11, java.sql.Types.BIGINT);
            }
            
            // 设置删除时间
            if (model.getDeletedAt() != null) {
                ps.setTimestamp(12, Timestamp.valueOf(model.getDeletedAt()));
            } else {
                ps.setNull(12, java.sql.Types.TIMESTAMP);
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            model.setId(key.longValue());
        }
        return model;
    }

    /**
     * 更新LLM模型
     *
     * @param model LLM模型对象
     * @return 更新后的LLM模型对象
     */
    private LlmModel update(LlmModel model) {
        String modelParametersJson;
        try {
            modelParametersJson = model.getModelParameters() != null ? 
                    objectMapper.writeValueAsString(model.getModelParameters()) : "{}";
        } catch (JsonProcessingException e) {
            modelParametersJson = "{}";
        }
        
        jdbcTemplate.update(SQL_UPDATE,
                model.getName(),
                model.getProvider(),
                model.getVersion(),
                model.getDescription(),
                model.getApiUrl(),
                model.getApiKey(),
                model.getApiType(),
                modelParametersJson,
                model.getCreatedByUser() != null ? model.getCreatedByUser().getId() : null,
                model.getCreatedChangeLog() != null ? model.getCreatedChangeLog().getId() : null,
                model.getDeletedAt() != null ? Timestamp.valueOf(model.getDeletedAt()) : null,
                model.getId());

        return model;
    }

    /**
     * 根据ID查找LLM模型
     *
     * @param id LLM模型ID
     * @return LLM模型的Optional包装
     */
    public Optional<LlmModel> findById(Long id) {
        try {
            LlmModel model = jdbcTemplate.queryForObject(SQL_FIND_BY_ID, new Object[]{id}, new LlmModelRowMapper());
            return Optional.ofNullable(model);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    /**
     * 根据模型名称查找
     *
     * @param name 模型名称
     * @return 匹配的模型列表
     */
    public List<LlmModel> findByName(String name) {
        return jdbcTemplate.query(
                SQL_FIND_BY_NAME,
                new Object[]{name},
                new LlmModelRowMapper()
        );
    }
    
    /**
     * 根据提供商查找
     *
     * @param provider 提供商名称
     * @return 匹配的模型列表
     */
    public List<LlmModel> findByProvider(String provider) {
        return jdbcTemplate.query(
                SQL_FIND_BY_PROVIDER,
                new Object[]{provider},
                new LlmModelRowMapper()
        );
    }
    
    /**
     * 查找所有未删除的模型
     *
     * @return 未删除的模型列表
     */
    public List<LlmModel> findByDeletedAtIsNull() {
        return jdbcTemplate.query(SQL_FIND_BY_DELETED_AT_IS_NULL, new LlmModelRowMapper());
    }
    
    /**
     * 根据提供商和版本查找
     *
     * @param provider 提供商名称
     * @param version 版本
     * @return 匹配的模型列表
     */
    public List<LlmModel> findByProviderAndVersion(String provider, String version) {
        return jdbcTemplate.query(
                SQL_FIND_BY_PROVIDER_AND_VERSION,
                new Object[]{provider, version},
                new LlmModelRowMapper()
        );
    }
    
    /**
     * 检查指定名称和API URL的模型是否存在
     *
     * @param name 模型名称
     * @param apiUrl API URL
     * @return 如果存在返回true，否则返回false
     */
    public boolean existsByNameAndApiUrl(String name, String apiUrl) {
        Integer count = jdbcTemplate.queryForObject(
                SQL_EXISTS_BY_NAME_AND_API_URL,
                Integer.class,
                name, apiUrl
        );
        return count != null && count > 0;
    }
    
    /**
     * 查找所有LLM模型
     *
     * @return LLM模型列表
     */
    public List<LlmModel> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new LlmModelRowMapper());
    }

    /**
     * 软删除LLM模型
     *
     * @param id LLM模型ID
     * @return 是否成功
     */
    public boolean softDelete(Long id) {
        int affected = jdbcTemplate.update(SQL_SOFT_DELETE, Timestamp.valueOf(LocalDateTime.now()), id);
        return affected > 0;
    }

    /**
     * LLM模型行映射器
     */
    private class LlmModelRowMapper implements RowMapper<LlmModel> {
        @Override
        public LlmModel mapRow(ResultSet rs, int rowNum) throws SQLException {
            LlmModel model = new LlmModel();
            model.setId(rs.getLong("id"));
            model.setName(rs.getString("name"));
            model.setProvider(rs.getString("provider"));
            model.setVersion(rs.getString("version"));
            model.setDescription(rs.getString("description"));
            model.setApiUrl(rs.getString("api_url"));
            model.setApiKey(rs.getString("api_key"));
            model.setApiType(rs.getString("api_type"));
            
            // 解析JSON字符串为Map
            String modelParametersJson = rs.getString("model_parameters");
            if (modelParametersJson != null && !modelParametersJson.isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> modelParameters = objectMapper.readValue(modelParametersJson, Map.class);
                    model.setModelParameters(modelParameters);
                } catch (JsonProcessingException e) {
                    model.setModelParameters(new HashMap<>());
                }
            } else {
                model.setModelParameters(new HashMap<>());
            }
            
            // 设置时间
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                model.setCreatedAt(createdAt.toLocalDateTime());
            }
            
            Timestamp deletedAt = rs.getTimestamp("deleted_at");
            if (deletedAt != null) {
                model.setDeletedAt(deletedAt.toLocalDateTime());
            }
            
            // 设置创建者用户
            Long createdByUserId = rs.getLong("created_by_user_id");
            if (!rs.wasNull()) {
                UserRepository.findById(createdByUserId).ifPresent(user -> model.setCreatedByUser(user));
            }
            
            Long createdChangeLogId = rs.getLong("created_change_log_id");
            if (!rs.wasNull()) {
                ChangeLog changeLog = new ChangeLog();
                changeLog.setId(createdChangeLogId);
                model.setCreatedChangeLog(changeLog);
            }
            
            return model;
        }
    }
} 
