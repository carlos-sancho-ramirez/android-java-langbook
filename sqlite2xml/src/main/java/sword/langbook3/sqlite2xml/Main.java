package sword.langbook3.sqlite2xml;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class Main {
    public static void main(String[] args) {
        System.out.println("Hello World");

        int count = 0;
        try {
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:sample.db")) {
                try (Statement statement = connection.createStatement()) {
                    statement.setQueryTimeout(60);
                    try (ResultSet resultSet = statement.executeQuery("SELECT id FROM Acceptations")) {
                        while (resultSet.next()) {
                            count++;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Found " + count + " acceptation within the database");
    }

    private Main() {
    }
}
