package cc.litstar.util;

import java.io.IOException;  
import java.net.InetAddress;  
import java.net.Socket;  
import java.net.UnknownHostException;  

/**
 * @author hehaoxing
 * 工具类，存放静态方法
 */
public class NetworkUtil {
	/*** 
     *  true:already in using  false:not using  
     * @param port 
     */  
    public static boolean isLoclePortUsing(int port){  
        boolean flag = true;  
        try {  
            flag = isPortUsing("127.0.0.1", port);  
        } catch (Exception e) {  
        }  
        return flag;  
    }  
    /*** 
     *  true:already in using  false:not using  
     * @param host 
     * @param port 
     * @throws UnknownHostException  
     */  
    public static boolean isPortUsing(String host,int port) throws UnknownHostException{  
        boolean flag = false;  
        InetAddress theAddress = InetAddress.getByName(host);  
        try {  
            Socket socket = new Socket(theAddress,port);  
            flag = true;  
        } catch (IOException e) {  
              
        }  
        return flag;  
    }  
    
    public static void main(String[] args) {
    	System.out.println(NetworkUtil.isLoclePortUsing(1080));
    }
}
