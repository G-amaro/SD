package server;

import common.IEngine;
import common.OpCodes;
import common.Sale;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class Session implements Runnable {
    private final Socket socket;
    private final IEngine engine;
    private final UserManager userManager;
    private String currentUser = null;

    public Session(Socket socket, IEngine engine, UserManager userManager) {
        this.socket = socket;
        this.engine = engine;
        this.userManager = userManager;
    }

    @Override
    public void run() {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

            while (true) {
                int length = in.readInt(); // ler frame length (ignorado por agora, usamos readUTF/Int)
                long tag = in.readLong();
                int opCode = in.readInt();

                switch (opCode) {
                    case OpCodes.AUTH_REGISTER:
                        String uReg = in.readUTF();
                        String pReg = in.readUTF();
                        boolean regOk = userManager.registar(uReg, pReg);
                        sendResponse(out, tag, regOk ? 0 : 1, new byte[0]);
                        break;

                    case OpCodes.AUTH_LOGIN:
                        String uLog = in.readUTF();
                        String pLog = in.readUTF();
                        if (userManager.autenticar(uLog, pLog)) {
                            this.currentUser = uLog;
                            sendResponse(out, tag, 0, new byte[0]);
                        } else {
                            sendResponse(out, tag, 1, new byte[0]);
                        }
                        break;

                    case OpCodes.ADD_EVENT:
                        if (currentUser == null) {
                            sendResponse(out, tag, 99, "Login Required".getBytes());
                            break;
                        }
                        String prod = in.readUTF();
                        int qtd = in.readInt();
                        double preco = in.readDouble();
                        engine.registarVenda(prod, qtd, preco);
                        sendResponse(out, tag, 0, new byte[0]);
                        break;

                    case OpCodes.NOTIFY_SIMUL:
                        if (currentUser == null) {
                            sendResponse(out, tag, 99, "Login Required".getBytes());
                            break;
                        }
                        String prodWait = in.readUTF();
                        int qtdWait = in.readInt();
                        // bloqueia a thread ate condicao verificada
                        boolean result = engine.esperarPeloMenos(prodWait, qtdWait);
                        sendResponse(out, tag, 0, new byte[0]);
                        break;

                    case OpCodes.GET_EVENTS:
                        String prodFilter = in.readUTF();
                        List<Sale> lista = engine.getVendas(prodFilter);

                        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
                        DataOutputStream dOut = new DataOutputStream(bOut);
                        dOut.writeInt(lista.size());
                        for (Sale s : lista) {
                            dOut.writeInt(s.quantidade);
                            dOut.writeDouble(s.preco);
                        }
                        sendResponse(out, tag, 0, bOut.toByteArray());
                        break;

                    case OpCodes.AGG_VOLUME:
                        String prodSoma = in.readUTF();
                        int diaSoma = in.readInt();
                        double total = engine.consultarSoma(prodSoma, diaSoma);
                        sendResponseDouble(out, tag, 0, total);
                        break;

                    default:
                        // consumir bytes restantes se necessario ou fechar
                        in.skipBytes(length - 12); // tag(8) + op(4) ja lidos
                        break;
                }
            }
        } catch (EOFException e) {
            // conexao fechada normalmente
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendResponse(DataOutputStream out, long tag, int resultOpCode, byte[] data) throws IOException {
        synchronized(out) {
            out.writeInt(8 + 4 + data.length);
            out.writeLong(tag);
            out.writeInt(resultOpCode);
            if (data.length > 0) out.write(data);
            out.flush();
        }
    }

    private void sendResponseDouble(DataOutputStream out, long tag, int code, double valor) throws IOException {
        synchronized (out) {
            out.writeInt(20); // 8+4+8
            out.writeLong(tag);
            out.writeInt(code);
            out.writeDouble(valor);
            out.flush();
        }
    }
}