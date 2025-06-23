package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.DatasetVersion;
import com.example.demo.entity.jdbc.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
 * 基于JDBC的数据集版本仓库实现
 */
@Repository
public class DatasetVersionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final ChangeLogRepository changeLogRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO DATASET_VERSIONS (VERSION_NUMBER, NAME, DESCRIPTION, CREATION_TIME, " +
            "CREATED_BY_USER_ID, CREATED_CHANGE_LOG_ID, DELETED_AT) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE DATASET_VERSIONS SET VERSION_NUMBER=?, NAME=?, DESCRIPTION=?, CREATION_TIME=?, " +
            "CREATED_BY_USER_ID=?, CREATED_CHANGE_LOG_ID=?, DELETED_AT=? " +
            "WHERE ID=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM DATASET_VERSIONS WHERE ID=?";
    
    private static final String SQL_FIND_BY_VERSION_NUMBER = 
            "SELECT * FROM DATASET_VERSIONS WHERE VERSION_NUMBER=?";
    
    private static final String SQL_FIND_BY_NAME_CONTAINING = 
            "SELECT * FROM DATASET_VERSIONS WHERE LOWER(NAME) LIKE LOWER(?) ORDER BY CREATION_TIME DESC LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_BY_NAME_CONTAINING = 
            "SELECT COUNT(*) FROM DATASET_VERSIONS WHERE LOWER(NAME) LIKE LOWER(?)";
    
    private static final String SQL_FIND_BY_CREATED_BY_USER_ID = 
            "SELECT * FROM DATASET_VERSIONS WHERE CREATED_BY_USER_ID=? ORDER BY CREATION_TIME DESC LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_BY_CREATED_BY_USER_ID = 
            "SELECT COUNT(*) FROM DATASET_VERSIONS WHERE CREATED_BY_USER_ID=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM DATASET_VERSIONS ORDER BY CREATION_TIME DESC LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_ALL = 
            "SELECT COUNT(*) FROM DATASET_VERSIONS";
    
    private static final String SQL_FIND_BY_DELETED_AT_IS_NULL = 
            "SELECT * FROM DATASET_VERSIONS WHERE DELETED_AT IS NULL ORDER BY CREATION_TIME DESC LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_BY_DELETED_AT_IS_NULL = 
            "SELECT COUNT(*) FROM DATASET_VERSIONS WHERE DELETED_AT IS NULL";
    
    private static final String SQL_SOFT_DELETE = 
            "UPDATE DATASET_VERSIONS SET DELETED_AT=? WHERE ID=?";
    
    private static final String SQL_RESTORE = 
            "UPDATE DATASET_VERSIONS SET DELETED_AT=NULL WHERE ID=?";
    
    private static final String SQL_DELETE = 
            "DELETE FROM DATASET_VERSIONS WHERE ID=?";

    @Autowired
    public DatasetVersionRepository(JdbcTemplate jdbcTemplate, 
                                      UserRepository userRepository,
                                      ChangeLogRepository changeLogRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.changeLogRepository = changeLogRepository;
    }

    /**
     * 保存数据集版本
     *
     * @param datasetVersion 数据集版本对象
     * @return 带有ID的数据集版本对象
     */
    public DatasetVersion save(DatasetVersion datasetVersion) {
        if (datasetVersion.getId() == null) {
            return insert(datasetVersion);
        } else {
            return update(datasetVersion);
        }
    }

    /**
     * 插入新数据集版本
     *
     * @param datasetVersion 数据集版本对象
     * @return 带有ID的数据集版本对象
     */
    private DatasetVersion insert(DatasetVersion datasetVersion) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        // 设置默认创建时间
        if (datasetVersion.getCreationTime() == null) {
            datasetVersion.setCreationTime(LocalDateTime.now());
        }

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置版本号
            ps.setString(1, datasetVersion.getVersionNumber());
            
            // 设置名称
            ps.setString(2, datasetVersion.getName());
            
            // 设置描述
            if (datasetVersion.getDescription() != null) {
                ps.setString(3, datasetVersion.getDescription());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            
            // 设置创建时间
            ps.setTimestamp(4, Timestamp.valueOf(datasetVersion.getCreationTime()));
            
            // 设置创建者用户ID
            ps.setLong(5, datasetVersion.getCreatedByUser().getId());
            
            // 设置创建变更日志ID
            if (datasetVersion.getCreatedChangeLog() != null && datasetVersion.getCreatedChangeLog().getId() != null) {
                ps.setLong(6, datasetVersion.getCreatedChangeLog().getId());
            } else {
                ps.setNull(6, Types.BIGINT);
            }
            
            // 设置删除时间
            if (datasetVersion.getDeletedAt() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(datasetVersion.getDeletedAt()));
            } else {
                ps.setNull(7, Types.TIMESTAMP);
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            datasetVersion.setId(key.longValue());
        }

        return datasetVersion;
    }

    /**
     * 更新数据集版本
     *
     * @param datasetVersion 数据集版本对象
     * @return 更新后的数据集版本对象
     */
    private DatasetVersion update(DatasetVersion datasetVersion) {
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_UPDATE);
            
            // 设置版本号
            ps.setString(1, datasetVersion.getVersionNumber());
            
            // 设置名称
            ps.setString(2, datasetVersion.getName());
            
            // 设置描述
            if (datasetVersion.getDescription() != null) {
                ps.setString(3, datasetVersion.getDescription());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            
            // 设置创建时间
            ps.setTimestamp(4, Timestamp.valueOf(datasetVersion.getCreationTime()));
            
            // 设置创建者用户ID
            ps.setLong(5, datasetVersion.getCreatedByUser().getId());
            
            // 设置创建变更日志ID
            if (datasetVersion.getCreatedChangeLog() != null && datasetVersion.getCreatedChangeLog().getId() != null) {
                ps.setLong(6, datasetVersion.getCreatedChangeLog().getId());
            } else {
                ps.setNull(6, Types.BIGINT);
            }
            
            // 设置删除时间
            if (datasetVersion.getDeletedAt() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(datasetVersion.getDeletedAt()));
            } else {
                ps.setNull(7, Types.TIMESTAMP);
            }
            
            // 设置ID
            ps.setLong(8, datasetVersion.getId());
            
            return ps;
        });

        return datasetVersion;
    }

    /**
     * 根据ID查找数据集版本
     *
     * @param id 数据集版本ID
     * @return 数据集版本对象
     */
    public Optional<DatasetVersion> findById(Long id) {
        try {
            DatasetVersion datasetVersion = jdbcTemplate.queryForObject(
                SQL_FIND_BY_ID, 
                new DatasetVersionRowMapper(), 
                id
            );
            return Optional.ofNullable(datasetVersion);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据版本号查找数据集版本
     *
     * @param versionNumber 版本号
     * @return 数据集版本对象
     */
    public Optional<DatasetVersion> findByVersionNumber(String versionNumber) {
        try {
            DatasetVersion datasetVersion = jdbcTemplate.queryForObject(
                SQL_FIND_BY_VERSION_NUMBER, 
                new DatasetVersionRowMapper(), 
                versionNumber
            );
            return Optional.ofNullable(datasetVersion);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据名称模糊查询数据集版本
     *
     * @param name 名称关键字
     * @return 数据集版本列表
     */
    public List<DatasetVersion> findByNameContaining(String name) {
        String searchPattern = "%" + name + "%";
        return jdbcTemplate.query(
                SQL_FIND_BY_NAME_CONTAINING,
                new Object[]{searchPattern, Integer.MAX_VALUE, 0},
                new DatasetVersionRowMapper()
        );
    }

    /**
     * 根据创建者用户ID查询数据集版本
     *
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<DatasetVersion> findByCreatedByUserId(Long userId, Pageable pageable) {
        // 查询总数
        Integer total = jdbcTemplate.queryForObject(
            SQL_COUNT_BY_CREATED_BY_USER_ID,
            Integer.class,
            userId
        );
        
        // 查询数据
        List<DatasetVersion> content = jdbcTemplate.query(
            SQL_FIND_BY_CREATED_BY_USER_ID,
            new DatasetVersionRowMapper(),
            userId,
            pageable.getPageSize(),
            pageable.getOffset()
        );
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 分页查询所有数据集版本
     *
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<DatasetVersion> findAll(Pageable pageable) {
        // 查询总数
        Integer total = jdbcTemplate.queryForObject(
            SQL_COUNT_ALL,
            Integer.class
        );
        
        // 查询数据
        List<DatasetVersion> content = jdbcTemplate.query(
            SQL_FIND_ALL,
            new DatasetVersionRowMapper(),
            pageable.getPageSize(),
            pageable.getOffset()
        );
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 查询未删除的数据集版本
     *
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<DatasetVersion> findByDeletedAtIsNull(Pageable pageable) {
        // 查询总数
        Integer total = jdbcTemplate.queryForObject(
            SQL_COUNT_BY_DELETED_AT_IS_NULL,
            Integer.class
        );
        
        // 查询数据
        List<DatasetVersion> content = jdbcTemplate.query(
            SQL_FIND_BY_DELETED_AT_IS_NULL,
            new DatasetVersionRowMapper(),
            pageable.getPageSize(),
            pageable.getOffset()
        );
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 软删除数据集版本
     *
     * @param datasetVersion 数据集版本对象
     */
    public void softDelete(DatasetVersion datasetVersion) {
        LocalDateTime now = LocalDateTime.now();
        datasetVersion.setDeletedAt(now);
        jdbcTemplate.update(SQL_SOFT_DELETE, Timestamp.valueOf(now), datasetVersion.getId());
    }

    /**
     * 恢复软删除的数据集版本
     *
     * @param datasetVersion 数据集版本对象
     */
    public void restore(DatasetVersion datasetVersion) {
        datasetVersion.setDeletedAt(null);
        jdbcTemplate.update(SQL_RESTORE, datasetVersion.getId());
    }

    /**
     * 删除数据集版本
     *
     * @param datasetVersion 数据集版本对象
     */
    public void delete(DatasetVersion datasetVersion) {
        jdbcTemplate.update(SQL_DELETE, datasetVersion.getId());
    }

    /**
     * 查找所有未删除的数据集版本
     *
     * @return 未删除的数据集版本列表
     */
    public List<DatasetVersion> findAllActiveVersions() {
        return jdbcTemplate.query(
                SQL_FIND_BY_DELETED_AT_IS_NULL,
                new Object[]{Integer.MAX_VALUE, 0},
                new DatasetVersionRowMapper()
        );
    }

    /**
     * 检查版本号是否已存在
     *
     * @param versionNumber 版本号
     * @return 是否存在
     */
    public boolean existsByVersionNumber(String versionNumber) {
        try {
            return findByVersionNumber(versionNumber).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 数据集版本行映射器
     */
    private class DatasetVersionRowMapper implements RowMapper<DatasetVersion> {
        @Override
        public DatasetVersion mapRow(ResultSet rs, int rowNum) throws SQLException {
            DatasetVersion datasetVersion = new DatasetVersion();
            
            // 设置ID和基本属性
            datasetVersion.setId(rs.getLong("ID"));
            datasetVersion.setVersionNumber(rs.getString("VERSION_NUMBER"));
            datasetVersion.setName(rs.getString("NAME"));
            datasetVersion.setDescription(rs.getString("DESCRIPTION"));
            
            // 设置创建时间
            Timestamp creationTime = rs.getTimestamp("CREATION_TIME");
            if (creationTime != null) {
                datasetVersion.setCreationTime(creationTime.toLocalDateTime());
            }
            
            // 设置删除时间
            Timestamp deletedAt = rs.getTimestamp("DELETED_AT");
            if (deletedAt != null) {
                datasetVersion.setDeletedAt(deletedAt.toLocalDateTime());
            }
            
            // 获取创建者用户
            Long createdByUserId = rs.getLong("CREATED_BY_USER_ID");
            userRepository.findById(createdByUserId).ifPresent(datasetVersion::setCreatedByUser);
            
            // 获取创建变更日志
            Long createdChangeLogId = rs.getLong("CREATED_CHANGE_LOG_ID");
            if (!rs.wasNull()) {
                changeLogRepository.findById(createdChangeLogId).ifPresent(datasetVersion::setCreatedChangeLog);
            }
            
            return datasetVersion;
        }
    }
} 
