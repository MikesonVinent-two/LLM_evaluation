package com.example.demo.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * 元数据处理工具类
 * 用于处理JSON格式的元数据，支持JSON字符串和JSON对象两种格式
 */
public class MetadataUtils {

    private static final Logger logger = LoggerFactory.getLogger(MetadataUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理元数据，支持两种格式：
     * 1. JSON字符串格式："{\"source\":\"网站A\",\"category\":\"医疗\"}"
     * 2. JSON对象格式：{"source":"网站A","category":"医疗","tags":["内科","外科"]}
     *
     * @param metadata 元数据
     * @return 标准化后的JSON字符串
     */
    public static String normalizeMetadata(Object metadata) {
        if (metadata == null) {
            return null;
        }

        try {
            // 如果已经是字符串
            if (metadata instanceof String) {
                String metadataStr = (String) metadata;
                if (!StringUtils.hasText(metadataStr)) {
                    return null;
                }

                // 判断是否已经是JSON字符串
                if (isValidJsonString(metadataStr)) {
                    return metadataStr;
                } else {
                    // 尝试作为普通字符串处理
                    return objectMapper.writeValueAsString(metadataStr);
                }
            } else {
                // 如果是JSON对象（Map, 数组等）
                return objectMapper.writeValueAsString(metadata);
            }
        } catch (JsonProcessingException e) {
            logger.error("处理元数据时出错", e);
            // 转换失败时返回原始字符串
            return metadata.toString();
        }
    }

    /**
     * 检查字符串是否是有效的JSON
     *
     * @param jsonString 待检查的字符串
     * @return 是否是有效的JSON
     */
    private static boolean isValidJsonString(String jsonString) {
        try {
            objectMapper.readTree(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
} 