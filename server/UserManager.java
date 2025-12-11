package server;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class UserManager {
    private final Map<String, String> users;
    private final Lock lock;
    private final String FILE_NAME = "users.txt";

    public UserManager() {
        this.users = new HashMap<>();
        this.lock = new ReentrantLock();
        carregarDoDisco(); // Lê users ao iniciar
    }

    public boolean registar(String username, String password) {
        lock.lock();
        try {
            if (users.containsKey(username)) return false;

            users.put(username, password);
            gravarNoDisco(); // Grava logo que regista!
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean autenticar(String username, String password) {
        lock.lock();
        try {
            return users.containsKey(username) && users.get(username).equals(password);
        } finally {
            lock.unlock();
        }
    }

    private void gravarNoDisco() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (var entry : users.entrySet()) {
                pw.println(entry.getKey() + ":" + entry.getValue());
            }
        } catch (IOException e) {
            System.err.println("Erro ao gravar users: " + e.getMessage());
        }
    }

    private void carregarDoDisco() {
        File f = new File(FILE_NAME);
        if (!f.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) users.put(parts[0], parts[1]);
            }
        } catch (IOException e) {
            System.err.println("Erro ao carregar users: " + e.getMessage());
        }
    }
}