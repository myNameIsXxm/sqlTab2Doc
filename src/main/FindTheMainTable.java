package main;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.rtf.RtfWriter2;

public class FindTheMainTable {
	static {
		try {
			Class.forName("oracle.jdbc.OracleDriver");
			Properties prop = new Properties();
			prop.load(new FileInputStream(System.getProperty("user.dir") + "/config/config.properties"));
			root = prop.getProperty("root");
			url = prop.getProperty("url");
			username = prop.getProperty("username");
			password = prop.getProperty("password");
			owners = prop.getProperty("owner");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private static String root;
	private static String url;
	private static String username;
	private static String password;
	private static String owners;

	private static String sql_get_all_tables = "select a.TABLE_NAME,b.COMMENTS "
			+ "FROM all_tables a,all_tab_comments b WHERE a.TABLE_NAME=b.TABLE_NAME "
			+ "AND a.OWNER=b.OWNER AND a.OWNER='{user_name}' order by TABLE_NAME";
	private static String sql_get_table_count = "select count(*) from {table_name}";

	public void create() throws Exception {
		Connection conn = getConnection();
		String[] o_arr = owners.split(",");
		for (String user : o_arr) {
			System.out.println("开始计算"+user);
			if (user != null && !user.trim().equals("")) {
				List tables = SqlUtils.queryForList(sql_get_all_tables.replace("{user_name}", user), conn);
				Map<String, Long> map = new HashMap<String, Long>();
				for (Iterator iterator = tables.iterator(); iterator.hasNext();) {
					String[] arr = (String[]) iterator.next();
					String sql = sql_get_table_count.replace("{table_name}", user + "." + arr[0]);
					Long data_count = SqlUtils.queryForLong(sql, conn);
					String tablename = (arr[1] == null || arr[1].equals("")) ? arr[0] : arr[0] + " " + arr[1];
					map.put(tablename, data_count);
				}
				List<Map.Entry<String, Long>> list = new ArrayList<Map.Entry<String, Long>>(map.entrySet());
				Collections.sort(list, new Comparator<Map.Entry<String, Long>>() {
					public int compare(Entry<String, Long> o1, Entry<String, Long> o2) {
						return (int) (o2.getValue() - o1.getValue());
					}
				});
				Document document = new Document(PageSize.A4);
				RtfWriter2.getInstance(document, new FileOutputStream(root + user + "-COUNT.doc"));
				document.open();
				for (Map.Entry<String, Long> m : list) {
					DocUtils.addLine(document, CommonUtils.long2String(m.getValue()) + "\t\t" + m.getKey());
				}
				document.close();
			}
		}
		conn.close();
	}

	/**
	 * 获取数据库连接
	 */
	public static Connection getConnection() {
		try {
			return DriverManager.getConnection(url, username, password);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) throws Exception {
		new FindTheMainTable().create();
	}
}