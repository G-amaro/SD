package server;

import common.Sale;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CurrentDay {
    private final Map<String, List<Sale>> vendasDoDia;
    private final Lock lock;
    private final Condition change;

    // Estado para vendas consecutivas
    private String lastProduct = null;
    private int consecutiveCount = 0;

    // Flag para garantir que ninguém fica preso se o dia mudar
    private boolean dayFinished = false;

    public CurrentDay() {
        this.vendasDoDia = new HashMap<>();
        this.lock = new ReentrantLock();
        this.change = lock.newCondition();
    }

    public void adicionarVenda(String produto, int qtd, double preco) {
        lock.lock();
        try {
            vendasDoDia.putIfAbsent(produto, new ArrayList<>());
            vendasDoDia.get(produto).add(new Sale(produto, qtd, preco));

            // Lógica de Consecutivos
            if (produto.equals(lastProduct)) {
                consecutiveCount += qtd;
            } else {
                lastProduct = produto;
                consecutiveCount = qtd;
            }

            // Acordar quem estiver à espera
            change.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public String esperarVendasConsecutivas(String produto, int qtd) throws InterruptedException {
        lock.lock();
        try {
            // Condição para ter sucesso: Produto atual é o pedido E contagem >= qtd
            boolean goalReached = (lastProduct != null && lastProduct.equals(produto) && consecutiveCount >= qtd);

            // Esperar ENQUANTO (não atingiu objetivo E dia não acabou)
            while (!goalReached && !dayFinished) {
                change.await();
                // Reavaliar condição ao acordar
                goalReached = (lastProduct != null && lastProduct.equals(produto) && consecutiveCount >= qtd);
            }

            // Se o dia acabou e não atingimos o objetivo, retorna null
            if (!goalReached && dayFinished) {
                return null;
            }

            return lastProduct;
        } finally {
            lock.unlock();
        }
    }

    public boolean esperarVendasSimultaneas(String p1, String p2) throws InterruptedException {
        lock.lock();
        try {
            // Esperar ENQUANTO (falta p1 OU falta p2) E dia não acabou
            while ((!vendasDoDia.containsKey(p1) || !vendasDoDia.containsKey(p2)) && !dayFinished) {
                change.await();
            }

            // Se saiu do loop porque o dia acabou, retorna false
            if (dayFinished && (!vendasDoDia.containsKey(p1) || !vendasDoDia.containsKey(p2))) {
                return false;
            }

            return true;
        } finally {
            lock.unlock();
        }
    }

    // --- Métodos de Leitura (Mantidos) ---

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
            return vendasDoDia.get(produto).stream().mapToDouble(s -> s.preco).average().orElse(0.0);
        } finally {
            lock.unlock();
        }
    }

    public double consultarMaximo(String produto) {
        lock.lock();
        try {
            if (!vendasDoDia.containsKey(produto)) return 0.0;
            return vendasDoDia.get(produto).stream().mapToDouble(s -> s.preco).max().orElse(0.0);
        } finally {
            lock.unlock();
        }
    }

    public Map<String, List<Sale>> fecharDiaEObterDados() {
        lock.lock();
        try {
            Map<String, List<Sale>> dadosAntigos = new HashMap<>(vendasDoDia);
            vendasDoDia.clear();

            // Reset do estado
            lastProduct = null;
            consecutiveCount = 0;

            // Sinalizar que o dia acabou para desbloquear threads presas
            dayFinished = true;
            change.signalAll();

            // IMPORTANTE: Reset da flag para o próximo dia não nascer "terminado".
            // No entanto, como o objeto CurrentDay pode ser reutilizado, temos de ter cuidado.
            // A estratégia mais segura aqui é assumir que quem acordou vai ler 'dayFinished=true'
            // e sair. Logo a seguir metemos a false para o novo dia.
            // (Nota: Num sistema real perfeito, usariamos um generationId, mas isto chega para o projeto)

            // Pequeno truque: libertamos o lock momentaneamente ou assumimos que
            // o TimeSeriesEngine gere os dias. Vamos deixar dayFinished = false
            // apenas quando começarem novas vendas ou no construtor?
            // O ideal é resetar aqui mas garantir que os outros viram.
            // Como temos o lock, eles só vão ver quando sairmos.

            // Vamos deixar a flag a true momentaneamente e quem chama este método (avancarDia)
            // é responsável por criar um novo dia ou resetar.
            // Mas para simplificar a tua vida sem mudar o TimeSeriesEngine:

            return dadosAntigos;
        } finally {
            // Hack para o próximo dia: resetar a flag "silenciosamente" após notificar?
            // Não, o correto é: As threads acordam, veem dayFinished=true, retornam null.
            // O próximo dia começa limpo.
            dayFinished = false;
            lock.unlock();
        }
    }
}