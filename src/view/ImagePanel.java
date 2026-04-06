package view;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImagePanel extends JPanel {

    private BufferedImage image;

    private int offsetX = 0;
    private int offsetY = 0;

    public void setOffset(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
        repaint();
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    public ImagePanel() {
    this.image = null;
}

    public BufferedImage getImage() {
        return image;
    }

    public ImagePanel(BufferedImage image) {
        this.image = image;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (image != null) {
            int x = (getWidth() - image.getWidth()) / 2 + offsetX;
            int y = (getHeight() - image.getHeight()) / 2 + offsetY;
            g.drawImage(image, x, y, null);
        }
    }
}
