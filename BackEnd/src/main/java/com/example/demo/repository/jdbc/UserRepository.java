package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.User;
import com.example.demo.entity.jdbc.UserRole;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于JDBC的用户仓库实现
 */
@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String SQL_INSERT = 
            "INSERT INTO users (username, password, name, contact_info, role, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE users SET username=?, password=?, name=?, contact_info=?, role=?, updated_at=? WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM users WHERE id=? AND deleted_at IS NULL";
    
    private static final String SQL_FIND_BY_USERNAME = 
            "SELECT * FROM users WHERE username=? AND deleted_at IS NULL";
    
    private static final String SQL_EXISTS_BY_USERNAME = 
            "SELECT COUNT(*) FROM users WHERE username=? AND deleted_at IS NULL";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM users WHERE deleted_at IS NULL";
    
    private static final String SQL_FIND_ALL_PAGEABLE = 
            "SELECT * FROM users WHERE deleted_at IS NULL ORDER BY id LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_ALL = 
            "SELECT COUNT(*) FROM users WHERE deleted_at IS NULL";
    
    private static final String SQL_SEARCH_BY_KEYWORD = 
            "SELECT * FROM users WHERE (username LIKE ? OR name LIKE ? OR contact_info LIKE ?) " +
            "AND deleted_at IS NULL ORDER BY id LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_SEARCH_BY_KEYWORD = 
            "SELECT COUNT(*) FROM users WHERE (username LIKE ? OR name LIKE ? OR contact_info LIKE ?) " +
            "AND deleted_at IS NULL";
    
    private static final String SQL_SOFT_DELETE = 
            "UPDATE users SET deleted_at=? WHERE id=?";

    private static final String SQL_EXISTS_BY_ID = 
            "SELECT COUNT(*) FROM users WHERE id=? AND deleted_at IS NULL";

    @Autowired
    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 保存用户
     *
     * @param user 用户对象
     * @return 带有ID的用户对象
     */
    public User save(User user) {
        if (user.getId() == null) {
            return insert(user);
        } else {
            return update(user);
        }
    }

    /**
     * 插入新用户
     *
     * @param user 用户对象
     * @return 带有ID的用户对象
     */
    private User insert(User user) {
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getName());
            ps.setString(4, user.getContactInfo());
            ps.setString(5, user.getRole().name());
            ps.setTimestamp(6, Timestamp.valueOf(user.getCreatedAt()));
            ps.setTimestamp(7, Timestamp.valueOf(user.getUpdatedAt()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            user.setId(key.longValue());
        }
        return user;
    }

    /**
     * 更新用户
     *
     * @param user 用户对象
     * @return 更新后的用户对象
     */
    private User update(User user) {
        user.setUpdatedAt(LocalDateTime.now());

        jdbcTemplate.update(SQL_UPDATE,
                user.getUsername(),
                user.getPassword(),
                user.getName(),
                user.getContactInfo(),
                user.getRole().name(),
                Timestamp.valueOf(user.getUpdatedAt()),
                user.getId());

        return user;
    }

    /**
     * 根据ID查找用户
     *
     * @param id 用户ID
     * @return 用户的Optional包装
     */
    public Optional<User> findById(Long id) {
        try {
            User user = jdbcTemplate.queryForObject(SQL_FIND_BY_ID, new Object[]{id}, new UserRowMapper());
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户的Optional包装
     */
    public Optional<User> findByUsername(String username) {
        try {
            User user = jdbcTemplate.queryForObject(SQL_FIND_BY_USERNAME, new Object[]{username}, new UserRowMapper());
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 检查用户名是否已存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(SQL_EXISTS_BY_USERNAME, Integer.class, username);
        return count != null && count > 0;
    }

    /**
     * 查找所有用户
     *
     * @return 用户列表
     */
    public List<User> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new UserRowMapper());
    }
    
    /**
     * 分页查询所有用户
     *
     * @param pageable 分页参数
     * @return 用户分页数据
     */
    public Page<User> findAll(Pageable pageable) {
        List<User> users = jdbcTemplate.query(
                SQL_FIND_ALL_PAGEABLE,
                new Object[]{pageable.getPageSize(), pageable.getOffset()},
                new UserRowMapper()
        );
        
        Integer count = jdbcTemplate.queryForObject(SQL_COUNT_ALL, Integer.class);
        return new PageImpl<>(users, pageable, count != null ? count : 0);
    }
    
    /**
     * 根据关键词搜索用户并分页
     *
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 用户分页数据
     */
    public Page<User> searchByKeyword(String keyword, Pageable pageable) {
        String searchTerm = "%" + keyword + "%";
        
        List<User> users = jdbcTemplate.query(
                SQL_SEARCH_BY_KEYWORD,
                new Object[]{searchTerm, searchTerm, searchTerm, pageable.getPageSize(), pageable.getOffset()},
                new UserRowMapper()
        );
        
        Integer count = jdbcTemplate.queryForObject(
                SQL_COUNT_SEARCH_BY_KEYWORD, 
                Integer.class, 
                searchTerm, searchTerm, searchTerm
        );
        
        return new PageImpl<>(users, pageable, count != null ? count : 0);
    }

    /**
     * 软删除用户
     *
     * @param id 用户ID
     * @return 是否成功
     */
    public boolean softDelete(Long id) {
        int affected = jdbcTemplate.update(SQL_SOFT_DELETE, Timestamp.valueOf(LocalDateTime.now()), id);
        return affected > 0;
    }

    /**
     * 检查指定ID的用户是否存在
     *
     * @param id 用户ID
     * @return 是否存在
     */
    public boolean existsById(Long id) {
        Integer count = jdbcTemplate.queryForObject(SQL_EXISTS_BY_ID, Integer.class, id);
        return count != null && count > 0;
    }

    /**
     * 用户行映射器
     */
    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setUsername(rs.getString("username"));
            user.setPassword(rs.getString("password"));
            user.setName(rs.getString("name"));
            user.setContactInfo(rs.getString("contact_info"));
            user.setRole(UserRole.valueOf(rs.getString("role")));
            
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                user.setCreatedAt(createdAt.toLocalDateTime());
            }
            
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                user.setUpdatedAt(updatedAt.toLocalDateTime());
            }
            
            Timestamp deletedAt = rs.getTimestamp("deleted_at");
            if (deletedAt != null) {
                user.setDeletedAt(deletedAt.toLocalDateTime());
            }
            
            return user;
        }
    }
} 
