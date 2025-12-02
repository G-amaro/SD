package server;
import common.IEngine;
import java.util.List;
import common.Sale;

public class EngineDummy implements IEngine {
    @Override
    public void registarVenda(String produto, int qtd, double preco) {
        System.out.println("[SERVER-ENGINE] Venda Registada: " + produto + " | Qtd: " + qtd + " | Preço: " + preco);
    }

    @Override
    public double consultarSoma(String produto, int dia) {
        return 0.0; // Dummy
    }

    @Override
    public boolean esperarPeloMenos(String produto, int qtdDesejada) {
        return false;
    }

    @Override
    public List<Sale> getVendas(String produto) {
        return new java.util.ArrayList<>();
    }
}