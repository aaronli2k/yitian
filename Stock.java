
public class Stock {
	public String exchange;
	public String symbol;
	public String name;
	public String direction;
	public String interval;
	public String entryPrice;
	public String targetPrice1;
	public String targetPrice2;
	public String stopPrice;
	public String targetPct1;
	public String targetPct2;
	public String trader;
	public String date;
	public Long seqNo;


void parseStockContent(String content){
	String[] splitSentense= content.split("\n");
	for(String iterator : splitSentense){
		System.out.println(iterator);
		if(iterator.contains("Exchange")) 
			exchange = iterator.split(" ")[1].replace('\r', '\0');
		if(iterator.contains("Symbol")) 
			symbol = iterator.split(" ")[1].replace('\r', '\0');
		if(iterator.contains("Name")) 
			name = iterator.replace("Name", "").replace('\r', '\0');
		if(iterator.contains("Interval")) 
			interval = iterator.split(" ")[1].replace('\r', '\0');
		if(iterator.contains("Direction")) 
			direction = iterator.split(" ")[1].replace('\r', '\0');
		if(iterator.contains("Entry") && iterator.contains("Price")) 
			entryPrice = iterator.split(" ")[2].replace('\r', '\0');
		if(iterator.contains("Target") && iterator.contains("Price") && iterator.contains("1")) 
			targetPrice1 = iterator.split(" ")[3].replace('\r', '\0');
		if(iterator.contains("Target") && iterator.contains("Price") && iterator.contains("2")) 
			targetPrice2 = iterator.split(" ")[3].replace('\r', '\0');
		if(iterator.contains("Stop") && iterator.contains("Price")) 
			stopPrice = iterator.split(" ")[2].replace('\r', '\0');
		if(iterator.contains("Trader")) 
			trader = iterator.split(" ")[1].replace('\r', '\0');
	}    	
	System.out.println(exchange);
	System.out.println(symbol);
	System.out.println(name);
	System.out.println(direction);
	System.out.println(interval);
	System.out.println(entryPrice);
	System.out.println(targetPrice1);
	System.out.println(targetPrice2);
	System.out.println(stopPrice);
	System.out.println(trader);    	
}
}