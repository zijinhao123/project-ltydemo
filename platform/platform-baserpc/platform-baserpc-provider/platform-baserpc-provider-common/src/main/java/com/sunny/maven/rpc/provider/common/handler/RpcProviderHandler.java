package com.sunny.maven.rpc.provider.common.handler;

import com.sunny.maven.rpc.buffer.cache.BufferCacheManager;
import com.sunny.maven.rpc.buffer.object.BufferObject;
import com.sunny.maven.rpc.cache.result.CacheResultKey;
import com.sunny.maven.rpc.cache.result.CacheResultManager;
import com.sunny.maven.rpc.common.helper.RpcServiceHelper;
import com.sunny.maven.rpc.common.utils.StringUtils;
import com.sunny.maven.rpc.connection.manager.ConnectionManager;
import com.sunny.maven.rpc.constants.RpcConstants;
import com.sunny.maven.rpc.exception.processor.ExceptionPostProcessor;
import com.sunny.maven.rpc.fusing.api.FusingInvoker;
import com.sunny.maven.rpc.protocol.RpcProtocol;
import com.sunny.maven.rpc.protocol.enumeration.RpcStatus;
import com.sunny.maven.rpc.protocol.enumeration.RpcType;
import com.sunny.maven.rpc.protocol.header.RpcHeader;
import com.sunny.maven.rpc.protocol.request.RpcRequest;
import com.sunny.maven.rpc.protocol.response.RpcResponse;
import com.sunny.maven.rpc.provider.common.cache.ProviderChannelCache;
import com.sunny.maven.rpc.ratelimiter.api.RateLimiterInvoker;
import com.sunny.maven.rpc.reflect.api.ReflectInvoker;
import com.sunny.maven.rpc.spi.loader.ExtensionLoader;
import com.sunny.maven.rpc.threadpool.BufferCacheThreadPool;
import com.sunny.maven.rpc.threadpool.ConcurrentThreadPool;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @author SUNNY
 * @description: RPC服务提供者的Handler处理类
 * @create: 2022-12-26 17:29
 */
@Slf4j
public class RpcProviderHandler extends SimpleChannelInboundHandler<RpcProtocol<RpcRequest>> {
    /**
     * 存储服务名称#版本号#分组与对象实例的映射关系
     */
    private final Map<String, Object> handlerMap;
    /**
     * 调用采用哪种类型调用真实方法
     */
    private ReflectInvoker reflectInvoker;
    /**
     * 是否启用结果缓存
     */
    private final boolean enableResultCache;
    /**
     * 结果缓存管理器
     */
    private final CacheResultManager<RpcProtocol<RpcResponse>> cacheResultManager;
    /**
     * 线程池
     */
    private final ConcurrentThreadPool concurrentThreadPool;
    /**
     * 连接管理器
     */
    private ConnectionManager connectionManager;
    /**
     * 是否开启缓冲区
     */
    private boolean enableBuffer;
    /**
     * 缓冲区管理器
     */
    private BufferCacheManager<BufferObject<RpcRequest>> bufferCacheManager;
    /**
     * 是否开启限流
     */
    private boolean enableRateLimiter;
    /**
     * 限流SPI接口
     */
    private RateLimiterInvoker rateLimiterInvoker;
    /**
     * 当限流失败时的处理策略
     */
    private String rateLimiterFailStrategy;
    /**
     * 是否开启熔断
     */
    private boolean enableFusing;
    /**
     * 熔断SPI接口
     */
    private FusingInvoker fusingInvoker;
    /**
     * 异常处理后置处理器
     */
    private ExceptionPostProcessor exceptionPostProcessor;

