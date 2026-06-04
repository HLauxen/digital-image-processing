package controller;

import view.MainFrame;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ImageController {

    private final MainFrame frame;
    private static BufferedImage originalImage;
    private BufferedImage transformedImage;

    public enum Operacao {
        DILATACAO,
        EROSAO
    }

    public enum TipoElemento {
        CRUZ,
        QUADRADO
    }

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

        BufferedImage suavizada = mediana();

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

        BufferedImage suavizada = mediana();

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

    public static BufferedImage otsu() {

        BufferedImage suavizada = gaussianBlur(3);

        int largura = suavizada.getWidth();
        int altura = suavizada.getHeight();

        BufferedImage nova = new BufferedImage(
                largura,
                altura,
                BufferedImage.TYPE_INT_RGB
        );

        int[] histograma = new int[256];

        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                int pixel = new Color(suavizada.getRGB(x, y)).getRed();
                histograma[pixel]++;
            }
        }

        int total = largura * altura;

        // 2. Soma total dos níveis de cinza
        float soma = 0;
        for (int i = 0; i < 256; i++) {
            soma += i * histograma[i];
        }

        float somaB = 0;
        int pesoFundo = 0;
        int pesoFrente;

        float varianciaMax = 0;
        int threshold = 0;

        // 3. Encontrar threshold ótimo
        for (int t = 0; t < 256; t++) {
            pesoFundo += histograma[t];
            if (pesoFundo == 0) continue;

            pesoFrente = total - pesoFundo;
            if (pesoFrente == 0) break;

            somaB += (float) (t * histograma[t]);

            float mediaFundo = somaB / pesoFundo;
            float mediaFrente = (soma - somaB) / pesoFrente;

            float varianciaEntre = (float) pesoFundo * pesoFrente *
                    (mediaFundo - mediaFrente) * (mediaFundo - mediaFrente);

            if (varianciaEntre > varianciaMax) {
                varianciaMax = varianciaEntre;
                threshold = t;
            }
        }

        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {

                int pixel = new Color(suavizada.getRGB(x, y)).getRed();

                int valor = (pixel > threshold) ? 255 : 0;

                nova.setRGB(x, y, new Color(valor, valor, valor).getRGB());
            }
        }

        return nova;
    }

    public static BufferedImage mediana() {

        int tamanho = 3;
        int largura = originalImage.getWidth();
        int altura = originalImage.getHeight();

        BufferedImage nova = new BufferedImage(
                largura,
                altura,
                originalImage.getType()
        );

        int raio = tamanho / 2;

        for (int x = raio; x < largura - raio; x++) {
            for (int y = raio; y < altura - raio; y++) {

                int[] r = new int[tamanho * tamanho];
                int[] g = new int[tamanho * tamanho];
                int[] b = new int[tamanho * tamanho];

                int k = 0;

                // percorre vizinhança (kernel)
                for (int i = -raio; i <= raio; i++) {
                    for (int j = -raio; j <= raio; j++) {

                        Color cor = new Color(
                                originalImage.getRGB(x + i, y + j)
                        );

                        r[k] = cor.getRed();
                        g[k] = cor.getGreen();
                        b[k] = cor.getBlue();

                        k++;
                    }
                }

                // ordena os valores
                java.util.Arrays.sort(r);
                java.util.Arrays.sort(g);
                java.util.Arrays.sort(b);

                // pega a mediana
                int meio = r.length / 2;

                Color novaCor = new Color(
                        r[meio],
                        g[meio],
                        b[meio]
                );

                nova.setRGB(x, y, novaCor.getRGB());
            }
        }

        return nova;
    }

    private static int[][] binarizar(BufferedImage img, int threshold) {
        int largura = img.getWidth();
        int altura  = img.getHeight();

        int[][] grid = new int[altura][largura];

        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {

                int rgb = img.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >>  8) & 0xFF;
                int b =  rgb        & 0xFF;

                int cinza = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                grid[y][x] = (cinza >= threshold) ? 255 : 0;
            }
        }

        return grid;
    }

    public static int[][] criarElemento(TipoElemento tipo, int tamanho) {

        if (tamanho % 2 == 0) {
            tamanho++;
        }

        int[][] elemento = new int[tamanho][tamanho];
        int centro = tamanho / 2;

        for (int i = 0; i < tamanho; i++) {
            for (int j = 0; j < tamanho; j++) {

                if (tipo == TipoElemento.QUADRADO) {
                    elemento[i][j] = 1;
                } else if (tipo == TipoElemento.CRUZ) {
                    elemento[i][j] = (i == centro || j == centro) ? 1 : 0;
                }
            }
        }

        return elemento;
    }

    public static BufferedImage processarErosaoDilatacao(
            Operacao operacao,
            TipoElemento tipoElemento,
            int tamanhoElemento
    ) {
        final int largura = originalImage.getWidth();
        final int altura  = originalImage.getHeight();

        final int[][] elementoEstruturante = criarElemento(tipoElemento, tamanhoElemento);

        final int tamanhoReal = elementoEstruturante.length;
        final int offset      = tamanhoReal / 2;

        final int[][] imagemBinaria = binarizar(originalImage, 127);
        final int[][] resultado     = new int[altura][largura];

        if (operacao == Operacao.DILATACAO) {
            dilatacao(imagemBinaria, resultado, elementoEstruturante, tamanhoReal, offset, largura, altura);
        } else {
            erosao(imagemBinaria, resultado, elementoEstruturante, tamanhoReal, offset, largura, altura);
        }

        return getBufferedImage(largura, altura, resultado);
    }

    private static void dilatacao(
            int[][] src, int[][] dst,
            int[][] elem, int tamanho, int offset,
            int largura, int altura
    ) {
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {

                boolean encontrouObjeto = false;

                externo:
                for (int i = 0; i < tamanho; i++) {
                    for (int j = 0; j < tamanho; j++) {

                        if (elem[i][j] == 0) continue;

                        int nx = x + j - offset;
                        int ny = y + i - offset;

                        if (nx < 0 || nx >= largura || ny < 0 || ny >= altura) continue;

                        if (src[ny][nx] == 255) {
                            encontrouObjeto = true;
                            break externo;
                        }
                    }
                }

                dst[y][x] = encontrouObjeto ? 255 : 0;
            }
        }
    }

    private static void erosao(
            int[][] src, int[][] dst,
            int[][] elem, int tamanho, int offset,
            int largura, int altura
    ) {
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {

                boolean todosObjeto = true;

                externo:
                for (int i = 0; i < tamanho; i++) {
                    for (int j = 0; j < tamanho; j++) {

                        if (elem[i][j] == 0) continue;

                        int nx = x + j - offset;
                        int ny = y + i - offset;

                        if (nx < 0 || nx >= largura || ny < 0 || ny >= altura) continue;

                        if (src[ny][nx] == 0) {
                            todosObjeto = false;
                            break externo;
                        }
                    }
                }

                dst[y][x] = todosObjeto ? 255 : 0;
            }
        }
    }

    private static BufferedImage getBufferedImage(int largura, int altura, int[][] resultado) {
        BufferedImage imagemFinal = new BufferedImage(
                largura,
                altura,
                BufferedImage.TYPE_INT_RGB
        );

        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {

                int valor = resultado[y][x];

                int rgb =
                        (valor << 16) |
                                (valor << 8)  |
                                valor;

                imagemFinal.setRGB(x, y, rgb);
            }
        }
        return imagemFinal;
    }

    public static BufferedImage zhangSuen() {

        final int largura = originalImage.getWidth();
        final int altura  = originalImage.getHeight();
        final int threshold = 127;

        final int[][] grid = new int[altura][largura];

        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {

                int rgb = originalImage.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >>  8) & 0xFF;
                int b =  rgb        & 0xFF;

                int cinza = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                grid[y][x] = (cinza >= threshold) ? 1 : 0;
            }
        }

        boolean houveMudanca;

        do {
            houveMudanca = false;

            boolean[][] remover = new boolean[altura][largura];

            for (int y = 1; y < altura - 1; y++) {
                for (int x = 1; x < largura - 1; x++) {

                    if (grid[y][x] == 1 && satisfazPasso1ZhangSuen(grid, x, y)) {
                        remover[y][x] = true;
                        houveMudanca  = true;
                    }
                }
            }

            aplicarRemocaoZhangSuen(grid, remover, altura, largura);

            remover = new boolean[altura][largura];

            for (int y = 1; y < altura - 1; y++) {
                for (int x = 1; x < largura - 1; x++) {

                    if (grid[y][x] == 1 && satisfazPasso2ZhangSuen(grid, x, y)) {
                        remover[y][x] = true;
                        houveMudanca  = true;
                    }
                }
            }

            aplicarRemocaoZhangSuen(grid, remover, altura, largura);

        } while (houveMudanca);

        final int[][] resultado = new int[altura][largura];

        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                resultado[y][x] = (grid[y][x] == 1) ? 255 : 0;
            }
        }

        return getBufferedImage(largura, altura, resultado);
    }

    private static int[] vizinhosZhangSuen(int[][] g, int x, int y) {
        return new int[]{
                g[y-1][x  ],  // p2 - norte
                g[y-1][x+1],  // p3 - nordeste
                g[y  ][x+1],  // p4 - leste
                g[y+1][x+1],  // p5 - sudeste
                g[y+1][x  ],  // p6 - sul
                g[y+1][x-1],  // p7 - sudoeste
                g[y  ][x-1],  // p8 - oeste
                g[y-1][x-1]   // p9 - noroeste
        };
    }

    private static int bZhangSuen(int[] vizinhos) {
        int count = 0;
        for (int v : vizinhos) count += v;
        return count;
    }

    private static int aZhangSuen(int[] vizinhos) {
        int transicoes = 0;
        for (int i = 0; i < vizinhos.length; i++) {
            int atual    = vizinhos[i];
            int proximo  = vizinhos[(i + 1) % vizinhos.length];
            if (atual == 0 && proximo == 1) transicoes++;
        }
        return transicoes;
    }

    private static boolean satisfazPasso1ZhangSuen(int[][] g, int x, int y) {
        int[] v = vizinhosZhangSuen(g, x, y);
        int b = bZhangSuen(v);

        if (b < 2 || b > 6)  return false;
        if (aZhangSuen(v) != 1) return false;
        if (v[0] * v[2] * v[4] != 0) return false; // condição c: p2*p4*p6
        if (v[2] * v[4] * v[6] != 0) return false; // condição d: p4*p6*p8
        return true;
    }

    private static boolean satisfazPasso2ZhangSuen(int[][] g, int x, int y) {
        int[] v = vizinhosZhangSuen(g, x, y);
        int b = bZhangSuen(v);

        if (b < 2 || b > 6)  return false;
        if (aZhangSuen(v) != 1) return false;
        if (v[0] * v[2] * v[6] != 0) return false; // condição c: p2*p4*p8
        if (v[0] * v[4] * v[6] != 0) return false; // condição d: p2*p6*p8
        return true;
    }

    private static void aplicarRemocaoZhangSuen(int[][] grid, boolean[][] remover,
                                                int altura, int largura) {
        for (int y = 0; y < altura; y++)
            for (int x = 0; x < largura; x++)
                if (remover[y][x]) grid[y][x] = 0;
    }

    // =========================================================
    // EXERCÍCIOS FLOODFILL
    // =========================================================

    public static class ResultadoAnalise {
        public final BufferedImage imagem;
        public final String relatorio;

        public ResultadoAnalise(BufferedImage imagem, String relatorio) {
            this.imagem = imagem;
            this.relatorio = relatorio;
        }
    }

    private static BufferedImage copiarImagem(BufferedImage src) {
        BufferedImage copia = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < src.getHeight(); y++)
            for (int x = 0; x < src.getWidth(); x++)
                copia.setRGB(x, y, src.getRGB(x, y));
        return copia;
    }

    private static Color corParaRotulo(int rotulo) {
        float hue = (rotulo * 0.618033988749895f) % 1.0f;
        return Color.getHSBColor(hue, 0.85f, 0.9f);
    }

    // Exercício 1 — Floodfill básico
    // Preenche a região conexa a partir do pixel semente usando BFS (fila).
    // oito=true usa 8-conectividade, false usa 4-conectividade.
    public static BufferedImage floodfill(int seedX, int seedY, Color corSubstituicao, boolean oito) {
        BufferedImage result = copiarImagem(originalImage);
        int targetRGB = result.getRGB(seedX, seedY);
        int replacementRGB = corSubstituicao.getRGB();
        if (targetRGB == replacementRGB) return result;

        int largura = result.getWidth();
        int altura = result.getHeight();

        int[][] dirs4 = {{1,0},{-1,0},{0,1},{0,-1}};
        int[][] dirs8 = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
        int[][] dirs = oito ? dirs8 : dirs4;

        Queue<int[]> fila = new ArrayDeque<>();
        fila.add(new int[]{seedX, seedY});
        result.setRGB(seedX, seedY, replacementRGB);

        while (!fila.isEmpty()) {
            int[] p = fila.poll();
            for (int[] d : dirs) {
                int nx = p[0] + d[0], ny = p[1] + d[1];
                if (nx >= 0 && nx < largura && ny >= 0 && ny < altura
                        && result.getRGB(nx, ny) == targetRGB) {
                    result.setRGB(nx, ny, replacementRGB);
                    fila.add(new int[]{nx, ny});
                }
            }
        }
        return result;
    }

    // Encontra todas as regiões conexas (4-conectividade) na imagem binarizada.
    private static Map<Integer, List<int[]>> encontrarRegioes() {
        int largura = originalImage.getWidth();
        int altura = originalImage.getHeight();
        int[][] bin = binarizar(originalImage, 127);

        // 0 = objeto não visitado, -1 = fundo
        int[][] rotulos = new int[altura][largura];
        for (int y = 0; y < altura; y++)
            for (int x = 0; x < largura; x++)
                if (bin[y][x] == 0) rotulos[y][x] = -1;

        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        Map<Integer, List<int[]>> regioes = new LinkedHashMap<>();
        int rotulo = 1;

        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                if (rotulos[y][x] != 0) continue;
                List<int[]> pixels = new ArrayList<>();
                Queue<int[]> fila = new ArrayDeque<>();
                fila.add(new int[]{x, y});
                rotulos[y][x] = rotulo;
                while (!fila.isEmpty()) {
                    int[] p = fila.poll();
                    pixels.add(p);
                    for (int[] d : dirs) {
                        int nx = p[0]+d[0], ny = p[1]+d[1];
                        if (nx>=0 && nx<largura && ny>=0 && ny<altura && rotulos[ny][nx]==0) {
                            rotulos[ny][nx] = rotulo;
                            fila.add(new int[]{nx, ny});
                        }
                    }
                }
                regioes.put(rotulo, pixels);
                rotulo++;
            }
        }
        return regioes;
    }

    private static BufferedImage construirImagemRotulada(Map<Integer, List<int[]>> regioes) {
        int largura = originalImage.getWidth();
        int altura = originalImage.getHeight();
        BufferedImage img = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < altura; y++)
            for (int x = 0; x < largura; x++)
                img.setRGB(x, y, Color.BLACK.getRGB());
        for (Map.Entry<Integer, List<int[]>> entry : regioes.entrySet()) {
            int rgb = corParaRotulo(entry.getKey()).getRGB();
            for (int[] p : entry.getValue())
                img.setRGB(p[0], p[1], rgb);
        }
        return img;
    }

    // Exercício 2 — Rotulação de regiões conexas
    public static BufferedImage rotularRegioes() {
        Map<Integer, List<int[]>> regioes = encontrarRegioes();
        return construirImagemRotulada(regioes);
    }

    // Exercício 3 — Contagem de objetos
    public static ResultadoAnalise contarObjetos() {
        Map<Integer, List<int[]>> regioes = encontrarRegioes();
        BufferedImage img = construirImagemRotulada(regioes);
        String relatorio = "Total de objetos encontrados: " + regioes.size();
        return new ResultadoAnalise(img, relatorio);
    }

    // Exercício 4 — Cálculo de área por região
    public static ResultadoAnalise calcularArea() {
        Map<Integer, List<int[]>> regioes = encontrarRegioes();
        BufferedImage img = construirImagemRotulada(regioes);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-10s %10s%n", "Região", "Área (px)"));
        sb.append("-".repeat(22)).append("\n");
        for (Map.Entry<Integer, List<int[]>> entry : regioes.entrySet()) {
            sb.append(String.format("%-10d %10d%n", entry.getKey(), entry.getValue().size()));
        }
        sb.append("\nTotal de regiões: ").append(regioes.size());

        return new ResultadoAnalise(img, sb.toString());
    }

    // Exercício 5 — Perímetro e circularidade por região
    // Perímetro: pixels da borda (com ao menos 1 vizinho-4 fora da região).
    // Circularidade: C = (4π * A) / P²  → 1.0 para círculo perfeito.
    public static ResultadoAnalise calcularPerimetroCircularidade() {
        int largura = originalImage.getWidth();
        int altura = originalImage.getHeight();
        Map<Integer, List<int[]>> regioes = encontrarRegioes();
        BufferedImage img = construirImagemRotulada(regioes);

        // mapa pixel → rotulo para checar vizinhança
        int[][] mapa = new int[altura][largura];
        for (Map.Entry<Integer, List<int[]>> entry : regioes.entrySet())
            for (int[] p : entry.getValue())
                mapa[p[1]][p[0]] = entry.getKey();

        int[][] dirs4 = {{1,0},{-1,0},{0,1},{0,-1}};

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-8s %8s %10s %12s%n", "Região", "Área", "Perímetro", "Circularidade"));
        sb.append("-".repeat(42)).append("\n");

        for (Map.Entry<Integer, List<int[]>> entry : regioes.entrySet()) {
            int rotulo = entry.getKey();
            List<int[]> pixels = entry.getValue();
            int area = pixels.size();

            int perimetro = 0;
            for (int[] p : pixels) {
                for (int[] d : dirs4) {
                    int nx = p[0]+d[0], ny = p[1]+d[1];
                    if (nx < 0 || nx >= largura || ny < 0 || ny >= altura || mapa[ny][nx] != rotulo) {
                        perimetro++;
                        break;
                    }
                }
            }

            double circularidade = (perimetro == 0) ? 0 : (4.0 * Math.PI * area) / ((double) perimetro * perimetro);
            sb.append(String.format("%-8d %8d %10d %12.4f%n", rotulo, area, perimetro, circularidade));
        }
        sb.append("\nCircularidade = 1.0 → círculo perfeito");

        return new ResultadoAnalise(img, sb.toString());
    }
}
