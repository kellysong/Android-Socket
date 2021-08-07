package com.sjl.socket;

import com.sjl.socket.base.DataPacket;
import com.sjl.socket.base.DataReq;
import com.sjl.socket.base.DataRes;
import com.sjl.socket.base.DataResponseListener;
import com.sjl.socket.base.DataType;
import com.sjl.socket.util.FileUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * socket长连接
 *
 * @author Kelly
 * @version 1.0.0
 * @filename ApkClient
 * @time 2021/8/4 11:58
 * @copyright(C) 2021 song
 */
public class SimpleClient extends BaseSocket{
    private Socket socket;
    private DataOutputStream out;
    private ClientListener clientListener;
    public String ip;
    public int port;
    private Timer timer;


    private DataResponseListener dataResponseListener;


    public SimpleClient(String ip, int port) {
        this.ip = ip;
        this.port = port;
        REQUEST_FLAG =  DataPacket.CLIENT_REQUEST;
    }



    private void startHeart() {
        timer = new Timer();
        DataReq dataReq = new DataReq("heart") {
            @Override
            public DataType geDataType() {
                return DataType.HEART;
            }

            @Override
            public int getCmd() {
                return 0x01;
            }
        };
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!running) {
                    return;
                }
                if (socket != null && socket.isConnected() && out != null) {
                    try {
                        sendData(dataReq);
                    } catch (Exception e) {
                        System.out.println("心跳异常：" + e.getMessage());
                    }
                }
            }
        }, 1000, 8 * 1000);
    }

    private void stopHeart() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }


    public void connect() {
        if (isRunning()) {
            if (clientListener != null) {
                clientListener.onStarted();
            }
        } else {
            running = false;
            MyClient myClient = new MyClient();
            Thread thread = new Thread(myClient);
            thread.start();
        }
    }


    public void disconnect() {
        running = false;
        stopHeart();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (clientListener != null) {
            clientListener.onStopped();
        }
    }

    public interface ClientListener {
        void onStarted();

        void onStopped();

        void onException(Exception e);
    }

    public void setClientListener(ClientListener clientListener) {
        this.clientListener = clientListener;
    }

    public class MyClient implements Runnable {


        @Override
        public void run() {
            try {
                socket = new Socket(ip, port);
                running = true;

                OutputStream os = socket.getOutputStream();
                out = new DataOutputStream(os);
                if (clientListener != null) {
                    clientListener.onStarted();
                }
                //启动心跳
                startHeart();
                parseDataPacket(socket);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("客户端异常：" + e.getMessage());
                if (clientListener != null) {
                    clientListener.onException(e);
                }
            } finally {
                disconnect();
            }
        }


    }



    /**
     * 解析服务端数据包
     *
     * @param server
     * @throws IOException
     */
    private synchronized void parseDataPacket(Socket server) throws IOException {
        InputStream ins = server.getInputStream();
        DataInputStream inputStream = new DataInputStream(ins);
        while (true) {
            //读取cmd
            byte type = inputStream.readByte();
            if (type == DataPacket.End) {
                continue;
            }
            DataType dataType = DataType.parseType(type);
            if (dataType != null) {
                DataPacket responsePacket = new DataPacket();
                switch (dataType) {
                    case TEXT_ONLY: {
                        //解析文本
                        responsePacket.readText(type, inputStream);
                        System.out.println("====文本end");
                        handleResponse(responsePacket);
                        break;
                    }
                    case FILE_AND_TEXT: {
                        //解析文本
                        responsePacket.readTextAndFile(type, inputStream, fileSaveDir);
                        System.out.println("文件大小:" + responsePacket.fileLength + ",格式化大小:" + FileUtils.formatFileSize(responsePacket.fileLength)
                                + ",文件写入成功:" + responsePacket.file.getName());
                        System.out.println("====文本和文件end");
                        handleResponse(responsePacket);
                        break;
                    }
                    case HEART: {
                        //解析服务端应答包
                        responsePacket.readText(type, inputStream);
                        System.out.println("收到服务端心跳应答end");
                        handleResponse(responsePacket);
                        break;
                    }
                    default:
                        break;
                }
            } else {
                System.out.println("服务端非法消息type：" + type);
            }

        }
    }

    private void handleResponse(DataPacket responsePacket) {
        byte flag = responsePacket.flag;
        int seq = responsePacket.seq;
        byte end = responsePacket.end;
        if (end == DataPacket.End) {
            if (flag == REQUEST_FLAG) { //C->S
                DataPacket requestPacket = msgList.get(seq);
                if (requestPacket != null) {
                    callbackResult(requestPacket, responsePacket);
                    msgList.remove(seq);
                }
            } else {
                //C<-S
                final DataPacket requestPacket = responsePacket;
                DataPacket replyResponsePacket = replyData(requestPacket, DataRes.SUCCESS, "Success");
                if (replyResponsePacket != null) {
                    callbackResult(requestPacket, replyResponsePacket);
                }
            }
        } else {
            //异常应答，不回调callbackResult
            replyData(responsePacket, DataRes.FAIL, "Fail");
        }
    }


    /**
     * 应答数据
     *  @param requestDataPacket
     * @param code
     * @param msg
     * @return
     */
    private DataPacket replyData(DataPacket requestDataPacket, int code, String msg) {
        try {
            synchronized (obj) {
                DataRes dataRes = getDataRes();
                dataRes.code = code;
                dataRes.msg = msg;
                String text = gson.toJson(dataRes);
                System.out.println("应答文本：" + text);
                byte[] dataByte = text.getBytes(DataPacket.ENCODE);
                DataPacket responsePacket = new DataPacket(DataType.TEXT_ONLY.getDataType(), requestDataPacket.flag, requestDataPacket.cmd, dataByte.length, dataByte, requestDataPacket.seq);
                responsePacket.writeText(out);
                return requestDataPacket;
            }
        } catch (Exception e) {
            System.out.println("应答异常:" + e.getMessage());
        }
        return null;
    }

    private static DataRes getDataRes() {
        return dataRes;
    }

    public void subscribeDataResponseListener(DataResponseListener dataResponseListener) {
        this.dataResponseListener = dataResponseListener;
    }

    private void callbackResult(DataPacket requestPacket, DataPacket responsePacket) {
        if (dataResponseListener == null) {
            return;
        }
        if (responsePacket.dataType == DataType.HEART.getDataType()) {
            dataResponseListener.heartBeatPacket(responsePacket);
        } else {
            dataResponseListener.dataPacket(requestPacket.cmd, requestPacket, responsePacket);
        }
    }


    public void sendData(DataReq dataReq) throws Exception {
        synchronized (obj) {
            DataType dataType = dataReq.geDataType();
            if (out == null) {
                System.out.println("客户端未连接");
                return;
            }
            if (dataType == DataType.TEXT_ONLY) {
                sendTextData(out, dataType.getDataType(), dataReq);
            } else if (dataType == DataType.FILE_AND_TEXT) {
                sendTextAndFileData(out, dataType.getDataType(), dataReq);
            } else if (dataType == DataType.HEART) {
                sendHeartData(out, DataType.HEART.getDataType(), dataReq);
            } else {
                System.out.println("不支持命令：" + dataType);
            }
        }

    }

    private void sendHeartData(DataOutputStream out, byte dataType, DataReq dataReq) throws Exception {
        DataPacket dataPacket = new DataPacket(dataType, REQUEST_FLAG, dataReq.getCmd());
        dataPacket.sendHeart(out);
        cacheMsg(dataPacket);
    }

    private void cacheMsg(DataPacket dataPacket) {
        msgList.put(dataPacket.seq, dataPacket);
    }

    private void sendTextData(DataOutputStream out, byte dataType, DataReq dataReq) throws Exception {
        String text = gson.toJson(dataReq.data);
        System.out.println("发送文本：" + text);
        byte[] dataByte = text.getBytes(DataPacket.ENCODE);
        DataPacket dataPacket = new DataPacket(dataType, REQUEST_FLAG, dataReq.getCmd(), dataByte.length, dataByte);
        dataPacket.writeText(out);
        cacheMsg(dataPacket);
    }


    private void sendTextAndFileData(DataOutputStream out, byte dataType, DataReq dataReq) throws Exception {
        File file = dataReq.file;
        if (file.isFile()) {
            long fileLength = file.length();
            String text = gson.toJson(dataReq.data);
            System.out.println("发送文本：" + text);
            byte[] dataByte = text.getBytes(DataPacket.ENCODE);
            DataPacket dataPacket = new DataPacket(dataType, REQUEST_FLAG, dataReq.getCmd(), dataByte.length, dataByte, fileLength, file);
            dataPacket.writeTextAndFile(out);
            cacheMsg(dataPacket);
        } else {
            System.out.println("不是文件，发送失败：" + dataReq.toString());
        }
    }

}
