package huige233.transcend.util;

public interface SataHdd extends A,B{
    // 连接线的数量
    public static final int CONNECT_LINE = 4;
    // 写数据
    public void writeData(String data);
    // 读数据
    public String readData();
}
interface A{
    public void a();
}
interface B{
    public void b();
}
