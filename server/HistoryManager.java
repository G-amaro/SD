package server;

import common.Sale;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HistoryManager {
    private static final int MAX_DAYS_RAM = 3; // Só mantemos 3 dias antigos na memória
    private static final String PASTA_DADOS = "dados_servidor";

    private final Map<Integer, DaySeries> diasCarregados;
    private final Queue<Integer> filaSwap; // FIFO para saber quem remover
    private final Lock lock;

    public HistoryManager() {
        this.diasCarregados = new HashMap<>();
        this.filaSwap = new LinkedList<>();
        this.lock = new ReentrantLock();
        new File(PASTA_DADOS).mkdir(); // Cria a pasta se não existir
    }

    // Guarda um dia no disco (Persistência)
    public void gravarDia(int dia, Map<String, List<Sale>> dados) {
        lock.lock();
        try {
            File f = new File(PASTA_DADOS, "dia_" + dia + ".dat");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
                DaySeries ds = new DaySeries(dados);
                oos.writeObject(ds);
                System.out.println("[Disco] Dia " + dia + " gravado com sucesso.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            lock.unlock();
        }
    }

    // Vai buscar um dia (Gestão de Memória)
    public DaySeries obterDia(int dia) {
        lock.lock();
        try {
            // 1. Já está na RAM?
            if (diasCarregados.containsKey(dia)) {
                return diasCarregados.get(dia);
            }

            // 2. Existe no disco?
            File f = new File(PASTA_DADOS, "dia_" + dia + ".dat");
            if (!f.exists()) return null;

            // 3. A RAM está cheia? SWAP OUT
            if (diasCarregados.size() >= MAX_DAYS_RAM) {
                int remover = filaSwap.poll();
                diasCarregados.remove(remover);
                System.out.println("[Memoria] Swap Out: Dia " + remover + " saiu da RAM.");
            }

            // 4. Carregar do Disco SWAP IN
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                DaySeries ds = (DaySeries) ois.readObject();
                ds.initCache(); // Recriar a cache transient

                diasCarregados.put(dia, ds);
                filaSwap.add(dia);
                System.out.println("[Memoria] Swap In: Dia " + dia + " entrou na RAM.");

                return ds;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } finally {
            lock.unlock();
        }
    }
}