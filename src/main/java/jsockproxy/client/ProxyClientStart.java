package jsockproxy.client;

import java.util.List;

public class ProxyClientStart {

	public static boolean ssl = ProxyClientApp.ssl;
	public static String token = ProxyClientApp.token;
	public static String trunnelHost = ProxyClientApp.trunnelHost;
	public static int trunnelPort = ProxyClientApp.trunnelPort;
	public static List<String> groups = ProxyClientApp.groups;
	public static List<String> serverFrontPorts = ProxyClientApp.serverFrontPorts;
	public static boolean proxyLogin = ProxyClientApp.proxyLogin;
	public static String proxyUsername = ProxyClientApp.proxyUsername;
	public static String proxyPassword = ProxyClientApp.proxyPassword;

	{
		for (int i = 0; i < groups.size(); i++) {
			String groupName = groups.get(i);
			int serverFrontPort = Integer.parseInt(serverFrontPorts.get(i));
			new ProxyClient(groupName,serverFrontPort).start();
		}
	}

}
