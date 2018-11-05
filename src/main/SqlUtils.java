package main;

import java.io.IOException;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class SqlUtils {
	public static String queryForString(String sql, Connection conn) {
		Statement stmt = null;
		ResultSet rs = null;
		List<String[]> list = new ArrayList<String[]>();
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				String[] arr = new String[rs.getMetaData().getColumnCount()];
				for (int i = 0; i < arr.length; i++) {
						if(rs.getMetaData().getColumnType(i+1)== Types.CLOB){
							arr[i] = clobToString(rs.getClob(i + 1));
						}else{
							arr[i] = rs.getString(i + 1);
						}
				}
				list.add(arr);
			}
		} catch (Exception e) {
			list.add(new String[1]);
			e.printStackTrace();
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return list.get(0)[0];
	}

	public static Long queryForLong(String sql, Connection conn) {
		String str = queryForString(sql, conn);
		if(str==null){
			return -1l;
		}
		return Long.parseLong(str);
	}

	public static List<String[]> queryForList(String sql, Connection conn) {
		Statement stmt = null;
		ResultSet rs = null;
		List<String[]> list = new ArrayList<String[]>();
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				String[] arr = new String[rs.getMetaData().getColumnCount()];
				for (int i = 0; i < arr.length; i++) {
					if(rs.getMetaData().getColumnType(i+1)== Types.CLOB){
						arr[i] = clobToString(rs.getClob(i + 1));
					}else{
						arr[i] = rs.getString(i + 1);
					}
				}
				list.add(arr);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return list;
	}
	
	
	/**
     * 将"Clob"型数据转换成"String"型数据
     * 需要捕获"SQLException","IOException"
     * prama:    colb1 将被转换的"Clob"型数据
     * return:    返回转好的字符串
     * @throws SQLException 
     * @throws IOException */
    public static String clobToString(Clob colb) throws SQLException, IOException
    {
        String outfile = "";
        if(colb != null){
            java.io.Reader is = colb.getCharacterStream();
            java.io.BufferedReader br = new java.io.BufferedReader(is);          
            String s = br.readLine();
            while (s != null) {
                outfile += s;
                s = br.readLine();
            }
            is.close();
            br.close();   
        }
        return  outfile;       
    }
	
}
