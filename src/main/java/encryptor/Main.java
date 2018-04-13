package encryptor;

import java.sql.*;

public class Main {
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
       //  Class.forName("org.postgresql.Driver");

        String url = "jdbc:postgresql:games";
        String username = "postgres";
        String password = "secret";
        Connection conn = DriverManager.getConnection(url, username, password);

        Statement statement = conn.createStatement();
        ResultSet resultSet = statement.executeQuery("select * from social_media_record");
        if (resultSet.next()) {
            System.out.println(resultSet.getString(2));
        }


    }

}
