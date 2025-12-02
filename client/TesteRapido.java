package client;
import common.OpCodes;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

public class TesteRapido {
    public static void main(String[] args) throws Exception {
        Socket s = new Socket("localhost", 12345);
        TaggedConnection conn = new TaggedConnection(s);

        conn.sendVenda(1, "Pao", 10, 1.5);

        Thread.sleep(1000);
    }
}