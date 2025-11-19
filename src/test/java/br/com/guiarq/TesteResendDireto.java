package br.com.guiarq;

import com.resend.Resend;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.CreateEmailOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

public class TesteResendDireto {

    public static void main(String[] args) {
        System.out.println("=== Testando envio direto pelo Resend ===");

        // Pegando a chave direto da variável de ambiente
        String apiKey = System.getenv("RESEND_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("❌ ERRO: Variável RESEND_API_KEY não está configurada!");
            return;
        }

        try {
            Resend resend = new Resend(apiKey);

            // Apenas para teste: cria um PNG fake para anexar
            byte[] fakePng = Files.readAllBytes(Path.of("src/test/resources/fake.png"));
            String base64 = Base64.getEncoder().encodeToString(fakePng);

            Attachment attachment = Attachment.builder()
                    .fileName("teste.png")
                    .content(base64)
                    .build();
            CreateEmailOptions email = CreateEmailOptions.builder()
                    .from("Guia RQ <onboarding@resend.dev>")
                    .to("gropoguiarq@gmail.com")
                    .subject("Teste de Envio - Resend Direto")
                    .html("<h1>Teste funcionando! 🚀</h1><p>Este é um envio direto usando Resend 3.0.0</p>")
                    .attachments(List.of(attachment))
                    .build();

            var result = resend.emails().send(email);

            System.out.println("✅ Email enviado com sucesso!");
            System.out.println("ID do envio: " + result.getId());

        } catch (Exception e) {
            System.out.println("❌ ERRO AO ENVIAR EMAIL:");
            e.printStackTrace();
        }
    }
}
