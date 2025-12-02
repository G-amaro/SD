package common;

import java.util.List;

public interface IEngine {
    // Métodos de Escrita
    void registarVenda(String produto, int qtd, double preco);

    // Métodos de Leitura (Exemplos)
    double consultarSoma(String produto, int dia);
    boolean esperarPeloMenos(String produto, int qtdDesejada);

    List<Sale> getVendas(String produto);

    // Adicionar restantes métodos conforme necessário
}