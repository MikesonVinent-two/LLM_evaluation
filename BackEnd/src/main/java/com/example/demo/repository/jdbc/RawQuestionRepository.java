package com.example.demo.repository.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

import com.example.demo.entity.jdbc.RawAnswer;
import com.example.demo.entity.jdbc.RawQuestion;

/**
 * 基于JDBC的原始问题仓库实?
 */
@Repository
public class RawQuestionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RawAnswerRepository rawAnswerRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO RAW_QUESTIONS (TITLE, CONTENT, SOURCE_URL, SOURCE_SITE, CRAWL_TIME, " +
            "TAGS, OTHER_METADATA) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE RAW_QUESTIONS SET TITLE=?, CONTENT=?, SOURCE_URL=?, SOURCE_SITE=?, " +
            "CRAWL_TIME=?, TAGS=?, OTHER_METADATA=? " +
            "WHERE ID=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM RAW_QUESTIONS WHERE ID=?";
    
    private static final String SQL_EXISTS_BY_SOURCE_URL = 
            "SELECT COUNT(*) FROM RAW_QUESTIONS WHERE SOURCE_URL=?";
    
    private static final String SQL_FIND_BY_IDS_ORDER_BY_ID_DESC = 
            "SELECT * FROM RAW_QUESTIONS WHERE ID IN (%s) ORDER BY ID DESC LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_BY_IDS = 
            "SELECT COUNT(*) FROM RAW_QUESTIONS WHERE ID IN (%s)";
    
    private static final String SQL_FIND_BY_NOT_IN_IDS_ORDER_BY_ID_DESC = 
            "SELECT * FROM RAW_QUESTIONS WHERE ID NOT IN (%s) ORDER BY ID DESC LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_BY_NOT_IN_IDS = 
            "SELECT COUNT(*) FROM RAW_QUESTIONS WHERE ID NOT IN (%s)";
    
    private static final String SQL_FIND_BY_SOURCE_SITE = 
            "SELECT * FROM RAW_QUESTIONS WHERE LOWER(SOURCE_SITE) LIKE LOWER(?) ORDER BY ID DESC LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_BY_SOURCE_SITE = 
            "SELECT COUNT(*) FROM RAW_QUESTIONS WHERE LOWER(SOURCE_SITE) LIKE LOWER(?)";
    
    private static final String SQL_FIND_BY_TITLE_OR_CONTENT = 
            "SELECT * FROM RAW_QUESTIONS WHERE LOWER(TITLE) LIKE LOWER(?) OR LOWER(CONTENT) LIKE LOWER(?) " +
            "ORDER BY ID DESC LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_BY_TITLE_OR_CONTENT = 
            "SELECT COUNT(*) FROM RAW_QUESTIONS WHERE LOWER(TITLE) LIKE LOWER(?) OR LOWER(CONTENT) LIKE LOWER(?)";
    
    private static final String SQL_FIND_BY_TAG_NAMES = 
            "SELECT DISTINCT rq.* FROM RAW_QUESTIONS rq " +
            "JOIN RAW_QUESTION_TAGS qt ON rq.ID = qt.RAW_QUESTION_ID " +
            "JOIN TAGS t ON qt.TAG_ID = t.ID " +
            "WHERE t.TAG_NAME IN (%s) " +
            "GROUP BY rq.ID " +
            "HAVING COUNT(DISTINCT t.TAG_NAME) = ? " +
            "ORDER BY rq.ID DESC LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_BY_TAG_NAMES = 
            "SELECT COUNT(DISTINCT rq.ID) FROM RAW_QUESTIONS rq " +
            "JOIN RAW_QUESTION_TAGS qt ON rq.ID = qt.RAW_QUESTION_ID " +
            "JOIN TAGS t ON qt.TAG_ID = t.ID " +
            "WHERE t.TAG_NAME IN (%s) " +
            "GROUP BY rq.ID " +
            "HAVING COUNT(DISTINCT t.TAG_NAME) = ?";
    
    private static final String SQL_FIND_BY_IDS_AND_TAG_NAMES = 
            "SELECT DISTINCT rq.* FROM RAW_QUESTIONS rq " +
            "JOIN RAW_QUESTION_TAGS qt ON rq.ID = qt.RAW_QUESTION_ID " +
            "JOIN TAGS t ON qt.TAG_ID = t.ID " +
            "WHERE rq.ID IN (%s) AND t.TAG_NAME IN (%s) " +
            "GROUP BY rq.ID " +
            "HAVING COUNT(DISTINCT t.TAG_NAME) = ? " +
            "ORDER BY rq.ID DESC LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_BY_IDS_AND_TAG_NAMES = 
            "SELECT COUNT(DISTINCT rq.ID) FROM RAW_QUESTIONS rq " +
            "JOIN RAW_QUESTION_TAGS qt ON rq.ID = qt.RAW_QUESTION_ID " +
            "JOIN TAGS t ON qt.TAG_ID = t.ID " +
            "WHERE rq.ID IN (%s) AND t.TAG_NAME IN (%s) " +
            "GROUP BY rq.ID " +
            "HAVING COUNT(DISTINCT t.TAG_NAME) = ?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM RAW_QUESTIONS";
    
    private static final String SQL_DELETE = 
            "DELETE FROM RAW_QUESTIONS WHERE ID=?";

    @Autowired
    public RawQuestionRepository(JdbcTemplate jdbcTemplate, RawAnswerRepository rawAnswerRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.rawAnswerRepository = rawAnswerRepository;
    }

    /**
     * 保存原始问题
     *
     * @param rawQuestion 原始问题对象
     * @return 带有ID的原始问题对?
     */
    public RawQuestion save(RawQuestion rawQuestion) {
        if (rawQuestion.getId() == null) {
            return insert(rawQuestion);
        } else {
            return update(rawQuestion);
        }
    }

    /**
     * 插入新原始问?
     *
     * @param rawQuestion 原始问题对象
     * @return 带有ID的原始问题对?
     */
    private RawQuestion insert(RawQuestion rawQuestion) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        // 设置默认抓取时间
        if (rawQuestion.getCrawlTime() == null) {
            rawQuestion.setCrawlTime(LocalDateTime.now());
        }

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置标题
            ps.setString(1, rawQuestion.getTitle());
            
            // 设置内容
            if (rawQuestion.getContent() != null) {
                ps.setString(2, rawQuestion.getContent());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            
            // 设置来源URL
            ps.setString(3, rawQuestion.getSourceUrl());
            
            // 设置来源网站
            if (rawQuestion.getSourceSite() != null) {
                ps.setString(4, rawQuestion.getSourceSite());
            } else {
                ps.setNull(4, Types.VARCHAR);
            }
            
            // 设置抓取时间
            ps.setTimestamp(5, Timestamp.valueOf(rawQuestion.getCrawlTime()));
            
            // 设置标签
            if (rawQuestion.getTags() != null) {
                ps.setString(6, rawQuestion.getTags());
            } else {
                ps.setString(6, "[]");
            }
            
            // 设置其他元数?
            if (rawQuestion.getOtherMetadata() != null) {
                ps.setString(7, rawQuestion.getOtherMetadata());
            } else {
                ps.setString(7, "{}");
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            rawQuestion.setId(key.longValue());
        }

        return rawQuestion;
    }

    /**
     * 更新原始问题
     *
     * @param rawQuestion 原始问题对象
     * @return 更新后的原始问题对象
     */
    private RawQuestion update(RawQuestion rawQuestion) {
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_UPDATE);
            
            // 设置标题
            ps.setString(1, rawQuestion.getTitle());
            
            // 设置内容
            if (rawQuestion.getContent() != null) {
                ps.setString(2, rawQuestion.getContent());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            
            // 设置来源URL
            ps.setString(3, rawQuestion.getSourceUrl());
            
            // 设置来源网站
            if (rawQuestion.getSourceSite() != null) {
                ps.setString(4, rawQuestion.getSourceSite());
            } else {
                ps.setNull(4, Types.VARCHAR);
            }
            
            // 设置抓取时间
            ps.setTimestamp(5, Timestamp.valueOf(rawQuestion.getCrawlTime()));
            
            // 设置标签
            if (rawQuestion.getTags() != null) {
                ps.setString(6, rawQuestion.getTags());
            } else {
                ps.setString(6, "[]");
            }
            
            // 设置其他元数?
            if (rawQuestion.getOtherMetadata() != null) {
                ps.setString(7, rawQuestion.getOtherMetadata());
            } else {
                ps.setString(7, "{}");
            }
            
            // 设置ID
            ps.setLong(8, rawQuestion.getId());
            
            return ps;
        });

        return rawQuestion;
    }

    /**
     * 根据ID查找原始问题
     *
     * @param id 原始问题ID
     * @return 原始问题对象
     */
    public Optional<RawQuestion> findById(Long id) {
        try {
            RawQuestion rawQuestion = jdbcTemplate.queryForObject(
                SQL_FIND_BY_ID, 
                new RawQuestionRowMapper(), 
                id
            );
            return Optional.ofNullable(rawQuestion);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 检查是否存在指定来源URL的原始问?
     *
     * @param sourceUrl 来源URL
     * @return 是否存在
     */
    public boolean existsBySourceUrl(String sourceUrl) {
        Integer count = jdbcTemplate.queryForObject(
            SQL_EXISTS_BY_SOURCE_URL, 
            Integer.class, 
            sourceUrl
        );
        return count != null && count > 0;
    }

    /**
     * 根据ID列表查询并按ID降序排序
     *
     * @param ids ID列表
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<RawQuestion> findByIdInOrderByIdDesc(List<Long> ids, Pageable pageable) {
        if (ids.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // 构建SQL中的IN子句
        String placeholders = String.join(",", ids.stream()
            .map(id -> "?")
            .collect(Collectors.toList()));
        
        // 查询总数
        String countSql = String.format(SQL_COUNT_BY_IDS, placeholders);
        Integer total = jdbcTemplate.queryForObject(
            countSql,
            Integer.class,
            ids.toArray()
        );
        
        // 查询数据
        String querySql = String.format(SQL_FIND_BY_IDS_ORDER_BY_ID_DESC, placeholders);
        
        // 创建完整参数列表（包括分页参数）
        List<Object> params = new ArrayList<>(ids);
        params.add(pageable.getPageSize());
        params.add(pageable.getOffset());
        
        List<RawQuestion> content = jdbcTemplate.query(
            querySql,
            new RawQuestionRowMapper(),
            params.toArray()
        );
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 查询不在ID列表中的记录并按ID降序排序
     *
     * @param ids ID列表
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<RawQuestion> findByIdNotInOrderByIdDesc(List<Long> ids, Pageable pageable) {
        if (ids.isEmpty()) {
            // 如果ID列表为空，则查询所有记?
            return findAll(pageable);
        }
        
        // 构建SQL中的IN子句
        String placeholders = String.join(",", ids.stream()
            .map(id -> "?")
            .collect(Collectors.toList()));
        
        // 查询总数
        String countSql = String.format(SQL_COUNT_BY_NOT_IN_IDS, placeholders);
        Integer total = jdbcTemplate.queryForObject(
            countSql,
            Integer.class,
            ids.toArray()
        );
        
        // 查询数据
        String querySql = String.format(SQL_FIND_BY_NOT_IN_IDS_ORDER_BY_ID_DESC, placeholders);
        
        // 创建完整参数列表（包括分页参数）
        List<Object> params = new ArrayList<>(ids);
        params.add(pageable.getPageSize());
        params.add(pageable.getOffset());
        
        List<RawQuestion> content = jdbcTemplate.query(
            querySql,
            new RawQuestionRowMapper(),
            params.toArray()
        );
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 根据来源网站模糊查询
     *
     * @param sourceSite 来源网站
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<RawQuestion> findBySourceSiteContainingIgnoreCase(String sourceSite, Pageable pageable) {
        // 添加通配符用于模糊查?
        String likePattern = "%" + sourceSite + "%";
        
        // 查询总数
        Integer total = jdbcTemplate.queryForObject(
            SQL_COUNT_BY_SOURCE_SITE,
            Integer.class,
            likePattern
        );
        
        // 查询数据
        List<RawQuestion> content = jdbcTemplate.query(
            SQL_FIND_BY_SOURCE_SITE,
            new RawQuestionRowMapper(),
            likePattern,
            pageable.getPageSize(),
            pageable.getOffset()
        );
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 根据标题或内容模糊查?
     *
     * @param titleKeyword 标题关键?
     * @param contentKeyword 内容关键?
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<RawQuestion> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
            String titleKeyword, String contentKeyword, Pageable pageable) {
        // 添加通配符用于模糊查?
        String titleLikePattern = "%" + titleKeyword + "%";
        String contentLikePattern = "%" + contentKeyword + "%";
        
        // 查询总数
        Integer total = jdbcTemplate.queryForObject(
            SQL_COUNT_BY_TITLE_OR_CONTENT,
            Integer.class,
            titleLikePattern,
            contentLikePattern
        );
        
        // 查询数据
        List<RawQuestion> content = jdbcTemplate.query(
            SQL_FIND_BY_TITLE_OR_CONTENT,
            new RawQuestionRowMapper(),
            titleLikePattern,
            contentLikePattern,
            pageable.getPageSize(),
            pageable.getOffset()
        );
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 根据多个标签查询问题
     *
     * @param tagNames 标签名称列表
     * @param tagCount 标签数量
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<RawQuestion> findByTagNames(List<String> tagNames, Long tagCount, Pageable pageable) {
        if (tagNames.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // 构建SQL中的IN子句
        String placeholders = String.join(",", tagNames.stream()
            .map(tag -> "?")
            .collect(Collectors.toList()));
        
        // 查询总数
        String countSql = String.format(SQL_COUNT_BY_TAG_NAMES, placeholders);
        
        // 创建计数查询参数
        List<Object> countParams = new ArrayList<>(tagNames);
        countParams.add(tagCount);
        
        Integer total = 0;
        try {
            total = jdbcTemplate.queryForObject(
                countSql,
                Integer.class,
                countParams.toArray()
            );
        } catch (EmptyResultDataAccessException e) {
            // 如果没有结果，则总数为0
        }
        
        // 查询数据
        String querySql = String.format(SQL_FIND_BY_TAG_NAMES, placeholders);
        
                // 查询数据
        List<RawQuestion> content;
        if (pageable.isUnpaged()) {
            // 如果是不分页查询，不添加LIMIT和OFFSET参数
            String unpagedQuerySql = String.format(
                "SELECT DISTINCT rq.* FROM RAW_QUESTIONS rq " +
                "JOIN RAW_QUESTION_TAGS qt ON rq.ID = qt.RAW_QUESTION_ID " +
                "JOIN TAGS t ON qt.TAG_ID = t.ID " +
                "WHERE t.TAG_NAME IN (%s) " +
                "GROUP BY rq.ID " +
                "HAVING COUNT(DISTINCT t.TAG_NAME) = ? " +
                "ORDER BY rq.ID DESC", placeholders);
            
            List<Object> unpagedParams = new ArrayList<>(tagNames);
            unpagedParams.add(tagCount);
            
            content = jdbcTemplate.query(
                unpagedQuerySql,
                new RawQuestionRowMapper(),
                unpagedParams.toArray()
            );
        } else {
            // 分页查询
            List<Object> params = new ArrayList<>(tagNames);
            params.add(tagCount);
            params.add(pageable.getPageSize());
            params.add(pageable.getOffset());

            content = jdbcTemplate.query(
                querySql,
                new RawQuestionRowMapper(),
                params.toArray()
            );
        }
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 根据ID列表和标签名称查询问题
     *
     * @param ids 问题ID列表
     * @param tagNames 标签名称列表
     * @param tagCount 标签数量
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<RawQuestion> findByIdsAndTagNames(List<Long> ids, List<String> tagNames, Long tagCount, Pageable pageable) {
        if (ids.isEmpty() || tagNames.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // 构建ID的IN子句
        String idPlaceholders = String.join(",", ids.stream()
            .map(id -> "?")
            .collect(Collectors.toList()));
        
        // 构建标签的IN子句
        String tagPlaceholders = String.join(",", tagNames.stream()
            .map(tag -> "?")
            .collect(Collectors.toList()));
        
        // 查询总数
        String countSql = String.format(SQL_COUNT_BY_IDS_AND_TAG_NAMES, idPlaceholders, tagPlaceholders);
        
        // 创建计数查询参数
        List<Object> countParams = new ArrayList<>();
        countParams.addAll(ids);
        countParams.addAll(tagNames);
        countParams.add(tagCount);
        
        Integer total = 0;
        try {
            total = jdbcTemplate.queryForObject(
                countSql,
                Integer.class,
                countParams.toArray()
            );
        } catch (EmptyResultDataAccessException e) {
            // 如果没有结果，则总数为0
        }
        
        // 查询数据
        List<RawQuestion> content;
        if (pageable.isUnpaged()) {
            // 如果是不分页查询，不添加LIMIT和OFFSET参数
            String unpagedQuerySql = String.format(
                "SELECT DISTINCT rq.* FROM RAW_QUESTIONS rq " +
                "JOIN RAW_QUESTION_TAGS qt ON rq.ID = qt.RAW_QUESTION_ID " +
                "JOIN TAGS t ON qt.TAG_ID = t.ID " +
                "WHERE rq.ID IN (%s) AND t.TAG_NAME IN (%s) " +
                "GROUP BY rq.ID " +
                "HAVING COUNT(DISTINCT t.TAG_NAME) = ? " +
                "ORDER BY rq.ID DESC", idPlaceholders, tagPlaceholders);
            
            List<Object> unpagedParams = new ArrayList<>();
            unpagedParams.addAll(ids);
            unpagedParams.addAll(tagNames);
            unpagedParams.add(tagCount);
            
            content = jdbcTemplate.query(
                unpagedQuerySql,
                new RawQuestionRowMapper(),
                unpagedParams.toArray()
            );
        } else {
            // 分页查询
            String querySql = String.format(SQL_FIND_BY_IDS_AND_TAG_NAMES, idPlaceholders, tagPlaceholders);
            
            List<Object> params = new ArrayList<>();
            params.addAll(ids);
            params.addAll(tagNames);
            params.add(tagCount);
            params.add(pageable.getPageSize());
            params.add(pageable.getOffset());
            
            content = jdbcTemplate.query(
                querySql,
                new RawQuestionRowMapper(),
                params.toArray()
            );
        }
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 查找所有原始问?
     *
     * @return 所有原始问题列?
     */
    public List<RawQuestion> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new RawQuestionRowMapper());
    }

    /**
     * 分页查找所有原始问?
     *
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<RawQuestion> findAll(Pageable pageable) {
        // 查询总数
        Integer total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM RAW_QUESTIONS",
            Integer.class
        );
        
        // 查询数据
        List<RawQuestion> content;
        if (pageable.isUnpaged()) {
            // 如果是不分页查询，获取所有数据
            content = jdbcTemplate.query(
                "SELECT * FROM RAW_QUESTIONS ORDER BY ID DESC",
                new RawQuestionRowMapper()
            );
        } else {
            // 分页查询
            content = jdbcTemplate.query(
                "SELECT * FROM RAW_QUESTIONS ORDER BY ID DESC LIMIT ? OFFSET ?",
                new RawQuestionRowMapper(),
                pageable.getPageSize(),
                pageable.getOffset()
            );
        }
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 删除原始问题
     *
     * @param rawQuestion 原始问题对象
     */
    public void delete(RawQuestion rawQuestion) {
        jdbcTemplate.update(SQL_DELETE, rawQuestion.getId());
    }

    /**
     * 原始问题行映射器
     */
    private class RawQuestionRowMapper implements RowMapper<RawQuestion> {
        @Override
        public RawQuestion mapRow(ResultSet rs, int rowNum) throws SQLException {
            RawQuestion rawQuestion = new RawQuestion();
            
            // 设置ID和基本属?
            rawQuestion.setId(rs.getLong("ID"));
            rawQuestion.setTitle(rs.getString("TITLE"));
            rawQuestion.setContent(rs.getString("CONTENT"));
            rawQuestion.setSourceUrl(rs.getString("SOURCE_URL"));
            rawQuestion.setSourceSite(rs.getString("SOURCE_SITE"));
            
            // 设置标签和元数据
            rawQuestion.setTags(rs.getString("TAGS"));
            rawQuestion.setOtherMetadata(rs.getString("OTHER_METADATA"));
            
            // 设置时间字段
            Timestamp crawlTime = rs.getTimestamp("CRAWL_TIME");
            if (crawlTime != null) {
                rawQuestion.setCrawlTime(crawlTime.toLocalDateTime());
            }
            
            return rawQuestion;
        }
    }

    /**
     * 根据原始问题ID查找所有原始回答
     * 
     * @param rawQuestionId 原始问题ID
     * @return 原始回答列表
     */
    public List<RawAnswer> findRawAnswersByQuestionId(Long rawQuestionId) {
        return rawAnswerRepository.findByRawQuestionId(rawQuestionId);
    }
} 
