package jsockproxy.core.tunnel;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import jsockproxy.core.handler.ClientiHandler;
import jsockproxy.core.handler.ServerHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSONObject;
import javax.net.ssl.SSLException;

import jsockproxy.server.ProxyServer;
import jsockproxy.server.ProxyServerApp;
import jsockproxy.utils.JsonUtil;
import jsockproxy.utils.MyException;

public class TunnelClientSocket extends Thread{
	
	private final static Logger logger = LoggerFactory.getLogger(TunnelClientSocket.class);
	private final static String characterSet = "UTF-8";
	private static String token = ProxyServerApp.token;
	private ProxyServer stcpServer;
	private Socket clientSocket;
	private InputStream in;
	private OutputStream out;
	private DataInputStream din;
	private DataOutputStream dout;

	private String groupName;
	private String role;
	
	public TunnelClientSocket(ProxyServer stcpServer, Socket cliSocket){
		this.stcpServer = stcpServer;
		this.clientSocket = cliSocket;
	}
	
	public DataInputStream getDin() {
		return din;
	}
	
	public DataOutputStream getDout() {
		return dout;
	}
	
	public void run() {
		
		try {
			in = clientSocket.getInputStream();
			out = clientSocket.getOutputStream();
			din = new DataInputStream(in);
			dout = new DataOutputStream(out);
			
			//解析头部信息
			JSONObject headStr = readDataStr(din);
			String msgType = headStr.getString("msgType");//消息类型，controlConnect
			String tokenStr = headStr.getString("token");//gzsendi
			String groupName = headStr.getString("groupName");//stcp
			String role = headStr.getString("role"); //control,visitor,controlCli,visitorCli
			if(StringUtils.isEmpty(msgType)) {
				throw new MyException("msgType is null.");
			}
			if(StringUtils.isEmpty(tokenStr)) {
				throw new MyException("token is null.");
			}
			if(StringUtils.isEmpty(groupName)) {
				throw new MyException("groupName is null.");
			}
			if(StringUtils.isEmpty(role)) {
				throw new MyException("role is null.");
			}
			if(!tokenStr.equals(token)) {
				throw new MyException("token is error..");
			}
			this.groupName = groupName;
			this.role = role;
			
			if("control".equals(role)){
				new ServerHandler(this,headStr).handler();
			}else if("controlCli".equals(role)){
				new ClientiHandler(this,headStr).handler();
			}else {
				throw new MyException("unkown role..");
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
	
	public void close(){
		
		try {
			
			if(clientSocket !=null && !clientSocket.isClosed()) {
				clientSocket.close();
				logger.info(groupName + " -> clientSocket  >>>> " + clientSocket +" socket closed ");
			}
			
		} catch (Exception e1) {
			logger.error("",e1);
		} finally {
			clientSocket = null;
		}
		
	}
	
	//读取socket消息头 , 0x070x07+字符串长度+Json字符串
	private JSONObject readDataStr(DataInputStream dis) throws IOException{
		
		byte firstByte = dis.readByte();
		byte secondByte = dis.readByte();
		if( firstByte != 0x07 && secondByte != 0x07){
			throw new MyException("unkown clientSocket.");
		}
		
		int resultlength = dis.readInt();
		byte[] datas = new byte[resultlength];
		int totalReadedSize = 0;
		while(totalReadedSize < resultlength) {
			int readedSize = dis.read(datas,totalReadedSize,resultlength-totalReadedSize);
			totalReadedSize += readedSize;
		}
		String headStr = new String(datas,characterSet);
		
		return JsonUtil.fromJson(headStr);
	}
	
	public ProxyServer getStcpServer() {
		return stcpServer;
	}
	
	public String getGroupName() {
		return groupName;
	}

	public String getRole() {
		return role;
	}
	
	public Socket getClientSocket() {
		return clientSocket;
	}

}
