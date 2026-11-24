package br.com.petrilens.captura;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
class ImagemCifrada {
    private static final OAEPParameterSpec OAEP = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
    private final RSAPrivateKey chavePrivada;

    ImagemCifrada(@Value("${petrilens.capturas.chave-privada}") String caminho) {
        chavePrivada = lerChavePrivada(caminho);
    }

    static class ChaveIndisponivel extends RuntimeException {}

    byte[] decifrar(CapturaInformada captura) {
        try {
            byte[] chaveCifrada = decodificar(captura.chaveCifrada());
            byte[] iv = decodificar(captura.iv());
            byte[] imagemCifrada = decodificar(captura.imagemCifrada());
            if (chaveCifrada.length != 256 || iv.length != 12 || imagemCifrada.length < 20) {
                throw new IllegalArgumentException();
            }
            if (chavePrivada == null) {
                throw new ChaveIndisponivel();
            }

            byte[] chaveAes = decifrarChaveAes(chaveCifrada);
            try {
                return decifrarJpeg(imagemCifrada, chaveAes, iv);
            } finally {
                Arrays.fill(chaveAes, (byte) 0);
            }
        } catch (GeneralSecurityException | IllegalArgumentException erro) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private byte[] decifrarChaveAes(byte[] chaveCifrada) throws GeneralSecurityException {
        // RSA-OAEP usa SHA-256 também no MGF1 para recuperar a chave AES enviada pela estação.
        Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsa.init(Cipher.DECRYPT_MODE, chavePrivada, OAEP);
        return rsa.doFinal(chaveCifrada);
    }

    private byte[] decifrarJpeg(byte[] imagemCifrada, byte[] chaveAes, byte[] iv)
            throws GeneralSecurityException {
        if (chaveAes.length != 16) {
            throw new IllegalArgumentException();
        }
        // AES-GCM verifica a integridade do JPEG com a tag de 16 bytes anexada à imagem cifrada.
        Cipher gcm = Cipher.getInstance("AES/GCM/NoPadding");
        gcm.init(Cipher.DECRYPT_MODE, new SecretKeySpec(chaveAes, "AES"), new GCMParameterSpec(128, iv));
        return gcm.doFinal(imagemCifrada);
    }

    private static byte[] decodificar(String base64) {
        if (base64.length() % 4 != 0) {
            throw new IllegalArgumentException();
        }
        return Base64.getDecoder().decode(base64);
    }

    private static RSAPrivateKey lerChavePrivada(String caminho) {
        if (caminho.isBlank()) {
            return null;
        }
        String inicio = DelimitadorPemPkcs8.BEGIN.texto();
        String fim = DelimitadorPemPkcs8.END.texto();
        try (var entrada = Files.newInputStream(Path.of(caminho))) {
            byte[] bytes = entrada.readNBytes(8193);
            if (bytes.length > 8192) {
                return null;
            }
            String pem = new String(bytes, StandardCharsets.US_ASCII).strip();
            if (!pem.startsWith(inicio) || !pem.endsWith(fim)) {
                return null;
            }
            byte[] der = Base64.getDecoder().decode(pem.substring(inicio.length(),
                    pem.length() - fim.length()).replaceAll("\\s+", ""));
            var chave = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
            return chave instanceof RSAPrivateKey rsa && rsa.getModulus().bitLength() == 2048 ? rsa : null;
        } catch (IOException | GeneralSecurityException | IllegalArgumentException erro) {
            return null;
        }
    }
}
