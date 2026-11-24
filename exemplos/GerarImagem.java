import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Random;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;

// Execute da raiz: java exemplos/GerarImagem.java
class GerarImagem {
    public static void main(String[] args) throws Exception {
        var imagem = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        var g = imagem.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(233, 239, 242));
        g.fillRect(0, 0, 800, 600);
        g.setColor(new Color(30, 55, 66));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        g.drawString("PetriLens | PLACA FICTICIA", 38, 45);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        g.drawString("Simulacao de captura - sem material biologico real", 38, 77);
        g.setColor(new Color(120, 146, 152));
        g.fillOval(169, 99, 462, 462);
        g.setColor(new Color(248, 249, 235));
        g.fillOval(176, 106, 448, 448);
        g.setColor(new Color(225, 211, 152));
        g.fillOval(189, 119, 422, 422);
        var random = new Random(6);
        for (int i = 0; i < 48; i++) {
            double angulo = random.nextDouble() * Math.PI * 2;
            double raio = Math.sqrt(random.nextDouble()) * 181;
            int x = 400 + (int) (Math.cos(angulo) * raio);
            int y = 330 + (int) (Math.sin(angulo) * raio);
            int r = 4 + random.nextInt(7);
            g.setColor(new Color(125, 65, 87));
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
            g.setColor(new Color(245, 203, 213));
            g.fillOval(x - r + 2, y - r + 2, r, r);
        }
        g.setColor(new Color(30, 55, 66));
        g.drawString("JPEG 800 x 600 | colonias ilustrativas", 38, 582);
        g.dispose();
        var writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        var parametros = writer.getDefaultWriteParam();
        parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        parametros.setCompressionQuality(0.65f);
        Path jpeg = Path.of("exemplos/placa-ficticia.jpg");
        try (var arquivo = Files.newOutputStream(jpeg);
                var saida = ImageIO.createImageOutputStream(arquivo)) {
            writer.setOutput(saida);
            writer.write(null, new IIOImage(imagem, null, null), parametros);
        } finally {
            writer.dispose();
        }
        byte[] bytes = Files.readAllBytes(jpeg);
        if (bytes.length >= 30000) throw new AssertionError("JPEG deve ter menos de 30 KB");
        String base64 = Base64.getEncoder().encodeToString(bytes);
        Files.writeString(Path.of("firmware/sketch/placa-base64.txt"), base64);
        if (!java.util.Arrays.equals(bytes, Base64.getDecoder().decode(base64))) throw new AssertionError();
        var conferida = ImageIO.read(jpeg.toFile());
        if (conferida.getWidth() != 800 || conferida.getHeight() != 600) throw new AssertionError();
        System.out.printf("JPEG 800x600: %d bytes; Base64: %d bytes; round-trip identico%n", bytes.length, base64.length());
    }
}
