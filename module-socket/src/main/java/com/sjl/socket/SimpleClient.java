package com.sjl.socket;

import com.sjl.socket.base.ConsoleUtils;
import com.sjl.socket.base.DataPacket;
import com.sjl.socket.base.DataReq;
import com.sjl.socket.base.DataRes;
import com.sjl.socket.base.DataResponseListener;
import com.sjl.socket.base.DataType;
import com.sjl.socket.base.HeartbeatListener;
import com.sjl.socket.util.FileUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * socket长连接
 *
 * @author Kelly
 * @version 1.0.0
 * @filename ApkClient
 * @time 2021/8/4 11:58
 * @copyright(C) 2021 song
 */
public class SimpleClient extends BaseSocket {
    private Socket socket;
    private DataOutputStream out;
    private ClientListener clientListener;
    public String ip;
    public int port;
    private Timer timer;
    protected Object obj = new Object();
    private static final int HEART_TIME = 10 * 1000;

    private static final AtomicInteger ai = new AtomicInteger(-1);
    private Map<Integer, DataPacket> mDataPacketList = new ConcurrentHashMap<>();
    /**
     * 数据回调
     */
    private Map<Integer, DataResponseListener> mDataCallbacks = new ConcurrentHashMap<>();

    /**
     * 心跳监听
     */
    private HeartbeatListener heartbeatListener;
    /**
     * 服务端消息监听
     */
    private DataResponseListener dataResponseListener;

    public SimpleClient(String ip, int port) {
        this.ip = ip;
        this.port = port;
        REQUEST_FLAG = DataPacket.CLIENT_REQUEST;
    }


