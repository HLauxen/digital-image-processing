package view;

import controller.DesafioController;
import controller.ImageController;
import controller.ImageController.Operacao;
import controller.ImageController.TipoElemento;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class MainFrame extends JFrame {

    private final ImagePanel originalPanel;
    private ImagePanel transformedPanel;
    private final ImageController controller;

    // --- Modo Desafio ---
    private CardLayout cardLayout;
    private JPanel mainCard;
    private ImagePanel desafioImagePanel;
    private JTextArea desafioTextArea;

    public MainFrame() {
        setTitle("Processamento Digital de Imagens");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);

        controller = new ImageController(this);

        JLabel authorsLabel = new JLabel("Autor: Henrique Lauxen Seefeld");
        authorsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(authorsLabel, BorderLayout.NORTH);

        originalPanel = new ImagePanel();
        transformedPanel = new ImagePanel();

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                originalPanel,
                transformedPanel
        );
        splitPane.setDividerLocation(600);

        // Card: modo normal (antes/depois)
        cardLayout = new CardLayout();
        mainCard = new JPanel(cardLayout);
        mainCard.add(splitPane, "NORMAL");

        // Card: modo desafio (painel único)
        mainCard.add(criarPainelDesafio(), "DESAFIO");

        add(mainCard, BorderLayout.CENTER);

        setJMenuBar(createMenuBar());
        setVisible(true);
    }

    private JPanel criarPainelDesafio() {
        JPanel painel = new JPanel(new BorderLayout(0, 4));

        JButton btnVoltar = new JButton("← Voltar ao modo normal");
        btnVoltar.addActionListener(e -> cardLayout.show(mainCard, "NORMAL"));
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topo.add(btnVoltar);

        desafioImagePanel = new ImagePanel();

        desafioTextArea = new JTextArea(7, 0);
        desafioTextArea.setEditable(false);
        desafioTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        desafioTextArea.setBackground(new Color(30, 30, 30));
        desafioTextArea.setForeground(new Color(220, 220, 220));
        JScrollPane scroll = new JScrollPane(desafioTextArea);
        scroll.setPreferredSize(new Dimension(0, 160));

        painel.add(topo, BorderLayout.NORTH);
        painel.add(desafioImagePanel, BorderLayout.CENTER);
        painel.add(scroll, BorderLayout.SOUTH);
        return painel;
    }

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
        repaint();

        // Processa em background para não travar a UI
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
                } catch (Exception ex) {
                    desafioTextArea.setText("Erro ao processar: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private JMenuItem createMenuItem(String title, String accelerator, ActionListener action) {
        JMenuItem item = new JMenuItem(title);
        item.setFont(new Font("SansSerif", Font.PLAIN, 12));
        item.addActionListener(action);

        if (accelerator != null) {
            item.setAccelerator(KeyStroke.getKeyStroke(accelerator));
        }

        return item;
    }

    private void showTranslationDialog() {

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        JSpinner dxSpinner = new JSpinner(new SpinnerNumberModel(0, -2000, 2000, 10));
        JSpinner dySpinner = new JSpinner(new SpinnerNumberModel(0, -2000, 2000, 10));

        panel.add(new JLabel("Deslocamento X:"));
        panel.add(dxSpinner);
        panel.add(new JLabel("Deslocamento Y:"));
        panel.add(dySpinner);

        int result = JOptionPane.showConfirmDialog(MainFrame.this, panel,
                "Translação", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            int dx = (int) dxSpinner.getValue();
            int dy = (int) dySpinner.getValue();
            transformedPanel.setImage(ImageController.transladar(dx, dy));
        }
    }

    private void showRotationDialog() {

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        JSpinner dxSpinner = new JSpinner(new SpinnerNumberModel(0, -2000, 2000, 10));

        panel.add(new JLabel("Rotacionar X graus:"));
        panel.add(dxSpinner);

        int result = JOptionPane.showConfirmDialog(MainFrame.this, panel,
                "Rotação", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            double graus = (int) dxSpinner.getValue();
            transformedPanel.setImage(ImageController.rotacionar(graus));
        }
    }

    private void showMirrorDialog() {
        String[] options = {"Horizontal", "Vertical"};

        int result = JOptionPane.showOptionDialog(
                MainFrame.this,
                "Escolha o tipo de espelhamento:",
                "Espelhar",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (result == 0) {
            transformedPanel.setImage(ImageController.espelharHorizontal());
        } else if (result == 1) {
            transformedPanel.setImage(ImageController.espelharVertical());
        }
    }

    private void showIncreaseScaleDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));

        JSpinner scaleSpinner = new JSpinner(
                new SpinnerNumberModel(2, 2, 10, 1)
        );

        panel.add(new JLabel("Fator de aumento:"));
        panel.add(scaleSpinner);

        int result = JOptionPane.showConfirmDialog(
                MainFrame.this,
                panel,
                "Aumentar Escala",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            int scale = (int) scaleSpinner.getValue();
            transformedPanel.setImage(ImageController.escalar(scale, scale));
        }
    }

    private void showDecreaseScaleDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));

        JSpinner scaleSpinner = new JSpinner(
                new SpinnerNumberModel(2, 2, 10, 1)
        );

        panel.add(new JLabel("Divisor da escala:"));
        panel.add(scaleSpinner);

        int result = JOptionPane.showConfirmDialog(
                MainFrame.this,
                panel,
                "Diminuir Escala",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            int divisor = (int) scaleSpinner.getValue();

            double scale = 1.0 / divisor;

            transformedPanel.setImage(ImageController.escalar(scale, scale));
        }
    }

    private void showGrayScaleDialog() {
        transformedPanel.setImage(ImageController.grayscale());
    }

    private void showShineDialog() {
        JSpinner brilhoSpinner = new JSpinner(
                new SpinnerNumberModel(0, -255, 255, 10)
        );

        int result = JOptionPane.showConfirmDialog(
                MainFrame.this,
                brilhoSpinner,
                "Ajustar Brilho",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            int brilho = (int) brilhoSpinner.getValue();
            transformedPanel.setImage(ImageController.ajustarBrilho(brilho));
        }
    }

    private void showContrastDialog() {
        JSpinner contrasteSpinner = new JSpinner(
                new SpinnerNumberModel(1.0, 0.1, 5.0, 0.1)
        );

        int result = JOptionPane.showConfirmDialog(
                MainFrame.this,
                contrasteSpinner,
                "Ajustar Contraste",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            double contraste = (double) contrasteSpinner.getValue();
            transformedPanel.setImage(ImageController.ajustarContraste(contraste));
        }
    }

    private void showGaussianDialog() {
        JSpinner kernelSpinner = new JSpinner(
                new SpinnerNumberModel(3, 3, 15, 2) // começa em 3, vai de 3 até 15, passo 2 (ímpares)
        );

        int result = JOptionPane.showConfirmDialog(
                MainFrame.this,
                kernelSpinner,
                "Filtro Gaussiano (Tamanho do Kernel)",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            int tamanhoKernel = (int) kernelSpinner.getValue();
            transformedPanel.setImage(
                    ImageController.gaussianBlur(tamanhoKernel)
            );
        }
    }

    private void showThresholdDialog() {
        transformedPanel.setImage(
                ImageController.otsu()
        );
    }

    private void showRobertsDialog() {
        transformedPanel.setImage(
                ImageController.roberts()
        );
    }

    private void showMarrDialog() {
        transformedPanel.setImage(
                ImageController.marrHildreth()
        );
    }

    private void showDilationDialog() {
        transformedPanel.setImage(
                ImageController.processarErosaoDilatacao(Operacao.DILATACAO, TipoElemento.CRUZ, 3)
        );
    }

    private void showErosionDialog() {
        transformedPanel.setImage(
                ImageController.processarErosaoDilatacao(Operacao.EROSAO, TipoElemento.CRUZ, 3)
        );
    }

    private void showThinDialog() {
        transformedPanel.setImage(
                ImageController.zhangSuen()
        );
    }

    private void ativarFloodfill(boolean oito) {
        String conectividade = oito ? "8 direções" : "4 direções";
        JOptionPane.showMessageDialog(this,
                "Modo Floodfill (" + conectividade + ") ativado.\nClique na imagem original para selecionar o pixel semente.",
                "Floodfill", JOptionPane.INFORMATION_MESSAGE);

        originalPanel.setClickListener((imgX, imgY) -> {
            originalPanel.setClickListener(null);
            java.awt.Color cor = JColorChooser.showDialog(this, "Escolha a cor de substituição", java.awt.Color.RED);
            if (cor != null) {
                transformedPanel.setImage(ImageController.floodfill(imgX, imgY, cor, oito));
            }
        });
    }

    private void showRotularRegioes() {
        transformedPanel.setImage(ImageController.rotularRegioes());
    }

    private void showContarObjetos() {
        ImageController.ResultadoAnalise resultado = ImageController.contarObjetos();
        transformedPanel.setImage(resultado.imagem);
        JOptionPane.showMessageDialog(this, resultado.relatorio, "Contagem de Objetos", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showCalcularArea() {
        ImageController.ResultadoAnalise resultado = ImageController.calcularArea();
        transformedPanel.setImage(resultado.imagem);
        mostrarRelatorio("Área por Região", resultado.relatorio);
    }

    private void showPerimetroCircularidade() {
        ImageController.ResultadoAnalise resultado = ImageController.calcularPerimetroCircularidade();
        transformedPanel.setImage(resultado.imagem);
        mostrarRelatorio("Perímetro e Circularidade", resultado.relatorio);
    }

    private void mostrarRelatorio(String titulo, String texto) {
        JTextArea area = new JTextArea(texto);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setEditable(false);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new java.awt.Dimension(480, 320));
        JOptionPane.showMessageDialog(this, scroll, titulo, JOptionPane.INFORMATION_MESSAGE);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menuArquivo = getJMenu();

        JMenu menuGeo = new JMenu("Transformações Geométricas");
        menuGeo.add(createMenuItem("Translação...", null, e -> showTranslationDialog()));
        menuGeo.add(createMenuItem("Rotacionar...", null, e -> showRotationDialog()));
        menuGeo.add(createMenuItem("Espelhar...", null, e -> showMirrorDialog()));
        menuGeo.add(createMenuItem("Aumentar...", null, e -> showIncreaseScaleDialog()));
        menuGeo.add(createMenuItem("Diminuir...", null, e -> showDecreaseScaleDialog()));

        JMenu menuFiltros = new JMenu("Filtros");
        menuFiltros.add(createMenuItem("Grayscale...", null, e -> showGrayScaleDialog()));
        menuFiltros.add(createMenuItem("Brilho...", null, e -> showShineDialog()));
        menuFiltros.add(createMenuItem("Contraste...", null, e -> showContrastDialog()));
        menuFiltros.add(createMenuItem("Suavizar (Gauss)...", null, e -> showGaussianDialog()));
        menuFiltros.add(createMenuItem("Threshold...", null, e -> showThresholdDialog()));
        menuFiltros.add(createMenuItem("Detecta Bordas (Roberts)...", null, e -> showRobertsDialog()));
        menuFiltros.add(createMenuItem("Detecta Bordas (Marr Hildreth)...", null, e -> showMarrDialog()));

        JMenu menuMorfologia = new JMenu("Morfologia Matemática");
        menuMorfologia.add(createMenuItem("Dilatação", null, e -> showDilationDialog()));
        menuMorfologia.add(createMenuItem("Erosão", null, e -> showErosionDialog()));
        menuMorfologia.add(createMenuItem("Abertura", null, e -> showContrastDialog()));
        menuMorfologia.add(createMenuItem("Fechamento", null, e -> showGaussianDialog()));
        menuMorfologia.add(createMenuItem("Afinamento", null, e -> showThinDialog()));

        JMenu menuExtracao = new JMenu("Extração de Características");
        menuExtracao.add(createMenuItem("Floodfill 4-dir (clique na imagem)...", null, e -> ativarFloodfill(false)));
        menuExtracao.add(createMenuItem("Floodfill 8-dir (clique na imagem)...", null, e -> ativarFloodfill(true)));
        menuExtracao.add(createMenuItem("Rotular Regiões", null, e -> showRotularRegioes()));
        menuExtracao.add(createMenuItem("Contar Objetos", null, e -> showContarObjetos()));
        menuExtracao.add(createMenuItem("Área por Região", null, e -> showCalcularArea()));
        menuExtracao.add(createMenuItem("Perímetro e Circularidade", null, e -> showPerimetroCircularidade()));

        JMenu menuDesafio = new JMenu("Desafios");
        menuDesafio.add(createMenuItem("a) Relógio Analógico → Horário Digital", null,
                e -> executarDesafio(DesafioController.TipoDesafio.RELOGIO)));
        menuDesafio.add(createMenuItem("b) Contar Objetos por Cor", null,
                e -> executarDesafio(DesafioController.TipoDesafio.OBJETOS_COLORIDOS)));
        menuDesafio.add(createMenuItem("c) Detectar Letras (A–Z)", null,
                e -> executarDesafio(DesafioController.TipoDesafio.LETRAS)));
        menuDesafio.add(createMenuItem("d) Identificar Placas de Trânsito", null,
                e -> executarDesafio(DesafioController.TipoDesafio.PLACAS)));
        menuDesafio.add(createMenuItem("e) Barras: Mais Alta e Mais Baixa", null,
                e -> executarDesafio(DesafioController.TipoDesafio.BARRAS)));

        menuBar.add(menuArquivo);
        menuBar.add(menuGeo);
        menuBar.add(menuFiltros);
        menuBar.add(menuMorfologia);
        menuBar.add(menuExtracao);
        menuBar.add(menuDesafio);

        return menuBar;
    }

    private JMenu getJMenu() {
        JMenu menuArquivo = new JMenu("Arquivo");
        JMenuItem abrir = new JMenuItem("Abrir Imagem");
        JMenuItem salvar = new JMenuItem("Salvar Imagem");
        JMenuItem sobre = new JMenuItem("Sobre");
        JMenuItem sair = new JMenuItem("Sair");

        abrir.addActionListener(e -> controller.openImage());
        salvar.addActionListener(e -> controller.saveImage());
        sobre.addActionListener(e -> controller.showAbout());
        sair.addActionListener(e -> System.exit(0));

        menuArquivo.add(abrir);
        menuArquivo.add(salvar);
        menuArquivo.addSeparator();
        menuArquivo.add(sobre);
        menuArquivo.add(sair);
        return menuArquivo;
    }

    public ImagePanel getOriginalPanel() {
        return originalPanel;
    }

    public ImagePanel getTransformedPanel() {
        return transformedPanel;
    }
}
