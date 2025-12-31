package client;

import common.OpCodes; // Import correto
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.*;

public class ClientLib implements AutoCloseable {
    private Demultiplexer conn;

    public ClientLib() throws IOException {
        this.conn = new Demultiplexer("localhost", 12345);
    }

    public String register(String username, String password) throws IOException {
        return sendAuthRequest(OpCodes.AUTH_REGISTER, username, password) ? "Registo OK" : "Erro de registo";
    }

    public String login(String username, String password) throws IOException {
        return sendAuthRequest(OpCodes.AUTH_LOGIN, username, password) ? "Login OK" : "Erro de login";
    }

    public boolean sendAuthRequest(int opCode, String username, String password) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeUTF(username);
        dos.writeUTF(password);
        byte[] response = conn.sendRequest(opCode, out.toByteArray());
        return new DataInputStream(new ByteArrayInputStream(response)).readBoolean();
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
        return success ? "Venda Registada" : "Erro (Dia fechado ou dados invalidos)";
    }

    public String novoDia() throws IOException {
        byte[] response = conn.sendRequest(OpCodes.NEW_DAY, new byte[0]);
        boolean success = new DataInputStream(new ByteArrayInputStream(response)).readBoolean();
        return success ? "Novo dia iniciado" : "Erro ao mudar dia";
    }

    public int getQuantidade(String produto, int dias) throws IOException {
        byte[] response = sendAggRequest(OpCodes.AGG_COUNT, produto, dias);
        return new DataInputStream(new ByteArrayInputStream(response)).readInt();
    }

    public double getVolume(String produto, int dias) throws IOException {
        byte[] response = sendAggRequest(OpCodes.AGG_VOLUME, produto, dias);
        return new DataInputStream(new ByteArrayInputStream(response)).readDouble();
    }

    public double getPrecoMedio(String produto, int dias) throws IOException {
        byte[] response = sendAggRequest(OpCodes.AGG_AVG, produto, dias);
        return new DataInputStream(new ByteArrayInputStream(response)).readDouble();
    }

    public double getPrecoMaximo(String produto, int dias) throws IOException {
        byte[] response = sendAggRequest(OpCodes.AGG_MAX, produto, dias);
        return new DataInputStream(new ByteArrayInputStream(response)).readDouble();
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

        // Envio do pedido (Igual ao que tinhas)
        dos.writeInt(dia);
        dos.writeInt(produtos.size());
        for (String produto : produtos) {
            dos.writeUTF(produto);
        }

        byte[] response = conn.sendRequest(OpCodes.GET_EVENTS, out.toByteArray());
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(response));

        // 1. Ler o dicionário (Descompressão)
        int numNomes = dis.readInt();
        String[] dicionario = new String[numNomes];
        for (int i = 0; i < numNomes; i++) {
            dicionario[i] = dis.readUTF();
        }

        // 2. Mapas para agregar os valores (Nome -> Valor)
        Map<String, Integer> qtdTotal = new HashMap<>();
        Map<String, Double> volTotal = new HashMap<>();

        // 3. Ler a lista de eventos e somar (Em vez de adicionar logo à lista final)
        int numEventos = dis.readInt();
        for (int i = 0; i < numEventos; i++) {
            int nomeIndex = dis.readInt();
            int qtd = dis.readInt();
            double preco = dis.readDouble();

            String nomeProduto = dicionario[nomeIndex];

            // Acumula quantidade
            qtdTotal.put(nomeProduto, qtdTotal.getOrDefault(nomeProduto, 0) + qtd);
            // Acumula volume (qtd * preco) para depois calcularmos a média correta
            volTotal.put(nomeProduto, volTotal.getOrDefault(nomeProduto, 0.0) + (qtd * preco));
        }

        // 4. Gerar a lista final formatada (uma linha por produto)
        List<String> eventosFormatados = new ArrayList<>();

        for (String prod : qtdTotal.keySet()) {
            int q = qtdTotal.get(prod);
            double v = volTotal.get(prod);
            double precoMedio = (q > 0) ? (v / q) : 0.0;

            eventosFormatados.add(String.format("Produto: %s | Qtd Total: %d | Preço Médio: %.2f", prod, q, precoMedio));
        }

        return eventosFormatados;
    }
    public String subscreverVendasSimultaneas(String produto1, String produto2) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeUTF(produto1);
        dos.writeUTF(produto2);
        byte[] response = conn.sendRequest(OpCodes.NOTIFY_SIMUL, out.toByteArray());
        boolean success = new DataInputStream(new ByteArrayInputStream(response)).readBoolean();
        return success ? "Vendas simultaneas detetadas!" : "O dia acabou sem vendas simultaneas.";
    }

    public String subscreverVendasConsecutivas(String produto, int qtdNecessaria) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeUTF(produto);
        dos.writeInt(qtdNecessaria);
        byte[] response = conn.sendRequest(OpCodes.NOTIFY_CONSEC, out.toByteArray());

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(response));
        boolean success = dis.readBoolean();

        if (success) {
            String prodName = dis.readUTF();
            return qtdNecessaria + " vendas consecutivas de " + prodName + "!";
        } else {
            return "O dia acabou sem a sequencia.";
        }
    }

    @Override
    public void close() throws IOException {
        conn.close();
    }
}