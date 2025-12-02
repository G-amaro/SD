package server;
import common.IEngine;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import common.Sale;

public class TimeSeriesEngine implements IEngine {

    private final CurrentDay currentDay;

    public TimeSeriesEngine() {
        this.currentDay = new CurrentDay();
    }

    @Override
    public void registarVenda(String produto, int qtd, double preco) {
        // Agora delega para o gestor thread-safe
        currentDay.adicionarVenda(produto, qtd, preco);
    }

    @Override
    public double consultarSoma(String produto, int dia) {
        // Exemplo simples: Se for o dia 0 (hoje), calculamos na hora
        if (dia == 0) {
            List<Sale> vendas = currentDay.getVendas(produto);
            double total = 0;
            for (Sale s : vendas) {
                total += (s.quantidade * s.preco); // Ou só quantidade, depende da query
            }
            return total;
        }
        return 0.0; // Histórico (Otavio)
    }

    @Override
    public boolean esperarPeloMenos(String produto, int qtdDesejada) {
        try {
            return currentDay.esperarPorVendas(produto, qtdDesejada);
        } catch (InterruptedException e) {
            return false;
        }
    }

    public void avancarDia() {
        // 1. Maciel: Limpa a RAM e dá os dados
        Map<String, List<Sale>> vendasDeHoje = currentDay.fecharDiaEObterDados();

        // 2. Otavio: Grava no disco (tens de fazer isto)
        // storageManager.persistirDia(vendasDeHoje);

        System.out.println("--- DIA AVANÇADO ---");
    }

    @Override
    public List<Sale> getVendas(String produto) {
        return currentDay.getVendas(produto);
    }
}