/* 
 * Copyright 2018 OpenSourceConsulting.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Revision History
 * Author			Date				Description
 * ---------------	----------------	------------
 * Sang-cheon Park	Jun 11, 2018		First Draft.
 */
package com.osci.confluence.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import oracle.sql.CLOB;

/**
 * <pre>
 * 
 * </pre>
 * 
 * @author Sang-cheon Park
 * @version 1.0
 */
@Component
public class RemoveSpanService {

	private static final Logger logger = LoggerFactory.getLogger(RemoveSpanService.class);

	@Value("${jdbc.driver}")
	private String driver;
	@Value("${jdbc.url}")
	private String url;
	@Value("${jdbc.username}")
	private String username;
	@Value("${jdbc.password}")
	private String password;
	@Value("${wiki.span.count:3}")
	private Integer spanCount;
	@Value("${wiki.dryRun:false}")
	private Boolean dryRun;

	private Connection conn = null;

	public void removeSpan(Integer id) {
		logger.debug("wiki.span.count : {}", spanCount);

		Statement stmt = null;
		ResultSet rs = null;

		String query = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();

			// Update 대상 Row를 질의한다.
			if (id != null) {
				query = "select BODYCONTENTID, BODY, CONTENTID, BODYTYPEID from BODYCONTENT WHERE CONTENTID = '" + id + "'";
			} else {
				query = "select BODYCONTENTID, BODY, CONTENTID, BODYTYPEID from BODYCONTENT WHERE dbms_lob.instr(BODY, '";

				int cnt = 0;

				while (cnt++ < spanCount) {
					query += "<span lang=\"ko\">";
				}

				query += "') > 0";
			}

			rs = stmt.executeQuery(query);

			int contentId = 0;
			String content = null;

			ArrayList<String> contents = new ArrayList<String>();

			while (rs.next()) {
				contentId = rs.getInt("CONTENTID");
				content = clobToString((CLOB) rs.getClob("BODY"));

				contents.add("/pages/viewpage.action?pageId=" + contentId);

				content = unwrap(Jsoup.parse(content, "", Parser.xmlParser()));

				if (!dryRun) {
					updateBody(contentId, content);
				}
			}
			logger.debug(":+:+:+:+:+:+:+:+:+:+:+:+");
			logger.debug("Job Done!! Page Count is " + contents.size());
			logger.debug(":+:+:+:+:+:+:+:+:+:+:+:+");

			String fileName = "./pages.txt";
			try {
				File file = new File(fileName);
				FileWriter fw = new FileWriter(file, false);
				fw.write(String.join("\n", contents));
				fw.flush();
				fw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			logger.error("Unhandled exception occurred while get contents and update.", e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					/* Nothing to do */ }
			}

			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					/* Nothing to do */ }
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					/* Nothing to do */ }
			}
		}
	}

	/**
	 * update Body by contentId
	 * 
	 * @param contentId
	 * @param content
	 */
	private void updateBody(int contentId, String content) {
		Statement stmt = null;
		ResultSet rs = null;

		// 파라미터로 받아온 데이터로 Body를 업데이트 한다.
		try {
			conn = getConnection();
			conn.setAutoCommit(false);

			stmt = conn.createStatement();

			// *
			int count = content.length() / 1800;

			if ((content.length() % 1800) > 0) {
				count += 1;
			}

			String contentStr = null;

			if (count > 1) {
				for (int i = 1; i <= count; i++) {
					if (i == 1) {
						contentStr = "to_clob('{" + content.substring(0, i * 1800).replaceAll("'", "''") + "}')";
					} else if (i == count) {
						contentStr += " || to_clob('{" + content.substring(((i - 1) * 1800)).replaceAll("'", "''") + "}')";
					} else {
						contentStr += " || to_clob('{" + content.substring(((i - 1) * 1800), i * 1800).replaceAll("'", "''") + "}')";
					}
				}
			} else {
				contentStr = "to_clob('{" + content.replaceAll("'", "''") + "}')";
			}

			String sql = "update BODYCONTENT set BODY = " + contentStr + " where CONTENTID = " + contentId;

			String fileName = "./query.txt";
			try {
				File file = new File(fileName);
				FileWriter fw = new FileWriter(file, false);
				fw.write(sql);
				fw.flush();
				fw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			stmt.executeUpdate(sql);

			stmt.close();

			conn.commit();
			conn.setAutoCommit(true);
		} catch (Exception e) {
			logger.error("Unhandled exception occurred while update contents to DB.", e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					/* Nothing to do */ }
			}

			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					/* Nothing to do */ }
			}
		}
	}

	/**
	 * <pre>
	 * Remove <span lang="ko"/> and <span class="*" /> except <span style="*"/>
	 * </pre>
	 * 
	 * @param element
	 * @return
	 */
	private String unwrap(Element element) {
		/*
		 * Elements elements =
		 * element.select("span[lang=ko], span[class]").not("[style]");
		 * 
		 * for (int i = 0; i < elements.size(); i++) { elements.get(i).unwrap(); } /
		 */
		Elements elements = element.select("span[lang=ko], span[class]").not("[style]");

		int cnt = 1;
		for (Element e : elements) {
			cnt = 1;

			Element child = e;
			for (int i = 0; i < spanCount; i++) {
				if (hasChildSpan(child)) {
					cnt++;
					child = child.child(0);
				}
			}

			if (cnt >= spanCount) {
				e.unwrap();
			}
		}
		// */

		return element.html();
	}

	/**
	 * <pre>
	 * Check if element's child has span tag
	 * </pre>
	 * 
	 * @param element
	 * @return
	 */
	private boolean hasChildSpan(Element element) {
		try {
			if (element.childNodeSize() == 1 && element.html().indexOf("<span") > -1
			    && (element.child(0).hasAttr("lang") || element.child(0).hasAttr("class")) && !element.child(0).hasAttr("style")) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		return false;
	}

	/**
	 * <pre>
	 * Get string from CLOB
	 * </pre>
	 * 
	 * @param clob
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 */
	private String clobToString(CLOB clob) throws IOException, SQLException {
		if (clob == null) {
			return "";
		}

		StringBuffer strOut = new StringBuffer();
		String str = "";
		BufferedReader br = new BufferedReader(clob.getCharacterStream());

		while ((str = br.readLine()) != null) {
			strOut.append(str);
		}

		return strOut.toString();
	}

	/**
	 * @return
	 */
	private Connection getConnection() {

		try {
			if (conn == null || conn.isClosed()) {
				Class.forName(driver);
				conn = DriverManager.getConnection(url, username, password);

				logger.debug("DB Connected.");
			}
		} catch (Exception e) {
			logger.error("Unhandled exception occurred while get connection.", e);
		}

		return conn;
	}
}
// end of RemoveSpanService.java