    private void startHeartbeat() {
        timer = new Timer();
        DataReq dataReq = new DataReq("heart") {
            @Override
            public DataType geDataType() {
                return DataType.HEARTBEAT;
            }

            @Override
            public int getCmd() {
                return 0x01;
            }

            @Override
            public int getSeq() {
                int andDecrement = ai.getAndDecrement();
                if (andDecrement == Integer.MIN_VALUE) {
                    ai.set(-1);
                }
                return andDecrement;
            }
        };
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!running) {
                    return;
                }
                if (isConnected()) {
                    try {
                        sendData(dataReq);
                    } catch (Exception e) {
                        ConsoleUtils.e("心跳异常", e);
                    }
                }
            }
        }, 1000, HEART_TIME);
    }

    private void stopHeartbeat() {
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
        stopHeartbeat();
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }
        if (clientListener != null) {
            clientListener.onStopped();
        }
    }

    public boolean isConnected() {
        if (socket != null && socket.isConnected() && !socket.isClosed()) {
            return true;
        }
        return false;
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
                startHeartbeat();
                parseDataPacket(socket);
            } catch (Exception e) {
                ConsoleUtils.e("客户端IO异常", e);
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
                        ConsoleUtils.i("====文本end");
                        handleResponse(responsePacket);
                        break;
                    }
                    case FILE_AND_TEXT: {
                        //解析文本
                        responsePacket.readTextAndFile(type, inputStream, fileSaveDir);
                        ConsoleUtils.i("文件大小:" + responsePacket.fileLength + ",格式化大小:" + FileUtils.formatFileSize(responsePacket.fileLength)
                                + ",文件写入成功:" + responsePacket.file.getName());
                        ConsoleUtils.i("====文本和文件end");
                        handleResponse(responsePacket);
                        break;
                    }
                    case HEARTBEAT: {
                        //解析服务端应答包
                        responsePacket.readText(type, inputStream);
                        ConsoleUtils.i("收到服务端心跳应答end");
                        handleResponse(responsePacket);
                        break;
                    }
                    default:
                        break;
                }
            } else {
                ConsoleUtils.e("服务端非法消息type：" + type);
            }

        }
    }

    private void handleResponse(DataPacket responsePacket) {
        byte flag = responsePacket.flag;
        int seq = responsePacket.seq;
        byte end = responsePacket.end;
        if (end == DataPacket.End) {
            if (flag == REQUEST_FLAG) { //C->S
                DataPacket requestPacket = mDataPacketList.get(seq);
                callbackClientRequestResult(requestPacket, responsePacket);
            } else {
                //C<-S
                final DataPacket requestPacket = responsePacket;
                DataPacket replyResponsePacket = replyData(requestPacket, DataRes.SUCCESS, "Success");
                callbackServerRequestResult(requestPacket, replyResponsePacket);
            }
        } else {
            //异常应答，不回调callbackResult
            replyData(responsePacket, DataRes.FAIL, "Fail");
        }
    }


    /**
     * 应答数据
     *
     * @param requestDataPacket
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
                byte[] dataByte = text.getBytes(DataPacket.ENCODE);
                DataPacket responsePacket = new DataPacket(DataType.TEXT_ONLY.getDataType(), requestDataPacket.flag, requestDataPacket.cmd, requestDataPacket.seq, dataByte);
                responsePacket.writeText(out);
                return responsePacket;
            }
        } catch (Exception e) {
            ConsoleUtils.e("应答异常", e);
        }
        return null;
    }


    public void subscribeDataResponseListener(DataResponseListener dataResponseListener) {
        this.dataResponseListener = dataResponseListener;
    }

    public void unsubscribeDataResponseListener() {
        this.dataResponseListener = null;
    }

    public void setHeartbeatListener(HeartbeatListener heartbeatListener) {
        this.heartbeatListener = heartbeatListener;
    }

    /**
     * 来自服务端的请求信息
     *
     * @param requestPacket
     * @param responsePacket
     */
    private void callbackServerRequestResult(DataPacket requestPacket, DataPacket responsePacket) {
        if (responsePacket != null) {
            if (dataResponseListener != null) {//数据
                dataResponseListener.sendOnSuccess(requestPacket.cmd, requestPacket, responsePacket);
            }
        }
    }

    /**
     * 来自客户端端的请求信息
     *
     * @param requestPacket
     * @param responsePacket
     */
    private void callbackClientRequestResult(DataPacket requestPacket, DataPacket responsePacket) {
        int seq = 0;
        try {
            if (requestPacket != null) {
                seq = requestPacket.seq;
                if (responsePacket.dataType == DataType.HEARTBEAT.getDataType()) {//心跳
                    if (heartbeatListener == null) {
                        return;
                    }
                    heartbeatListener.heartBeatPacket(requestPacket, responsePacket, ip);
                } else {
                    DataResponseListener dataResponseListener = mDataCallbacks.get(seq);
                    if (dataResponseListener != null) {
                        dataResponseListener.sendOnSuccess(requestPacket.cmd, requestPacket, responsePacket);
                    }
                }
            }

        } catch (Exception e) {
            //异常处理
            if (requestPacket != null) {
                seq = requestPacket.seq;
                if (responsePacket.dataType == DataType.HEARTBEAT.getDataType()) {//心跳
                    if (heartbeatListener == null) {
                        return;
                    }
                    heartbeatListener.heartBeatPacket(requestPacket, responsePacket, ip);
                } else {
                    DataResponseListener dataResponseListener = mDataCallbacks.get(seq);
                    if (dataResponseListener != null) {
                        dataResponseListener.sendOnSuccess(requestPacket.cmd, requestPacket, responsePacket);
                    }
                }
            }
        } finally {
            mDataPacketList.remove(seq);
            mDataCallbacks.remove(seq);
        }
    }

    /**
     * 异步发送数据
     *
     * @param dataReq
     * @param dataResponseListener
     */
    public void sendData(DataReq dataReq, DataResponseListener dataResponseListener) {
        if (dataResponseListener == null) {
            return;
        }

        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    int seq = sendData(dataReq);
                    mDataCallbacks.put(seq, dataResponseListener);
                    //10s等待服务端，返回信息
                    int readWaitTimeout = 10 * 1000;
                    long start = System.currentTimeMillis();
                    boolean success = false;
                    while (System.currentTimeMillis() - start <= readWaitTimeout) {
                        DataResponseListener temp = mDataCallbacks.get(seq);
                        if (temp == null) {
                            //说明被移除成功，证明接收成功
                            success = true;
                            break;
                        }
                    }
                    if (!success) {
                        mDataPacketList.remove(seq);
                        mDataCallbacks.remove(seq);
                        dataResponseListener.sendOnError(dataReq.getCmd(), new RuntimeException("超时读取数据"));
                    }
                } catch (Exception e) {
                    dataResponseListener.sendOnError(dataReq.getCmd(), e);
                }

            }
        });
    }


    /**
     * @param dataReq
     * @return 返回报文的序列号
     * @throws Exception
     */
    public int sendData(DataReq dataReq) throws Exception {
        if (dataReq == null) {
            throw new NullPointerException("dataReq为空");
        }
        synchronized (obj) {
            DataType dataType = dataReq.geDataType();
            if (out == null) {
                ConsoleUtils.e("未连接服务端");
                return 0;
            }
            if (dataType == DataType.TEXT_ONLY) {
                return sendTextData(out, dataType.getDataType(), dataReq);
            } else if (dataType == DataType.FILE_AND_TEXT) {
                return sendTextAndFileData(out, dataType.getDataType(), dataReq);
            } else if (dataType == DataType.HEARTBEAT) {
                return sendHeartData(out, DataType.HEARTBEAT.getDataType(), dataReq);
            } else {
                ConsoleUtils.e("不支持命令：" + dataType);
                throw new IllegalArgumentException("非法数据包发送");
            }
        }

    }

    private int sendHeartData(DataOutputStream out, byte dataType, DataReq dataReq) throws Exception {
        DataPacket dataPacket = new DataPacket(dataType, REQUEST_FLAG, dataReq.getCmd(), dataReq.getSeq());
        dataPacket.sendHeart(out);
        cacheMsg(dataPacket);
        return dataPacket.seq;
    }

    private void cacheMsg(DataPacket dataPacket) {
        mDataPacketList.put(dataPacket.seq, dataPacket);
    }

    private int sendTextData(DataOutputStream out, byte dataType, DataReq dataReq) throws Exception {
        String text = gson.toJson(dataReq.data);
        ConsoleUtils.i("发送文本：" + text);
        byte[] dataByte = text.getBytes(DataPacket.ENCODE);
        DataPacket dataPacket = new DataPacket(dataType, REQUEST_FLAG, dataReq.getCmd(), dataReq.getSeq(), dataByte);
        dataPacket.writeText(out);
        cacheMsg(dataPacket);
        return dataPacket.seq;
    }


    private int sendTextAndFileData(DataOutputStream out, byte dataType, DataReq dataReq) throws Exception {
        File file = dataReq.file;
        if (file.exists() && file.isFile()) {
            String text = gson.toJson(dataReq.data);
            ConsoleUtils.i("发送文本：" + text);
            byte[] dataByte = text.getBytes(DataPacket.ENCODE);
            byte[] fileNameData = dataReq.fileName.getBytes(DataPacket.ENCODE);
            DataPacket dataPacket = new DataPacket(dataType, REQUEST_FLAG, dataReq.getCmd(), dataReq.getSeq(), dataByte, fileNameData, file);
            dataPacket.writeTextAndFile(out);
            cacheMsg(dataPacket);
            return dataPacket.seq;
        } else {
            throw new IllegalArgumentException("不存在该文件或不是文件，发送失败：" + dataReq.toString());
        }
    }

}
