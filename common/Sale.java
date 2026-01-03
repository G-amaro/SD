package common;

import java.io.Serializable;

public class Sale implements Serializable {

    public String produto;
    public int quantidade;
    public double preco;

    public Sale(String produto, int quantidade, double preco) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.preco = preco;
    }
}