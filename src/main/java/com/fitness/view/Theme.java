package com.fitness.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/** Shared colours and small UI builders so every screen looks consistent. */
public final class Theme {
    public static final Color PRIMARY = new Color(41, 128, 185);
    public static final Color SUCCESS = new Color(39, 174, 96);
    public static final Color DANGER = new Color(192, 57, 43);
    public static final Color WARNING = new Color(230, 126, 34);
    public static final Color BG = new Color(245, 247, 250);
    public static final Color TEXT = new Color(44, 62, 80);
    public static final Color MUTED = new Color(127, 140, 141);
    public static final Color TRACK = new Color(228, 231, 235);
    public static final Color BORDER = new Color(220, 224, 230);
    public static final Color[] PALETTE = {
            new Color(52, 152, 219), new Color(46, 204, 113), new Color(155, 89, 182),
            new Color(241, 196, 15), new Color(231, 76, 60), new Color(26, 188, 156)
    };

    private Theme() { }

    public static Font font(int style, int size) { return new Font(Font.SANS_SERIF, style, size); }

    public static JPanel card(String title, LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(Color.WHITE);
        var inner = BorderFactory.createCompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(12, 12, 12, 12));
        if (title == null) {
            p.setBorder(inner);
        } else {
            TitledBorder tb = BorderFactory.createTitledBorder(inner, title);
            tb.setTitleFont(font(Font.BOLD, 13));
            tb.setTitleColor(TEXT);
            p.setBorder(tb);
        }
        return p;
    }

    public static JButton button(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(font(Font.BOLD, 13));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    public static JPanel header(String title, String subtitle, Runnable onLogout) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PRIMARY);
        p.setBorder(new EmptyBorder(14, 20, 14, 20));

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(font(Font.BOLD, 20));
        t.setForeground(Color.WHITE);
        JLabel s = new JLabel(subtitle);
        s.setFont(font(Font.PLAIN, 12));
        s.setForeground(new Color(220, 235, 248));
        left.add(t);
        left.add(s);

        JButton out = new JButton("Log out");
        out.setFocusPainted(false);
        out.addActionListener(e -> onLogout.run());

        p.add(left, BorderLayout.WEST);
        p.add(out, BorderLayout.EAST);
        return p;
    }

    public static void error(Component parent, Throwable t) {
        String msg = t.getMessage() == null ? t.toString() : t.getMessage();
        JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void styleTable(JTable t) {
        t.setRowHeight(28);
        t.setShowVerticalLines(false);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setReorderingAllowed(false);
        t.setFillsViewportHeight(true);
    }
}
