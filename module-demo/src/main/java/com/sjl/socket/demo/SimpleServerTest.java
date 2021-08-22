package com.sjl.socket.demo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sjl.socket.SimpleServer;
import com.sjl.socket.base.ConsoleUtils;
import com.sjl.socket.base.DataPacket;
import com.sjl.socket.base.DataReq;
import com.sjl.socket.base.DataResponseListener;
import com.sjl.socket.business.FileInfo;
import com.sjl.socket.business.ResultRes;
import com.sjl.socket.business.SampleCmd;
import com.sjl.socket.business.TextReq;

import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
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
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                // 得到显示器屏幕的宽高
                int width = toolkit.getScreenSize().width;
                int height = toolkit.getScreenSize().height;
                // 定义窗体的宽高
                int windowsW = 1280;
                int windowsH = 720;
                // 设置窗体位置和大小
                frame.setBounds((width - windowsW) / 2,
                        (height - windowsH) / 2, windowsW, windowsH);

                //显示窗口
                frame.setVisible(true);
                serverLogArea = frame.serverLogArea;
                clientLogArea = frame.clientLogArea;

                final JButton btnStart = frame.btnStart;
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
//                        simpleServer.pushDataTpAllClient(dataReq);
                        //异步推送
                        simpleServer.pushDataTpClient(SimpleClientTest.ip, dataReq, new DataResponseListener() {
                            @Override
                            public void sendOnSuccess(int cmd, DataPacket requestPacket, DataPacket responsePacket) {
                                ConsoleUtils.i("推送消息成功,cmd: " + cmd + " ,requestPacket: " + requestPacket + " responsePacket: " + responsePacket);
                                showServerMsg(requestPacket.toString());
                            }

                            @Override
                            public void sendOnError(int cmd, Throwable t) {
                                showServerMsg(t.getMessage());
                            }
                        });
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
                        SimpleClientTest.sendData(dataReq);
                    }
                });
                frame.addWindowListener(new WindowListener() {
                    @Override
                    public void windowOpened(WindowEvent e) {

                    }

                    @Override
                    public void windowClosing(WindowEvent e) {

                        SimpleClientTest.disconnect();
                        if (simpleServer != null) {
                            simpleServer.shutdown();
                        }
                    }

                    @Override
                    public void windowClosed(WindowEvent e) {

                    }

                    @Override
                    public void windowIconified(WindowEvent e) {
                    }

                    @Override
                    public void windowDeiconified(WindowEvent e) {

                    }

                    @Override
                    public void windowActivated(WindowEvent e) {

                    }

                    @Override
                    public void windowDeactivated(WindowEvent e) {

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
        //服务端保存目录
        File dir = new File(SimpleClientTest.getApkDir(), "apk" + File.separator + "serverDownload");
        simpleServer.setFileSaveDir(dir.getAbsolutePath());
        //订阅客户端的请求信息
        simpleServer.subscribeDataResponseListener(new DataResponseListener() {
            @Override
            public void sendOnSuccess(int cmd, DataPacket requestPacket, DataPacket responsePacket) {
                //子线程
                SampleCmd sampleCmd = SampleCmd.fromValue(cmd);
                if (sampleCmd == null) {
                    return;
                }
                ConsoleUtils.i("服务端监听客服端:cmd: " + cmd + " ,requestPacket: " + requestPacket + " responsePacket: " + responsePacket);
                String msg = new String(responsePacket.textData);
                Type type = new TypeToken<ResultRes<FileInfo>>() {
                }.getType();
                //解析出数据，处理业务逻辑
                ResultRes<FileInfo> dataRes = new Gson().fromJson(msg, type);
                switch (sampleCmd) {
                    case NORMAL_TEXT:
                        break;
                    case UPLOAD_APK:
                        break;
                    default:
                        break;
                }
                showServerMsg(requestPacket.toString());
            }

            @Override
            public void sendOnError(int cmd, Throwable t) {
                ConsoleUtils.e("接收客户端信息异常:" + cmd, t);
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
