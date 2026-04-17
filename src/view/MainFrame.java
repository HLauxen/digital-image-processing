package view;

import controller.ImageController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class MainFrame extends JFrame {

    private final ImagePanel originalPanel;
    private ImagePanel transformedPanel;
    private final ImageController controller;

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

        add(splitPane, BorderLayout.CENTER);

        setJMenuBar(createMenuBar());

        setVisible(true);
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
        menuFiltros.add(createMenuItem("Suavizar...", null, e -> showGaussianDialog()));
        menuFiltros.add(createMenuItem("Detecta Bordas (Roberts)...", null, e -> showRobertsDialog()));
        menuFiltros.add(createMenuItem("Detecta Bordas (Marr Hildreth)...", null, e -> showMarrDialog()));

        JMenu menuMorfologia = new JMenu("Morfologia Matemática");
        menuMorfologia.add(new JMenuItem("Dilatação"));
        menuMorfologia.add(new JMenuItem("Erosão"));
        menuMorfologia.add(new JMenuItem("Abertura"));
        menuMorfologia.add(new JMenuItem("Fechamento"));
        menuMorfologia.add(new JMenuItem("Afinamento"));

        JMenu menuExtracao = new JMenu("Extração de Características");
        menuExtracao.add(new JMenuItem("Desafio"));

        menuBar.add(menuArquivo);
        menuBar.add(menuGeo);
        menuBar.add(menuFiltros);
        menuBar.add(menuMorfologia);
        menuBar.add(menuExtracao);

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
