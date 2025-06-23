package com.example.demo.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.TagRecommendRequest;
import com.example.demo.dto.TagRecommendResponse;
import com.example.demo.entity.jdbc.Tag;
import com.example.demo.repository.jdbc.TagRepository;

@Service
public class TagRecommendationService {
    
    private final TagRepository tagRepository;
    
    // 模拟标签对应的文档集，用于计算IDF
    private final Map<String, List<String>> tagDocuments = new HashMap<>();
    
    // 注入TagRepository
    @Autowired
    public TagRecommendationService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
        // 为每个标签生成一些模拟文档，用于TF-IDF计算
        initializeTagDocuments();
    }
    
    private void initializeTagDocuments() {
        // 获取所有标签
        List<Tag> tags = tagRepository.findAll();
        
        // 这里简单模拟一些与标签相关的文档
        // 实际应用中应该从数据库加载
        for (Tag tag : tags) {
            List<String> docs = new ArrayList<>();
            // 为每个标签生成3-5个模拟文档
            int docCount = 3 + new Random().nextInt(3);
            for (int i = 0; i < docCount; i++) {
                docs.add(generateMockDocument(tag.getTagName()));
            }
            tagDocuments.put(tag.getTagName(), docs);
        }
    }
    
    private String generateMockDocument(String tag) {
        // 根据标签生成相关的模拟文档
        // 这里简单拼接一些常见词汇
        List<String> commonWords = Arrays.asList(
            "problem", "issue", "question", "solution", "example", 
            "code", "implementation", "error", "bug", "fix", 
            "how", "what", "why", "when", "which", "where", 
            "best", "practice", "performance", "optimization"
        );
        
        StringBuilder sb = new StringBuilder();
        // 添加标签相关词汇
        sb.append(tag).append(" ").append(tag).append(" ");
        
        // 添加一些与特定标签相关的词汇
        switch (tag) {
            case "java":
                sb.append("jvm class object interface inheritance polymorphism ");
                break;
            case "python":
                sb.append("dict list tuple pandas numpy scikit-learn ");
                break;
            case "database":
                sb.append("table schema query index transaction acid ");
                break;
            case "spring":
                sb.append("bean dependency-injection mvc boot security ");
                break;
            // 其他标签可以添加更多特定词汇
            default:
                // 为其他标签添加一些通用词汇
                sb.append("tutorial guide example reference documentation ");
        }
        
        // 添加一些通用词汇
        Random random = new Random();
        int wordCount = 10 + random.nextInt(20);
        for (int i = 0; i < wordCount; i++) {
            sb.append(commonWords.get(random.nextInt(commonWords.size()))).append(" ");
        }
        
        return sb.toString().toLowerCase();
    }
    
    public TagRecommendResponse recommendTags(TagRecommendRequest request) {
        // 检查请求对象
        if (request == null) {
            throw new IllegalArgumentException("标签推荐请求不能为空");
        }
        
        String text = request.getText();
        // 检查文本是否为空
        if (text == null || text.trim().isEmpty()) {
            // 如果文本为空，则返回空结果
            TagRecommendResponse emptyResponse = new TagRecommendResponse();
            emptyResponse.setTags(new ArrayList<>());
            emptyResponse.setConfidence(new ArrayList<>());
            return emptyResponse;
        }
        
        // 获取并预处理输入文本
        text = text.toLowerCase();
        
        // 1. 分词处理
        List<String> words = tokenize(text);
        
        // 2. 过滤常见停用词
        words = filterStopWords(words);
        
        // 3. 计算TF-IDF分数
        Map<String, Double> tagScores = calculateTagScores(words, request.getExistingTags());
        
        // 4. 排序并选择TOP N个标签
        List<Map.Entry<String, Double>> sortedScores = tagScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5) // 最多推荐5个标签
                .collect(Collectors.toList());
        
        // 5. 构建响应
        List<String> recommendedTags = new ArrayList<>();
        List<Double> confidenceScores = new ArrayList<>();
        
        for (Map.Entry<String, Double> entry : sortedScores) {
            // 设置阈值，只推荐得分较高的标签
            if (entry.getValue() > 0.1) {
                recommendedTags.add(entry.getKey());
                confidenceScores.add(entry.getValue());
            }
        }
        
        TagRecommendResponse response = new TagRecommendResponse();
        response.setTags(recommendedTags);
        response.setConfidence(confidenceScores);
        
        return response;
    }
    
    private List<String> tokenize(String text) {
        // 简单的分词处理，实际应用中可以使用更复杂的NLP库
        return Arrays.asList(text.split("\\s+|\\p{Punct}"));
    }
    
    private List<String> filterStopWords(List<String> words) {
        // 常见英文停用词
        Set<String> stopWords = new HashSet<>(Arrays.asList(
            "a", "an", "the", "and", "or", "but", "is", "are", "was", "were", 
            "be", "been", "being", "have", "has", "had", "do", "does", "did", 
            "can", "could", "will", "would", "shall", "should", "may", "might", 
            "must", "in", "on", "at", "to", "for", "with", "by", "about", "of", 
            "this", "that", "these", "those", "i", "you", "he", "she", "it", "we", "they"
        ));
        
        return words.stream()
                .filter(word -> !stopWords.contains(word) && word.length() > 1)
                .collect(Collectors.toList());
    }
    
    private Map<String, Double> calculateTagScores(List<String> words, List<String> existingTags) {
        // 获取所有标签
        List<Tag> allTagEntities = tagRepository.findAll();
        List<String> allTags = allTagEntities.stream()
                .map(Tag::getTagName)
                .collect(Collectors.toList());
        
        // 计算文档中每个词的频率 (TF)
        Map<String, Integer> wordFrequency = new HashMap<>();
        for (String word : words) {
            wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
        }
        
        // 文档总词数
        int totalWords = words.size();
        
        // 计算每个标签的得分
        Map<String, Double> tagScores = new HashMap<>();
        
        for (String tag : allTags) {
            // 跳过已有标签
            if (existingTags != null && existingTags.contains(tag)) {
                continue;
            }
            
            double score = 0.0;
            
            // 对标签名称本身进行评分
            String[] tagParts = tag.split("-");
            for (String tagPart : tagParts) {
                // 计算标签词在文档中的TF
                double tf = (double) wordFrequency.getOrDefault(tagPart, 0) / totalWords;
                
                // 计算标签词的IDF (使用模拟数据)
                double idf = calculateIDF(tagPart);
                
                // TF-IDF分数
                score += tf * idf;
            }
            
            // 对标签相关文档进行相似度评分
            List<String> tagDocs = tagDocuments.get(tag);
            if (tagDocs != null) {
                for (String doc : tagDocs) {
                    double similarity = calculateCosineSimilarity(words, tokenize(doc));
                    score += similarity * 0.5; // 相似度得分权重降低
                }
                
                // 取平均值
                if (!tagDocs.isEmpty()) {
                    score = score / tagDocs.size();
                }
            }
            
            tagScores.put(tag, score);
        }
        
        return tagScores;
    }
    
    private double calculateIDF(String term) {
        // 计算包含该词的文档数
        int docContainingTerm = 0;
        
        // 总文档数
        int totalDocs = 0;
        
        for (List<String> docs : tagDocuments.values()) {
            totalDocs += docs.size();
            
            for (String doc : docs) {
                if (doc.contains(term)) {
                    docContainingTerm++;
                    break; // 一个标签的文档集合中只计算一次
                }
            }
        }
        
        // 避免除零错误
        if (docContainingTerm == 0) {
            return 0.0;
        }
        
        // IDF计算公式: log(总文档数 / 包含词项的文档数)
        return Math.log((double) totalDocs / docContainingTerm);
    }
    
    private double calculateCosineSimilarity(List<String> words1, List<String> words2) {
        // 构建词向量
        Map<String, Integer> vector1 = new HashMap<>();
        Map<String, Integer> vector2 = new HashMap<>();
        
        // 填充向量1
        for (String word : words1) {
            vector1.put(word, vector1.getOrDefault(word, 0) + 1);
        }
        
        // 填充向量2
        for (String word : words2) {
            vector2.put(word, vector2.getOrDefault(word, 0) + 1);
        }
        
        // 计算点积
        double dotProduct = 0.0;
        for (Map.Entry<String, Integer> entry : vector1.entrySet()) {
            String word = entry.getKey();
            if (vector2.containsKey(word)) {
                dotProduct += entry.getValue() * vector2.get(word);
            }
        }
        
        // 计算向量模长
        double magnitude1 = 0.0;
        for (Integer count : vector1.values()) {
            magnitude1 += count * count;
        }
        magnitude1 = Math.sqrt(magnitude1);
        
        double magnitude2 = 0.0;
        for (Integer count : vector2.values()) {
            magnitude2 += count * count;
        }
        magnitude2 = Math.sqrt(magnitude2);
        
        // 避免除零错误
        if (magnitude1 == 0 || magnitude2 == 0) {
            return 0.0;
        }
        
        // 计算余弦相似度
        return dotProduct / (magnitude1 * magnitude2);
    }
} 