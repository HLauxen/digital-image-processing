package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class ImagePanel extends JPanel {

    public interface ClickListener {
        void onClick(int imageX, int imageY);
    }

    private BufferedImage image;
    private int offsetX = 0;
    private int offsetY = 0;
    private MouseAdapter currentMouseAdapter;

    public ImagePanel() {
        this.image = null;
    }

    public ImagePanel(BufferedImage image) {
        this.image = image;
    }

    public void setOffset(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
        repaint();
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setClickListener(ClickListener listener) {
        if (currentMouseAdapter != null) {
            removeMouseListener(currentMouseAdapter);
            currentMouseAdapter = null;
        }
        if (listener != null) {
            currentMouseAdapter = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (image == null) return;
                    int ox = (getWidth() - image.getWidth()) / 2 + offsetX;
                    int oy = (getHeight() - image.getHeight()) / 2 + offsetY;
                    int imgX = e.getX() - ox;
                    int imgY = e.getY() - oy;
                    if (imgX >= 0 && imgX < image.getWidth() && imgY >= 0 && imgY < image.getHeight()) {
                        listener.onClick(imgX, imgY);
                    }
                }
            };
            addMouseListener(currentMouseAdapter);
        }
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
