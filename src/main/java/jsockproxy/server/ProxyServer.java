package jsockproxy.server;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jsockproxy.core.tunnel.TunnelClientSocket;
import jsockproxy.core.tunnel.TunnelServer;
import jsockproxy.core.tunnel.TunnelServerSocket;

/**
 * ------------------------------------------->>>
协议约定：
String controlConnect --->>> {\"msgType\":\"cotrolConnect\",\"token\":\"123456\",\"role\":\"control\",\"groupName\":\"stcp\",\"remoteFrontPort\":\"13306\"} 
String controlHeart   --->>> {\"msgType\":\"controlHeart\"} 
String visitorCliConnect --->>> {\"msgType\":\"visitorCliConnect\",\"token\":\"123456\",\"role\":\"visitorCli\",\"groupName\":\"stcp\",\"globalTraceId\":\"\"} 
String visitorCliConnectResp --->>> {\"msgType\":\"visitorCliConnectResp\"} 
String dataBindReq --->>> {\"msgType\":\"dataBindReq\",\"groupName\":\"stcp\",\"globalTraceId\":\"\"} 
String controlCliConnect --->>> {\"msgType\":\"controlCliConnect\",\"token\":\"123456\",\"role\":\"controlCli\",\"groupName\":\"stcp\",\"globalTraceId\":\"\"} 
 * @author liujh
 *
 */
public class ProxyServer {
	
	private final static Logger logger = LoggerFactory.getLogger(ProxyServer.class);
	private int serverPort;
	private int soTimeOut = 120000;//2分钟超时
	private Map<String, TunnelClientSocket> groupNames = new ConcurrentHashMap<String, TunnelClientSocket>();
	private Map<String, TunnelServer> frontServers = new ConcurrentHashMap<String, TunnelServer>();
	private Map<String, TunnelServerSocket> frontSocketThreads = new ConcurrentHashMap<String, TunnelServerSocket>();
	
	public Map<String, TunnelServerSocket> getFrontSocketThreads() {
		return frontSocketThreads;
	}

	public Map<String, TunnelServer> getFrontServers() {
		return frontServers;
	}

	public Map<String, TunnelClientSocket> getGroupNames() {
		return groupNames;
	}

	public ProxyServer(int serverPort){

		this.serverPort = serverPort;
		
	}
	
	public void startServer(){

		ServerSocket serverSocket = null;
		try {
			if(ProxyServerApp.ssl == false) {
				serverSocket = new ServerSocket(serverPort);
			}else {
				
				SSLContext ctx = SSLContext.getInstance("SSL");
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		        KeyStore ks = KeyStore.getInstance("JKS");
		        KeyStore tks = KeyStore.getInstance("JKS");
		        
		        InputStream kserverIn = ProxyServer.class.getClassLoader().getResourceAsStream("cert/kserver.ks");
		        InputStream tserverIn = ProxyServer.class.getClassLoader().getResourceAsStream("cert/tserver.ks");
		        ks.load(kserverIn, "sendiserverpass".toCharArray());
		        tks.load(tserverIn, "sendiserverpublicpass".toCharArray());
		        kserverIn.close();
		        tserverIn.close();
		        
		        kmf.init(ks, "sendiserverpass".toCharArray());
		        tmf.init(tks);
		        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		        serverSocket = (SSLServerSocket) ctx.getServerSocketFactory().createServerSocket(serverPort);
		        ((SSLServerSocket)serverSocket).setNeedClientAuth(true);
			}
			
			logger.info("proxyServer started , listen on " +serverSocket.getInetAddress().getHostAddress()+":"+ serverPort );

			// 一直监听，接收到新连接，则开启新线程去处理
			while (true) {
				Socket clientSocket = serverSocket.accept();
				clientSocket.setSoTimeout(soTimeOut);
				new TunnelClientSocket(this,clientSocket).start();
			}
		} catch (Exception e) {
			logger.error("",e);
		}
	}

}
