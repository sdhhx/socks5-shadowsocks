package cc.litstar;

import cc.litstar.proxy.SocksServer;
import cc.litstar.server.RemoteServer;

/**
 * @author hehaoxing
 * 测试用
 */
public class TestServer {
	
	public static void main(String[] args) throws Exception {
		RemoteServer remoteServer = new RemoteServer("45.32.59.166", 8399, null, "1234567890", "aes-256-cfb");
		 SocksServer server = SocksServer.newInstance(remoteServer);
		 server.start();
		 Thread.sleep(1000000);
		 server.stop();
	}
}
