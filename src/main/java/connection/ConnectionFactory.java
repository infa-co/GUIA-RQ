package connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    private static final String URL = System.getenv("DATABASE_URL");
    private static final String USER = System.getenv("DATABASE_USER");
    private static final String PASSWORD = System.getenv("DATABASE_PASSWORD");

    public static Connection getConnection() {
        if (URL == null || USER == null || PASSWORD == null) {
            throw new RuntimeException("❌ Variáveis de ambiente não configuradas! Configure DATABASE_URL, DATABASE_USER e DATABASE_PASSWORD");
        }

        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("✅ Driver PostgreSQL carregado.");

            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Conexão estabelecida com o banco de dados!");
            return connection;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ Driver PostgreSQL não encontrado.", e);
        } catch (SQLException e) {
            throw new RuntimeException("❌ Erro ao conectar: " + e.getMessage(), e);
        }
    }
}