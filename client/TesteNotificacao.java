package client;

public class TesteNotificacao {
    public static void main(String[] args) throws Exception {
        System.out.println(">>> INICIANDO TESTE DE NOTIFICACAO <<<");

        new Thread(() -> {
            try (ClientLib lib = new ClientLib()) {
                lib.register("observador", "pass");
                lib.login("observador", "pass");

                System.out.println("[OBS] Subscrevendo 3 vendas consecutivas de 'ProdutoX'...");
                String resposta = lib.subscreverVendasConsecutivas("ProdutoX", 3);

                System.out.println("[OBS] !!! Notificacao Recebida !!!");
                System.out.println("[OBS] Mensagem: " + resposta);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        Thread.sleep(1000);

        new Thread(() -> {
            try (ClientLib lib = new ClientLib()) {
                lib.register("vendedor", "pass");
                lib.login("vendedor", "pass");

                for (int i = 1; i <= 3; i++) {
                    System.out.println("[VEND] A vender ProdutoX (" + i + "/3)...");
                    lib.addVenda("ProdutoX", 1, 10.0);
                    Thread.sleep(500);
                }
                System.out.println("[VEND] Vendas concluidas.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}