package com.example.demo.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            factory.addConnectorCustomizers(connector -> {
                // 设置URI编码为UTF-8
                connector.setURIEncoding("UTF-8");
                
                // 获取协议处理器
                Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
                
                // 设置更宽松的字符处理
                connector.setProperty("relaxedPathChars", "[]|");
                connector.setProperty("relaxedQueryChars", "[]|{}^`\"<>");
                
                // 允许反斜杠
                connector.setProperty("allowBackslash", "true");
                
                // 设置最大HTTP头大小
                protocol.setMaxHttpHeaderSize(65536);
                
                // 设置连接超时
                protocol.setConnectionTimeout(120000);
            });
        };
    }
} 