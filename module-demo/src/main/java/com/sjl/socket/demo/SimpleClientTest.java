package com.sjl.socket.demo;


import com.sjl.socket.SimpleClient;
import com.sjl.socket.base.DataPacket;
import com.sjl.socket.base.DataReq;
import com.sjl.socket.base.DataResponseListener;
import com.sjl.socket.business.FileInfo;
import com.sjl.socket.business.TextReq;
import com.sjl.socket.util.FileUtils;

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
    /*  public static void main(String[] args) {
          connect();
      }*/
    static SimpleClient apkClient;

    public static void connect() {
        String apk_55 = FileUtils.currentWorkDir + "apk" + File.separator + "55_BeeBoxCabinetScenic_dev_release_v1.55.7_2021-07-29_16-24.apk";
        File file = new File(apk_55);
        if (file.isFile()) {
            DataReq dataReq = new TextReq(new FileInfo());
//            DataReq dataReq = new FileReq(new FileInfo(), file);//需要发送多台设备,拷贝多次

//            sendData("192.168.56.118", 8090, dataReq);


            /***
             *把PC电脑端TCP端口12580的数据转发到与电脑通过adb连接的Android设备的TCP端口8090上。
             *
             * adb forward tcp:12580 tcp:8090
             * pc作为客户端，Android模拟器作为服务端
             */
//            sendData("127.0.0.1", 12580, dataReq);


            sendData("127.0.0.1", 8090, dataReq);
        }
    }


    private static void sendData(String ip, int port, DataReq dataReq) {
        apkClient = new SimpleClient(ip, port);
        apkClient.setClientListener(new SimpleClient.ClientListener() {
            @Override
            public void onStarted() {
                sendDataWithThread(dataReq);
            }

            @Override
            public void onStopped() {

            }

            @Override
            public void onException(Exception e) {

            }
        });
        apkClient.setFileSaveDir(FileUtils.currentWorkDir + "apk");
        apkClient.subscribeDataResponseListener(new DataResponseListener() {
            @Override
            public void heartBeatPacket(DataPacket dataPacket) {

            }

            //        80864108
//        17875556
            @Override
            public void dataPacket(int cmd, DataPacket requestPacket, DataPacket responsePacket) {

                System.out.println("客服端监听服务端:cmd: " + cmd + " ,requestPacket: " + requestPacket + " responsePacket: " + responsePacket);
                if (msgCallback != null && responsePacket.flag == DataPacket.SERVER_REQUEST) {//服务端的请求信息
                    msgCallback.result(requestPacket.toString());
                }
            }

        });
        apkClient.connect();
    }


    public static void sendDataWithThread(DataReq dataReq) {
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
