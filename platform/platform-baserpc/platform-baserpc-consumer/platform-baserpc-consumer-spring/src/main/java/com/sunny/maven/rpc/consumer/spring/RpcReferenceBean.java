package com.sunny.maven.rpc.consumer.spring;

import com.sunny.maven.rpc.consumer.RpcClient;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author SUNNY
 * @description: RpcReferenceBean
 * @create: 2023-02-15 10:18
 */
public class RpcReferenceBean implements FactoryBean<Object> {
    /**
     * 接口类型
     */
    private Class<?> interfaceClass;
    /**
     * 版本号
     */
    private String version;
    /**
     * 注册中心类型：zookeeper/nacos/apoll/etcd/eureka等
     */
    private String registryType;
    /**
     * 负载均衡类型：zkconsistenthash
     */
    private String loadbalancerType;
    /**
     * 序列化类型：fst/kryo/protostuff/jdk/hessian2/json
     */
    private String serializationType;
    /**
     * 注册中心地址
     */
    private String registryAddress;
    /**
     * 超时时间
     */
    private long timeout;
    /**
     * 服务分组
     */
    private String group;
    /**
     * 是否异步
     */
    private boolean async;
    /**
     * 是否单向调用
     */
    private boolean oneWay;
    /**
     * 代理方式
     */
    private String proxy;
    /**
     * 生成的代理对象
     */
    private Object object;
    /**
     * 扫描空闲连接时间，默认60秒
     */
    private int scanNotActiveChannelInterval;
    /**
     * 心跳检测时间
     */
    private int heartbeatInterval;
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

    private RpcClient rpcClient;
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
     * 并发线程池核心线程数
     */
    private int corePoolSize;
    /**
     * 并发线程池最大线程数
     */
    private int maximumPoolSize;
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

    @Override
    public Object getObject() throws Exception {
        return object;
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }

    public void init() {
        rpcClient = new RpcClient(registryAddress, registryType, version, group, serializationType,
                loadbalancerType, timeout, proxy, async, oneWay, heartbeatInterval, scanNotActiveChannelInterval,
                retryInterval, retryTimes, enableResultCache, resultCacheExpire, enableDirectServer, directServerUrl,
                enableDelayConnection, corePoolSize, maximumPoolSize, flowType, enableBuffer, bufferSize, reflectType,
                fallbackClassName, enableRateLimiter, rateLimiterType, permits, milliSeconds, rateLimiterFailStrategy,
                enableFusing, fusingType, totalFailure, fusingMilliSeconds, exceptionPostProcessorType);
        rpcClient.setFallbackClass(fallbackClass);
        this.object = rpcClient.create(interfaceClass);
    }

    public void setInterfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setRegistryType(String registryType) {
        this.registryType = registryType;
    }

    public void setLoadbalancerType(String loadbalancerType) {
        this.loadbalancerType = loadbalancerType;
    }

    public void setSerializationType(String serializationType) {
        this.serializationType = serializationType;
    }

