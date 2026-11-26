package rs.sep.bank.qr;

import java.math.BigDecimal;
import java.util.StringJoiner;

public class IpsQrGenerator {

    private static final String IDENTIFICATION_CODE = "PR";
    private static final String VERSION = "01";
    private static final String CHARACTER_SET = "1";
    private static final String ALLOWED_CHARS = "A-Za-zČĆŽŠĐčćžšđ0-9 !(/@}“):\\[~#*;\\]„$+<\\^”%,=_\"\\&\\->`’‘.?{'";
    private static final String NAME_REGEX = "[" + ALLOWED_CHARS + "\\r\\n]+";
    private static final String SINGLE_LINE_REGEX = "[" + ALLOWED_CHARS + "]+";
    private static final String CURRENCY = "RSD";

    public String generate(IpsQrData data) {
        StringJoiner qr = new StringJoiner("|");
        qr.add("K:" + IDENTIFICATION_CODE);
        qr.add("V:" + VERSION);
        qr.add("C:" + CHARACTER_SET);
        qr.add("R:" + formatAccount(data.recipientAccount()));
        qr.add("N:" + formatName(data.recipientName()));
        qr.add("I:" + formatAmount(data.amount()));
        qr.add("SF:" + formatPaymentCode(data.paymentCode()));
        if (data.purpose() != null && !data.purpose().isEmpty()) {
            qr.add("S:" + formatPurpose(data.purpose()));
        }
        if (data.reference() != null && !data.reference().isEmpty()) {
            qr.add("RO:" + formatReference(data.reference()));
        }

        return qr.toString();
    }

    private String formatAccount(String account){
        if (account == null) {
            throw new IllegalArgumentException("Recipient account is required");
        }
        
        String digits = account.replace("-", "");
        if(!digits.matches("\\d{18}")){
            throw new IllegalArgumentException("Account must have 18 digits: " + account );
        }
        return digits;
    }

    private String formatName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Recipient name is required");
        }
        if (name.length() > 70) {
            throw new IllegalArgumentException("Recipient name must be at most 70 characters: " + name);
        }
        if (name.split("\\R").length > 3) {
            throw new IllegalArgumentException("Recipient name must have at most 3 lines: " + name);
        }
        if (!name.matches(NAME_REGEX)) {
            throw new IllegalArgumentException("Recipient name contains a disallowed character: " + name);
        }
        return name;
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero: " + amount);
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Amount can have at most 2 decimal places: " + amount);
        }
        String value = CURRENCY + amount.setScale(2).toPlainString().replace('.', ',');
        if (value.length() > 18) {
            throw new IllegalArgumentException("Amount is too large: " + amount);
        }
        return value;
    }

    private String formatPaymentCode(String paymentCode) {
        if (paymentCode == null) {
            throw new IllegalArgumentException("Payment code is required");
        }
        if (!paymentCode.matches("[12]\\d{2}")) {
            throw new IllegalArgumentException("Payment code must be 3 digits starting with 1 or 2: " + paymentCode);
        }
        return paymentCode;
    }

    private String formatPurpose(String purpose) {
        if (purpose.length() > 35) {
            throw new IllegalArgumentException("Purpose must be at most 35 characters: " + purpose);
        }
        if (!purpose.matches(SINGLE_LINE_REGEX)) {
            throw new IllegalArgumentException("Purpose contains a disallowed character or a line break: " + purpose);
        }
        return purpose;
    }

    private String formatReference(String reference) {
        if (reference.length() > 25) {
            throw new IllegalArgumentException("Reference must be at most 25 characters: " + reference);
        }
        if (!reference.matches("\\d{2}[0-9-]+")) {
            throw new IllegalArgumentException("Reference must start with a 2-digit model and contain only digits and hyphens: " + reference);
        }
        if (reference.startsWith("97") && reference.contains("-")) {
            throw new IllegalArgumentException("Reference with model 97 must not contain hyphens: " + reference);
        }
        return reference;
    }

}
