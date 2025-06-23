package com.example.demo.exception;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.demo.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

/**
 * 全局异常处理器，用于统一处理各种异常并返回标准格式的错误响应
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理自定义的枚举反序列化异常
     */
    @ExceptionHandler(EnumDeserializationException.class)
    public ResponseEntity<ApiErrorResponse> handleEnumDeserializationException(EnumDeserializationException ex) {
        logger.error("枚举反序列化错误: " + ex.getMessage(), ex);
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                "400", 
                "请求中包含无效的枚举值",
                "INVALID_ENUM_VALUE");
        
        String allowedValuesStr = Arrays.stream(ex.getAllowedValues())
                .collect(Collectors.joining(", "));
        
        errorResponse.addValidationError(
                ex.getFieldName(),
                String.format("'%s' 不是有效的值。允许的值: [%s]", ex.getInvalidValue(), allowedValuesStr),
                ex.getInvalidValue());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 处理Jackson的反序列化异常，包括无效的枚举值
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        logger.error("请求体解析错误: " + ex.getMessage(), ex);
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                "400", 
                "请求格式错误，无法解析",
                "INVALID_REQUEST_FORMAT");
        
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) cause;
            if (ife.getTargetType().isEnum()) {
                // 处理枚举类型错误
                String fieldName = getFieldName(ife);
                Object invalidValue = ife.getValue();
                Class<?> enumType = ife.getTargetType();
                
                Object[] enumValues = enumType.getEnumConstants();
                String allowedValuesStr = Arrays.stream(enumValues)
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));
                
                errorResponse.setMessage("请求中包含无效的枚举值");
                errorResponse.setError("INVALID_ENUM_VALUE");
                errorResponse.addValidationError(
                        fieldName,
                        String.format("'%s' 不是有效的值。允许的值: [%s]", invalidValue, allowedValuesStr),
                        invalidValue);
            } else {
                // 处理其他类型转换错误
                String fieldName = getFieldName(ife);
                Object invalidValue = ife.getValue();
                String targetTypeName = ife.getTargetType().getSimpleName();
                
                errorResponse.addValidationError(
                        fieldName,
                        String.format("'%s' 不能转换为类型 '%s'", invalidValue, targetTypeName),
                        invalidValue);
            }
        } else {
            // 其他解析错误
            errorResponse.setDebugMessage(ex.getMessage());
        }
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 处理请求参数验证错误
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        logger.error("参数验证错误: " + ex.getMessage(), ex);
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                "400", 
                "请求参数验证失败",
                "VALIDATION_FAILED");
        
        BindingResult result = ex.getBindingResult();
        for (FieldError fieldError : result.getFieldErrors()) {
            errorResponse.addValidationError(
                    fieldError.getField(),
                    fieldError.getDefaultMessage(),
                    fieldError.getRejectedValue());
        }
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 处理参数类型不匹配错误
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        logger.error("参数类型不匹配: " + ex.getMessage(), ex);
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                "400", 
                "参数类型不匹配",
                "TYPE_MISMATCH");
        
        errorResponse.addValidationError(
                ex.getName(),
                String.format("'%s' 不能转换为类型 '%s'", ex.getValue(), ex.getRequiredType().getSimpleName()),
                ex.getValue());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception ex) {
        logger.error("发生未处理的异常: " + ex.getMessage(), ex);
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                "500", 
                "服务器内部错误",
                "INTERNAL_SERVER_ERROR");
        
        errorResponse.setDebugMessage(ex.getMessage());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 从JsonMappingException中提取字段名称
     */
    private String getFieldName(JsonMappingException ex) {
        if (ex.getPath() != null && !ex.getPath().isEmpty()) {
            return ex.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(name -> name != null)
                    .collect(Collectors.joining("."));
        }
        return "未知字段";
    }
} 