
package com.sjl.socket.test;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.mbms.FileInfo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sjl.socket.SimpleServer;
import com.sjl.socket.base.DataPacket;
import com.sjl.socket.base.DataResponseListener;
import com.sjl.socket.business.ResultRes;
import com.sjl.socket.business.SampleCmd;
import com.sjl.socket.test.util.Logger;
import com.sjl.socket.test.util.NetUtils;

import java.io.File;
import java.lang.reflect.Type;

import androidx.annotation.Nullable;

/**
 * 后台服务
 */
public class CoreService extends Service {

    private SimpleServer simpleServer;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        simpleServer = new SimpleServer(8090);
        simpleServer.setServerListener(new SimpleServer.ServerListener() {
            @Override
            public void onStarted() {
                String hostAddress = NetUtils.getLocalIPAddress().getHostAddress();
                ServerManager.onServerStart(CoreService.this, hostAddress);
            }

            @Override
            public void onStopped() {
                ServerManager.onServerStop(CoreService.this);
            }

            @Override
            public void onException(Exception e) {
                ServerManager.onServerError(CoreService.this, e.getMessage());
            }
        });

        File dir = new File(Environment.getExternalStorageDirectory(), "fileTemp");
        simpleServer.setFileSaveDir(dir.getAbsolutePath());
        //数据监听
        simpleServer.subscribeDataResponseListener(new DataResponseListener() {
            @Override
            public void heartBeatPacket(DataPacket dataPacket) {

            }

            @Override
            public void dataPacket(int cmd, DataPacket requestPacket, DataPacket responsePacket) {
                SampleCmd sampleCmd = SampleCmd.fromValue(cmd);
                if (sampleCmd == null) {
                    return;
                }
                Logger.i("服务端监听客服端:cmd: " + cmd + " ,requestPacket: " + requestPacket + " responsePacket: " + responsePacket);

                String msg = new String(responsePacket.textData);
                Type type = new TypeToken<ResultRes<FileInfo>>() {
                }.getType();
                ResultRes<FileInfo> dataRes = new Gson().fromJson(msg, type);
                Logger.i("收到数据:" + dataRes);
                /*switch (sampleCmd) {
                    case NORMAL_TEXT:
                        break;
                    case UPLOAD_APK:
                        break;
                    default:
                        break;
                }*/
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startServer();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopServer();
        super.onDestroy();
    }

    /**
     * Start server.
     */
    private void startServer() {
        if (simpleServer.isRunning()) {
            String hostAddress = NetUtils.getLocalIPAddress().getHostAddress();
            ServerManager.onServerStart(CoreService.this, hostAddress);
        } else {
            simpleServer.startup();
        }
    }

    /**
     * Stop server.
     */
    private void stopServer() {
        simpleServer.shutdown();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}