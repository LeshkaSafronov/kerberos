package network;

public class Settings {
    public static final String DB_HOST = "jdbc:postgresql:kerberos";
    public static final String DB_USERNAME = "postgres";
    public static final String DB_PASSWORD = "secret";
    public static final String AS_HOST = "localhost:8080/auth";
    public static final long TICKET_ESTIMATE_SECONDS = 86400;
    public static final String AS_TGS_KEY = "aleshka";
    public static final String TGS_HOST = "localhost:8080/tgs";
    public static final String SS_HOST = "localhost:8080/ss";
    public static final String TGS_SS_KEY = "synesis";
}
