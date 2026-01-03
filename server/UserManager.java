package server;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class UserManager {

    private final Map<String,String> users;
    private final Lock lock;
    private static final String FILE_NAME = "users.txt";

    public UserManager() {

        this.users = new HashMap<>();
        this.lock = new ReentrantLock();

        carregarUsers(); // Tenta carregar, se não der, inicia vazio
    }

    private void carregarUsers() {

        File f = new File(FILE_NAME);

        if (!f.exists()) return; // Se não existe, não faz mal

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {

            String line;

            while ((line = br.readLine()) != null) {

                String[] parts = line.split(":");
                if (parts.length == 2) {
                    users.put(parts[0], parts[1]);
                }
            }

            System.out.println("Users carregados: " + users.size());

        } catch (IOException e) {
            System.out.println("Erro ao ler users.txt: " + e.getMessage());
        }
    }

    public boolean registar(String username, String password){

        lock.lock();
        try {
            if (users.containsKey(username)) return false;

            users.put(username, password);
            guardarUserNoDisco(username, password); // Persistência simples

            return true;

        } finally {
            lock.unlock();
        }
    }

    public boolean autenticar(String username,String password){

        lock.lock();
        try {
            return users.containsKey(username) && users.get(username).equals(password);

        } finally {
            lock.unlock();
        }
    }


    private void guardarUserNoDisco(String u, String p) {

        try (BufferedWriter bw=new BufferedWriter(new FileWriter(FILE_NAME, true))) {

            bw.write(u + ":" + p);
            bw.newLine();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}