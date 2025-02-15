package com.sunny.maven.rpc.codec;

import com.sunny.maven.rpc.flow.processor.FlowPostProcessor;
import com.sunny.maven.rpc.protocol.header.RpcHeader;
import com.sunny.maven.rpc.serialization.api.Serialization;
import com.sunny.maven.rpc.serialization.jdk.JdkSerialization;
import com.sunny.maven.rpc.spi.loader.ExtensionLoader;
import com.sunny.maven.rpc.threadpool.FlowPostProcessorThreadPool;

/**
 * @author SUNNY
 * @description: 实现编解码的接口，提供序列化和反序列化的默认方法
 * @create: 2022-12-28 17:19
 */
public interface RpcCodec {
    /**
     * 根据serializationType通过SPI获取序列化句柄
     * @param serializationType 序列化方式
     * @return Serialization对象
     */
    default Serialization getSerialization(String serializationType) {
        return ExtensionLoader.getExtension(Serialization.class, serializationType);
    }

    /**
     * 调用RPC框架流量分析后置处理器
     * @param flowPostProcessor postProcessor 后置处理器
     * @param rpcHeader 封装了流量信息的消息头
     */
    default void flowPostProcessor(FlowPostProcessor flowPostProcessor, RpcHeader rpcHeader) {
        FlowPostProcessorThreadPool.submit(() -> {
            flowPostProcessor.postRpcHeaderProcessor(rpcHeader);
        });
    }
}
