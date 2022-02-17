package jsockproxy.core.handler;

import java.io.DataInputStream;
import java.util.Map;
import com.alibaba.fastjson.JSONObject;

import jsockproxy.core.tunnel.TunnelClientSocket;
import jsockproxy.core.tunnel.TunnelServerSocket;
import jsockproxy.utils.MyException;

public class ClientiHandler {
	
	private int bufferSize = 8092;
	private TunnelServerSocket tunnelServerSocket;
	private TunnelClientSocket tunnelClientSocket;
	private JSONObject headStr;
	
	public ClientiHandler(TunnelClientSocket tunnelClientSocket, JSONObject headStr){
		this.tunnelClientSocket = tunnelClientSocket;
		this.headStr = headStr;
	}
	
	public void handler() throws Exception{
		try {
			Map<String, TunnelServerSocket> frontSocketThreads = tunnelClientSocket.getStcpServer().getFrontSocketThreads();
			String globalTraceId = headStr.getString("globalTraceId"); 
			TunnelServerSocket tunnelServerSocket = frontSocketThreads.get(globalTraceId);
			
			//frontSocketThread没有连接
			if(tunnelServerSocket == null) {
				throw new MyException("no tunnelSocketThread connected, globalTraceId: " + globalTraceId);
			}
			
			//绑定成功
			this.tunnelServerSocket = tunnelServerSocket;
			this.tunnelServerSocket.setControlCliHandler(this);
			
			//绑定成功后进行数据转发
			byte[] data = new byte[bufferSize];
			int len = 0;
			DataInputStream din = this.getClientSocketThread().getDin();
			while((len = din.read(data)) > 0){
				if(len == bufferSize) {//读到了缓存大小一致的数据，不需要拷贝，直接使用
					tunnelServerSocket.getDout().write(data);
					
				}else {//读到了比缓存大小的数据，需要拷贝到新数组然后再使用
					byte[] dest = new byte[len];
					System.arraycopy(data, 0, dest, 0, len);
					tunnelServerSocket.getDout().write(dest);
					
				}
			}
		} catch (Exception e) {
			throw e;
		}finally {
			if(tunnelServerSocket != null) {
				tunnelServerSocket.close();
			}
			tunnelServerSocket = null;
		}
		
	}
	
	public TunnelClientSocket getClientSocketThread() {
		return tunnelClientSocket;
	}

	public void setClientSocketThread(TunnelClientSocket tunnelClientSocket) {
		this.tunnelClientSocket = tunnelClientSocket;
	}

}
