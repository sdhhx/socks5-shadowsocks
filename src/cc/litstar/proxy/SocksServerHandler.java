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
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthStatus;

@ChannelHandler.Sharable
/**
 * 客户端与Shadowsocks sslocal之间的建连交互
 */
public final class SocksServerHandler extends SimpleChannelInboundHandler<SocksMessage> {
	//用于设置Shadowsocks服务器信息
	private RemoteServer remoteServer;
	
    public static final SocksServerHandler INSTANCE = new SocksServerHandler();

    private final static Logger logger = LoggerFactory.getLogger(SocksServerHandler.class);
    
    private SocksServerHandler() { }

    public SocksServerHandler setRemoteServer(RemoteServer remoteServer) {
    	if(!remoteServer.equals(this.remoteServer)) {
    		this.remoteServer = remoteServer;
    	}
    	return this;
    }
    
    @Override
    public void channelRead0(ChannelHandlerContext ctx, SocksMessage socksRequest) throws Exception {
        switch (socksRequest.version()) {
            case SOCKS5:
                if (socksRequest instanceof Socks5InitialRequest) {
                    // auth support example
                	//Socks5PasswordAuthRequestDecoder是Socks5初始连接设置密码使用，这里换入一个请求
                    //ctx.pipeline().addFirst(new Socks5PasswordAuthRequestDecoder());
                    //ctx.write(new DefaultSocks5AuthMethodResponse(Socks5AuthMethod.PASSWORD));
                	logger.info("Receive Socks5InitialRequest");
                    ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
                    ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
                    //收到使用密码建立请求，返回成功
                } else if (socksRequest instanceof Socks5PasswordAuthRequest) {
                	logger.info("Receive Socks5PasswordAuthRequest");
                    ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
                    ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
                //Socks5CommandRequest是客户端的建连请求
                } else if (socksRequest instanceof Socks5CommandRequest) {
                    Socks5CommandRequest socks5CmdRequest = (Socks5CommandRequest) socksRequest;
                    //收到Connect请求则处理，不然全部扔掉
                    if (socks5CmdRequest.type() == Socks5CommandType.CONNECT) {
                    	logger.info("Receive Socks5CommandRequest Connection");
                        ctx.pipeline().addLast(new SocksServerConnectHandler(remoteServer));
                        ctx.pipeline().remove(this);
                        System.out.println(ctx.pipeline());
                        //触发下一个入站Handler的channelRead，从头到尾，这里是SocksServerConnectHandler
                        ctx.fireChannelRead(socksRequest);
                    } else {
                        ctx.close();
                    }
                } else {
                    ctx.close();
                }
                break;
            case UNKNOWN:
                ctx.close();
                break;
            default:
            	ctx.close();
            	break;
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
        SocksServerUtils.closeOnFlush(ctx.channel());
    }
}

