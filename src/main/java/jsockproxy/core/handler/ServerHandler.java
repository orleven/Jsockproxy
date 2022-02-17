package jsockproxy.core.handler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

import jsockproxy.core.tunnel.TunnelClientSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSONObject;

import jsockproxy.utils.JsonUtil;
import jsockproxy.utils.MessageUtil;
import jsockproxy.core.tunnel.TunnelServer;
import jsockproxy.utils.MyException;


public class ServerHandler {
	
	private final static Logger logger = LoggerFactory.getLogger(ServerHandler.class);
	
	private TunnelClientSocket tunnelClientSocket;
	private JSONObject headStr;
	private final static String characterSet = "UTF-8";
	
	public ServerHandler(TunnelClientSocket tunnelClientSocket, JSONObject headStr) {
		this.tunnelClientSocket = tunnelClientSocket;
		this.headStr = headStr;
	}
	
	public void handler() throws IOException{
		
		Map<String, TunnelClientSocket> groupNames = tunnelClientSocket.getStcpServer().getGroupNames();
		Map<String, TunnelServer> frontServers = tunnelClientSocket.getStcpServer().getFrontServers();
		String groupName = tunnelClientSocket.getGroupName();
		
		if(groupNames.containsKey(groupName)) {
			throw new MyException("duplicate control, groupName: " + groupName);
		}

		if(frontServers.containsKey(groupName)) {
			frontServers.get(groupName).close();
		}
		int frontPort = Integer.parseInt(headStr.getString("serverFrontPort"));//本地启动端口
		TunnelServer tunnelServer = new TunnelServer(tunnelClientSocket.getStcpServer(),frontPort, tunnelClientSocket.getGroupName());
		tunnelServer.start();

		try {
			//放入内存
			groupNames.put(groupName, tunnelClientSocket);
			frontServers.put(groupName, tunnelServer);
			
			while(true){
				JSONObject dataStr = readDataStr(tunnelClientSocket.getDin());
				String msgType = dataStr.getString("msgType");//消息类型，controlConnect,controlHeart,visitorConnect,visitorHeart,dataBindReq
				
				//心跳回应
				if("controlHeart".equals(msgType)) {
					logger.info(groupName + " -> "  + tunnelClientSocket + " receive heart pkg request.");
					DataOutputStream dout = tunnelClientSocket.getDout();
					dout.write(MessageUtil.bytessTart);
					String heartStr = "{\"msgType\":\"controlHeart\"}";
					dout.writeInt(heartStr.getBytes(characterSet).length);
					dout.write(heartStr.getBytes(characterSet));
					
				}else {
					throw new MyException("unknown msgType.");
				}
			}
			
		} catch (Exception e) {
			throw e;
		}finally {
			groupNames.remove(groupName);
			if(frontServers.containsKey(groupName)) {
				frontServers.get(groupName).close();
			}
			frontServers.remove(groupName);
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

}
