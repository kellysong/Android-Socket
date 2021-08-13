# Android-Socket

Android 端Java Socket长连接示例，当然Java端也是支持,IDEA新建Java工程并引入module-base、module-business、module-demo、module-socket

# 应用场景

1. 一键推送apk到各Android终端设备，进行批量静默安装（适用root设备）
2. IM聊天
3. 作为Android端或Java端测试工具使用

等等...



# 测试步骤

Android工程：



- 手机运行app作为服务端，使用module-demo的客户端作为连接（依赖局域网）

- 直接使用module-demo的客户端和服务端测试（PC本地测试）


Java工程：

1. IDEA新建Java工程并引入module-base、module-business、module-demo、module-socket

2. 直接使用module-demo的客户端和服务端测试（PC本地测试或多个PC联调（其中一个作为服务端，其它作为客户端））

