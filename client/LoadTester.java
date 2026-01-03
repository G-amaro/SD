package client;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadTester {

    // CONFIGURAÇÕES DO TESTE
    private static final int NUM_CLIENTES_NORMAIS=10;   // Clientes a fazer vendas/consultas
    private static final int OPS_POR_CLIENTE= 200;       // Quantas ops cada um faz
    private static final int NUM_CLIENTES_ESPERA=2;     // Clientes que testam as notificações

    private static final AtomicInteger sucessos = new AtomicInteger(0);
    private static final AtomicInteger falhas = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        System.out.println(">>> INICIANDO TESTE DE CARGA <<<");

        System.out.println("Clientes Normais: " + NUM_CLIENTES_NORMAIS);
        System.out.println("Ops por Cliente: " + OPS_POR_CLIENTE);

        Thread[] workers = new Thread[NUM_CLIENTES_NORMAIS + NUM_CLIENTES_ESPERA];
        long startTime = System.currentTimeMillis();

        // LANÇAR CLIENTES QUE FICAM À ESPERA (NOTIFICAÇÕES)
        //servem para provar que o servidor gere bem threads bloqueadas
        for (int i=0;i<NUM_CLIENTES_ESPERA; i++) {

            final int id =i;
            workers[i]=new Thread(() -> runWaiter(id));
            workers[i].start();
        }

        //Dá tempo para os Waiters se registarem no servidor
        Thread.sleep(500);

        //LANÇAR CLIENTES DE CARGA (VENDAS E LEITURAS)
        for (int i=0;i<NUM_CLIENTES_NORMAIS;i++) {

            final int id= i;

            workers[NUM_CLIENTES_ESPERA + i]= new Thread(() -> runWorker(id));
            workers[NUM_CLIENTES_ESPERA + i].start();
        }

        //ESPERAR QUE OS WORKERS ACABEM
        // Nota: Não esperamos pelos Waiters aqui porque eles podem ficar bloqueados se a condição não cumprir
        for (int i=NUM_CLIENTES_ESPERA;i< workers.length; i++) {

            workers[i].join();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("\n=== RESULTADOS ===");
        System.out.println("Tempo Total: " + duration + "ms");
        System.out.println("Sucessos: " + sucessos.get());
        System.out.println("Falhas: " + falhas.get());
        System.out.println("Throughput Estimado: " + (sucessos.get() / (duration / 1000.0)) + " ops/sec");

        // Forçar saída (para matar os Waiters que sobraram)
        System.exit(0);
    }

    //LÓGICA DO CLIENTE NORMAL (MISTURA ESCRITA E LEITURA)
    private static void runWorker(int id) {

        try (ClientLib lib = new ClientLib()) {

            // Cada thread precisa da sua própria conexão
            String user="user" + id;
            lib.register(user, "pass"); // Tenta registar (pode falhar se já existir, não faz mal)
            lib.login(user, "pass");

            Random rand=new Random();
            String[] produtos ={"Batata", "Arroz", "Massa", "Atum"};

            for (int j =0;j < OPS_POR_CLIENTE;j++) {
                String prod =produtos[rand.nextInt(produtos.length)];

                try {
                    double r = rand.nextDouble();

                    if (r < 0.4) {
                        // 40% Vendas (ESCRITA)
                        lib.addVenda(prod, rand.nextInt(10) + 1, rand.nextDouble() * 10);

                    } else if (r < 0.7) {
                        // 30% Consultas Simples (LEITURA)
                        lib.getQuantidade(prod, 0);

                    } else {
                        // 30% Consultas Pesadas (LEITURA AGREGADA)
                        lib.getPrecoMedio(prod, 0);
                    }
                    sucessos.incrementAndGet();

                } catch (Exception e) {
                    System.out.println("Erro no Cliente " + id + ": " + e.getMessage());
                    falhas.incrementAndGet();
                }
            }

            System.out.println("Cliente " + id + " terminou.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // LÓGICA DO CLIENTE QUE ESPERA (NOTIFICAÇÕES)
    private static void runWaiter(int id) {

        try (ClientLib lib= new ClientLib()) {

            String user= "waiter" + id;
            lib.register(user, "pass");
            lib.login(user, "pass");

            System.out.println("(Waiter " + id + ") À espera de 50 Batatas consecutivas...");

            // Este método vai bloquear até a condição ser verdadeira
            // Se o LoadTester terminar sem isto acontecer, esta thread é morta pelo System.exit(0)
            String resultado = lib.subscreverVendasConsecutivas("Batata", 50);

            System.out.println("!!! NOTIFICAÇÃO RECEBIDA (Waiter " + id + "): " + resultado);
            sucessos.incrementAndGet();

        } catch (Exception e) {
            // Ignorar erros de fecho de socket
        }
    }
}