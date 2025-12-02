package server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class UserManager {
    private final Map<String, String> users;
    private final Lock lock;


    public UserManager() {
        this.users = new HashMap<>();
        this.lock = new ReentrantLock();
    }

    public boolean registar(String username, String password) {
        lock.lock();
        try {
            if (users.containsKey(username)) {
                return false;
            }
            users.put(username, password);
            System.out.println("User registado: " + username);
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
}