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

    private static void caixaColorida(BufferedImage img, Componente c, Color cor, String label) {
        Graphics2D g = img.createGraphics();
        g.setColor(cor);
        g.setStroke(new BasicStroke(2));
        g.drawRect(c.minX, c.minY, c.largura() - 1, c.altura() - 1);
        g.setColor(Color.WHITE);
        g.fillRect(c.minX, c.minY - 14, label.length() * 7 + 4, 14);
        g.setColor(cor);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
        g.drawString(label, c.minX + 2, c.minY - 2);
        g.dispose();
    }

    // =========================================================
    // DESAFIO A — RELÓGIO ANALÓGICO  (varredura radial)
    //
    // Por que a abordagem por componentes conexos falhava:
    //  1. Os dois ponteiros se UNEM no pino central → FloodFill os
    //     agrupa num único componente; o segundo candidato acaba sendo
    //     um número ou marcação da borda.
    //  2. A diagonal do bounding-box é ruim para distinguir hora/minuto
    //     quando os ponteiros formam um único bloco.
    //  3. pixelMaisLonge() pode retornar a cauda da seta (ponta traseira)
    //     em vez da ponta frontal, invertendo o ângulo 180°.
    //
    // Solução — varredura radial:
    //  Para cada ângulo θ ∈ [0°,360°), conta quantos pixels escuros
    //  existem ao longo da linha (centro → θ) dentro de [Rmin, Rmax].
    //  • Rmax = 65% do semi-eixo menor  →  evita números e marcações
    //    da borda, que ficam nos 80–95% externos do raio.
    //  • Os dois ângulos de pico = direções dos ponteiros.
    //  • O pico de maior comprimento (via comprimentoRadial) = minuteiro.
    // =========================================================
    private static ResultadoDesafio processarRelogio(BufferedImage img) {
        BufferedImage res = copiar(img);
        int w = img.getWidth(), h = img.getHeight();

        // Threshold 128 para detectar a borda do relógio (inclusive bordas finas).
        // A varredura radial usa mask90 (threshold 90) para contar apenas pixels
        // verdadeiramente escuros (ponteiros), evitando ruído de sombras/números.
        boolean[][] mask128 = maskEscuro(img, 128);
        boolean[][] mask = maskEscuro(img, 90);
        List<Componente> comps = encontrarComponentes(mask128, w, h);

        // ── Passo 0: Detectar o centro do relógio ────────────────────────────
        // Usa diagonal() — não area() — para encontrar o anel externo.
        // Com area(), uma borda fina perde para os ponteiros espessos.
        // Com diagonal(), o componente de maior extensão espacial é sempre o anel.
        Componente maiorComp = null;
        for (Componente c : comps)
            if (maiorComp == null || c.diagonal() > maiorComp.diagonal()) maiorComp = c;

        int cx, cy, clockRadius;
        if (maiorComp != null && maiorComp.diagonal() > Math.min(w, h) * 0.3) {
            cx = (maiorComp.minX + maiorComp.maxX) / 2;
            cy = (maiorComp.minY + maiorComp.maxY) / 2;
            clockRadius = Math.max(maiorComp.largura(), maiorComp.altura()) / 2;
        } else {
            cx = w / 2;
            cy = h / 2;
            clockRadius = Math.min(w, h) / 2;
        }

        // ── Passo 1: Varredura radial de densidade ───────────────────────────
        // Rmax = 78%: cobre ponteiros (horário ~65-70%, minuteiro ~80-85%)
        // sem atingir dígitos (~80-88%) nem marcações (~90-95%).
        // RmaxLong = 92%: mede o comprimento real de cada ponteiro.
        int Rmin = Math.max(8, clockRadius / 10);
        int Rmax = (int)(clockRadius * 0.78);
        int RmaxLong = (int)(clockRadius * 0.92);

        // Resolução de 0,5° (720 passos) para melhor precisão angular.
        final int PASSOS = 720;
        double[] densidade = new double[PASSOS];
        for (int ang = 0; ang < PASSOS; ang++) {
            double graus = ang * 360.0 / PASSOS;
            double dx = Math.sin(Math.toRadians(graus));
            double dy = -Math.cos(Math.toRadians(graus)); // 0° = topo (12h), horário
            int count = 0;
            for (int r = Rmin; r <= Rmax; r++) {
                int px = cx + (int) Math.round(dx * r);
                int py = cy + (int) Math.round(dy * r);
                if (px >= 0 && px < w && py >= 0 && py < h && mask[py][px])
                    count++;
            }
            densidade[ang] = count;
        }

        // ── Passo 2: Suavização angular (janela ±8 passos = ±4°) ─────────────
        double[] smooth = new double[PASSOS];
        for (int a = 0; a < PASSOS; a++) {
            double s = 0;
            for (int k = -8; k <= 8; k++) s += densidade[(a + k + PASSOS) % PASSOS];
            smooth[a] = s / 17.0;
        }

        // ── Passo 3: Pico 1 = maior densidade ────────────────────────────────
        int angPico1 = 0;
        for (int a = 1; a < PASSOS; a++)
            if (smooth[a] > smooth[angPico1]) angPico1 = a;

        // ── Passo 4: Pico 2 = maior densidade afastado ≥ 20° do pico 1 ──────
        // 20° = 40 passos com resolução de 0,5°
        int angPico2 = -1;
        for (int a = 0; a < PASSOS; a++) {
            int diff = Math.abs(a - angPico1);
            diff = Math.min(diff, PASSOS - diff);
            if (diff < 40) continue;
            if (angPico2 == -1 || smooth[a] > smooth[angPico2]) angPico2 = a;
        }

        double limiar = (Rmax - Rmin) * 0.08;

        // Converte índices de passos para graus reais
        double grausPico1 = angPico1 * 360.0 / PASSOS;
        double grausPico2 = (angPico2 >= 0) ? angPico2 * 360.0 / PASSOS : -1;

        String horario;
        String debug = String.format(
                "[debug] centro=(%d,%d)  raio=%d  Rmax=%d(78%%)  RmaxLong=%d(92%%)%n" +
                "pico1=%.1f° (%.1f)  pico2=%.1f° (%.1f)  limiar=%.1f",
                cx, cy, clockRadius, Rmax, RmaxLong,
                grausPico1, smooth[angPico1],
                grausPico2, angPico2 >= 0 ? smooth[angPico2] : 0.0,
                limiar);

        if (angPico2 >= 0 && smooth[angPico1] > limiar) {

            int comp1 = comprimentoRadial(mask, cx, cy, (int)Math.round(grausPico1), Rmin, RmaxLong, w, h);
            int comp2 = comprimentoRadial(mask, cx, cy, (int)Math.round(grausPico2), Rmin, RmaxLong, w, h);

            // Ponteiro mais longo = minuteiro
            double angMinGraus = (comp1 >= comp2) ? grausPico1 : grausPico2;
            double angHorGraus = (comp1 >= comp2) ? grausPico2 : grausPico1;
            int lenMin = Math.max(comp1, comp2);
            int lenHor = Math.min(comp1, comp2);

            int mins  = (int) Math.round(angMinGraus / 6.0)  % 60;
            int horas = (int) Math.round(angHorGraus / 30.0) % 12;
            if (horas == 0) horas = 12;

            horario = String.format("%02d:%02d", horas, mins);

            desenharSeta(res, cx, cy, (int)Math.round(angMinGraus), lenMin, new Color(30, 100, 255), "MIN");
            desenharSeta(res, cx, cy, (int)Math.round(angHorGraus), lenHor, new Color(220, 40, 40),  "HR");

            Graphics2D g = res.createGraphics();
            g.setColor(Color.GREEN);
            g.fillOval(cx - 5, cy - 5, 10, 10);
            g.dispose();

        } else {
            horario = "Ponteiros não detectados";
        }

        return new ResultadoDesafio(res,
                "Horário detectado: " + horario + "\n\n" +
                "Azul  = minuteiro  |  Vermelho = horário\n" +
                "Verde = centro detectado\n\n" + debug);
    }

    /**
     * Retorna a distância (em pixels) do pixel escuro mais distante do centro
     * ao longo do ângulo dado, dentro de [Rmin, Rmax].
     */
    private static int comprimentoRadial(boolean[][] mask, int cx, int cy,
                                         int ang, int Rmin, int Rmax, int w, int h) {
        double dx = Math.sin(Math.toRadians(ang));
        double dy = -Math.cos(Math.toRadians(ang));
        int ultimo = Rmin;
        for (int r = Rmin; r <= Rmax; r++) {
            int px = cx + (int) Math.round(dx * r);
            int py = cy + (int) Math.round(dy * r);
            if (px >= 0 && px < w && py >= 0 && py < h && mask[py][px])
                ultimo = r;
        }
        return ultimo;
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

        // Gera templates de referência A-Z e classifica cada componente
        Map<Character, BufferedImage> templates = gerarTemplates(30, 40);
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

    /** Renderiza cada letra A-Z com AWT para usar como template de comparação. */
    private static Map<Character, BufferedImage> gerarTemplates(int tw, int th) {
        Map<Character, BufferedImage> map = new LinkedHashMap<>();
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, th - 4);
        for (char ch = 'A'; ch <= 'Z'; ch++) {
            BufferedImage t = new BufferedImage(tw, th, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g = t.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, tw, th);
            g.setColor(Color.BLACK);
            g.setFont(font);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            FontMetrics fm = g.getFontMetrics();
            int tx = (tw - fm.charWidth(ch)) / 2;
            int ty = (th + fm.getAscent() - fm.getDescent()) / 2;
            g.drawString(String.valueOf(ch), tx, ty);
            g.dispose();
            map.put(ch, t);
        }
        return map;
    }

    private static char classificarPorTemplate(BufferedImage img, Componente c,
                                               Map<Character, BufferedImage> templates) {
        // Extrai sub-imagem do componente e redimensiona para o tamanho do template
        int tw = templates.values().iterator().next().getWidth();
        int th = templates.values().iterator().next().getHeight();

        BufferedImage patch = new BufferedImage(tw, th, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = patch.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img.getSubimage(c.minX, c.minY, c.largura(), c.altura()), 0, 0, tw, th, null);
        g.dispose();

        char melhor = '?';
        double menorErro = Double.MAX_VALUE;

        for (Map.Entry<Character, BufferedImage> entry : templates.entrySet()) {
            BufferedImage tmpl = entry.getValue();
            double erro = 0;
            for (int y = 0; y < th; y++) {
                for (int x = 0; x < tw; x++) {
                    int p1 = new Color(patch.getRGB(x, y)).getRed();
                    int p2 = new Color(tmpl.getRGB(x, y)).getRed();
                    erro += (p1 - p2) * (p1 - p2);
                }
            }
            if (erro < menorErro) { menorErro = erro; melhor = entry.getKey(); }
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
        Color[] cores = new Color[]{Color.RED, Color.ORANGE, Color.BLUE, Color.GREEN};
        Color destaque = switch (cor) {
            case "vermelho" -> Color.RED;
            case "amarelo"  -> Color.ORANGE;
            case "azul"     -> Color.BLUE;
            default         -> new Color(0, 160, 0);
        };

        List<Componente> comps = encontrarComponentes(mask, w, h);
        int count = 0;
        for (Componente c : comps) {
            if (c.area() < minArea) continue;
            String tipo = tipoDePlaca(c, cor);
            sb.append(String.format("  [%-22s] %-7s em (%d,%d)%n", tipo, "("+cor+")", c.minX, c.minY));
            caixaColorida(res, c, destaque, tipo);
            count++;
        }
        return count;
    }

    private static String tipoDePlaca(Componente c, String cor) {
        double ar   = c.aspectRatio();
        double fill = c.fill();

        return switch (cor) {
            case "vermelho" -> {
                // Octógono (PARE): fill ≈ 0.83; Círculo (proibição): fill ≈ 0.79
                if (ar > 0.8 && ar < 1.25 && fill > 0.75) yield "PARE";
                if (ar > 0.8 && ar < 1.25)                yield "PROIBIÇÃO";
                yield "REGULAMENTAÇÃO";
            }
            case "amarelo"  -> {
                // Triângulo: fill ≈ 0.50; Losango: fill ≈ 0.50
                if (ar > 0.8 && ar < 1.2 && fill < 0.60)  yield "ADVERTÊNCIA";
                if (ar > 1.5)                              yield "ADVERTÊNCIA (FAIXA)";
                yield "ADVERTÊNCIA";
            }
            case "azul"     -> {
                if (ar > 1.3) yield "INFORMATIVO (HORIZ.)";
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

        // Tenta com threshold alto (barras coloridas em fundo branco)
        boolean[][] mask = maskEscuro(img, 200);
        List<Componente> comps = encontrarComponentes(mask, w, h);

        int minArea = Math.max(30, (w * h) / 2000);
        List<Componente> barras = new ArrayList<>();
        for (Componente c : comps)
            if (c.area() >= minArea && c.altura() > c.largura() * 1.5)
                barras.add(c);

        // Se não achou, tenta com threshold mais baixo (barras escuras)
        if (barras.size() < 2) {
            mask = maskEscuro(img, 128);
            comps = encontrarComponentes(mask, w, h);
            barras.clear();
            for (Componente c : comps)
                if (c.area() >= minArea && c.altura() > c.largura() * 1.5)
                    barras.add(c);
        }

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
            Color cor = (c == maisAlta) ? Color.RED : (c == maisBaixa) ? Color.BLUE : Color.GREEN;
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
}
