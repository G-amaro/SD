package common;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TaggedConnection implements AutoCloseable {

    private final Socket socket;

    private final DataOutputStream out;
    private final DataInputStream in;

    private final Lock readLock = new ReentrantLock();
    private final Lock writeLock = new ReentrantLock();

    public static class Frame {

        public final long tag;
        public final int opCode;
        public final byte[] data;

        public Frame (long tag,int opCode, byte[] data) {

            this.tag = tag;
            this.opCode = opCode;
            this.data = data;
        }

    }

    public TaggedConnection(Socket socket) throws IOException {

        this.socket = socket;
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    }

    public void send(Frame frame) throws IOException {
        writeLock.lock();

        try{
            this.out.writeInt(8 + 4 + frame.data.length);
            this.out.writeLong(frame.tag);
            this.out.writeInt(frame.opCode);
            this.out.write(frame.data);
            this.out.flush();
        } finally {
            writeLock.unlock();
        }
    }

    public void send(long tag,int opCode, byte[] data)throws IOException {
        this.send(new Frame(tag,opCode,data));
    }

    public Frame receive() throws IOException {
        readLock.lock();

        try {

            int size = this.in.readInt();
            long tag = this.in.readLong();
            int opCode = this.in.readInt();

            byte[] data = new byte[size-12];
            this.in.readFully(data);

            return new Frame(tag, opCode, data);

        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void close() throws IOException {

        socket.close();
    }
}
