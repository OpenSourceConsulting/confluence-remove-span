package query;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;


public class QueryTest {

	public QueryTest() {
		// TODO Auto-generated constructor stub
	}

	private static Connection conn = null;

	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		// TODO Auto-generated method stub

		Statement stmt = null;
		ResultSet rs = null;

		String query = null;
		Class.forName("oracle.jdbc.OracleDriver");
		conn = DriverManager.getConnection("	jdbc:oracle:thin:@192.168.0.155:1521:oscOra12", "sec_wiki", "sec_wiki");
		stmt = conn.createStatement();

		// Update 대상 Row를 질의한다.
		query = "select BODYCONTENTID, BODY, CONTENTID, BODYTYPEID from BODYCONTENT WHERE dbms_lob.instr(BODY, '";
		query += "') > 0";

		rs = stmt.executeQuery(query);


		ArrayList<String> contents = new ArrayList<String>();

		while (rs.next()) {
			System.out.println(rs.getClob("BODY"));
		}

	}
}
