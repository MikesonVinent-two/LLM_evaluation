package com.example.demo.dto;

public class DatasetQuestionMappingDTO {
    
    private Long id;
    private Long datasetVersionId;
    private String datasetVersionName;
    private Long standardQuestionId;
    private String standardQuestionText;
    private Integer orderInDataset;
    
    public DatasetQuestionMappingDTO() {
    }
    
    public DatasetQuestionMappingDTO(Long id, Long datasetVersionId, String datasetVersionName, 
                                    Long standardQuestionId, String standardQuestionText, Integer orderInDataset) {
        this.id = id;
        this.datasetVersionId = datasetVersionId;
        this.datasetVersionName = datasetVersionName;
        this.standardQuestionId = standardQuestionId;
        this.standardQuestionText = standardQuestionText;
        this.orderInDataset = orderInDataset;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getDatasetVersionId() {
        return datasetVersionId;
    }
    
    public void setDatasetVersionId(Long datasetVersionId) {
        this.datasetVersionId = datasetVersionId;
    }
    
    public String getDatasetVersionName() {
        return datasetVersionName;
    }
    
    public void setDatasetVersionName(String datasetVersionName) {
        this.datasetVersionName = datasetVersionName;
    }
    
    public Long getStandardQuestionId() {
        return standardQuestionId;
    }
    
    public void setStandardQuestionId(Long standardQuestionId) {
        this.standardQuestionId = standardQuestionId;
    }
    
    public String getStandardQuestionText() {
        return standardQuestionText;
    }
    
    public void setStandardQuestionText(String standardQuestionText) {
        this.standardQuestionText = standardQuestionText;
    }
    
    public Integer getOrderInDataset() {
        return orderInDataset;
    }
    
    public void setOrderInDataset(Integer orderInDataset) {
        this.orderInDataset = orderInDataset;
    }
} 