package jsockproxy.server;


public class ProxyServerStart {
	
	public static int serverPort = ProxyServerApp.serverPort;
	public static String token = ProxyServerApp.token;
	public static boolean ssl = ProxyServerApp.ssl;
	
	{
		ProxyServer proxyServer = new ProxyServer(serverPort);
		proxyServer.startServer();
	}

}
