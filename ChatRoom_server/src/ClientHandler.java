import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread{
    protected Socket socket;
    protected BufferedReader reader;
    protected PrintWriter writer;
    protected String nickname;
    protected boolean isClosed = false;

    private Server server;
    public ClientHandler(Server server,Socket socket,String nickname) throws IOException {
        this.socket = socket;
        this.server = server;
        this.nickname=nickname;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException ex) {
            ex.printStackTrace();
            close();
        }
    }

    public void run() {
        while (server.isRunning&&!isClosed){
            try {
                String inmsg =reader.readLine();
                //System.out.println(inmsg);
                Message in= Message.deserialize(inmsg);
                if (in.type==0){
                    server.msgQueen.add(in);
                }else if(in.type==2){
                    Message msg=new Message("【离开了聊天室】",nickname);
                    server.msgQueen.add(msg);
                    isClosed=true;
                }
            } catch (IOException e) {
                //目前检查为客户端没有退出聊天室，直接关闭导致的
                server.log(socket.getInetAddress()+Integer.toString(socket.getPort())+"的对话异常断开");
                System.out.println("对话异常断开");
                Message msg=new Message("【离开了聊天室】",nickname);
                server.msgQueen.add(msg);
                isClosed=true;
            }
        }
    }

    // 关闭连接
    public void close() {
        if (isClosed) return;
        isClosed = true;
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (IOException ex) {
            server.log("连接关闭异常：" + ex.getMessage());
        }
    }

    // 检查连接状态
    public boolean isClosed() {
        return isClosed || socket.isClosed();
    }

    // 发送消息给客户端
    public void sendMessage(String message) {
        if (!isClosed && writer != null) {
            writer.println(message);
        }
    }
}



