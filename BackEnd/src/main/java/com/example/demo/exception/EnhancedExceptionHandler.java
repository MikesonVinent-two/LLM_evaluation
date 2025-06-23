package com.example.demo.exception;

import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.example.demo.config.ExceptionHandlerConfig;
import com.example.demo.dto.EnhancedApiErrorResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 增强版全局异常处理器，提供详细的错误信息
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class EnhancedExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(EnhancedExceptionHandler.class);
    
    @Autowired
    private ExceptionHandlerConfig config;

    /**
     * 处理自定义的资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    protected ResponseEntity<EnhancedApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        
        EnhancedApiErrorResponse errorResponse = createErrorResponse(
                HttpStatus.NOT_FOUND,
                "资源未找到",
                "RESOURCE_NOT_FOUND",
                ex,
                request
        );
        
        errorResponse.setExceptionMessage(ex.getMessage());
        errorResponse.addSolutionHint("请确认请求的资源ID是否正确，资源可能已被删除或尚未创建");
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    /**
     * 处理参数验证失败异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<EnhancedApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        EnhancedApiErrorResponse errorResponse = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "请求参数验证失败",
                "VALIDATION_FAILED",
                ex,
                request
        );
        
        BindingResult result = ex.getBindingResult();
        for (FieldError fieldError : result.getFieldErrors()) {
            errorResponse.addValidationError(
                    fieldError.getField(),
                    fieldError.getDefaultMessage(),
                    fieldError.getRejectedValue()
            );
            
            // 添加约束信息
            if (errorResponse.getValidationErrors() != null && !errorResponse.getValidationErrors().isEmpty()) {
                EnhancedApiErrorResponse.ValidationError validationError = 
                    errorResponse.getValidationErrors().get(errorResponse.getValidationErrors().size() - 1);
                
                validationError.setCode(fieldError.getCode());
                if (fieldError.getArguments() != null && fieldError.getArguments().length > 1) {
                    for (int i = 1; i < fieldError.getArguments().length; i++) {
                        if (fieldError.getArguments()[i] != null) {
                            validationError.addConstraint(
                                "constraint_" + i, 
                                fieldError.getArguments()[i].toString()
                            );
                        }
                    }
                }
            }
        }
        
        errorResponse.addSolutionHint("请根据字段验证错误信息修正请求参数");
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 处理请求体解析异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<EnhancedApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {
        
        EnhancedApiErrorResponse errorResponse = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "请求体格式错误，无法解析",
                "MALFORMED_JSON_REQUEST",
                ex,
                request
        );
        
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException) {
            handleInvalidFormatException((InvalidFormatException) cause, errorResponse);
        } else if (cause instanceof MismatchedInputException) {
            errorResponse.setMessage("JSON格式不匹配，请检查请求体格式");
            errorResponse.setError("JSON_FORMAT_MISMATCH");
        } else {
            errorResponse.setDebugMessage(ex.getMessage());
        }
        
        errorResponse.addSolutionHint("请检查JSON格式是否正确，字段类型是否匹配");
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 处理枚举反序列化异常
     */
    @ExceptionHandler(EnumDeserializationException.class)
    protected ResponseEntity<EnhancedApiErrorResponse> handleEnumDeserializationException(
            EnumDeserializationException ex, WebRequest request) {
        
        EnhancedApiErrorResponse errorResponse = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "枚举值错误",
                "INVALID_ENUM_VALUE",
                ex,
                request
        );
        
        String allowedValuesStr = Arrays.stream(ex.getAllowedValues())
                .collect(Collectors.joining(", "));
        
        errorResponse.addValidationError(
                ex.getFieldName(),
                String.format("'%s' 不是有效的值。允许的值: [%s]", ex.getInvalidValue(), allowedValuesStr),
                ex.getInvalidValue()
        );
        
        Map<String, String> data = new HashMap<>();
        data.put("fieldName", ex.getFieldName());
        data.put("invalidValue", String.valueOf(ex.getInvalidValue()));
        data.put("allowedValues", allowedValuesStr);
        errorResponse.setData(data);
        
        errorResponse.addSolutionHint(String.format("请使用以下枚举值之一: %s", allowedValuesStr));
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<EnhancedApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        
        EnhancedApiErrorResponse errorResponse = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "参数类型不匹配",
                "TYPE_MISMATCH",
                ex,
                request
        );
        
        String typeName = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "未知类型";
        errorResponse.addValidationError(
                ex.getName(),
                String.format("参数 '%s' 的值 '%s' 不能转换为类型 '%s'", ex.getName(), ex.getValue(), typeName),
                ex.getValue()
        );
        
        errorResponse.addSolutionHint(String.format("请提供正确类型的参数值，类型应为: %s", typeName));
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<EnhancedApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {
        
        EnhancedApiErrorResponse errorResponse = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "参数验证失败",
                "CONSTRAINT_VIOLATION",
                ex,
                request
        );
        
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            String field = propertyPath.contains(".") ? 
                    propertyPath.substring(propertyPath.lastIndexOf('.') + 1) : propertyPath;
            
            errorResponse.addValidationError(
                    field,
                    violation.getMessage(),
                    violation.getInvalidValue()
            );
            
            // 添加约束信息
            if (errorResponse.getValidationErrors() != null && !errorResponse.getValidationErrors().isEmpty()) {
                EnhancedApiErrorResponse.ValidationError validationError = 
                    errorResponse.getValidationErrors().get(errorResponse.getValidationErrors().size() - 1);
                
                violation.getConstraintDescriptor().getAttributes().forEach((key, value) -> {
                    if (!"message".equals(key) && !"groups".equals(key) && !"payload".equals(key)) {
                        validationError.addConstraint(key, value.toString());
                    }
                });
            }
        }
        
        errorResponse.addSolutionHint("请根据验证错误信息修正请求参数");
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 处理数据库完整性异常
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<EnhancedApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, WebRequest request) {
        
        EnhancedApiErrorResponse errorResponse = createErrorResponse(
                HttpStatus.CONFLICT,
                "数据操作违反了数据库完整性约束",
                "DATA_INTEGRITY_VIOLATION",
                ex,
                request
        );
        
        if (ex.getCause() instanceof SQLException) {
            SQLException sqlEx = (SQLException) ex.getCause();
            errorResponse.setExceptionMessage(sqlEx.getMessage());
            
            // 尝试分析SQL异常
            if (sqlEx.getMessage().contains("foreign key")) {
                errorResponse.setMessage("操作失败：外键约束冲突");
                errorResponse.setError("FOREIGN_KEY_VIOLATION");
                errorResponse.addSolutionHint("请确保引用的外键数据存在");
            } else if (sqlEx.getMessage().contains("unique") || sqlEx.getMessage().contains("duplicate")) {
                errorResponse.setMessage("操作失败：唯一性约束冲突");
                errorResponse.setError("UNIQUE_CONSTRAINT_VIOLATION");
                errorResponse.addSolutionHint("请使用不同的值，当前值已存在");
            }
        }
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
    
    /**
     * 处理数据访问异常
     */
    @ExceptionHandler(DataAccessException.class)
    protected ResponseEntity<EnhancedApiErrorResponse> handleDataAccessException(
            DataAccessException ex, WebRequest request) {
        
        EnhancedApiErrorResponse errorResponse = createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "数据库访问错误",
                "DATABASE_ERROR",
                ex,
                request
        );
        
        errorResponse.addSolutionHint("请联系系统管理员报告此问题");
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 处理HTTP客户端错误
     */
    @ExceptionHandler(HttpClientErrorException.class)
    protected ResponseEntity<EnhancedApiErrorResponse> handleHttpClientErrorException(
            HttpClientErrorException ex, WebRequest request) {
        
        EnhancedApiErrorResponse errorResponse = createErrorResponse(
                HttpStatus.valueOf(ex.getStatusCode().value()),
                "外部API调用客户端错误: " + ex.getStatusCode(),
                "EXTERNAL_API_CLIENT_ERROR",
                ex,
                request
        );
        
        errorResponse.setExceptionMessage(ex.getResponseBodyAsString());
        
        Map<String, Object> data = new HashMap<>();
        data.put("statusCode", ex.getStatusCode().value());
        data.put("statusText", ex.getStatusText());
        data.put("responseBody", ex.getResponseBodyAsString());
        errorResponse.setData(data);
        
        errorResponse.addSolutionHint("请检查对外部API的请求参数是否正确");
        
        return new ResponseEntity<>(errorResponse, HttpStatus.valueOf(ex.getStatusCode().value()));
    }
    
    /**
     * 处理HTTP服务器错误
     */
    @ExceptionHandler(HttpServerErrorException.class)
    protected ResponseEntity<EnhancedApiErrorResponse> handleHttpServerErrorException(
            HttpServerErrorException ex, WebRequest request) {
        
        EnhancedApiErrorResponse errorResponse = createErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "外部API服务器错误: " + ex.getStatusCode(),
                "EXTERNAL_API_SERVER_ERROR",
                ex,
                request
        );
        
        errorResponse.setExceptionMessage(ex.getResponseBodyAsString());
        
        Map<String, Object> data = new HashMap<>();
        data.put("statusCode", ex.getStatusCode().value());
        data.put("statusText", ex.getStatusText());
        data.put("responseBody", ex.getResponseBodyAsString());
        errorResponse.setData(data);
        
        errorResponse.addSolutionHint("外部服务暂时不可用，请稍后重试");
        
        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    /**
     * 处理资源访问异常
     */
    @ExceptionHandler(ResourceAccessException.class)
    protected ResponseEntity<EnhancedApiErrorResponse> handleResourceAccessException(
            ResourceAccessException ex, WebRequest request) {
        
        EnhancedApiErrorResponse errorResponse = createErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "无法连接到外部API",
                "EXTERNAL_API_CONNECTION_ERROR",
                ex,
                request
        );
        
        errorResponse.setExceptionMessage(ex.getMessage());
        errorResponse.addSolutionHint("外部服务连接失败，请检查网络连接或稍后重试");
        
        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    protected ResponseEntity<EnhancedApiErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex, WebRequest request) {
        
        EnhancedApiErrorResponse errorResponse = createErrorResponse(
                HttpStatus.NOT_FOUND,
                String.format("找不到接口: %s %s", ex.getHttpMethod(), ex.getRequestURL()),
                "API_ENDPOINT_NOT_FOUND",
                ex,
                request
        );
        
        errorResponse.addSolutionHint("请检查API路径是否正确");
        errorResponse.addDocumentationLink("/api/swagger-ui.html");
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    /**
     * 处理HTTP请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<EnhancedApiErrorResponse> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {
        
        StringBuilder builder = new StringBuilder();
        builder.append(ex.getMethod());
        builder.append(" 方法不支持此接口。");
        if (ex.getSupportedHttpMethods() != null) {
            builder.append("支持的方法包括: ");
            ex.getSupportedHttpMethods().forEach(t -> builder.append(t).append(" "));
        }
        
        EnhancedApiErrorResponse errorResponse = createErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                builder.toString(),
                "METHOD_NOT_ALLOWED",
                ex,
                request
        );
        
        errorResponse.addSolutionHint(String.format("请使用以下HTTP方法之一: %s", 
                ex.getSupportedHttpMethods() != null ? ex.getSupportedHttpMethods() : ""));
        
        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }
    
    /**
     * 处理其他所有异常
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<EnhancedApiErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        EnhancedApiErrorResponse errorResponse = createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "服务器内部错误",
                "INTERNAL_SERVER_ERROR",
                ex,
                request
        );
        
        errorResponse.setExceptionMessage(ex.getMessage());
        errorResponse.addSolutionHint("请联系系统管理员并提供追踪ID");
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 创建错误响应对象
     */
    private EnhancedApiErrorResponse createErrorResponse(
            HttpStatus status, String message, String error, 
            Exception ex, WebRequest request) {
        
        EnhancedApiErrorResponse errorResponse = new EnhancedApiErrorResponse(
                String.valueOf(status.value()),
                message,
                error
        );
        
        // 获取请求路径
        String path = "";
        if (request instanceof ServletWebRequest) {
            ServletWebRequest servletRequest = (ServletWebRequest) request;
            path = servletRequest.getRequest().getRequestURI();
        }
        errorResponse.setPath(path);
        
        // 设置异常信息
        if (config.isDetailedErrorMessages()) {
            errorResponse.setExceptionName(ex.getClass().getName());
            errorResponse.setExceptionMessage(ex.getMessage());
        }
        
        // 生成追踪ID
        if (config.isTraceEnabled()) {
            String traceId = UUID.randomUUID().toString();
            errorResponse.setTraceId(traceId);
            
            // 记录详细日志
            log.error("异常处理 - TraceID: {} - 路径: {} - 状态码: {} - 错误: {}", 
                    traceId, path, status.value(), error, ex);
        } else {
            log.error("异常处理 - 路径: {} - 状态码: {} - 错误: {}", 
                    path, status.value(), error, ex);
        }
        
        // 仅在开发环境添加调试信息
        if (config.isDevelopmentMode()) {
            errorResponse.setDebugMessage(ex.getMessage());
            
            // 添加堆栈跟踪
            if (config.isIncludeStacktraceInResponse()) {
                StringBuilder stackTrace = new StringBuilder();
                for (StackTraceElement element : ex.getStackTrace()) {
                    if (element.getClassName().startsWith("com.example.demo")) {
                        stackTrace.append(element.toString()).append("\n");
                    }
                }
                errorResponse.setStackTrace(stackTrace.toString());
            }
        }
        
        return errorResponse;
    }
    
    /**
     * 处理无效格式异常
     */
    private void handleInvalidFormatException(InvalidFormatException ex, EnhancedApiErrorResponse errorResponse) {
        if (ex.getTargetType().isEnum()) {
            // 处理枚举类型错误
            String fieldName = getFieldName(ex);
            Object invalidValue = ex.getValue();
            Class<?> enumType = ex.getTargetType();
            
            Object[] enumValues = enumType.getEnumConstants();
            String allowedValuesStr = Arrays.stream(enumValues)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            
            errorResponse.setMessage("请求中包含无效的枚举值");
            errorResponse.setError("INVALID_ENUM_VALUE");
            errorResponse.addValidationError(
                    fieldName,
                    String.format("'%s' 不是有效的值。允许的值: [%s]", invalidValue, allowedValuesStr),
                    invalidValue
            );
            
            // 添加更多帮助信息
            Map<String, String> data = new HashMap<>();
            data.put("enumType", enumType.getSimpleName());
            data.put("allowedValues", allowedValuesStr);
            errorResponse.setData(data);
        } else {
            // 处理其他类型转换错误
            String fieldName = getFieldName(ex);
            Object invalidValue = ex.getValue();
            String targetTypeName = ex.getTargetType().getSimpleName();
            
            errorResponse.addValidationError(
                    fieldName,
                    String.format("'%s' 不能转换为类型 '%s'", invalidValue, targetTypeName),
                    invalidValue
            );
        }
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