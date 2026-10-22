package epms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;

/**
 * MODULE 2 - Employee & Salary Database.
 * Creates / loads the employee, salary, deductions, users and payroll tables
 * (with employee_id relationships and cascading delete), seeds demo data on
 * first run, and reports connection/storage failures clearly.
 * Tables are stored as tab-separated files in ./data so no database server
 * is needed (see README for swapping this class for JDBC + MySQL).
 */
public final class Database {
    public record Employee(int id, String code, String name, String email, String dept, String designation, String joinDate) {}
    public record Salary(int empId, double basic, double hra, double allowances) {}
    public record Deduction(int empId, double taxPct, double pfPct, double other) {}
    public record User(int id, String username, String hash, String role, int employeeId) {}
    public record Payroll(int empId, String month, double basic, double hra, double allowances, double bonus,
                          double gross, double tax, double pf, double other, double totalDeductions, double net) {}
    public record Full(Employee e, Salary s, Deduction d) {}

    /** One persisted table: rows <-> objects, saved atomically after every change. */
    private static final class Table<T> {
        final Path file; final Function<String[], T> parse; final Function<T, String[]> write;
        final List<T> rows = new ArrayList<>();
        Table(Path dir, String name, Function<String[], T> parse, Function<T, String[]> write) {
            this.file = dir.resolve(name + ".tsv"); this.parse = parse; this.write = write;
            try {
                if (Files.exists(file)) {
                    for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                        if (!line.isEmpty()) rows.add(parse.apply(split(line)));
                    }
                }
            } catch (IOException | RuntimeException ex) {
                throw new IllegalStateException("Cannot read table " + file + ": " + ex.getMessage(), ex);
            }
        }
        void save() {
            StringBuilder sb = new StringBuilder();
            for (T r : rows) sb.append(join(write.apply(r))).append('\n');
            try {
                Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
                Files.writeString(tmp, sb, StandardCharsets.UTF_8);
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                throw new IllegalStateException("Database write failed: " + ex.getMessage(), ex);
            }
        }
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
    }
    private static String unesc(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                b.append(n == 't' ? '\t' : n == 'n' ? '\n' : n == 'r' ? '\r' : n);
            } else b.append(c);
        }
        return b.toString();
    }
    private static String join(String[] f) {
        StringJoiner j = new StringJoiner("\t");
        for (String x : f) j.add(esc(x));
        return j.toString();
    }
    private static String[] split(String line) {
        String[] p = line.split("\t", -1);
        for (int i = 0; i < p.length; i++) p[i] = unesc(p[i]);
        return p;
    }
    private static String s(double d) { return Double.toString(d); }
    private static String s(int d) { return Integer.toString(d); }

    private final Table<Employee> employees;
    private final Table<Salary> salary;
    private final Table<Deduction> deductions;
    private final Table<User> users;
    private final Table<Payroll> payroll;

    public Database(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Database initialisation failed (" + dir + "): " + e.getMessage(), e);
        }
        employees = new Table<>(dir, "employees",
                f -> new Employee(Integer.parseInt(f[0]), f[1], f[2], f[3], f[4], f[5], f[6]),
                e -> new String[]{s(e.id()), e.code(), e.name(), e.email(), e.dept(), e.designation(), e.joinDate()});
        salary = new Table<>(dir, "salary",
                f -> new Salary(Integer.parseInt(f[0]), Double.parseDouble(f[1]), Double.parseDouble(f[2]), Double.parseDouble(f[3])),
                x -> new String[]{s(x.empId()), s(x.basic()), s(x.hra()), s(x.allowances())});
        deductions = new Table<>(dir, "deductions",
                f -> new Deduction(Integer.parseInt(f[0]), Double.parseDouble(f[1]), Double.parseDouble(f[2]), Double.parseDouble(f[3])),
                x -> new String[]{s(x.empId()), s(x.taxPct()), s(x.pfPct()), s(x.other())});
        users = new Table<>(dir, "users",
                f -> new User(Integer.parseInt(f[0]), f[1], f[2], f[3], Integer.parseInt(f[4])),
                u -> new String[]{s(u.id()), u.username(), u.hash(), u.role(), s(u.employeeId())});
        payroll = new Table<>(dir, "payroll",
                f -> new Payroll(Integer.parseInt(f[0]), f[1], d(f[2]), d(f[3]), d(f[4]), d(f[5]), d(f[6]), d(f[7]), d(f[8]), d(f[9]), d(f[10]), d(f[11])),
                p -> new String[]{s(p.empId()), p.month(), s(p.basic()), s(p.hra()), s(p.allowances()), s(p.bonus()), s(p.gross()),
                        s(p.tax()), s(p.pf()), s(p.other()), s(p.totalDeductions()), s(p.net())});
        if (users.rows.isEmpty()) seed();
    }
    private static double d(String x) { return Double.parseDouble(x); }

    private void seed() {
        users.rows.add(new User(1, "admin", Auth.hashPassword("admin123"), Auth.HR, 0));
        Object[][] demo = {
            {"E001", "Aarav Sharma", "aarav@example.com", "Engineering", "Software Engineer", "2022-06-01", 50000, 20000, 5000, 10, 12, 200},
            {"E002", "Diya Nair", "diya@example.com", "Engineering", "Senior Engineer", "2020-01-15", 70000, 28000, 8000, 15, 12, 200},
            {"E003", "Rohan Iyer", "rohan@example.com", "Finance", "Accountant", "2021-03-10", 40000, 16000, 4000, 8, 12, 150},
            {"E004", "Meera Pillai", "meera@example.com", "HR", "HR Executive", "2023-02-20", 35000, 14000, 3500, 5, 12, 100},
            {"E005", "Karthik Raj", "karthik@example.com", "Sales", "Sales Manager", "2019-09-05", 60000, 24000, 6000, 12, 12, 250}};
        for (Object[] r : demo) {
            Employee e = new Employee(0, (String) r[0], (String) r[1], (String) r[2], (String) r[3], (String) r[4], (String) r[5]);
            addEmployee(e, new Salary(0, num(r[6]), num(r[7]), num(r[8])),
                    new Deduction(0, num(r[9]), num(r[10]), num(r[11])), "emp123");
        }
    }
    private static double num(Object o) { return ((Number) o).doubleValue(); }

    // ---- employees ---------------------------------------------------------
    public synchronized List<Full> allFull() {
        List<Full> out = new ArrayList<>();
        for (Employee e : employees.rows) out.add(full(e.id()));
        out.sort(Comparator.comparing(f -> f.e().code()));
        return out;
    }
    public synchronized Full full(int id) {
        Employee e = employees.rows.stream().filter(x -> x.id() == id).findFirst().orElse(null);
        if (e == null) return null;
        return new Full(e,
                salary.rows.stream().filter(x -> x.empId() == id).findFirst().orElse(new Salary(id, 0, 0, 0)),
                deductions.rows.stream().filter(x -> x.empId() == id).findFirst().orElse(new Deduction(id, 0, 12, 0)));
    }
    public synchronized Employee employee(int id) { Full f = full(id); return f == null ? null : f.e(); }

    /** @throws IllegalArgumentException if the employee code already exists */
    public synchronized int addEmployee(Employee e, Salary s, Deduction d, String password) {
        if (employees.rows.stream().anyMatch(x -> x.code().equalsIgnoreCase(e.code())))
            throw new IllegalArgumentException("Employee code already exists.");
        int id = employees.rows.stream().mapToInt(Employee::id).max().orElse(0) + 1;
        employees.rows.add(new Employee(id, e.code(), e.name(), e.email(), e.dept(), e.designation(), e.joinDate()));
        salary.rows.add(new Salary(id, s.basic(), s.hra(), s.allowances()));
        deductions.rows.add(new Deduction(id, d.taxPct(), d.pfPct(), d.other()));
        int uid = users.rows.stream().mapToInt(User::id).max().orElse(0) + 1;
        users.rows.add(new User(uid, e.code().toLowerCase(), Auth.hashPassword(password), Auth.EMPLOYEE, id));
        saveAll();
        return id;
    }
    public synchronized void updateEmployee(int id, Employee e, Salary s, Deduction d) {
        if (employees.rows.stream().anyMatch(x -> x.id() != id && x.code().equalsIgnoreCase(e.code())))
            throw new IllegalArgumentException("Employee code already exists.");
        employees.rows.replaceAll(x -> x.id() == id ? new Employee(id, e.code(), e.name(), e.email(), e.dept(), e.designation(), e.joinDate()) : x);
        salary.rows.replaceAll(x -> x.empId() == id ? new Salary(id, s.basic(), s.hra(), s.allowances()) : x);
        deductions.rows.replaceAll(x -> x.empId() == id ? new Deduction(id, d.taxPct(), d.pfPct(), d.other()) : x);
        saveAll();
    }
    /** Cascading delete: salary, deductions, login and payroll history go with the employee. */
    public synchronized void deleteEmployee(int id) {
        employees.rows.removeIf(x -> x.id() == id);
        salary.rows.removeIf(x -> x.empId() == id);
        deductions.rows.removeIf(x -> x.empId() == id);
        users.rows.removeIf(x -> x.employeeId() == id);
        payroll.rows.removeIf(x -> x.empId() == id);
        saveAll();
    }

    // ---- users -------------------------------------------------------------
    public synchronized User findUser(String username) {
        return users.rows.stream().filter(u -> u.username().equalsIgnoreCase(username)).findFirst().orElse(null);
    }

    // ---- payroll -----------------------------------------------------------
    public synchronized void savePayroll(List<Payroll> list) {
        for (Payroll p : list) {
            payroll.rows.removeIf(x -> x.empId() == p.empId() && x.month().equals(p.month())); // re-run = update
            payroll.rows.add(p);
        }
        payroll.save();
    }
    public synchronized Payroll payroll(int empId, String month) {
        return payroll.rows.stream().filter(p -> p.empId() == empId && p.month().equals(month)).findFirst().orElse(null);
    }
    public synchronized List<Payroll> payrollForMonth(String month) {
        return payroll.rows.stream().filter(p -> p.month().equals(month)).toList();
    }
    public synchronized List<Payroll> payrollForEmployee(int empId) {
        return payroll.rows.stream().filter(p -> p.empId() == empId)
                .sorted(Comparator.comparing(Payroll::month).reversed()).toList();
    }
    public synchronized Set<String> months() {
        return new TreeSet<>(payroll.rows.stream().map(Payroll::month).toList());
    }
    public synchronized int employeeCount() { return employees.rows.size(); }

    private void saveAll() {
        employees.save(); salary.save(); deductions.save(); users.save(); payroll.save();
    }
}
