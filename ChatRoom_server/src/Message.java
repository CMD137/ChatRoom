import java.text.SimpleDateFormat;
import java.util.Date;

public class Message {
    public String content;;
    public String senderName;
    public String time;

    //-1:错误提醒(昵称重复)；-2：被踢出提醒
    // 0:消息(默认)
    //1:登陆确认；
    //2.关闭信号:server收到来自客户端的2，向所有客户端发送【离开了聊天室】(这条信息type=0)。
    //         client收到来自server的2，是server的关闭信号提醒。
    public int type  = 0;

    public Message(String content, String senderName, int type) {
        this.content = content;
        this.senderName = senderName;
        this.time =  new SimpleDateFormat("HH:mm:ss").format(new Date());
        this.type = type;
    }

    public Message(String content, String senderName) {
        this.content = content;
        this.senderName = senderName;
        this.time =  new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    //拷贝用
    public Message(String content, String senderName, String time, int type ){
        this.content = content;
        this.senderName = senderName;
        this.time = time;
        this.type = type;
    }

    //带上时间昵称前缀
    public String formatContent() {

        //支持多行信息：为保持格式，将前缀替换为等效宽度的空格。
        //获得前缀的长度
        int characterCount = getCharacterCount(("【" + time + "，" + senderName + "】:"));
        StringBuilder space = new StringBuilder();
        for (int i =0;i<characterCount;i++)
            space.append("  ");
        //在每个换行符后加上前缀等效的空格
        String newContent = content.replace("\n", "\n" + space);

        return "【"+time+"，"+senderName+"】:"+newContent+"\n";
    }



    // 序列化方法
    public String serialize() {
        content = content.replace("\n", "\\n");
        return String.format("%s$%s$%s$%d",
                content,
                senderName,
                time,
                type);
    }

    //反序列化方法
    public static Message deserialize(String serializedMsg) {
        String[] parts = serializedMsg.split("\\$", 4);

        String content = parts[0];
        content = content.replace("\\n", "\n");// 还原换行符
        String senderName = parts[1];
        String time = parts[2];
        int type = Integer.parseInt(parts[3]);

        Message message = new Message(content, senderName,time,type);
        return message;
    }

    private int getCharacterCount(String str) {
        int width = 0;
        for (char c : str.toCharArray()) {
            // 中文字符范围：0x4E00-0x9FA5
            if (c >= 0x4E00 && c <= 0x9FA5) {
                width += 2;
            } else {
                width += 1;
            }
        }
        return width;
    }
}
