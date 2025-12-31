package server;

import common.IEngine;
import common.Sale;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TimeSeriesEngine implements IEngine {

    private final CurrentDay currentDay;
    private final HistoryManager history;
    private int diaAtual;

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    // Construtor principal com D e S
    public TimeSeriesEngine(int janelaD, int maxS) {
        this.currentDay = new CurrentDay();
        this.history = new HistoryManager(maxS); // Passamos o S para o gestor
        this.diaAtual = carregarEstadoDia();
    }

    public TimeSeriesEngine() {
        this(0, 3);
    }

    private int carregarEstadoDia() {
        return history.getUltimoDiaGuardado() + 1;
    }

    @Override
    public void registarVenda(String produto, int qtd, double preco) {
        readLock.lock();
        try {
            currentDay.adicionarVenda(produto, qtd, preco);
        } finally {
            readLock.unlock();
        }
    }

    public void avancarDia() {
        writeLock.lock();
        try {
            System.out.println(">>> A fechar dia " + diaAtual + "...");
            Map<String, List<Sale>> dados = currentDay.fecharDiaEObterDados();

            if (!dados.isEmpty()) {
                history.gravarDia(diaAtual, dados);
            } else {
                System.out.println(">>> Dia " + diaAtual + " vazio. Não foi criado ficheiro.");
            }

            diaAtual++;
            System.out.println(">>> Novo dia iniciado: " + diaAtual);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public int getQuantidade(String produto, int dias) {
        readLock.lock();
        try {
            int total = 0;
            int inicio = Math.max(1, diaAtual - dias);

            for (int d = inicio; d < diaAtual; d++) {
                // Aqui o history decide se vai à RAM ou carrega temporariamente do disco
                DaySeries ds = history.obterDia(d);
                if (ds != null) total += ds.getQuantidade(produto);
            }
            return total;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public double getVolume(String produto, int dias) {
        readLock.lock();
        try {
            double total = 0.0;
            int inicio = Math.max(1, diaAtual - dias);

            for (int d = inicio; d < diaAtual; d++) {
                DaySeries ds = history.obterDia(d);
                if (ds != null) total += ds.getVolume(produto);
            }
            return total;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public double getMedia(String produto, int dias) {
        readLock.lock();
        try {
            double totalFaturado = 0.0;
            int totalUnidades = 0;
            int inicio = Math.max(1, diaAtual - dias);

            for (int d = inicio; d < diaAtual; d++) {
                DaySeries ds = history.obterDia(d);
                if (ds != null) {
                    List<Sale> vendas = ds.getVendas(produto);
                    if (vendas != null) {
                        for (Sale s : vendas) {
                            totalFaturado += s.preco * s.quantidade;
                            totalUnidades += s.quantidade;
                        }
                    }
                }
            }
            return (totalUnidades == 0) ? 0.0 : totalFaturado / totalUnidades;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public double getMax(String produto, int dias) {
        readLock.lock();
        try {
            double maxGlobal = 0.0;
            int inicio = Math.max(1, diaAtual - dias);

            for (int d = inicio; d < diaAtual; d++) {
                DaySeries ds = history.obterDia(d);
                double maxDia = (ds != null) ? ds.getMax(produto) : 0.0;
                if (maxDia > maxGlobal) maxGlobal = maxDia;
            }
            return maxGlobal;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Sale> getVendas(Set<String> produtos, int diaPedido) {
        readLock.lock();
        try {
            List<Sale> resultado = new ArrayList<>();

            if (diaPedido == diaAtual) {
                resultado.addAll(currentDay.getVendas(produtos));
            } else if (diaPedido < diaAtual && diaPedido >= 1) {
                DaySeries ds = history.obterDia(diaPedido);
                if (ds != null) {
                    for (String p : produtos) {
                        resultado.addAll(ds.getVendas(p));
                    }
                }
            }
            return resultado;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean esperarVendasSimultaneas(String p1, String p2) {
        try {
            return currentDay.esperarVendasSimultaneas(p1, p2);
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public String esperarVendasConsecutivas(String produto, int qtd) {
        try {
            return currentDay.esperarVendasConsecutivas(produto, qtd);
        } catch (InterruptedException e) {
            return null;
        }
    }

    @Override public double consultarSoma(String p, int d) { return getVolume(p, d); }
    @Override public List<Sale> getVendas(String p) { return null; }

    public void encerrar() {
        avancarDia();
    }
}