package epms;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * MODULE 1 - Authentication & Role Management.
 *  - secure login for HR administrators and employees
 *  - SHA-256 password hashing (random salt per user)
 *  - role-based access control: employees see only their own payroll data
 */
public final class Auth {
    public static final String HR = "HR", EMPLOYEE = "EMPLOYEE";
    private static final SecureRandom RNG = new SecureRandom();

    private Auth() {}

    public static String token(int bytes) {
        byte[] b = new byte[bytes];
        RNG.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    public static String hashPassword(String password) {
        return hashPassword(password, token(8));
    }

    private static String hashPassword(String password, String salt) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest((salt + password).getBytes(StandardCharsets.UTF_8));
            return salt + "$" + HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean verifyPassword(String password, String stored) {
        String salt = stored.substring(0, stored.indexOf('$'));
        return MessageDigest.isEqual(hashPassword(password, salt).getBytes(StandardCharsets.UTF_8),
                stored.getBytes(StandardCharsets.UTF_8));
    }

    /** Returns the user if the credentials are valid, otherwise null. */
    public static Database.User login(Database db, String username, String password) {
        Database.User u = db.findUser(username.trim());
        return u != null && verifyPassword(password, u.hash()) ? u : null;
    }

    /** HR may view anybody; an employee only their own record. */
    public static boolean canViewEmployee(Database.User u, int employeeId) {
        return u != null && (HR.equals(u.role()) || u.employeeId() == employeeId);
    }
}
