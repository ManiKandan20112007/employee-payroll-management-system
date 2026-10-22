package epms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * MODULE 4 - Payroll Report Generation.
 *  - monthly payslip for an individual employee (web page + PDF)
 *  - consolidated department-wise payroll report for HR
 *  - export as PDF or Excel (.xlsx) for audit and record keeping
 * PDF and XLSX are written by hand, so no external libraries are needed.
 */
public final class ReportGenerator {
    private ReportGenerator() {}

    public static final String[] HEAD = {"Department", "Employees", "Gross", "Tax", "PF", "Other", "Net Pay"};

    /** One row per department: [dept, count, gross, tax, pf, other, net]. */
    public static List<Object[]> departmentReport(Database db, String month) {
        Map<String, double[]> acc = new TreeMap<>();
        for (Database.Payroll p : db.payrollForMonth(month)) {
            Database.Employee e = db.employee(p.empId());
            if (e == null) continue;
            double[] a = acc.computeIfAbsent(e.dept(), k -> new double[6]);
            a[0]++; a[1] += p.gross(); a[2] += p.tax(); a[3] += p.pf(); a[4] += p.other(); a[5] += p.net();
        }
        List<Object[]> rows = new ArrayList<>();
        acc.forEach((k, a) -> rows.add(new Object[]{k, (int) a[0], a[1], a[2], a[3], a[4], a[5]}));
        return rows;
    }

    public static Object[] totals(List<Object[]> rows) {
        Object[] t = {"TOTAL", 0, 0.0, 0.0, 0.0, 0.0, 0.0};
        for (Object[] r : rows) {
            t[1] = (int) t[1] + (int) r[1];
            for (int i = 2; i < 7; i++) t[i] = (double) t[i] + (double) r[i];
        }
        return t;
    }

    /** Plain-text payslip for on-screen display. */
    public static String payslipText(Database.Employee e, Database.Payroll p) {
        String bar = "-".repeat(44) + "\n";
        StringBuilder b = new StringBuilder();
        b.append("PAYSLIP - ").append(p.month()).append("\n").append(bar);
        b.append(e.name()).append(" (").append(e.code()).append(")\n");
        b.append(e.designation()).append(", ").append(e.dept()).append("\n").append(bar);
        b.append(String.format("%-24s %18s%n", "EARNINGS", "Rs."));
        b.append(String.format("%-24s %18s%n", "Basic", money(p.basic())));
        b.append(String.format("%-24s %18s%n", "HRA", money(p.hra())));
        b.append(String.format("%-24s %18s%n", "Allowances", money(p.allowances())));
        b.append(String.format("%-24s %18s%n", "Bonus", money(p.bonus())));
        b.append(String.format("%-24s %18s%n", "GROSS PAY", money(p.gross()))).append(bar);
        b.append(String.format("%-24s %18s%n", "DEDUCTIONS", "Rs."));
        b.append(String.format("%-24s %18s%n", "Income tax", money(p.tax())));
        b.append(String.format("%-24s %18s%n", "Provident fund", money(p.pf())));
        b.append(String.format("%-24s %18s%n", "Other", money(p.other())));
        b.append(String.format("%-24s %18s%n", "TOTAL DEDUCTIONS", money(p.totalDeductions()))).append(bar);
        b.append(String.format("%-24s %18s%n", "NET PAY", money(p.net())));
        return b.toString();
    }

    private static String money(double v) { return String.format(Locale.US, "%,.2f", v); }

    // ---------------------------------------------------------------- PDF
    public static byte[] payslipPdf(Database.Employee e, Database.Payroll p) {
        SimplePdf pdf = new SimplePdf();
        pdf.add("PAYSLIP - " + p.month(), 20, true);
        pdf.add("", 12, false);
        pdf.add(e.name() + " (" + e.code() + ")", 13, true);
        pdf.add(e.designation() + ", " + e.dept(), 12, false);
        pdf.add("", 12, false);
        pdf.add(String.format("%-22s %14s", "EARNINGS", "AMOUNT (Rs.)"), 12, true);
        pdf.add(String.format("%-22s %14s", "Basic", money(p.basic())), 12, false);
        pdf.add(String.format("%-22s %14s", "HRA", money(p.hra())), 12, false);
        pdf.add(String.format("%-22s %14s", "Allowances", money(p.allowances())), 12, false);
        pdf.add(String.format("%-22s %14s", "Bonus", money(p.bonus())), 12, false);
        pdf.add(String.format("%-22s %14s", "GROSS PAY", money(p.gross())), 12, true);
        pdf.add("", 12, false);
        pdf.add(String.format("%-22s %14s", "DEDUCTIONS", "AMOUNT (Rs.)"), 12, true);
        pdf.add(String.format("%-22s %14s", "Income tax", money(p.tax())), 12, false);
        pdf.add(String.format("%-22s %14s", "Provident fund", money(p.pf())), 12, false);
        pdf.add(String.format("%-22s %14s", "Other", money(p.other())), 12, false);
        pdf.add(String.format("%-22s %14s", "TOTAL DEDUCTIONS", money(p.totalDeductions())), 12, true);
        pdf.add("", 12, false);
        pdf.add("NET PAY: Rs. " + money(p.net()), 16, true);
        return pdf.build(false);
    }

