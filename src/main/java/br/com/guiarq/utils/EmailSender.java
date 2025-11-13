package br.com.guiarq.utils;

public class EmailSender  {
    public void enviarQRCode(String email, String assunto, String mensagem, byte[] qrBytes) {
        // Aqui entra a API de e-mail (Resend, Gmail SMTP, etc.)
        System.out.println("Enviando QR Code para " + email);
    }
}
