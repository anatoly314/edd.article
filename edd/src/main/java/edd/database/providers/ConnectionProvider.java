package edd.database.providers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionProvider {
	public static Connection getConnection() throws SQLException{
		String connectionString = "";
		Connection con = DriverManager.getConnection(connectionString);
		return con;
	}
}
