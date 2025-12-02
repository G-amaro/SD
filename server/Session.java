package server;

import common.OpCodes;
import common.Sale;
import java.io.*;
import java.net.Socket;
import java.util.*;

public class Session implements Runnable {
    private final Socket socket;
    private final TimeSeriesEngine engine;
    private final UserManager userManager;
    private String currentUser = null;

    public Session(Socket socket, common.IEngine engine, UserManager userManager) {
        this.socket = socket;
        this.engine = (TimeSeriesEngine) engine; // Cast necessário
        this.userManager = userManager;
    }

    @Override
    public void run() {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

            while (true) {
                int length = in.readInt();
                long tag = in.readLong();
                int opCode = in.readInt();

                switch (opCode) {
                    case OpCodes.AUTH_REGISTER:
                        boolean regOk = userManager.registar(in.readUTF(), in.readUTF());
                        sendResponseBoolean(out, tag, regOk);
                        break;

                    case OpCodes.AUTH_LOGIN:
                        String u = in.readUTF();
                        if (userManager.autenticar(u, in.readUTF())) {
                            this.currentUser = u;
                            sendResponseBoolean(out, tag, true);
                        } else {
                            sendResponseBoolean(out, tag, false);
                        }
                        break;

                    case OpCodes.ADD_EVENT:
                        String prod = in.readUTF();
                        int qtd = in.readInt();
                        double preco = in.readDouble();
                        if (currentUser != null) {
                            engine.registarVenda(prod, qtd, preco);
                            sendResponseBoolean(out, tag, true);
                        } else {
                            sendResponseBoolean(out, tag, false);
                        }
                        break;

                    case OpCodes.NEW_DAY:
                        engine.avancarDia();
                        sendResponseBoolean(out, tag, true);
                        break;

                    // --- AGREGAÇÕES ---
                    case OpCodes.AGG_COUNT:
                        sendResponseInt(out, tag, engine.getQuantidade(in.readUTF(), in.readInt()));
                        break;
                    case OpCodes.AGG_VOLUME:
                        sendResponseDouble(out, tag, engine.getVolume(in.readUTF(), in.readInt()));
                        break;
                    case OpCodes.AGG_AVG:
                        sendResponseDouble(out, tag, engine.getMedia(in.readUTF(), in.readInt()));
                        break;
                    case OpCodes.AGG_MAX:
                        sendResponseDouble(out, tag, engine.getMax(in.readUTF(), in.readInt()));
                        break;

                    // --- LISTA COM SERIALIZAÇÃO COMPACTA (O "FERRARI") ---
                    case OpCodes.GET_EVENTS:
                        int dia = in.readInt();
                        int numProds = in.readInt();
                        Set<String> prods = new HashSet<>();
                        for(int i=0; i<numProds; i++) prods.add(in.readUTF());

                        List<Sale> vendas = engine.getVendas(prods, dia);

                        // Preparar resposta COMPACTA
                        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
                        DataOutputStream dOut = new DataOutputStream(bOut);

                        // A. Criar Dicionário
                        List<String> dictList = new ArrayList<>(prods);
                        Map<String, Integer> dictMap = new HashMap<>();
                        dOut.writeInt(dictList.size()); // Tamanho Dict
                        for(int i=0; i<dictList.size(); i++) {
                            String s = dictList.get(i);
                            dOut.writeUTF(s);
                            dictMap.put(s, i);
                        }

                        // B. Escrever Eventos usando indices
                        dOut.writeInt(vendas.size());
                        for (Sale s : vendas) {
                            int idx = dictMap.getOrDefault(s.produto, -1);
                            if (idx != -1) {
                                dOut.writeInt(idx); // Índice em vez de String
                                dOut.writeInt(s.quantidade);
                                dOut.writeDouble(s.preco);
                            }
                        }
                        sendResponseBytes(out, tag, bOut.toByteArray());
                        break;

                    case OpCodes.NOTIFY_SIMUL:
                        String s1 = in.readUTF();
                        String s2 = in.readUTF();
                        // Simplificação: espera por um, depois pelo outro
                        boolean r1 = engine.esperarVenda(s1);
                        boolean r2 = engine.esperarVenda(s2);
                        sendResponseBoolean(out, tag, r1 && r2);
                        break;

                    case OpCodes.NOTIFY_CONSEC:
                        String sc = in.readUTF();
                        in.readInt(); // qtd ignorada na simplificação
                        engine.esperarVenda(sc);
                        sendResponseBoolean(out, tag, true);
                        break;

                    default:
                        in.skipBytes(length - 12);
                }
            }
        } catch (EOFException e) {
            // Cliente saiu
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helpers de Envio
    private void sendResponseBoolean(DataOutputStream out, long tag, boolean val) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        new DataOutputStream(b).writeBoolean(val);
        sendResponseBytes(out, tag, b.toByteArray());
    }

    private void sendResponseInt(DataOutputStream out, long tag, int val) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        new DataOutputStream(b).writeInt(val);
        sendResponseBytes(out, tag, b.toByteArray());
    }

    private void sendResponseDouble(DataOutputStream out, long tag, double val) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        new DataOutputStream(b).writeDouble(val);
        sendResponseBytes(out, tag, b.toByteArray());
    }

    private void sendResponseBytes(DataOutputStream out, long tag, byte[] data) throws IOException {
        synchronized(out) {
            out.writeInt(8 + 4 + data.length);
            out.writeLong(tag);
            out.writeInt(0);
            out.write(data);
            out.flush();
        }
    }
}