    public RpcProviderHandler(String reflectType, boolean enableResultCache, int cacheResultExpire, int corePoolSize,
                              int maximumPoolSize, int maxConnections, String disuseStrategyType, boolean enableBuffer,
                              int bufferSize, boolean enableRateLimiter, String rateLimiterType, int permits,
                              int milliSeconds, String rateLimiterFailStrategy, boolean enableFusing, String fusingType,
                              double totalFailure, int fusingMilliSeconds, String exceptionPostProcessorType,
                              Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
        this.reflectInvoker = ExtensionLoader.getExtension(ReflectInvoker.class, reflectType);
        this.enableResultCache = enableResultCache;
        if (cacheResultExpire <= 0) {
            cacheResultExpire = RpcConstants.RPC_SCAN_RESULT_CACHE_EXPIRE;
        }
        this.cacheResultManager = CacheResultManager.getInstance(cacheResultExpire, enableResultCache);
        this.concurrentThreadPool = ConcurrentThreadPool.getInstance(corePoolSize, maximumPoolSize);
        this.connectionManager = ConnectionManager.getInstance(maxConnections, disuseStrategyType);
        this.enableBuffer = enableBuffer;
        this.initBuffer(bufferSize);
        this.enableRateLimiter = enableRateLimiter;
        this.initRateLimiter(rateLimiterType, permits, milliSeconds);
        if (StringUtils.isEmpty(rateLimiterFailStrategy)) {
            rateLimiterFailStrategy = RpcConstants.RATE_LIMITER_FAIL_STRATEGY_DIRECT;
        }
        this.rateLimiterFailStrategy = rateLimiterFailStrategy;
        this.enableFusing = enableFusing;
        this.initFusing(fusingType, totalFailure, fusingMilliSeconds);
        if (StringUtils.isEmpty(exceptionPostProcessorType)) {
            exceptionPostProcessorType = RpcConstants.EXCEPTION_POST_PROCESSOR_PRINT;
        }
        this.exceptionPostProcessor = ExtensionLoader.getExtension(ExceptionPostProcessor.class,
                exceptionPostProcessorType);
    }

    /**
     * 初始化熔断SPI接口
     */
    private void initFusing(String fusingType, double totalFailure, int fusingMilliSeconds) {
        if (enableFusing) {
            fusingType = StringUtils.isEmpty(fusingType) ? RpcConstants.DEFAULT_FUSING_INVOKER : fusingType;
            this.fusingInvoker = ExtensionLoader.getExtension(FusingInvoker.class, fusingType);
            this.fusingInvoker.init(totalFailure, fusingMilliSeconds);
        }
    }

    /**
     * 初始化限流器
     */
    private void initRateLimiter(String rateLimiterType, int permits, int milliSeconds) {
        if (enableRateLimiter) {
            rateLimiterType = StringUtils.isEmpty(rateLimiterType) ?
                    RpcConstants.DEFAULT_RATELIMITER_INVOKER : rateLimiterType;
            this.rateLimiterInvoker = ExtensionLoader.getExtension(RateLimiterInvoker.class, rateLimiterType);
            this.rateLimiterInvoker.init(permits, milliSeconds);
        }
    }

    /**
     * 初始化缓冲区数据
     */
    private void initBuffer(int bufferSize) {
        // 开启缓冲
        if (enableBuffer) {
            log.info("enable buffer...");
            this.bufferCacheManager = BufferCacheManager.getInstance(bufferSize);
            BufferCacheThreadPool.submit(this::consumerBufferCache);
        }
    }

