package client;

public class ScenarioTest {

    public static void main(String[] args) throws InterruptedException {

        System.out.println(">>> INICIANDO TESTE DE CENÁRIO (NOTIFICAÇÕES) <<<");

        // THREAD 1: O Observador (Fica à espera)
        Thread observador = new Thread(() -> {

            try (ClientLib lib=new ClientLib()) {
                lib.register("observador", "pass");
                lib.login("observador", "pass");

                System.out.println("[OBSERVADOR] Vou subscrever '50 Batatas' e esperar...");
                // Isto vai bloquear até haver 50 batatas no total do dia
                String msg=lib.subscreverVendasConsecutivas("Batata", 50);

                System.out.println("\n[OBSERVADOR] !!! ACORDEI !!! Mensagem: " + msg);

            } catch (Exception e) {

                e.printStackTrace();
            }
        });

        // THREAD 2: O Vendedor Lento (Vende aos poucos)
        Thread vendedor=new Thread(() -> {

            try {
                // Dá tempo ao observador para se ligar
                Thread.sleep(1000);

                try (ClientLib lib =new ClientLib()) {

                    lib.register("vendedor", "pass");
                    lib.login("vendedor", "pass");

                    System.out.println("[VENDEDOR] Vou vender 20 Batatas...");
                    lib.addVenda("Batata", 20, 1.5);
                    Thread.sleep(1500);

                    System.out.println("[VENDEDOR] Vou vender mais 20 Batatas (Total 40)...");
                    lib.addVenda("Batata", 20, 1.5);
                    Thread.sleep(1500); // O observador ainda deve estar bloqueado

                    System.out.println("[VENDEDOR] Vou vender mais 15 Batatas (Total 55)...");
                    lib.addVenda("Batata", 15, 1.5);
                    //deve desbloquear aqui

                    System.out.println("[VENDEDOR] Trabalho concluído.");
                }
            } catch (Exception e) {

                e.printStackTrace();
            }
        });

        observador.start();
        vendedor.start();
        observador.join();
        vendedor.join();

        System.out.println(">>> CENÁRIO CONCLUÍDO COM SUCESSO <<<");
    }

}