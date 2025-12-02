package client;

import common.OpCodes;
import java.net.Socket;
import java.io.*;

public class TestClient {
    public static void main(String[] args) {
        System.out.println("--- CLIENTE COMPLETO (Login + Venda + Consulta) ---");

        try (Socket socket = new Socket("localhost", 12345);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            // --- 1. REGISTAR ---
            System.out.println("\n--- 1. REGISTO ---");
            byte[] pReg = criarPayloadLogin("Maciel", "pass123");
            enviarPedido(out, 1, OpCodes.AUTH_REGISTER, pReg);
            lerResposta(in);

            // --- 2. LOGIN ---
            System.out.println("\n--- 2. LOGIN ---");
            byte[] pLog = criarPayloadLogin("Maciel", "pass123");
            enviarPedido(out, 2, OpCodes.AUTH_LOGIN, pLog);
            lerResposta(in);

            // --- 3. VENDER 5 TECLADOS ---
            System.out.println("\n--- 3. VENDER (5 Teclados a 100.0) ---");
            byte[] pVenda = criarPayloadVenda("Teclado", 5, 100.0);
            enviarPedido(out, 3, OpCodes.ADD_EVENT, pVenda);
            lerResposta(in);

            // --- 4. CONSULTAR SOMA (NOVO!) ---
            // Queremos saber quanto faturámos hoje com "Teclado"
            System.out.println("\n--- 4. CONSULTAR FATURAÇÃO (OpCode 5) ---");

            // Payload: [String Produto] [Int Dia]
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream temp = new DataOutputStream(baos);
            temp.writeUTF("Teclado");
            temp.writeInt(0); // Dia 0 = Hoje
            temp.flush();

            enviarPedido(out, 4, OpCodes.AGG_VOLUME, baos.toByteArray());

            // Ler resposta (que agora é um Double!)
            lerRespostaDouble(in);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- MÉTODOS AUXILIARES ---
    private static byte[] criarPayloadLogin(String user, String pass) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream temp = new DataOutputStream(baos);
        temp.writeUTF(user);
        temp.writeUTF(pass);
        return baos.toByteArray();
    }

    private static byte[] criarPayloadVenda(String prod, int qtd, double preco) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream temp = new DataOutputStream(baos);
        temp.writeUTF(prod);
        temp.writeInt(qtd);
        temp.writeDouble(preco);
        return baos.toByteArray();
    }

    private static void enviarPedido(DataOutputStream out, long tag, int opCode, byte[] payload) throws IOException {
        out.writeInt(8 + 4 + payload.length);
        out.writeLong(tag);
        out.writeInt(opCode);
        if (payload.length > 0) out.write(payload);
        out.flush();
    }

    private static void lerResposta(DataInputStream in) throws IOException {
        in.readInt(); // Length
        long tag = in.readLong();
        int code = in.readInt();
        System.out.println("   -> Resposta Tag " + tag + ": " + (code == 0 ? "✅ OK" : "❌ ERRO " + code));
    }

    private static void lerRespostaDouble(DataInputStream in) throws IOException {
        in.readInt(); // Length
        long tag = in.readLong();
        int code = in.readInt();
        double valor = in.readDouble(); // A leitura extra!
        System.out.println("   -> 💰 TOTAL FATURADO (Tag " + tag + "): " + valor + "€");
    }
}