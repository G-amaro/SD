package client;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DataSeeder {

    private static final int NUM_USERS = 5;       // Quantos utilizadores a inserir dados
    private static final int VENDAS_POR_USER = 100; // Quantas vendas cada um faz
    private static final String[] PRODUTOS = {"Batata", "Arroz", "Cebola", "Azeite", "Leite"};

    public static void main(String[] args) {
        System.out.println(">>> INICIANDO DATA SEEDER (Povoamento de Dados) <<<");

        ExecutorService executor = Executors.newFixedThreadPool(NUM_USERS);
        AtomicInteger totalVendas = new AtomicInteger(0);
        long start = System.currentTimeMillis();

        for (int i = 1; i <= NUM_USERS; i++) {
            final int id = i;
            executor.submit(() -> {
                try (ClientLib lib = new ClientLib()) {
                    String user = "seeder_" + id;
                    lib.register(user, "pass123");
                    lib.login(user, "pass123");

                    Random rand = new Random();
                    System.out.println("User [" + user + "] iniciou a injeção de dados...");

                    for (int j = 0; j < VENDAS_POR_USER; j++) {
                        String prod = PRODUTOS[rand.nextInt(PRODUTOS.length)];
                        int qtd = rand.nextInt(20) + 1;     // 1 a 20 unidades
                        double preco = 1.0 + (rand.nextDouble() * 10.0); // 1.0 a 11.0 euros

                        lib.addVenda(prod, qtd, preco);
                        totalVendas.incrementAndGet();

                        // Pequena pausa aleatória para simular latência real (opcional)
                        // Thread.sleep(rand.nextInt(10));
                    }
                    System.out.println("User [" + user + "] terminou.");
                } catch (Exception e) {
                    System.err.println("Erro no user " + id + ": " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long end = System.currentTimeMillis();
        System.out.println("\n>>> CONCLUSÃO <<<");
        System.out.println("Total de vendas inseridas: " + totalVendas.get());
        System.out.println("Tempo decorrido: " + (end - start) + "ms");
    }
}