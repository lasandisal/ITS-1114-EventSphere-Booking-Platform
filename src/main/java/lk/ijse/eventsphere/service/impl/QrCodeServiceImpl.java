package lk.ijse.eventsphere.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lk.ijse.eventsphere.service.QrCodeService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class QrCodeServiceImpl implements QrCodeService {

    @Override
    public byte[] generateQrPng(String payload, int sizePx) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            // A QR that fails to generate for a CONFIRMED payment is a real
            // problem, not something to swallow — surfaces as a 500 so it
            // gets noticed rather than silently shipping a booking with no ticket.
            throw new IllegalStateException("Failed to generate QR code", e);
        }
    }
}
