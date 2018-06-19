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
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
	@Value("${wiki.span.count:2}")
	private Integer spanCount;
	@Value("${wiki.dryRun:false}")
	private Boolean dryRun;
	
	private Connection conn = null;

	public void removeSpan(Integer id) {
		logger.debug("wiki.span.count : {}", spanCount);
		logger.debug("wiki.dryRun : {}", dryRun);
		
		Statement stmt = null;
		ResultSet rs = null;
		
		String query = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			
		    // CLOB column에 대한 lock을 얻는다.
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
			
			logger.debug("Query string : [{}]", query);

			rs = stmt.executeQuery(query);
			
		    int contentId = 0;
		    String content = null;
			while (rs.next()) {
				contentId = rs.getInt("CONTENTID");
				content = clobToString((CLOB) rs.getClob("BODY"));

				logger.debug("Before : contentId=[{}], body=[{}]", contentId, content);

				//content = unwrap(Jsoup.parse(content));
				content = unwrap(Jsoup.parse(content, "", Parser.xmlParser()));
				
				if (!dryRun) {
					// db update
					updateBody(contentId, content); 
				}
	    		
	    			logger.debug("After : contentId=[{}], body=[{}]", contentId, content);
			}
		} catch (Exception e) {
			logger.error("Unhandled exception occurred while get contents and update.", e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) { /* Nothing to do */ }
			}
			
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) { /* Nothing to do */ }
			}
			
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) { /* Nothing to do */ }
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
			
//			CLOB lob_loc = null;

//		    ResultSet rset = stmt.executeQuery("SELECT BODY FROM BODYCONTENT WHERE CONTENTID = " + contentId + " FOR UPDATE");
		    
//			if (rset.next()) {
//				lob_loc = ((OracleResultSet) rset).getCLOB(1);
//			}
			
			// Open the LOB for READWRITE:
//			OracleCallableStatement cstmt = (OracleCallableStatement) conn.prepareCall("BEGIN DBMS_LOB.OPEN(?, DBMS_LOB.LOB_READWRITE); END;");
//			cstmt.setCLOB(1, lob_loc);
//			cstmt.execute();
			
			// Erase the LOB
//			OracleCallableStatement cstmt = (OracleCallableStatement) conn.prepareCall("BEGIN DBMS_LOB.TRIM(?, 0); END;");
//			cstmt.setCLOB(1, lob_loc);
//			cstmt.execute();
			
			// Update the LOB
//			cstmt = (OracleCallableStatement) conn.prepareCall("BEGIN DBMS_LOB.WRITE(?, ?, 1, ?); END;");
//			cstmt.setCLOB(1, lob_loc);
//			cstmt.setInt(2, content.length());
//			cstmt.setString(3, content);
//			cstmt.execute();

			int count = content.length() / 2000;
			
			if ((content.length() % 2000) > 0) {
				count += 1;
			}
			
			String contentStr = null;

			if (count > 1) {
				for (int i = 1; i <= count; i++) {
					if (i == 1) {
						contentStr = "to_clob('" + content.substring(0, i * 2000) + "')";
					} else if (i == count) {
						contentStr += " || to_clob('" + content.substring(((i - 1) * 2000)) + "')";
					} else {
						contentStr += " || to_clob('" + content.substring(((i - 1) * 2000), i * 2000) + "')";
					}
				}
			} else {
				contentStr = "to_clob('" + content + "')";
			}
			
			String sql = "update BODYCONTENT set BODY = " + contentStr + " where CONTENTID = " + contentId;
			logger.debug(sql);
			stmt.executeUpdate(sql);
			
			// Close the LOB:
//			cstmt = (OracleCallableStatement) conn.prepareCall("BEGIN DBMS_LOB.CLOSE(?); END;");
//			cstmt.setCLOB(1, lob_loc);
//			cstmt.execute();

			stmt.close();
			
			conn.commit();
			conn.setAutoCommit(true);
		} catch (Exception e) {
			logger.error("Unhandled exception occurred while update contents to DB.", e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) { /* Nothing to do */ }
			}
			
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) { /* Nothing to do */ }
			}
		}
	}

	/**
	 * <pre>
	 * Remove <span lang="ko"/> and <span class="*" /> except <span style="*"/>
	 * </pre>
	 * @param element
	 * @return
	 */
	private String unwrap(Element element) {
        /*
        Elements elements = element.select("span[lang=ko], span[class]").not("[style]");
        
        for (int i = 0; i < elements.size(); i++) {
            elements.get(i).unwrap();
        }
        /*/
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
        //*/
        
        return element.html();
    }
	
	/**
	 * <pre>
	 * Check if element's child has span tag
	 * </pre>
	 * @param element
	 * @return
	 */
	private boolean hasChildSpan(Element element) {
        try {
            if (element.childNodeSize() == 1 && 
                    element.html().indexOf("<span") > -1 && 
                    (element.child(0).hasAttr("lang") || element.child(0).hasAttr("class")) && 
                    !element.child(0).hasAttr("style")) {
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
		logger.debug("JDBC Driver : {}", driver);
		logger.debug("JDBC Url : {}", url);
		logger.debug("JDBC Username : {}", username);
		logger.debug("JDBC Password : {}", password);
		
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
//end of RemoveSpanService.java