package rs.sep.bank.qr;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class IpsQrValidatorTest {

    private final IpsQrValidator validator = new IpsQrValidator();

    //vraca isti tekst od kojeg je napravljen
    @Test
    void parsesNbsExampleText() {
        String text = "K:PR|V:01|C:1|R:845000000040484987|N:JP EPS BEOGRAD|I:RSD3596,13"
                + "|SF:189|S:UPLATA PO RAČUNU ZA EL. ENERGIJU|RO:97163220000111111111000";

        IpsQrData data = validator.validate(text);

        assertThat(data).isEqualTo(new IpsQrData(
                "845000000040484987",
                "JP EPS BEOGRAD",
                new BigDecimal("3596.13"),
                "189",
                "UPLATA PO RAČUNU ZA EL. ENERGIJU",
                "97163220000111111111000"
        ));
    }

    @Test
    void rejectsTextEndingWithPipe() {
        assertThatThrownBy(() -> validator.validate("K:PR|V:01|C:1|"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTextStartingWithPipe() {
        assertThatThrownBy(() -> validator.validate("|K:PR|V:01|C:1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmptyValue() {
        assertThatThrownBy(() -> validator.validate("K:PR|V:01|C:1|S:|R:845000000040484987"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDuplicateTag() {
        assertThatThrownBy(() -> validator.validate("K:PR|V:01|C:1|C:1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnknownTag() {
        assertThatThrownBy(() -> validator.validate("K:PR|V:01|C:1|X:123"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsFieldWithoutColon() {
        assertThatThrownBy(() -> validator.validate("K:PR|V:01|C:1|bezdvotacke"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankText() {
        assertThatThrownBy(() -> validator.validate("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingRequiredTag() {
        assertThatThrownBy(() -> validator.validate(
                "K:PR|V:01|C:1|N:JP EPS BEOGRAD|I:RSD3596,13|SF:189"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    //K
    @Test
    void rejectsWrongIdentificationCode() {
        assertThatThrownBy(() -> validator.validate(
                "K:PT|V:01|C:1|R:845000000040484987|N:JP EPS BEOGRAD|I:RSD3596,13|SF:189"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    //V
    @Test
    void rejectsWrongVersion() {
        assertThatThrownBy(() -> validator.validate(
                "K:PR|V:02|C:1|R:845000000040484987|N:JP EPS BEOGRAD|I:RSD3596,13|SF:189"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    //R
    @Test
    void rejectsAccountWithHyphens() {
        assertThatThrownBy(() -> validator.validate(
                "K:PR|V:01|C:1|R:845-0000000404849-87|N:JP EPS BEOGRAD|I:RSD3596,13|SF:189"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    //R
    @Test
    void rejectsAccountWith17Digits() {
        assertThatThrownBy(() -> validator.validate(
                "K:PR|V:01|C:1|R:84500000004048498|N:JP EPS BEOGRAD|I:RSD3596,13|SF:189"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // I
    @Test
    void rejectsAmountWithoutDigitBeforeComma() {
        assertThatThrownBy(() -> validator.validate(
                "K:PR|V:01|C:1|R:845000000040484987|N:JP EPS BEOGRAD|I:RSD,01|SF:189"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // I
    @Test
    void rejectsAmountWithDot() {
        assertThatThrownBy(() -> validator.validate(
                "K:PR|V:01|C:1|R:845000000040484987|N:JP EPS BEOGRAD|I:RSD3596.13|SF:189"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // I
    @Test
    void rejectsAmountWithSpace() {
        assertThatThrownBy(() -> validator.validate(
                "K:PR|V:01|C:1|R:845000000040484987|N:JP EPS BEOGRAD|I:RSD 3596,13|SF:189"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // I
    @Test
    void rejectsZeroAmount() {
        assertThatThrownBy(() -> validator.validate(
                "K:PR|V:01|C:1|R:845000000040484987|N:JP EPS BEOGRAD|I:RSD0,00|SF:189"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // I
    @Test
    void acceptsAmountWithoutDecimals() {
        IpsQrData data = validator.validate(
                "K:PR|V:01|C:1|R:845000000040484987|N:JP EPS BEOGRAD|I:RSD1025,|SF:189");

        assertThat(data.amount()).isEqualTo(new BigDecimal("1025.00"));
    }

    // I
    @Test
    void acceptsAmountWithOneDecimal() {
        IpsQrData data = validator.validate(
                "K:PR|V:01|C:1|R:845000000040484987|N:JP EPS BEOGRAD|I:RSD1025,1|SF:189");

        assertThat(data.amount()).isEqualTo(new BigDecimal("1025.10"));
    }

    // N
    @Test
    void acceptsNameWithColon() {
        IpsQrData data = validator.validate(
                "K:PR|V:01|C:1|R:845000000040484987|N:FIRMA: PARKING|I:RSD100,00|SF:221");

        assertThat(data.recipientName()).isEqualTo("FIRMA: PARKING");
    }

    // P
    @Test
    void ignoresPayerTag() {
        IpsQrData data = validator.validate(
                "K:PR|V:01|C:1|R:845000000040484987|N:JP EPS BEOGRAD|I:RSD100,00|P:PETAR PETROVIC|SF:189");

        assertThat(data.recipientName()).isEqualTo("JP EPS BEOGRAD");
    }

    // S, RO
    @Test
    void optionalFieldsAreNullWhenMissing() {
        IpsQrData data = validator.validate(
                "K:PR|V:01|C:1|R:845000000040484987|N:JP EPS BEOGRAD|I:RSD100,00|SF:189");

        assertThat(data.purpose()).isNull();
        assertThat(data.reference()).isNull();
    }

    // N
    @Test
    void rejectsNameLongerThan70() {
        assertThatThrownBy(() -> validator.validate(
                "K:PR|V:01|C:1|R:845000000040484987|N:" + "A".repeat(71) + "|I:RSD100,00|SF:189"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // SF
    @Test
    void rejectsPaymentCodeStartingWith3() {
        assertThatThrownBy(() -> validator.validate(
                "K:PR|V:01|C:1|R:845000000040484987|N:JP EPS BEOGRAD|I:RSD100,00|SF:389"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // RO
    @Test
    void rejectsReferenceWithSpace() {
        assertThatThrownBy(() -> validator.validate(
                "K:PR|V:01|C:1|R:845000000040484987|N:JP EPS BEOGRAD|I:RSD100,00|SF:189|RO:0012 34"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // generator -> validator: krug vraca iste podatke
    @Test
    void readsBackWhatGeneratorWrote() {
        IpsQrData original = new IpsQrData(
                "845000000040484987", "PARKING SERVIS DOO", new BigDecimal("300.00"),
                "221", "PARKING ZONA 1", "001234");

        String text = new IpsQrGenerator().generate(original);
        IpsQrData readBack = validator.validate(text);

        assertThat(readBack).isEqualTo(original);
    }
}
