package rs.sep.bank.qr;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IpsQrValidator {

    private static final Set<String> ALLOWED_TAGS = Set.of("K", "V", "C", "R", "N", "I", "P", "SF", "S", "RO");
    private static final List<String> REQUIRED_TAGS = List.of("K", "V", "C", "R", "N", "I", "SF");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("RSD(\\d+),(\\d{0,2})");
    private final IpsQrGenerator generator = new IpsQrGenerator();

    public IpsQrData validate(String text) {
        Map<String, String> fields = splitIntoFields(text);
        checkRequiredTags(fields);
        checkFixedValue(fields, "K", IpsQrGenerator.IDENTIFICATION_CODE);
        checkFixedValue(fields, "V", IpsQrGenerator.VERSION);
        checkFixedValue(fields, "C", IpsQrGenerator.CHARACTER_SET);
        String account = parseAccount(fields.get("R"));
        BigDecimal amount = parseAmount(fields.get("I"));

        IpsQrData data = new IpsQrData(
                account,
                fields.get("N"),
                amount,
                fields.get("SF"),
                fields.get("S"),
                fields.get("RO")
        );

        generator.generate(data);
        return data;
    }

    private Map<String, String> splitIntoFields(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("QR text is required");
        }

        Map<String, String> fields = new LinkedHashMap<>();

        for (String part : text.split("\\|", -1)) {
            int colon = part.indexOf(':');
            if (colon <= 0) {
                throw new IllegalArgumentException("Field without tag: '" + part + "'");
            }

            String tag = part.substring(0, colon);
            String value = part.substring(colon + 1);

            if (!ALLOWED_TAGS.contains(tag)) {
                throw new IllegalArgumentException("Unknown tag: " + tag);
            }

            if (value.isEmpty()) {
                throw new IllegalArgumentException("Empty value for tag: " + tag);
            }

            if (fields.containsKey(tag)) {
                throw new IllegalArgumentException("Duplicate tag: " + tag);
            }
            fields.put(tag, value);
        }
        return fields;
    }

    private void checkRequiredTags(Map<String, String> fields) {
        for (String tag : REQUIRED_TAGS) {
            if (!fields.containsKey(tag)) {
                throw new IllegalArgumentException("Missing required tag: " + tag);
            }
        }
    }

    private void checkFixedValue(Map<String, String> fields, String tag, String expected) {
        if (!expected.equals(fields.get(tag))) {
            throw new IllegalArgumentException("Tag " + tag + " must be " + expected + ", but was: " + fields.get(tag));
        }
    }

    private String parseAccount(String value) {
        if (!value.matches("\\d{18}")) {
            throw new IllegalArgumentException("Account must be exactly 18 digits, without hyphens: " + value);
        }
        return value;
    }

    private BigDecimal parseAmount(String value) {
        if (value.length() < 5 || value.length() > 18) {
            throw new IllegalArgumentException("Amount must have 5 to 18 characters: " + value);
        }
        Matcher matcher = AMOUNT_PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Amount must look like RSD1234,56: " + value);
        }
        String whole = matcher.group(1);
        String decimals = matcher.group(2);
        BigDecimal amount = new BigDecimal(whole + "." + (decimals.isEmpty() ? "0" : decimals)).setScale(2);
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero: " + value);
        }
        return amount;
    }



}
