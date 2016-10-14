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
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;

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

	public Ta4J_backtest(ApiDemo apiDemo, Contract currencyContract, Calendar cal){
		apiDemoHost = apiDemo;
		currencyContractHost = currencyContract;
		serverTimeCalendar = cal;
		forexHistoricalHandler = new backtestingHistortyDataHandler(currencyContractHost);
	}
	
	
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
		localCal.add(Calendar.YEAR, -10);
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
			System.out.println(currencyContractHost.symbol() + currencyContractHost.currency() + " time: " + bar.formattedTime()+ " bar high: " + bar.high() + " bar low: " + bar.low() + " bar close: " + bar.close() + " bar volume: " + bar.volume());


					if(oneYearAgoDate.after(new Date(bar.time() * 1000))){
						completed = true;
						break;
						}
					//If last acquired time is before this tick time, which means this data is acquired and should be abandoned.
					if(lastProcessedtime.before(new Date(bar.time() * 1000)))
						continue;
					
	                double open = bar.open();
	                double high = bar.high();
	                double low = bar.low();
	                double close = bar.close();
	                double volume = bar.volume();
	                Tick tick = new Tick(new DateTime(bar.time() * 1000), open, high, low, close, volume);
	                ticks.add(tick);
	                
	            }
			if(completed == true)
				break;
			if(ticks.isEmpty())
				continue;
			 TimeSeries oneYearSeries = new TimeSeries(currencyContractHost.symbol() + currencyContractHost.currency() + "_ticks", ticks);
	//	      allTicks.addAll(ticks);
		       new TicksAccesser(oneYearSeries).writeToCsv();
		       System.out.println(new Date() + "Write ticks into file " + currencyContractHost.symbol() + currencyContractHost.currency() + "_ticks.csv"); 
		       
		       //before adjusting calendar to new date, ensure that we receive all ticks.
				DateTime currentTickDate = (ticks.get(ticks.size() - 1).getEndTime());
				if(currentTickDate.isBefore(lastRequestDate)){
					localCal.add(Calendar.MONTH, -1);

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
//		String dateStrFile =  ticks.get(ticks.size() - 1).getSimpleFileDateName() + "_" + ticks.get(0).getSimpleFileDateName();
//        backTimeSeries = new TimeSeries(currencyContractHost.symbol() + currencyContractHost.currency() + "_ticks_" + dateStrFile, ticks);
//        
//        new IndicatorsToCsv(backTimeSeries);
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
	
	private void testASimpleAlgo(TimeSeries series){


        // Getting a time series (from any provider: CSV, web service, etc.)
 //       TimeSeries series = CsvTradesLoader.loadBitstampSeries();


        // Getting the close price of the ticks
        Decimal firstClosePrice = series.getTick(0).getClosePrice();
        System.out.println("First close price: " + firstClosePrice.toDouble());
        // Or within an indicator:
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        // Here is the same close price:
        System.out.println(firstClosePrice.isEqual(closePrice.getValue(0))); // equal to firstClosePrice

        // Getting the simple moving average (SMA) of the close price over the last 5 ticks
        SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
        // Here is the 5-ticks-SMA value at the 42nd index
        System.out.println("5-ticks-SMA value at the 42nd index: " + shortSma.getValue(42).toDouble());

        // Getting a longer SMA (e.g. over the 30 last ticks)
        SMAIndicator longSma = new SMAIndicator(closePrice, 10);


        // Relative strength index
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        
        StochasticOscillatorKIndicator sof = new StochasticOscillatorKIndicator(series, 14);
        SMAIndicator sma = new SMAIndicator(sof, 3);
        StochasticOscillatorDIndicator sos = new StochasticOscillatorDIndicator(sma);

        
        
        // Ok, now let's building our trading rules!

        // Buying rules
        // We want to buy:
        //  - if the 5-ticks SMA crosses over 30-ticks SMA
        //  - and if the K goes above D
        Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma)
                .and(new CrossedUpIndicatorRule(sof, sos))
//                .and(new UnderIndicatorRule(rsi, Decimal.valueOf("50")));
               .and(new OverIndicatorRule(rsi, Decimal.valueOf("20")))
               .or(new StopLossRule(closePrice, Decimal.valueOf("0.2")))
               .or(new StopGainRule(closePrice, Decimal.valueOf("0.3")));
        
        // Selling rules
        // We want to sell:
        //  - if the 5-ticks SMA crosses under 30-ticks SMA
        //  - or if if the price looses more than 3%
        //  - or if the price earns more than 2%
        Rule sellingRule = new CrossedDownIndicatorRule(shortSma, longSma)
        		.and(new CrossedDownIndicatorRule(sof, sos))
        		.and(new UnderIndicatorRule(rsi, Decimal.valueOf("50")))
                .or(new StopLossRule(closePrice, Decimal.valueOf("0.2")))
                .or(new StopGainRule(closePrice, Decimal.valueOf("0.3")));
        
        // Running our juicy trading strategy...
        TradingRecord tradingRecord = series.run(new Strategy(buyingRule, sellingRule));
        System.out.println("Number of trades for our strategy: " + tradingRecord.getTradeCount());


        // Analysis

        // Getting the cash flow of the resulting trades
        CashFlow cashFlow = new CashFlow(series, tradingRecord);

        // Getting the profitable trades ratio
        AnalysisCriterion profitTradesRatio = new AverageProfitableTradesCriterion();
        System.out.println("Profitable trades ratio: " + profitTradesRatio.calculate(series, tradingRecord));
        // Getting the reward-risk ratio
        AnalysisCriterion rewardRiskRatio = new RewardRiskRatioCriterion();
        System.out.println("Reward-risk ratio: " + rewardRiskRatio.calculate(series, tradingRecord));

        // Total profit of our strategy
        // vs total profit of a buy-and-hold strategy
        AnalysisCriterion vsBuyAndHold = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
        System.out.println("Our profit vs buy-and-hold profit: " + vsBuyAndHold.calculate(series, tradingRecord));

        // Your turn!
//        System.exit(0);
    
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
				apiDemoHost.controller().reqHistoricalData(currencyContract, endTime, durationInMonths, DurationUnit.MONTH, BarSize._5_mins, WhatToShow.MIDPOINT, true, forexHistoricalHandler);
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
