package jsockproxy.server;


public class ProxyServerApp {

	public static int serverPort = 443;  // 服务端端口
	public static String token = "test123456";  // token，服务端与客户端需要匹配才行
	public static boolean ssl = true;  // 是否开启ssl
	
	public static void main(String[] args) {
		
		for(int i=0;i<args.length;i++) {
	      if ("-serverPort".equals(args[i])) {
	    	  serverPort = Integer.parseInt(args[i+1]);
	    	  i++;
	      }else if ("-token".equals(args[i])) {
	    	  token = args[i+1];
	    	  i++;
	      }else if ("-ssl".equals(args[i])) {
	    	  ssl = Boolean.parseBoolean(args[i+1]);
	    	  i++;
	      }
	    }
		
		ProxyServer proxyServer = new ProxyServer(serverPort);
		proxyServer.startServer();
	}

}
