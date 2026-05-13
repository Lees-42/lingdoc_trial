package com.ruoyi.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置
 *
 * 用途：AI 调用等耗时操作使用独立线程池，不占用 Tomcat 线程
 *
 * @author lingdoc
 */
@Configuration
@EnableAsync
public class AsyncConfig
{
    /**
     * AI 任务专用线程池
     *
     * 参数设计：
     * - corePoolSize=8: 基础并发数
     * - maxPoolSize=32: 峰值并发数
     * - queueCapacity=100: 排队缓冲区
     * - keepAliveSeconds=60: 空闲线程存活时间
     * - rejectionPolicy=CallerRunsPolicy: 队列满时主线程执行（降级保护）
     */
    @Bean("aiTaskExecutor")
    public Executor aiTaskExecutor()
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-task-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
