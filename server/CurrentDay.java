package server;

import common.Sale;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CurrentDay {
    private final Map<String, List<Sale>> vendasDoDia;
    private final Map<String, Condition> esperaPorProduto;
    private final Lock lock;

    public CurrentDay() {
        this.vendasDoDia = new HashMap<>();
        this.esperaPorProduto = new HashMap<>();
        this.lock = new ReentrantLock();
    }

    public void adicionarVenda(String produto, int qtd, double preco) {
        lock.lock();
        try {
            vendasDoDia.putIfAbsent(produto, new ArrayList<>());
            vendasDoDia.get(produto).add(new Sale(produto, qtd, preco));

            // notificar threads a espera deste produto
            if (esperaPorProduto.containsKey(produto)) {
                esperaPorProduto.get(produto).signalAll();
            }
            System.out.println("Venda registada: " + produto);
        } finally {
            lock.unlock();
        }
    }

    public boolean esperarPorVendas(String produto, int qtdDesejada) throws InterruptedException {
        lock.lock();
        try {
            esperaPorProduto.putIfAbsent(produto, lock.newCondition());
            Condition cond = esperaPorProduto.get(produto);

            while (contarVendas(produto) < qtdDesejada) {
                cond.await();
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    private int contarVendas(String produto) {
        return vendasDoDia.containsKey(produto) ? vendasDoDia.get(produto).size() : 0;
    }

    public List<Sale> getVendas(String produto) {
        lock.lock();
        try {
            if (vendasDoDia.containsKey(produto)) {
                return new ArrayList<>(vendasDoDia.get(produto));
            }
            return new ArrayList<>();
        } finally {
            lock.unlock();
        }
    }

    public double consultarSoma(String produto) {
        lock.lock();
        try {
            if (!vendasDoDia.containsKey(produto)) return 0.0;
            return vendasDoDia.get(produto).stream()
                    .mapToDouble(s -> s.quantidade * s.preco)
                    .sum();
        } finally {
            lock.unlock();
        }
    }

    public Map<String, List<Sale>> fecharDiaEObterDados() {
        lock.lock();
        try {
            Map<String, List<Sale>> dadosAntigos = new HashMap<>(vendasDoDia);
            vendasDoDia.clear();

            // evitar deadlocks em threads a espera no dia anterior
            for (Condition c : esperaPorProduto.values()) {
                c.signalAll();
            }
            esperaPorProduto.clear();
            return dadosAntigos;
        } finally {
            lock.unlock();
        }
    }
}