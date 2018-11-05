package main;

import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.lowagie.text.Cell;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Table;
import com.lowagie.text.rtf.RtfWriter2;
import com.lowagie.text.rtf.style.RtfParagraphStyle;
/**
 * 数据库文档生成
 */
public class OracleDoc {
	private static Map<String,String> keyType = new HashMap<String,String>();
	private static RtfParagraphStyle font_title = RtfParagraphStyle.STYLE_HEADING_1;
	private static float exec_time = 3f;
	static{
		try {
			keyType.put("P", "主键");
			keyType.put("C", "检查");
			keyType.put("U", "唯一");
			Class.forName("oracle.jdbc.OracleDriver");
			Properties prop = new Properties();  
            prop.load(new FileInputStream(System.getProperty("user.dir")+"/config/config.properties"));  
            root=prop.getProperty("root");  
            url=prop.getProperty("url");  
            username=prop.getProperty("username");  
            password=prop.getProperty("password");  
            owners=prop.getProperty("owner");  
            file_name=prop.getProperty("file_name");  
        } catch(Exception e) {  
            e.printStackTrace();  
        }
	}
	private static String root;
	private static String url;
	private static String username; 
	private static String password; 
	private static String owner; 
	private static String owners; 
	private static String file_name;
	
	private static String sql_get_user_count = "SELECT count(*) FROM all_tables "
		+"WHERE owner='{user_name}'";
	private static String sql_get_all_tables = "select a.TABLE_NAME,b.COMMENTS "
		+"FROM all_tables a,all_tab_comments b WHERE a.TABLE_NAME=b.TABLE_NAME "
		+"AND a.OWNER=b.OWNER AND a.OWNER='{user_name}' order by TABLE_NAME";	
	private static String sql_get_all_columns = "select T1.column_name,"
		+"T1.data_type,T1.data_length,t2.comments,T1.NULLABLE,(select "
		+"max(constraint_type) FROM all_constraints x left join all_cons_columns y "
		+"on (x.constraint_name=y.constraint_name AND x.OWNER=y.OWNER) "
		+"WHERE x.table_name=t1.TABLE_NAME and y.COLUMN_NAME=T1.column_name "
		+"AND x.OWNER='{user_name}') FROM all_tab_cols t1, all_col_comments t2, "
		+"all_tab_comments t3  where t1.TABLE_NAME=t2.table_name(+) AND "
		+"t1.COLUMN_NAME=t2.column_name(+)  and t1.TABLE_NAME=t3.table_name(+) "
		+"AND t1.owner=t2.OWNER(+) AND t1.owner=t3.OWNER(+) and t1.TABLE_NAME"
		+"='{table_name}' AND t1.owner='{user_name}' ORDER by T1.COLUMN_ID";
	private static String sql_get_table_count = "select count(*) from {table_name}";
	
	public void create() throws Exception {
		Connection conn = getConnection();
		String[] o_arr = owners.split(",");
		boolean to_many = o_arr.length>1?true:false;
		for(String user:o_arr){
			if(user!=null && !user.trim().equals("")){
				owner=user;
				long start = System.currentTimeMillis();
				//查询开始
				//查看用户下有几张表
				Long table_count = SqlUtils.queryForLong(sql_get_user_count.replace("{user_name}", owner),conn);
				DecimalFormat df = new DecimalFormat("0.00");
				System.out.println("用户"+owner+"共有"+table_count+"张表,预计耗时"
					+df.format(exec_time*table_count/60)+"分钟,"
					+getEndTime(exec_time*table_count)+"结束");
				//获取所有表
				List tables = SqlUtils.queryForList(sql_get_all_tables.replace("{user_name}", owner),conn);
				//初始化word文档
				Document document = new Document(PageSize.A4); 
				if(to_many || file_name==null || file_name.trim().equals("")){
					file_name = owner+"("+table_count+"张表).doc";
				}
				RtfWriter2.getInstance(document,new FileOutputStream(root+file_name));  
				document.open();
				int i=1;
				for (Iterator iterator = tables.iterator(); iterator.hasNext();) {
					long b1 = System.currentTimeMillis();
					String [] arr = (String []) iterator.next();
					System.out.print(i+"/"+table_count+".正在处理数据表------"+owner+"."+arr[0]);
					Long data_count = SqlUtils.queryForLong(sql_get_table_count.replace("{table_name}", owner+"."+arr[0]),conn);
					System.out.print("("+data_count);
					addTableMetaData(document,arr,i,data_count);
					List columns = SqlUtils.queryForList(sql_get_all_columns
						.replace("{table_name}", arr[0])
						.replace("{user_name}", owner)
						.replace("{user_name}", owner),conn);
					addTableDetail(document,columns);
					DocUtils.addBlank(document);
					i++;
					long b2 = System.currentTimeMillis();
					System.out.println("/"+(b2-b1)+"ms)...done");
				}
				document.close();  
				if(i>1){
					long end = System.currentTimeMillis();
					System.out.println("执行"+owner+"(共"+table_count+"张表)消耗时间"
						+((end-start)/1000)+"秒,平均时间"
						+((end-start)/(i-1))+"毫秒/张");
				}
			}
		}
		conn.close();
	}
	
