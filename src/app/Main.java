package app;

import view.MainFrame;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        configurarVisual();
        SwingUtilities.invokeLater(() -> {
            new MainFrame();
        });
    }

    private static void configurarVisual() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");

            Color primary = new Color(108, 99, 255);
            Color bg      = new Color(242, 243, 250);

            UIManager.put("nimbusBase",         primary);
            UIManager.put("nimbusBlueGrey",      new Color(200, 200, 215));
            UIManager.put("control",             bg);
            UIManager.put("text",                new Color(28, 28, 45));
            UIManager.put("menuText",            new Color(28, 28, 45));
            UIManager.put("infoText",            new Color(28, 28, 45));

            Font uiFont   = new Font("SansSerif", Font.PLAIN, 13);
            Font boldFont = new Font("SansSerif", Font.BOLD, 13);
            UIManager.put("defaultFont",        uiFont);
            UIManager.put("Menu.font",          boldFont);
            UIManager.put("MenuItem.font",      uiFont);
            UIManager.put("Label.font",         uiFont);
            UIManager.put("Button.font",        boldFont);
            UIManager.put("OptionPane.messageFont", uiFont);
            UIManager.put("OptionPane.buttonFont",  boldFont);
            UIManager.put("TitledBorder.font",  boldFont);

        } catch (Exception ignored) {}
    }
}
