Socks5-shadowsocks
================================

一个简单的ss客户端，参照了netty4.1官方example与相关博客，其中加解密参考了一些开源项目。其仅作学习之用。

##相关协议与技术

###1. Socks5协议

Socks5协议是一种网络代理协议，此通信协议已被标准化并收录于RFC1928。其通信流程如下：
1) 客户端连到服务器后，然后就发送请求来协商版本和认证方法：

```
		+----+----------+----------+ 
		|VER | NMETHODS | METHODS  | 
		+----+----------+----------+ 
		| 1　| 　　1　 　| 1 to 255 | 
		+----+----------+----------+
	VER（版本）在这个协议版本中被设置为X'05'。
	NMETHODS（方法）中包含在METHODS（方法）中出现的方法标识的数据（用字节表示）。
```

2) 服务器发送一个METHOD（方法）选择报文：

```		
		+----+--------+ 
		|VER | METHOD | 
		+----+--------+ 
		| 1　| 　1　　 | 
		+----+--------+
	常见的的METHOD的值有：
		X'00'    无验证需求
		X'02'    用户名/密码(USERNAME/PASSWORD) 
		X'FF'    无可接受方法(NO ACCEPTABLE METHODS)
		
```

3) 客户端向服务端发送详细协商请求信息：

```
		+----+-----+-------+------+----------+----------+ 
		|VER | CMD |　RSV　 | ATYP | DST.ADDR | DST.PORT | 
		+----+-----+-------+------+----------+----------+ 
		| 1　| 　1  | X'00' | 　1　| Variable |　　 2　　 | 
		+----+-----+-------+------+----------+----------+ 
	VER(Socks协议版本号)：X'05' 
　  CMD(请求指令) 
　 		o CONNECT X'01' 
　 		o BIND X'02' 
　 		o UDP ASSOCIATE X'03' 
　  ATYP(ADDR字段的类别，允许出现IPv4/v6和域名两种不同的地址标识信息)
　 		o IP V4 address: X'01' 
　 		o DOMAINNAME: X'03' 
　	    oIP V6 address: X'04' 
　  DST.ADDR(目标地址)
　  DST.PORT(目标端口)

```

4) 服务器向客户端回复信息：

```
		+----+-----+-------+------+----------+----------+
		|VER | REP |　RSV　 | ATYP | BND.ADDR | BND.PORT |
		+----+-----+-------+------+----------+----------+
		| 1　|　1　 | X'00' |　1 　 | Variable | 　　2　　 |
		+----+-----+-------+------+----------+----------+
	REP(回复信息，标明建连协商成功或建连发生错误):
		X'00' 建连成功
		其他均为失败信息。

```

###2. Shadowsocks

Shadowsocks是一个开源软件。其客户端与服务端之间通过相应协议进行通信，并支持了多种加密方式，且不同端口包含不同秘钥。其名称与socks5协议相似，但本质上的通信流程并不相同。
客户端向服务器端发送建连请求信息：
```
		+------+----------+----------+ 
		| ATYP | DST.ADDR | DST.PORT | 
		+------+----------+----------+ 
		| 　1 　| Variable |　　 2　　 | 
		+------+----------+----------+ 
	字段含义与Socks5相同。
	在实际的传输中，建连信息与其发送数据信息相同，都是加密传输的。
```
此后依照秘钥与加密算法对数据做加密，即实现了一种安全的代理模式。

###3. PAC

PAC(代理自动配置)。其规定了一个指向PAC文件的URL，这个文件中包括一个JavaScript函数来确定访问每个URL时所选用的合适代理。即可以针对所访问的URL，对流量进行分流。
在这里，提取协议交互流程中的目的地址与端口并做判断，可以实现类似PAC的功能。


##工作流程
	
&nbsp;&nbsp;此程序与shadowsocks服务器建连流程，与sslocal原理类似：
1. 浏览器插件(例如SwitchOmega)向sslocal发送请求并建立socks5连接。
2. 数据请求流程：
   浏览器 -> sslocal -> ssserver -> 目的网站
   其中ssslocal将浏览器传输的正常数据进行了加密，并发送到ssserver服务器。
3. 数据传回流程：
   目的网站 -> ssserver -> sslocal -> 浏览器
   其中sslocal将ssserver传来的加密数据进行解密，并发送到给浏览器。

这里借助Netty中Channel的变化，解释其执行流程：
1. Netty初始化，与socks客户端通信的Channel:
   SocksServerInitializer->SocksPortUnificationServerHandler->SocksServerHandler
   SocksPortUnificationServerHandler在收到Socks5初始化请求后，会移出此Channel并添加Socks5InitialRequestDecoder与Socks5ServerEncoder。
2. 收到Socks5客户端初始化信息:
   Socks5CommandRequestDecoder->Socks5InitialRequestDecoder->Socks5ServerEncoder->SocksServerHandler
   在Channel头部添加Socks5CommandRequestDecoder目的为对Connect建连消息解码。
   初始化完成后，Socks5InitialRequestDecoder变更为直通。
3. 收到Socks5客户端Connect建连信息：
   Socks5CommandRequestDecoder->Socks5InitialRequestDecoder->Socks5ServerEncoder->SocksServerConnectHandler
   Connect认证完成后，移除SocksServerHandler添加SocksServerConnectHandler，并调用fireChannelRead将建连请求传递到SocksServerConnectHandler中。
4. SocksServerConnectHandler收到消息后，取出目的地址决定是否使用远端ssserver服务器端代理。
5. 建立与远端服务器(不使用shadowsocks代理)或代理服务器(使用shadowsocks代理)的连接，借助DirectClientHandler实现回调。若建连成      功，则向客户端传回Socks5建连回复。若使用代理服务器，则需要向shadowsocks传送建连数据。
   在此过程中创建了到ssserver代理服务器或远端目的服务器的Channel。
6. 传回建连成功信息后，两个Channel的RelayHandler持有彼此的Channel:
   与ssserver服务器或者远端服务器的Channel:
   RelayHandler(持有与socks5客户端之间的Channel)
   与socks5客户端之间的Channel:
   Socks5CommandRequestDecoder->Socks5InitialRequestDecoder->Socks5ServerEncoder->RelayHandler(持有另一个Channel)
   在传递数据时，通过将消息写入彼此的Channel实现消息转发。若使用shadowsocks代理，则对数据加解密，否则直接传递。
   


