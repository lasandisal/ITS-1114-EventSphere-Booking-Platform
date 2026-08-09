package lk.ijse.eventsphere.service;

public interface QrCodeService {

    // Generates an in-memory PNG (no disk I/O) for the given payload text —
    // the caller passes the already-signed payload from TicketSigningUtil,
    // not a raw ticket code.
    byte[] generateQrPng(String payload, int sizePx);
}
