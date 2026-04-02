import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Quick database connection test.
 * Run this BEFORE Main.java to confirm MySQL is working.
 *
 * Compile & run:
 *   javac -cp lib/mysql-connector-j-*.jar TestDB.java
 *   java  -cp .:lib/mysql-connector-j-*.jar TestDB
 */
public class TestDB {

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/sports_tracker",
                "root",
                "SHIVANG123@k"
            );
            System.out.println("Connected to Database ✅");
            System.out.println("Catalog: " + conn.getCatalog());
            conn.close();
        } catch (Exception e) {
            System.err.println("Connection FAILED ❌");
            e.printStackTrace();
        }
    }
}