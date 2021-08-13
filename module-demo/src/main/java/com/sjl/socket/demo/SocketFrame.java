package com.sjl.socket.demo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

/**
 * (int x,int y,int width,int height)
 *
 * @author Kelly
 * @version 1.0.0
 * @filename SocketFrame
 * @time 2021/8/8 11:40
 * @copyright(C) 2021 song
 */
public class SocketFrame extends JFrame {
    public static final int PORT = 8090;

    public JTextArea serverLogArea, clientLogArea;
    public JTextField textField,textField2,textField3;
    public JButton btnStart, btnStop;
    public JScrollPane jScrollPane, jScrollPane2;
    public JButton btnSend, btnSend2;
    public JButton btnClear,btnClear2;
    public SocketFrame(String title) {
        super(title);
        initNorth();
        initCenter();
    }

    private void initNorth() {
        JPanel north = new JPanel();
        north.setPreferredSize(new Dimension(getWidth() / 2, 80));
        north.setLayout(new GridLayout(1, 2, 10, 5));
        add(north, BorderLayout.NORTH);

        JPanel serverPanel = new JPanel();
        serverPanel.setLayout(new GridLayout(2, 1, 10, 5));
        JPanel panel1 = new JPanel();
        JLabel label = new JLabel("端口号");
        //创建JTextField，16表示16列，用于JTextField的宽度显示而不是限制字符个数
        textField = new JTextField(16);
        btnStart = new JButton("启动");
        btnStop = new JButton("停止");
        textField.setText(String.valueOf(PORT));
        btnStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (serverFrameListener != null) {
                    serverFrameListener.onClickStart();
                }
            }
        });
        btnStop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (serverFrameListener != null) {
                    serverFrameListener.onClickStop();
                }
            }
        });

        panel1.add(label);
        panel1.add(textField);
        panel1.add(btnStart);
        panel1.add(btnStop);


        JPanel panel2 = new JPanel();
        textField2 = new JTextField(16);
        btnSend = new JButton("推送消息到客户端");
        btnClear = new JButton("清空");
        btnSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (serverFrameListener != null) {
                    serverFrameListener.onClickServerSend();
                }
            }
        });
        btnClear.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                textField2.setText("");
                serverLogArea.setText("");
            }
        });
        //添加控件
        panel2.add(textField2);
        panel2.add(btnSend);
        panel2.add(btnClear);

        serverPanel.add(panel1);
        serverPanel.add(panel2);


        north.add(serverPanel);


        JPanel clientPanel = new JPanel();
        clientPanel.setLayout(new GridLayout(2, 1, 10, 5));
        JPanel panel3 = new JPanel();
        JPanel panel4 = new JPanel();

        JButton btnConnect = new JButton("连接");
        textField3 = new JTextField(16);
        btnSend2 = new JButton("发送消息到服务端");
        btnClear2 = new JButton("清空");
        panel3.add(btnConnect);
        panel4.add(textField3);
        panel4.add(btnSend2);
        panel4.add(btnClear2);
        btnConnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SimpleClientTest.connect(3);//请修改SimpleClientTest连接信息
            }
        });
        btnSend2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (serverFrameListener != null) {
                    serverFrameListener.onClickClientSend();
                }
            }
        });
        btnClear2.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                textField3.setText("");
                clientLogArea.setText("");
            }
        });
        clientPanel.add(panel3);
        clientPanel.add(panel4);

        north.add(clientPanel);

    }

    private void initCenter() {
        JPanel content = new JPanel();
        content.setLayout(new GridLayout(1, 2, 10, 5));
        add(content, BorderLayout.CENTER);

        JPanel serverContentPanel = new JPanel();
        serverContentPanel.setLayout(new BorderLayout());
        serverContentPanel.setBorder(new TitledBorder(null, "服务端消息", TitledBorder.LEADING, TitledBorder.TOP, new Font("monospaced", Font.PLAIN, 12), null));
        serverLogArea = new JTextArea();
        serverLogArea.setLineWrap(true);
        serverLogArea.setFont(new Font("monospaced", Font.PLAIN, 14));

        jScrollPane = new JScrollPane(serverLogArea);

        serverContentPanel.add(jScrollPane);
        content.add(serverContentPanel);

        JPanel clientContentPanel = new JPanel();
        clientContentPanel.setLayout(new BorderLayout());
        clientContentPanel.setBorder(new TitledBorder(null, "客服端端消息", TitledBorder.LEADING, TitledBorder.TOP, new Font("monospaced", Font.PLAIN, 12), null));
        clientLogArea = new JTextArea();
        clientLogArea.setLineWrap(true);
        clientLogArea.setFont(new Font("monospaced", Font.PLAIN, 14));

        jScrollPane2 = new JScrollPane(clientLogArea);
        clientContentPanel.add(jScrollPane2);
        content.add(clientContentPanel);
    }


    private ServerFrameListener serverFrameListener;

    public void setServerFrameListener(ServerFrameListener serverFrameListener) {
        this.serverFrameListener = serverFrameListener;
    }

    public interface ServerFrameListener {
        void onClickStart();

        void onClickStop();

        void onClickServerSend();

        void onClickClientSend();
    }


}