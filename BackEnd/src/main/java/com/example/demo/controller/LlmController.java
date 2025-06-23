package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.LlmRequestDTO;
import com.example.demo.dto.LlmResponseDTO;
import com.example.demo.dto.ModelInfoDTO;
import com.example.demo.dto.ModelRequestDTO;
import com.example.demo.service.LlmService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/llm")
@CrossOrigin(origins = "*")
public class LlmController {

    private final LlmService llmService;

    @Autowired
    public LlmController(LlmService llmService) {
        this.llmService = llmService;
    }

    /**
     * 发送请求到LLM API并获取回答
     * 
     * @param request LLM请求参数
     * @return LLM响应结果
     */
    @PostMapping("/chat")
    public ResponseEntity<LlmResponseDTO> chat(@Valid @RequestBody LlmRequestDTO request) {
        LlmResponseDTO response = llmService.sendRequest(request);
        
        if (response.getSuccess()) {
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 获取可用的模型列表
     * 
     * @param request 包含API URL和密钥的请求参数
     * @return 模型列表
     */
    @PostMapping("/models")
    public ResponseEntity<List<ModelInfoDTO>> getModels(@Valid @RequestBody ModelRequestDTO request) {
        try {
            List<ModelInfoDTO> models = llmService.getAvailableModels(request.getApiUrl(), request.getApiKey());
            return new ResponseEntity<>(models, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 