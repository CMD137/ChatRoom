import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class Server extends JFrame implements ActionListener {
    //组件
    protected JTextField portField;
    protected JTextArea logArea;
    protected JButton startBtn, stopBtn, kickBtn, sendBtn;
    protected JTextField kickNameField;
    protected JTextField clientListField;
    protected JTextArea messageArea;

    // 数据
    protected ServerSocket serverSocket;
    protected volatile boolean isRunning = false;
    protected int PORT = 10086; // 默认端口
    protected List<ClientHandler> clients = new ArrayList<>(); // 客户端连接列表
    protected Deque<Message> msgQueen = new LinkedList<>(); // 消息队列
    protected inspectorThread inspector;
    protected ListeningThread listener;


    //初始化图形界面
    public Server() {
        setTitle("聊天室管理面板      计算机231-220501208-邓旭昌");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 顶部控制面板
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout());

        // 第一行：端口和按钮控制
        JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        portField = new JTextField("" + PORT, 10);
        startBtn = new JButton("启动服务");
        stopBtn = new JButton("关停服务");
        kickBtn = new JButton("踢出用户:");
        kickNameField = new JTextField(25);

        firstRow.add(new JLabel("端口号:"));
        firstRow.add(portField);
        firstRow.add(startBtn);
        firstRow.add(stopBtn);
        firstRow.add(kickBtn);
        firstRow.add(new JLabel("昵称:"));
        firstRow.add(kickNameField);

        // 第二行：客户列表
        JPanel secondRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        clientListField = new JTextField(78); // 设置适当宽度
        clientListField.setEditable(false); // 设置为只读
        secondRow.add(new JLabel("在线用户:"));
        secondRow.add(clientListField);

        // 将两行添加到控制面板
        controlPanel.add(firstRow, BorderLayout.NORTH);
        controlPanel.add(secondRow,  BorderLayout.SOUTH);

        add(controlPanel, BorderLayout.NORTH);

        // 日志显示区域
        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // 底部消息输入面板
        JPanel bottomPanel = new JPanel(new BorderLayout());
        messageArea = new JTextArea(3, 60); // 3行高度，60列宽度
        messageArea.setLineWrap(true); // 自动换行
        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        sendBtn = new JButton("发送");
        bottomPanel.add(messageScrollPane, BorderLayout.CENTER);
        bottomPanel.add(sendBtn, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // 按钮事件监听
        startBtn.addActionListener(this);
        stopBtn.addActionListener(this);
        kickBtn.addActionListener(this);
        sendBtn.addActionListener(this);

        // 组件初始状态
        enableControls(false);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    //主入口
    public static void main(String[] args) {
        Server server = new Server();
    }

    //更新用户列表
    protected synchronized void updateClientList() {
        StringBuilder sb = new StringBuilder();
        for (ClientHandler client : clients) {
            if (!client.isClosed()) {
                sb.append(client.nickname).append(", ");
            }
        }
        // 移除最后的逗号和空格
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2);
        }
        clientListField.setText(sb.toString());
    }

    //集中管理组件状态
    protected void enableControls(boolean status) {
        // 端口框、启动按钮与其他组件逻辑相反
        portField.setEditable(!status);
        startBtn.setEnabled(!status);
        stopBtn.setEnabled(status);
        kickNameField.setEnabled(status);
        kickBtn.setEnabled(status);
        messageArea.setEnabled(status);
        sendBtn.setEnabled(status);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startBtn) {
            startServer(e);
        } else if (e.getSource() == stopBtn) {
            stopServer(e);
        } else if (e.getSource() == kickBtn) {
            kickClient(e);
        } else if (e.getSource() == sendBtn) {
            sendAdminMessage();
        }
    }

    //管理员发送信息
    protected void sendAdminMessage() {
        String content = messageArea.getText().trim();
        if (!content.isEmpty()) {
            Message msg = new Message(content,  "管理员");
            //为了格式化
            String temp=msg.serialize();
            msg=Message.deserialize(temp);

            msgQueen.add(msg);
            messageArea.setText("");
        }
    }

    // 启动服务器
    private void startServer(ActionEvent e) {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            serverSocket = new ServerSocket(port);
            isRunning = true;
            enableControls(true);
            log("服务器已启动，端口：" + port);

            // 启动迎宾线程
            listener = new ListeningThread(this);
            listener.start();

            // 启动巡检线程
            inspector = new inspectorThread(this);
            inspector.start();

        } catch (Exception ex) {
            log("启动失败：" + ex.getMessage());
        }
    }

    // 关停服务器
    private void stopServer(ActionEvent e) {
        enableControls(false);
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                log("服务器关闭");
                Message msg = new Message("服务器将关闭，所有连接将断开", "管理员",2);
                msgQueen.addFirst(msg);

                //等待1s让inspector发送完关闭消息
                Thread.sleep(1000);

                // 断开所有客户端连接
                for (ClientHandler client : clients) {
                    client.isClosed=true;
                }
                serverSocket.close();
            }
        } catch (Exception ex) {
            log("关停异常：" + ex.getMessage());
        }
        isRunning = false;
    }

    // 踢出指定用户
    private void kickClient(ActionEvent e) {
        String nickname = kickNameField.getText().trim();
        if (nickname.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入要踢出的昵称！");
            return;
        }

        ClientHandler target = null;
        for (ClientHandler handler : clients) {
            if (handler.nickname.equals(nickname)) {
                target = handler;
                break;
            }
        }

        if (target != null) {
            Message getOut = new Message(target.nickname,"管理员",-2);
            msgQueen.add(getOut);
            Message msg = new Message("【因违规被踢出群聊室】", nickname);
            msgQueen.add(msg);
            log("已踢出用户：" + nickname);
            kickNameField.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "用户不存在！");
        }
    }

    // 日志输出，只有管理员可见
    protected void log(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        synchronized (logArea) {
            logArea.append("----------" + time + "，来自服务器的信息 \t：" + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }

}