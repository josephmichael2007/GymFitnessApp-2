package com.fitness.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;

/** Animated ring showing value / goal. */
public class CircularProgressPanel extends JPanel {
    private int value = 0;
    private int goal = 1000;
    private double shown = 0;
    private Timer timer;
    private String caption = "kcal today";

    public CircularProgressPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(200, 200));
    }

    public void setCaption(String caption) { this.caption = caption; repaint(); }

    public void setProgress(int value, int goal) {
        this.value = value;
        this.goal = Math.max(goal, 1);
        final double target = Math.min(1.0, (double) value / this.goal);
        if (timer != null) timer.stop();
        timer = new Timer(15, e -> {
            shown += (target - shown) * 0.15;
            if (Math.abs(target - shown) < 0.002) {
                shown = target;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int stroke = 14;
        int d = Math.min(w, h) - stroke - 8;
        int x = (w - d) / 2, y = (h - d) / 2;

        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.TRACK);
        g2.drawOval(x, y, d, d);

        g2.setColor(value >= goal ? Theme.SUCCESS : Theme.PRIMARY);
        if (shown > 0) g2.draw(new Arc2D.Double(x, y, d, d, 90, -360 * shown, Arc2D.OPEN));

        g2.setColor(Theme.TEXT);
        g2.setFont(Theme.font(Font.BOLD, Math.max(16, d / 6)));
        FontMetrics fm = g2.getFontMetrics();
        String big = String.valueOf(value);
        int cy = h / 2;
        g2.drawString(big, (w - fm.stringWidth(big)) / 2, cy);

        g2.setColor(Theme.MUTED);
        g2.setFont(Theme.font(Font.PLAIN, 12));
        fm = g2.getFontMetrics();
        String sub = caption + " / " + goal;
        g2.drawString(sub, (w - fm.stringWidth(sub)) / 2, cy + 18);
        String pct = (value * 100 / goal) + "%";
        g2.drawString(pct, (w - fm.stringWidth(pct)) / 2, cy + 34);
        g2.dispose();
    }
}
