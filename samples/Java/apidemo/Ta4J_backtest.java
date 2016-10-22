package apidemo;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.WhatToShow;
import com.ib.controller.Bar;
import com.ib.controller.ApiController.IHistoricalDataHandler;
import com.opencsv.CSVReader;

import eu.verdelhan.ta4j.AnalysisCriterion;
import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.analysis.CashFlow;
import eu.verdelhan.ta4j.analysis.criteria.AverageProfitableTradesCriterion;
import eu.verdelhan.ta4j.analysis.criteria.RewardRiskRatioCriterion;
import eu.verdelhan.ta4j.analysis.criteria.TotalProfitCriterion;
import eu.verdelhan.ta4j.analysis.criteria.VersusBuyAndHoldCriterion;
import eu.verdelhan.ta4j.indicators.oscillators.StochasticOscillatorDIndicator;
import eu.verdelhan.ta4j.indicators.oscillators.StochasticOscillatorKIndicator;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.RSIIndicator;
import eu.verdelhan.ta4j.indicators.trackers.SMAIndicator;
import eu.verdelhan.ta4j.trading.rules.CrossedDownIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.CrossedUpIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.OverIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.StopGainRule;
import eu.verdelhan.ta4j.trading.rules.StopLossRule;
import eu.verdelhan.ta4j.trading.rules.UnderIndicatorRule;
import ta4jexamples.indicators.IndicatorsToCsv;
import ta4jexamples.loaders.CsvTradesLoader;



public class Ta4J_backtest  extends Thread{
	private ApiDemo apiDemoHost;
	private Contract currencyContractHost;
	private Calendar serverTimeCalendar;
	private boolean historicalDataEnd = true;
	private TimeSeries backTimeSeries;
	private backtestingHistortyDataHandler forexHistoricalHandler;
	private int INIT_TIMEOUT = 60;
	private ConcurrentHashMap<String, Contract> contractMapHost;
	private ConcurrentHashMap<Long, forex> forexOrderMapHost;


	
	
	public Ta4J_backtest(ApiDemo apiDemo, ConcurrentHashMap<String, Contract> contractMap, Calendar cal, ConcurrentHashMap<Long,forex> orderHashMap ) {
		apiDemoHost = apiDemo;
		contractMapHost = contractMap;
		serverTimeCalendar = cal;
		forexOrderMapHost = orderHashMap;
		// TODO Auto-generated constructor stub
		
	}

