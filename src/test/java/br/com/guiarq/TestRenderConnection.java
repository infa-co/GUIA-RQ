package br.com.guiarq;

import java.sql.Connection;
import java.sql.DriverManager;

public class TestRenderConnection {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://dpg-d4f9a1drnu6s73due9f0-a.oregon-postgres.render.com:5432/guia_rq?sslmode=require";
        String user = "guia_rq_user";
        String password = "tbksHYYiSgPKYZmAzDkbsnPagL2uNODj";

        try {
            System.out.println("Tentando conectar...");
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("✅ Conexão bem-sucedida!");
            conn.close();
        } catch (Exception e) {
            System.out.println("❌ Falha ao conectar:");
            e.printStackTrace();
        }
    }
}
