package client;

import common.TaggedConnection;
import common.TaggedConnection.Frame;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Demultiplexer implements AutoCloseable{
    private final TaggedConnection conn;
    private final Lock lock = new ReentrantLock();
    private final Map<Long, Entry> responses = new HashMap<>();
    private long lastId = 0;
    public Exception failure = null;

    private class Entry{
        byte[] data = null;
        final Condition cond = lock.newCondition();
    }

    public Demultiplexer(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        this.conn = new TaggedConnection(socket);

        Thread reader = new Thread(this::readLoop);
        reader.start();
    }

    public byte[] sendRequest(int opCode, byte[] data) throws IOException {
        long id;
        Entry myEntry = new  Entry();

        lock.lock();
        try {
            id = ++lastId;
            responses.put(id, myEntry);
        }finally {
            lock.unlock();
        }

        conn.send(id, opCode, data);

        lock.lock();
        try{
            while (myEntry.data == null && failure == null){
                try{
                    myEntry.cond.await();
                }catch (InterruptedException e){
                    throw new IOException("Interrompido");
                }
            }
            if (failure != null) throw new IOException("Conexão", failure);

            responses.remove(id);
            return myEntry.data;
        }finally {
            lock.unlock();
        }
    }

    private void readLoop(){
        try {
            while (true) {
                Frame frame = conn.receive();
                lock.lock();
                try {
                    Entry e = responses.get(frame.tag);
                    if (e != null) {
                        e.data = frame.data;
                        e.cond.signal();
                    }
                }finally {
                    lock.unlock();
                }
            }
        }catch (Exception e){
            lock.lock();
            try {
                failure = e;
                responses.values().forEach(entry-> entry.cond.signalAll());

            }finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void close() throws IOException{

        conn.close();
    }


}