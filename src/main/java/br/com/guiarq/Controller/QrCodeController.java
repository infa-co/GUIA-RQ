package br.com.guiarq.Controller;

import br.com.guiarq.utils.QrCodeGenerator;
import com.google.zxing.WriterException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/qr")
public class QrCodeController {

    @GetMapping("/{ticketId}")
    public ResponseEntity<byte[]> gerarQr(@PathVariable String ticketId)
            throws WriterException, IOException {

        String conteudo = "https://guiaranchoqueimado.com.br/ticket/" + ticketId;
        byte[] qrBytes = QrCodeGenerator.generateQrBytes(conteudo);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"ticket-" + ticketId + ".png\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(qrBytes);
    }
}