package rs.sep.bank.qr;

import java.math.BigDecimal;

public record IpsQrData(
        String recipientAccount,   // R  – racun primaoca, 18 cifara
        String recipientName,      // N  – naziv primaoca, max 70
        BigDecimal amount,         // I  – iznos u dinarima
        String paymentCode,        // SF – sifra placanja, 3 cifre
        String purpose,            // S  – svrha (opciono, moze null)
        String reference           // RO – poziv na broj (opciono, moze null)
) {
}
