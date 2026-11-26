package rs.sep.bank.qr;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IpsQrGeneratorTest {

    private final IpsQrGenerator generator = new IpsQrGenerator();

    @Test
    void generatesNbsExampleText() {
        IpsQrData data = new IpsQrData(
                "845000000040484987",
                "JP EPS BEOGRAD",
                new BigDecimal("3596.13"),
                "189",
                "UPLATA PO RAČUNU ZA EL. ENERGIJU",
                "97163220000111111111000"
        );

        String text = generator.generate(data);

        assertThat(text).isEqualTo(
                "K:PR|V:01|C:1|R:845000000040484987|N:JP EPS BEOGRAD|I:RSD3596,13"
                        + "|SF:189|S:UPLATA PO RAČUNU ZA EL. ENERGIJU|RO:97163220000111111111000"
        );
    }

    @Test
    void rejectsAccountWith17Digits() {
        IpsQrData data = new IpsQrData(
                "84500000004048498",
                "JP EPS BEOGRAD",
                new BigDecimal("3596.13"),
                "189",
                null,
                null
        );

        assertThatThrownBy(() -> generator.generate(data))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAccountWithLetter() {
        IpsQrData data = new IpsQrData(
                "84500000004048498A", "JP EPS BEOGRAD", new BigDecimal("3596.13"), "189", null, null);

        assertThatThrownBy(() -> generator.generate(data))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullAccount() {
        IpsQrData data = new IpsQrData(
                null, "JP EPS BEOGRAD", new BigDecimal("3596.13"), "189", null, null);

        assertThatThrownBy(() -> generator.generate(data))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNameLongerThan70() {
        IpsQrData data = new IpsQrData(
                "845000000040484987", "A".repeat(71), new BigDecimal("3596.13"), "189", null, null);

        assertThatThrownBy(() -> generator.generate(data))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNameWithPipe() {
        IpsQrData data = new IpsQrData(
                "845000000040484987", "JP EPS|BEOGRAD", new BigDecimal("3596.13"), "189", null, null);

        assertThatThrownBy(() -> generator.generate(data))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsZeroAmount() {
        IpsQrData data = new IpsQrData(
                "845000000040484987", "JP EPS BEOGRAD", BigDecimal.ZERO, "189", null, null);

        assertThatThrownBy(() -> generator.generate(data))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAmountWith3Decimals() {
        IpsQrData data = new IpsQrData(
                "845000000040484987", "JP EPS BEOGRAD", new BigDecimal("1.005"), "189", null, null);

        assertThatThrownBy(() -> generator.generate(data))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsPaymentCodeStartingWith3() {
        IpsQrData data = new IpsQrData(
                "845000000040484987", "JP EPS BEOGRAD", new BigDecimal("3596.13"), "389", null, null);

        assertThatThrownBy(() -> generator.generate(data))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsPurposeWithLineBreak() {
        IpsQrData data = new IpsQrData(
                "845000000040484987", "JP EPS BEOGRAD", new BigDecimal("3596.13"), "189", "UPLATA\nPARKING", null);

        assertThatThrownBy(() -> generator.generate(data))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsReferenceWithSpace() {
        IpsQrData data = new IpsQrData(
                "845000000040484987", "JP EPS BEOGRAD", new BigDecimal("3596.13"), "189", null, "0012 34");

        assertThatThrownBy(() -> generator.generate(data))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsModel97WithHyphen() {
        IpsQrData data = new IpsQrData(
                "845000000040484987", "JP EPS BEOGRAD", new BigDecimal("3596.13"), "189", null, "97-14123412");

        assertThatThrownBy(() -> generator.generate(data))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void omitsOptionalFieldsWhenNull() {
        IpsQrData data = new IpsQrData(
                "845000000040484987", "JP EPS BEOGRAD", new BigDecimal("3596.13"), "189", null, null);

        String text = generator.generate(data);

        assertThat(text).isEqualTo(
                "K:PR|V:01|C:1|R:845000000040484987|N:JP EPS BEOGRAD|I:RSD3596,13|SF:189");
    }

    @Test
    void removesHyphensFromAccount() {
        IpsQrData data = new IpsQrData(
                "845-0000000404849-87", "JP EPS BEOGRAD", new BigDecimal("3596.13"), "189", null, null);

        String text = generator.generate(data);

        assertThat(text).contains("|R:845000000040484987|");
    }
}