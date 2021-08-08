package com.sjl.socket.demo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sjl.socket.SimpleServer;
import com.sjl.socket.base.DataPacket;
import com.sjl.socket.base.DataReq;
import com.sjl.socket.base.DataResponseListener;
import com.sjl.socket.business.FileInfo;
import com.sjl.socket.business.ResultRes;
import com.sjl.socket.business.SampleCmd;
import com.sjl.socket.business.TextReq;
import com.sjl.socket.util.FileUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename SimpleServerTest
 * @time 2021/8/8 11:33
 * @copyright(C) 2021 song
 */
public class SimpleServerTest {
    static SimpleServer simpleServer = null;

    static JTextArea serverLogArea, clientLogArea;

    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SocketFrame frame = new SocketFrame("Socket测试");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setLocationRelativeTo(null);//窗口居中
                frame.setSize(1280, 720);
                //显示窗口
                frame.setVisible(true);
                serverLogArea = frame.serverLogArea;
                clientLogArea = frame.clientLogArea;

                final JButton btnStart = frame.btnStart;
                final JButton btnStop = frame.btnStop;
                final JTextField textField = frame.textField;
                final JTextField textField2 = frame.textField2;
                final JTextField textField3 = frame.textField3;
                frame.setServerFrameListener(new SocketFrame.ServerFrameListener() {
                    @Override
                    public void onClickStart() {

                        String port = textField.getText().trim();
                        if (port == null || port.length() == 0) {
                            Object[] options = {"确定 ", "取消"};
                            JOptionPane.showOptionDialog(null, "port不能为空", "提示", JOptionPane.DEFAULT_OPTION,
                                    JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                        } else {
                            //            JOptionPane.showMessageDialog(this, "您输入了：" + str);
                            startup(Integer.parseInt(port));
                            btnStart.setEnabled(false);
                        }
                    }

                    @Override
                    public void onClickStop() {
                        if (simpleServer != null && simpleServer.isRunning()) {
                            simpleServer.shutdown();
                            btnStart.setEnabled(true);
                        } else {
                            JOptionPane.showMessageDialog(null, "服务还未启动，请启动服务再停止");
                        }
                    }


                    @Override
                    public void onClickServerSend() {
                        String msg = textField2.getText().trim();
                        if (msg == null || msg.length() == 0) {
                            JOptionPane.showMessageDialog(null, "消息不能空");
                            return;
                        }
                        FileInfo fileInfo = new FileInfo();
                        fileInfo.msg = msg;
                        DataReq dataReq = new TextReq(fileInfo);
                        simpleServer.pushDataTpAllClient(dataReq);
                    }

                    @Override
                    public void onClickClientSend() {
                        String msg = textField3.getText().trim();
                        if (msg == null || msg.length() == 0) {
                            JOptionPane.showMessageDialog(null, "消息不能空");
                            return;
                        }
                        FileInfo fileInfo = new FileInfo();
                        fileInfo.msg = msg;
                        DataReq dataReq = new TextReq(fileInfo);
                        SimpleClientTest.sendDataWithThread(dataReq);
                    }
                });
            }

        });

    }


    private static void startup(int port) {
        simpleServer = new SimpleServer(port);
        simpleServer.setServerListener(new SimpleServer.ServerListener() {
            @Override
            public void onStarted() {
                showServerMsg("onStarted");
            }

            @Override
            public void onStopped() {
                showServerMsg("onStopped");
            }

            @Override
            public void onException(Exception e) {
                showServerMsg("onException:" + e.getMessage());

            }
        });
        File dir = new File(FileUtils.currentWorkDir + File.separator + "apk" + File.separator + "fileTemp");
        simpleServer.setFileSaveDir(dir.getAbsolutePath());
        //数据监听
        simpleServer.subscribeDataResponseListener(new DataResponseListener() {
            @Override
            public void heartBeatPacket(DataPacket dataPacket) {
                //子线程
            }

            @Override
            public void dataPacket(int cmd, DataPacket requestPacket, DataPacket responsePacket) {
                //子线程
                SampleCmd sampleCmd = SampleCmd.fromValue(cmd);
                if (sampleCmd == null) {
                    return;
                }
                System.out.println("服务端监听客服端:cmd: " + cmd + " ,requestPacket: " + requestPacket + " responsePacket: " + responsePacket);
                String msg = new String(responsePacket.textData);
                Type type = new TypeToken<ResultRes<FileInfo>>() {
                }.getType();
                ResultRes<FileInfo> dataRes = new Gson().fromJson(msg, type);
                System.out.println("收到数据:" + dataRes);
                switch (sampleCmd) {
                    case NORMAL_TEXT:
                        break;
                    case UPLOAD_APK:
                        break;
                    default:
                        break;
                }
                if (responsePacket.flag == DataPacket.CLIENT_REQUEST){//客户端端的请求信息
                    showServerMsg(requestPacket.toString());
                }
            }
        });

        //定向客户端推送数据
//        simpleServer.pushDataTpClient("","");
        //往所有有客户端推送数据
//        simpleServer.pushDataTpAllClient();
        simpleServer.startup();
        showServerMsg("服务器已启动");

        SimpleClientTest.setMsgCallback(new SimpleClientTest.MsgCallback() {
            @Override
            public void result(String msg) {
                showClientMsg(msg);
            }
        });
    }

    private static void showServerMsg(String msg) {
        if (serverLogArea == null) {
            return;
        }
        serverLogArea.append(getFormatTime() + ":" + msg + "\n");
        serverLogArea.selectAll();
        //定位到最后一行
        if (serverLogArea.getSelectedText() != null) {
            serverLogArea.setCaretPosition(serverLogArea.getSelectedText().length());
            serverLogArea.requestFocus();

        }
    }

    private static void showClientMsg(String msg) {
        if (clientLogArea == null) {
            return;
        }
        getFormatTime();
        clientLogArea.append(getFormatTime() + ":" + msg + "\n");
        clientLogArea.selectAll();
        //定位到最后一行
        if (clientLogArea.getSelectedText() != null) {
            clientLogArea.setCaretPosition(clientLogArea.getSelectedText().length());
            clientLogArea.requestFocus();
        }
    }

    private static String getFormatTime() {
        Date date = new Date();
        String strDateFormat = "mm:ss:SSS";
        SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
        return sdf.format(date);
    }
}
