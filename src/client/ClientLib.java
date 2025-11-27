package client;

import common.OpCodes;

import java.io.*;

public class ClientLib implements AutoCloseable {
    private Demultiplexer conn;

    public ClientLib() throws IOException {
        this.conn = new Demultiplexer("localhost",12345);
    }

    public String login(String username, String password) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        dos.writeUTF(username);
        dos.writeUTF(password);


        byte[] response = conn.sendRequest(OpCodes.LOGIN, out.toByteArray());

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(response));
        boolean success = dis.readBoolean();
        return success ? "Login OK" : "Erro no login";
    }

    public String addVenda(String produto, int qtd, double preco) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        dos.writeUTF(produto);
        dos.writeInt(qtd);
        dos.writeDouble(preco);


        byte[] response = conn.sendRequest(OpCodes.ADD_VENDA, out.toByteArray());

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(response));
        boolean success = dis.readBoolean();
        return success ? "Add Venda OK" : "Erro ao adicionar Venda";
    }

    @Override
    public void close() throws IOException {
        conn.close();
    }


}