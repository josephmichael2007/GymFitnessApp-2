package com.fitness.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.util.LinkedHashMap;
import java.util.Map;

/** Donut chart of calories by workout category, with a legend. */
public class DonutChartPanel extends JPanel {
    private Map<String, Integer> data = new LinkedHashMap<>();
    private final String title;

    public DonutChartPanel(String title) {
        this.title = title;
        setOpaque(false);
        setPreferredSize(new Dimension(320, 210));
    }

    public void setData(Map<String, Integer> data) {
        this.data = data;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(Theme.TEXT);
        g2.setFont(Theme.font(Font.BOLD, 13));
        g2.drawString(title, 4, 14);

        int total = data.values().stream().mapToInt(Integer::intValue).sum();
        int areaTop = 26;
        int d = Math.min(getHeight() - areaTop - 8, getWidth() / 2 - 10);
        if (d <= 20) { g2.dispose(); return; }

        if (total == 0) {
            g2.setColor(Theme.MUTED);
            g2.setFont(Theme.font(Font.PLAIN, 12));
            g2.drawString("No data yet", 10, areaTop + 30);
            g2.dispose();
            return;
        }

        int x = 8, y = areaTop + (getHeight() - areaTop - d) / 2;
        int stroke = Math.max(14, d / 5);
        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        double start = 90;
        int i = 0;
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            double sweep = -360.0 * e.getValue() / total;
            g2.setColor(Theme.PALETTE[i % Theme.PALETTE.length]);
            g2.draw(new Arc2D.Double(x + stroke / 2.0, y + stroke / 2.0, d - stroke, d - stroke, start, sweep, Arc2D.OPEN));
            start += sweep;
            i++;
        }

        g2.setColor(Theme.TEXT);
        g2.setFont(Theme.font(Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();
        String t = String.valueOf(total);
        g2.drawString(t, x + (d - fm.stringWidth(t)) / 2, y + d / 2 + 5);

        int lx = x + d + 18, ly = y + 14;
        g2.setFont(Theme.font(Font.PLAIN, 12));
        i = 0;
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            g2.setColor(Theme.PALETTE[i % Theme.PALETTE.length]);
            g2.fillRoundRect(lx, ly - 10, 12, 12, 4, 4);
            g2.setColor(Theme.TEXT);
            g2.drawString(e.getKey() + "  " + (e.getValue() * 100 / total) + "%", lx + 18, ly);
            ly += 20;
            i++;
        }
        g2.dispose();
    }
}
