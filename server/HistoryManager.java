package server;

import common.Sale;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HistoryManager {

    private final int maxDaysRam;
    private static final String PASTA_DADOS = "dados_servidor";

    private final Map<Integer, DaySeries> diasCarregados;
    private final Queue<Integer> filaSwap;
    private final Lock lock;

    public HistoryManager(int maxS) {

        this.maxDaysRam =maxS;
        this.diasCarregados = new HashMap<>();
        this.filaSwap = new LinkedList<>();
        this.lock =new ReentrantLock();

        new File(PASTA_DADOS).mkdir();
    }


    public int getUltimoDiaGuardado() {

        File pasta = new File(PASTA_DADOS);

        if (!pasta.exists()) return 0;
        File[] ficheiros = pasta.listFiles();

        if (ficheiros==null) return 0;
        int maxDia = 0;

        for (File f:ficheiros){

            String nome = f.getName();

            if (nome.startsWith("dia_") && nome.endsWith(".dat")) {

                try {
                    String numeroStr=nome.substring(4, nome.length() - 4);
                    int d=Integer.parseInt(numeroStr);
                    if (d > maxDia) maxDia = d;
                } catch (NumberFormatException e) {}
            }
        }

        return maxDia;
    }

    public void gravarDia(int dia, Map<String,List<Sale>> dados){

        lock.lock();
        try {
            File f = new File(PASTA_DADOS, "dia_" + dia + ".dat");
            try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)))) {

                // Formato: [NumProdutos: int] -> Loop [ProdName: UTF, NumVendas: int, Loop [Qtd: int, Preco: double]]
                dos.writeInt(dados.size());

                for (Map.Entry<String, List<Sale>> entry : dados.entrySet()) {

                    String produto = entry.getKey();
                    List<Sale> lista = entry.getValue();

                    dos.writeUTF(produto);
                    dos.writeInt(lista.size());

                    for (Sale s:lista) {
                        dos.writeInt(s.quantidade);
                        dos.writeDouble(s.preco);
                    }
                }
                dos.flush();

                System.out.println("[Disco] Dia " + dia + " gravado com sucesso (Binary Format).");

            } catch (IOException e) {
                e.printStackTrace();
            }

        } finally {
            lock.unlock();
        }

    }

    public DaySeries obterDia(int dia) {

        lock.lock();
        try {
            //está na RAM?
            if (diasCarregados.containsKey(dia)) {
                return diasCarregados.get(dia);
            }

            //existe no disco?
            File f= new File(PASTA_DADOS, "dia_" + dia + ".dat");

            if (!f.exists()) return null;

            //carregar do Disco (Leitura Binária)
            try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(f)))) {

                Map<String, List<Sale>> mapaRecuperado = new HashMap<>();
                int numProdutos = dis.readInt();

                for (int i =0; i <numProdutos;i++) {

                    String prodName = dis.readUTF();
                    int numVendas = dis.readInt();

                    List<Sale> listaVendas = new ArrayList<>(numVendas);

                    for (int j =0; j< numVendas; j++) {

                        int qtd = dis.readInt();
                        double preco = dis.readDouble();

                        listaVendas.add(new Sale(prodName,qtd, preco));
                    }
                    mapaRecuperado.put(prodName,listaVendas);

                }

                DaySeries ds = new DaySeries(mapaRecuperado);

                //logica memoria
                if (diasCarregados.size() >=maxDaysRam) {
                    Integer diaParaRemover =filaSwap.poll();
                    if (diaParaRemover !=null) {

                        diasCarregados.remove(diaParaRemover);
                        System.out.println("[Memoria] Swap Out: Dia " + diaParaRemover + " saiu da RAM.");
                    }
                }

                diasCarregados.put(dia,ds);
                filaSwap.add(dia);

                System.out.println("[Memoria] Swap In: Dia " + dia + " entrou na RAM.");

                return ds;

            } catch (IOException e) {

                e.printStackTrace();

                return null;
            }

        } finally {
            lock.unlock();
        }

    }

}
