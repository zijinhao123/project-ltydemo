package com.sunny.maven.rpc.consumer;

import com.sunny.maven.rpc.common.exception.RegistryException;
import com.sunny.maven.rpc.consumer.common.RpcConsumer;
import com.sunny.maven.rpc.proxy.api.ProxyFactory;
import com.sunny.maven.rpc.proxy.api.async.IAsyncObjectProxy;
import com.sunny.maven.rpc.proxy.api.config.ProxyConfig;
import com.sunny.maven.rpc.proxy.api.object.ObjectProxy;
import com.sunny.maven.rpc.registry.api.RegistryService;
import com.sunny.maven.rpc.registry.api.config.RegistryConfig;
import com.sunny.maven.rpc.spi.loader.ExtensionLoader;
import com.sunny.maven.rpc.threadpool.ConcurrentThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @author SUNNY
 * @description: 服务消费客户端
 * @create: 2023-01-01 22:34
 */
@Slf4j
public class RpcClient {
    /**
     * 服务版本号
     */
    private String serviceVersion;
    /**
     * 服务分组
     */
    private String serviceGroup;
    /**
     * 序列化类型
     */
    private String serializationType;
    /**
     * 超时时间，默认15s
     */
    private long timeout;
    /**
     * 代理
     */
    private String proxy;
    /**
     * 是否异步调用
     */
    private boolean async;
    /**
     * 是否单向调用
     */
    private boolean oneWay;
    /**
     * 注册服务
     */
    private RegistryService registryService;
    /**
     * 心跳间隔时间，默认30秒
     */
    private int heartbeatInterval;
    /**
     * 扫描并移除空闲连接时间，默认60秒
     */
    private int scanNotActiveChannelInterval;
    /**
     * 重试间隔时间
     */
    private int retryInterval = 1000;
    /**
     * 重试次数
     */
    private int retryTimes = 3;
    /**
     * 是否开启结果缓存
     */
    private boolean enableResultCache;
    /**
     * 缓存结果的时长，单位是毫秒
     */
    private int resultCacheExpire;
    /**
     * 是否开启直连服务
     */
    private boolean enableDirectServer;
    /**
     * 直连服务的地址
     */
    private String directServerUrl;
    /**
     * 是否开启延迟连接
     */
    private boolean enableDelayConnection;
    /**
     * 并发处理线程池
     */
    private ConcurrentThreadPool concurrentThreadPool;
    /**
     * 流控分析类型
     */
    private String flowType;
    /**
     * 是否开启数据缓冲
     */
    private boolean enableBuffer;
    /**
     * 缓冲区大小
     */
    private int bufferSize;
    /**
     * 反射类型
     */
    private String reflectType;
    /**
     * 容错class名称
     */
    private String fallbackClassName;
    /**
     * 容错class
     */
    private Class<?> fallbackClass;
    /**
     * 是否开启限流
     */
    private boolean enableRateLimiter;
    /**
     * 限流类型
     */
    private String rateLimiterType;
    /**
     * 在milliSeconds毫秒内最多能够通过的请求个数
     */
    private int permits;
    /**
     * 毫秒数
     */
    private int milliSeconds;
    /**
     * 当限流失败时的处理策略
     */
    private String rateLimiterFailStrategy;
    /**
     * 是否开启熔断策略
     */
    private boolean enableFusing;
    /**
     * 熔断规则标识
     */
    private String fusingType;
    /**
     * 在fusingMilliSeconds毫秒内触发熔断操作的上限值
     */
    private double totalFailure;
    /**
     * 熔断的毫秒时长
     */
    private int fusingMilliSeconds;
    /**
     * 异常处理后置处理器类型
     */
    private String exceptionPostProcessorType;

