package test;

import common.OpCodes;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MockServer {

    public static void main(String[] args) {
        System.out.println(">>> MOCK SERVER INICIADO NA PORTA 12345 <<<");
        System.out.println("Esperando conexão do Cliente...");

        try (ServerSocket ss = new ServerSocket(12345)) {
            while (true) {
                Socket s = ss.accept();
                System.out.println("Novo cliente conectado!");

                // Cria uma thread para atender este cliente
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
                // --- 1. LER PEDIDO (Protocolo: Size -> ID -> OpCode -> Data) ---

                // O teu ConnectionManager envia o tamanho primeiro
                int size = in.readInt();

                // Ler ID e OpCode
                long id = in.readLong();
                int opCode = in.readInt();

                // Calcular tamanho do payload (Size total - 8 bytes do ID - 4 bytes do OpCode)
                int payloadSize = size - 8 - 4;
                byte[] payload = new byte[payloadSize];
                in.readFully(payload);

                System.out.println("Recebido Pedido -> ID: " + id + " | OpCode: " + opCode);

                // --- 2. PROCESSAR (Fingir que fazemos algo) ---
                boolean sucesso = true;

                // Exemplo simples: Descodificar o payload só para mostrar no log
                DataInputStream payloadIn = new DataInputStream(new ByteArrayInputStream(payload));
                if (opCode == OpCodes.LOGIN) {
                    String user = payloadIn.readUTF();
                    System.out.println("   > Tentativa de Login: " + user);
                } else if (opCode == OpCodes.ADD_VENDA) {
                    String prod = payloadIn.readUTF();
                    int qtd = payloadIn.readInt();
                    System.out.println("   > Venda: " + qtd + "x " + prod);
                }

                // --- 3. ENVIAR RESPOSTA (Protocolo: Size -> ID -> Data) ---

                // Preparar a resposta (um boolean true)
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeBoolean(sucesso); // Responde sempre TRUE
                byte[] responseData = baos.toByteArray();

                // Enviar de volta
                // IMPORTANTE: O teu ConnectionManager espera (Size -> ID -> Data)
                out.writeInt(8 + 4 +responseData.length); // Tamanho (ID + Dados)
                out.writeLong(id);                     // O MESMO ID que recebemos
                out.writeInt(0);
                out.write(responseData);               // O boolean
                out.flush();

                System.out.println("   < Resposta enviada (ID " + id + ")");
            }

        } catch (EOFException e) {
            System.out.println("Cliente desconectou-se.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}