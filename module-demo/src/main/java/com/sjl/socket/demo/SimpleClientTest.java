package com.sjl.socket.demo;


import com.sjl.socket.SimpleClient;
import com.sjl.socket.base.ConsoleUtils;
import com.sjl.socket.base.DataPacket;
import com.sjl.socket.base.DataReq;
import com.sjl.socket.base.DataResponseListener;
import com.sjl.socket.business.FileInfo;
import com.sjl.socket.business.FileReq;
import com.sjl.socket.util.FileUtils;

import org.junit.Test;

import java.io.File;

/**
 * socket测试用例
 *
 * <p>一键推送apk到各Android终端设备，进行批量静默安装（适用root设备）</p>
 *
 * @author Kelly
 * @version 1.0.0
 * @filename SimpleClientTest
 * @time 2021/8/5 14:13
 * @copyright(C) 2021 song
 */
public class SimpleClientTest {
    public static SimpleClient apkClient;
    public static String ip;

    public static void main(String[] args) {
        connect(1);
        ConsoleUtils.i("========");


    }


    public static File getApkDir() {
        String path = SimpleClientTest.class.getResource(".").getPath();
        path = path.substring(0, path.indexOf("build"));
        File dir = new File(path);
        return dir;
    }

    @Test
    public void printDir() {
        //C:\Users\Administrator\Downloads\Android-Socket\module-demo
        File dir = getApkDir();
        ConsoleUtils.i(dir.getAbsolutePath());
        File file = new File(dir, "apk" + File.separator + "test.apk");
        ConsoleUtils.i(file.getAbsolutePath() + ":" + file.exists());
    }

    public static void connect(int way) {
        File dir = getApkDir();
        File file = new File(dir, "apk" + File.separator + "test.apk");
        if (file.isFile()) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.msg = "纯文本信息";
            //发送文件
/*            fileInfo.msg = "纯文本信息";
            DataReq dataReq = new TextReq(fileInfo);*/
            //发送文件
            fileInfo.msg = "文本和文件信息";
            DataReq dataReq = new FileReq(fileInfo, file, "my_test.apk");
            //需要发送多台设备,拷贝多次connectWay
            connectWay(way, dataReq);


        }
    }

    private static void connectWay(int way, DataReq dataReq) {
        switch (way) {
            case 1:
                //测试方法1：局域网或广域网（外网）测试
                ip = "192.168.56.118";
                sendData(ip, 8090, dataReq);
                break;
            case 2:
                /***
                 *
                 * 测试方法2：pc作为客户端，Android模拟器作为服务端
                 *把PC电脑端TCP端口12580的数据转发到与电脑通过adb连接的Android设备的TCP端口8090上。
                 *
                 * 测试之前需要执行：adb forward tcp:12580 tcp:8090
                 *
                 */
                ip = "127.0.0.1";
                sendData("127.0.0.1", 12580, dataReq);
                break;
            case 3://测试方法3：PC本地测试
                ip = "127.0.0.1";
                sendData("127.0.0.1", 8090, dataReq);
                break;
            default:
                break;
        }
    }


    private static void sendData(String ip, int port, final DataReq dataReq) {
        apkClient = new SimpleClient(ip, port);
        apkClient.setClientListener(new SimpleClient.ClientListener() {
            @Override
            public void onStarted() {
//                sendDataWithThread(dataReq);
                sendData(dataReq);
            }

            @Override
            public void onStopped() {

            }

            @Override
            public void onException(Exception e) {

            }
        });
     /*   apkClient.setHeartbeatListener(new HeartbeatListener() {
            @Override
            public void heartBeatPacket(DataPacket requestPacket, DataPacket responsePacket, String source) {
                ConsoleUtils.i("心跳响应：" + source);
            }
        });*/
        apkClient.setFileSaveDir(FileUtils.currentWorkDir + "apk");
        //订阅服务端的请求信息
        apkClient.subscribeDataResponseListener(new DataResponseListener() {
            @Override
            public void sendOnSuccess(int cmd, DataPacket requestPacket, DataPacket responsePacket) {
                ConsoleUtils.i("客服端监听服务端,cmd: " + cmd + " ,requestPacket: " + requestPacket + " responsePacket: " + responsePacket);
                if (msgCallback != null) {
                    msgCallback.result(requestPacket.toString());
                }
            }

            @Override
            public void sendOnError(int cmd, Throwable t) {
                ConsoleUtils.e("接收服务端信息异常:" + cmd, t);
            }

        });
        apkClient.connect();
    }


    public static void disconnect() {
        if (apkClient == null) {
            return;
        }
        apkClient.disconnect();
    }

    public static void sendData(DataReq dataReq) {
        if (apkClient == null) {
            return;
        }
        apkClient.sendData(dataReq, new DataResponseListener() {
            @Override
            public void sendOnSuccess(int cmd, DataPacket requestPacket, DataPacket responsePacket) {
                ConsoleUtils.i("发送消息成功,cmd: " + cmd + " ,requestPacket: " + requestPacket + " responsePacket: " + responsePacket);
                if (msgCallback != null) {
                    msgCallback.result(requestPacket.toString());
                }
            }

            @Override
            public void sendOnError(int cmd, Throwable t) {
                if (msgCallback != null) {
                    msgCallback.result(t.getMessage());
                }
            }
        });
    }

    @Deprecated
    public static void sendDataWithThread(final DataReq dataReq) {
        if (apkClient == null) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    apkClient.sendData(dataReq);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (msgCallback != null) {
                        msgCallback.result(e.getMessage());
                    }

                }
            }
        }).start();
    }

    static MsgCallback msgCallback;

    public static void setMsgCallback(MsgCallback msgCallback) {
        SimpleClientTest.msgCallback = msgCallback;
    }

    public interface MsgCallback {
        void result(String msg);
    }
}
