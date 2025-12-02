package common;

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