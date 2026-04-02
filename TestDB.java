import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Quick database connection test.
 * Run this BEFORE Main.java to confirm PostgreSQL is working.
 *
 * Set credentials via environment variables before running:
 *   export DB_URL=jdbc:postgresql://localhost:5432/sports_tracker
 *   export DB_USER=postgres
 *   export DB_PASS=your_password
 *
 * Compile & run:
 *   javac -cp lib/postgresql-*.jar TestDB.java
 *   java  -cp .:lib/postgresql-*.jar TestDB
 */
public class TestDB {

    public static void main(String[] args) {
        String url  = System.getenv("DB_URL")  != null ? System.getenv("DB_URL")  : "jdbc:postgresql://localhost:5432/sports_tracker";
        String user = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "postgres";
        String pass = System.getenv("DB_PASS") != null ? System.getenv("DB_PASS") : "";
        try {
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection(url, user, pass);
            System.out.println("Connected to Database ✅");
            System.out.println("Schema: " + conn.getSchema());
            conn.close();
        } catch (Exception e) {
            System.err.println("Connection FAILED ❌");
            e.printStackTrace();
        }
    }
}