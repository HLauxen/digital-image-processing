package controller;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class DesafioController {

    public enum TipoDesafio {
        RELOGIO, OBJETOS_COLORIDOS, LETRAS, PLACAS, BARRAS
    }

    public static class ResultadoDesafio {
        public final BufferedImage imagem;
        public final String texto;

        public ResultadoDesafio(BufferedImage imagem, String texto) {
            this.imagem = imagem;
            this.texto = texto;
        }
    }

    // =========================================================
    // ALGORITMO GENÉRICO: Componente conexo via FloodFill (BFS)
    // =========================================================

    private static class Componente {
        final int rotulo;
        final List<int[]> pixels = new ArrayList<>();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        long somaX = 0, somaY = 0;

        Componente(int rotulo) { this.rotulo = rotulo; }

        void add(int x, int y) {
            pixels.add(new int[]{x, y});
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
            somaX += x;
            somaY += y;
        }

        int area()         { return pixels.size(); }
        int largura()      { return maxX - minX + 1; }
        int altura()       { return maxY - minY + 1; }
        double centroX()   { return pixels.isEmpty() ? 0 : (double) somaX / pixels.size(); }
        double centroY()   { return pixels.isEmpty() ? 0 : (double) somaY / pixels.size(); }
        double aspectRatio() { return altura() == 0 ? 0 : (double) largura() / altura(); }
        double fill()      { return (double) area() / (largura() * altura()); }
        double diagonal()  { return Math.hypot(largura(), altura()); }
    }

    /**
     * Algoritmo genérico: percorre a máscara booleana e agrupa pixels
     * conectados (4-vizinhança) usando FloodFill BFS.
     * Este único método é reutilizado em todos os 5 desafios.
     */
    private static List<Componente> encontrarComponentes(boolean[][] mask, int largura, int altura) {
        int[][] rotulos = new int[altura][largura];
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        List<Componente> resultado = new ArrayList<>();
        int rotulo = 1;

        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                if (!mask[y][x] || rotulos[y][x] != 0) continue;

                Componente comp = new Componente(rotulo);
                Queue<int[]> fila = new ArrayDeque<>();
                fila.add(new int[]{x, y});
                rotulos[y][x] = rotulo;

                while (!fila.isEmpty()) {
                    int[] p = fila.poll();
                    comp.add(p[0], p[1]);
                    for (int[] d : dirs) {
                        int nx = p[0] + d[0], ny = p[1] + d[1];
                        if (nx >= 0 && nx < largura && ny >= 0 && ny < altura
                                && mask[ny][nx] && rotulos[ny][nx] == 0) {
                            rotulos[ny][nx] = rotulo;
                            fila.add(new int[]{nx, ny});
                        }
                    }
                }

                resultado.add(comp);
                rotulo++;
            }
        }
        return resultado;
    }

    // =========================================================
    // PONTO DE ENTRADA
    // =========================================================

    public static ResultadoDesafio processar(BufferedImage img, TipoDesafio tipo) {
        return switch (tipo) {
            case RELOGIO          -> processarRelogio(img);
            case OBJETOS_COLORIDOS -> processarObjetosColoridos(img);
            case LETRAS           -> processarLetras(img);
            case PLACAS           -> processarPlacas(img);
            case BARRAS           -> processarBarras(img);
        };
    }

    // =========================================================
    // UTILITÁRIOS
    // =========================================================

    private static BufferedImage copiar(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        dst.createGraphics().drawImage(src, 0, 0, null);
        return dst;
    }

    /** Cria máscara de pixels escuros (objetos em fundo claro). */
    private static boolean[][] maskEscuro(BufferedImage img, int threshold) {
        int w = img.getWidth(), h = img.getHeight();
        boolean[][] m = new boolean[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                Color c = new Color(img.getRGB(x, y));
                int gray = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
                m[y][x] = gray < threshold;
            }
        return m;
    }

    /** Cria máscara por faixa de matiz (HSB). */
    private static boolean[][] maskHue(BufferedImage img, float hMin, float hMax,
                                       float sMin, float bMin) {
        int w = img.getWidth(), h = img.getHeight();
        boolean[][] m = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = new Color(img.getRGB(x, y));
                float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
                float hue = hsb[0] * 360;
                if (hsb[1] < sMin || hsb[2] < bMin) continue;
                if (hMin <= hMax) m[y][x] = hue >= hMin && hue < hMax;
                else              m[y][x] = hue >= hMin || hue < hMax; // wraps at 360
            }
        }
        return m;
    }

    /** Máscara por saturação: mantém regiões coloridas (cores vivas) e
     *  descarta fundo branco, cinzas claros e textos pretos (saturação ~0). */
    private static boolean[][] maskColorida(BufferedImage img, float sMin, float bMin) {
        int w = img.getWidth(), h = img.getHeight();
        boolean[][] m = new boolean[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                Color c = new Color(img.getRGB(x, y));
                float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
                m[y][x] = hsb[1] >= sMin && hsb[2] >= bMin;
            }
        return m;
    }

    private static void caixaColorida(BufferedImage img, Componente c, Color cor, String label) {
        caixaColoridaRect(img, c.minX, c.minY, c.largura(), c.altura(), cor, label);
    }

    /** Desenha uma caixa rotulada a partir de um retângulo (x,y,largura,altura). */
    private static void caixaColoridaRect(BufferedImage img, int x, int y, int lw, int lh,
                                          Color cor, String label) {
        Graphics2D g = img.createGraphics();
        g.setColor(cor);
        g.setStroke(new BasicStroke(2));
        g.drawRect(x, y, lw - 1, lh - 1);
        g.setColor(Color.WHITE);
        g.fillRect(x, y - 14, label.length() * 7 + 4, 14);
        g.setColor(cor);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
        g.drawString(label, x + 2, y - 2);
        g.dispose();
    }

    // =========================================================
    // DESAFIO A — RELÓGIO ANALÓGICO  (varredura radial dos ponteiros)
    //
    // Por que as tentativas anteriores erravam:
    //  • A varredura ia até ~80–92% do raio, alcançando os NÚMEROS e as
    //    MARCAÇÕES da borda → picos falsos (ex.: detectar um "ponteiro" às
    //    349° onde só existe o "12").
    //  • Medir o comprimento por densidade confundia a CAUDA traseira do
    //    ponteiro (contrapeso) com um segundo ponteiro.
    //
    // Estratégia robusta adotada:
    //  • Centro e raio vêm do maior componente escuro (o aro do relógio).
    //  • Os ponteiros saem do centro; números/marcações ficam a ≳63% do raio.
    //    Por isso a varredura usa apenas o anel radial [R/15 , 0.60R]: contém
    //    os ponteiros inteiros e exclui hub, números, marcações e o aro.
    //  • Para cada ângulo medimos o ALCANCE (pixel escuro mais distante).
    //    O alcance privilegia a ponta real do ponteiro, ignorando a cauda.
    //  • Ponteiro mais comprido = minuteiro; o outro = horário.
    // =========================================================
    private static ResultadoDesafio processarRelogio(BufferedImage img) {
        BufferedImage res = copiar(img);
        int w = img.getWidth(), h = img.getHeight();

        boolean[][] mask = maskEscuro(img, 128);
        List<Componente> comps = encontrarComponentes(mask, w, h);

        // ── Centro e raio: maior componente em extensão espacial = aro externo ─
        Componente aro = null;
        for (Componente c : comps)
            if (aro == null || c.diagonal() > aro.diagonal()) aro = c;

        int cx, cy, raio;
        if (aro != null && aro.diagonal() > Math.min(w, h) * 0.3) {
            cx = (aro.minX + aro.maxX) / 2;
            cy = (aro.minY + aro.maxY) / 2;
            raio = Math.max(aro.largura(), aro.altura()) / 2;
        } else {
            cx = w / 2; cy = h / 2; raio = Math.min(w, h) / 2;
        }

        int Rmin = Math.max(8, raio / 15);   // ignora o hub central
        double Rmax = raio * 0.60;           // pára antes dos números (~63% do raio)

        // ── Varredura radial de alcance (resolução de 0,5°) ────────────────────
        final int PASSOS = 720;
        double[] alcance = new double[PASSOS];
        for (int a = 0; a < PASSOS; a++) {
            double graus = a * 360.0 / PASSOS;
            double dx = Math.sin(Math.toRadians(graus));
            double dy = -Math.cos(Math.toRadians(graus)); // 0° = topo (12h), horário
            int ultimo = 0;
            for (int r = Rmin; r <= Rmax; r++) {
                int px = cx + (int) Math.round(dx * r);
                int py = cy + (int) Math.round(dy * r);
                if (px >= 0 && px < w && py >= 0 && py < h && mask[py][px]) ultimo = r;
            }
            alcance[a] = ultimo;
        }

        // ── Suavização angular (±3 passos = ±1,5°) ─────────────────────────────
        double[] suave = new double[PASSOS];
        for (int a = 0; a < PASSOS; a++) {
            double s = 0;
            for (int k = -3; k <= 3; k++) s += alcance[(a + k + PASSOS) % PASSOS];
            suave[a] = s / 7.0;
        }

        // ── Pico 1 = maior alcance ; Pico 2 = maior alcance ≥ 12° distante ─────
        int p1 = 0;
        for (int a = 1; a < PASSOS; a++) if (suave[a] > suave[p1]) p1 = a;
        int p2 = -1;
        for (int a = 0; a < PASSOS; a++) {
            int d = Math.abs(a - p1); d = Math.min(d, PASSOS - d);
            if (d < 24) continue; // 24 passos = 12°
            if (p2 == -1 || suave[a] > suave[p2]) p2 = a;
        }

        String horario, debug;
        if (p2 >= 0 && suave[p1] > Rmin * 1.2) {
            double g1 = p1 * 360.0 / PASSOS, g2 = p2 * 360.0 / PASSOS;
            double len1 = suave[p1], len2 = suave[p2];

            // Ponteiro mais comprido = minuteiro.
            double angMin = (len1 >= len2) ? g1 : g2;
            double angHor = (len1 >= len2) ? g2 : g1;
            int lenMin = (int) Math.max(len1, len2);
            int lenHor = (int) Math.min(len1, len2);

            int mins  = (int) Math.round(angMin / 6.0) % 60;
            // Hora corrigida pelo avanço do ponteiro das horas: no horário H:mm
            // ele está em H*30 + mm*0,5 graus. Descontar mm*0,5 evita o erro de
            // arredondamento na fronteira (ex.: ponteiro em 7,5h às 7:30).
            int horas = (int) Math.round((angHor - mins * 0.5) / 30.0);
            horas = ((horas % 12) + 12) % 12;
            if (horas == 0) horas = 12;

            horario = String.format("%02d:%02d", horas, mins);

            desenharSeta(res, cx, cy, (int) Math.round(angMin), lenMin, new Color(30, 100, 255), "MIN");
            desenharSeta(res, cx, cy, (int) Math.round(angHor), lenHor, new Color(220, 40, 40),  "HR");

            Graphics2D g = res.createGraphics();
            g.setColor(Color.GREEN);
            g.fillOval(cx - 5, cy - 5, 10, 10);
            g.dispose();

            debug = String.format(
                    "[debug] centro=(%d,%d)  raio=%d  Rmin=%d  Rmax=%d (60%%)%n" +
                    "minuteiro=%.1f° (alcance %.0f)  horário=%.1f° (alcance %.0f)",
                    cx, cy, raio, Rmin, (int) Rmax,
                    angMin, Math.max(len1, len2), angHor, Math.min(len1, len2));
        } else {
            horario = "Ponteiros não detectados";
            debug = String.format("[debug] centro=(%d,%d) raio=%d — sem dois picos válidos", cx, cy, raio);
        }

        return new ResultadoDesafio(res,
                "Horário detectado: " + horario + "\n\n" +
                "Azul  = minuteiro  |  Vermelho = horário\n" +
                "Verde = centro detectado\n\n" + debug);
    }

    /** Desenha uma linha colorida saindo do centro na direção do ângulo. */
    private static void desenharSeta(BufferedImage img, int cx, int cy,
                                     int ang, int comprimento, Color cor, String label) {
        double dx = Math.sin(Math.toRadians(ang));
        double dy = -Math.cos(Math.toRadians(ang));
        int ex = cx + (int)(dx * comprimento);
        int ey = cy + (int)(dy * comprimento);

        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(cor);
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx, cy, ex, ey);
        // Círculo na ponta
        g.fillOval(ex - 5, ey - 5, 10, 10);
        // Rótulo
        g.setColor(Color.WHITE);
        g.fillRect(ex - 2, ey - 16, label.length() * 7 + 4, 13);
        g.setColor(cor);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
        g.drawString(label, ex, ey - 4);
        g.dispose();
    }

    // =========================================================
    // DESAFIO B — OBJETOS COLORIDOS
    // =========================================================
    private static ResultadoDesafio processarObjetosColoridos(BufferedImage img) {
        BufferedImage res = copiar(img);
        int w = img.getWidth(), h = img.getHeight();
        int minArea = Math.max(50, (w * h) / 5000);

        // [nome, hMin, hMax, sMin, bMin, cor-destaque]
        Object[][] cores = {
            {"Vermelho", 345f, 15f,  0.5f, 0.3f, Color.RED},
            {"Laranja",  15f,  45f,  0.5f, 0.4f, Color.ORANGE},
            {"Amarelo",  45f,  75f,  0.4f, 0.5f, Color.YELLOW},
            {"Verde",    90f,  150f, 0.4f, 0.2f, Color.GREEN},
            {"Ciano",   150f,  200f, 0.4f, 0.3f, new Color(0,200,200)},
            {"Azul",    200f,  260f, 0.4f, 0.2f, Color.BLUE},
            {"Roxo",    260f,  345f, 0.4f, 0.2f, new Color(128,0,200)},
        };

        StringBuilder sb = new StringBuilder("Objetos detectados por cor:\n\n");
        sb.append(String.format("%-12s %s%n", "Cor", "Quantidade"));
        sb.append("-".repeat(24)).append("\n");
        int total = 0;

        for (Object[] entrada : cores) {
            String nome = (String) entrada[0];
            float hMin = (float) entrada[1], hMax = (float) entrada[2];
            float sMin = (float) entrada[3], bMin = (float) entrada[4];
            Color destaque = (Color) entrada[5];

            boolean[][] mask = maskHue(img, hMin, hMax, sMin, bMin);
            List<Componente> comps = encontrarComponentes(mask, w, h);

            List<Componente> objetos = new ArrayList<>();
            for (Componente c : comps)
                if (c.area() >= minArea) objetos.add(c);

            if (!objetos.isEmpty()) {
                sb.append(String.format("%-12s %d%n", nome, objetos.size()));
                total += objetos.size();
                for (int i = 0; i < objetos.size(); i++)
                    caixaColorida(res, objetos.get(i), destaque, nome.substring(0, 3) + (i+1));
            }
        }

        sb.append("\nTotal: ").append(total).append(" objeto(s)");
        return new ResultadoDesafio(res, sb.toString());
    }

    // =========================================================
    // DESAFIO C — LETRAS DO ALFABETO
    // =========================================================
    private static ResultadoDesafio processarLetras(BufferedImage img) {
        BufferedImage res = copiar(img);
        int w = img.getWidth(), h = img.getHeight();

        // Tenta binarizar como escuro sobre claro; se poucos componentes, inverte
        boolean[][] mask = maskEscuro(img, 128);
        List<Componente> comps = encontrarComponentes(mask, w, h);

        int minArea = 20, maxArea = (w * h) / 6;
        List<Componente> letrasComp = filtrarLetras(comps, minArea, maxArea);

        // Se invertido (texto claro em fundo escuro), tenta ao contrário
        if (letrasComp.size() < 2) {
            boolean[][] maskInv = new boolean[h][w];
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++)
                    maskInv[y][x] = !mask[y][x];
            comps = encontrarComponentes(maskInv, w, h);
            letrasComp = filtrarLetras(comps, minArea, maxArea);
        }

        // Ordena: esquerda→direita, cima→baixo (leitura natural)
        letrasComp.sort((a, b) -> {
            int linhaA = a.minY / Math.max(1, h / 10);
            int linhaB = b.minY / Math.max(1, h / 10);
            return linhaA != linhaB ? Integer.compare(linhaA, linhaB) : Integer.compare(a.minX, b.minX);
        });

        // Gera templates de referência A-Z (3 fontes) e classifica cada componente
        List<Template> templates = gerarTemplates(48, 64);
        Set<Character> encontradas = new LinkedHashSet<>();
        Color[] palette = {Color.RED, new Color(0,150,0), Color.BLUE, new Color(200,0,200)};

        for (int i = 0; i < letrasComp.size(); i++) {
            Componente c = letrasComp.get(i);
            char letra = classificarPorTemplate(img, c, templates);
            encontradas.add(letra);
            Color cor = palette[i % palette.length];
            caixaColorida(res, c, cor, String.valueOf(letra));
        }

        List<Character> ordenadas = new ArrayList<>(encontradas);
        Collections.sort(ordenadas);

        StringBuilder sb = new StringBuilder();
        sb.append("Componentes encontrados: ").append(letrasComp.size()).append("\n");
        sb.append("Letras únicas detectadas (").append(ordenadas.size()).append("):\n\n");
        for (char ch : ordenadas) sb.append(ch).append(" ");

        return new ResultadoDesafio(res, sb.toString());
    }

    private static List<Componente> filtrarLetras(List<Componente> comps, int minArea, int maxArea) {
        List<Componente> out = new ArrayList<>();
        for (Componente c : comps)
            if (c.area() >= minArea && c.area() <= maxArea && c.largura() >= 3 && c.altura() >= 5)
                out.add(c);
        return out;
    }

    /** Template A–Z: caractere renderizado e normalizado (recorte + escala). */
    private static class Template {
        final char ch;
        final BufferedImage img;
        Template(char ch, BufferedImage img) { this.ch = ch; this.img = img; }
    }

    /**
     * Gera os templates A–Z em TRÊS famílias tipográficas (sem serifa, com
     * serifa e monoespaçada). Comparar contra várias fontes torna o
     * reconhecimento robusto ao tipo de letra usado na imagem — era a causa
     * de B→H e C→L quando só existia a fonte sem serifa.
     */
    private static List<Template> gerarTemplates(int tw, int th) {
        List<Template> lista = new ArrayList<>();
        String[] fontes = { Font.SANS_SERIF, Font.SERIF, Font.MONOSPACED };
        for (String fonte : fontes)
            for (char ch = 'A'; ch <= 'Z'; ch++)
                lista.add(new Template(ch, normalizarGlifo(renderizarGlifo(ch, fonte), tw, th)));
        return lista;
    }

    /** Desenha um caractere grande (preto sobre branco). */
    private static BufferedImage renderizarGlifo(char ch, String fonte) {
        BufferedImage t = new BufferedImage(200, 200, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = t.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 200);
        g.setColor(Color.BLACK);
        g.setFont(new Font(fonte, Font.BOLD, 120));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(String.valueOf(ch),
                (200 - fm.charWidth(ch)) / 2,
                (200 + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
        return t;
    }

    /**
     * Recorta a tinta (pixels escuros) ao seu bounding-box e redimensiona para
     * tw×th. Aplicado IGUALMENTE a templates e às letras da imagem, garante que
     * ambos fiquem centrados e na mesma escala antes da comparação.
     */
    private static BufferedImage normalizarGlifo(BufferedImage src, int tw, int th) {
        int w = src.getWidth(), h = src.getHeight();
        int minx = w, miny = h, maxx = -1, maxy = -1;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (new Color(src.getRGB(x, y)).getRed() < 128) {
                    if (x < minx) minx = x; if (x > maxx) maxx = x;
                    if (y < miny) miny = y; if (y > maxy) maxy = y;
                }
        if (maxx < 0) { minx = 0; miny = 0; maxx = w - 1; maxy = h - 1; }
        BufferedImage crop = src.getSubimage(minx, miny, maxx - minx + 1, maxy - miny + 1);

        BufferedImage out = new BufferedImage(tw, th, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, tw, th);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(crop, 0, 0, tw, th, null);
        g.dispose();
        return out;
    }

    /** Classifica o componente pelo template (SSD em tons de cinza) mais próximo. */
    private static char classificarPorTemplate(BufferedImage img, Componente c, List<Template> templates) {
        int tw = templates.get(0).img.getWidth();
        int th = templates.get(0).img.getHeight();

        BufferedImage sub = img.getSubimage(c.minX, c.minY, c.largura(), c.altura());
        BufferedImage cinza = new BufferedImage(sub.getWidth(), sub.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D gg = cinza.createGraphics();
        gg.drawImage(sub, 0, 0, null);
        gg.dispose();
        BufferedImage patch = normalizarGlifo(cinza, tw, th);

        char melhor = '?';
        double menorErro = Double.MAX_VALUE;
        for (Template t : templates) {
            double erro = 0;
            for (int y = 0; y < th; y++)
                for (int x = 0; x < tw; x++) {
                    int p1 = new Color(patch.getRGB(x, y)).getRed();
                    int p2 = new Color(t.img.getRGB(x, y)).getRed();
                    erro += (p1 - p2) * (p1 - p2);
                }
            if (erro < menorErro) { menorErro = erro; melhor = t.ch; }
        }
        return melhor;
    }

    // =========================================================
    // DESAFIO D — PLACAS DE TRÂNSITO
    // =========================================================
    private static ResultadoDesafio processarPlacas(BufferedImage img) {
        BufferedImage res = copiar(img);
        int w = img.getWidth(), h = img.getHeight();
        int minArea = Math.max(100, (w * h) / 1000);

        StringBuilder sb = new StringBuilder("Placas identificadas:\n\n");
        int total = 0;

        // Vermelho: PARE / proibição
        total += detectarPlacas(img, res, sb,
                maskHue(img, 345f, 15f, 0.4f, 0.2f),
                w, h, minArea, "vermelho");
        // Amarelo/laranja: advertência
        total += detectarPlacas(img, res, sb,
                maskHue(img, 30f, 75f, 0.5f, 0.4f),
                w, h, minArea, "amarelo");
        // Azul: informativo
        total += detectarPlacas(img, res, sb,
                maskHue(img, 200f, 240f, 0.4f, 0.2f),
                w, h, minArea, "azul");
        // Verde: permissão/direção
        total += detectarPlacas(img, res, sb,
                maskHue(img, 100f, 160f, 0.35f, 0.2f),
                w, h, minArea, "verde");

        if (total == 0) sb.append("Nenhuma placa detectada.");
        else sb.append("\nTotal: ").append(total).append(" placa(s).");

        return new ResultadoDesafio(res, sb.toString());
    }

    private static int detectarPlacas(BufferedImage img, BufferedImage res, StringBuilder sb,
                                      boolean[][] mask, int w, int h, int minArea, String cor) {
        Color destaque = switch (cor) {
            case "vermelho" -> Color.RED;
            case "amarelo"  -> Color.ORANGE;
            case "azul"     -> Color.BLUE;
            default         -> new Color(0, 160, 0);
        };

        // Componentes relevantes da cor.
        List<int[]> caixas = new ArrayList<>();
        for (Componente c : encontrarComponentes(mask, w, h))
            if (c.area() >= minArea)
                caixas.add(new int[]{ c.minX, c.minY, c.maxX, c.maxY, c.area() });

        // Uma mesma placa pode aparecer fracionada: por exemplo, no PARE a faixa
        // branca da borda separa o anel externo do miolo do octógono. Mesclamos
        // caixas sobrepostas para tratá-las como UMA placa só.
        List<int[]> placas = mesclarCaixas(caixas);

        int count = 0;
        for (int[] g : placas) {
            int lw = g[2] - g[0] + 1, lh = g[3] - g[1] + 1;
            double ar = (double) lw / lh;
            double fill = (double) g[4] / (lw * lh);
            String tipo = tipoDePlaca(ar, fill, cor);
            sb.append(String.format("  [%-22s] %-7s em (%d,%d)%n", tipo, "(" + cor + ")", g[0], g[1]));
            caixaColoridaRect(res, g[0], g[1], lw, lh, destaque, tipo);
            count++;
        }
        return count;
    }

    /** Mescla caixas (minX,minY,maxX,maxY,area) que se sobrepõem; a área do
     *  grupo é a soma das áreas, para estimar o preenchimento real da placa. */
    private static List<int[]> mesclarCaixas(List<int[]> caixas) {
        List<int[]> grupos = new ArrayList<>();
        for (int[] c : caixas) {
            int[] box = c.clone();
            boolean mesclou = true;
            while (mesclou) {
                mesclou = false;
                Iterator<int[]> it = grupos.iterator();
                while (it.hasNext()) {
                    int[] g = it.next();
                    if (caixasSobrepoem(box, g)) {
                        box[0] = Math.min(box[0], g[0]);
                        box[1] = Math.min(box[1], g[1]);
                        box[2] = Math.max(box[2], g[2]);
                        box[3] = Math.max(box[3], g[3]);
                        box[4] = box[4] + g[4];
                        it.remove();
                        mesclou = true;
                    }
                }
            }
            grupos.add(box);
        }
        return grupos;
    }

    private static boolean caixasSobrepoem(int[] a, int[] b) {
        int t = 4; // tolerância em pixels
        return a[0] <= b[2] + t && b[0] <= a[2] + t && a[1] <= b[3] + t && b[1] <= a[3] + t;
    }

    private static String tipoDePlaca(double ar, double fill, String cor) {
        return switch (cor) {
            case "vermelho" -> {
                boolean quadrada = ar > 0.8 && ar < 1.25;
                // Octógono PARE é sólido (preenchimento alto); círculos de
                // proibição/regulamentação são anéis vazados (preenchimento baixo).
                if (quadrada && fill > 0.55) yield "PARE";
                if (quadrada)                yield "PROIBIÇÃO";
                yield "REGULAMENTAÇÃO";
            }
            case "amarelo"  -> {
                if (ar > 0.8 && ar < 1.2 && fill < 0.60) yield "ADVERTÊNCIA";
                if (ar > 1.5)                            yield "ADVERTÊNCIA (FAIXA)";
                yield "ADVERTÊNCIA";
            }
            case "azul"     -> {
                if (ar > 1.3)  yield "INFORMATIVO (HORIZ.)";
                if (ar < 0.75) yield "INFORMATIVO (VERT.)";
                yield "INFORMATIVO";
            }
            default         -> {
                if (ar > 1.2) yield "INDICAÇÃO (HORIZ.)";
                yield "INDICAÇÃO";
            }
        };
    }

    // =========================================================
    // DESAFIO E — BARRAS VERTICAIS
    // =========================================================
    private static ResultadoDesafio processarBarras(BufferedImage img) {
        BufferedImage res = copiar(img);
        int w = img.getWidth(), h = img.getHeight();
        int minArea = Math.max(150, (w * h) / 2000);

        // Barras coloridas sobre fundo claro: a máscara por saturação mantém as
        // barras (cores vivas, ex.: salmão) e descarta o fundo branco, as linhas
        // de grade cinza-claro e os rótulos pretos dos eixos (saturação ~0).
        // (maskEscuro falhava aqui: a cor das barras é clara e fragmentava.)
        List<Componente> barras = filtrarBarras(
                encontrarComponentes(maskColorida(img, 0.20f, 0.30f), w, h), minArea);

        // Fallback: barras escuras/cinza (sem cor saturada) sobre fundo claro.
        if (barras.isEmpty())
            barras = filtrarBarras(
                    encontrarComponentes(maskEscuro(img, 150), w, h), minArea);

        if (barras.isEmpty())
            return new ResultadoDesafio(res, "Nenhuma barra vertical detectada.");

        // Ordena por posição X (esquerda para direita)
        barras.sort(Comparator.comparingInt(c -> c.minX));

        // Identifica mais alta e mais baixa
        Componente maisAlta  = barras.stream().max(Comparator.comparingInt(Componente::altura)).orElse(barras.get(0));
        Componente maisBaixa = barras.stream().min(Comparator.comparingInt(Componente::altura)).orElse(barras.get(0));

        // Anota resultado na imagem
        for (int i = 0; i < barras.size(); i++) {
            Componente c = barras.get(i);
            Color cor = (c == maisAlta) ? Color.RED : (c == maisBaixa) ? Color.BLUE : new Color(0, 160, 0);
            caixaColorida(res, c, cor, "B" + (i + 1));
            // Rótulo de altura abaixo da barra
            Graphics2D g = res.createGraphics();
            g.setColor(cor);
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            g.drawString(c.altura() + "px", c.minX, Math.min(h - 2, c.maxY + 12));
            g.dispose();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-8s %-8s %-8s%n", "Barra", "Altura", "Largura"));
        sb.append("-".repeat(26)).append("\n");
        for (int i = 0; i < barras.size(); i++) {
            Componente c = barras.get(i);
            String mark = (c == maisAlta) ? " ▲MAX" : (c == maisBaixa) ? " ▼MIN" : "";
            sb.append(String.format("B%-7d %-8d %-8d%s%n", i+1, c.altura(), c.largura(), mark));
        }
        sb.append("\nMAIS ALTA  (▲ vermelho): B").append(barras.indexOf(maisAlta)  + 1)
          .append(" — ").append(maisAlta.altura()).append("px");
        sb.append("\nMAIS BAIXA (▼ azul):     B").append(barras.indexOf(maisBaixa) + 1)
          .append(" — ").append(maisBaixa.altura()).append("px");

        return new ResultadoDesafio(res, sb.toString());
    }

    private static List<Componente> filtrarBarras(List<Componente> comps, int minArea) {
        List<Componente> out = new ArrayList<>();
        for (Componente c : comps)
            if (c.area() >= minArea && c.altura() >= c.largura() * 0.8)
                out.add(c);
        return out;
    }
}
