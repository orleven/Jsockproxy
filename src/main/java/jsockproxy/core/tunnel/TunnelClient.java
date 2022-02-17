package jsockproxy.core.tunnel;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.Arrays;
import javax.net.ssl.*;

import jsockproxy.client.ProxyClient;
import jsockproxy.client.ProxyClientApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jsockproxy.utils.MessageUtil;
import jsockproxy.utils.MyException;

import com.alibaba.fastjson.JSONObject;

public class TunnelClient extends Thread{
	
	private final static String characterSet = "UTF-8";
	private int bufferSize = 8092;
	private final static Logger logger = LoggerFactory.getLogger(TunnelClient.class);
	private int soTimeOut = 120000;//2分钟超时
	private String groupName;//分组名称
	private String globalTraceId;
	
	private String token = ProxyClientApp.token;
	private String role = "controlCli";
	private String trunnelHost = ProxyClientApp.trunnelHost;
	private int trunnelPort = ProxyClientApp.trunnelPort;
	
	private String remoteHost = null;
	private int remotePort = 0;
	
	private boolean socksNeedLogin = ProxyClientApp.proxyLogin;
	private String proxyUsername = ProxyClientApp.proxyUsername;
	private String proxyPassword = ProxyClientApp.proxyPassword;

	private Socket controlCliSocket;
	private Socket remoteSocket; //远程真实要访问的服务
	private InputStream rin ;
	private DataOutputStream dout = null;
	
	public TunnelClient(String remoteHost, int remotePort, JSONObject dataStr){
		this.groupName = dataStr.getString("groupName");
		this.globalTraceId = dataStr.getString("globalTraceId"); 
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
	}

	public void run() {
		
		InputStream in = null;
		OutputStream out = null;
		DataInputStream din = null;
		
		try {
			if(ProxyClientApp.ssl == false) {
				controlCliSocket = new Socket();
				controlCliSocket.setSoTimeout(soTimeOut);//5分钟未收到数据，自动关闭资源
				controlCliSocket.connect(new InetSocketAddress(trunnelHost, trunnelPort));
			}else {
				SSLContext ctx = SSLContext.getInstance("SSL");
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		        KeyStore ks = KeyStore.getInstance("JKS");
		        KeyStore tks = KeyStore.getInstance("JKS");
		        
		        InputStream kclientIn = ProxyClient.class.getClassLoader().getResourceAsStream("cert/kclient.ks");
		        InputStream tclientIn = ProxyClient.class.getClassLoader().getResourceAsStream("cert/tclient.ks");
		        ks.load(kclientIn, "sendiclientpass".toCharArray());
		        tks.load(tclientIn, "sendiclientpublicpass".toCharArray());
		        kclientIn.close();
		        tclientIn.close();
		        
		        kmf.init(ks, "sendiclientpass".toCharArray());
		        tmf.init(tks);
		        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		        controlCliSocket = (SSLSocket) ctx.getSocketFactory().createSocket(trunnelHost, trunnelPort);
		        controlCliSocket.setSoTimeout(soTimeOut);//5分钟未收到数据，自动关闭资源
				
			}
			
			logger.info(groupName + " controlCliSocket>>>> " + controlCliSocket + "  connected" );
			
			in = controlCliSocket.getInputStream();
			out = controlCliSocket.getOutputStream();
			din = new DataInputStream(in);
			dout = new DataOutputStream(out);
			
			//发送建立绑定请求
			//进行第一个数据包下发，告诉服务器当前是什么角色,
			dout.write(MessageUtil.bytessTart);
			String headStr = "{\"msgType\":\"controlCliConnect\",\"token\":\""+token+"\",\"role\":\""+role+"\",\"groupName\":\""+groupName+"\",\"globalTraceId\":\""+globalTraceId+"\"} ";
			dout.writeInt(headStr.getBytes(characterSet).length);
			dout.write(headStr.getBytes(characterSet));



			byte[] tmp = new byte[1];
			in.read(tmp);
			byte protocol = tmp[0];//1.获取协议头，如果是0x05表示是socks5

			if ((0x05 == protocol)) {// 如果开启代理5，并以socks5协议请求

				tmp = new byte[2]; //获取可供选择的方法，及选择的方法
				in.read(tmp);

				boolean isLogin = false;

				byte method = tmp[0];
				if (0x02 == tmp[0]) {
					method = 0x00;
					in.read();
				}else {
					method = 0x00;
				}

				if (socksNeedLogin) {
					method = 0x02;
				}

				//2.请求认证返回
				tmp = new byte[] { 0x05, method };
				out.write(tmp);

				//3.处理登录,这里直接返回成功
				if (0x02 == method) {// 处理登录.

					//获取用户名
					int b = in.read();
					String user = null;
					String pwd = null;
					if(0x01 == b) {
						b = in.read();//读取用户名长度
						tmp = new byte[b];
						in.read(tmp);//读取用户名
						user = new String(tmp);

						b = in.read();//读取密码长度
						tmp = new byte[b];
						in.read(tmp);//读取密码
						pwd = new String(tmp);

						if (null != user && user.trim().equals(proxyUsername) && null != pwd && pwd.trim().equals(proxyPassword)) {// 权限过滤
							isLogin = true;
							tmp = new byte[] { 0x05, 0x00 };// 登录成功
							out.write(tmp);
							logger.info("{} login success !", user);
						} else {
							new MyException(user + " login faild !");
						}

					}

				}
				//4.客户端 -> 代理服务器，发送目标信息
				//版本号(1字节)	命令(1字节)	保留(1字节)	请求类型(1字节)	地址(不定长)	端口(2字节)
				if(!socksNeedLogin || isLogin) {

					tmp = new byte[4];
					in.read(tmp);
					logger.info("proxy header >>  {}", Arrays.toString(tmp));

					//int cmd = tmp[1];
					remoteHost = getHost(tmp[3], in);//获程远程主机ip

					tmp = new byte[2];//获程远程主机端口
					in.read(tmp);
					remotePort = ByteBuffer.wrap(tmp).asShortBuffer().get() & 0xFFFF;

					tmp = new byte[] { 0x05, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };// 登录成功,不告客户端底层ip和port的写法
					out.write(tmp);

				}

			}else {
				new MyException("not socks proxy : openSock5[]");
			}

			if (!remoteHost.isEmpty() & remotePort != 0){

				//建立与远程真实socket的连接
				remoteSocket = new Socket(remoteHost, remotePort);
				//设置超时，超过时间未收到客户端请求，关闭资源
				remoteSocket.setSoTimeout(soTimeOut);

				logger.info(groupName + " remoteSocket>>>> " + remoteSocket + "  connected" );

				rin = remoteSocket.getInputStream();
				OutputStream rout = remoteSocket.getOutputStream();

				new ReadThread().start();

				//写数据,负责读取客户端发送过来的数据，转发给远程
				byte[] data = new byte[bufferSize];
				int len = 0;
				while((len = din.read(data)) > 0){

					if(len == bufferSize) {//读到了缓存大小一致的数据，不需要拷贝，直接使用
						rout.write(data);
					}else {//读到了比缓存大小的数据，需要拷贝到新数组然后再使用
						byte[] dest = new byte[len];
						System.arraycopy(data, 0, dest, 0, len);
						rout.write(dest);
					}

				}
			}


		} catch (SocketException | SSLException e) {
			if (!e.toString().contains("Socket closed")){
				logger.error("",e);
			}
		} catch (Exception e) {
			logger.error("",e);
		}finally {
			close();			
		}
		
		
	}
	