	public void collectHistoricalData(){
		
		 for(Entry<String, Contract> currentContract : contractMapHost.entrySet())
			 {
			 	boolean need2Collect = false;
			 	currencyContractHost = currentContract.getValue();

			 	//Only collect data for symbols in Excel file.
			 	for(Entry<Long, forex> currentForexOrder : forexOrderMapHost.entrySet()){			 	
			 		if(currentForexOrder.getValue().Symbol.equals(currencyContractHost.symbol() + currencyContractHost.currency()) == true){
			 			need2Collect = true;
			 			break;
			 			}
			 	}
			 	
			 	if(need2Collect == false)
			 		continue;
			 	
				forexHistoricalHandler = new backtestingHistortyDataHandler(currencyContractHost);
//				 try {
////					Ta4J_backtest.join();
//				} 
//				 catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
			
		SimpleDateFormat formatter;
		String orderDateStr;
		Date pastDate = serverTimeCalendar.getTime();
		Calendar localCal = Calendar.getInstance();
		localCal.setTime(pastDate);
		localCal.add(Calendar.YEAR, -8);
		Date oneYearAgoDate = localCal.getTime();
	    final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		localCal.setTime(pastDate);
		
		List<Tick> allTicks = new ArrayList<Tick>();
		List<Tick> ticks = null;// = new ArrayList<Tick>();

	//	 years = new TreeSet<Integer>();
		ConcurrentHashMap<Integer, List<Tick>> yearKeyMap = new ConcurrentHashMap<Integer, List<Tick>>();
		long timeout = 0;
		Date lastProcessedtime = pastDate;
		
		while(true){
			
			
			
			pastDate = localCal.getTime();
			formatter = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
			orderDateStr = formatter.format(pastDate);
			if( pastDate.after(oneYearAgoDate)){
				requestHistoricalBar(1,  orderDateStr, currencyContractHost);
				System.out.println(new Date() + " Request historical data : " + orderDateStr + " for " + currencyContractHost.symbol() + currencyContractHost.currency());
				timeout = 0;
			}
			historicalDataEnd = false;
			

			
			while(historicalDataEnd == false){
				try {
					Thread.sleep(1000);
					timeout++;
					if(timeout > INIT_TIMEOUT){
						requestHistoricalBar(1,  orderDateStr, currencyContractHost);
						System.out.println(new Date() + " Request historical data : " + orderDateStr + " for " + currencyContractHost.symbol() + currencyContractHost.currency());
						timeout = 0;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			//Process data in a month;

            DateTime lastRequestDate = null;
            try {
            	lastRequestDate = new DateTime(formatter.parse(orderDateStr));
			} catch (ParseException e) {
//				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			
			TreeSet<Long> keys = new TreeSet<Long>(currencyContractHost.historicalBarMap.keySet());
			TreeSet<Long> treereverse = (TreeSet<Long>) keys.descendingSet();
			boolean completed = false;
			ticks = new ArrayList<Tick>();

			for (Long key : treereverse){
			Bar bar = currencyContractHost.historicalBarMap.get(key);
			currencyContractHost.historicalBarMap.remove(key);

			  DateTimeZone zoneVic = DateTimeZone.forID("Australia/Melbourne");
              DateTime newTime = new DateTime(bar.time() * 1000);
              newTime = newTime.plus(Period.years(1900)).withZone(zoneVic);
              newTime = newTime.minus(Period.seconds(52));
              newTime = newTime.minus(Period.minutes(4));

  			System.out.println(currencyContractHost.symbol() + currencyContractHost.currency() + " time: " + newTime+ " bar high: " + bar.high() + " bar low: " + bar.low() + " bar close: " + bar.close() + " bar volume: " + bar.volume());
             
				if(oneYearAgoDate.after(newTime.toDate())){
					completed = true;
					break;
					}

					//If last acquired time is before this tick time, which means this data is acquired and should be abandoned.
					if(lastProcessedtime.before(newTime.toDate()))
						continue;
					
	                double open = bar.open();
	                double high = bar.high();
	                double low = bar.low();
	                double close = bar.close();
	                double volume = bar.volume();
	              
	                
	                Tick tick = new Tick(newTime, open, high, low, close, volume);
	                ticks.add(tick);
	                
	            }
			if(completed == true)
				break;
			if(ticks.isEmpty())
				continue;
			 TimeSeries oneYearSeries = new TimeSeries(currencyContractHost.symbol() + currencyContractHost.currency() + "_ticks", ticks);
	//	      allTicks.addAll(ticks);
		     new IndicatorsToCsv(oneYearSeries);
	//	       new TicksAccesser(oneYearSeries).writeToCsv();
		       System.out.println(new Date() + "Write ticks into file " + currencyContractHost.symbol() + currencyContractHost.currency() + "_ticks.csv"); 
		       
		       //before adjusting calendar to new date, ensure that we receive all ticks.
				DateTime currentTickDate = (ticks.get(ticks.size() - 1).getEndTime());
				if(currentTickDate.isBefore(lastRequestDate)){
					localCal.add(Calendar.YEAR, -1);
//					localCal.add(Calendar.HOUR, 24);

				}
				
				lastProcessedtime = ticks.get(ticks.size() - 1).getEndTime().toDate();

				
//	            
//
//				
//
//			
			
          

	//        ticks.clear();
    //currencyContractHost.historicalBarMap.clear();		
			
			
		}
		
		for(Integer year : yearKeyMap.keySet()){
	//		ticks = yearKeyMap.get(year);
	//	  SimpleDateFormat formatterYear = new SimpleDateFormat("yyyy");

	//	  String dateStr = formatterYear.format(date.toDate());
	//	  TimeSeries oneYearSeries = new TimeSeries(currencyContractHost.symbol() + currencyContractHost.currency() + "_ticks_" + year, ticks);
	//      allTicks.addAll(ticks);
	 //      new TicksAccesser(oneYearSeries);
		}
		
//		
//   //     backTimeSeries.addTick(tick);
//		List<Tick> allTicks = new ArrayList<Tick>();
//
//		for(List<Tick> ticks: ticksList){
//			allTicks.addAll(ticks);
//		}
		
		//Process data in a month;
//
//        DateTime date = null;
//
//		
//		TreeSet<Long> keys = new TreeSet<Long>(currencyContractHost.historicalBarMap.keySet());
//		TreeSet<Long> treereverse = (TreeSet<Long>) keys.descendingSet();
//		for (Long key : treereverse){
//		Bar bar = currencyContractHost.historicalBarMap.get(key);
//		System.out.println(currencyContractHost.symbol() + currencyContractHost.currency() + " time: " + bar.formattedTime()+ " bar high: " + bar.high() + " bar low: " + bar.low() + " bar close: " + bar.close() + " bar volume: " + bar.volume());
////		show(m_currencyContract.symbol() + m_currencyContract.currency() + " time: " + bar.formattedTime()+ " bar high: " + bar.high() + " bar low: " + bar.low() + " bar close: " + bar.close());
//
//		
//
//      
//       
//  //          String[] line;
//  //          while ((line = csvReader.readNext()) != null) 
//            {
//				try {
//					date = new DateTime(DATE_FORMAT.parse(bar.formattedTime()));
//				} catch (ParseException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				//If current data
//				if(oneYearAgoDate.after(date.toDate()))
//					break;
//                double open = bar.open();
//                double high = bar.high();
//                double low = bar.low();
//                double close = bar.close();
//                double volume = bar.volume();
//                Tick tick = new Tick(date, open, high, low, close, volume);
//                ticks.add(tick);
//            }
//            ;
//
//			
//
//		
//		}
//		
	//	String dateStrFile =  ticks.get(ticks.size() - 1).getSimpleFileDateName() + "_" + ticks.get(0).getSimpleFileDateName();
     //   backTimeSeries = new TimeSeries(currencyContractHost.symbol() + currencyContractHost.currency() + "_ticks_" + dateStrFile, ticks);
//        
      //  new IndicatorsToCsv(backTimeSeries);
 //       testASimpleAlgo(backTimeSeries);
        
   //     currencyContractHost.historicalBarMap.clear();	

	
	}
	}

	public void run() {
		collectHistoricalData();
//		SimpleDateFormat formatter;
//		String orderDateStr;
//		Date pastDate = serverTimeCalendar.getTime();
//		Calendar localCal = Calendar.getInstance();
//		localCal.setTime(pastDate);
//		localCal.add(Calendar.YEAR, -12);
//		Date oneYearAgoDate = localCal.getTime();
//	    final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//		localCal.setTime(pastDate);
//		
//		List<Tick> allTicks = new ArrayList<Tick>();
//		List<Tick> ticks = null;// = new ArrayList<Tick>();
//
//	//	 years = new TreeSet<Integer>();
//		ConcurrentHashMap<Integer, List<Tick>> yearKeyMap = new ConcurrentHashMap<Integer, List<Tick>>();
//		long timeout = 0;
//		Date lastProcessedtime = pastDate;
//		
//		while(true){
//			
//			
//			
//			pastDate = localCal.getTime();
//			formatter = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
//			orderDateStr = formatter.format(pastDate);
//			if( pastDate.after(oneYearAgoDate)){
//				requestHistoricalBar(1,  orderDateStr, currencyContractHost);
//			}
//			historicalDataEnd = false;
//			
//
//			
//			while(historicalDataEnd == false){
//				try {
//					Thread.sleep(1000);
//					timeout++;
//					if(timeout > INIT_TIMEOUT){
//						requestHistoricalBar(1,  orderDateStr, currencyContractHost);
//						System.out.println(new Date() + " Request historical data : " + orderDateStr);
//						timeout = 0;
//					}
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//			
//			//Process data in a month;
//
//            DateTime lastRequestDate = null;
//            try {
//            	lastRequestDate = new DateTime(formatter.parse(orderDateStr));
//			} catch (ParseException e) {
////				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
////			
//			TreeSet<Long> keys = new TreeSet<Long>(currencyContractHost.historicalBarMap.keySet());
//			TreeSet<Long> treereverse = (TreeSet<Long>) keys.descendingSet();
//			boolean completed = false;
//			ticks = new ArrayList<Tick>();
//
//			for (Long key : treereverse){
//			Bar bar = currencyContractHost.historicalBarMap.get(key);
//			currencyContractHost.historicalBarMap.remove(key);
//			System.out.println(currencyContractHost.symbol() + currencyContractHost.currency() + " time: " + bar.formattedTime()+ " bar high: " + bar.high() + " bar low: " + bar.low() + " bar close: " + bar.close() + " bar volume: " + bar.volume());
////			show(m_currencyContract.symbol() + m_currencyContract.currency() + " time: " + bar.formattedTime()+ " bar high: " + bar.high() + " bar low: " + bar.low() + " bar close: " + bar.close());
////
////			
////
////	      
////	       
////          String[] line;
////	  //          while ((line = csvReader.readNext()) != null) 
////	            {
//					
//					
//					
//
//				//	if(!yearKeyMap.containsKey(date.getYear()))
//					{
//					//	yearKeyMap.put(date.getYear(), ticks);
//					}
//	//				ticks = yearKeyMap.get(date.getYear());
//
//					if(oneYearAgoDate.after(lastRequestDate.toDate())){
//						completed = true;
//						break;
//						}
//					//If last acquired time is before this tick time, which means this data is acquired and should be abandoned.
//					if(lastProcessedtime.before(new Date(bar.time() * 1000)))
//						continue;
//					
//	                double open = bar.open();
//	                double high = bar.high();
//	                double low = bar.low();
//	                double close = bar.close();
//	                double volume = bar.volume();
//	                Tick tick = new Tick(new DateTime(bar.time() * 1000), open, high, low, close, volume);
//	                ticks.add(tick);
//	                
//	            }
//			if(completed == true)
//				break;
//			if(ticks.isEmpty())
//				continue;
//			 TimeSeries oneYearSeries = new TimeSeries(currencyContractHost.symbol() + currencyContractHost.currency() + "_ticks", ticks);
//	//	      allTicks.addAll(ticks);
//		       new TicksAccesser(oneYearSeries);
//
//		       
//		       //before adjusting calendar to new date, ensure that we receive all ticks.
//				DateTime currentTickDate = (ticks.get(ticks.size() - 1).getEndTime());
//				if(currentTickDate.isBefore(lastRequestDate)){
//					localCal.add(Calendar.MONTH, -1);
//
//				}
//				
//				lastProcessedtime = ticks.get(ticks.size() - 1).getEndTime().toDate();
//
//				
////	            
////
////				
////
////			
//			
//          
//
//	//        ticks.clear();
//    //currencyContractHost.historicalBarMap.clear();		
//			
//			
//		}
//		
//		for(Integer year : yearKeyMap.keySet()){
//	//		ticks = yearKeyMap.get(year);
//	//	  SimpleDateFormat formatterYear = new SimpleDateFormat("yyyy");
//
//	//	  String dateStr = formatterYear.format(date.toDate());
//	//	  TimeSeries oneYearSeries = new TimeSeries(currencyContractHost.symbol() + currencyContractHost.currency() + "_ticks_" + year, ticks);
//	//      allTicks.addAll(ticks);
//	 //      new TicksAccesser(oneYearSeries);
//		}
//		
////		
////   //     backTimeSeries.addTick(tick);
////		List<Tick> allTicks = new ArrayList<Tick>();
////
////		for(List<Tick> ticks: ticksList){
////			allTicks.addAll(ticks);
////		}
//		
//		//Process data in a month;
////
////        DateTime date = null;
////
////		
////		TreeSet<Long> keys = new TreeSet<Long>(currencyContractHost.historicalBarMap.keySet());
////		TreeSet<Long> treereverse = (TreeSet<Long>) keys.descendingSet();
////		for (Long key : treereverse){
////		Bar bar = currencyContractHost.historicalBarMap.get(key);
////		System.out.println(currencyContractHost.symbol() + currencyContractHost.currency() + " time: " + bar.formattedTime()+ " bar high: " + bar.high() + " bar low: " + bar.low() + " bar close: " + bar.close() + " bar volume: " + bar.volume());
//////		show(m_currencyContract.symbol() + m_currencyContract.currency() + " time: " + bar.formattedTime()+ " bar high: " + bar.high() + " bar low: " + bar.low() + " bar close: " + bar.close());
////
////		
////
////      
////       
////  //          String[] line;
////  //          while ((line = csvReader.readNext()) != null) 
////            {
////				try {
////					date = new DateTime(DATE_FORMAT.parse(bar.formattedTime()));
////				} catch (ParseException e) {
////					// TODO Auto-generated catch block
////					e.printStackTrace();
////				}
////				//If current data
////				if(oneYearAgoDate.after(date.toDate()))
////					break;
////                double open = bar.open();
////                double high = bar.high();
////                double low = bar.low();
////                double close = bar.close();
////                double volume = bar.volume();
////                Tick tick = new Tick(date, open, high, low, close, volume);
////                ticks.add(tick);
////            }
////            ;
////
////			
////
////		
////		}
////		
////		String dateStrFile =  ticks.get(ticks.size() - 1).getSimpleFileDateName() + "_" + ticks.get(0).getSimpleFileDateName();
////        backTimeSeries = new TimeSeries(currencyContractHost.symbol() + currencyContractHost.currency() + "_ticks_" + dateStrFile, ticks);
////        
////        new IndicatorsToCsv(backTimeSeries);
// //       testASimpleAlgo(backTimeSeries);
//        
//   //     currencyContractHost.historicalBarMap.clear();	
//
//	
	}
	
	
	
	
	public void requestHistoricalBar(Integer durationInMonths, String endTime, Contract currencyContract){
		//forex orderDetail;
		
		//Only request it once.
		/*
		historicalDataReq.clear();
		SortedSet<Long> keys = new TreeSet<Long>(orderHashMap.keySet());
		for (Long key : keys)
		{
			orderDetail = orderHashMap.get(key);
		    if(orderDetail.ValidDuration.equals("60"))
		    	continue;
		    if(historicalDataReq.contains(orderDetail.Symbol))
		    	continue;
		    historicalDataReq.add(orderDetail.Symbol);
		    currencyContract = contractMap.get(orderDetail.Symbol);
		  */  
	//		if(forexHistoricalHandler != null && currencyContract != null)
				apiDemoHost.controller().reqHistoricalData(currencyContract, endTime, durationInMonths, DurationUnit.YEAR, BarSize._1_day, WhatToShow.MIDPOINT, true, forexHistoricalHandler);
	//		else
	//		{
	//			System.out.println("Null pointer here, Please check your order" + currencyContract + forexHistoricalHandler);
	//			show(new Date() + "Null pointer here, Please check your order" + currencyContract + forexHistoricalHandler);
	//		}
			
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	class backtestingHistortyDataHandler implements IHistoricalDataHandler{
		Contract m_currencyContract;

		public backtestingHistortyDataHandler(Contract currencyContract) {
			// TODO Auto-generated constructor stub
			m_currencyContract = currencyContract;
		}

		@Override
		public void historicalData(Bar bar, boolean hasGaps) {
			// TODO Auto-generated method stub
			m_currencyContract.putHistoricalBar(bar.time(), bar);
			
	//		contractMap.put(m_currencyContract.symbol() + m_currencyContract.currency(), m_currencyContract);
			
			
		}

		@Override
		public void historicalDataEnd() {
			// TODO Auto-generated method stub
//			System.out.println(new Date() + " end of bar high: ");
//			TreeSet<String> keys = new TreeSet<String>(m_currencyContract.historicalBarMap.keySet());
//			TreeSet<String> treereverse = (TreeSet) keys.descendingSet();
//			for (String key : treereverse){
//			Bar bar = m_currencyContract.historicalBarMap.get(key);
//			System.out.println(m_currencyContract.symbol() + m_currencyContract.currency() + " time: " + bar.formattedTime()+ " bar high: " + bar.high() + " bar low: " + bar.low() + " bar close: " + bar.close());
//			show(m_currencyContract.symbol() + m_currencyContract.currency() + " time: " + bar.formattedTime()+ " bar high: " + bar.high() + " bar low: " + bar.low() + " bar close: " + bar.close());

//			}
//			m_currencyContract.historicalBarMap.g
			System.out.println(new Date() + " historicalDataEnd");

			historicalDataEnd = true;

		}
	};
	
	
}
