package server;

import common.IEngine;
import common.Sale;
import java.util.List;
import java.util.Map;
import java.util.Set; // Importante!

public class TimeSeriesEngine implements IEngine {

    private final CurrentDay currentDay;

    public TimeSeriesEngine() {
        this.currentDay = new CurrentDay();
    }

    @Override
    public void registarVenda(String produto, int qtd, double preco) {
        currentDay.adicionarVenda(produto, qtd, preco);
    }

    // --- NOVOS MÉTODOS DE CONSULTA ---
    public int getQuantidade(String produto, int dia) {
        return currentDay.consultarQuantidade(produto);
    }

    public double getVolume(String produto, int dia) {
        return currentDay.consultarVolume(produto);
    }

    public double getMedia(String produto, int dia) {
        return currentDay.consultarMedia(produto);
    }

    public double getMax(String produto, int dia) {
        return currentDay.consultarMaximo(produto);
    }

    public List<Sale> getVendas(Set<String> produtos, int dia) {
        return currentDay.getVendas(produtos);
    }

    // Adaptação da notificação
    public boolean esperarVenda(String produto) {
        try { return currentDay.esperarPorVenda(produto); }
        catch (InterruptedException e) { return false; }
    }

    // Métodos da Interface antiga (para não partir compilação, se a interface ainda os tiver)
    @Override public double consultarSoma(String p, int d) { return getVolume(p, d); }
    @Override public boolean esperarPeloMenos(String p, int q) { return esperarVenda(p); }
    @Override public List<Sale> getVendas(String p) { return null; } // Deprecated

    public void avancarDia() {
        Map<String, List<Sale>> dados = currentDay.fecharDiaEObterDados();
        // Otavio grava 'dados' aqui
        System.out.println("--- DIA AVANÇADO ---");
    }
}