package server;

import common.IEngine;
import common.Sale;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TimeSeriesEngine implements IEngine {

    private final CurrentDay currentDay;
    private final HistoryManager history;
    private int diaAtual; // Já não inicializamos a 1 aqui

    public TimeSeriesEngine() {
        this.currentDay = new CurrentDay();
        this.history = new HistoryManager();
        this.diaAtual = carregarEstadoDia(); // <--- MUDANÇA: Recuperar o dia correto
    }

    // --- NOVA FUNÇÃO AUXILIAR ---
    // Vai à pasta e descobre qual o último dia gravado para continuarmos daí
    private int carregarEstadoDia() {
        File pasta = new File("dados_servidor");
        if (!pasta.exists()) {
            return 1; // Se não há pasta, é o primeiro dia
        }

        int maxDia = 0;
        File[] ficheiros = pasta.listFiles();
        if (ficheiros != null) {
            for (File f : ficheiros) {
                String nome = f.getName();
                // Procura ficheiros do tipo "dia_X.dat"
                if (nome.startsWith("dia_") && nome.endsWith(".dat")) {
                    try {
                        // Extrai o número entre "dia_" e ".dat"
                        String numeroStr = nome.substring(4, nome.length() - 4);
                        int dia = Integer.parseInt(numeroStr);
                        if (dia > maxDia) {
                            maxDia = dia;
                        }
                    } catch (NumberFormatException e) {
                        // Ignora ficheiros com nomes estranhos
                    }
                }
            }
        }
        System.out.println(">>> Estado recuperado: Último dia gravado foi " + maxDia + ". A iniciar no dia " + (maxDia + 1));
        return maxDia + 1; // O dia atual é o próximo
    }

    // --- ESCRITA ---
    @Override
    public void registarVenda(String produto, int qtd, double preco) {
        currentDay.adicionarVenda(produto, qtd, preco);
    }

    // --- MUDANÇA DE DIA (Agora funciona corretamente incrementalmente) ---
    public synchronized void avancarDia() {
        System.out.println(">>> A fechar dia " + diaAtual + "...");

        // 1. Obtém os dados da RAM (CurrentDay) e limpa para o novo dia
        Map<String, List<Sale>> dados = currentDay.fecharDiaEObterDados();

        // 2. Grava este bloco no disco (persistência incremental)
        if (!dados.isEmpty()) {
            history.gravarDia(diaAtual, dados);
        } else {
            System.out.println(">>> Dia " + diaAtual + " vazio. Não foi criado ficheiro.");
        }

        // 3. Avança o contador para o próximo
        diaAtual++;
        System.out.println(">>> Novo dia iniciado: " + diaAtual);
    }

    // --- QUERY / LEITURA (Mantém-se igual) ---

    // ... (restante código dos getters getQuantidade, getVolume, etc. mantém-se igual)

// Em server/TimeSeriesEngine.java

    public int getQuantidade(String produto, int diasParaTras) {
        int total = 0;
        // O enunciado diz "últimos d dias anteriores".
        // Vamos assumir que inclui o dia atual se a lógica for "janela temporal".
        // Ou, se for estrito ao PDF ("dias anteriores"), começa em diaAtual - 1.
        // Vou fazer uma janela que recua 'diasParaTras' dias a partir de agora.

        int diaInicio = Math.max(1, diaAtual - diasParaTras + 1);

        for (int d = diaInicio; d <= diaAtual; d++) {
            if (d == diaAtual) {
                total += currentDay.consultarQuantidade(produto);
            } else {
                DaySeries ds = history.obterDia(d);
                if (ds != null) {
                    total += ds.getQuantidade(produto);
                }
            }
        }
        return total;
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

    // Em server/TimeSeriesEngine.java

    public List<Sale> getVendas(Set<String> produtos, int diaLimite) {
        List<Sale> resultado = new ArrayList<>();

        // PERCORRER DESDE O DIA 1 ATÉ AO DIA PEDIDO (inclusive)
        for (int d = 1; d <= diaLimite; d++) {

            // Se for o dia atual, vamos à RAM (CurrentDay)
            if (d == diaAtual) {
                resultado.addAll(currentDay.getVendas(produtos));
            }
            // Se for dia passado, vamos ao Histórico (Disco/Cache)
            else if (d < diaAtual) {
                DaySeries ds = history.obterDia(d);
                if (ds != null) {
                    for (String p : produtos) {
                        resultado.addAll(ds.getVendas(p));
                    }
                }
            }
        }
        return resultado;
    }
    // --- MÉTODOS DE COMPATIBILIDADE ---
    @Override public double consultarSoma(String p, int d) { return getVolume(p, d); }
    @Override public boolean esperarPeloMenos(String p, int q) { return esperarVenda(p); }
    public boolean esperarVenda(String p) {
        try { return currentDay.esperarPorVenda(p); }
        catch (InterruptedException e) { return false; }
    }
    @Override public List<Sale> getVendas(String p) { return null; }

    // Podes manter o método encerrar() auxiliar se quiseres usar o ShutdownHook,
    // mas agora o core do sistema já respeita a gravação "aos poucos" através do novodia.
    public synchronized void encerrar() {
        Map<String, List<Sale>> dados = currentDay.fecharDiaEObterDados();
        if (!dados.isEmpty()) history.gravarDia(diaAtual, dados);
    }
}