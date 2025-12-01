package test;

import common.OpCodes;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MockServer {

    public static void main(String[] args) {
        System.out.println(">>> MOCK SERVER COMPLEXO INICIADO (PORTA 12345) <<<");
        try (ServerSocket ss = new ServerSocket(12345)) {
            while (true) {
                Socket s = ss.accept();
                new Thread(() -> handleClient(s)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket s) {
        try (DataInputStream in = new DataInputStream(s.getInputStream());
             DataOutputStream out = new DataOutputStream(s.getOutputStream())) {

            while (true) {
                // --- 1. LER HEADER ---
                int size = in.readInt();
                long id = in.readLong();
                int opCode = in.readInt();

                // Ler Payload
                byte[] payload = new byte[size - 12]; // Total - (Long + Int)
                in.readFully(payload);
                DataInputStream pIn = new DataInputStream(new ByteArrayInputStream(payload));

                System.out.println("OpCode recebido: " + opCode + " (ID " + id + ")");

                // --- 2. PREPARAR RESPOSTA (Baseado no OpCode) ---
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream pOut = new DataOutputStream(baos);

                switch (opCode) {
                    case OpCodes.AUTH_REGISTER:
                    case OpCodes.AUTH_LOGIN:
                    case OpCodes.ADD_EVENT:
                    case OpCodes.NEW_DAY:
                        // Resposta simples: Boolean
                        pOut.writeBoolean(true);
                        break;

                    case OpCodes.AGG_COUNT:
                        // Retorna um Inteiro (Ex: 150 vendas)
                        pOut.writeInt(150);
                        break;

                    case OpCodes.AGG_VOLUME:
                    case OpCodes.AGG_AVG:
                    case OpCodes.AGG_MAX:
                        // Retorna um Double (Ex: 99.99)
                        pOut.writeDouble(99.99);
                        break;

                    case OpCodes.GET_EVENTS:
                        // --- SIMULAÇÃO DA SERIALIZAÇÃO COMPACTA ---
                        // Cenário: 2 Produtos ("Batata", "Arroz"), 3 Vendas.

                        // 1. Dicionário
                        pOut.writeInt(2); // Tamanho do Dicionário
                        pOut.writeUTF("Batata"); // Index 0
                        pOut.writeUTF("Arroz");  // Index 1

                        // 2. Lista de Eventos (IndexProduto, Qtd, Preço)
                        pOut.writeInt(3); // Número de eventos

                        // Evento A: Batata (0)
                        pOut.writeInt(0); pOut.writeInt(10); pOut.writeDouble(1.5);
                        // Evento B: Arroz (1)
                        pOut.writeInt(1); pOut.writeInt(5); pOut.writeDouble(2.0);
                        // Evento C: Batata (0)
                        pOut.writeInt(0); pOut.writeInt(2); pOut.writeDouble(1.5);
                        break;

                    case OpCodes.NOTIFY_SIMUL:
                    case OpCodes.NOTIFY_CONSEC:
                        // Simular Bloqueio (Espera 2 segundos antes de responder)
                        System.out.println("   ... bloqueado à espera de notificação ...");
                        try { Thread.sleep(2000); } catch (InterruptedException e) {}
                        pOut.writeBoolean(true); // Aconteceu!
                        break;

                    default:
                        System.out.println("OpCode desconhecido!");
                }

                byte[] responseData = baos.toByteArray();

                // --- 3. ENVIAR RESPOSTA (Size -> ID -> OpCode -> Data) ---
                out.writeInt(8 + 4 + responseData.length);
                out.writeLong(id);
                out.writeInt(0); // Sucesso
                out.write(responseData);
                out.flush();
            }

        } catch (IOException e) {
            System.out.println("Cliente saiu.");
        }
    }
}