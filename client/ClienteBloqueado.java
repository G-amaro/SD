package client;

import common.OpCodes;
import java.io.*;
import java.net.Socket;

public class ClienteBloqueado {
    public static void main(String[] args) {
        System.out.println("--- CLIENTE BLOQUEADO (Com Login) ---");

        try (Socket socket = new Socket("localhost", 12345);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            // --- 1. AUTENTICAÇÃO (Obrigatória agora!) ---
            System.out.println("1. A autenticar como 'Espectador'...");

            // Regista (ignora se falhar, pode já existir)
            enviarAuth(out, 10, OpCodes.AUTH_REGISTER, "Espectador", "pass");
            lerResposta(in);

            // Login (Este tem de dar certo)
            enviarAuth(out, 11, OpCodes.AUTH_LOGIN, "Espectador", "pass");
            lerResposta(in); // Se der erro aqui, o resto falha

            // --- 2. PEDIDO DE BLOQUEIO ---
            String produtoAlvo = "Teclado";
            int qtdAlvo = 3;

            System.out.println("\n2. A pedir bloqueio (Esperar 3 Teclados)...");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream payloadOut = new DataOutputStream(baos);
            payloadOut.writeUTF(produtoAlvo);
            payloadOut.writeInt(qtdAlvo);
            payloadOut.flush();
            byte[] payload = baos.toByteArray();

            // Enviar Pedido (OpCode 9 - NOTIFY_SIMUL)
            int totalSize = 8 + 4 + payload.length;
            out.writeInt(totalSize);
            out.writeLong(999);
            out.writeInt(OpCodes.NOTIFY_SIMUL);
            out.write(payload);
            out.flush();

            System.out.println("⏳ 3. Pedido enviado. Estou BLOQUEADO à espera... (O programa NÃO deve avançar)");

            // --- 3. FICAR À ESPERA ---
            int respLen = in.readInt();
            long respTag = in.readLong();
            int respCode = in.readInt();

            System.out.println("🎉 4. ACORDEI! O servidor disse que as vendas aconteceram!");
            System.out.println("   Tag: " + respTag + " | Code: " + respCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Métodos auxiliares simples
    private static void enviarAuth(DataOutputStream out, long tag, int opCode, String u, String p) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream temp = new DataOutputStream(baos);
        temp.writeUTF(u);
        temp.writeUTF(p);
        byte[] payload = baos.toByteArray();

        out.writeInt(8 + 4 + payload.length);
        out.writeLong(tag);
        out.writeInt(opCode);
        out.write(payload);
        out.flush();
    }

    private static void lerResposta(DataInputStream in) throws IOException {
        in.readInt(); // Length
        in.readLong(); // Tag
        int code = in.readInt();
        if (code != 0) System.out.println("   ⚠️ Aviso: Resposta com código " + code);
    }
}