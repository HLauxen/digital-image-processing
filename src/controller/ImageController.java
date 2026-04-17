package controller;

import view.MainFrame;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageController {

    private final MainFrame frame;
    private static BufferedImage originalImage;
    private BufferedImage transformedImage;

    public ImageController(MainFrame frame) {
        this.frame = frame;
    }

    public void openImage() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                originalImage = ImageIO.read(file);
                transformedImage = originalImage;

                frame.getOriginalPanel().setImage(originalImage);
                frame.getTransformedPanel().setImage(transformedImage);

            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Erro ao abrir imagem.");
            }
        }
    }

    public void saveImage() {
        if (transformedImage == null) return;

        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                ImageIO.write(transformedImage, "png", file);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Erro ao salvar imagem.");
            }
        }
    }

    public void showAbout() {
        JOptionPane.showMessageDialog(frame,
                "Sistema de Processamento Digital de Imagens\nTrabalho Acadêmico 2026");
    }

    public static int[] aplicarTransformacao(int x, int y, double[][] m) {
        int xd = (int) Math.round(m[0][0] * x + m[0][1] * y + m[0][2]);
        int yd = (int) Math.round(m[1][0] * x + m[1][1] * y + m[1][2]);
        return new int[]{xd, yd};
    }

    public static BufferedImage transformar(double[][] matriz, int novaLargura, int novaAltura) {
        int largura = originalImage.getWidth();
        int altura = originalImage.getHeight();

        BufferedImage nova = new BufferedImage(novaLargura, novaAltura, originalImage.getType());

        Graphics2D g2d = nova.createGraphics();
        g2d.setColor(new Color(238, 238, 238));
        g2d.fillRect(0, 0, novaLargura, novaAltura);
        g2d.dispose();

        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {

                int rgb = originalImage.getRGB(x, y);

                int[] novo = aplicarTransformacao(x, y, matriz);
                int xd = novo[0];
                int yd = novo[1];

                if (xd >= 0 && xd < novaLargura && yd >= 0 && yd < novaAltura) {
                    nova.setRGB(xd, yd, rgb);
                }
            }
        }

        return nova;
    }

    public static BufferedImage transladar(int tx, int ty) {
        double[][] m = {
                {1, 0, tx},
                {0, 1, ty},
                {0, 0, 1}
        };

        int largura = originalImage.getWidth() + Math.abs(tx);
        int altura = originalImage.getHeight() + Math.abs(ty);

        return transformar(m, largura, altura);
    }

    public static BufferedImage escalar(double sx, double sy) {
        double[][] m = {
                {sx, 0, 0},
                {0, sy, 0},
                {0, 0, 1}
        };

        int largura = (int) (originalImage.getWidth() * sx);
        int altura = (int) (originalImage.getHeight() * sy);

        return transformar(m, largura, altura);
    }

    public static BufferedImage espelharHorizontal() {
        int largura = originalImage.getWidth();
        int altura = originalImage.getHeight();

        double[][] m = {
                {-1, 0, largura},
                {0, 1, 0},
                {0, 0, 1}
        };

        return transformar(m, largura, altura);
    }

    public static BufferedImage espelharVertical() {
        int largura = originalImage.getWidth();
        int altura = originalImage.getHeight();

        double[][] m = {
                {1, 0, 0},
                {0, -1, altura},
                {0, 0, 1}
        };

        return transformar(m, largura, altura);
    }

    public static BufferedImage rotacionar(double graus) {
        double rad = Math.toRadians(graus);

        int largura = originalImage.getWidth();
        int altura = originalImage.getHeight();

        double cx = largura / 2.0;
        double cy = altura / 2.0;

        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double[][] m = {
                {cos, -sin, cx - cx * cos + cy * sin},
                {sin,  cos, cy - cx * sin - cy * cos},
                {0, 0, 1}
        };

        return transformar(m, largura, altura);
    }

    private static int limitar(int valor) {
        return Math.max(0, Math.min(255, valor));
    }

    // grayscale = r + g + b / 3.

    public static BufferedImage grayscale() {
        int largura = originalImage.getWidth();
        int altura = originalImage.getHeight();

        BufferedImage nova = new BufferedImage(
                largura,
                altura,
                originalImage.getType()
        );

        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {

                int rgb = originalImage.getRGB(x, y);
                Color cor = new Color(rgb);

                int gray = (cor.getRed() + cor.getGreen() + cor.getBlue()) / 3;

                Color novaCor = new Color(gray, gray, gray);
                nova.setRGB(x, y, novaCor.getRGB());
            }
        }

        return nova;
    }

    public static BufferedImage ajustarBrilho(int brilho) {
        int largura = originalImage.getWidth();
        int altura = originalImage.getHeight();

        BufferedImage nova = new BufferedImage(
                largura,
                altura,
                originalImage.getType()
        );

        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {

                Color cor = new Color(originalImage.getRGB(x, y));

                int r = limitar(cor.getRed() + brilho);
                int g = limitar(cor.getGreen() + brilho);
                int b = limitar(cor.getBlue() + brilho);

                nova.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }

        return nova;
    }

    // imgDestinho = contraste * (x, y) + brilho

    public static BufferedImage ajustarContraste(double contraste) {
        int largura = originalImage.getWidth();
        int altura = originalImage.getHeight();

        BufferedImage nova = new BufferedImage(
                largura,
                altura,
                originalImage.getType()
        );

        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {

                Color cor = new Color(originalImage.getRGB(x, y));

                int r = limitar((int) (contraste * cor.getRed()));
                int g = limitar((int) (contraste * cor.getGreen()));
                int b = limitar((int) (contraste * cor.getBlue()));

                nova.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }

        return nova;
    }

    private static double[][] gerarKernelGaussiano(int tamanho) {
        double[][] kernel = new double[tamanho][tamanho];

        int raio = tamanho / 2;
        double sigma = tamanho / 3.0;
        double soma = 0;

        for (int x = -raio; x <= raio; x++) {
            for (int y = -raio; y <= raio; y++) {

                double valor = Math.exp(-(x * x + y * y) / (2 * sigma * sigma));
                kernel[x + raio][y + raio] = valor;
                soma += valor;
            }
        }

        for (int i = 0; i < tamanho; i++) {
            for (int j = 0; j < tamanho; j++) {
                kernel[i][j] /= soma;
            }
        }

        return kernel;
    }

    public static BufferedImage gaussianBlur(int tamanho) {
        int largura = originalImage.getWidth();
        int altura = originalImage.getHeight();

        BufferedImage nova = new BufferedImage(
                largura,
                altura,
                originalImage.getType()
        );

        int raio = tamanho / 2;

        double[][] kernel = gerarKernelGaussiano(tamanho);
        double somaKernel = 0;

        for (int i = 0; i < tamanho; i++) {
            for (int j = 0; j < tamanho; j++) {
                somaKernel += kernel[i][j];
            }
        }

        for (int x = raio; x < largura - raio; x++) {
            for (int y = raio; y < altura - raio; y++) {

                double r = 0, g = 0, b = 0;

                for (int i = -raio; i <= raio; i++) {
                    for (int j = -raio; j <= raio; j++) {

                        int rgb = originalImage.getRGB(x + i, y + j);
                        Color cor = new Color(rgb);

                        double peso = kernel[i + raio][j + raio];

                        r += cor.getRed() * peso;
                        g += cor.getGreen() * peso;
                        b += cor.getBlue() * peso;
                    }
                }

                int novoR = (int)(r / somaKernel);
                int novoG = (int)(g / somaKernel);
                int novoB = (int)(b / somaKernel);

                Color novaCor = new Color(
                        Math.min(255, novoR),
                        Math.min(255, novoG),
                        Math.min(255, novoB)
                );

                nova.setRGB(x, y, novaCor.getRGB());
            }
        }

        return nova;
    }

    public static BufferedImage roberts() {

        BufferedImage suavizada = gaussianBlur(3);

        int largura = suavizada.getWidth();
        int altura = suavizada.getHeight();

        BufferedImage nova = new BufferedImage(
                largura,
                altura,
                BufferedImage.TYPE_INT_RGB
        );

        int threshold = 30;

        for (int x = 0; x < largura - 1; x++) {
            for (int y = 0; y < altura - 1; y++) {

                int p1 = new Color(suavizada.getRGB(x, y)).getRed();
                int p2 = new Color(suavizada.getRGB(x + 1, y)).getRed();
                int p3 = new Color(suavizada.getRGB(x, y + 1)).getRed();
                int p4 = new Color(suavizada.getRGB(x + 1, y + 1)).getRed();

                int g1 = p1 - p4;
                int g2 = p2 - p3;

                int g = (int) Math.sqrt(g1 * g1 + g2 * g2);

                int valor = (g > threshold) ? 255 : 0;

                nova.setRGB(x, y, new Color(valor, valor, valor).getRGB());
            }
        }

        return nova;
    }

    public static BufferedImage marrHildreth() {

        BufferedImage suavizada = gaussianBlur(3);

        int largura = suavizada.getWidth();
        int altura = suavizada.getHeight();

        BufferedImage nova = new BufferedImage(
                largura,
                altura,
                BufferedImage.TYPE_INT_RGB
        );

        int threshold = 15;

        // Kernel Laplaciano simples (3x3)
        int[][] laplaciano = {
                { 0, -1,  0 },
                {-1,  4, -1 },
                { 0, -1,  0 }
        };

        double[][] lap = new double[largura][altura];

        for (int x = 1; x < largura - 1; x++) {
            for (int y = 1; y < altura - 1; y++) {

                double soma = 0;

                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {

                        int pixel = new Color(
                                suavizada.getRGB(x + i, y + j)
                        ).getRed();

                        soma += pixel * laplaciano[i + 1][j + 1];
                    }
                }

                lap[x][y] = soma;
            }
        }

        for (int x = 1; x < largura - 1; x++) {
            for (int y = 1; y < altura - 1; y++) {

                boolean isBorda = false;
                double atual = lap[x][y];

                // verifica vizinhos (mudança de sinal)
                for (int i = -1; i <= 1 && !isBorda; i++) {
                    for (int j = -1; j <= 1; j++) {

                        double vizinho = lap[x + i][y + j];

                        if ((atual > 0 && vizinho < 0) ||
                                (atual < 0 && vizinho > 0)) {

                            if (Math.abs(atual - vizinho) > threshold) {
                                isBorda = true;
                                break;
                            }
                        }
                    }
                }

                int valor = isBorda ? 255 : 0;
                nova.setRGB(x, y, new Color(valor, valor, valor).getRGB());
            }
        }

        return nova;
    }
}
