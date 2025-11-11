package connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    private static final String URL = "${DATABASE_URL}";
    private static final String USER = "${DB_USER}";
    private static final String PASSWORD = "${DB_PASSWORD}";

    public static Connection getConnection() {
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("Driver PostgreSQL carregado com sucesso.");

            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Conexão estabelecida com sucesso com o banco pgAdmin!");
            return connection;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver PostgreSQL não encontrado. Verifique se o .jar está no classpath.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao conectar ao banco de dados: " + e.getMessage(), e);
        }
    }
    public static void main(String[] args) {
        Connection conn = getConnection();
    }
}

