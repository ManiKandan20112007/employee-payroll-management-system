package epms;

import java.awt.*;
import java.awt.event.*;
import java.util.Locale;

/** Small AWT helpers: colours, fonts, styled widgets and modal message boxes. */
final class Ui {
    private Ui() {}

    static final Color NAVY = new Color(0x1F3A5F), BG = new Color(0xF4F6FA), ACCENT = new Color(0x2F7DE1),
            RED = new Color(0xB42318), GREEN = new Color(0x157F4B), MUTE = new Color(0x667085), WHITE = Color.WHITE;
    static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 24), HEAD = new Font(Font.SANS_SERIF, Font.BOLD, 16),
            BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 14), MONO = new Font(Font.MONOSPACED, Font.PLAIN, 13),
            MONO_B = new Font(Font.MONOSPACED, Font.BOLD, 13);

    static String money(double v) { return String.format(Locale.US, "%,.2f", v); }
    static String num(double v) { return v == Math.rint(v) ? Long.toString((long) v) : Double.toString(v); }

    static Label label(String text, Font f, Color fg) {
        Label l = new Label(text);
        l.setFont(f);
        l.setForeground(fg);
        return l;
    }

    static Button button(String text, Color bg, ActionListener a) {
        Button b = new Button(text);
        b.setFont(BODY);
        b.setBackground(bg);
        b.setForeground(bg == WHITE || bg == BG ? Color.BLACK : WHITE);
        b.addActionListener(a);
        return b;
    }

    static TextField field(int cols) {
        TextField t = new TextField(cols);
        t.setFont(BODY);
        t.setBackground(WHITE);
        return t;
    }

    static TextArea textArea(int rows, int cols) {
        TextArea t = new TextArea("", rows, cols, TextArea.SCROLLBARS_BOTH);
        t.setEditable(false);
        t.setFont(MONO);
        t.setBackground(WHITE);
        return t;
    }

    /** Panel with a solid background and the given layout. */
    static Panel panel(LayoutManager lm) {
        Panel p = new Panel(lm);
        p.setBackground(BG);
        p.setFont(BODY);
        return p;
    }

    static GridBagConstraints gbc(int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x; c.gridy = y;
        c.insets = new Insets(5, 8, 5, 8);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        return c;
    }

    // AWT has no JOptionPane, so these build modal Dialogs by hand.
    static void message(Frame owner, String title, String text, boolean error) {
        Dialog d = new Dialog(owner, title, true);
        d.setBackground(BG);
        d.setLayout(new BorderLayout(10, 10));
        Label l = label(text, BODY, error ? RED : Color.BLACK);
        Panel center = panel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        center.add(l);
        d.add(center, BorderLayout.CENTER);
        Panel south = panel(new FlowLayout());
        south.add(button("OK", ACCENT, e -> d.dispose()));
        d.add(south, BorderLayout.SOUTH);
        show(d, owner);
    }

    static boolean confirm(Frame owner, String text) {
        boolean[] yes = {false};
        Dialog d = new Dialog(owner, "Confirm", true);
        d.setBackground(BG);
        d.setLayout(new BorderLayout(10, 10));
        Panel center = panel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        center.add(label(text, BODY, Color.BLACK));
        d.add(center, BorderLayout.CENTER);
        Panel south = panel(new FlowLayout());
        south.add(button("Yes", RED, e -> { yes[0] = true; d.dispose(); }));
        south.add(button("No", BG, e -> d.dispose()));
        d.add(south, BorderLayout.SOUTH);
        show(d, owner);
        return yes[0];
    }

    /** Show a modal dialog centred on its owner; the close button disposes it. */
    static void show(Dialog d, Frame owner) {
        d.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { d.dispose(); }
        });
        d.pack();
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }
}
