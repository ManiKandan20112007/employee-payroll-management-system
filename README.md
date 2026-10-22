# Employee Payroll Management System - AWT desktop app
Pure java.awt (no Swing, no libraries) - JDK 17+.

IntelliJ: File > Open this folder, mark `src` as Sources Root, run `epms.EpmsApp`.
Terminal: Windows `run.bat` | Mac/Linux `./run.sh`

Logins: HR `admin / admin123`; employees `e001`..`e005` / `emp123`
Data is saved in ./data (created on first run).

src/epms/EpmsApp.java          AWT window: login, HR screens, employee screen
src/epms/Ui.java               AWT helpers: colours, fonts, buttons, dialogs
src/epms/Auth.java             Module 1 - login, SHA-256 hashing, roles
src/epms/Database.java         Module 2 - employee / salary / deductions / payroll tables
src/epms/SalaryCalculator.java Module 3 - gross, tax, PF, net pay
src/epms/ReportGenerator.java  Module 4 - payslips, department report, PDF & Excel export
