package client;

import common.OpCodes;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

public class TaggedConnection implements AutoCloseable {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final ReentrantLock sendLock = new ReentrantLock();

    public TaggedConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    // Método Genérico de Envio (CORRIGIDO)
    public void sendFrame(long tag, int opCode, byte[] payload) throws IOException {
        sendLock.lock();
        try {
            // Length = Tag(8) + OpCode(4) + Payload Length
            out.writeInt(8 + 4 + payload.length);
            out.writeLong(tag);
            out.writeInt(opCode);
            if (payload.length > 0) {
                out.write(payload);
            }
            out.flush();
        } finally {
            sendLock.unlock();
        }
    }

    // Exemplo de uso específico: Enviar Venda
    public void sendVenda(long tag, String produto, int qtd, double preco) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream tempOut = new DataOutputStream(baos);

        tempOut.writeUTF(produto);
        tempOut.writeInt(qtd);
        tempOut.writeDouble(preco);
        tempOut.flush();

        sendFrame(tag, OpCodes.ADD_EVENT, baos.toByteArray());
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}