package client;

import common.OpCodes;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ClientLib implements AutoCloseable {
    private Demultiplexer conn;

    public ClientLib() throws IOException {
        this.conn = new Demultiplexer("localhost",12345);
    }

    public String register(String username, String password) throws IOException {
        return sendAuthRequest(OpCodes.AUTH_REGISTER,username, password) ? "Registo OK" : "Erro de registo";
    }

    public String login(String username, String password) throws IOException {
        return sendAuthRequest(OpCodes.AUTH_LOGIN,username, password) ? "Registo OK" : "Erro de registo";
    }

    public boolean sendAuthRequest(int opCode, String username, String password) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        dos.writeUTF(username);
        dos.writeUTF(password);


        byte[] response = conn.sendRequest(opCode, out.toByteArray());

        return new DataInputStream (new ByteArrayInputStream(response)).readBoolean();

    }

    public String addVenda(String produto, int qtd, double preco) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        dos.writeUTF(produto);
        dos.writeInt(qtd);
        dos.writeDouble(preco);


        byte[] response = conn.sendRequest(OpCodes.ADD_EVENT, out.toByteArray());

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(response));
        boolean success = dis.readBoolean();
        return success ? "Venda Registada" : "Erro (Dia dechado ou dados invalidos)";
    }

    public String novoDia() throws IOException {
        byte[] response = conn.sendRequest(OpCodes.NEW_DAY, new byte[0]);
        boolean success = new DataInputStream (new ByteArrayInputStream(response)).readBoolean();
        return success ? "Novo dia iniciado" : "Erro ao mudar dia";
    }

    public int getQuantidade(String produto, int dias) throws IOException {
        byte[] response = sendAggRequest(OpCodes.AGG_COUNT, produto, dias);
        return new DataInputStream (new ByteArrayInputStream(response)).readInt();
    }

    public double getVolume(String produto, int dias) throws IOException {
        byte[] response = sendAggRequest(OpCodes.AGG_VOLUME, produto, dias);
        return new DataInputStream (new ByteArrayInputStream(response)).readDouble();
    }

    public double getPrecoMedio(String produto, int dias) throws IOException {
        byte[] response = sendAggRequest(OpCodes.AGG_AVG, produto, dias);
        return new DataInputStream (new ByteArrayInputStream(response)).readDouble();
    }

    public double getPrecoMaximo(String produto, int dias) throws IOException {
        byte[] response = sendAggRequest(OpCodes.AGG_MAX, produto, dias);
        return new DataInputStream (new ByteArrayInputStream(response)).readDouble();
    }

    private byte[] sendAggRequest(int opCode, String produto, int dias) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeUTF(produto);
        dos.writeInt(dias);

        return conn.sendRequest(opCode, out.toByteArray());
    }

    public List<String> getEventos(Set<String> produtos, int dia) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        dos.writeInt(dia);
        dos.writeInt(produtos.size());
        for (String produto : produtos) {
            dos.writeUTF(produto);
        }

        byte[] response = conn.sendRequest(OpCodes.GET_EVENTS, out.toByteArray());
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(response));

        List<String> eventos = new ArrayList<>();

        int numNomes = dis.readInt();
        String[] dicionario = new  String[numNomes];
        for (int i = 0; i < numNomes; i++) {
            dicionario[i] = dis.readUTF();
        }

        int numEventos = dis.readInt();
        for(int i = 0; i < numEventos; i++) {
            int nomeIndex = dis.readInt();
            int qtd = dis.readInt();
            double preco = dis.readDouble();

            String nomeProduto = dicionario[nomeIndex];
            eventos.add(String.format("Venda: %s | Qtd: %d | Preco: %.2f", nomeProduto, qtd, preco));
        }

        return eventos;
    }

    public String subscreverVendasSimultaneas(String produto1, String produto2) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeUTF(produto1);
        dos.writeUTF(produto2);

        byte[] response = conn.sendRequest(OpCodes.NOTIFY_SIMUL, out.toByteArray());
        boolean success = new DataInputStream (new ByteArrayInputStream(response)).readBoolean();

        return success ? "Vendas simultaneas detetadas!" : "O dia acabou sem vendas simultaneas.";
    }

    public String subscreverVendasConsecutivas(String produto, int qtdNecessaria) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeUTF(produto);
        dos.writeInt(qtdNecessaria);

        byte[] response = conn.sendRequest(OpCodes.NOTIFY_CONSEC, out.toByteArray());
        boolean success = new DataInputStream (new ByteArrayInputStream(response)).readBoolean();

        return success ? qtdNecessaria + " vendas consecutivas de " + produto + "!" : "O dia acabou sem a sequencia.";
    }


    @Override
    public void close() throws IOException {
        conn.close();
    }


}