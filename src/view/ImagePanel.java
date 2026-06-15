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
    private boolean fitToPanel = false;
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

    /** Quando ativo, a imagem é redimensionada para caber inteira no painel
     *  (mantendo a proporção, centralizada) em vez de desenhada em tamanho
     *  real. Evita que imagens maiores que o painel apareçam cortadas. */
    public void setFitToPanel(boolean fit) {
        this.fitToPanel = fit;
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
        if (image == null) return;

        if (fitToPanel) {
            // "Contain": escala a imagem para caber inteira no painel mantendo a
            // proporção e a centraliza — assim nada é cortado nas extremidades.
            int pw = getWidth(), ph = getHeight();
            int iw = image.getWidth(), ih = image.getHeight();
            if (pw <= 0 || ph <= 0 || iw <= 0 || ih <= 0) return;

            double escala = Math.min((double) pw / iw, (double) ph / ih);
            int dw = (int) Math.round(iw * escala);
            int dh = (int) Math.round(ih * escala);
            int x = (pw - dw) / 2;
            int y = (ph - dh) / 2;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(image, x, y, dw, dh, null);
            g2.dispose();
        } else {
            int x = (getWidth() - image.getWidth()) / 2 + offsetX;
            int y = (getHeight() - image.getHeight()) / 2 + offsetY;
            g.drawImage(image, x, y, null);
        }
    }
}
