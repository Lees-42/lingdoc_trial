package com.ruoyi.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

/**
 * Jackson 配置类
 * 提供全局 ObjectMapper Bean
 */
@Configuration
public class JacksonConfig
{
    @Bean
    @Primary
    public ObjectMapper objectMapper()
    {
        ObjectMapper mapper = new ObjectMapper();
        // 忽略未知字段，防止 JSON 字段不匹配时报错
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}
