package server;

import common.IEngine;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    public static void main(String[] args) throws Exception {
        int port = 12345;
        int janelaD = 0; // 0 = Sem limite (ou default)
        int maxS = 3;    // Default: 3 dias em memória

        // LER ARGUMENTOS: java ServerMain <PORT> <D> <S>
        try {
            if (args.length >= 1) port = Integer.parseInt(args[0]);
            if (args.length >= 2) janelaD = Integer.parseInt(args[1]);
            if (args.length >= 3) maxS = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.err.println("Uso incorreto. Exemplo: java ServerMain 12345 30 3");
            return;
        }

        ServerSocket ss = new ServerSocket(port);

        // Passamos D e S para o motor
        IEngine engine = new TimeSeriesEngine(janelaD, maxS);
        UserManager userManager = new UserManager();

        // Hook para gravar se fechar com Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("\nA encerrar servidor...");
                ((TimeSeriesEngine) engine).encerrar();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        System.out.println(">>> SERVIDOR INICIADO NA PORTA " + port + " <<<");
        System.out.println(">>> JANELA TEMPORAL (D): " + (janelaD > 0 ? janelaD + " dias" : "Infinito"));
        System.out.println(">>> MEMORIA MAXIMA  (S): " + maxS + " dias");

        while (true) {
            Socket s = ss.accept();
            new Thread(new Session(s, engine, userManager)).start();
        }
    }
}