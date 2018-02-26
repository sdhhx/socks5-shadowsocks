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

import cc.litstar.encrypt.ICrypt;
import cc.litstar.pac.Socks5Pac;
import cc.litstar.server.RemoteServer;
import cc.litstar.encrypt.CryptFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;

@ChannelHandler.Sharable
public final class SocksServerConnectHandler extends SimpleChannelInboundHandler<SocksMessage> {

	//启动类，测试连接
    private final Bootstrap b = new Bootstrap();
    //Shadowsocks数据加密
    private ICrypt _crypt;
    //Shadowsocks ssserver信息
    private RemoteServer remoteServer;
    //当前请求URL是否需要被代理
    private boolean isProxy = true;
    
  	private final static Logger logger = LoggerFactory.getLogger(SocksServerConnectHandler.class);
  	
  	public SocksServerConnectHandler(RemoteServer remoteServer) {
		this.remoteServer = remoteServer;
		this._crypt = CryptFactory.get(this.remoteServer.getEncryptMethod(), this.remoteServer.getPassword());
	}
    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final SocksMessage message) throws Exception {
    	//这样不方便做PAC
        if (message instanceof Socks5CommandRequest) {
        	//消息的强制类型转换
            final Socks5CommandRequest request = (Socks5CommandRequest) message;
            //创建Promise，由DirectClientHandler回调触发方法，持有服务器到Host端的Future
            //此时promise变量不持有Channel，而是触发IO操作后持有与其IO操作相关的Channel
            Promise<Channel> promise = ctx.executor().newPromise();
            promise.addListener(
                    new FutureListener<Channel>() {
                        @Override
                        public void operationComplete(final Future<Channel> future) throws Exception {
                        	//ssslocal直连或与ssserver建立的连接又被称作outBoundChannel()，这里有着空的pipeline
                        	//promise关联的channel为ssslocal直连或与ssserver建立连接的channel
                            final Channel outboundChannel = future.getNow();
                            if (future.isSuccess()) {
                            	//注意是ctx.channel()，也就是向浏览器写回建连成功消息，出站
                            	//inRelay，客户端与浏览器方向的Relay，转发浏览器数据
                            	final RelayHandler inRelay = new RelayHandler(ctx.channel(), SocksServerConnectHandler.this, 1);
                            	//outRelay，客户端与远程服务器方向的Relay，接受浏览器数据
                            	final RelayHandler outRelay = new RelayHandler(outboundChannel, SocksServerConnectHandler.this, 2);
                                ChannelFuture responseFuture =
                                        ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                                                Socks5CommandStatus.SUCCESS,
                                                request.dstAddrType(),
                                                request.dstAddr(),
                                                request.dstPort()));

                                responseFuture.addListener(new ChannelFutureListener() {
                                    @Override
                                    public void operationComplete(ChannelFuture channelFuture) {
                                    	if(isProxy) {
                                    		sendConnectRemoteMessage(request, outboundChannel);
                                    	}
                                    	//Command请求处理完成，将Channel对接
                                        ctx.pipeline().remove(SocksServerConnectHandler.this);
                                        //outBoundChannel的管道上持有inBoundChannel的RelayHandler
                                        outboundChannel.pipeline().addLast(inRelay);
                                        //inBoundChannel的管道上持有outBoundChannel的RelayHandler
                                        ctx.pipeline().addLast(outRelay);
                                        logger.info("Channel establish successful");
                                    }
                                });
                            } else {
                                ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                                        Socks5CommandStatus.FAILURE, request.dstAddrType()));
                                SocksServerUtils.closeOnFlush(ctx.channel());
                            }
                        }
                    });
            
            //inboundChannel是浏览器与客户端建立的连接的Channel
            //相同的eventLoop管理两个Channel
            final Channel inboundChannel = ctx.channel();
            b.group(inboundChannel.eventLoop())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new DirectClientHandler(promise));
            
            //dstAddr是包含域名的
            logger.info(request.dstAddr() + ": " + Socks5Pac.getSocks5Pac().isProxyed(request.dstAddr()));
            setProxy(request.dstAddr());
            
            //与远端服务器建连成功后回调
            //promise设置的channel是sslocal发放ssserver的
            //b.connect(request.dstAddr(), request.dstPort())
            b.connect(getIpAddr(request), getPort(request)).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        // Connection established use handler provided results
                    	// Promise的回调方法被调用
                    } else {
                        // Close the connection if the connection attempt has failed.
                    	// 写回失败消息
                        ctx.channel().writeAndFlush(
                                new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
                        SocksServerUtils.closeOnFlush(ctx.channel());
                    }
                }
            });
        } else {
            ctx.close();
        }
    }
    
    public void setProxy(String host) {
		this.isProxy = Socks5Pac.getSocks5Pac().isProxyed(host);
	}

    /**
     * 决定建连的IP地址，需要代理则返回代理服务器IP与端口
     */
    private String getIpAddr(Socks5CommandRequest request) {
		if (isProxy) {
			return remoteServer.getServerIP();
		} else {
			return request.dstAddr();
		}
	}
    
    private int getPort(Socks5CommandRequest request) {
		if (isProxy) {
			return remoteServer.getPort();
		} else {
			return request.dstPort();
		}
	}
    
    /**
	 * localserver和remoteserver进行connect发送的数据
	 */
	private void sendConnectRemoteMessage(Socks5CommandRequest request, Channel outboundChannel) {
		ByteBuf buff = Unpooled.buffer();
		//复制netty4.0的方法
		encodeAsByteBuf(request, buff);
		if (buff.hasArray()) {
			int len = buff.readableBytes();
			byte[] arr = new byte[len];
			buff.getBytes(0, arr);
			byte[] data = remoteByte(arr);
			sendRemote(data, data.length, outboundChannel);
		}
	}
	
	/**
	 * 将Socks5请求编码为二进制形式，并置于byteBuf之中
	 */
	private void encodeAsByteBuf(Socks5CommandRequest request, ByteBuf byteBuf) {
		SocksVersion socksVersion = request.version();
		Socks5CommandType socks5CommandType = request.type();
		String socks5DstAddr = request.dstAddr();
		int socks5DstPort = request.dstPort();
		Socks5AddressType socks5AddressType = request.dstAddrType();
		
		byteBuf.writeByte(socksVersion.byteValue());
        byteBuf.writeByte(socks5CommandType.byteValue());
        byteBuf.writeByte(0x00);
        byteBuf.writeByte(socks5AddressType.byteValue());
        switch (socks5AddressType.byteValue()) {
            case 0x01: {
                byteBuf.writeBytes(NetUtil.createByteArrayFromIpAddressString(socks5DstAddr));
                byteBuf.writeShort(socks5DstPort);
                break;
            }
            case 0x03: {
                byteBuf.writeByte(socks5DstAddr.length());
                byteBuf.writeBytes(socks5DstAddr.getBytes(CharsetUtil.US_ASCII));
                byteBuf.writeShort(socks5DstPort);
                break;
            }
            case 0x04: {
                byteBuf.writeBytes(NetUtil.createByteArrayFromIpAddressString(socks5DstAddr));
                byteBuf.writeShort(socks5DstPort);
                break;
            }
        }
	}
    
	/**
	 * localserver和remoteserver进行connect发送的数据
	 * 
	 * +-----+-----+-------+------+----------+----------+ 
	 * | VER | CMD | RSV   | ATYP | DST.ADDR | DST.PORT |
	 * +-----+-----+-------+------+----------+----------+ 
	 * | 1   | 1   | X'00' | 1    |Variable  | 2        | 
	 * +-----+-----+-------+------+----------+----------+
	 * 
	 * Shadowsocks与socks5协议是不同的，Shadowsocks建连请求为Socks5 Command命令去掉前三个字符
	 * 故这里在Socks5建连请求的基础上，跳过前面3个字节用作建连报文
	 */
	private byte[] remoteByte(byte[] data) {
		int dataLength = data.length;
		dataLength -= 3;
		byte[] temp = new byte[dataLength];
		System.arraycopy(data, 3, temp, 0, dataLength);
		return temp;
	}
	
	/**
	 * 给remoteserver发送数据--需要进行加密处理
	 */
	public void sendRemote(byte[] data, int length, Channel channel) {
		ByteArrayOutputStream _remoteOutStream = null;
		try {
			_remoteOutStream = new ByteArrayOutputStream();
			if (isProxy) {
				_crypt.encrypt(data, length, _remoteOutStream);
				data = _remoteOutStream.toByteArray();
			}
			channel.writeAndFlush(Unpooled.wrappedBuffer(data));
		} catch (Exception e) {
			//logger.error("sendRemote error", e);
		} finally {
			if (_remoteOutStream != null) {
				try {
					_remoteOutStream.close();
				} catch (IOException e) {
				}
			}
		}
		//logger.info("sendRemote message:isProxy = " + isProxy + ",length = " + length + ",channel = " + channel);
	}
	
	/**
	 * 给本地客户端回复消息--需要进行解密处理
	 */
	public void sendLocal(byte[] data, int length, Channel channel) {
		ByteArrayOutputStream _localOutStream = null;
		try {
			_localOutStream = new ByteArrayOutputStream();
			if (isProxy) {
				_crypt.decrypt(data, length, _localOutStream);
				data = _localOutStream.toByteArray();
			}
			channel.writeAndFlush(Unpooled.wrappedBuffer(data));
		} catch (Exception e) {
			//logger.error("sendLocal error", e);
		} finally {
			if (_localOutStream != null) {
				try {
					_localOutStream.close();
				} catch (IOException e) {
				}
			}
		}
		//logger.info("sendLocal message:isProxy = " + isProxy + ",length = " + length + ",channel = " + channel);
	}
	
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        SocksServerUtils.closeOnFlush(ctx.channel());
    }
}
