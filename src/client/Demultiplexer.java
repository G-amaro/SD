package client;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.*;

public class ConnectionManager implements AutoCloseable{
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    private final Lock sendLock = new ReentrantLock();
    private final Lock mapLock = new ReentrantLock();
    private final Condition response = mapLock.newCondition();


    private Map<Long, byte[]> responses = new HashMap<>();

    private long lastId = 0;
    private boolean running = true;

    public ConnectionManager(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.dis = new DataInputStream(socket.getInputStream());
        this.dos = new DataOutputStream(socket.getOutputStream());

        Thread reader = new Thread(this::readLoop);
        reader.start();
    }

    public byte[] sendRequest(int opCode, byte[] data) throws IOException {
        long myId;

        sendLock.lock();
        try{
            myId = ++lastId;
            dos.writeInt(8+4+ data.length);
            dos.writeLong(myId);
            dos.writeInt(opCode);
            dos.write(data);
            dos.flush();
        } finally {
            sendLock.unlock();
        }

        mapLock.lock();
        try{
            while(!responses.containsKey(myId)){
                try{
                    response.await();
                }catch(InterruptedException e){
                    throw new IOException("Interrupted");
                }
            }
            return responses.remove(myId);
        } finally{
            mapLock.unlock();
        }

    }

    private void readLoop(){
        try{
            while(running){
                int size = dis.readInt();
                long id = dis.readLong();
                byte[] data = new byte[size - 8]; //total - ID length
                dis.readFully(data);

                mapLock.lock();
                try {
                    responses.put(id, data);
                    response.signalAll();
                }finally{
                    mapLock.unlock();
                }
            }
        }catch (IOException e){
            System.out.println("Conecção fechada.");
        }
    }

    public void close() throws IOException{
        running = false;
        socket.close();
    }


}