    /**
     * 消费缓冲区的数据
     */
    private void consumerBufferCache() {
        // 不断消息缓冲区的数据
        while (true) {
            BufferObject<RpcRequest> bufferObject = this.bufferCacheManager.take();
            if (bufferObject != null) {
                ChannelHandlerContext ctx = bufferObject.getCtx();
                RpcProtocol<RpcRequest> protocol = bufferObject.getProtocol();
                RpcHeader header = protocol.getHeader();
                RpcProtocol<RpcResponse> responseRpcProtocol = handlerRequestMessageWithCache(protocol, header);
                this.writeAndFlush(header.getRequestId(), ctx, responseRpcProtocol);
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ProviderChannelCache.add(ctx.channel());
        connectionManager.add(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ProviderChannelCache.remove(ctx.channel());
        connectionManager.remove(ctx.channel());
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        ProviderChannelCache.remove(ctx.channel());
        connectionManager.remove(ctx.channel());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 如果是IdleStateEvent事件
        if (evt instanceof IdleStateEvent) {
            Channel channel= ctx.channel();
            try {
                log.info("IdleStateEvent triggered, close channel {}", channel.remoteAddress());
                connectionManager.remove(channel);
                channel.close();
            } finally {
                channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcProtocol<RpcRequest> protocol) throws Exception {
        concurrentThreadPool.submit(() -> {
            connectionManager.update(ctx.channel());
            if (enableBuffer) {
                // 开启队列缓冲
                this.bufferRequest(ctx, protocol);
            } else {
                // 未开启队列缓冲
                this.submitRequest(ctx, protocol);
            }
        });
    }

    /**
     * 缓冲数据
     */
    private void bufferRequest(ChannelHandlerContext ctx, RpcProtocol<RpcRequest> protocol) {
        RpcHeader header = protocol.getHeader();
        if (header.getMsgType() == (byte) RpcType.HEARTBEAT_FROM_CONSUMER.getType()) {
            // 接收到服务消费者发送的心跳消息
            RpcProtocol<RpcResponse> responseRpcProtocol = handlerHeartbeatMessageFromConsumer(protocol, header);
            this.writeAndFlush(header.getRequestId(), ctx, responseRpcProtocol);
        } else if (header.getMsgType() == (byte) RpcType.HEARTBEAT_TO_PROVIDER.getType()) {
            // 接收到服务消费者响应的心跳消息
            handlerHeartbeatMessageToProvider(protocol, ctx.channel());
        } else if (header.getMsgType() == (byte) RpcType.REQUEST.getType()) {
            // 请求消息
            this.bufferCacheManager.put(new BufferObject<>(ctx, protocol));
        }
    }

    /**
     * 提交请求
     */
    private void submitRequest(ChannelHandlerContext ctx, RpcProtocol<RpcRequest> protocol) {
        RpcProtocol<RpcResponse> responseRpcProtocol = handlerMessage(protocol, ctx.channel());
        this.writeAndFlush(protocol.getHeader().getRequestId(), ctx, responseRpcProtocol);
    }

    /**
     * 向服务消费者写回数据
     */
    private void writeAndFlush(long requestId, ChannelHandlerContext ctx,
                               RpcProtocol<RpcResponse> responseRpcProtocol) {
        ctx.writeAndFlush(responseRpcProtocol).addListener((ChannelFutureListener) channelFuture ->
                log.debug("Send response for request {}", requestId));
    }

    /**
     * 处理消息
     */
    private RpcProtocol<RpcResponse> handlerMessage(RpcProtocol<RpcRequest> protocol, Channel channel) {
        RpcProtocol<RpcResponse> responseRpcProtocol = null;
        RpcHeader header = protocol.getHeader();
        if (header.getMsgType() == (byte) RpcType.HEARTBEAT_FROM_CONSUMER.getType()) {
            // 接收到服务消费者发送的心跳消息
            responseRpcProtocol = handlerHeartbeatMessageFromConsumer(protocol, header);
        } else if (header.getMsgType() == (byte) RpcType.HEARTBEAT_TO_PROVIDER.getType()) {
            // 接收到服务消费者响应的心跳消息
            handlerHeartbeatMessageToProvider(protocol, channel);
        } else if (header.getMsgType() == (byte) RpcType.REQUEST.getType()) {
            // 请求消息
            responseRpcProtocol = handlerRequestMessageWithCacheAndRateLimiter(protocol, header);
        }
        return responseRpcProtocol;
    }

    /**
     * 带有限流模式提交请求信息
     */
    private RpcProtocol<RpcResponse> handlerRequestMessageWithCacheAndRateLimiter(RpcProtocol<RpcRequest> protocol,
                                                                                  RpcHeader header) {
        RpcProtocol<RpcResponse> responseRpcProtocol = null;
        if (enableRateLimiter) {
            if (rateLimiterInvoker.tryAcquire()) {
                try {
                    responseRpcProtocol = this.handlerRequestMessageWithCache(protocol, header);
                } finally {
                    rateLimiterInvoker.release();
                }
            } else {
                responseRpcProtocol = this.invokeFailRateLimiterMethod(protocol, header);
            }
        } else {
            responseRpcProtocol = this.handlerRequestMessageWithCache(protocol, header);
        }
        return responseRpcProtocol;
    }

    /**
     * 执行限流失败时的处理逻辑
     */
    private RpcProtocol<RpcResponse> invokeFailRateLimiterMethod(RpcProtocol<RpcRequest> protocol, RpcHeader header) {
        log.info("execute {} fail rate limiter strategy...", rateLimiterFailStrategy);
        switch (rateLimiterFailStrategy) {
            case RpcConstants.RATE_LIMITER_FAIL_STRATEGY_EXCEPTION:
            case RpcConstants.RATE_LIMITER_FAIL_STRATEGY_FALLBACK:
                return this.handlerFallbackMessage(protocol);
            case RpcConstants.RATE_LIMITER_FAIL_STRATEGY_DIRECT:
                return this.handlerRequestMessageCache(protocol, header);
        }
        return this.handlerRequestMessageCache(protocol, header);
    }

    /**
     * 处理降级（容错）消息
     */
    private RpcProtocol<RpcResponse> handlerFallbackMessage(RpcProtocol<RpcRequest> protocol) {
        RpcProtocol<RpcResponse> responseRpcProtocol = new RpcProtocol<>();
        RpcHeader header = protocol.getHeader();
        header.setStatus((byte) RpcStatus.FAIL.getCode());
        header.setMsgType((byte) RpcType.RESPONSE.getType());
        responseRpcProtocol.setHeader(header);

        RpcResponse response = new RpcResponse();
        response.setError("provider execute ratelimiter fallback strategy...");
        responseRpcProtocol.setBody(response);

        return responseRpcProtocol;
    }

    /**
     * 结合缓存处理结果
     */
    private RpcProtocol<RpcResponse> handlerRequestMessageWithCache(RpcProtocol<RpcRequest> protocol,
                                                                    RpcHeader header) {
        header.setMsgType((byte) RpcType.RESPONSE.getType());
        if (this.enableResultCache) {
            return this.handlerRequestMessageCache(protocol, header);
        }
        return this.handlerRequestMessageWithFusing(protocol, header);
    }

    /**
     * 处理缓存
     */
    private RpcProtocol<RpcResponse> handlerRequestMessageCache(RpcProtocol<RpcRequest> protocol, RpcHeader header) {
        RpcRequest request = protocol.getBody();
        CacheResultKey cacheResultKey = new CacheResultKey(request.getClassName(), request.getMethodName(),
                request.getParameterTypes(), request.getParameters(), request.getVersion(), request.getGroup());
        RpcProtocol<RpcResponse> responseRpcProtocol = this.cacheResultManager.get(cacheResultKey);
        if (responseRpcProtocol == null) {
            responseRpcProtocol = this.handlerRequestMessageWithFusing(protocol, header);
            // 设置保存的时间
            cacheResultKey.setCacheTimeStamp(System.currentTimeMillis());
            this.cacheResultManager.put(cacheResultKey, responseRpcProtocol);
        }
        RpcHeader responseHeader = responseRpcProtocol.getHeader();
        responseHeader.setRequestId(header.getRequestId());
        responseRpcProtocol.setHeader(responseHeader);
        return responseRpcProtocol;
    }

    /**
     * 处理服务消费者响应的心跳消息
     */
    private void handlerHeartbeatMessageToProvider(RpcProtocol<RpcRequest> protocol, Channel channel) {
        log.info("receive service consumer heartbeat message, the consumer is:{}, the heartbeat message is:{}",
                channel.remoteAddress(), protocol.getBody().getParameters()[0]);
    }

    /**
     * 处理心跳消息
     */
    private RpcProtocol<RpcResponse> handlerHeartbeatMessageFromConsumer(RpcProtocol<RpcRequest> protocol,
                                                                         RpcHeader header) {
        header.setMsgType((byte) RpcType.HEARTBEAT_TO_CONSUMER.getType());
        RpcRequest request = protocol.getBody();
        RpcProtocol<RpcResponse> responseRpcProtocol = new RpcProtocol<>();
        RpcResponse response = new RpcResponse();
        response.setResult(RpcConstants.HEARTBEAT_PONG);
        response.setOneway(request.isOneway());
        response.setAsync(request.isAsync());
        header.setStatus((byte) RpcStatus.SUCCESS.getCode());
        responseRpcProtocol.setHeader(header);
        responseRpcProtocol.setBody(response);
        return responseRpcProtocol;
    }

    /**
     * 结合服务熔断请求方法
     */
    private RpcProtocol<RpcResponse> handlerRequestMessageWithFusing(RpcProtocol<RpcRequest> protocol, RpcHeader header) {
        if (enableFusing) {
            return this.handlerFusingRequestMessage(protocol, header);
        } else {
            return this.handlerRequestMessage(protocol, header);
        }
    }

    /**
     * 开启熔断策略时调用的方法
     */
    private RpcProtocol<RpcResponse> handlerFusingRequestMessage(RpcProtocol<RpcRequest> protocol, RpcHeader header) {
        // 如果触发了熔断的规则，则直接返回降级处理数据
        if (fusingInvoker.invokeFusingStrategy()) {
            return handlerFallbackMessage(protocol);
        }
        // 请求计数加1
        fusingInvoker.incrementCount();
        // 调用handlerRequestMessage()方法获取数据
        RpcProtocol<RpcResponse> responseRpcProtocol = this.handlerRequestMessage(protocol, header);
        if (responseRpcProtocol == null) {
            return null;
        }
        // 如果是调用失败，则失败次数加1
        if (responseRpcProtocol.getHeader().getStatus() == (byte) RpcStatus.FAIL.getCode()) {
            fusingInvoker.markFail();
        } else {
            fusingInvoker.markSuccess();
        }
        return responseRpcProtocol;
    }

    /**
     * 处理请求消息
     */
    private RpcProtocol<RpcResponse> handlerRequestMessage(RpcProtocol<RpcRequest> protocol, RpcHeader header) {
        RpcRequest request = protocol.getBody();
        log.debug("Receive request " + header.getRequestId());
        RpcProtocol<RpcResponse> responseRpcProtocol = new RpcProtocol<>();
        RpcResponse response = new RpcResponse();
        try {
            Object result = handler(request);
            response.setResult(result);
            response.setOneway(request.isOneway());
            response.setAsync(request.isAsync());
            header.setStatus((byte) RpcStatus.SUCCESS.getCode());
        } catch (Throwable t) {
            exceptionPostProcessor.postExceptionProcessor(t);
            response.setError(t.toString());
            header.setStatus((byte) RpcStatus.FAIL.getCode());
            log.error("RPC Server handle request error", t);
        }
        responseRpcProtocol.setHeader(header);
        responseRpcProtocol.setBody(response);
        return responseRpcProtocol;
    }

    private Object handler(RpcRequest request) throws Throwable {
        String serviceKey = RpcServiceHelper.buildServiceKey(request.getClassName(), request.getVersion(),
                request.getGroup());
        Object serviceBean = handlerMap.get(serviceKey);
        if (serviceBean == null) {
            throw new RuntimeException(String.format("service not exist: %s:%s", request.getClassName(),
                    request.getMethodName()));
        }
        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();

        log.debug(serviceClass.getName());
        log.debug(methodName);
        if (parameterTypes != null && parameterTypes.length > 0) {
            for (Class<?> parameterType : parameterTypes) {
                log.debug(parameterType.getName());
            }
        }
        if (parameters != null && parameters.length > 0) {
            for (Object parameter : parameters) {
                log.debug(parameter.toString());
            }
        }
        return this.reflectInvoker.invokeMethod(serviceBean, serviceClass, methodName, parameterTypes, parameters);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("server caught exception", cause);
        exceptionPostProcessor.postExceptionProcessor(cause);
        ProviderChannelCache.remove(ctx.channel());
        connectionManager.remove(ctx.channel());
        ctx.close();
    }
}
