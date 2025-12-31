package common;

public class OpCodes {
    // Autenticação (Amaro)
    public static final int AUTH_REGISTER = 0;
    public static final int AUTH_LOGIN = 1;

    // Escrita (Maciel)
    public static final int ADD_EVENT = 2;
    public static final int NEW_DAY = 3;

    // Consultas (Maciel + Amaro)
    public static final int AGG_COUNT = 4;
    public static final int AGG_VOLUME = 5;
    public static final int AGG_AVG = 6;
    public static final int AGG_MAX = 7;

    // Lista Compacta (Amaro)
    public static final int GET_EVENTS = 8;

    // Notificações (Maciel + Amaro)
    public static final int NOTIFY_SIMUL = 9;
    public static final int NOTIFY_CONSEC = 10;

}