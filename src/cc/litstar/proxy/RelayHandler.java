/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package cc.litstar.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * 用于浏览器--sslocal与sslocal与ssserver之间的转发
 */
public final class RelayHandler extends ChannelInboundHandlerAdapter {

	//用于中转的Channel，主要用于将一个Channel的入站消息转换为另一个Channel的出站消息
    private final Channel relayChannel;
    //用于调度方法
    private SocksServerConnectHandler connectHandler;
    //RelayHandler的类别：
    //  1. outBoundChannel的管道上持有inBoundChannel的RelayHandler，即inRelay
    //     用于将消息发往本地浏览器的中转Handler
    //  2. inBoundChannel的管道上持有outBoundChannel的RelayHandler，即outRelay
    //     用于将消息发往远程服务器的中转Handler
    private int relayType;
    
    //日志类
    private final static Logger logger = LoggerFactory.getLogger(RelayHandler.class);

    public RelayHandler(Channel relayChannel, SocksServerConnectHandler connectHandler, int relayType) {
		this.relayChannel = relayChannel;
		this.connectHandler = connectHandler;
		this.relayType = relayType;
	}

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    /**
     * 四次握手结束后，客户端发送真正的数据
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (relayChannel.isActive()) {
        	//拦截了Channel的出站请求，交于另一个Channel，消息出站
        	if(relayType == 1) {
        		//inRelayHandler
        		//logger.info(relayChannel.toString() + " Send data to browser");
        		ByteBuf bytebuff = (ByteBuf) msg;
				if (!bytebuff.hasArray()) {
					int len = bytebuff.readableBytes();
					byte[] arr = new byte[len];
					bytebuff.getBytes(0, arr);
					connectHandler.sendLocal(arr, arr.length, relayChannel);
				}
        		//relayChannel.writeAndFlush(msg);
        	} else if(relayType == 2) {
        		//outRelayHandler
        		//logger.info(relayChannel.toString() + " Send data to ssserver");
        		ByteBuf bytebuff = (ByteBuf) msg;
				if (!bytebuff.hasArray()) {
					int len = bytebuff.readableBytes();
					byte[] arr = new byte[len];
					bytebuff.getBytes(0, arr);
					connectHandler.sendRemote(arr, arr.length, relayChannel);
				}
        		//relayChannel.writeAndFlush(msg);
        	} else {
        		logger.info("Wrong relay Handler used");
        	}
        	
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (relayChannel.isActive()) {
            SocksServerUtils.closeOnFlush(relayChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
