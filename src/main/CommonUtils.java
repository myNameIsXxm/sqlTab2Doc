package main;

import java.text.DecimalFormat;

public class CommonUtils {
	public static String long2String(Long count){
		if(count>10000){
			return new DecimalFormat("0.00").format(count/10000.0)+"Íò";
		}
		return count+"";
	}
	
	
	public static void main(String[] args) {
		System.out.println(long2String(7143l));
	}
}
