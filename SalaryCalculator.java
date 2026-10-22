package epms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MODULE 3 - Salary Calculation.
 *   gross = basic + HRA + allowances + bonus
 *   tax   = gross x tax%          (income tax)
 *   pf    = basic x PF%           (provident fund)
 *   net   = gross - (tax + pf + other deductions)
 * Results are written to the payroll table each cycle; re-running a month updates it.
 */
public final class SalaryCalculator {
    private SalaryCalculator() {}

    private static double r2(double v) { return Math.round(v * 100.0) / 100.0; }

    public static Database.Payroll calculate(int empId, String month, Database.Salary s, Database.Deduction d, double bonus) {
        double gross = s.basic() + s.hra() + s.allowances() + bonus;
        double tax = gross * d.taxPct() / 100.0;
        double pf = s.basic() * d.pfPct() / 100.0;
        double totalDed = tax + pf + d.other();
        return new Database.Payroll(empId, month, r2(s.basic()), r2(s.hra()), r2(s.allowances()), r2(bonus),
                r2(gross), r2(tax), r2(pf), r2(d.other()), r2(totalDed), r2(gross - totalDed));
    }

    /** Process payroll for every employee for `month` (yyyy-MM); returns how many were processed. */
    public static int runPayroll(Database db, String month, Map<Integer, Double> bonuses) {
        List<Database.Payroll> out = new ArrayList<>();
        for (Database.Full f : db.allFull()) {
            out.add(calculate(f.e().id(), month, f.s(), f.d(), bonuses.getOrDefault(f.e().id(), 0.0)));
        }
        db.savePayroll(out);
        return out.size();
    }
}
