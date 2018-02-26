package cc.litstar.pac;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * @author hehaoxing
 * Socks5协议的PAC协议
 */
public class Socks5Pac {
	//从配置文件中读取pac自动代理配置文件列表
	PacList pacList = null;
	
	private HashSet<String> allowSet;
	private HashSet<String> blockSet;
	private ArrayList<String> regAllowList;
	
	private Set<String> cachedAllowedSet;
	private Set<String> cachedBlockedSet;
	//打包后注意复制配置文件
	private static final String pacFile = "conf/SS-paclist.json";

	private static volatile Socks5Pac socks5Pac = null;
	
	private Socks5Pac() {}
	
	/**
	 * 加载Pac文件列表
	 */
	private void loadPacList() {
		File pac = new File(pacFile);
		if(pac.exists()) {
			String json = "";
			String line = null;
			try(BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(pac), "UTF-8")) ){
				//注释写法
				while((line = in.readLine()) != null){
					if(!line.startsWith(";")){
						json += line;
					}	
				}
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
				e.printStackTrace();
			}
			json = json.replaceAll("\\s+", "");
			//将字符串读入配置文件
			Gson gson = new Gson();  
			Type type = new TypeToken<PacList>(){}.getType();  
			try{
				pacList = gson.fromJson(json, type);  
			}catch (Exception e) {
				System.exit(0);
			}
			allowSet = new HashSet<>(pacList.getAllowList());
			blockSet = new HashSet<>(pacList.getBlockList());
			regAllowList = new ArrayList<>(pacList.getRegAllowList());
			cachedAllowedSet = Collections.synchronizedSet(new HashSet<>());
			cachedBlockedSet = Collections.synchronizedSet(new HashSet<>());
		} else {
			System.exit(0);
		}
	}
	
	public boolean isProxyed(String domain) {
		//1. 首先判断是否在缓存中，避免重复判断
		if(cachedBlockedSet.contains(domain)) {
			return false;
		} else if(cachedAllowedSet.contains(domain)) {
			return true;
		}
		//2. 判断是否在私网网段中，不为私网网段做代理
		if(isInnerIP(domain)) {
			cachedBlockedSet.add(domain);
			return true;
		}
		//3. 首先判断是否在阻断表中，以小数点隔开
		if(blockSet.contains(domain)) {
			cachedBlockedSet.add(domain);
			return false;
		}
		int pos = 0;
		String subStr = domain;
		while(true) {
			pos = subStr.indexOf(".") + 1;
			if(pos == 0) {
				break;
			}
			subStr = subStr.substring(pos);
			if(blockSet.contains(subStr)) {
				cachedBlockedSet.add(domain);
				return false;
			}
		}
		//4. 然后判断是否在运行表中，以小数点隔开
		if(allowSet.contains(domain)) {
			cachedAllowedSet.add(domain);
			return true;
		}
		pos = 0;
		subStr = domain;
		while(true) {
			pos = subStr.indexOf(".") + 1;
			if(pos == 0) {
				break;
			}
			subStr = subStr.substring(pos);
			if(allowSet.contains(subStr)) {
				cachedAllowedSet.add(domain);
				return true;
			}
		}
		//5. 正则表的逐项正则匹配
		for(String pattern : regAllowList) {
			if(Pattern.matches(pattern, domain)){
				cachedAllowedSet.add(domain);
				return true;
			}
		}
		cachedBlockedSet.add(domain);
		return false;
	}
	

	public boolean isInnerIP(String ipAddress) {
		if(!isIpv4(ipAddress)) {
			return false;
		} else {
			long ipNum = ipToLong(ipAddress);
			//10.0.0.0 -- 10.255.255.255
			if(ipNum >= 167772160L && ipNum <= 184549375L) {
				return true;
			//172.16.0.0 -- 172.31.255.255
			} else if(ipNum >= 2886729728L && ipNum <= 2887778303L) {
				return true;
			//192.168.0.0 -- 192.168.255.255
			} else if(ipNum >= 3232235520L && ipNum <= 3232301055L) {
				return true;
			}
			return false;
		}
	}
	
	/**
	 * 判断是否是IPv4地址
	 */
	private boolean isIpv4(String ipAddress) {
		if(ipAddress == null) {
			return false;
		}
        String ip = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
        Pattern pattern = Pattern.compile(ip);
        Matcher matcher = pattern.matcher(ipAddress);
        return matcher.matches();
    }
	
	/**
	 * IP地址转换为整形
	 */
	private static long ipToLong(String ipAddress) {   
		String [] ip = ipAddress.split("\\.");   
		long a = Integer.parseInt(ip[0]);   
		long b = Integer.parseInt(ip[1]);   
		long c = Integer.parseInt(ip[2]);   
		long d = Integer.parseInt(ip[3]);   
		return a * 256 * 256 * 256 + b * 256 * 256 + c * 256 + d;   
	}
	
	public static synchronized Socks5Pac getSocks5Pac() {
		if(socks5Pac == null) {
			socks5Pac = new Socks5Pac();
			socks5Pac.loadPacList();
		}
		return socks5Pac;
	}

}
