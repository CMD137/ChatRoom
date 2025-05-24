import javax.swing.*;
import java.io.IOException;

public class ListeningThread extends Thread{
    Client client;

    public ListeningThread(Client client){
        this.client = client;
    }

    public void run(){
        while (client.isRunning){
            try {
                Thread.sleep(1);
                String readLine = client.reader.readLine();

                if (!client.isRunning)
                    return;

                Message in = Message.deserialize(readLine);
                if (in.type==-2){
                    //被踢
                    JOptionPane.showMessageDialog(client, "您已因违规而被提出聊天室！", "警告", JOptionPane.ERROR_MESSAGE);
                    client.exitChatRoom();
                }else if (in.type==0){
                    synchronized (client.chatArea){
                        client.chatArea.append(in.formatContent());
                    }
                }else if (in.type==2){
                    //关闭信号
                    client.log("收到服务器关闭信号，断开连接");
                    client.exitChatRoom();
                }
            } catch (IOException e) {
                //一般由服务器直接关闭软件或先断开连接引起
                if (client.isRunning){
                    client.log("收到服务器关闭信号，断开连接");
                    client.exitChatRoom();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
