package com.spy.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConnectionPool {
	Log log = LogFactory.getLog(ConnectionPool.class);
	static ConnectionPool pool = null;
	private static int iMaxSession = 20;

	static public ConnectionPool getInstance() {
		if (null == pool) {
			try {
				pool = new ConnectionPool();
			} catch (Exception e) {
				e.printStackTrace();
				pool = null;
			}
		}
		return pool;
	}

	static String driver = null;
	static String connurl = null;
	static String username = null;
	static String password = null;
	static Stack connections = new Stack();

	protected ConnectionPool()
			throws ClassNotFoundException, SQLException, IOException, IllegalAccessException, InstantiationException {
		Properties props = new Properties();
		try {
			String os_name = System.getProperty("os.name").toLowerCase();
			if (os_name.indexOf("win") != -1) {
				String path = ConnectionPool.class.getClassLoader().getResource("DBConnection.properties").getPath();
				path = path.replace("%20", " ");
				props.load(new FileInputStream(path));
			} else {
				props.load(getClass().getResourceAsStream("/DBConnection.properties"));
			}
		} catch (Exception ex) {
			log.error("Had a problem loaction DBConnection.properties");
			ex.printStackTrace();
		} finally {
		}
		driver = props.getProperty("driver");
		connurl = props.getProperty("connurl");
		username = props.getProperty("username");
		password = props.getProperty("password");
		log.info("driver:" + driver + "  connurl:" + connurl + "  username:" + username + "  password:" + password);
		DriverManager.registerDriver((Driver) Class.forName(driver).newInstance());
	}

	synchronized public Connection createConnection() throws SQLException {

		Connection conn;
		if (connections.empty() == false) {
			conn = (Connection) connections.pop();

			if (conn == null || conn.isClosed()) {
				conn = DriverManager.getConnection(connurl, username, password);
				conn.setAutoCommit(false);
			}
		} else {
			conn = DriverManager.getConnection(connurl, username, password);
			conn.setAutoCommit(false);
		}
		return conn;
	}

	synchronized public static Connection getConnection() {
		Connection conn = null;
		try {
			if (!connections.isEmpty() && connections.size() > 0) {
				conn = (Connection) connections.pop();
			} else {
				ConnectionPool connpool = ConnectionPool.getInstance();
				conn = connpool.createConnection();
			}
			if (conn == null || conn.isClosed()) {
				conn = DriverManager.getConnection(connurl, username, password);
				conn.setAutoCommit(false);
			}
		} catch (Exception e) {
			// System.out.println("conn = DriverManager.getConnection(connurl,
			// username, password);��ʼ��error");
			e.printStackTrace();
		}
		return conn;
	}

	synchronized public static void releaseConnection(Connection conn) {
		if (conn != null) {
			if (connections.size() >= iMaxSession) {
				try {
					conn.close();
					conn = null;
				} catch (SQLException e) {
					System.out.println("Close DBConnection:");
					e.printStackTrace();
				}
			} else {
				try {
					if (conn != null && !conn.isClosed()) {
						connections.push(conn);
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