	private String getEndTime(Float m) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.SECOND,m.intValue());
		return new SimpleDateFormat("hh点mm分ss秒").format(cal.getTime());
	}
	
	/**
	 * 添加包含字段详细信息的表格
	 */
	public static void addTableDetail(Document document,List columns)
		throws Exception{
		float[] title_len = {6F, 20F, 17F, 8F, 11F, 30F, 8F};
		Table table = new Table(title_len.length);  
		table.setWidth(100f);
	    table.setBorderWidth(1);  
	    table.setBorderColor(Color.BLACK);  
	    table.setPadding(0);  
	    table.setSpacing(0);  
	    Cell cell1 = new Cell("序号");// 单元格
	    cell1.setHeader(true);  
	    
	    Cell cell2 = new Cell("列名");// 单元格
	    cell2.setHeader(true); 
	    
	    Cell cell3 = new Cell("类型");// 单元格
	    cell3.setHeader(true); 
	    
	    Cell cell4 = new Cell("长度");// 单元格
	    cell4.setHeader(true); 
	    
	    Cell cell5 = new Cell("约束");// 单元格
	    cell5.setHeader(true); 
	    
	    Cell cell6 = new Cell("说明");// 单元格
	    cell6.setHeader(true);
	    
	    Cell cell7 = new Cell("备注");// 个人备注
	    cell7.setHeader(true);
	    //设置表头格式
	    table.setWidths(title_len);
	    cell1.setHorizontalAlignment(Cell.ALIGN_CENTER);
	    cell1.setBackgroundColor(Color.gray);
	    cell2.setHorizontalAlignment(Cell.ALIGN_CENTER);
	    cell2.setBackgroundColor(Color.gray);
	    cell3.setHorizontalAlignment(Cell.ALIGN_CENTER);
	    cell3.setBackgroundColor(Color.gray);
	    cell4.setHorizontalAlignment(Cell.ALIGN_CENTER);
	    cell4.setBackgroundColor(Color.gray);
	    cell5.setHorizontalAlignment(Cell.ALIGN_CENTER);
	    cell5.setBackgroundColor(Color.gray);
	    cell6.setHorizontalAlignment(Cell.ALIGN_CENTER);
	    cell6.setBackgroundColor(Color.gray);
	    cell7.setHorizontalAlignment(Cell.ALIGN_CENTER);
	    cell7.setBackgroundColor(Color.gray);
	    table.addCell(cell1);  
	    table.addCell(cell2);  
	    table.addCell(cell3);  
	    table.addCell(cell4);  
	    table.addCell(cell5);
	    table.addCell(cell6);
	    table.addCell(cell7);
	    table.endHeaders();// 表头结束
	    int x = 1;
	    for (Iterator iterator = columns.iterator(); iterator.hasNext();) {
			String [] arr2 = (String []) iterator.next();
			Cell c1 = new Cell(x+"");
			Cell c2 = new Cell(arr2[0]);
			Cell c3 = new Cell(arr2[1]);
			Cell c4 = new Cell(arr2[2]);
			
			String key = keyType.get(arr2[5]);
			if(key==null){
				key = "";
			}
			if(arr2[4]!=null && arr2[4].equals("N")){
				if(!key.equals("")){
					key+=",";
				}
				key+="非空";
			}
			Cell c5 = new Cell(key);
			Cell c6 = new Cell(arr2[3]);
			Cell c7 = new Cell();
			c1.setHorizontalAlignment(Cell.ALIGN_CENTER);
			c2.setHorizontalAlignment(Cell.ALIGN_CENTER);
			c3.setHorizontalAlignment(Cell.ALIGN_CENTER);
			c4.setHorizontalAlignment(Cell.ALIGN_CENTER);
			c5.setHorizontalAlignment(Cell.ALIGN_CENTER);
			c6.setHorizontalAlignment(Cell.ALIGN_CENTER);
			c7.setHorizontalAlignment(Cell.ALIGN_CENTER);
			table.addCell(c1);
			table.addCell(c2);
			table.addCell(c3);
			table.addCell(c4);
			table.addCell(c5);
			table.addCell(c6);
			table.addCell(c7);
			x++;
		}
	    document.add(table);
	}

	/**
	 * 增加表概要信息
	 */
	public static void addTableMetaData(Document dcument,String [] arr,int i,Long count) throws Exception{
		Paragraph ph1 = new Paragraph(i+". "+arr[0]+" "+(arr[1]==null?"":arr[1])+" "+count);
		ph1.setFont(font_title);
		dcument.add(ph1);
	}
	/**
	 * 获取数据库连接
	 */
	public static Connection getConnection(){
		try {
			return DriverManager.getConnection(url, username, password);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args) throws Exception {	
		new OracleDoc().create();
	}
}