package rs.sep.bank.qr;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IpsQrImageGeneratorTest {

    private final IpsQrImageGenerator imageGenerator = new IpsQrImageGenerator();

    @Test
    void imageContainsTheSameText() throws Exception {
        String text = "K:PR|V:01|C:1|R:845000000040484987|N:JP EPS BEOGRAD|I:RSD3596,13"
                + "|SF:189|S:UPLATA PO RAČUNU ZA EL. ENERGIJU|RO:97163220000111111111000";

        byte[] png = imageGenerator.generatePng(text, 300);

        String decoded = readQr(png);
        assertThat(decoded).isEqualTo(text);
    }

    @Test
    void imageHasRequestedSize() throws Exception {
        byte[] png = imageGenerator.generatePng("K:PR|V:01|C:1", 300);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image.getWidth()).isEqualTo(300);
        assertThat(image.getHeight()).isEqualTo(300);
    }

    @Test
    void rejectsBlankText() {
        assertThatThrownBy(() -> imageGenerator.generatePng("  ", 300))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private String readQr(byte[] png) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        BinaryBitmap bitmap = new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        return new QRCodeReader()
                .decode(bitmap, Map.of(DecodeHintType.CHARACTER_SET, "UTF-8"))
                .getText();
    }


}