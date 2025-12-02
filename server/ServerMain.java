package server;
import java.net.ServerSocket;
import java.net.Socket;
import common.IEngine;




public class ServerMain {
    public static void main(String[] args) throws Exception {
        int port = 12345;
        ServerSocket ss = new ServerSocket(port);
        IEngine engine = new TimeSeriesEngine();
        UserManager userManager = new UserManager();

        System.out.println("Servidor iniciado na porta " + port + "...");

        while (true) {
            Socket s = ss.accept();
            // Lança uma thread para cada cliente
            new Thread(new Session(s, engine, userManager)).start();
        }
    }
}
