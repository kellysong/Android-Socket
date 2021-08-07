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
import java.net.ServerSocket;
import java.net.Socket;

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
public class SimpleServer extends BaseSocket{
    private int port;
    private ServerSocket serverSocket = null;
    private Thread serverReceiverThread;
    private ServerListener serverListener;

    public SimpleServer(int port) {
        this.port = port;
        REQUEST_FLAG =  DataPacket.SERVER_REQUEST;
    }



    public void startup() {
        running = false;
        serverReceiverThread = new Thread(new MyServerThread());
        serverReceiverThread.start();
    }


    public void shutdown() {
        running = false;
        if (serverSocket != null) {
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


    public void setServerListener(ServerListener serverListener) {
        this.serverListener = serverListener;
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
                System.out.println("Server is running");
                running = true;
                if (serverListener != null) {
                    serverListener.onStarted();
                }
                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println("client is connected");
                    System.out.println(socket.getLocalSocketAddress());
                    Thread clientThread = new Thread(new ClientConnectThread(socket));
                    clientThread.start();
                }
            } catch (Exception e) {
                System.out.println("服务端异常:" + e.getMessage());
                running = false;
                if (serverListener != null) {
                    serverListener.onException(e);
                }
            }
        }
    }

    private class ClientConnectThread implements Runnable {
        private Socket socket;
        private DataOutputStream out;
        private Object obj = new Object();

        public ClientConnectThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                OutputStream os = socket.getOutputStream();
                out = new DataOutputStream(os);
                parseDataPacket(socket);
            } catch (Exception e) {
                System.out.println("客服端连接服务端中断:" + e.getMessage());
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
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
                            responsePacket.readHeart(type, inputStream);
                            System.out.println("====收到客服端心跳end");
                            handleResponse(responsePacket);
                            break;
                        }
                        default:
                            break;
                    }
                } else {
                    System.out.println("客服端非法消息type：" + type);
                }

            }

        }

        private void handleResponse(DataPacket responsePacket) {
            byte flag = responsePacket.flag;
            int seq = responsePacket.seq;
            byte end = responsePacket.end;
            if (end == DataPacket.End) {
                if (flag == REQUEST_FLAG) { //C<-S
                    DataPacket requestPacket = msgList.get(seq);
                    if (requestPacket != null) {
                        callbackResult(requestPacket, responsePacket);
                        msgList.remove(seq);
                    }
                } else {
                    //C->S
                    final DataPacket requestPacket = responsePacket;
                    DataPacket replyResponsePacket = replyData(requestPacket, DataRes.SUCCESS, "Success");
                    if (replyResponsePacket != null) {
                        callbackResult(requestPacket, replyResponsePacket);
                    }
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
                    System.out.println("应答文本：" + text);
                    byte[] dataByte = text.getBytes(DataPacket.ENCODE);
                    byte dataType = requestDataPacket.dataType;
                    if (requestDataPacket.dataType != DataType.HEART.getDataType()) {
                        dataType = DataType.TEXT_ONLY.getDataType();
                    }
                    DataPacket dataPacket = new DataPacket(dataType, requestDataPacket.flag, requestDataPacket.cmd, dataByte.length, dataByte, requestDataPacket.seq);
                    dataPacket.writeText(out);
                    return dataPacket;
                }
            } catch (Exception e) {
                System.out.println("应答异常:" + e.getMessage());
            }

            return null;
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
    }

    private static DataRes getDataRes() {
        return dataRes;
    }

    private DataResponseListener dataResponseListener;

    public void subscribeDataResponseListener(DataResponseListener dataResponseListener) {
        this.dataResponseListener = dataResponseListener;
    }


}