package apidemo;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import jxl.Cell;
import jxl.CellType;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

public class ReadExcel {

  private String inputFile;
  private static int rowPosition = 0, colPosition = 0;
  

  public void setInputFile(String inputFile) {
    this.inputFile = inputFile;
  }

  public ConcurrentHashMap<Long, forex> read(ConcurrentHashMap<Long, forex> orderHashMap) throws IOException  {
	forex orderDetail = new forex();
//	HashMap<Long, forex> orderHashMap = new HashMap<Long, forex>(50);
	int validDuration = 0;
    File inputWorkbook = new File(inputFile);
    Workbook w;


    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
	String dateStr = dateFormat.format(new Date());	
	Long groupId = Long.parseLong(dateStr); 
    
    
    Long currentSeqNo = new Long(0), lastDateCode = new Long(0), currentDateCode = new Long(0);
    try {
      w = Workbook.getWorkbook(inputWorkbook);
      // Get the first sheet
      Sheet sheet = w.getSheet(0);
      // Loop over first 10 column and lines
      
      for (int i = 0; i < sheet.getRows(); i++) {
     	  if(sheet.getCell(0, i).getContents().isEmpty()) break;
     	  
     	  orderDetail.Date = sheet.getCell(0, i).getContents();
    	  orderDetail.Time = sheet.getCell(1, i).getContents(); 
    	  
    	
    	  
    	  orderDetail.Symbol = sheet.getCell(2, i).getContents();
    	  orderDetail.Quantity = sheet.getCell(3, i).getContents();
    	  orderDetail.TradeMethod = sheet.getCell(4, i).getContents();
    	  orderDetail.EntryMethod = sheet.getCell(5, i).getContents();
    	  orderDetail.TriggerPct = sheet.getCell(6, i).getContents();
    	  orderDetail.LossPct = sheet.getCell(7, i).getContents();
    	  orderDetail.ProfitPct = sheet.getCell(8, i).getContents();
    	  orderDetail.ExitMethod = sheet.getCell(9, i).getContents();
    	  orderDetail.ValidDuration = sheet.getCell(10, i).getContents();
    	  orderDetail.Importance = sheet.getCell(11, i).getContents();
    	 
    	  orderDetail.OrderID = sheet.getCell(12, i).getContents();
    	  
    	  
    	  if(!orderDetail.OrderID.isEmpty()) continue;    	  
    	  if(orderDetail.Date.contains("Date")) continue;
    	  

    		  
    	  DateFormat formatter; 			    	      
		  formatter = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		  String orderDateStr= orderDetail.Date + " " + orderDetail.Time;	
    	  Date  orderTime  = null;
		  try {
				orderTime  = (Date)formatter.parse(orderDateStr);
		  }catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
		} 
		  formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		  orderDateStr = formatter.format(orderTime);
    	  currentDateCode = orderDetail.orderSeqNo = Long.parseLong(orderDateStr + "00");
    	  
  	  
    	  
    	  if(currentDateCode.equals(lastDateCode)){
    		  orderDetail.orderSeqNo = currentSeqNo + 1;
        	  currentSeqNo = orderDetail.orderSeqNo;    		  
    	  }else{
    		currentSeqNo = orderDetail.orderSeqNo;
    		lastDateCode = currentSeqNo;
    	  }
    	  
    	  
    	  /* orderDetail.OrderStatus;
    	  orderDetail.MarketPrice;
    	  orderDetail.TriggeredPrice;
    	  orderDetail.ActualPrice;
    	  orderDetail.MaxDrawdown;
    	  orderDetail.PeakGain;
    	  orderDetail.LossGain;
    	  orderDetail.ClosedPrice;
    	  */
/*
    	  for (int j = 0; j < sheet.getColumns(); j++) {
        
          Cell cell = sheet.getCell(j, i);

          CellType type = cell.getType();
          if (type == CellType.LABEL) {
            System.out.print(
                cell.getContents() + " ");
          }

          else if (type == CellType.NUMBER) {
              System.out.print(
                      cell.getContents() + " ");
          }
          else if (type == CellType.DATE){
              System.out.print(
                      cell.getContents() + " ");
                }

        }
    	  System.out.println(" SeqNo: " + orderDetail.orderSeqNo);
    	 */ 
    	  
    	  
    	
		  

    	  int validDurationinM = 0;
    	  if(orderDetail.ValidDuration != null)	
    		  validDuration = Integer.parseInt(orderDetail.ValidDuration);
    	  //Aaron try
    	  
//    	  if(validDuration > 60)
  //  	  { //case for multiple order
 //   		  validDurationinM = validDuration / 60;
 //   		  for(int counter = 0; counter < validDurationinM; counter++){
  //  			  while(orderHashMap.get(orderDetail.orderSeqNo) != null){
  //  	    		  orderDetail.orderSeqNo++;
//    	    	  }
    	    	  //Only read data again if submitted status isn't empty. It adds new class instances into map onlys when it can't find same order Seq No in the map.
//    	    	  orderHashMap.get(orderDetail.orderSeqNo);
//    	    	  if(orderHashMap.get(orderDetail.orderSeqNo) == null/* || orderHashMap.get(orderDetail.orderSeqNo).OrderStatus ==null || orderHashMap.get(orderDetail.orderSeqNo).OrderStatus.isEmpty()*/)
//    	    	  {	  
    	    		  
//    	    	      		  //create a new instance
//    	    		  forex newOrderDetail = new forex();
//    	    		  newOrderDetail.groupId = groupId;
//    	    		  newOrderDetail.Date = orderDetail.Date;
//    	    		  newOrderDetail.Time = orderDetail.Time;
//    	    		  newOrderDetail.Symbol = orderDetail.Symbol;
//    	    		  newOrderDetail.TriggerPct = orderDetail.TriggerPct;
//    	    		  newOrderDetail.LossPct = orderDetail.LossPct;
//    	    		  newOrderDetail.ProfitPct = orderDetail.ProfitPct;
//    	    		  newOrderDetail.Quantity = orderDetail.Quantity;
//    	    		  newOrderDetail.TradeMethod = orderDetail.TradeMethod;
//    	    		  newOrderDetail.EntryMethod = orderDetail.EntryMethod;
//    	    		  newOrderDetail.ExitMethod = orderDetail.ExitMethod;
//    	    		  newOrderDetail.orderSeqNo = orderDetail.orderSeqNo;
//    	    		  newOrderDetail.OCA = true;
//    	    		  newOrderDetail.ValidDuration = "60"; //change each order to valid only 60 seconds.
    	    		  
//    	    		  System.out.println(new Date() + " SeqNo: " + newOrderDetail.orderSeqNo + " " +  newOrderDetail.Symbol + " " + newOrderDetail.ValidDuration  + newOrderDetail.OCA); 
    	    		  
//    	    		  orderHashMap.put(newOrderDetail.orderSeqNo, newOrderDetail);
    	    		  
//    	    		  orderPlusDuration = new Date(orderTime.getTime() + 60 * 1000); //Added 1 minutes.
//   	    		  orderTime = orderPlusDuration;
//    	    		  orderDateStr = formatter.format(orderPlusDuration);
					
//					  dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
//					  orderDateStr = dateFormat.format(orderPlusDuration);
//					  orderDateStr = orderDateStr + "00";
//					  orderDetail.orderSeqNo = Long.parseLong(orderDateStr); 
    				  
//					  dateFormat = new SimpleDateFormat("yyyyMMdd");
//					  orderDetail.Date = dateFormat.format(orderPlusDuration);
					  
//					  dateFormat = new SimpleDateFormat("HH:mm");
//					  orderDetail.Time = dateFormat.format(orderPlusDuration);
					  

    				  
//    	    		  orderDetail.orderSeqNo = orderDetail.orderSeqNo + 100;
//    	    	  }
//    		  }
//    		  groupId++;
//    	  }else
    	  { //case for single order
    	  
    	
    	  //Only read data again if submitted status isn't empty. It adds new class instances into map onlys when it can't find same order Seq No in the map.
    	  orderHashMap.get(orderDetail.orderSeqNo);
    	  if(orderHashMap.get(orderDetail.orderSeqNo) == null/* || orderHashMap.get(orderDetail.orderSeqNo).OrderStatus ==null || orderHashMap.get(orderDetail.orderSeqNo).OrderStatus.isEmpty()*/)
    	  {	  
    		  System.out.println(new Date() + " SeqNo: " + orderDetail.orderSeqNo + " " +  orderDetail.Symbol + " " + orderDetail.ValidDuration + orderDetail.OCA); 
    		  orderDetail.groupId = new Long(0); //This is a single order. so group ID = 0;
  	  orderHashMap.put(orderDetail.orderSeqNo, orderDetail);
    	      		  //create a new instance
   		  orderDetail = new forex();
    	  }
    	 }
      }
    } catch (BiffException e) {
      e.printStackTrace();
    }
	return orderHashMap;
  }

  public static void main(String[] args) throws IOException {
    ReadExcel test = new ReadExcel();
    test.setInputFile("Forex.xls");
    test.read(null);
  }

} 
