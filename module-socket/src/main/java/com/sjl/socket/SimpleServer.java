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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * java.net.SocketException: Connection reset 引起这个异常的原因有两个：
 * 一、客户端和服务器端如果一端的Socket被关闭，另一端仍发送数据，发送的第一个数据包引发该异常；
 * 二、客户端和服务器端一端退出，但退出时并未关闭该连接，另一端如果在从连接中读数据则抛出该异常。
 * 简单来说就是在连接断开后的读和写操作引起的。 你检查一下你的连接。
 *
 * @author Kelly
 * @version 1.0.0
 * @filename SimpleServer
 * @time 2021/8/4 11:40
 * @copyright(C) 2021 song
 */
public class SimpleServer extends BaseSocket {
    private int port;
    private ServerSocket serverSocket = null;
    private Thread serverReceiverThread;
    private ServerListener serverListener;
    /**
     * 心跳监听
     */
    private HeartbeatListener heartbeatListener;
    /**
     * 客户端消息监听
     */
    private DataResponseListener dataResponseListener;

    protected Map<String, ServerClient> clientConnectionList = new ConcurrentHashMap<>();

    public SimpleServer(int port) {
        this.port = port;
        REQUEST_FLAG = DataPacket.SERVER_REQUEST;
    }


    public void startup() {
        running = false;
        serverReceiverThread = new Thread(new MyServerThread());
        serverReceiverThread.start();
    }


    public void shutdown() {
        running = false;
        for (Map.Entry<String, ServerClient> entry : clientConnectionList.entrySet()) {
            String key = entry.getKey();
            ServerClient serverClient = entry.getValue();
            serverClient.disconnect();
            ConsoleUtils.i("断开-->" + key + "连接");
        }
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (serverListener != null) {
            serverListener.onStopped();
        }
    }

    private Map<String, ServerClient> getClientConnectionList() {
        return clientConnectionList;
    }

    private ServerClient getClientConnection(String clientIp) {
        return clientConnectionList.get(clientIp);
    }

    public void setServerListener(ServerListener serverListener) {
        this.serverListener = serverListener;
    }


    public boolean pushDataTpClient(String clientIp, DataReq dataReq) {
        SimpleServer.ServerClient clientConnectionList = getClientConnection(clientIp);
        if (clientConnectionList != null) {
            try {
                clientConnectionList.sendData(dataReq);
                return true;
            } catch (Exception e) {
                ConsoleUtils.e("推送-->" + clientIp + "失败", e);
            }
        }
        return false;

    }

    /**
     * 异步推送数据到指定客户端
     *
     * @param clientIp
     * @param dataReq
     * @param dataResponseListener
     */
    public void pushDataTpClient(String clientIp, DataReq dataReq, DataResponseListener dataResponseListener) {
        SimpleServer.ServerClient clientConnectionList = getClientConnection(clientIp);
        if (clientConnectionList != null) {
            clientConnectionList.sendData(dataReq, dataResponseListener);
        } else {
            if (dataResponseListener != null) {
                dataResponseListener.sendOnError(dataReq.getCmd(), new RuntimeException("客户端未连接，推送失败"));
            }
        }
    }

    public void pushDataTpAllClient(DataReq dataReq) {
        Map<String, ServerClient> clientConnectionList = getClientConnectionList();
        for (Map.Entry<String, ServerClient> entry : clientConnectionList.entrySet()) {
            String key = entry.getKey();
            ServerClient serverClient = entry.getValue();
            if (serverClient.isConnected()) {
                try {
                    serverClient.sendData(dataReq);
                } catch (Exception e) {
                    ConsoleUtils.e("推送-->" + key + "失败", e);
                }
            }

        }
    }

    public interface ServerListener {
        void onStarted();

        void onStopped();

        void onException(Exception e);
    }

    /**
     * 服务端连接监听线程
     */
    private class MyServerThread implements Runnable {


        public void run() {
            try {
                serverSocket = new ServerSocket(port);
                ConsoleUtils.i("Server is running");
                running = true;
                if (serverListener != null) {
                    serverListener.onStarted();
                }
                while (true) {
                    if (!serverSocket.isClosed()) {
                        Socket socket = serverSocket.accept();
                        ConsoleUtils.i("client is connected");
                        InetAddress inetAddress = socket.getInetAddress();
                        String ip = inetAddress.getHostAddress();
                        //inet address: 127.0.0.1
                        ConsoleUtils.i("inet address: " + inetAddress.toString());
                        ServerClient oldServerClient = getClientConnection(ip);
                        if (oldServerClient != null && oldServerClient.isConnected()) {
                            oldServerClient.disconnect();
                        }
                        ServerClient serverClient = new ServerClient(socket, ip);
                        Thread clientThread = new Thread(serverClient);
                        clientThread.start();
                        //同一个客户端ip,只保留一个最新的长连接
                        clientConnectionList.put(ip, serverClient);

                    }

                }
            } catch (Exception e) {
                ConsoleUtils.e("服务端监听异常", e);
                running = false;
                if (serverListener != null) {
                    serverListener.onException(e);
                }
            }
        }
    }

