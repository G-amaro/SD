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

    private final int janelaD;

    private final ReadWriteLock rwLock= new ReentrantReadWriteLock();
    private final Lock readLock= rwLock.readLock();
    private final Lock writeLock= rwLock.writeLock();

    // Construtor principal
    public TimeSeriesEngine(int janelaD,int maxS) {

        this.janelaD = janelaD;
        this.currentDay = new CurrentDay();
        this.history = new HistoryManager(maxS);
        this.diaAtual = carregarEstadoDia();
    }

    private int carregarEstadoDia(){

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

    public void avancarDia(){

        writeLock.lock();
        try {
            System.out.println(">>> A fechar dia " + diaAtual + "...");
            Map<String, List<Sale>> dados= currentDay.fecharDiaEObterDados();

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
    public int getQuantidade(String produto,int dias) {

        readLock.lock();
        try {
            int total = 0;
            // Se janelaD > 0, usa o menor entre o pedido e o limite. Se for 0, aceita tudo.
            int limiteEfetivo = (janelaD > 0) ? Math.min(dias, janelaD):dias;
            int inicio = Math.max(1, diaAtual -limiteEfetivo);

            for (int d =inicio; d <diaAtual;d++) {
                DaySeries ds= history.obterDia(d);

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
            int limiteEfetivo= (janelaD > 0) ? Math.min(dias, janelaD) : dias;
            int inicio= Math.max(1, diaAtual - limiteEfetivo);

            for (int d=inicio; d <diaAtual;d++) {
                DaySeries ds= history.obterDia(d);
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

            int limiteEfetivo =(janelaD > 0) ? Math.min(dias, janelaD) : dias;
            int inicio =Math.max(1, diaAtual - limiteEfetivo);

            for (int d=inicio;d< diaAtual;d++) {

                DaySeries ds = history.obterDia(d);
                if (ds!=null) {
                    totalFaturado += ds.getVolume(produto);
                    totalUnidades += ds.getQuantidade(produto);
                }

            }

            return (totalUnidades==0) ? 0.0 :totalFaturado / totalUnidades;

        } finally {

            readLock.unlock();
        }
    }

    @Override
    public double getMax(String produto,int dias) {

        readLock.lock();

        try {
            double maxGlobal =0.0;
            int limiteEfetivo= (janelaD > 0) ? Math.min(dias, janelaD) : dias;
            int inicio =Math.max(1, diaAtual - limiteEfetivo);

            for (int d=inicio; d <diaAtual; d++)  {
                DaySeries ds = history.obterDia(d);

                if (ds !=null) {
                    double maxDia = ds.getMax(produto);
                    if (maxDia > maxGlobal) maxGlobal = maxDia;
                }
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

            if (diaPedido < diaAtual && diaPedido >= 1) {

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
    public boolean esperarVendasSimultaneas(String p1,String p2) {

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

    public void encerrar() {
        avancarDia();
    }
}