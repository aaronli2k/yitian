package apidemo;


import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import jxl.CellView;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.format.UnderlineStyle;
import jxl.read.biff.BiffException;
import jxl.write.Formula;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;


public class WriteExcel {

  private WritableCellFormat timesBoldUnderline;
  private WritableCellFormat times;
  private String inputFile;
private int rowPos;
  
public void setOutputFile(String inputFile) {
  this.inputFile = inputFile;
  }

  public void write() throws IOException, WriteException {
    File file = new File(inputFile);
    WorkbookSettings wbSettings = new WorkbookSettings();

    wbSettings.setLocale(new Locale("en", "EN"));

    WritableWorkbook workbook = Workbook.createWorkbook(file, wbSettings);
    workbook.createSheet("Report", 0);
    WritableSheet excelSheet = workbook.getSheet(0);
    createLabel(excelSheet);
    createContent(excelSheet);

    workbook.write();
    workbook.close();
  }

  public void write(ConcurrentHashMap<Long, forex> orderHashMap) throws RowsExceededException, WriteException{
	    File file = new File(inputFile);
	    forex orderDetail = new forex();
	    long dateTimeCode = 0, lastdateTimeCode = 0, lastSeqNo = 0;
	    
	   
	    WorkbookSettings wbSettings = new WorkbookSettings();

	    wbSettings.setLocale(new Locale("en", "EN"));

	    WritableWorkbook workbook = null;
		try {
			workbook = Workbook.createWorkbook(file, wbSettings);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		 DateFormat formatter; 			    	      
	   	 formatter = new SimpleDateFormat("yyyyMMdd");

    	// Get the date today using Calendar object.
    	// Using DateFormat format method we can create a string 
    	// representation of a date with the defined format.
    	
 	        String orderDateStr = formatter.format(new Date());
		
	    workbook.createSheet("Report_" + orderDateStr, 0);
	    
	    /*
	    /* */
	  /*  
	//    WritableWorkbook workbook = Workbook.getWorkbook(file);
	//    Workbook workbook = null;
		try {
			workbook = Workbook.getWorkbook(file);
		} catch (BiffException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
	 */   
//	    This creates a readable spreadsheet. To obtain a writable version of this spreadsheet, a copy must be made, as follows:
	    /*
		    WritableWorkbook copy = null;
			try {
				copy = Workbook.createWorkbook(file);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		   */  
		    WritableSheet excelSheet = workbook.getSheet(0);
	   
	    {
	    	
	    	
	    	// w = Workbook.getWorkbook(inputWorkbook);
	         // Get the first sheet
	       //  Sheet sheet = workbook.getSheet(0);
	         // Loop over first 10 column and lines
	         
	         	orderDetail.Date = excelSheet.getCell(0, 0).getContents();
       	  		orderDetail.Time = excelSheet.getCell(1, 0).getContents(); 
       	  		String content = excelSheet.getCell(19, 0).getContents();
       	  		if(content != null && !content.isEmpty())
       	  			orderDetail.orderSeqNo = Long.parseLong(content); 
       	  		
       	  		//Write first row information.
       	  		if(!orderDetail.Date.equals("Date")){       	  		
        	 //Added Date, Time, Symbol, Quantity, Trade Method, Entry Mode, Trigger %, Profit Taking %, Loss %, Exit Method, Duration, Order ID, Order Status, Market Price, Trigger Price, Actual Price.     				
       		  addLabel(excelSheet, 0, 0, "Date");
       		  addLabel(excelSheet, 1, 0, "Time");
       		  addLabel(excelSheet, 2, 0, "Symbol");
       		  addLabel(excelSheet, 3, 0, "Quantity");
       		  addLabel(excelSheet, 4, 0, "Trade Method");
       		  addLabel(excelSheet, 5, 0, "Entry Mode");
       		  addLabel(excelSheet, 6, 0, "Treigger %");
       		  addLabel(excelSheet, 7, 0, "Profit Taking %");
       		  addLabel(excelSheet, 8, 0, "Loss %");
       		  addLabel(excelSheet, 9, 0, "Exit Method");
       		  addLabel(excelSheet, 10, 0, "Duration");
       		  addLabel(excelSheet, 11, 0, "Order ID");
       		  addLabel(excelSheet, 12, 0, "Order Status");
       		  addLabel(excelSheet, 13, 0, "Market Price");
       		  addLabel(excelSheet, 14, 0, "Trigger Price");
       		  addLabel(excelSheet, 15, 0, "Actual Price");
       		  addLabel(excelSheet, 16, 0, "Max Drawdown");
       		  addLabel(excelSheet, 17, 0, "Peak gain");
       		  addLabel(excelSheet, 18, 0, "Lost/Gain");
       		
       		  addLabel(excelSheet, 19, 0, "SeqNo.");
       		  addLabel(excelSheet, 20, 0, "Closed Price");
       		  addLabel(excelSheet, 21, 0, "Closed Time");
       		  
       		  addLabel(excelSheet, 22, 0, "Open Time");
   //    		  addLabel(excelSheet, 19, 0, "Date");
   //    		  addLabel(excelSheet, 20, 0, "Date");
   //    		  addLabel(excelSheet, 21, 0, "Date");
  //     		  addLabel(excelSheet, 0, 0, "Date");
  //     		  addLabel(excelSheet, 0, 0, "Date");
       	  		}
       	  		
       	  		//loop thru all orders in HashMap;
       	  		rowPos = 1;
       			//	System.out.println("Looping thru all orders: " + new Date());
       				SortedSet<Long> keys = new TreeSet<Long>(orderHashMap.keySet());
       				for (Long key : keys){
       					orderDetail = orderHashMap.get(key);
       					if(orderDetail == null) continue;
       				   // do something
       					
       			do{		
       				content = excelSheet.getCell(19, rowPos).getContents();
           	  		if(content == null || content.isEmpty())
           	  			break;
           	  		else if(orderDetail.orderSeqNo == Long.parseLong(content))
           	  			break; 
           	  		rowPos++;	        	 
       			}while(true);
       			addLabel(excelSheet, 0, rowPos, orderDetail.Date);
       			addLabel(excelSheet, 1, rowPos, orderDetail.Time);
       			addLabel(excelSheet, 2, rowPos, orderDetail.Symbol);
       			addLabel(excelSheet, 3, rowPos, orderDetail.Quantity);
       			addLabel(excelSheet, 4, rowPos, orderDetail.TradeMethod);
       			addLabel(excelSheet, 5, rowPos, orderDetail.EntryMethod);
       			addLabel(excelSheet, 6, rowPos, orderDetail.TriggerPct);
       			addLabel(excelSheet, 7, rowPos, orderDetail.LossPct);
       			addLabel(excelSheet, 8, rowPos, orderDetail.ProfitPct);
       			addLabel(excelSheet, 9, rowPos, orderDetail.ExitMethod);
       			addLabel(excelSheet, 10, rowPos, orderDetail.ValidDuration);
       			addLabel(excelSheet, 11, rowPos, orderDetail.orderIdList.toString());
       			addLabel(excelSheet, 12, rowPos, orderDetail.OrderStatus);
       			addLabel(excelSheet, 13, rowPos, orderDetail.MarketPrice);
       			addLabel(excelSheet, 14, rowPos, orderDetail.TriggeredPrice);
       			addLabel(excelSheet, 15, rowPos, orderDetail.ActualPrice);
       			addLabel(excelSheet, 16, rowPos, orderDetail.MaxDrawdown);
       			addLabel(excelSheet, 17, rowPos, orderDetail.PeakGain);
       			addLabel(excelSheet, 18, rowPos, orderDetail.LossGain);
       			addLabel(excelSheet, 19, rowPos, new Long(orderDetail.orderSeqNo).toString());
         		addLabel(excelSheet, 20, rowPos, orderDetail.ClosedPrice);
           		addLabel(excelSheet, 21, rowPos, orderDetail.CloseTime);
           		addLabel(excelSheet, 22, rowPos, orderDetail.OpenTime);
       			
       		
	        	 /* 
	        	 

		       	  
		       	  if(orderHashMap.containsKey(orderDetail.orderSeqNo))
		       	  {
		       		orderDetail = orderHashMap.get(orderDetail.orderSeqNo);   		
		       		
		       		
//		          createLabel(excelSheet);
		       		//    createContent(excelSheet);
		       		    
		       		    //col 10. Order ID. Number  public String OrderID;
		       			if(orderDetail.orderIdList.size() != 0)       				
		       		    addLabel(excelSheet, 10, rowPos, orderDetail.orderIdList.toString());
		       		    
		       		    //Col 11. Order status  public String OrderStatus;
		       		    addLabel(excelSheet, 11, 1, orderDetail.OrderStatus);
		       		    
		       		    
		       			
		       			//col 12 public String MarketPrice;
		       		    if(orderDetail.MarketPrice != null && !orderDetail.MarketPrice.isEmpty())
		       		    	addNumber(excelSheet, 12, 1, Double.parseDouble(orderDetail.MarketPrice));
		       		    
		       		    
		       		    
		       			//Col 13 public String TriggeredPrice;
		       		 if(orderDetail.TriggeredPrice != null && !orderDetail.TriggeredPrice.isEmpty())
		       		    addNumber(excelSheet, 13, 1, Double.parseDouble(orderDetail.TriggeredPrice));
		       		    
		       		    //col 14 public String ActualPrice;
		       		if(orderDetail.ActualPrice != null && !orderDetail.ActualPrice.isEmpty())
		       		    addNumber(excelSheet, 14, 1, Double.parseDouble(orderDetail.ActualPrice));
		       			
		       			//col 15 public String MaxDrawdown;
		       		if(orderDetail.MaxDrawdown != null && !orderDetail.MaxDrawdown.isEmpty())
		       		    addNumber(excelSheet, 15, 1, Double.parseDouble(orderDetail.MaxDrawdown));
		       		    
		       			//col 16 public String PeakGain;
		       		if(orderDetail.PeakGain != null && !orderDetail.PeakGain.isEmpty())
		       		    addNumber(excelSheet, 16, 1, Double.parseDouble(orderDetail.PeakGain));
		       		    
		       			//Col 17 public String LossGain;
		       		if(orderDetail.LossGain != null && !orderDetail.LossGain.isEmpty())
		       		    addNumber(excelSheet, 17, 1, Double.parseDouble(orderDetail.LossGain));
		       		    
		       		    //Col 18 public String ClosedPrice;
		       		if( orderDetail.ClosedPrice != null &&!orderDetail.ClosedPrice.isEmpty())
		       		    addNumber(excelSheet, 18, 1, Double.parseDouble(orderDetail.ClosedPrice));
		       		
		       		//Col 20 Sequence No;
		       		    addLabel(excelSheet, 20, 1, new Long(orderDetail.orderSeqNo).toString());
		       		
		       	  }
		       	  */
		       	  
	       	  
		       	  
		       	  

	       	  }
	    	
	    }
	    
	    
	    
	    
	/*
	//    createLabel(excelSheet);
	//    createContent(excelSheet);
	    
	    //col 10. Order ID. Number  public String OrderID;
	//    addNumber(excelSheet, 10, 1, (double) 99);
	    
	    //Col 11. Order status  public String OrderStatus;
	//    addLabel(excelSheet, 11, 1, "Submitted");
	    
	    
		
		//col 12 public String MarketPrice;
	    addNumber(excelSheet, 12, 1, 103.53);
	    
	    
	    
		//Col 13 public String TriggeredPrice;
	    addNumber(excelSheet, 13, 1, 103.53);
	    
	    //col 14 public String ActualPrice;
	    addNumber(excelSheet, 14, 1, 101.53);
		
		//col 15 public String MaxDrawdown;
	    addNumber(excelSheet, 15, 1, 10000.0);
	    
		//col 16 public String PeakGain;
	    addNumber(excelSheet, 16, 1, 10000.0);
	    
		//Col 17 public String LossGain;
	    addNumber(excelSheet, 17, 1, 10000.0);
	    
	    //Col 18 public String ClosedPrice;
	    addNumber(excelSheet, 18, 1, 99.0);
	    
	    */
	    try {
	    	workbook.write();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    try {
	    	workbook.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  }
  
  
  private void createLabel(WritableSheet sheet)
      throws WriteException {
    // Lets create a times font
    WritableFont times10pt = new WritableFont(WritableFont.TIMES, 10);
    // Define the cell format
    times = new WritableCellFormat(times10pt);
    // Lets automatically wrap the cells
    times.setWrap(true);

    // create create a bold font with unterlines
    WritableFont times10ptBoldUnderline = new WritableFont(WritableFont.TIMES, 10, WritableFont.BOLD, false,
        UnderlineStyle.SINGLE);
    timesBoldUnderline = new WritableCellFormat(times10ptBoldUnderline);
    // Lets automatically wrap the cells
    timesBoldUnderline.setWrap(true);

    CellView cv = new CellView();
    cv.setFormat(times);
    cv.setFormat(timesBoldUnderline);
    cv.setAutosize(true);

    // Write a few headers
    addCaption(sheet, 0, 0, "Header 1");
    addCaption(sheet, 1, 0, "This is another header");
    

  }

  private void createContent(WritableSheet sheet) throws WriteException,
      RowsExceededException {
    // Write a few number
    for (int i = 1; i < 10; i++) {
      // First column
      addNumber(sheet, 0, i, (double) (i + 10));
      // Second column
      addNumber(sheet, 1, i, (double) (i * i));
    }
    // Lets calculate the sum of it
    StringBuffer buf = new StringBuffer();
    buf.append("SUM(A2:A10)");
    Formula f = new Formula(0, 10, buf.toString());
    sheet.addCell(f);
    buf = new StringBuffer();
    buf.append("SUM(B2:B10)");
    f = new Formula(1, 10, buf.toString());
    sheet.addCell(f);

    // now a bit of text
    for (int i = 12; i < 20; i++) {
      // First column
      addLabel(sheet, 0, i, "Boring text " + i);
      // Second column
      addLabel(sheet, 1, i, "Another text");
    }
  }

  private void addCaption(WritableSheet sheet, int column, int row, String s)
      throws RowsExceededException, WriteException {
    Label label;
    label = new Label(column, row, s, timesBoldUnderline);
    sheet.addCell(label);
  }

  private void addNumber(WritableSheet sheet, int column, int row,
      Double val) throws WriteException, RowsExceededException {
    Number number;
    number = new Number(column, row, val);
    sheet.addCell(number);
  }

  private void addLabel(WritableSheet sheet, int column, int row, String s)
      throws WriteException, RowsExceededException {
    Label label;
    label = new Label(column, row, s);
    sheet.addCell(label);
  }

  public static void main(String[] args) throws WriteException, IOException, BiffException {
    WriteExcel test = new WriteExcel();
    test.setOutputFile("Forex.xls");
    forex orderDetail = new forex();
//    test.write(orderDetail);
    System.out
        .println("Please check the result file under Forex.xls ");
  }
} 