    /**
     * ServerClient即客户端连接服务端的Client
     */
    public class ServerClient implements Runnable {
        private Socket socket;
        private String ip;
        private DataOutputStream out;
        private Object obj = new Object();
        private Map<Integer, DataPacket> mDataPacketList = new ConcurrentHashMap<>();
        /**
         * 数据回调
         */
        private Map<Integer, DataResponseListener> mDataCallbacks = new ConcurrentHashMap<>();

        public ServerClient(Socket socket, String ip) {
            this.socket = socket;
            this.ip = ip;
        }

        @Override
        public void run() {
            try {
                OutputStream os = socket.getOutputStream();
                out = new DataOutputStream(os);
                parseDataPacket(socket);
            } catch (Exception e) {
                ConsoleUtils.e("客服端连接服务端中断", e);
            } finally {
                disconnect();
            }
        }

        public void disconnect() {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
//                    e.printStackTrace();
                }
            }
        }

        public boolean isConnected() {
            if (socket != null && socket.isConnected() && !socket.isClosed()) {
                return true;
            }
            return false;
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
                    ConsoleUtils.e("客户端未连接");
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
            if (file.isFile()) {
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

        /**
         * 解析数据包
         *
         * @param client
         * @throws IOException
         */
        private synchronized void parseDataPacket(Socket client) throws IOException {
            InputStream ins = client.getInputStream();
            DataInputStream inputStream = new DataInputStream(ins);
            //服务端解包过程
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
                            responsePacket.readHeart(type, inputStream);
                            ConsoleUtils.i("====收到客服端心跳end");
                            handleResponse(responsePacket);
                            break;
                        }
                        default:
                            break;
                    }
                } else {
                    ConsoleUtils.e("客服端非法消息type：" + type);
                }

            }

        }

        private void handleResponse(DataPacket responsePacket) {
            byte flag = responsePacket.flag;
            int seq = responsePacket.seq;
            byte end = responsePacket.end;
            if (end == DataPacket.End) {
                if (flag == REQUEST_FLAG) { //C<-S
                    DataPacket requestPacket = mDataPacketList.get(seq);
                    callbackServerRequestResult(requestPacket, responsePacket);
                } else {
                    //C->S
                    final DataPacket requestPacket = responsePacket;
                    DataPacket replyResponsePacket = replyData(requestPacket, DataRes.SUCCESS, "Success");
                    callbackClientRequestResult(requestPacket, replyResponsePacket);
                }
            } else {
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
                    byte dataType = requestDataPacket.dataType;
                    if (requestDataPacket.dataType != DataType.HEARTBEAT.getDataType()) {
                        dataType = DataType.TEXT_ONLY.getDataType();
                    }
                    DataPacket responsePacket = new DataPacket(dataType, requestDataPacket.flag, requestDataPacket.cmd, requestDataPacket.seq, dataByte);
                    responsePacket.writeText(out);
                    return responsePacket;
                }
            } catch (Exception e) {
                ConsoleUtils.e("应答异常", e);
            }

            return null;
        }


        /**
         * 来自服务端的请求信息（没有心跳信息）
         *
         * @param requestPacket
         * @param responsePacket
         */
        private void callbackServerRequestResult(DataPacket requestPacket, DataPacket responsePacket) {
            int seq = 0;
            try {

                if (requestPacket != null) {
                    seq = requestPacket.seq;
                    DataResponseListener dataResponseListener = mDataCallbacks.get(seq);
                    if (dataResponseListener != null) {
                        dataResponseListener.sendOnSuccess(requestPacket.cmd, requestPacket, responsePacket);
                    }
                }

            } catch (Exception e) {
                //异常处理
                if (requestPacket != null) {
                    seq = requestPacket.seq;
                    DataResponseListener dataResponseListener = mDataCallbacks.get(seq);
                    if (dataResponseListener != null) {
                        dataResponseListener.sendOnError(requestPacket.cmd, e);
                    }
                }
            } finally {
                mDataPacketList.remove(seq);
                mDataCallbacks.remove(seq);
            }
        }

        /**
         * 来自客户端端的请求信息
         *
         * @param requestPacket
         * @param responsePacket
         */
        private void callbackClientRequestResult(DataPacket requestPacket, DataPacket responsePacket) {

            if (responsePacket != null) {
                if (responsePacket.dataType == DataType.HEARTBEAT.getDataType()) {//心跳
                    if (heartbeatListener == null) {
                        return;
                    }
                    heartbeatListener.heartBeatPacket(requestPacket, responsePacket, ip);
                } else if (dataResponseListener != null) {//数据
                    dataResponseListener.sendOnSuccess(requestPacket.cmd, requestPacket, responsePacket);
                }
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

                        int readWaitTimeout = 10 * 1000;
                        long start = System.currentTimeMillis();
                        boolean success = false;
                        while (System.currentTimeMillis() - start <= readWaitTimeout) {
                            DataPacket dataPacket = mDataPacketList.get(seq);
                            if (dataPacket == null) {
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
}