package com.sunny.maven.rpc.codec;

import com.sunny.maven.rpc.common.utils.SerializationUtils;
import com.sunny.maven.rpc.constants.RpcConstants;
import com.sunny.maven.rpc.flow.processor.FlowPostProcessor;
import com.sunny.maven.rpc.protocol.RpcProtocol;
import com.sunny.maven.rpc.protocol.enumeration.RpcType;
import com.sunny.maven.rpc.protocol.header.RpcHeader;
import com.sunny.maven.rpc.protocol.request.RpcRequest;
import com.sunny.maven.rpc.protocol.response.RpcResponse;
import com.sunny.maven.rpc.serialization.api.Serialization;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;

import java.util.List;

/**
 * @author SUNNY
 * @description: 实现RPC解码操作
 * @create: 2022-12-28 17:36
 */
public class RpcDecoder extends ByteToMessageDecoder implements RpcCodec {

    private FlowPostProcessor flowPostProcessor;
    public RpcDecoder(FlowPostProcessor flowPostProcessor) {
        this.flowPostProcessor = flowPostProcessor;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < RpcConstants.HEADER_TOTAL_LEN) {
            return ;
        }
        in.markReaderIndex();
        short magic = in.readShort();
        if (magic != RpcConstants.MAGIC) {
            throw new IllegalArgumentException("magic number is illegal, " + magic);
        }
        byte msgType = in.readByte();
        byte status = in.readByte();
        long requestId = in.readLong();
        ByteBuf serializationTypeByteBuf = in.readBytes(SerializationUtils.MAX_SERIALIZATION_TYPE_COUNT);
        String serializationType = SerializationUtils.subString(serializationTypeByteBuf.toString(CharsetUtil.UTF_8));
        int dataLength = in.readInt();
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return ;
        }
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        RpcType msgTypeEnum = RpcType.findByType(msgType);
        if (msgTypeEnum == null) {
            return ;
        }
        RpcHeader header = new RpcHeader();
        header.setMagic(magic);
        header.setStatus(status);
        header.setRequestId(requestId);
        header.setMsgType(msgType);
        header.setSerializationType(serializationType);
        header.setMsgLen(dataLength);
        // TODO Serialization是扩展点
        Serialization serialization = getSerialization(serializationType);
        switch (msgTypeEnum) {
            case REQUEST:
            // TODO 新增CASE
            // 服务消费者发送给服务提供者的心跳数据
            case HEARTBEAT_FROM_CONSUMER:
            // 服务提供者发送给服务消费者的心跳数据
            case HEARTBEAT_TO_PROVIDER:
                RpcRequest request = serialization.deSerialize(data, RpcRequest.class);
                if (request != null) {
                    RpcProtocol<RpcRequest> protocol = new RpcProtocol<>();
                    protocol.setHeader(header);
                    protocol.setBody(request);
                    out.add(protocol);
                }
                break;
            case RESPONSE:
            // TODO 新增case
            // 服务提供者响应服务消费者的心跳数据
            case HEARTBEAT_TO_CONSUMER:
            // 服务消费者响应服务提供者的心跳数据
            case HEARTBEAT_FROM_PROVIDER:
                RpcResponse response = serialization.deSerialize(data, RpcResponse.class);
                if (response != null) {
                    RpcProtocol<RpcResponse> protocol = new RpcProtocol<>();
                    protocol.setHeader(header);
                    protocol.setBody(response);
                    out.add(protocol);
                }
                break;
        }
        // 异步调用流控分析后置处理器
        this.flowPostProcessor(flowPostProcessor, header);
    }
}