	/**
     * 获取目标的服务器地址
     */
    private String getHost(byte type, InputStream in) throws IOException {
        String host = null;
        byte[] tmp = null;
        switch (type) {
        case 0x01:// IPV4协议
            tmp = new byte[4];
            in.read(tmp);
            host = InetAddress.getByAddress(tmp).getHostAddress();
            break;
        case 0x03:// 使用域名
            int l = in.read();
            tmp = new byte[l];
            in.read(tmp);
            host = new String(tmp);
            break;
        case 0x04:// 使用IPV6
            tmp = new byte[16];
            in.read(tmp);
            host = InetAddress.getByAddress(tmp).getHostAddress();
            break;
        default:
            break;
        }
        return host;
    }
	
	public void close(){
		
		if(controlCliSocket !=null){
			try {
				controlCliSocket.close();
				logger.info(groupName + " controlCliSocket  >>>> " + controlCliSocket +" socket closed ");
			} catch (Exception e1) {
				logger.error("",e1);
			} finally {
				controlCliSocket = null;
			}
		}
		
		if(remoteSocket !=null){
			try {
				remoteSocket.close();
				logger.info(groupName + " remoteSocket >>>> " + remoteSocket +" socket closed ");
			} catch (Exception e1) {
				logger.error("",e1);
			} finally {
				remoteSocket = null;
			}
			
		}
		
	}
	
	//读数据线程负责读取远程数据后回写到客户端
	class ReadThread extends Thread {
		
		@Override
		public void run() {
			try {
				byte[] data = new byte[bufferSize];
				int len = 0;
				while((len = rin.read(data)) > 0){
					if(len == bufferSize) {//读到了缓存大小一致的数据，不需要拷贝，直接使用
						dout.write(data);
					}else {//读到了比缓存大小的数据，需要拷贝到新数组然后再使用
						byte[] dest = new byte[len];
						System.arraycopy(data, 0, dest, 0, len);
						dout.write(dest);
					}
				}
			} catch (SocketException | SSLException e) {
				if (!e.toString().contains("Socket closed")){
					logger.error("",e);
				}
			} catch (Exception e) {
				logger.error("",e);
			} finally {
				close();
			}
		}
	}
}
