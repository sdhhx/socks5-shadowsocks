package cc.litstar.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.litstar.pac.Socks5Pac;
import cc.litstar.server.RemoteServer;
import cc.litstar.util.NetworkUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * 启动netty Shadowsocks客户端
 */
public final class SocksServer {
	
	private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap b;
    
    private RemoteServer remoteServer;
    private int localPort;
    
    private Thread executor;

    private static SocksServer socksServer = null;
    
    private final static Logger logger = LoggerFactory.getLogger(SocksServer.class);
    
    private SocksServer(RemoteServer remoteServer, int localPort) {
    	this.remoteServer = remoteServer;
    	this.localPort = localPort;
    }
    
    public static SocksServer newInstance(RemoteServer remoteServer) {
    	socksServer = new SocksServer(remoteServer, 1080);
    	socksServer.init();
    	return socksServer;
    }
    
    public static SocksServer newInstance(RemoteServer remoteServer, int localPort) {
    	socksServer = new SocksServer(remoteServer, localPort);
    	socksServer.init();
    	return socksServer;
    }
    
    public static SocksServer getInstance() {
    	return socksServer;
    }
    
    private void init() {
    	new Thread(() -> Socks5Pac.getSocks5Pac()).start();
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
        	.channel(NioServerSocketChannel.class)
        	.handler(new LoggingHandler(LogLevel.INFO))
        	//初始化
        	.childHandler(new SocksServerInitializer(remoteServer));
    }

    public boolean start() {
    	if(NetworkUtil.isLoclePortUsing(localPort) || b == null) {
    		return false;
    	}
    	executor = new Thread(() -> {
    		try {
    			logger.info("Socks5 proxy started");
                b.bind(localPort).sync().channel().closeFuture().sync();
    		} catch (InterruptedException e) {
    			logger.info("Socks5 proxy stoped");
			} finally {
	            bossGroup.shutdownGracefully();
	            workerGroup.shutdownGracefully();
			}
    	});
    	executor.start();
    	return true;
    }
    
    public void stop() {
    	if(executor != null) {
    		executor.interrupt();
    	}	
    }
    
}
