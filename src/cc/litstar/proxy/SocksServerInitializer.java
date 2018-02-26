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

import cc.litstar.server.RemoteServer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;

/**
 * Netty加载了启动的默认Channel
 */
public final class SocksServerInitializer extends ChannelInitializer<SocketChannel> {
	
	private RemoteServer remoteServer;
	
	public SocksServerInitializer(RemoteServer remoteServer) {
		super();
		this.remoteServer = remoteServer;
	}
	
	private final static Logger logger = LoggerFactory.getLogger(SocksServerInitializer.class);
    @Override
    public void initChannel(SocketChannel ch) throws Exception {
    	logger.info("Start init channel " + ch.toString());
    	//入站从头到尾，出站从尾到头
        ch.pipeline().addLast(
        		//不需要输出每一个数据报文
                //new LoggingHandler(LogLevel.DEBUG),
                /**	1. 先获取了一个字节，用来判断 Socks 版本号
                 *  2. 往 pipeline加入 Socks5编码器
                 *  3. 添加 Socks5InitialRequestDecoder对请求解码
                 *  4. SocksPortUnificationServerHandler已完成任务，从 pipeline中移除*/
                new SocksPortUnificationServerHandler(),
                //解码后交由SocksServerHandler处理
                SocksServerHandler.INSTANCE.setRemoteServer(remoteServer));
    }
}
