package server;

import common.IEngine;
import common.Sale;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TimeSeriesEngine implements IEngine {

    private final CurrentDay currentDay;  // Maciel
    private final HistoryManager history; // Octávio
    private int diaAtual = 1;

    public TimeSeriesEngine() {
        this.currentDay = new CurrentDay();
        this.history = new HistoryManager();
    }

    // --- ESCRITA (Sempre no dia corrente) ---
    @Override
    public void registarVenda(String produto, int qtd, double preco) {
        currentDay.adicionarVenda(produto, qtd, preco);
    }

    // --- MUDANÇA DE DIA (Integração) ---
    public synchronized void avancarDia() {
        System.out.println(">>> A fechar dia " + diaAtual + "...");

        // 1. Tira tudo da RAM do Maciel
        Map<String, List<Sale>> dados = currentDay.fecharDiaEObterDados();

        // 2. Octávio grava no disco
        history.gravarDia(diaAtual, dados);

        // 3. Avança
        diaAtual++;
        System.out.println(">>> Novo dia iniciado: " + diaAtual);
    }

    // --- LEITURAS (Híbridas) ---

    public int getQuantidade(String produto, int dia) {
        if (dia == diaAtual) return currentDay.consultarQuantidade(produto);
        if (dia < diaAtual) {
            DaySeries ds = history.obterDia(dia);
            return (ds != null) ? ds.getQuantidade(produto) : 0;
        }
        return 0;
    }

    public double getVolume(String produto, int dia) {
        if (dia == diaAtual) return currentDay.consultarVolume(produto);
        if (dia < diaAtual) {
            DaySeries ds = history.obterDia(dia);
            return (ds != null) ? ds.getVolume(produto) : 0.0;
        }
        return 0.0;
    }

    public double getMedia(String produto, int dia) {
        if (dia == diaAtual) return currentDay.consultarMedia(produto);
        if (dia < diaAtual) {
            DaySeries ds = history.obterDia(dia);
            return (ds != null) ? ds.getMedia(produto) : 0.0;
        }
        return 0.0;
    }

    public double getMax(String produto, int dia) {
        if (dia == diaAtual) return currentDay.consultarMaximo(produto);
        if (dia < diaAtual) {
            DaySeries ds = history.obterDia(dia);
            return (ds != null) ? ds.getMax(produto) : 0.0;
        }
        return 0.0;
    }

    public List<Sale> getVendas(Set<String> produtos, int dia) {
        if (dia == diaAtual) return currentDay.getVendas(produtos);
        if (dia < diaAtual) {
            DaySeries ds = history.obterDia(dia);
            if (ds == null) return new ArrayList<>();

            List<Sale> res = new ArrayList<>();
            for(String p : produtos) res.addAll(ds.getVendas(p));
            return res;
        }
        return new ArrayList<>();
    }

    // --- Métodos de compatibilidade (Interface antiga) ---
    @Override public double consultarSoma(String p, int d) { return getVolume(p, d); }
    @Override public boolean esperarPeloMenos(String p, int q) { return esperarVenda(p); }
    public boolean esperarVenda(String p) {
        try { return currentDay.esperarPorVenda(p); }
        catch (InterruptedException e) { return false; }
    }
    @Override public List<Sale> getVendas(String p) { return null; }
}