package network;

public class Settings {
    public static final String DB_HOST = "jdbc:postgresql:kerberos";
    public static final String DB_USERNAME = "postgres";
    public static final String DB_PASSWORD = "secret";
    public static final String TGT_HOST = "localhost:9090";
    public static final long TICKET_ESTIMATE_SECONDS = 86400;
    public static final String AS_TGS_KEY = "aleshka";
}
