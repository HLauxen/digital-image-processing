package view;

import controller.DesafioController;
import controller.ImageController;
import controller.ImageController.Operacao;
import controller.ImageController.TipoElemento;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class MainFrame extends JFrame {

    // --- Paleta ---
    private static final Color C_PRIMARY   = new Color(108, 99, 255);
    private static final Color C_ACCENT    = new Color(255, 107, 157);
    private static final Color C_BG        = new Color(242, 243, 250);
    private static final Color C_SURFACE   = new Color(255, 255, 255);
    private static final Color C_BORDER    = new Color(218, 218, 232);
    private static final Color C_TEXT      = new Color(28, 28, 45);
    private static final Color C_MUTED     = new Color(140, 140, 168);
    private static final Color C_DARK_BG   = new Color(22, 22, 38);
    private static final Color C_DARK_TEXT = new Color(210, 212, 230);

    private final ImagePanel originalPanel;
    private ImagePanel transformedPanel;
    private final ImageController controller;

    private CardLayout cardLayout;
    private JPanel mainCard;
    private ImagePanel desafioImagePanel;
    private JTextArea desafioTextArea;
    private JLabel statusLabel;

    public MainFrame() {
        setTitle("PDI — Processamento Digital de Imagens");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 760);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_BG);

        controller = new ImageController(this);

        originalPanel   = new ImagePanel();
        transformedPanel = new ImagePanel();

        // --- Layout principal ---
        setLayout(new BorderLayout());
        add(criarHeaderPanel(), BorderLayout.NORTH);
        add(criarConteudo(),    BorderLayout.CENTER);
        add(criarStatusBar(),  BorderLayout.SOUTH);

        setJMenuBar(createMenuBar());
        setVisible(true);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Header
    // ═══════════════════════════════════════════════════════════════

    private JPanel criarHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, C_PRIMARY,
                        getWidth(), 0, C_ACCENT);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setPreferredSize(new Dimension(0, 58));
        header.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));
        header.setOpaque(false);

        JLabel title = new JLabel("PDI — Processamento Digital de Imagens");
        title.setFont(new Font("SansSerif", Font.BOLD, 17));
        title.setForeground(Color.WHITE);

        JLabel author = new JLabel("Henrique Lauxen Seefeld");
        author.setFont(new Font("SansSerif", Font.PLAIN, 13));
        author.setForeground(new Color(255, 255, 255, 190));

        header.add(title,  BorderLayout.WEST);
        header.add(author, BorderLayout.EAST);
        return header;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Conteúdo central (CardLayout)
    // ═══════════════════════════════════════════════════════════════

    private JPanel criarConteudo() {
        cardLayout = new CardLayout();
        mainCard   = new JPanel(cardLayout);
        mainCard.setBackground(C_BG);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                criarImageCard(originalPanel,   "ORIGINAL",  C_PRIMARY),
                criarImageCard(transformedPanel, "RESULTADO", C_ACCENT)
        );
        splitPane.setDividerLocation(630);
        splitPane.setDividerSize(6);
        splitPane.setBackground(C_BG);
        splitPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));

        mainCard.add(splitPane,            "NORMAL");
        mainCard.add(criarPainelDesafio(), "DESAFIO");

        return mainCard;
    }

    private JPanel criarImageCard(ImagePanel panel, String titulo, Color cor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(C_BG);

        // Rótulo colorido no topo
        JLabel label = new JLabel(titulo);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setForeground(cor);
        label.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        label.setOpaque(true);
        label.setBackground(C_SURFACE);

        // Separador colorido abaixo do rótulo
        JPanel labelBar = new JPanel(new BorderLayout());
        labelBar.setBackground(C_SURFACE);
        labelBar.add(label, BorderLayout.CENTER);
        JPanel colorLine = new JPanel();
        colorLine.setPreferredSize(new Dimension(0, 3));
        colorLine.setBackground(cor);
        labelBar.add(colorLine, BorderLayout.SOUTH);

        // Container da imagem com borda e sombra suave
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(C_SURFACE);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 12, 0),
                new ShadowBorder(C_BORDER)
        ));
        wrapper.add(labelBar, BorderLayout.NORTH);

        panel.setBackground(new Color(230, 230, 240));
        wrapper.add(panel, BorderLayout.CENTER);

        card.add(wrapper, BorderLayout.CENTER);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Painel Desafio
    // ═══════════════════════════════════════════════════════════════

    private JPanel criarPainelDesafio() {
        JPanel painel = new JPanel(new BorderLayout(0, 0));
        painel.setBackground(C_BG);

        JButton btnVoltar = criarBotaoPilula("← Voltar ao modo normal", C_PRIMARY);
        btnVoltar.addActionListener(e -> cardLayout.show(mainCard, "NORMAL"));

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        topo.setBackground(C_BG);
        topo.add(btnVoltar);

        desafioImagePanel = new ImagePanel();
        desafioImagePanel.setBackground(new Color(230, 230, 240));

        desafioTextArea = new JTextArea(6, 0);
        desafioTextArea.setEditable(false);
        desafioTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        desafioTextArea.setBackground(C_DARK_BG);
        desafioTextArea.setForeground(C_DARK_TEXT);
        desafioTextArea.setCaretColor(C_DARK_TEXT);
        desafioTextArea.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JScrollPane scroll = new JScrollPane(desafioTextArea);
        scroll.setPreferredSize(new Dimension(0, 170));
        scroll.setBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, C_PRIMARY));
        scroll.getViewport().setBackground(C_DARK_BG);

        JPanel imgWrapper = new JPanel(new BorderLayout());
        imgWrapper.setBackground(C_BG);
        imgWrapper.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        imgWrapper.add(desafioImagePanel, BorderLayout.CENTER);

        painel.add(topo,       BorderLayout.NORTH);
        painel.add(imgWrapper, BorderLayout.CENTER);
        painel.add(scroll,     BorderLayout.SOUTH);
        return painel;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Status bar
    // ═══════════════════════════════════════════════════════════════

    private JPanel criarStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(C_SURFACE);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));
        bar.setPreferredSize(new Dimension(0, 26));

        statusLabel = new JLabel("  Pronto");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusLabel.setForeground(C_MUTED);
        bar.add(statusLabel, BorderLayout.WEST);

        JLabel version = new JLabel("PDI 2025  ");
        version.setFont(new Font("SansSerif", Font.PLAIN, 11));
        version.setForeground(C_MUTED);
        bar.add(version, BorderLayout.EAST);

        return bar;
    }

    private void setStatus(String msg) {
        if (statusLabel != null)
            SwingUtilities.invokeLater(() -> statusLabel.setText("  " + msg));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Helpers de estilo
    // ═══════════════════════════════════════════════════════════════

    private JButton criarBotaoPilula(String texto, Color cor) {
        JButton btn = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(cor.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(cor.brighter());
                } else {
                    g2.setColor(cor);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(7, 18, 7, 18));
        return btn;
    }

    private JMenuItem createMenuItem(String title, String accelerator, ActionListener action) {
        JMenuItem item = new JMenuItem(title);
        item.setFont(new Font("SansSerif", Font.PLAIN, 13));
        item.addActionListener(action);
        if (accelerator != null) {
            item.setAccelerator(KeyStroke.getKeyStroke(accelerator));
        }
        return item;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Lógica de desafio
    // ═══════════════════════════════════════════════════════════════

    private void executarDesafio(DesafioController.TipoDesafio tipo) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Selecione uma imagem para o desafio");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        BufferedImage img;
        try {
            img = ImageIO.read(fc.getSelectedFile());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao abrir imagem.");
            return;
        }

        cardLayout.show(mainCard, "DESAFIO");
        desafioImagePanel.setImage(img);
        desafioTextArea.setText("Processando...");
        setStatus("Executando desafio...");
        repaint();

        SwingWorker<DesafioController.ResultadoDesafio, Void> worker = new SwingWorker<>() {
            @Override
            protected DesafioController.ResultadoDesafio doInBackground() {
                return DesafioController.processar(img, tipo);
            }
            @Override
            protected void done() {
                try {
                    DesafioController.ResultadoDesafio r = get();
                    desafioImagePanel.setImage(r.imagem);
                    desafioTextArea.setText(r.texto);
                    setStatus("Desafio concluído.");
                } catch (Exception ex) {
                    desafioTextArea.setText("Erro ao processar: " + ex.getMessage());
                    setStatus("Erro no processamento.");
                }
            }
        };
        worker.execute();
    }

    // ═══════════════════════════════════════════════════════════════
    //  Dialogs de operação
    // ═══════════════════════════════════════════════════════════════

    private void showTranslationDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        JSpinner dxSpinner = new JSpinner(new SpinnerNumberModel(0, -2000, 2000, 10));
        JSpinner dySpinner = new JSpinner(new SpinnerNumberModel(0, -2000, 2000, 10));
        panel.add(new JLabel("Deslocamento X:")); panel.add(dxSpinner);
        panel.add(new JLabel("Deslocamento Y:")); panel.add(dySpinner);
        if (JOptionPane.showConfirmDialog(this, panel, "Translação", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            transformedPanel.setImage(ImageController.transladar((int) dxSpinner.getValue(), (int) dySpinner.getValue()));
            setStatus("Translação aplicada.");
        }
    }

    private void showRotationDialog() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, -2000, 2000, 10));
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
        panel.add(new JLabel("Rotacionar X graus:")); panel.add(spinner);
        if (JOptionPane.showConfirmDialog(this, panel, "Rotação", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            transformedPanel.setImage(ImageController.rotacionar((int) spinner.getValue()));
            setStatus("Rotação aplicada.");
        }
    }

    private void showMirrorDialog() {
        String[] options = {"Horizontal", "Vertical"};
        int result = JOptionPane.showOptionDialog(this, "Escolha o tipo de espelhamento:", "Espelhar",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (result == 0) { transformedPanel.setImage(ImageController.espelharHorizontal()); setStatus("Espelhamento horizontal."); }
        else if (result == 1) { transformedPanel.setImage(ImageController.espelharVertical()); setStatus("Espelhamento vertical."); }
    }

    private void showIncreaseScaleDialog() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(2, 2, 10, 1));
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
        panel.add(new JLabel("Fator de aumento:")); panel.add(spinner);
        if (JOptionPane.showConfirmDialog(this, panel, "Aumentar Escala", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            int s = (int) spinner.getValue();
            transformedPanel.setImage(ImageController.escalar(s, s));
            setStatus("Escala aumentada ×" + s + ".");
        }
    }

    private void showDecreaseScaleDialog() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(2, 2, 10, 1));
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
        panel.add(new JLabel("Divisor da escala:")); panel.add(spinner);
        if (JOptionPane.showConfirmDialog(this, panel, "Diminuir Escala", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            int d = (int) spinner.getValue();
            transformedPanel.setImage(ImageController.escalar(1.0 / d, 1.0 / d));
            setStatus("Escala reduzida ÷" + d + ".");
        }
    }

    private void showGrayScaleDialog() {
        transformedPanel.setImage(ImageController.grayscale());
        setStatus("Grayscale aplicado.");
    }

    private void showShineDialog() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, -255, 255, 10));
        if (JOptionPane.showConfirmDialog(this, spinner, "Ajustar Brilho", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            transformedPanel.setImage(ImageController.ajustarBrilho((int) spinner.getValue()));
            setStatus("Brilho ajustado.");
        }
    }

    private void showContrastDialog() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 5.0, 0.1));
        if (JOptionPane.showConfirmDialog(this, spinner, "Ajustar Contraste", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            transformedPanel.setImage(ImageController.ajustarContraste((double) spinner.getValue()));
            setStatus("Contraste ajustado.");
        }
    }

    private void showGaussianDialog() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(3, 3, 15, 2));
        if (JOptionPane.showConfirmDialog(this, spinner, "Filtro Gaussiano (Tamanho do Kernel)", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            transformedPanel.setImage(ImageController.gaussianBlur((int) spinner.getValue()));
            setStatus("Filtro Gaussiano aplicado.");
        }
    }

    private void showThresholdDialog() {
        transformedPanel.setImage(ImageController.otsu());
        setStatus("Threshold Otsu aplicado.");
    }

    private void showRobertsDialog() {
        transformedPanel.setImage(ImageController.roberts());
        setStatus("Detecção de bordas Roberts.");
    }

    private void showMarrDialog() {
        transformedPanel.setImage(ImageController.marrHildreth());
        setStatus("Detecção de bordas Marr-Hildreth.");
    }

    private void showDilationDialog() {
        transformedPanel.setImage(ImageController.processarErosaoDilatacao(Operacao.DILATACAO, TipoElemento.CRUZ, 3));
        setStatus("Dilatação aplicada.");
    }

    private void showErosionDialog() {
        transformedPanel.setImage(ImageController.processarErosaoDilatacao(Operacao.EROSAO, TipoElemento.CRUZ, 3));
        setStatus("Erosão aplicada.");
    }

    private void showOpeningDialog() {
        transformedPanel.setImage(ImageController.abertura(TipoElemento.CRUZ, 3));
        setStatus("Abertura (erosão + dilatação).");
    }

    private void showClosingDialog() {
        transformedPanel.setImage(ImageController.fechamento(TipoElemento.CRUZ, 3));
        setStatus("Fechamento (dilatação + erosão).");
    }

    private void showThinDialog() {
        transformedPanel.setImage(ImageController.zhangSuen());
        setStatus("Afinamento Zhang-Suen aplicado.");
    }

    // ═══════════════════════════════════════════════════════════════
    //  Menu bar
    // ═══════════════════════════════════════════════════════════════

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(C_SURFACE);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));

        menuBar.add(criarMenuArquivo());
        menuBar.add(criarMenu("Geometria", new String[][]{
                {"Translação...",   null,  null},
                {"Rotacionar...",   null,  null},
                {"Espelhar...",     null,  null},
                {"Aumentar...",     null,  null},
                {"Diminuir...",     null,  null},
        }, new ActionListener[]{
                e -> showTranslationDialog(),
                e -> showRotationDialog(),
                e -> showMirrorDialog(),
                e -> showIncreaseScaleDialog(),
                e -> showDecreaseScaleDialog(),
        }));
        menuBar.add(criarMenu("Filtros", new String[][]{
                {"Grayscale",            null, null},
                {"Brilho...",            null, null},
                {"Contraste...",         null, null},
                {"Suavizar (Gauss)...",  null, null},
                {"Threshold (Otsu)",     null, null},
                {"Bordas Roberts",       null, null},
                {"Bordas Marr-Hildreth", null, null},
        }, new ActionListener[]{
                e -> showGrayScaleDialog(),
                e -> showShineDialog(),
                e -> showContrastDialog(),
                e -> showGaussianDialog(),
                e -> showThresholdDialog(),
                e -> showRobertsDialog(),
                e -> showMarrDialog(),
        }));
        menuBar.add(criarMenu("Morfologia", new String[][]{
                {"Dilatação",  null, null},
                {"Erosão",     null, null},
                {"Abertura",   null, null},
                {"Fechamento", null, null},
                {"Afinamento", null, null},
        }, new ActionListener[]{
                e -> showDilationDialog(),
                e -> showErosionDialog(),
                e -> showOpeningDialog(),
                e -> showClosingDialog(),
                e -> showThinDialog(),
        }));
        menuBar.add(criarMenuDesafios());

        return menuBar;
    }

    private JMenu criarMenuArquivo() {
        JMenu menu = new JMenu("Arquivo");
        estilizarMenu(menu);
        menu.add(createMenuItem("Abrir Imagem",  "ctrl O", e -> { controller.openImage();  setStatus("Imagem aberta."); }));
        menu.add(createMenuItem("Salvar Imagem", "ctrl S", e -> { controller.saveImage();  setStatus("Imagem salva."); }));
        menu.addSeparator();
        menu.add(createMenuItem("Sobre", null, e -> controller.showAbout()));
        menu.add(createMenuItem("Sair",  null, e -> System.exit(0)));
        return menu;
    }

    private JMenu criarMenu(String nome, String[][] itens, ActionListener[] acoes) {
        JMenu menu = new JMenu(nome);
        estilizarMenu(menu);
        for (int i = 0; i < itens.length; i++) {
            // Operações de Geometria/Filtros/Morfologia exigem uma imagem aberta.
            menu.add(createMenuItem(itens[i][0], itens[i][1], exigeImagem(acoes[i])));
        }
        return menu;
    }

    // Envolve uma ação para que só execute se houver imagem aberta;
    // caso contrário, avisa o usuário.
    private ActionListener exigeImagem(ActionListener acao) {
        return e -> {
            if (!ImageController.temImagem()) {
                JOptionPane.showMessageDialog(this,
                        "Abra uma imagem antes de aplicar uma operação.",
                        "Nenhuma imagem aberta",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            acao.actionPerformed(e);
        };
    }

    private JMenu criarMenuDesafios() {
        JMenu menu = new JMenu("Exercícios ✦");
        estilizarMenu(menu);
        menu.setForeground(C_PRIMARY);
        menu.add(createMenuItem("a) Relógio Analógico → Horário Digital", null,
                e -> executarDesafio(DesafioController.TipoDesafio.RELOGIO)));
        menu.add(createMenuItem("b) Contar Objetos por Cor", null,
                e -> executarDesafio(DesafioController.TipoDesafio.OBJETOS_COLORIDOS)));
        menu.add(createMenuItem("c) Detectar Letras (A–Z)", null,
                e -> executarDesafio(DesafioController.TipoDesafio.LETRAS)));
        menu.add(createMenuItem("d) Identificar Placas de Trânsito", null,
                e -> executarDesafio(DesafioController.TipoDesafio.PLACAS)));
        menu.add(createMenuItem("e) Barras: Mais Alta e Mais Baixa", null,
                e -> executarDesafio(DesafioController.TipoDesafio.BARRAS)));
        return menu;
    }

    private void estilizarMenu(JMenu menu) {
        menu.setFont(new Font("SansSerif", Font.PLAIN, 13));
        menu.setForeground(C_TEXT);
        menu.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Getters públicos
    // ═══════════════════════════════════════════════════════════════

    public ImagePanel getOriginalPanel()    { return originalPanel; }
    public ImagePanel getTransformedPanel() { return transformedPanel; }

    // ═══════════════════════════════════════════════════════════════
    //  Borda com sombra suave
    // ═══════════════════════════════════════════════════════════════

    private static class ShadowBorder implements Border {
        private final Color borderColor;
        ShadowBorder(Color borderColor) { this.borderColor = borderColor; }

        @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 3, 3); }
        @Override public boolean isBorderOpaque() { return false; }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, 25));
            g2.drawRoundRect(x + 2, y + 2, w - 3, h - 3, 6, 6);
            g2.setColor(new Color(0, 0, 0, 12));
            g2.drawRoundRect(x + 3, y + 3, w - 3, h - 3, 6, 6);
            g2.setColor(borderColor);
            g2.drawRoundRect(x, y, w - 3, h - 3, 6, 6);
            g2.dispose();
        }
    }
}
