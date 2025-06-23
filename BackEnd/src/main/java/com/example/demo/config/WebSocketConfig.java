package com.example.demo.config;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * WebSocket配置类
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);
    
    @Value("${spring.websocket.base-path:/ws}")
    private String websocketPath;
    
    /**
     * 配置消息代理
     * 
     * @param registry 消息代理注册表
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单的基于内存的消息代理
        registry.enableSimpleBroker("/topic", "/queue");
        
        // 客户端发送消息的目的地前缀
        registry.setApplicationDestinationPrefixes("/app");
        
        // 用户专有的目的地前缀
        registry.setUserDestinationPrefix("/user");
    }
    
    /**
     * 注册STOMP端点
     * 
     * @param registry STOMP端点注册表
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 自定义握手处理器
        CustomHandshakeHandler handshakeHandler = new CustomHandshakeHandler();
        
        logger.info("注册WebSocket端点: {}", websocketPath);
        
        // 注册STOMP端点，支持SockJS
        registry.addEndpoint(websocketPath)
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(handshakeHandler)
                .addInterceptors(new DetailedLoggingHandshakeInterceptor())
                .withSockJS()
                .setSessionCookieNeeded(false);
        
        // 添加原生WebSocket端点
        registry.addEndpoint(websocketPath)
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(handshakeHandler)
                .addInterceptors(new DetailedLoggingHandshakeInterceptor());
    }
    
    /**
     * 配置WebSocket传输
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(8192)
                   .setSendBufferSizeLimit(8192)
                   .setSendTimeLimit(10000);
    }
    
    /**
     * 配置WebSocket容器
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);
        container.setMaxSessionIdleTimeout(60000L); // 60秒
        return container;
    }
    
    /**
     * 提供WebSocketHandler装饰工厂，用于添加连接和断开连接的日志
     */
    @Bean
    public WebSocketHandlerDecoratorFactory webSocketHandlerDecoratorFactory() {
        return handler -> new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                logger.debug("WebSocket连接已建立: {}", session.getId());
                super.afterConnectionEstablished(session);
            }
            
            @Override
            public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus closeStatus) throws Exception {
                logger.debug("WebSocket连接已关闭: {}, 状态: {}", session.getId(), closeStatus);
                super.afterConnectionClosed(session, closeStatus);
            }
        };
    }
    
    /**
     * 详细的日志握手拦截器
     */
    private static class DetailedLoggingHandshakeInterceptor implements HandshakeInterceptor {
        private static final Logger logger = LoggerFactory.getLogger(DetailedLoggingHandshakeInterceptor.class);
        
        // 添加一个ThreadLocal来存储每个请求的属性
        private final ThreadLocal<Map<String, Object>> attributesHolder = new ThreadLocal<>();
        
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                      WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
            logger.debug("WebSocket握手开始 - 请求URI: {}", request.getURI());
            logger.debug("WebSocket握手开始 - 请求头: {}", request.getHeaders());
            logger.debug("WebSocket握手开始 - 远程地址: {}", request.getRemoteAddress());
            
            // 添加一些属性，以便在握手完成后使用
            attributes.put("ip", request.getRemoteAddress());
            attributes.put("startTime", System.currentTimeMillis());
            
            // 存储属性以便在afterHandshake中使用
            attributesHolder.set(new HashMap<>(attributes));
            
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                  WebSocketHandler wsHandler, Exception exception) {
            Map<String, Object> attributes = attributesHolder.get();
            Long startTime = attributes != null ? (Long) attributes.get("startTime") : System.currentTimeMillis();
            long duration = System.currentTimeMillis() - startTime;
            
            if (exception != null) {
                logger.error("WebSocket握手失败 - 耗时: {}ms - 异常: {}", duration, exception.getMessage(), exception);
                response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            } else {
                logger.debug("WebSocket握手成功 - 耗时: {}ms - 请求URI: {}", duration, request.getURI());
                logger.debug("WebSocket握手成功 - 响应头: {}", response.getHeaders());
            }
            
            // 清理ThreadLocal
            attributesHolder.remove();
        }
    }
    
    /**
     * 自定义握手处理器，用于更好地诊断问题
     */
    private static class CustomHandshakeHandler extends DefaultHandshakeHandler {
        private static final Logger logger = LoggerFactory.getLogger(CustomHandshakeHandler.class);
        
        @Override
        protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
            // 创建匿名用户
            logger.debug("确定WebSocket用户 - 请求URI: {}", request.getURI());
            return () -> "anonymous";
        }
    }
}