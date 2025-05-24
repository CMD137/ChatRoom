import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

//迎宾线程
public class ListeningThread extends Thread{
    Server server;
    public ListeningThread(Server server){
        this.server = server;
    }

    public void run(){
        while (server.isRunning) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Socket clientSocket = null;
            try {
                //创建连接
                clientSocket = server.serverSocket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));;
                PrintWriter writer=new PrintWriter(clientSocket.getOutputStream(), true);
                server.log("新客户端连接：" + clientSocket.getInetAddress()+":"+clientSocket.getPort());

                //规定第一条信息的内容是客户端的昵称，检查如果昵称已存在，则返回发送错误信息并拒绝连接
                Message in = Message.deserialize(reader.readLine());
                for (ClientHandler handler : server.clients) {
                    if (handler.nickname.equals(in.content)) {
                        Message out = new Message("昵称已存在", "管理员", -1);
                        writer.println(out.serialize());
                        writer.close();
                        server.log(clientSocket.getInetAddress()+":"+clientSocket.getPort()+"的昵称重复，已拒绝连接：" + in.content);
                        throw new IOException("重复昵称：" + in.content);
                    }

                }

                Message out = new Message("昵称已确认", "管理员", 1);
                server.log(clientSocket.getInetAddress()+":"+clientSocket.getPort()+"的昵称已确认：" + in.content+"\t成功链接");
                writer.println(out.serialize());


                // 创建客户端处理线程
                ClientHandler handler = new ClientHandler(server,clientSocket,in.senderName);
                server.clients.add(handler);
                handler.start();
                server.updateClientList();

                // 将上线消息加入消息队列
                Message msg = new Message("【进入了聊天室】", in.senderName, in.time, in.type);
                server.msgQueen.add(msg);

            } catch (IOException ex) {
                if (server.isRunning) {
                    server.log("迎宾线程异常：" + ex.getMessage());
                    if(clientSocket != null && !clientSocket.isClosed()) {
                        try {
                            clientSocket.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    }
}
