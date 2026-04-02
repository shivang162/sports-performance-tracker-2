import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Quick database connection test.
 * Run this BEFORE Main.java to confirm MySQL is working.
 *
 * Set credentials via environment variables before running:
 *   export DB_URL=jdbc:mysql://localhost:3306/sports_tracker
 *   export DB_USER=root
 *   export DB_PASS=your_password
 *
 * Compile & run:
 *   javac -cp lib/mysql-connector-j-*.jar TestDB.java
 *   java  -cp .:lib/mysql-connector-j-*.jar TestDB
 */
public class TestDB {

    public static void main(String[] args) {
        String url  = System.getenv("DB_URL")  != null ? System.getenv("DB_URL")  : "jdbc:mysql://localhost:3306/sports_tracker";
        String user = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root";
        String pass = System.getenv("DB_PASS") != null ? System.getenv("DB_PASS") : "";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, user, pass);
            System.out.println("Connected to Database ✅");
            System.out.println("Catalog: " + conn.getCatalog());
            conn.close();
        } catch (Exception e) {
            System.err.println("Connection FAILED ❌");
            e.printStackTrace();
        }
    }
}