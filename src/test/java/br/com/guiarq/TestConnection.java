package br.com.guiarq;

import java.sql.Connection;
import java.sql.DriverManager;

public class TestConnection {
    public static void main(String[] args) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🔍 TESTE DE VARIÁVEIS DE AMBIENTE E CONEXÃO");
        System.out.println("=".repeat(60) + "\n");

        // Verifica variáveis individuais
        System.out.println("📋 Variáveis Individuais:");
        System.out.println("   DATABASE_HOST: " + System.getenv("DATABASE_HOST"));
        System.out.println("   DATABASE_PORT: " + System.getenv("DATABASE_PORT"));
        System.out.println("   DATABASE_NAME: " + System.getenv("DATABASE_NAME"));
        System.out.println("   DATABASE_USER: " + System.getenv("DATABASE_USER"));

        String password = System.getenv("DATABASE_PASSWORD");
        System.out.println("   DATABASE_PASSWORD: " + (password != null ? "***CONFIGURADA*** (tamanho: " + password.length() + " caracteres)" : "❌ NÃO CONFIGURADA"));

        String databaseUrl = System.getenv("DATABASE_URL");
        System.out.println("\n📋 Variável Completa:");
        System.out.println("   DATABASE_URL: " + (databaseUrl != null ? databaseUrl : "❌ NÃO CONFIGURADA"));

        String stripeKey = System.getenv("STRIPE_SECRET_KEY");
        System.out.println("\n📋 Stripe:");
        System.out.println("   STRIPE_SECRET_KEY: " + (stripeKey != null ? "✅ CONFIGURADA (tamanho: " + stripeKey.length() + " caracteres)" : "❌ NÃO CONFIGURADA"));

        System.out.println("\n" + "-".repeat(60));
        System.out.println("🔌 TESTANDO CONEXÃO COM O BANCO DE DADOS");
        System.out.println("-".repeat(60) + "\n");

        // Testa a conexão
        if (databaseUrl == null || databaseUrl.isEmpty()) {
            System.out.println("❌ ERRO: DATABASE_URL não está configurada!");
            System.out.println("\n💡 SOLUÇÃO:");
            System.out.println("   1. Feche o IntelliJ completamente");
            System.out.println("   2. Abra as Variáveis de Ambiente do Windows");
            System.out.println("   3. Adicione DATABASE_URL com o valor completo");
            System.out.println("   4. Reabra o IntelliJ");
            return;
        }

        String user = System.getenv("DATABASE_USER");
        String pass = System.getenv("DATABASE_PASSWORD");

        if (user == null || pass == null) {
            System.out.println("❌ ERRO: DATABASE_USER ou DATABASE_PASSWORD não estão configuradas!");
            return;
        }

        try {
            System.out.println("⏳ Carregando driver PostgreSQL...");
            Class.forName("org.postgresql.Driver");
            System.out.println("✅ Driver carregado com sucesso!\n");

            System.out.println("⏳ Conectando ao banco de dados...");
            System.out.println("   URL: " + databaseUrl);
            System.out.println("   Usuário: " + user);

            Connection conn = DriverManager.getConnection(databaseUrl, user, pass);

            System.out.println("\n" + "=".repeat(60));
            System.out.println("✅✅✅ CONEXÃO BEM-SUCEDIDA! ✅✅✅");
            System.out.println("=".repeat(60));
            System.out.println("🎉 Banco de dados: " + conn.getMetaData().getDatabaseProductName());
            System.out.println("🎉 Versão: " + conn.getMetaData().getDatabaseProductVersion());
            System.out.println("=".repeat(60) + "\n");

            conn.close();
            System.out.println("✅ Conexão fechada com sucesso!");

        } catch (ClassNotFoundException e) {
            System.out.println("\n❌ ERRO: Driver PostgreSQL não encontrado!");
            System.out.println("💡 Verifique se a dependência está no pom.xml");
            e.printStackTrace();

        } catch (java.sql.SQLException e) {
            System.out.println("\n❌ ERRO AO CONECTAR AO BANCO DE DADOS!");
            System.out.println("💡 Possíveis causas:");
            System.out.println("   1. URL de conexão incorreta");
            System.out.println("   2. Usuário ou senha incorretos");
            System.out.println("   3. Banco de dados inacessível");
            System.out.println("   4. Firewall bloqueando a conexão");
            System.out.println("\n📋 Detalhes do erro:");
            System.out.println("   Mensagem: " + e.getMessage());
            System.out.println("   SQLState: " + e.getSQLState());
            System.out.println("   Código: " + e.getErrorCode());
            System.out.println("\n📜 Stack trace completo:");
            e.printStackTrace();
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("FIM DO TESTE");
        System.out.println("=".repeat(60) + "\n");
    }
}