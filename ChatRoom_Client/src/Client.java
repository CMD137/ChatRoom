import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Client extends JFrame implements ActionListener {
    // 组件
    protected JTextField serverIPField;
    protected JTextField serverPortField;
    protected JTextField nicknameField;
    protected JTextArea chatArea;
    protected JTextArea messageArea;
    protected JButton enterBtn;
    protected JButton exitBtn;
    protected JButton sendBtn;

    // 数据
    protected Socket socket;
    protected BufferedReader reader;
    protected PrintWriter writer;
    protected String nickname;
    protected volatile boolean isRunning = false;
    protected ListeningThread  listeningThread;

    //默认IP：127.0.0.1
    protected String defaultIP = "127.0.0.1";
    protected int defaultPort = 10086;


    // 初始化图形界面
    public Client() {
        setTitle("聊天室客户端      计算机231-220501208-邓旭昌");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 顶部控制面板
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout());

        // 第一行：端口和昵称
        JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        serverIPField = new JTextField("" + defaultIP, 10);
        serverPortField = new JTextField("" + defaultPort, 10);
        nicknameField = new JTextField("", 10);
        firstRow.add(new JLabel("服务器IP："));
        firstRow.add(serverIPField);
        firstRow.add(new JLabel("服务器端口："));
        firstRow.add(serverPortField);
        firstRow.add(new JLabel("昵称："));
        firstRow.add(nicknameField);


        // 第二行：进出按钮
        JPanel secondRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        enterBtn = new JButton("进入聊天室");
        exitBtn = new JButton("退出聊天室");
        // 设置按钮大小
        Dimension buttonSize = new Dimension(250, 30);
        enterBtn.setPreferredSize(buttonSize);
        exitBtn.setPreferredSize(buttonSize);
        secondRow.add(enterBtn);
        secondRow.add(exitBtn);

        // 将两行添加到控制面板
        controlPanel.add(firstRow, BorderLayout.NORTH);
        controlPanel.add(secondRow, BorderLayout.SOUTH);

        add(controlPanel, BorderLayout.NORTH);

        //聊天信息框
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

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
        enterBtn.addActionListener(this);
        exitBtn.addActionListener(this);
        sendBtn.addActionListener(this);


        // 组件初始状态
        enableControls(true);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // 主入口
    public static void main(String[] args) {
        Client client = new Client();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == enterBtn) {
            enterChatRoom();
        }
        else if (e.getSource() == exitBtn) {
            exitChatRoom();
        }
        else if (e.getSource() == sendBtn) {
            send2Server();
        }
    }

    //集中管理组件状态
    protected void enableControls(boolean status) {
        // 退出、发送按钮与其他组件逻辑相反
        exitBtn.setEnabled(!status);
        messageArea.setEditable(!status);
        sendBtn.setEnabled(!status);

        enterBtn.setEnabled(status);
        nicknameField.setEnabled(status);
        serverIPField.setEnabled(status);
        serverPortField.setEnabled(status);
    }
    private void enterChatRoom() {
        nickname = nicknameField.getText().trim();
        if (nickname.equals("管理员")||nickname.isEmpty()) {
            JOptionPane.showMessageDialog(this, "昵称不能为空或为“管理员”", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String serverIP = serverIPField.getText().trim();
        String serverPort = serverPortField.getText().trim();
        if (serverIP.isEmpty() || serverPort.isEmpty()) {
            JOptionPane.showMessageDialog(this, "服务器IP或端口不能为空", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        //如果用户给出的端口号不是整数，不是[1024,65535]之间的动态端口号，都要提示用户。
        if (!serverPort.matches("[0-9]+") || Integer.parseInt(serverPort) < 1024 || Integer.parseInt(serverPort) > 65535) {
            JOptionPane.showMessageDialog(this, "请输入正确的端口号，要求在[1024,65535]之间", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            socket=new Socket(serverIP,Integer.parseInt(serverPort));
            reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer=new PrintWriter(socket.getOutputStream(),true);
        }catch (Exception e){
            JOptionPane.showMessageDialog(this, "未找到目标服务", "错误", JOptionPane.ERROR_MESSAGE);
            exitChatRoom();
            return;
        }

        //规定第一条信息发送昵称
        Message msg=new Message(nickname,nickname);
        writer.println(msg.serialize());
        try {
            Message in = Message.deserialize(reader.readLine());
            if (in.type==-1){
                //昵称重复
                JOptionPane.showMessageDialog(this, "昵称重复", "错误", JOptionPane.ERROR_MESSAGE);
                exitChatRoom();
            }else if (in.type==1){
                //登录确认
                isRunning=true;
                enableControls(false);
                String time= new SimpleDateFormat("HH:mm:ss").format(new Date());
                log("您已进入聊天室");
                listeningThread=new ListeningThread(this);
                listeningThread.start();
            }else {
                System.out.println("未预料的错误");
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "连接已关闭", "错误", JOptionPane.ERROR_MESSAGE);
        }


    }

    protected void exitChatRoom() {
        //System.out.println("exit");
        isRunning=false;
        Message exit =new Message("",nickname,2);
        String msg=exit.serialize();
        if(writer!=null){
            writer.println(msg);
            writer.close();
        }
        /*
        if (socket!=null){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        */
        log("您已退出聊天室");
        enableControls(true);
    }

    private void send2Server() {
        String content = messageArea.getText();
        /*// 检查是否包含换行符
        if (content.contains("\n") || content.contains("\r")) {
            JOptionPane.showMessageDialog(this, "消息不支持换行", "错误", JOptionPane.WARNING_MESSAGE);
            messageArea.setText("");
            return;
        }*/
        Message out =new Message(content,nickname);
        //System.out.println(out.serialize());
        writer.println(out.serialize());
        messageArea.setText("");
    }

    protected void log(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        synchronized (chatArea) {
            chatArea.append("----------" + time + "，来自客户端的信息 \t：" + msg + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
    }
}
