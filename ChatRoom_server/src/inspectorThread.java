import java.util.Iterator;

public class inspectorThread extends Thread{
    Server server;
    public inspectorThread(Server server){
        this.server = server;
    }



    public void run(){
        while (server.isRunning) {
            try {
                Thread.sleep(1);
                //1.检查所有客户端，清理失效的的客户端
                Iterator<ClientHandler> iterator = server.clients.iterator();
                while (iterator.hasNext()) {
                    ClientHandler client = iterator.next();
                    if (client.isClosed()) {
                        iterator.remove();
                        server.updateClientList();
                    }
                }

                //2.将消息队列中的消息发送给所有客户端
                while (server.msgQueen.size() > 0) {
                    Message message = server.msgQueen.poll();
                    if (message.type==-2){
                        //踢出信号私发
                          for (ClientHandler client : server.clients) {
                            if (client.nickname.equals(message.content)){
                                client.sendMessage(message.serialize());
                                client.isClosed=true;
                            }
                          }
                          continue;
                    }//正常信息
                    //给自己看的
                    synchronized (server.logArea){
                        server.logArea.append(message.formatContent());
                        //test
                        System.out.println("formatContent:\n"+message.formatContent());
                        String temp=message.serialize();
                        Message tm=Message.deserialize(temp);
                        System.out.println("deserialize:\n"+tm.formatContent());
                        //
                    }

                    for (ClientHandler client : server.clients) {
                        client.sendMessage(message.serialize());
                    }
                }

            } catch (InterruptedException ex) {
                if (server.isRunning) {
                    server.log("巡检线程中断：" + ex.getMessage());
                }
            }
        }
    }
}
