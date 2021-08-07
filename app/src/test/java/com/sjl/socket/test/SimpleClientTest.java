package com.sjl.socket.test;


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
 * @author Kelly
 * @version 1.0.0
 * @filename SimpleClientTest
 * @time 2021/8/5 14:13
 * @copyright(C) 2021 song
 */
public class SimpleClientTest {
    public static void main(String[] args) {
        String apk_55 = FileUtils.currentWorkDir + "apk" + File.separator + "55_BeeBoxCabinetScenic_dev_release_v1.55.7_2021-07-29_16-24.apk";
        File file = new File(apk_55);
        if (file.isFile()) {
            DataReq dataReq = new TextReq(new FileInfo());
//            DataReq dataReq = new FileReq(new FileInfo(), file);//需要发送多台设备,拷贝多次
            sendData("192.168.56.118", 8090, dataReq);
        }

    }

    private static void sendData(String ip, int port, DataReq dataReq) {
        SimpleClient apkClient = new SimpleClient(ip, port);
        apkClient.setClientListener(new SimpleClient.ClientListener() {
            @Override
            public void onStarted() {
                sendDataWithThread(apkClient, dataReq);
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

            }

        });
        apkClient.connect();
    }



    private static void sendDataWithThread(SimpleClient apkClient, DataReq dataReq) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    apkClient.sendData(dataReq);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
