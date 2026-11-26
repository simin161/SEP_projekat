package rs.sep.bank.qr;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class IpsQrGeneratorTest {

    private final IpsQrGenerator generator = new IpsQrGenerator();

    @Test
    void generatesNbsExampleText() {
        //Arrange
        IpsQrData data = new IpsQrData(
                "845000000040484987",
                "JP EPS BEOGRAD",
                new BigDecimal("3596.13"),
                "189",
                "UPLATA PO RAČUNU ZA EL. ENERGIJU",
                "97163220000111111111000"
        );

        //Act
        String text = generator.generate(data);

        //Assert
        assertThat(text).isEqualTo(
                "K:PR|V:01|C:1|R:845000000040484987|N:JP EPS BEOGRAD|I:RSD3596,13"
                        + "|SF:189|S:UPLATA PO RAČUNU ZA EL. ENERGIJU|RO:97163220000111111111000"
        );
    }
}