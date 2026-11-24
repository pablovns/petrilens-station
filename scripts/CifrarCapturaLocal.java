import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

// Somente para a prova curl localhost, com a imagem fictícia e um par de chaves de teste.
class CifrarCapturaLocal {
    public static void main(String[] args) throws Exception {
        String pem = Files.readString(Path.of(args[0])).strip();
        String inicio = "-----BEGIN PUBLIC KEY-----";
        String fim = "-----END PUBLIC KEY-----";
        if (!pem.startsWith(inicio) || !pem.endsWith(fim)) {
            throw new IllegalArgumentException("Forneça apenas a chave pública PEM de teste.");
        }
        var der = Base64.getDecoder().decode(pem.substring(inicio.length(),
                pem.length() - fim.length()).replaceAll("\\s+", ""));
        var publica = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));

        SecureRandom aleatorio = new SecureRandom();
        byte[] chave = new byte[16];
        byte[] iv = new byte[12];
        aleatorio.nextBytes(chave);
        aleatorio.nextBytes(iv);
        var rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, publica, new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT), aleatorio);
        var gcm = Cipher.getInstance("AES/GCM/NoPadding");
        gcm.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(chave, "AES"), new GCMParameterSpec(128, iv));
        var base64 = Base64.getEncoder();
        byte[] jpeg = Files.readAllBytes(Path.of(args[1]));
        String corpo = Files.readString(Path.of(args[2]))
                .replace("\"L2026-001\"", "\"L2026-002\"")
                .replace("\"A1\"", "\"A5\"")
                .replace("<Base64 RSA-OAEP>", base64.encodeToString(rsa.doFinal(chave)))
                .replace("<Base64 IV>", base64.encodeToString(iv))
                .replace("<Base64 AES-GCM>", base64.encodeToString(gcm.doFinal(jpeg)));
        Files.writeString(Path.of(args[3]), corpo);
    }
}
