package client;

import java.io.IOException;
import java.util.*;

public class TextUI
{
    public static void main(String[] args) throws IOException {

        ClientLib lib =new ClientLib();
        Scanner sc=new Scanner(System.in);

        try{
            System.out.println("==== Cliente Iniciado ====");
            System.out.println("Comandos Disponiveis:");
            System.out.println("  Auth:   register <u/p> | login <user> <password>");
            System.out.println("  Write:  venda <prod> <qtd> <preços> | novodia");
            System.out.println("  Read:   soma <prod> <dias> | media <prod> <dias> | volume <prod> <dias> | max <prod> <dias>");
            System.out.println("  List:   eventos <dia> <prod1> <prod2> ...");
            System.out.println("  Notify: sub-simul <prod1> <prod2> | sub-conseq <produto> <qtd>");
            System.out.println("==========================");

            while (sc.hasNextLine()) {

                String line=sc.nextLine();
                String[] tokens= line.split(" ");
                String command= tokens[0].toLowerCase();

                try {

                    switch (command) {
                        case "register":
                            System.out.println(lib.register(tokens[1],tokens[2]));
                            break;

                        case "login":
                            System.out.println(lib.login(tokens[1],tokens[2]));
                            break;

                        case "venda":
                            System.out.println(lib.addVenda(tokens[1], Integer.parseInt(tokens[2]), Double.parseDouble(tokens[3])));
                            break;

                        case "novodia":
                            System.out.println(lib.novoDia());
                            break;

                        case "soma":
                            System.out.println("Total: " + lib.getQuantidade(tokens[1], Integer.parseInt(tokens[2])));
                            break;

                        case "volume":
                            System.out.println("Volume: " + lib.getVolume(tokens[1], Integer.parseInt(tokens[2])));
                            break;

                        case "media":
                            System.out.println("Media: " + lib.getPrecoMedio(tokens[1], Integer.parseInt(tokens[2])));
                            break;

                        case "max":
                            System.out.println("Max: " + lib.getPrecoMaximo(tokens[1], Integer.parseInt(tokens[2])));
                            break;

                        case "eventos":
                            int dia =Integer.parseInt(tokens[1]);

                            if (dia < 1) {
                                System.out.println("Erro: O dia tem de ser maior que 0.");
                                break;
                            }

                            Set<String> prods=new HashSet<>();

                            for (int i=2; i<tokens.length;i++) {
                                prods.add(tokens[i]);
                            }

                            //se não houver produtos, avisa o utilizador em vez de enviar pedido vazio
                            if (prods.isEmpty()) {

                                System.out.println("Erro: Tens de indicar pelo menos um produto. Ex: eventos 1 Banana");

                            } else {

                                List<String> eventos = lib.getEventos(prods, dia);
                                System.out.println("--- Eventos recebidos ---");
                                eventos.forEach(System.out::println);
                            }

                            break;

                        case "sub-simul":
                            System.out.println("À espera de vendas simultaneas...");
                            System.out.println(lib.subscreverVendasSimultaneas(tokens[1],tokens[2]));
                            break;

                        case "sub-conseq":

                            System.out.println("À espera de vendas consecutivas...");
                            System.out.println(lib.subscreverVendasConsecutivas(tokens[1],Integer.parseInt(tokens[2])));
                            break;

                        default:
                            System.out.println("Opcao invalida");
                    }

                }catch(Exception e){

                    e.printStackTrace();
                }

            }

        }catch (Exception e) {
            e.printStackTrace();
        }

    }

}
