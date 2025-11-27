package client;

import java.io.IOException;
import java.util.Scanner;

public class TextUI
{
    public static void main(String[] args) throws IOException {
        ClientLib lib =new ClientLib();
        Scanner sc = new Scanner(System.in);
        try{
            System.out.println("Cliente iniciado. Comandos:");
            System.out.println("  login <user> <password>");
            System.out.println("  venda <prod> <qtd> <preços>");

            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                String[] tokens = line.split(" ");

                switch (tokens[0]) {
                    case "login":
                        System.out.println(lib.login(tokens[1], tokens[2]));
                        break;

                    case "venda":
                        lib.addVenda(tokens[1], Integer.parseInt(tokens[2]), Double.parseDouble(tokens[3]));
                        break;

                    default:
                        System.out.println("Opcao invalida");
                }

            }
        }catch (Exception e) {
            e.printStackTrace();
        }


    }
}