    public RpcClient(String registryAddress, String registryType, String serviceVersion, String serviceGroup,
                     String serializationType, String registryLoadBalanceType, long timeout, String proxy,
                     boolean async, boolean oneWay, int heartbeatInterval, int scanNotActiveChannelInterval,
                     int retryInterval, int retryTimes, boolean enableResultCache, int resultCacheExpire,
                     boolean enableDirectServer, String directServerUrl, boolean enableDelayConnection,
                     int corePoolSize, int maximumPoolSize, String flowType, boolean enableBuffer, int bufferSize,
                     String reflectType, String fallbackClassName, boolean enableRateLimiter, String rateLimiterType,
                     int permits, int milliSeconds, String rateLimiterFailStrategy, boolean enableFusing,
                     String fusingType, double totalFailure, int fusingMilliSeconds,
                     String exceptionPostProcessorType) {
        this.serviceVersion = serviceVersion;
        this.serviceGroup = serviceGroup;
        this.serializationType = serializationType;
        this.timeout = timeout;
        this.proxy = proxy;
        this.async = async;
        this.oneWay = oneWay;
        this.heartbeatInterval = heartbeatInterval;
        this.scanNotActiveChannelInterval = scanNotActiveChannelInterval;
        this.retryInterval = retryInterval;
        this.retryTimes = retryTimes;
        this.enableResultCache = enableResultCache;
        this.resultCacheExpire = resultCacheExpire;
        this.enableDirectServer = enableDirectServer;
        this.directServerUrl = directServerUrl;
        this.enableDelayConnection = enableDelayConnection;
        this.flowType = flowType;
        this.enableBuffer = enableBuffer;
        this.bufferSize = bufferSize;
        this.reflectType = reflectType;
        this.fallbackClassName = fallbackClassName;
        this.enableRateLimiter = enableRateLimiter;
        this.rateLimiterType = rateLimiterType;
        this.permits = permits;
        this.milliSeconds = milliSeconds;
        this.rateLimiterFailStrategy = rateLimiterFailStrategy;
        this.enableFusing = enableFusing;
        this.fusingType = fusingType;
        this.totalFailure = totalFailure;
        this.fusingMilliSeconds = fusingMilliSeconds;
        this.exceptionPostProcessorType = exceptionPostProcessorType;
        this.registryService = this.getRegistryService(registryAddress, registryType, registryLoadBalanceType);
        this.concurrentThreadPool = ConcurrentThreadPool.getInstance(corePoolSize, maximumPoolSize);
    }

    public void setFallbackClass(Class<?> fallbackClass) {
        this.fallbackClass = fallbackClass;
    }

    private RegistryService getRegistryService(String registryAddress, String registryType,
                                               String registryLoadBalanceType) {
        if (StringUtils.isEmpty(registryType)) {
            throw new IllegalArgumentException("registry type is null");
        }
        RegistryService registryService = ExtensionLoader.getExtension(RegistryService.class, registryType);
        try {
            registryService.init(new RegistryConfig(registryAddress, registryType, registryLoadBalanceType));
        } catch (Exception e) {
            log.error("RpcClient init registry service throws exception:{}", e.getMessage());
            throw new RegistryException(e.getMessage(), e);
        }
        return registryService;
    }

    public <T> T create(Class<T> interfaceClass) {
        ProxyFactory proxyFactory = ExtensionLoader.getExtension(ProxyFactory.class, proxy);
        proxyFactory.init(new ProxyConfig(interfaceClass, serviceVersion, serviceGroup, timeout,
                RpcConsumer.getInstance().
                        setHeartbeatInterval(heartbeatInterval).
                        setScanNotActiveChannelInterval(scanNotActiveChannelInterval).
                        setRetryInterval(retryInterval).
                        setRetryTimes(retryTimes).
                        setEnableDirectServer(enableDirectServer).
                        setDirectServerUrl(directServerUrl).
                        setEnableDelayConnection(enableDelayConnection).
                        setConcurrentThreadPool(concurrentThreadPool).
                        setFlowPostProcessor(flowType).
                        setEnableBuffer(enableBuffer).
                        setBufferSize(bufferSize).
                        setExceptionPostProcessor(exceptionPostProcessorType).
                        buildNettyGroup().
                        buildConnection(registryService),
                serializationType, async, oneWay, registryService, enableResultCache, resultCacheExpire, reflectType,
                fallbackClassName, fallbackClass, enableRateLimiter, rateLimiterType, permits, milliSeconds,
                rateLimiterFailStrategy, enableFusing, fusingType, totalFailure, fusingMilliSeconds,
                exceptionPostProcessorType));
        return proxyFactory.getProxy(interfaceClass);
    }

    public <T> IAsyncObjectProxy createAsync(Class<T> interfaceClass) {
        return new ObjectProxy<T>(interfaceClass, serviceVersion, serviceGroup, timeout,
                RpcConsumer.getInstance().
                        setHeartbeatInterval(heartbeatInterval).
                        setScanNotActiveChannelInterval(scanNotActiveChannelInterval).
                        setRetryInterval(retryInterval).
                        setRetryTimes(retryTimes).
                        setEnableDirectServer(enableDirectServer).
                        setDirectServerUrl(directServerUrl).
                        setEnableDelayConnection(enableDelayConnection).
                        setConcurrentThreadPool(concurrentThreadPool).
                        setFlowPostProcessor(flowType).
                        setEnableBuffer(enableBuffer).
                        setBufferSize(bufferSize).
                        setExceptionPostProcessor(exceptionPostProcessorType).
                        buildNettyGroup().
                        buildConnection(registryService),
                serializationType, async, oneWay, registryService, enableResultCache, resultCacheExpire, reflectType,
                fallbackClassName, enableRateLimiter, rateLimiterType, permits, milliSeconds, rateLimiterFailStrategy,
                enableFusing, fusingType, totalFailure, fusingMilliSeconds, exceptionPostProcessorType, fallbackClass);
    }

    public void shutdown() {
        RpcConsumer.getInstance().close();
    }
}
