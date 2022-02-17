package jsockproxy.client;

import java.util.Arrays;
import java.util.List;

public class ProxyClientApp {

	public static boolean ssl = true; // 是否开启ssl
	public static String token = "test123456";  // token，服务端与客户端需要匹配才行
	public static String trunnelHost = "127.0.0.1"; // 服务端IP
	public static int trunnelPort = 443; // 服务端端口
	public static List<String> groups = Arrays.asList("test");  // 代理组名，可忽视
	public static List<String> serverFrontPorts = Arrays.asList("65080"); // socks代理端口
	public static boolean proxyLogin = true;  // 登陆配置
	public static String proxyUsername = "test"; // 代理用户名
	public static String proxyPassword = "test1234"; // 代理密码


	public static void main(String[] args) {

		for (int i = 0; i < args.length; i++) {
			if ("-trunnelHost".equals(args[i])) {
				trunnelHost = args[i + 1];
				i++;
			} else if ("-trunnelPort".equals(args[i])) {
				trunnelPort = Integer.parseInt(args[i + 1]);
				i++;
			} else if ("-token".equals(args[i])) {
				token = args[i + 1];
				i++;
			} else if ("-groups".equals(args[i])) {
				groups = Arrays.asList(args[i + 1].split(","));
				i++;
			}else if ("-serverFrontPorts".equals(args[i])) {
				serverFrontPorts = Arrays.asList(args[i + 1].split(","));
				i++;
			} else if ("-proxyUsername".equals(args[i])) {
				proxyUsername = args[i + 1];
				i++;
			} else if ("-proxyPassword".equals(args[i])) {
				proxyPassword = args[i + 1];
				i++;
			} else if ("-ssl".equals(args[i])) {
		    	ssl = Boolean.parseBoolean(args[i+1]);
		    	i++;
		     }
		}

		for (int i = 0; i < groups.size(); i++) {
			String groupName = groups.get(i);
			int serverFrontPort = Integer.parseInt(serverFrontPorts.get(i));
			new ProxyClient(groupName,serverFrontPort).start();
		}

	}

}
