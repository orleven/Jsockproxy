package jsockproxy.core.tunnel;

import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jsockproxy.server.ProxyServer;

public class TunnelServer extends Thread{
	
	private final static Logger logger = LoggerFactory.getLogger(TunnelServer.class);
	
	private int frontPort;
	private String groupName;//分组名称
	private int soTimeOut = 120000;//2分钟超时
	
	private ProxyServer stcpServer;
	ServerSocket serverSocket = null;
	
	public TunnelServer(ProxyServer stcpServer, int frontPort, String groupName){
		this.stcpServer = stcpServer;
		this.frontPort = frontPort;
		this.groupName = groupName;
		
	}
	
	public void run() {
		
		try {
			serverSocket = new ServerSocket(frontPort);
			
			logger.info(groupName + " -> frontServer started , listen on " +serverSocket.getInetAddress().getHostAddress()+":"+ frontPort );

			// 一直监听，接收到新连接，则开启新线程去处理
			while (true) {
				Socket visitroCliSocket = serverSocket.accept();
				visitroCliSocket.setSoTimeout(soTimeOut);
				new TunnelServerSocket(this,visitroCliSocket,groupName).start();
			}

		} catch (Exception e) {
			logger.error("",e);
		}
		
	}
	
	public void close(){
		
		try {
			
			if(serverSocket !=null && !serverSocket.isClosed()) {
				serverSocket.close();
				logger.info(groupName + " -> frontServer  >>>> " + serverSocket +" closed ");
			}
			
		} catch (Exception e1) {
			logger.error("",e1);
			
		} finally {
			serverSocket = null;
		}
		
	}
	
	public ProxyServer getStcpServer() {
		return stcpServer;
	}
	
	public String getGroupName() {
		return groupName;
	}

	public int getFrontPort() {
		return frontPort;
	}

}
