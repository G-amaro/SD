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

            if (esperaPorProduto.containsKey(produto)) {
                System.out.println("   [CurrentDay] Notificando threads à espera de: " + produto);
                esperaPorProduto.get(produto).signalAll();
            }
            // System.out.println("   [CurrentDay] Venda guardada na RAM.");
        } finally {
            lock.unlock();
        }
    }

    // Permite buscar vendas de VÁRIOS produtos ao mesmo tempo
    public List<Sale> getVendas(Set<String> produtos) {
        lock.lock();
        try {
            List<Sale> resultado = new ArrayList<>();
            for (String p : produtos) {
                if (vendasDoDia.containsKey(p)) {
                    resultado.addAll(vendasDoDia.get(p));
                }
            }
            return resultado;
        } finally {
            lock.unlock();
        }
    }

    public int consultarQuantidade(String produto) {
        lock.lock();
        try {
            if (!vendasDoDia.containsKey(produto)) return 0;
            return vendasDoDia.get(produto).stream().mapToInt(s -> s.quantidade).sum();
        } finally {
            lock.unlock();
        }
    }

    public double consultarVolume(String produto) {
        lock.lock();
        try {
            if (!vendasDoDia.containsKey(produto)) return 0.0;
            return vendasDoDia.get(produto).stream().mapToDouble(s -> s.quantidade * s.preco).sum();
        } finally {
            lock.unlock();
        }
    }

    public double consultarMedia(String produto) {
        lock.lock();
        try {
            if (!vendasDoDia.containsKey(produto)) return 0.0;
            return vendasDoDia.get(produto).stream()
                    .mapToDouble(s -> s.preco) // Assumindo media de preço unitário
                    .average().orElse(0.0);
        } finally {
            lock.unlock();
        }
    }

    public double consultarMaximo(String produto) {
        lock.lock();
        try {
            if (!vendasDoDia.containsKey(produto)) return 0.0;
            return vendasDoDia.get(produto).stream()
                    .mapToDouble(s -> s.preco)
                    .max().orElse(0.0);
        } finally {
            lock.unlock();
        }
    }

    // --- LÓGICA DE NOTIFICAÇÃO DO AMARO (SIMULADA) ---
    public boolean esperarPorVenda(String produto) throws InterruptedException {
        lock.lock();
        try {
            esperaPorProduto.putIfAbsent(produto, lock.newCondition());
            Condition c = esperaPorProduto.get(produto);
            int nInicial = vendasDoDia.containsKey(produto) ? vendasDoDia.get(produto).size() : 0;

            while (true) {
                int nAtual = vendasDoDia.containsKey(produto) ? vendasDoDia.get(produto).size() : 0;
                if (nAtual > nInicial) return true;
                c.await();
            }
        } finally {
            lock.unlock();
        }
    }

    // Método antigo (mantém para compatibilidade se necessário)
    public boolean esperarPeloMenos(String produto, int qtd) throws InterruptedException {
        return esperarPorVenda(produto);
    }

    public Map<String, List<Sale>> fecharDiaEObterDados() {
        lock.lock();
        try {
            Map<String, List<Sale>> dadosAntigos = new HashMap<>(vendasDoDia);
            vendasDoDia.clear();
            for (Condition c : esperaPorProduto.values()) c.signalAll();
            esperaPorProduto.clear();
            return dadosAntigos;
        } finally {
            lock.unlock();
        }
    }
}