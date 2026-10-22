# Employee Payroll Management System (EPMS)

A Java desktop application, built with AWT, that automates payroll for an organisation. It calculates employee salaries, deductions and bonuses, and generates payroll reports. HR administrators manage everything, and employees can see only their own payslips.

Mini project for **CS8392 – Object Oriented Programming**, Department of CSE, K. Ramakrishnan College of Engineering (Autonomous), Samayapuram, Trichy.

## Features
- Secure login for HR administrators and employees
- Passwords hashed with salted SHA-256
- Role-based access control: employees can view only their own payroll data
- Add, edit and delete employees with salary and deduction details
- Salary calculation: gross = basic + HRA + allowances + bonus, then tax and provident fund deducted to give net pay
- Monthly payslips and a department-wise payroll report
- Export reports to PDF and Excel (.xlsx)

## Modules
| Module | File | What it does |
|---|---|---|
| 1. Authentication & Role Management | `Auth.java` | Login, SHA-256 hashing, role checks |
| 2. Employee & Salary Database | `Database.java` | Employee, salary, deductions, users and payroll tables |
| 3. Salary Calculation | `SalaryCalculator.java` | Gross pay, tax, PF, net pay for each cycle |
| 4. Report Generation | `ReportGenerator.java` | Payslips, department reports, PDF and Excel export |
| User interface | `EpmsApp.java`, `Ui.java` | AWT windows and dialogs |

## Technologies
- Java 17 or newer (plain JDK, no external libraries)
- `java.awt` for the GUI
- Data stored in local files (a `data/` folder is created on first run)

## How to run
**IntelliJ IDEA:** open the project folder, mark `src` as *Sources Root*, then run `epms.EpmsApp`.

**Terminal:**
```
# Windows
run.bat
# Mac / Linux
./run.sh
```

## Demo logins
| Role | Username | Password |
|---|---|---|
| HR | `admin` | `admin123` |
| Employee | `e001` to `e005` | `emp123` |

## SDG mapping
- SDG 8 – Decent Work and Economic Growth
- SDG 9 – Industry, Innovation and Infrastructure

## References
- Herbert Schildt, *Java: The Complete Reference*, 12th Edition, McGraw-Hill, 2021
- Kathy Sierra, Bert Bates, *Head First Java*, 2nd Edition, O'Reilly, 2020
- Oracle, Java documentation – https://docs.oracle.com/javase/tutorial/
