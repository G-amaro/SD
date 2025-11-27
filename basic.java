// Método na classe TaggedConnection ou similar
public void sendVenda(long tag, String produto, int qtd, double preco) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream tempOut = new DataOutputStream(baos);

    // 1. Escrever o Payload temporariamente para saber o tamanho
    tempOut.writeUTF(produto);
    tempOut.writeInt(qtd);
    tempOut.writeDouble(preco);

    byte[] payload = baos.toByteArray();
<<<<<<< HEAD
    jhagsdjhgda
=======
>>>>>>> 38d4627a5b1e7c8736154dabb9321207cc48c321

    // 2. Escrever no Socket real (com Lock para thread-safety)
    synchronized (socketLock) {
        realOut.writeInt(payload.length); // Header: Length
        realOut.writeLong(tag);           // Header: Tag
        realOut.writeInt(OpCodes.ADD_EVENT); // Header: OpCode
        realOut.write(payload);           // Body
        realOut.flush();
    }
}

public class OpCodes {
    // Autenticação
    public static final int AUTH_REGISTER = 0;
    public static final int AUTH_LOGIN = 1;

    // Escrita (Dia Corrente)
    public static final int ADD_EVENT = 2;     // Adicionar venda
    public static final int NEW_DAY = 3;       // Avançar dia (Passagem de tempo)

    // Consultas (Agregações)
    public static final int AGG_COUNT = 4;     // Quantidade total
    public static final int AGG_VOLUME = 5;    // Volume de faturação
    public static final int AGG_AVG = 6;       // Preço médio
    public static final int AGG_MAX = 7;       // Preço máximo

    // Lista (Payload complexo)
    public static final int GET_EVENTS = 8;    // Filtrar eventos (Compact Serialization)

    // Notificações (Blocking)
    public static final int NOTIFY_SIMUL = 9;  // Vendas simultâneas
    public static final int NOTIFY_CONSEC = 10;// Vendas consecutivas

    // Erros
    public static final int ERROR = 99;        // Caso algo corra mal
}