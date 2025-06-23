package com.example.demo.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC工具类
 */
@Component
public class JdbcUtils {
    
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    
    @Autowired
    public JdbcUtils(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }
    
    /**
     * 执行查询，返回对象列表
     *
     * @param sql SQL语句
     * @param params 查询参数
     * @param rowMapper 行映射器
     * @return 对象列表
     */
    public <T> List<T> query(String sql, Object[] params, RowMapper<T> rowMapper) {
        return jdbcTemplate.query(sql, params, rowMapper);
    }
    
    /**
     * 执行查询，返回单个对象
     *
     * @param sql SQL语句
     * @param params 查询参数
     * @param rowMapper 行映射器
     * @return 单个对象的Optional包装
     */
    public <T> Optional<T> queryForObject(String sql, Object[] params, RowMapper<T> rowMapper) {
        try {
            T result = jdbcTemplate.queryForObject(sql, params, rowMapper);
            return Optional.ofNullable(result);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    /**
     * 执行插入并返回生成的主键
     *
     * @param sql SQL语句
     * @param params 参数
     * @return 生成的主键
     */
    public Long insert(String sql, Object... params) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps;
        }, keyHolder);
        
        Number key = keyHolder.getKey();
        return (key != null) ? key.longValue() : null;
    }
    
    /**
     * 使用命名参数执行查询
     *
     * @param sql SQL语句
     * @param paramMap 命名参数映射
     * @param rowMapper 行映射器
     * @return 对象列表
     */
    public <T> List<T> queryWithNamedParams(String sql, Map<String, Object> paramMap, RowMapper<T> rowMapper) {
        return namedParameterJdbcTemplate.query(sql, paramMap, rowMapper);
    }
    
    /**
     * 使用命名参数执行插入并返回生成的主键
     *
     * @param sql SQL语句
     * @param params 命名参数
     * @return 生成的主键
     */
    public Long insertWithNamedParams(String sql, Map<String, Object> params) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource paramSource = new MapSqlParameterSource(params);
        
        namedParameterJdbcTemplate.update(sql, paramSource, keyHolder, new String[]{"id"});
        
        Number key = keyHolder.getKey();
        return (key != null) ? key.longValue() : null;
    }
    
    /**
     * 执行更新操作
     *
     * @param sql SQL语句
     * @param params 参数
     * @return 影响的行数
     */
    public int update(String sql, Object... params) {
        return jdbcTemplate.update(sql, params);
    }
    
    /**
     * 使用命名参数执行更新操作
     *
     * @param sql SQL语句
     * @param params 命名参数
     * @return 影响的行数
     */
    public int updateWithNamedParams(String sql, Map<String, Object> params) {
        return namedParameterJdbcTemplate.update(sql, params);
    }
    
    /**
     * 执行删除操作
     *
     * @param sql SQL语句
     * @param params 参数
     * @return 影响的行数
     */
    public int delete(String sql, Object... params) {
        return jdbcTemplate.update(sql, params);
    }
    
    /**
     * 批量更新
     *
     * @param sql SQL语句
     * @param batchArgs 批量参数
     * @return 每批处理的影响行数
     */
    public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
        return jdbcTemplate.batchUpdate(sql, batchArgs);
    }
} 