    public void setRegistryAddress(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public void setOneWay(boolean oneWay) {
        this.oneWay = oneWay;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public void setScanNotActiveChannelInterval(int scanNotActiveChannelInterval) {
        this.scanNotActiveChannelInterval = scanNotActiveChannelInterval;
    }

    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public void setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public String getVersion() {
        return version;
    }

    public String getRegistryType() {
        return registryType;
    }

    public String getLoadbalancerType() {
        return loadbalancerType;
    }

    public String getSerializationType() {
        return serializationType;
    }

    public String getRegistryAddress() {
        return registryAddress;
    }

    public long getTimeout() {
        return timeout;
    }

    public String getGroup() {
        return group;
    }

    public boolean isAsync() {
        return async;
    }

    public boolean isOneWay() {
        return oneWay;
    }

    public String getProxy() {
        return proxy;
    }

    public int getScanNotActiveChannelInterval() {
        return scanNotActiveChannelInterval;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public int getRetryInterval() {
        return retryInterval;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public RpcClient getRpcClient() {
        return rpcClient;
    }

    public boolean isEnableResultCache() {
        return enableResultCache;
    }

    public void setEnableResultCache(boolean enableResultCache) {
        this.enableResultCache = enableResultCache;
    }

    public int getResultCacheExpire() {
        return resultCacheExpire;
    }

    public void setResultCacheExpire(int resultCacheExpire) {
        this.resultCacheExpire = resultCacheExpire;
    }

    public boolean isEnableDirectServer() {
        return enableDirectServer;
    }

    public void setEnableDirectServer(boolean enableDirectServer) {
        this.enableDirectServer = enableDirectServer;
    }

    public String getDirectServerUrl() {
        return directServerUrl;
    }

    public void setDirectServerUrl(String directServerUrl) {
        this.directServerUrl = directServerUrl;
    }

    public boolean isEnableDelayConnection() {
        return enableDelayConnection;
    }

    public void setEnableDelayConnection(boolean enableDelayConnection) {
        this.enableDelayConnection = enableDelayConnection;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public String getFlowType() {
        return flowType;
    }

    public void setFlowType(String flowType) {
        this.flowType = flowType;
    }

    public boolean isEnableBuffer() {
        return enableBuffer;
    }

    public void setEnableBuffer(boolean enableBuffer) {
        this.enableBuffer = enableBuffer;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public String getReflectType() {
        return reflectType;
    }

    public void setReflectType(String reflectType) {
        this.reflectType = reflectType;
    }

    public String getFallbackClassName() {
        return fallbackClassName;
    }

    public void setFallbackClassName(String fallbackClassName) {
        this.fallbackClassName = fallbackClassName;
    }

    public Class<?> getFallbackClass() {
        return fallbackClass;
    }

    public void setFallbackClass(Class<?> fallbackClass) {
        this.fallbackClass = fallbackClass;
    }

    public boolean isEnableRateLimiter() {
        return enableRateLimiter;
    }

    public void setEnableRateLimiter(boolean enableRateLimiter) {
        this.enableRateLimiter = enableRateLimiter;
    }

    public String getRateLimiterType() {
        return rateLimiterType;
    }

    public void setRateLimiterType(String rateLimiterType) {
        this.rateLimiterType = rateLimiterType;
    }

    public int getPermits() {
        return permits;
    }

    public void setPermits(int permits) {
        this.permits = permits;
    }

    public int getMilliSeconds() {
        return milliSeconds;
    }

    public void setMilliSeconds(int milliSeconds) {
        this.milliSeconds = milliSeconds;
    }

    public String getRateLimiterFailStrategy() {
        return rateLimiterFailStrategy;
    }

    public void setRateLimiterFailStrategy(String rateLimiterFailStrategy) {
        this.rateLimiterFailStrategy = rateLimiterFailStrategy;
    }

    public boolean isEnableFusing() {
        return enableFusing;
    }

    public void setEnableFusing(boolean enableFusing) {
        this.enableFusing = enableFusing;
    }

    public String getFusingType() {
        return fusingType;
    }

    public void setFusingType(String fusingType) {
        this.fusingType = fusingType;
    }

    public double getTotalFailure() {
        return totalFailure;
    }

    public void setTotalFailure(double totalFailure) {
        this.totalFailure = totalFailure;
    }

    public int getFusingMilliSeconds() {
        return fusingMilliSeconds;
    }

    public void setFusingMilliSeconds(int fusingMilliSeconds) {
        this.fusingMilliSeconds = fusingMilliSeconds;
    }

    public String getExceptionPostProcessorType() {
        return exceptionPostProcessorType;
    }

    public void setExceptionPostProcessorType(String exceptionPostProcessorType) {
        this.exceptionPostProcessorType = exceptionPostProcessorType;
    }
}
