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
        this.engine = (TimeSeriesEngine) engine;
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
                        String uReg = in.readUTF();
                        String pReg = in.readUTF();
                        System.out.println("[AUTH] Novo registo: " + uReg);
                        boolean regOk = userManager.registar(uReg, pReg);
                        sendResponseBoolean(out, tag, regOk);
                        break;

                    case OpCodes.AUTH_LOGIN:
                        String u = in.readUTF();
                        String p = in.readUTF();
                        System.out.println("[AUTH] Tentativa de login: " + u);
                        if (userManager.autenticar(u, p)) {
                            this.currentUser = u;
                            System.out.println("[AUTH] Login Sucesso: " + u);
                            sendResponseBoolean(out, tag, true);
                        } else {
                            System.out.println("[AUTH] Login Falhou: " + u);
                            sendResponseBoolean(out, tag, false);
                        }
                        break;

                    case OpCodes.ADD_EVENT:
                        String prod = in.readUTF();
                        int qtd = in.readInt();
                        double preco = in.readDouble();
                        if (currentUser != null) {
                            System.out.println("[WRITE] Inserir Venda: " + qtd + "x " + prod);
                            engine.registarVenda(prod, qtd, preco);
                            sendResponseBoolean(out, tag, true);
                        } else {
                            sendResponseBoolean(out, tag, false);
                        }
                        break;

                    case OpCodes.NEW_DAY:
                        System.out.println("[TIME] Pedido para avançar dia...");
                        engine.avancarDia();
                        sendResponseBoolean(out, tag, true);
                        break;

                    // --- AGREGAÇÕES ---
                    case OpCodes.AGG_COUNT:

                        String pCount = in.readUTF();
                        System.out.println("[QUERY] Count " + pCount);
                        sendResponseInt(out, tag, engine.getQuantidade(pCount, in.readInt()));
                        break;

                    case OpCodes.AGG_VOLUME:

                        String pVol = in.readUTF();
                        System.out.println("[QUERY] Volume " + pVol);
                        sendResponseDouble(out, tag, engine.getVolume(pVol, in.readInt()));
                        break;

                    case OpCodes.AGG_AVG:

                        System.out.println("[QUERY] Média");
                        sendResponseDouble(out, tag, engine.getMedia(in.readUTF(), in.readInt()));
                        break;
                    case OpCodes.AGG_MAX:


                        System.out.println("[QUERY] Máximo");
                        sendResponseDouble(out, tag, engine.getMax(in.readUTF(), in.readInt()));
                        break;

                    case OpCodes.GET_EVENTS:
                        
                        System.out.println("[QUERY] Listar Eventos (Compacto)");
                        int dia = in.readInt();
                        int numProds = in.readInt();
                        Set<String> prods = new HashSet<>();
                        for(int i=0; i<numProds; i++) prods.add(in.readUTF());

                        List<Sale> vendas = engine.getVendas(prods, dia);

                        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
                        DataOutputStream dOut = new DataOutputStream(bOut);

                        List<String> dictList = new ArrayList<>(prods);
                        Map<String, Integer> dictMap = new HashMap<>();
                        dOut.writeInt(dictList.size());
                        for(int i=0; i<dictList.size(); i++) {
                            String s = dictList.get(i);
                            dOut.writeUTF(s);
                            dictMap.put(s, i);
                        }

                        dOut.writeInt(vendas.size());
                        for (Sale s : vendas) {
                            int idx = dictMap.getOrDefault(s.produto, -1);
                            if (idx != -1) {
                                dOut.writeInt(idx);
                                dOut.writeInt(s.quantidade);
                                dOut.writeDouble(s.preco);
                            }
                        }
                        sendResponseBytes(out, tag, bOut.toByteArray());
                        break;

                    case OpCodes.NOTIFY_SIMUL:
                        String s1 = in.readUTF();
                        String s2 = in.readUTF();
                        System.out.println("[NOTIFY] Cliente à espera de " + s1 + " E " + s2);
                        boolean r1 = engine.esperarVenda(s1);
                        boolean r2 = engine.esperarVenda(s2);
                        System.out.println("[NOTIFY] Condição satisfeita!");
                        sendResponseBoolean(out, tag, r1 && r2);
                        break;

                    case OpCodes.NOTIFY_CONSEC:
                        String sc = in.readUTF();
                        in.readInt();
                        System.out.println("[NOTIFY] Cliente à espera de " + sc);
                        engine.esperarVenda(sc);
                        System.out.println("[NOTIFY] Condição satisfeita!");
                        sendResponseBoolean(out, tag, true);
                        break;

                    default:
                        in.skipBytes(length - 12);
                }
            }
        } catch (EOFException e) {

            System.out.println("Cliente desconectou-se.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendResponseBoolean(DataOutputStream out, long tag, boolean val) throws IOException  {

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        new DataOutputStream(b).writeBoolean(val);
        sendResponseBytes(out, tag, b.toByteArray());
    }

    private void sendResponseInt(DataOutputStream out, long tag, int val) throws IOException  {

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