    public static byte[] departmentPdf(Database db, String month) {
        SimplePdf pdf = new SimplePdf();
        pdf.add("Department-wise Payroll Report - " + month, 18, true);
        pdf.add("", 12, false);
        String fmt = "%-14s %5s %12s %11s %11s %9s %12s";
        pdf.add(String.format(fmt, "Department", "Emps", "Gross", "Tax", "PF", "Other", "Net Pay"), 10, true);
        List<Object[]> rows = departmentReport(db, month);
        for (Object[] r : rows) pdf.add(line(fmt, r), 10, false);
        if (!rows.isEmpty()) pdf.add(line(fmt, totals(rows)), 10, true);
        else pdf.add("No payroll processed for this month.", 10, false);
        return pdf.build(true);
    }

    private static String line(String fmt, Object[] r) {
        return String.format(fmt, r[0], r[1], money((double) r[2]), money((double) r[3]),
                money((double) r[4]), money((double) r[5]), money((double) r[6]));
    }

    /** Minimal text-only PDF writer (Courier / Courier-Bold, A4, multi-page). */
    private static final class SimplePdf {
        record Line(String text, int size, boolean bold) {}
        private final List<Line> lines = new ArrayList<>();
        void add(String t, int size, boolean bold) { lines.add(new Line(t, size, bold)); }

        byte[] build(boolean landscape) {
            int w = landscape ? 842 : 595, h = landscape ? 595 : 842;
            List<StringBuilder> pages = new ArrayList<>();
            StringBuilder cur = null; double y = 0;
            for (Line l : lines) {
                double lh = l.size() * 1.5;
                if (cur == null || y - lh < 40) { cur = new StringBuilder(); pages.add(cur); y = h - 50; }
                cur.append("BT /").append(l.bold() ? "F2" : "F1").append(' ').append(l.size()).append(" Tf 40 ")
                   .append(String.format(Locale.US, "%.1f", y)).append(" Td (").append(esc(l.text())).append(") Tj ET\n");
                y -= lh;
            }
            if (pages.isEmpty()) pages.add(new StringBuilder());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            List<Integer> offsets = new ArrayList<>();
            write(out, "%PDF-1.4\n");
            int n = pages.size();                  // objects: 1 catalog, 2 pages, 3-4 fonts, then page/content pairs
            StringBuilder kids = new StringBuilder();
            for (int i = 0; i < n; i++) kids.append(5 + 2 * i).append(" 0 R ");
            obj(out, offsets, "<< /Type /Catalog /Pages 2 0 R >>");
            obj(out, offsets, "<< /Type /Pages /Kids [" + kids + "] /Count " + n + " >>");
            obj(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>");
            obj(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Courier-Bold >>");
            for (int i = 0; i < n; i++) {
                int content = 6 + 2 * i;
                obj(out, offsets, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + w + " " + h + "] /Contents " + content
                        + " 0 R /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> >>");
                String s = pages.get(i).toString();
                obj(out, offsets, "<< /Length " + s.getBytes(StandardCharsets.ISO_8859_1).length + " >>\nstream\n" + s + "endstream");
            }
            int xref = out.size();
            write(out, "xref\n0 " + (offsets.size() + 1) + "\n0000000000 65535 f \n");
            for (int off : offsets) write(out, String.format("%010d 00000 n \n", off));
            write(out, "trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
            return out.toByteArray();
        }
        private static void obj(ByteArrayOutputStream out, List<Integer> offsets, String body) {
            offsets.add(out.size());
            write(out, offsets.size() + " 0 obj\n" + body + "\nendobj\n");
        }
        private static void write(ByteArrayOutputStream out, String s) {
            out.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
        }
        private static String esc(String s) {
            StringBuilder b = new StringBuilder();
            for (char c : s.toCharArray()) {
                if (c == '(' || c == ')' || c == '\\') b.append('\\').append(c);
                else b.append(c < 256 && c >= 32 ? c : '?');
            }
            return b.toString();
        }
    }

    // -------------------------------------------------------------- Excel
    public static byte[] departmentXlsx(Database db, String month) {
        List<Object[]> rows = new ArrayList<>(departmentReport(db, month));
        if (!rows.isEmpty()) rows.add(totals(rows));
        StringBuilder sheet = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        sheet.append("<row r=\"1\">");
        for (int c = 0; c < HEAD.length; c++) sheet.append(strCell(c, 1, HEAD[c]));
        sheet.append("</row>");
        int r = 2;
        for (Object[] row : rows) {
            sheet.append("<row r=\"").append(r).append("\">");
            for (int c = 0; c < row.length; c++) {
                sheet.append(row[c] instanceof String s ? strCell(c, r, s)
                        : "<c r=\"" + (char) ('A' + c) + r + "\"><v>" + ((Number) row[c]).doubleValue() + "</v></c>");
            }
            sheet.append("</row>");
            r++;
        }
        sheet.append("</sheetData></worksheet>");
        String ct = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>";
        String rels = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>";
        String wb = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"" + xml(month)
                + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";
        String wbRels = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(bos)) {
            put(z, "[Content_Types].xml", ct); put(z, "_rels/.rels", rels); put(z, "xl/workbook.xml", wb);
            put(z, "xl/_rels/workbook.xml.rels", wbRels); put(z, "xl/worksheets/sheet1.xml", sheet.toString());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return bos.toByteArray();
    }
    private static String strCell(int c, int r, String s) {
        return "<c r=\"" + (char) ('A' + c) + r + "\" t=\"inlineStr\"><is><t>" + xml(s) + "</t></is></c>";
    }
    private static String xml(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    private static void put(ZipOutputStream z, String name, String body) throws IOException {
        z.putNextEntry(new ZipEntry(name));
        z.write(body.getBytes(StandardCharsets.UTF_8));
        z.closeEntry();
    }
}
