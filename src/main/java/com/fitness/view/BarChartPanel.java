package com.fitness.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;

/** Simple vertical bar chart with an optional dashed goal line. */
public class BarChartPanel extends JPanel {
    private Map<String, Integer> data = new LinkedHashMap<>();
    private int goal = 0;
    private final String title;

    public BarChartPanel(String title) {
        this.title = title;
        setOpaque(false);
        setPreferredSize(new Dimension(320, 210));
    }

    public void setData(Map<String, Integer> data, int goal) {
        this.data = data;
        this.goal = goal;
        repaint();
    }

    private static int niceMax(int m) {
        if (m <= 0) return 100;
        double step = Math.pow(10, Math.floor(Math.log10(m)));
        return (int) (Math.ceil(m / step) * step);
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

        int left = 42, right = 8, top = 28, bottom = 26;
        int w = getWidth() - left - right, h = getHeight() - top - bottom;
        if (w <= 0 || h <= 0) { g2.dispose(); return; }

        int maxVal = data.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int max = niceMax(Math.max(maxVal, goal));

        g2.setFont(Theme.font(Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i <= 4; i++) {
            int y = top + h - h * i / 4;
            g2.setColor(Theme.TRACK);
            g2.drawLine(left, y, left + w, y);
            g2.setColor(Theme.MUTED);
            String lbl = String.valueOf(max * i / 4);
            g2.drawString(lbl, left - 6 - fm.stringWidth(lbl), y + 4);
        }

        int n = Math.max(data.size(), 1);
        int slot = w / n;
        int barW = Math.max(8, Math.min(40, slot - 12));
        int i = 0;
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            int v = e.getValue();
            int bh = (int) ((double) v / max * h);
            int bx = left + i * slot + (slot - barW) / 2;
            int by = top + h - bh;
            g2.setColor(goal > 0 && v >= goal ? Theme.SUCCESS : Theme.PRIMARY);
            if (bh > 0) g2.fill(new RoundRectangle2D.Double(bx, by, barW, bh, 8, 8));
            g2.setColor(Theme.MUTED);
            String lbl = e.getKey();
            g2.drawString(lbl, left + i * slot + (slot - fm.stringWidth(lbl)) / 2, top + h + 16);
            if (v > 0) {
                g2.setColor(Theme.TEXT);
                String s = String.valueOf(v);
                g2.drawString(s, bx + (barW - fm.stringWidth(s)) / 2, by - 4);
            }
            i++;
        }

        if (goal > 0) {
            int gy = top + h - (int) ((double) goal / max * h);
            g2.setColor(Theme.DANGER);
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 4f}, 0f));
            g2.drawLine(left, gy, left + w, gy);
        }
        g2.dispose();
    }
}
