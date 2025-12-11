package server;

import common.Sale;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DaySeries implements Serializable {
    // Dados brutos vindos do disco
    private final Map<String, List<Sale>> vendas;

    // Cache de resultados (NÃO é gravada no disco, serve só para RAM)
    private transient Map<String, Double> cacheValues;
    private transient Map<String, Integer> cacheCounts;

    public DaySeries(Map<String, List<Sale>> vendas) {
        this.vendas = vendas;
        initCache(); // Inicializa mapas vazios
    }

    // Chamado logo após ler do disco para recriar os mapas da cache
    public void initCache() {
        this.cacheValues = new ConcurrentHashMap<>();
        this.cacheCounts = new ConcurrentHashMap<>();
    }

    public List<Sale> getVendas(String produto) {
        return vendas.getOrDefault(produto, List.of());
    }

    // Exemplo de agregação com Cache Inteligente
    public int getQuantidade(String produto) {
        if (cacheCounts.containsKey(produto)) {
            return cacheCounts.get(produto);
        }

        List<Sale> lista = vendas.get(produto);
        int total = (lista == null) ? 0 : lista.stream().mapToInt(s -> s.quantidade).sum();

        cacheCounts.put(produto, total); // Guarda para a próxima
        return total;
    }

    public double getVolume(String produto) {
        String key = "VOL:" + produto;
        if (cacheValues.containsKey(key)) return cacheValues.get(key);

        List<Sale> lista = vendas.get(produto);
        double total = (lista == null) ? 0.0 : lista.stream().mapToDouble(s -> s.quantidade * s.preco).sum();

        cacheValues.put(key, total);
        return total;
    }

    public double getMedia(String produto) {
        String key = "AVG:" + produto;
        if (cacheValues.containsKey(key)) return cacheValues.get(key);

        List<Sale> lista = vendas.get(produto);
        double media = (lista == null || lista.isEmpty()) ? 0.0 :
                lista.stream().mapToDouble(s -> s.preco).average().orElse(0.0);

        cacheValues.put(key, media);
        return media;
    }

    public double getMax(String produto) {
        String key = "MAX:" + produto;
        if (cacheValues.containsKey(key)) return cacheValues.get(key);

        List<Sale> lista = vendas.get(produto);
        double max = (lista == null || lista.isEmpty()) ? 0.0 :
                lista.stream().mapToDouble(s -> s.preco).max().orElse(0.0);

        cacheValues.put(key, max);
        return max;
    }
}