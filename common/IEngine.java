package common;

import java.util.List;

import java.util.Set;


public interface IEngine {

    void registarVenda(String produto,int qtd,double preco);

    int getQuantidade(String produto, int dias);

    double getVolume(String produto,int dias);

    double getMedia(String produto,int dias);

    double getMax(String produto, int dias);

    List<Sale> getVendas(Set<String> produtos, int dia);

    boolean esperarVendasSimultaneas(String p1,String p2);

    String esperarVendasConsecutivas(String produto, int qtd);

}