import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Quick SQLite connection test — no external database server required.
 * Run this to confirm SQLite is working before starting Main.java.
 *
 * Override the database file path via environment variable (optional):
 *   export DB_URL=jdbc:sqlite:/path/to/sports_tracker.db
 *
 * Compile & run:
 *   javac -cp lib/sqlite-jdbc-*.jar TestDB.java
 *   java  -cp .:lib/sqlite-jdbc-*.jar TestDB
 */
public class TestDB {

    public static void main(String[] args) {
        String url = System.getenv("DB_URL") != null
                ? System.getenv("DB_URL") : "jdbc:sqlite:sports_tracker.db";
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(url);
            System.out.println("Connected to SQLite ✅  (" + url + ")");
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT sqlite_version()")) {
                if (rs.next()) System.out.println("SQLite version: " + rs.getString(1));
            }
            conn.close();
        } catch (Exception e) {
            System.err.println("Connection FAILED ❌");
            e.printStackTrace();
        }
    }
}