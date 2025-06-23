package com.example.demo.service;

import com.example.demo.dto.RawAnswerDTO;
import com.example.demo.entity.jdbc.RawAnswer;

public interface RawAnswerService {
    RawAnswer createRawAnswer(RawAnswerDTO rawAnswerDTO);
} 