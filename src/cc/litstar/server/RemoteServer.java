package cc.litstar.server;

/**
 * @author hehaoxing
 * 用于连接的远程服务器信息，这里用作测试，后续用作参数
 */
public class RemoteServer {	
	//Shadowsocks远程服务器 
	private String serverIP;
	//用于连接的端口
	private int port;
	//Shadowsocks用户名(只能对应端口)
	private String username;
	//Shadowsocks密码
	private String password;
	//Shadowsocks加密方法
	private String encryptMethod;
	
	//远程服务器类构造方法
	public RemoteServer(String serverIP, int port, String username, String password, String encryptMethod) {
		super();
		this.serverIP = serverIP;
		this.port = port;
		this.username = username;
		this.password = password;
		this.encryptMethod = encryptMethod;
	}

	public String getServerIP() {
		return serverIP;
	}

	public void setServerIP(String serverIP) {
		this.serverIP = serverIP;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEncryptMethod() {
		return encryptMethod;
	}

	public void setEncryptMethod(String encryptMethod) {
		this.encryptMethod = encryptMethod;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((encryptMethod == null) ? 0 : encryptMethod.hashCode());
		result = prime * result + ((password == null) ? 0 : password.hashCode());
		result = prime * result + port;
		result = prime * result + ((serverIP == null) ? 0 : serverIP.hashCode());
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RemoteServer other = (RemoteServer) obj;
		if (encryptMethod == null) {
			if (other.encryptMethod != null)
				return false;
		} else if (!encryptMethod.equals(other.encryptMethod))
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (port != other.port)
			return false;
		if (serverIP == null) {
			if (other.serverIP != null)
				return false;
		} else if (!serverIP.equals(other.serverIP))
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}
	
}
