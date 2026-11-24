package br.com.petrilens.captura;

/** Limites do bloco PEM de uma chave em PKCS#8 sem senha. */
public enum DelimitadorPemPkcs8 {
    BEGIN, END;

    private static final String TIPO = "PRIVATE KEY";

    public String texto() {
        return "-----%s %s-----".formatted(name(), TIPO);
    }
}
