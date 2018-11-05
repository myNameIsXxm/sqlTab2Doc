package main;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.rtf.style.RtfParagraphStyle;

public class DocUtils {
	
	public static void addBlank(Document document) throws Exception {
		Paragraph ph = new Paragraph("");
		ph.setAlignment(Paragraph.ALIGN_LEFT);
		document.add(ph);
	}

	public static void addLine(Document document, String msg, Font font) throws Exception {
		Paragraph ph = new Paragraph(msg);
		ph.setAlignment(Paragraph.ALIGN_LEFT);
		//ph.setFont(font);
		document.add(ph);
	}
	
	public static void addLine(Document document, String msg) throws Exception {
		addLine(document,msg,RtfParagraphStyle.STYLE_NORMAL);
	}
}
