package br.com.guiarq.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

@Component
public class QrCodeGenerator {

    public byte[] generateQrCode(String text) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        var matrix = writer.encode(text, BarcodeFormat.QR_CODE, 300, 300);

        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", baos);

        return baos.toByteArray();
    }
}
