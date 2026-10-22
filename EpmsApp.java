package epms;

import epms.Database.*;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Employee Payroll Management System - desktop edition built only with java.awt.
 * Screens: Login -> HR (Employees / Run Payroll / Reports) or Employee (own payslips).
 * Layouts used: CardLayout, BorderLayout, GridBagLayout, GridLayout, FlowLayout.
 */
public final class EpmsApp extends Frame {
    private static final Pattern MONTH = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private final Database db = new Database(Path.of("data"));
    private final CardLayout rootLayout = new CardLayout();
    private final Panel root = new Panel(rootLayout);
    private User user;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new EpmsApp().setVisible(true));
    }

    public EpmsApp() {
        super("Employee Payroll Management System");
        setSize(1080, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setBackground(Ui.BG);
        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); System.exit(0); }
        });
        showLogin();
    }

    private void show(Panel p, String name) {
        root.removeAll();
        root.add(p, name);
        rootLayout.show(root, name);
        root.validate();
        validate();
        repaint();
    }

    // ============================================================ Module 1: login
    private void showLogin() {
        user = null;
        Panel outer = Ui.panel(new GridBagLayout());
        Panel card = new Panel(new GridBagLayout());
        card.setBackground(Ui.WHITE);
        Label title = Ui.label("Employee Payroll", Ui.TITLE, Ui.NAVY);
        Label title2 = Ui.label("Management System", Ui.TITLE, Ui.NAVY);
        TextField username = Ui.field(22), password = Ui.field(22);
        password.setEchoChar('*');
        Label error = Ui.label(" ", Ui.BODY, Ui.RED);
        ActionListener doLogin = e -> {
            User u = Auth.login(db, username.getText(), password.getText());
            if (u == null) { error.setText("Invalid username or password."); password.setText(""); return; }
            user = u;
            if (Auth.HR.equals(u.role())) show(hrPanel(), "hr"); else show(employeePanel(), "emp");
        };
        username.addActionListener(doLogin);
        password.addActionListener(doLogin);
        Button signIn = Ui.button("Sign in", Ui.ACCENT, doLogin);

        GridBagConstraints c = Ui.gbc(0, 0);
        c.insets = new Insets(18, 40, 0, 40); c.anchor = GridBagConstraints.CENTER; card.add(title, c);
        c = Ui.gbc(0, 1); c.insets = new Insets(0, 40, 14, 40); c.anchor = GridBagConstraints.CENTER; card.add(title2, c);
        c = Ui.gbc(0, 2); c.insets = new Insets(4, 40, 0, 40); card.add(Ui.label("Username", Ui.BODY, Ui.MUTE), c);
        c = Ui.gbc(0, 3); c.insets = new Insets(0, 40, 8, 40); card.add(username, c);
        c = Ui.gbc(0, 4); c.insets = new Insets(4, 40, 0, 40); card.add(Ui.label("Password", Ui.BODY, Ui.MUTE), c);
        c = Ui.gbc(0, 5); c.insets = new Insets(0, 40, 8, 40); card.add(password, c);
        c = Ui.gbc(0, 6); c.insets = new Insets(0, 40, 0, 40); card.add(error, c);
        c = Ui.gbc(0, 7); c.insets = new Insets(4, 40, 10, 40); card.add(signIn, c);
        c = Ui.gbc(0, 8); c.insets = new Insets(0, 40, 0, 40); card.add(Ui.label("Demo HR:  admin / admin123", Ui.BODY, Ui.MUTE), c);
        c = Ui.gbc(0, 9); c.insets = new Insets(0, 40, 22, 40); card.add(Ui.label("Demo employee:  e001 / emp123", Ui.BODY, Ui.MUTE), c);
        outer.add(card);
        show(outer, "login");
        username.requestFocus();
    }

    private Panel header(String heading, Panel nav) {
        Panel h = new Panel(new BorderLayout(10, 0));
        h.setBackground(Ui.NAVY);
        h.add(Ui.label("  💼  " + heading, Ui.HEAD, Ui.WHITE), BorderLayout.WEST);
        if (nav != null) h.add(nav, BorderLayout.CENTER);
        Panel east = new Panel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        east.setBackground(Ui.NAVY);
        east.add(Ui.label(user.username() + "  ·  " + user.role(), Ui.BODY, Ui.WHITE));
        east.add(Ui.button("Sign out", Ui.BG, e -> showLogin()));
        h.add(east, BorderLayout.EAST);
        return h;
    }

    // =================================================================== HR shell
    private Panel hrPanel() {
        Panel page = Ui.panel(new BorderLayout());
        CardLayout cards = new CardLayout();
        Panel content = Ui.panel(cards);
        Panel employees = employeesView(), payroll = payrollView(), reports = reportsView();
        content.add(employees, "emp");
        content.add(payroll, "pay");
        content.add(reports, "rep");

        Panel nav = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        nav.setBackground(Ui.NAVY);
        nav.add(Ui.button("Employees", Ui.ACCENT, e -> { refreshEmployees(); cards.show(content, "emp"); }));
        nav.add(Ui.button("Run Payroll", Ui.ACCENT, e -> { loadPayroll(); cards.show(content, "pay"); }));
        nav.add(Ui.button("Reports", Ui.ACCENT, e -> { showReport(); cards.show(content, "rep"); }));
        page.add(header("EPMS - HR Administration", nav), BorderLayout.NORTH);
        page.add(content, BorderLayout.CENTER);
        refreshEmployees();
        return page;
    }

    // ===================================================== Module 2: employee table
    private final java.awt.List empList = new java.awt.List(18, false);
    private final ArrayList<Integer> empIds = new ArrayList<>();
    private static final String EMP_FMT = "%-6s %-18s %-12s %11s %10s %10s %6s %6s";

    private Panel employeesView() {
        Panel p = Ui.panel(new BorderLayout(0, 6));
        p.add(Ui.label("  Employees", Ui.TITLE, Ui.NAVY), BorderLayout.NORTH);
        Panel mid = Ui.panel(new BorderLayout());
        Label cols = Ui.label(" " + String.format(EMP_FMT, "Code", "Name", "Dept", "Basic", "HRA", "Allow.", "Tax%", "PF%"), Ui.MONO_B, Ui.MUTE);
        mid.add(cols, BorderLayout.NORTH);
        empList.setFont(Ui.MONO);
        empList.setBackground(Ui.WHITE);
        mid.add(empList, BorderLayout.CENTER);
        Panel wrap = Ui.panel(new BorderLayout());
        wrap.add(mid, BorderLayout.CENTER);
        p.add(Ui.panel(new BorderLayout()), BorderLayout.WEST);
        p.add(wrap, BorderLayout.CENTER);
        Panel south = Ui.panel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        south.add(Ui.button("+ Add employee", Ui.GREEN, e -> { if (employeeDialog(null)) refreshEmployees(); }));
        south.add(Ui.button("Edit selected", Ui.ACCENT, e -> {
            Full f = selectedEmployee();
            if (f != null && employeeDialog(f)) refreshEmployees();
        }));
        south.add(Ui.button("Delete selected", Ui.RED, e -> {
            Full f = selectedEmployee();
            if (f != null && Ui.confirm(this, "Delete " + f.e().name() + " and all payroll history?")) {
                db.deleteEmployee(f.e().id());
                refreshEmployees();
            }
        }));
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private Full selectedEmployee() {
        int i = empList.getSelectedIndex();
        if (i < 0) { Ui.message(this, "Select an employee", "Please select an employee in the list first.", false); return null; }
        return db.full(empIds.get(i));
    }

    private void refreshEmployees() {
        empList.removeAll();
        empIds.clear();
        for (Full f : db.allFull()) {
            Employee e = f.e();
            empList.add(String.format(EMP_FMT, e.code(), cut(e.name(), 18), cut(e.dept(), 12), Ui.money(f.s().basic()),
                    Ui.money(f.s().hra()), Ui.money(f.s().allowances()), Ui.num(f.d().taxPct()), Ui.num(f.d().pfPct())));
            empIds.add(e.id());
        }
    }
    private static String cut(String s, int n) { return s.length() <= n ? s : s.substring(0, n - 1) + "…"; }

    /** Add / edit dialog (GridBagLayout form). Returns true if saved. */
    private boolean employeeDialog(Full ex) {
        boolean[] saved = {false};
        Dialog d = new Dialog(this, ex == null ? "Add employee" : "Edit employee", true);
        d.setBackground(Ui.BG);
        d.setFont(Ui.BODY);
        d.setLayout(new BorderLayout(0, 8));
        String[] labels = {"Employee code", "Full name", "Email", "Department", "Designation", "Join date (yyyy-mm-dd)",
                "Basic salary", "HRA", "Allowances", "Income tax % of gross", "Provident fund % of basic", "Other deductions"};
        TextField[] f = new TextField[labels.length];
        Panel form = Ui.panel(new GridBagLayout());
        for (int i = 0; i < labels.length; i++) {
            f[i] = Ui.field(18);
            int col = (i % 2) * 2, row = i / 2;
            form.add(Ui.label(labels[i], Ui.BODY, Ui.MUTE), Ui.gbc(col, row));
            form.add(f[i], Ui.gbc(col + 1, row));
        }
        TextField pw = Ui.field(18);
        pw.setEchoChar('*');
        if (ex == null) {
            form.add(Ui.label("Login password", Ui.BODY, Ui.MUTE), Ui.gbc(0, 6));
            form.add(pw, Ui.gbc(1, 6));
            form.add(Ui.label("(blank = emp123)", Ui.BODY, Ui.MUTE), Ui.gbc(2, 6));
            for (int i : new int[]{7, 8, 9, 11}) f[i].setText("0");
            f[10].setText("12");
        } else {
            Employee e = ex.e();
            String[] v = {e.code(), e.name(), e.email(), e.dept(), e.designation(), e.joinDate(), Ui.num(ex.s().basic()),
                    Ui.num(ex.s().hra()), Ui.num(ex.s().allowances()), Ui.num(ex.d().taxPct()), Ui.num(ex.d().pfPct()), Ui.num(ex.d().other())};
            for (int i = 0; i < v.length; i++) f[i].setText(v[i]);
        }
        Label err = Ui.label(" ", Ui.BODY, Ui.RED);
        Panel south = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        south.add(err);
        south.add(Ui.button("Save", Ui.GREEN, a -> {
            try {
                String code = f[0].getText().trim().toUpperCase(), name = f[1].getText().trim(), dept = f[3].getText().trim();
                double basic = parse(f[6], "Basic salary", 0, 1e9), hra = parse(f[7], "HRA", 0, 1e9),
                        allow = parse(f[8], "Allowances", 0, 1e9), tax = parse(f[9], "Tax %", 0, 60),
                        pf = parse(f[10], "PF %", 0, 30), other = parse(f[11], "Other deductions", 0, 1e9);
                if (code.isEmpty() || name.isEmpty() || dept.isEmpty() || basic <= 0)
                    throw new IllegalArgumentException("Code, name, department and basic salary are required.");
                Employee e = new Employee(0, code, name, f[2].getText().trim(), dept, f[4].getText().trim(), f[5].getText().trim());
                if (ex == null) db.addEmployee(e, new Salary(0, basic, hra, allow), new Deduction(0, tax, pf, other),
                        pw.getText().isEmpty() ? "emp123" : pw.getText());
                else db.updateEmployee(ex.e().id(), e, new Salary(0, basic, hra, allow), new Deduction(0, tax, pf, other));
                saved[0] = true;
                d.dispose();
            } catch (IllegalArgumentException x) {
                err.setText(x.getMessage());
                d.pack();
            }
        }));
        south.add(Ui.button("Cancel", Ui.BG, a -> d.dispose()));
        d.add(form, BorderLayout.CENTER);
        d.add(south, BorderLayout.SOUTH);
        Ui.show(d, this);
        return saved[0];
    }

    private static double parse(TextField t, String name, double lo, double hi) {
        try {
            String s = t.getText().trim();
            double v = s.isEmpty() ? 0 : Double.parseDouble(s);
            if (v < lo || v > hi) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a number between " + Ui.num(lo) + " and " + Ui.num(hi) + ".");
        }
    }

    // ================================================== Module 3: salary calculation
    private final TextField payMonth = Ui.field(8);
    private final Panel payRows = new Panel(new GridLayout(0, 6, 10, 6));
    private final Map<Integer, TextField> bonusFields = new HashMap<>();
    private final Label payStatus = Ui.label(" ", Ui.BODY, Ui.GREEN);

    private Panel payrollView() {
        Panel p = Ui.panel(new BorderLayout(0, 6));
        Panel top = Ui.panel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.add(Ui.label("Run payroll   Month (yyyy-MM):", Ui.HEAD, Ui.NAVY));
        payMonth.setText(YearMonth.now().toString());
        top.add(payMonth);
        top.add(Ui.button("Load", Ui.BG, e -> loadPayroll()));
        p.add(top, BorderLayout.NORTH);
        payRows.setBackground(Ui.BG);
        Panel wrapper = Ui.panel(new BorderLayout());
        wrapper.add(payRows, BorderLayout.NORTH);
        ScrollPane sp = new ScrollPane();
        sp.add(wrapper);
        p.add(sp, BorderLayout.CENTER);
        Panel south = Ui.panel(new GridLayout(2, 1));
        Panel actions = Ui.panel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        actions.add(Ui.button("Process payroll", Ui.GREEN, e -> processPayroll()));
        actions.add(Ui.label("gross = basic + HRA + allowances + bonus;  net = gross - tax - PF - other", Ui.BODY, Ui.MUTE));
        Panel status = Ui.panel(new BorderLayout());
        status.add(payStatus, BorderLayout.CENTER);
        south.add(actions);
        south.add(status);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private String validMonth(TextField t) {
        String m = t.getText().trim();
        if (!MONTH.matcher(m).matches()) { Ui.message(this, "Invalid month", "Month must look like 2026-09.", true); return null; }
        return m;
    }

    private void loadPayroll() {
        String m = payMonth.getText().trim();
        if (!MONTH.matcher(m).matches()) m = YearMonth.now().toString();
        payMonth.setText(m);
        payRows.removeAll();
        bonusFields.clear();
        for (String h : new String[]{"Code", "Name", "Department", "Bonus", "Gross", "Net"})
            payRows.add(Ui.label(h, Ui.MONO_B, Ui.MUTE));
        for (Full f : db.allFull()) {
            Employee e = f.e();
            Payroll p = db.payroll(e.id(), m);
            TextField bonus = Ui.field(8);
            if (p != null && p.bonus() > 0) bonus.setText(Ui.num(p.bonus()));
            bonusFields.put(e.id(), bonus);
            payRows.add(Ui.label(e.code(), Ui.BODY, Color.BLACK));
            payRows.add(Ui.label(e.name(), Ui.BODY, Color.BLACK));
            payRows.add(Ui.label(e.dept(), Ui.BODY, Color.BLACK));
            payRows.add(bonus);
            payRows.add(Ui.label(p == null ? "—" : Ui.money(p.gross()), Ui.BODY, Color.BLACK));
            payRows.add(Ui.label(p == null ? "—" : Ui.money(p.net()), Ui.BODY, Ui.GREEN));
        }
        payRows.getParent().validate();
        payRows.validate();
        payStatus.setText(" ");
    }

    private void processPayroll() {
        String m = validMonth(payMonth);
        if (m == null) return;
        Map<Integer, Double> bonuses = new HashMap<>();
        try {
            for (var en : bonusFields.entrySet())
                if (!en.getValue().getText().isBlank()) bonuses.put(en.getKey(), parse(en.getValue(), "Bonus", 0, 1e9));
        } catch (IllegalArgumentException x) { Ui.message(this, "Invalid bonus", x.getMessage(), true); return; }
        int n = SalaryCalculator.runPayroll(db, m, bonuses);
        loadPayroll();
        payStatus.setText("Payroll for " + m + " processed for " + n + " employees.");
    }

    // ================================================= Module 4: report generation
    private final TextField repMonth = Ui.field(8);
    private final TextArea repText = Ui.textArea(20, 90);
    private final Choice repEmp = new Choice();
    private final ArrayList<Integer> repEmpIds = new ArrayList<>();

    private Panel reportsView() {
        Panel p = Ui.panel(new BorderLayout(0, 6));
        Panel top = Ui.panel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.add(Ui.label("Reports   Month (yyyy-MM):", Ui.HEAD, Ui.NAVY));
        repMonth.setText(YearMonth.now().toString());
        top.add(repMonth);
        top.add(Ui.button("Show", Ui.ACCENT, e -> showReport()));
        top.add(Ui.button("Export PDF", Ui.GREEN, e -> {
            String m = validMonth(repMonth);
            if (m != null) save(ReportGenerator.departmentPdf(db, m), "payroll_" + m + ".pdf");
        }));
        top.add(Ui.button("Export Excel", Ui.GREEN, e -> {
            String m = validMonth(repMonth);
            if (m != null) save(ReportGenerator.departmentXlsx(db, m), "payroll_" + m + ".xlsx");
        }));
        p.add(top, BorderLayout.NORTH);
        p.add(repText, BorderLayout.CENTER);
        Panel south = Ui.panel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        south.add(Ui.label("Individual payslip:", Ui.BODY, Color.BLACK));
        repEmp.setFont(Ui.BODY);
        south.add(repEmp);
        south.add(Ui.button("View payslip", Ui.ACCENT, e -> {
            String m = validMonth(repMonth);
            if (m == null || repEmp.getItemCount() == 0) return;
            openPayslip(repEmpIds.get(repEmp.getSelectedIndex()), m);
        }));
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private void showReport() {
        String m = repMonth.getText().trim();
        if (!MONTH.matcher(m).matches()) { m = YearMonth.now().toString(); repMonth.setText(m); }
        repEmp.removeAll();
        repEmpIds.clear();
        for (Full f : db.allFull()) { repEmp.add(f.e().code() + " - " + f.e().name()); repEmpIds.add(f.e().id()); }
        var rows = ReportGenerator.departmentReport(db, m);
        StringBuilder b = new StringBuilder("Department-wise payroll report - " + m + "\n\n");
        if (rows.isEmpty()) {
            b.append("No payroll has been processed for ").append(m).append(".\nUse 'Run Payroll' first.\n");
        } else {
            String fmt = "%-14s %5s %13s %12s %12s %9s %13s%n";
            b.append(String.format(fmt, "Department", "Emps", "Gross", "Tax", "PF", "Other", "Net pay"));
            b.append("-".repeat(84)).append('\n');
            for (Object[] r : rows) b.append(row(fmt, r));
            b.append("-".repeat(84)).append('\n').append(row(fmt, ReportGenerator.totals(rows)));
            b.append("\nIndividual net pay\n").append("-".repeat(44)).append('\n');
            for (Payroll p : db.payrollForMonth(m)) {
                Employee e = db.employee(p.empId());
                if (e != null) b.append(String.format("%-6s %-20s %14s%n", e.code(), cut(e.name(), 20), Ui.money(p.net())));
            }
        }
        repText.setText(b.toString());
    }
    private static String row(String fmt, Object[] r) {
        return String.format(fmt, r[0], r[1], Ui.money((double) r[2]), Ui.money((double) r[3]), Ui.money((double) r[4]),
                Ui.money((double) r[5]), Ui.money((double) r[6]));
    }

    /** Payslip window: text view plus PDF download. Enforces role-based access. */
    private void openPayslip(int empId, String month) {
        if (!Auth.canViewEmployee(user, empId)) { Ui.message(this, "Access denied", "You can only view your own payslips.", true); return; }
        Employee e = db.employee(empId);
        Payroll p = db.payroll(empId, month);
        if (e == null || p == null) { Ui.message(this, "Not found", "No payslip for " + month + ".", false); return; }
        Dialog d = new Dialog(this, "Payslip " + month, true);
        d.setBackground(Ui.BG);
        d.setLayout(new BorderLayout(0, 8));
        TextArea t = Ui.textArea(22, 50);
        t.setText(ReportGenerator.payslipText(e, p));
        d.add(t, BorderLayout.CENTER);
        Panel south = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        south.add(Ui.button("Save as PDF", Ui.GREEN, a -> save(ReportGenerator.payslipPdf(e, p), "payslip_" + e.code() + "_" + month + ".pdf")));
        south.add(Ui.button("Close", Ui.BG, a -> d.dispose()));
        d.add(south, BorderLayout.SOUTH);
        Ui.show(d, this);
    }

    private void save(byte[] data, String suggested) {
        FileDialog fd = new FileDialog(this, "Save file", FileDialog.SAVE);
        fd.setFile(suggested);
        fd.setVisible(true);
        if (fd.getFile() == null) return;
        try {
            Files.write(Path.of(fd.getDirectory(), fd.getFile()), data);
            Ui.message(this, "Saved", "Saved " + fd.getFile(), false);
        } catch (IOException ex) {
            Ui.message(this, "Save failed", ex.getMessage(), true);
        }
    }

    // ======================================================= employee self-service
    private Panel employeePanel() {
        Employee me = db.employee(user.employeeId());
        Panel page = Ui.panel(new BorderLayout(0, 8));
        page.add(header("EPMS - My Payroll", null), BorderLayout.NORTH);
        if (me == null) {
            page.add(Ui.label("  Your employee record was not found.", Ui.BODY, Ui.RED), BorderLayout.CENTER);
            return page;
        }
        Panel body = Ui.panel(new BorderLayout(12, 8));
        body.add(Ui.label("  Welcome, " + me.name() + "   (" + me.designation() + ", " + me.dept() + ")", Ui.HEAD, Ui.NAVY), BorderLayout.NORTH);
        var slips = db.payrollForEmployee(me.id());           // own data only
        java.awt.List months = new java.awt.List(14, false);
        months.setFont(Ui.MONO);
        months.setBackground(Ui.WHITE);
        for (Payroll p : slips) months.add(p.month() + "   net " + Ui.money(p.net()));
        TextArea view = Ui.textArea(20, 50);
        view.setText(slips.isEmpty() ? "No payslips yet." : "Select a month on the left to see the payslip.");
        months.addItemListener(e -> {
            int i = months.getSelectedIndex();
            if (i >= 0) view.setText(ReportGenerator.payslipText(me, slips.get(i)));
        });
        Panel left = new Panel(new BorderLayout(0, 6)) {   // fixed-width side column
            @Override public Dimension getPreferredSize() { return new Dimension(260, super.getPreferredSize().height); }
        };
        left.setBackground(Ui.BG);
        left.add(Ui.label("My payslips", Ui.HEAD, Ui.NAVY), BorderLayout.NORTH);
        left.add(months, BorderLayout.CENTER);
        body.add(left, BorderLayout.WEST);
        body.add(view, BorderLayout.CENTER);
        Panel south = Ui.panel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        south.add(Ui.button("Save selected payslip as PDF", Ui.GREEN, e -> {
            int i = months.getSelectedIndex();
            if (i < 0) { Ui.message(this, "Select a month", "Choose a payslip first.", false); return; }
            Payroll p = slips.get(i);
            save(ReportGenerator.payslipPdf(me, p), "payslip_" + me.code() + "_" + p.month() + ".pdf");
        }));
        body.add(south, BorderLayout.SOUTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }
}
