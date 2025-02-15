package com.sunny.maven.rpc.demo.spring.annotation.provider.config;

import com.sunny.maven.rpc.provider.spring.RpcSpringServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author SUNNY
 * @description: 基于注解的配置类
 * @create: 2023-02-14 17:53
 */
@Configuration
@ComponentScan(value = {"com.sunny.maven.rpc.demo"})
@PropertySource(value = {"classpath:rpc.properties"})
public class SpringAnnotationProviderConfig {
    @Value("${server.address}")
    private String serverAddress;
    @Value("${server.registry.address}")
    private String serverRegistryAddress;
    @Value("${reflect.type}")
    private String reflectType;
    @Value("${registry.address}")
    private String registryAddress;
    @Value("${registry.type}")
    private String registryType;
    @Value("${registry.loadbalance.type}")
    private String registryLoadBalanceType;
    @Value("${server.heartbeatInterval}")
    private int heartbeatInterval;
    @Value("${server.scanNotActiveChannelInterval}")
    private int scanNotActiveChannelInterval;
    @Value("${server.enableResultCache}")
    private boolean enableResultCache;
    @Value("${server.resultCacheExpire}")
    private int resultCacheExpire;
    @Value("${server.corePoolSize}")
    private int corePoolSize;
    @Value("${server.maximumPoolSize}")
    private int maximumPoolSize;
    @Value("${server.flowType}")
    private String flowType;
    @Value("${server.maxConnections}")
    private int maxConnections;
    @Value("${server.disuseStrategyType}")
    private String disuseStrategyType;
    @Value("${server.enableBuffer}")
    private boolean enableBuffer;
    @Value("${server.bufferSize}")
    private int bufferSize;
    @Value("${server.enableRateLimiter}")
    private boolean enableRateLimiter;
    @Value("${server.rateLimiterType}")
    private String rateLimiterType;
    @Value("${server.permits}")
    private int permits;
    @Value("${server.milliSeconds}")
    private int milliSeconds;
    @Value("${server.rateLimiterFailStrategy}")
    private String rateLimiterFailStrategy;
    @Value("${server.enableFusing}")
    private boolean enableFusing;
    @Value("${server.fusingType}")
    private String fusingType;
    @Value("${server.totalFailure}")
    private double totalFailure;
    @Value("${server.fusingMilliSeconds}")
    private int fusingMilliSeconds;
    @Value("${server.exceptionPostProcessorType}")
    private String exceptionPostProcessorType;

    @Bean
    public RpcSpringServer rpcSpringServer() {
        return new RpcSpringServer(serverAddress, serverRegistryAddress, reflectType, registryAddress, registryType,
                registryLoadBalanceType, heartbeatInterval, scanNotActiveChannelInterval, enableResultCache,
                resultCacheExpire, corePoolSize, maximumPoolSize, flowType, maxConnections, disuseStrategyType,
                enableBuffer, bufferSize, enableRateLimiter, rateLimiterType, permits, milliSeconds,
                rateLimiterFailStrategy, enableFusing, fusingType, totalFailure, fusingMilliSeconds,
                exceptionPostProcessorType);
    }
}
