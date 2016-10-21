package apidemo;



import eu.verdelhan.ta4j.AnalysisCriterion;

/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2016 Marc de Verdelhan & respective authors (see AUTHORS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Order;
import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.Trade;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.analysis.CashFlow;
import eu.verdelhan.ta4j.analysis.criteria.AverageProfitableTradesCriterion;
import eu.verdelhan.ta4j.analysis.criteria.RewardRiskRatioCriterion;
import eu.verdelhan.ta4j.analysis.criteria.TotalProfitCriterion;
import eu.verdelhan.ta4j.analysis.criteria.VersusBuyAndHoldCriterion;
import eu.verdelhan.ta4j.indicators.oscillators.StochasticOscillatorDIndicator;
import eu.verdelhan.ta4j.indicators.oscillators.StochasticOscillatorKIndicator;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.simple.FixedBooleanIndicator;
import eu.verdelhan.ta4j.indicators.simple.MaxPriceIndicator;
import eu.verdelhan.ta4j.indicators.simple.MinPriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.RSIIndicator;
import eu.verdelhan.ta4j.indicators.trackers.SMAIndicator;
import eu.verdelhan.ta4j.trading.rules.BooleanIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.BooleanRule;
import eu.verdelhan.ta4j.trading.rules.CrossedDownIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.CrossedUpIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.OverIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.StopGainRule;
import eu.verdelhan.ta4j.trading.rules.StopLossRule;
import eu.verdelhan.ta4j.trading.rules.UnderIndicatorRule;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.joda.time.DateTime;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.TechnicalSignalTrend;
import com.ib.client.Types.WhatToShow;
import com.ib.controller.Bar;
import com.ib.controller.ApiController.IHistoricalDataHandler;

import ta4jexamples.indicators.TimeframeSettings;
import ta4jexamples.loaders.CsvTradesLoader;

/**
 * This class is an example of a dummy trading bot using ta4j.
 * <p>
 */
public class TechinicalAnalyzerTrader extends Thread{
	private final boolean PRINT_OUT_MESSAGE = true;
	private final int fastSMAPeriod = 7;
	private final int slowSMAPeriod = 13;
	private final int priceToUse = 0; //0: closePrice, 1: medianPrice, 2: typicalPrice


	/** Close price of the last tick */
	private static Decimal LAST_TICK_CLOSE_PRICE;
	private Contract currencyContractHost;
	private static Boolean historicalDataEnd;
	private ApiDemo apiDemoHost;
	private boolean newTickAvailable;
	private ConcurrentHashMap<Long, forex> orderHashMapHost;
	private ConcurrentHashMap<String, Contract> contractHashMapHost;
	private final int TICK_LIMIT = 5000;



	final SimpleDateFormat DATEOnly_FORMAT = new SimpleDateFormat("yyyyMMdd");
	final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd HH:mm");
	final SimpleDateFormat TIMEOnly_FORMAT = new SimpleDateFormat("HH:mm:ss");
	private ConcurrentHashMap<Long, Bar> shortBarHashMap, mediumBarHashMap, longBarHashMap;
	private ConcurrentHashMap<Long, Bar> extraBarHashMap;
	private ConcurrentHashMap<Long, Bar> dailyBarHashMap;



	public TechinicalAnalyzerTrader(ApiDemo apiDemo, Contract currencyContract, ConcurrentHashMap<String, Contract> contractHashMap , ConcurrentHashMap<Long, forex> orderHashMap){
		currencyContractHost = currencyContract;
		apiDemoHost = apiDemo;
		orderHashMapHost = orderHashMap;
		contractHashMapHost = contractHashMap;

		shortBarHashMap = currencyContract.historical5MBarMap;
		mediumBarHashMap = currencyContract.historical15MBarMap;
		longBarHashMap = currencyContract.historicalHourBarMap;
		extraBarHashMap = currencyContract.historical4HourBarMap;
		dailyBarHashMap = currencyContract.historicalDailyBarMap;




		System.out.println("**********************Techinical Analyzer Trader for " +currencyContract.symbol() + currencyContract.currency() + " Initialization **********************");

	}

	private DateTime nextTickRunTime(Tick currentTick, int duration){

		return currentTick.getEndTime().plusMinutes(duration);	


	}

	public synchronized  void run(){
		//();

		Tick newTick  = null;
		Bar bar = null;
		TimeframeSettings shortTimeFrameSetting = null;
		TimeframeSettings medianTimeFrameSetting = null;
		TimeframeSettings longTimeFrameSetting = null;
		TimeframeSettings extraTimeFrameSetting = null;
		TimeframeSettings dailyTimeFrameSetting = null;

		Tick lastLongTick = null;
		Tick lastMedianTick = null;
		Tick lastShortTick = null;
		Tick lastExtraTick = null;
		Tick lastDailyTick = null;


		TechnicalAnalyzerResult shortTAResult = null;
		TechnicalAnalyzerResult medianTAResult = null;
		TechnicalAnalyzerResult longTAResult = null;
		TechnicalAnalyzerResult extraTAResult = null;
		TechnicalAnalyzerResult dailyTAResult = null;


		TechinicalAnalyzer techAnalyzerDaily = new TechinicalAnalyzer(ApiDemo.INSTANCE, currencyContractHost,contractHashMapHost, orderHashMapHost, 1440, dailyBarHashMap, 200);

		TechinicalAnalyzer techAnalyzerExtra = new TechinicalAnalyzer(ApiDemo.INSTANCE, currencyContractHost,contractHashMapHost, orderHashMapHost, 240, extraBarHashMap, 400);


		TechinicalAnalyzer techAnalyzerLong = new TechinicalAnalyzer(ApiDemo.INSTANCE, currencyContractHost,contractHashMapHost, orderHashMapHost, 60, longBarHashMap, 1000);

		TechinicalAnalyzer techAnalyzerMedium = new TechinicalAnalyzer(ApiDemo.INSTANCE, currencyContractHost,contractHashMapHost, orderHashMapHost, 15, mediumBarHashMap, 2000);



		TechinicalAnalyzer techAnalyzerShort = new TechinicalAnalyzer(ApiDemo.INSTANCE, currencyContractHost,contractHashMapHost, orderHashMapHost, 5, shortBarHashMap, 6000);






		//	while(true)
		{


			System.out.println("**********************Techinical Analyzer Trader " + currencyContractHost.symbol() + currencyContractHost.currency() + " Running **********************");


			//			try {
			//				{
			////					currencyContractHost.tickLatch60M.await();
			////					currencyContractHost.tickLatch60M.reset();
			//
			//				}
			//				
			//			} catch (InterruptedException e1) {
			//				// TODO Auto-generated catch block
			//				e1.printStackTrace();
			//			}
			//
			//			try {
			//				{
			////					currencyContractHost.tickLatch15M.await();
			////					currencyContractHost.tickLatch15M.reset();
			//					
			//				}
			//				
			//			} catch (InterruptedException e1) {
			//				// TODO Auto-generated catch block
			//				e1.printStackTrace();
			//			}

			try {
				{
					currencyContractHost.tickLatch5M.await();
					currencyContractHost.tickLatch5M.reset();
					dailyTimeFrameSetting = techAnalyzerDaily.initDB(fastSMAPeriod, slowSMAPeriod, priceToUse);
					extraTimeFrameSetting = techAnalyzerExtra.initDB(fastSMAPeriod, slowSMAPeriod, priceToUse);
					longTimeFrameSetting = techAnalyzerLong.initDB(fastSMAPeriod, slowSMAPeriod, priceToUse);
					medianTimeFrameSetting = techAnalyzerMedium.initDB(fastSMAPeriod, slowSMAPeriod, priceToUse);
					shortTimeFrameSetting = techAnalyzerShort.initDB(fastSMAPeriod, slowSMAPeriod, priceToUse);


					lastDailyTick = dailyTimeFrameSetting.lastTick;
					lastExtraTick = extraTimeFrameSetting.lastTick;
					lastLongTick = longTimeFrameSetting.lastTick;
					lastMedianTick = medianTimeFrameSetting.lastTick;
					lastShortTick = shortTimeFrameSetting.lastTick;

				}

			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}



			//			shortBarHashMap.clear();
			//			mediumBarHashMap.clear();
			//			longBarHashMap.clear();

			// Building the trading strategy
			Strategy dailyLongStrategy = buildLongStrategy(dailyTimeFrameSetting.series, dailyTimeFrameSetting, 1440);
			Strategy dailyShortStrategy = buildShortStrategy(dailyTimeFrameSetting.series, dailyTimeFrameSetting, 1440);

			Strategy extraLongStrategy = buildLongStrategy(extraTimeFrameSetting.series, extraTimeFrameSetting, 240);
			Strategy extraShortStrategy = buildShortStrategy(extraTimeFrameSetting.series, extraTimeFrameSetting, 240);

			Strategy longLongStrategy = buildLongStrategy(longTimeFrameSetting.series, longTimeFrameSetting, 60);
			Strategy longShortStrategy = buildShortStrategy(longTimeFrameSetting.series, longTimeFrameSetting, 60);

			Strategy medianLongStrategy = buildLongStrategy(medianTimeFrameSetting.series, medianTimeFrameSetting, 15);
			Strategy medianShortStrategy = buildShortStrategy(medianTimeFrameSetting.series, medianTimeFrameSetting, 15);

			Strategy shortLongStrategy = buildLongStrategy(shortTimeFrameSetting.series, shortTimeFrameSetting, 5);
			Strategy shortShortStrategy = buildShortStrategy(shortTimeFrameSetting.series, shortTimeFrameSetting, 5);

			newTickAvailable = true;
			// Initializing the trading history
			TradingRecord longtradingRecord = new TradingRecord();
			TradingRecord shorttradingRecord = new TradingRecord();

			System.out.println("************************************************************");

			/**
			 * We run the strategy for the 50 next ticks.
			 */
			while (true) 
			{

				// wait for New tick
				if((shortBarHashMap.size() > 0 && newTickAvailable) == false)
				{
					try {
						{
							currencyContractHost.tickLatch5M.await();
							currencyContractHost.tickLatch5M.reset();
						}

					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

				}



				if(shortBarHashMap.size() == 0 || nextTickRunTime(lastShortTick, 15).toDate().after(new Date()))
					newTickAvailable = false;


				if(newTickAvailable == false || nextTickRunTime(lastShortTick, 5).isAfter(nextTickRunTime(lastDailyTick, 1440)))
				{	
					dailyTAResult = techAnalyzerDaily.analyze(dailyLongStrategy, dailyShortStrategy, lastDailyTick.getEndTime().toDate(), PRINT_OUT_MESSAGE);	
					lastDailyTick = dailyTAResult.processedTick;					
				}

				if(newTickAvailable == false || nextTickRunTime(lastShortTick, 5).isAfter(nextTickRunTime(lastExtraTick, 240)))
				{	
					extraTAResult = techAnalyzerExtra.analyze(extraLongStrategy, extraShortStrategy, lastExtraTick.getEndTime().toDate(), PRINT_OUT_MESSAGE);	
					lastExtraTick = extraTAResult.processedTick;

				}

				if(newTickAvailable == false || nextTickRunTime(lastShortTick, 5).isAfter(nextTickRunTime(lastLongTick, 60)))
				{	
					longTAResult = techAnalyzerLong.analyze(longLongStrategy, longShortStrategy, lastLongTick.getEndTime().toDate(), PRINT_OUT_MESSAGE);
					lastLongTick = longTAResult.processedTick;
				}

				if(newTickAvailable == false || nextTickRunTime(lastShortTick, 5).isAfter(nextTickRunTime(lastMedianTick, 15)))
				{	
					medianTAResult = techAnalyzerMedium.analyze(medianLongStrategy, medianShortStrategy, lastMedianTick.getEndTime().toDate(), PRINT_OUT_MESSAGE);
					lastMedianTick = medianTAResult.processedTick;
				}

				shortTAResult = techAnalyzerShort.analyze(shortLongStrategy, shortShortStrategy, lastShortTick.getEndTime().toDate(), PRINT_OUT_MESSAGE);
				lastShortTick = shortTAResult.processedTick;








				if(dailyTAResult == null || extraTAResult == null || longTAResult == null || medianTAResult == null || shortTAResult == null)
				{
					continue;					
				}

				if(PRINT_OUT_MESSAGE){	
					//Print out result for analysis purpose
					System.out.println(new Date() + " ******Technical analysis result **** " );




					System.out.println(lastDailyTick.getDateName() + " ******Technical analysis Daily result for LONG entry and exit**** " +  " Signal: " + dailyTAResult.technicalSignalLongEntry + dailyTAResult.technicalSignalLongEntry + " longSMA:  " + dailyTAResult.longSMA + " shortSMA: " + dailyTAResult.shortSMA);
					System.out.println(lastExtraTick.getDateName() + " ******Technical analysis Extra result for LONG entry and exit**** " +  " Signal: " + extraTAResult.technicalSignalLongEntry + extraTAResult.technicalSignalLongExit + " longSMA:  " + extraTAResult.longSMA + " shortSMA: " + extraTAResult.shortSMA);
					System.out.println(lastLongTick.getDateName() + " ******Technical analysis Long result for LONG entry and exit **** " +  " Signal: " + longTAResult.technicalSignalLongEntry + longTAResult.technicalSignalLongExit + " longSMA:  " + longTAResult.longSMA + " shortSMA: " + longTAResult.shortSMA);
					System.out.println(lastMedianTick.getDateName() + " ******Technical analysis Medium result for LONG entry and exit **** " +  " Signal: " + medianTAResult.technicalSignalLongEntry + medianTAResult.technicalSignalLongExit + " longSMA:  " + medianTAResult.longSMA + " shortSMA: " + medianTAResult.shortSMA);
					System.out.println(lastShortTick.getDateName() + " ******Technical analysis Short result for LONG entry and exit **** " +  " Signal: " + shortTAResult.technicalSignalLongEntry + shortTAResult.technicalSignalLongExit + " longSMA:  " + shortTAResult.longSMA + " shortSMA: " + shortTAResult.shortSMA);


					System.out.println(lastDailyTick.getDateName() + " ******Technical analysis Daily result for SHORT entry and exit**** " +  " Signal: " + dailyTAResult.technicalSignalLongEntry + dailyTAResult.technicalSignalLongEntry + " longSMA:  " + dailyTAResult.longSMA + " shortSMA: " + dailyTAResult.shortSMA);
					System.out.println(lastExtraTick.getDateName() + " ******Technical analysis Extra result for SHORT entry and exit**** " +  " Signal: " + extraTAResult.technicalSignalLongEntry + extraTAResult.technicalSignalLongExit + " longSMA:  " + extraTAResult.longSMA + " shortSMA: " + extraTAResult.shortSMA);
					System.out.println(lastLongTick.getDateName() + " ******Technical analysis Long result for SHORT entry and exit **** " +  " Signal: " + longTAResult.technicalSignalShortEntry + longTAResult.technicalSignalShortExit + " longSMA:  " + longTAResult.longSMA + " shortSMA: " + longTAResult.shortSMA);
					System.out.println(lastMedianTick.getDateName() + " ******Technical analysis Medium result for SHORT entry and exit **** " +  " Signal: " + medianTAResult.technicalSignalShortEntry + medianTAResult.technicalSignalShortExit + " longSMA:  " + medianTAResult.longSMA + " shortSMA: " + medianTAResult.shortSMA);
					System.out.println(lastShortTick.getDateName() + " ******Technical analysis Short result for SHORT entry and exit **** " +  " Signal: " + shortTAResult.technicalSignalShortEntry + shortTAResult.technicalSignalShortExit + " longSMA:  " + shortTAResult.longSMA + " shortSMA: " + shortTAResult.shortSMA);





				}


				//Entry condition for long.
				//Daily is long entry true
				//Extra in long entry true
				//If one of pattern is true.

				boolean entryRule;

				entryRule = ( dailyTAResult.technicalSignalLongEntry)
						&&(extraTAResult.technicalSignalLongEntry)
						||((longTAResult.technicalSignalLongEntry))
						||( (medianTAResult.technicalSignalLongEntry))		
						||((shortTAResult.technicalSignalLongEntry))		

						

						;


				//Now exit rule only one. which is daily signal change.
				//We should add more later.
				boolean exitRule;

				exitRule = ( (dailyTAResult.technicalSignalLongExit)
						//						.or(new BooleanRule(extraTAResult.technicalSignalLongEntry))
						//						.or(new BooleanRule(longTAResult.technicalSignalLongEntry))
						//						.or(new BooleanRule(medianTAResult.technicalSignalLongEntry))		
						//						.or(new BooleanRule(shortTAResult.technicalSignalLongEntry))		

						)

						;


				// Signals
				// Buy when SMA goes over close price
				// Sell when close price goes over SMA
	//			Strategy longSignals = new Strategy(entryRule, exitRule);




				if(entryRule && longtradingRecord.isClosed())
				{
					System.out.println(lastShortTick.getDateName() + " place order to Buy now @ " + lastShortTick.getClosePrice());
					placeTestMarketOrder("BUY", lastShortTick);
					longtradingRecord.enter(shortTAResult.endIndex, lastShortTick.getClosePrice(), Decimal.valueOf(25000));
				}




				//If extra change trend, time to close
				if(exitRule &&  !longtradingRecord.isClosed()){
					placeTestMarketOrder("CLOSE", lastShortTick);
					longtradingRecord.exit(shortTAResult.endIndex, lastShortTick.getClosePrice(), Decimal.valueOf(25000));
					System.out.println(lastShortTick.getDateName() + " Close Long order to now @ " + lastShortTick.getClosePrice());
				}



				//Entry condition for Short.
				//Extra in Down trend.
				//Long from up to down, then to up again. Now it is time to sell.
				//Then use medium or short as entry point.









				analyzeRecord(shortTAResult.series, longtradingRecord, shorttradingRecord);

				//				//If processed tick is ealier, don't submit the order.
				//				Calendar cal = Calendar.getInstance();
				//				cal.setTime(shortTAResult.processedTick.getEndTime().toDate());
				//				cal.add(Calendar.MINUTE, +5);
				//				if(new Date().after(cal.getTime())){
				//					continue;
				//				}






				//                
			}

		}

	}

	private void analyzeRecord(TimeSeries series, TradingRecord longtradingRecord, TradingRecord shorttradingRecord){
		// Analysis for long


		//Only output data when it finished

		Date startDate = null;
		Date endDate = null;
		//	final SimpleDateFormat DATEOnly_FORMAT = new SimpleDateFormat("yyyyMMdd");
		try {
			startDate = DATE_FORMAT.parse("20160930 23:50");
			endDate = new Date(); //DATE_FORMAT.parse("20161001 00:00");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//		//only process date between startDate and endDate
		if(new DateTime(series.getLastTick().getEndTime()).toDate().before(startDate)){
			//		System.out.println("First Tick to process time: " + startDate);
			return;
		}

		//		//only process date between startDate and endDate
		if(new DateTime(series.getLastTick().getEndTime()).toDate().after(endDate)){
			//			System.out.println("Last Tick to process time: " + endDate);
			return;
		}



		// Getting the cash flow of the resulting trades
		CashFlow cashFlow = new CashFlow(series, longtradingRecord);

		System.out.println("Number of trades for our Long strategy: " + longtradingRecord.getTradeCount());

		//					            List<Trade> List = longtradingRecord.getTrades();
		//					            Iterator<Trade> tradeIterator = List.iterator();
		//					            Order record;
		//					            Tick tick;
		//					            while(tradeIterator.hasNext()){
		//					            	
		//					            	record = tradeIterator.next().getEntry();
		//					            	record.getPrice();
		//					            	record.getType();
		//					            	tick = series.getTick(record.getIndex());
		//					            	System.out.println(tick.getDateName() + " " + record.getType() + " @ " + record.getPrice());
		//					            }

		// Getting the profitable trades ratio
		AnalysisCriterion profitTradesRatio = new AverageProfitableTradesCriterion();
		System.out.println("Profitable trades ratio: " + profitTradesRatio.calculate(series, longtradingRecord));
		// Getting the reward-risk ratio
		AnalysisCriterion rewardRiskRatio = new RewardRiskRatioCriterion();
		System.out.println("Reward-risk ratio: " + rewardRiskRatio.calculate(series, longtradingRecord));

		// Total profit of our strategy
		// vs total profit of a buy-and-hold strategy
		AnalysisCriterion vsBuyAndHold = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
		System.out.println("Our profit vs buy-and-hold profit: " + vsBuyAndHold.calculate(series, longtradingRecord));


		// Analysis

		// Getting the cash flow of the resulting trades
		cashFlow = new CashFlow(series, shorttradingRecord);

		System.out.println("Number of trades for our Short strategy: " + shorttradingRecord.getTradeCount());

		// Getting the profitable trades ratio
		profitTradesRatio = new AverageProfitableTradesCriterion();
		System.out.println("Profitable trades ratio: " + profitTradesRatio.calculate(series, shorttradingRecord));
		// Getting the reward-risk ratio
		rewardRiskRatio = new RewardRiskRatioCriterion();
		System.out.println("Reward-risk ratio: " + rewardRiskRatio.calculate(series, shorttradingRecord));

		// Total profit of our strategy
		// vs total profit of a buy-and-hold strategy
		vsBuyAndHold = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
		System.out.println("Our profit vs buy-and-hold profit: " + vsBuyAndHold.calculate(series, shorttradingRecord));


	}


	private void placeTestMarketOrder(String action, Tick newTick){


		//If processed tick is earlier which means it is buidling up data base only, don't submit the order.
		Calendar cal = Calendar.getInstance();
		cal.setTime(newTick.getEndTime().toDate());
		cal.add(Calendar.MINUTE, +10);
		if(new Date().after(cal.getTime())){
			return;
		}


		//First check whether this order has been submitted before. If yes, return.
		forex orderDetail = null;
		Date currentTime = newTick.getEndTime().toDate();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		String orderDateStr = formatter.format(currentTime);
		long orderSeqNo = Long.parseLong(orderDateStr + "00");
		while(orderHashMapHost.containsKey(orderSeqNo)){
			orderDetail = orderHashMapHost.get(orderSeqNo);
			if(orderDetail.Symbol.equals(currencyContractHost.symbol() + currencyContractHost.currency()) && orderDetail.TradeMethod.equals(action) && orderDetail.EntryMethod.equals("MKT"))
				return;

			orderSeqNo++;
		}

		orderDetail = new forex();
		orderDetail.Symbol = currencyContractHost.symbol() + currencyContractHost.currency();
		orderDetail.TradeMethod = action;
		orderDetail.EntryMethod = "MKT";
		orderDetail.Quantity = "25000";
		orderDetail.ValidDuration = "120";
		orderDetail.TriggerPct = "0";
		orderDetail.LossPct = "0.2";
		orderDetail.groupId = (long) 0;
		orderDetail.orderSeqNo = orderSeqNo;
		orderDetail.Date = DATEOnly_FORMAT.format(currentTime);
		orderDetail.Time = TIMEOnly_FORMAT.format(currentTime);


		orderHashMapHost.put(orderDetail.orderSeqNo, orderDetail);
		//       
		//	       apiDemoHost.placeMarketOrder(orderDetail);


		//		currencyContractHost.m_currentTechnicalSignal = action;
		//		ContractHashMapHost.put(orderDetail.Symbol, currencyContractHost);
		System.out.println(new Date() + " Market order placed for " + orderDetail.Symbol); 
	}

	/**
	 * @param series a time series
	 * @param timeFrameSetting 
	 * @return a dummy strategy
	 */
	private Strategy buildLongStrategy(TimeSeries series, TimeframeSettings timeFrameSetting, int duration) {
		if (series == null) {
			throw new IllegalArgumentException("Series cannot be null");
		}

		Rule entryRule = null;
		Rule exitRule = null;

		// Ok, now let's building our trading rules for daily!

		// Buying rules
		// We want to buy:
		//  - if the short SMA over long SMA
		//  - and MACD is above emaMacd
		// RSI is not oversold and overbought
		if(duration == 1440)
		{
			entryRule = ( new OverIndicatorRule(timeFrameSetting.shortSMA, timeFrameSetting.longSMA).or(new OverIndicatorRule(timeFrameSetting.closePrice, timeFrameSetting.shortSMA)))
					.and( new OverIndicatorRule(timeFrameSetting.macd, timeFrameSetting.emaMacd))
					.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")));
		}

		// Selling rules
		// We want to sell:
		//  - if the short SMA under long SMA
		//  - and MACD is below emaMacd
		if(duration == 1440)
		{
			exitRule = ( new UnderIndicatorRule(timeFrameSetting.shortSMA, timeFrameSetting.longSMA).or(new UnderIndicatorRule(timeFrameSetting.closePrice, timeFrameSetting.shortSMA)))
					.and( new UnderIndicatorRule(timeFrameSetting.macd, timeFrameSetting.emaMacd))
					.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")));
		}


		//Four hour rules. Use same rules as daily
		// Buying rules
		// We want to buy:
		//  - if the short SMA over long SMA
		//  - and MACD is above emaMacd
		// RSI is not oversold and overbought
		if(duration == 240)
		{
			entryRule = ( new OverIndicatorRule(timeFrameSetting.shortSMA, timeFrameSetting.longSMA).or(new OverIndicatorRule(timeFrameSetting.closePrice, timeFrameSetting.shortSMA)))
					.and( new OverIndicatorRule(timeFrameSetting.macd, timeFrameSetting.emaMacd))
					.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")));
		}

		// Selling rules
		// We want to sell:
		//  - if the short SMA under long SMA
		//  - and MACD is below emaMacd
		if(duration == 240)
		{
			exitRule = ( new UnderIndicatorRule(timeFrameSetting.shortSMA, timeFrameSetting.longSMA).or(new UnderIndicatorRule(timeFrameSetting.closePrice, timeFrameSetting.shortSMA)))
					.and( new UnderIndicatorRule(timeFrameSetting.macd, timeFrameSetting.emaMacd))
					.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")));
		}


		//Four hour rules. Use same rules as daily now. But we should fine tune it later.
		// Buying rules
		// We want to buy:
		//  - if the short SMA over long SMA
		//  - and MACD is above emaMacd
		// RSI is not oversold and overbought
		if(duration == 60)
		{
			entryRule = ( new OverIndicatorRule(timeFrameSetting.shortSMA, timeFrameSetting.longSMA).or(new OverIndicatorRule(timeFrameSetting.closePrice, timeFrameSetting.shortSMA)))
					.and( new OverIndicatorRule(timeFrameSetting.macd, timeFrameSetting.emaMacd))
					.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")));
		}

		// Selling rules
		// We want to sell:
		//  - if the short SMA under long SMA
		//  - and MACD is below emaMacd
		if(duration == 60)
		{
			exitRule = ( new UnderIndicatorRule(timeFrameSetting.shortSMA, timeFrameSetting.longSMA).or(new UnderIndicatorRule(timeFrameSetting.closePrice, timeFrameSetting.shortSMA)))
					.and( new UnderIndicatorRule(timeFrameSetting.macd, timeFrameSetting.emaMacd))
					.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")));
		}


		//15 minutes and 5 minutes
		// Buying rules
		// We want to entery the trade:
		//  use patern to enter the trade
		if(duration == 15 || duration == 5 )
		{
			entryRule = ( new BooleanIndicatorRule(timeFrameSetting.bullishEngulfingIndicator)
					.or(new BooleanIndicatorRule(timeFrameSetting.bullishHaramiIndicator))
					.or(new BooleanIndicatorRule(timeFrameSetting.dojiIndicator))
					.or(new BooleanIndicatorRule(timeFrameSetting.threeWhiteSoldiersIndicator))		

					)
					//							.and( new OverIndicatorRule(timeFrameSetting.macd, timeFrameSetting.emaMacd))
					//							.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
					//							.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")))
					;
		}		

		if(duration == 15 || duration == 5 )
		{
			exitRule = ( new UnderIndicatorRule(timeFrameSetting.shortSMA, timeFrameSetting.longSMA).or(new UnderIndicatorRule(timeFrameSetting.closePrice, timeFrameSetting.shortSMA)))
					.and( new UnderIndicatorRule(timeFrameSetting.macd, timeFrameSetting.emaMacd))
					.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")));
		}		
		
		

		// Signals
		// Buy when SMA goes over close price
		// Sell when close price goes over SMA
		Strategy buySellSignals = new Strategy(entryRule, exitRule);
		return buySellSignals;
	}



	/**
	 * @param series a time series
	 * @param timeFrameSetting 
	 * @return a dummy strategy
	 */
	private Strategy buildShortStrategy(TimeSeries series, TimeframeSettings timeFrameSetting, int duration) {
		if (series == null) {
			throw new IllegalArgumentException("Series cannot be null");
		}

		Rule entryRule = null;
		Rule exitRule = null;

		// Ok, now let's building our trading rules for daily!

		// Buying rules
		// We want to buy:
		//  - if the short SMA over long SMA
		//  - and MACD is above emaMacd
		// RSI is not oversold and overbought
		if(duration == 1440)
		{
			exitRule = ( new OverIndicatorRule(timeFrameSetting.shortSMA, timeFrameSetting.longSMA).or(new OverIndicatorRule(timeFrameSetting.closePrice, timeFrameSetting.shortSMA)))
					.and( new OverIndicatorRule(timeFrameSetting.macd, timeFrameSetting.emaMacd))
					.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")));
		}

		// Selling rules
		// We want to sell:
		//  - if the short SMA under long SMA
		//  - and MACD is below emaMacd
		if(duration == 1440)
		{
			entryRule = ( new UnderIndicatorRule(timeFrameSetting.shortSMA, timeFrameSetting.longSMA).or(new UnderIndicatorRule(timeFrameSetting.closePrice, timeFrameSetting.shortSMA)))
					.and( new UnderIndicatorRule(timeFrameSetting.macd, timeFrameSetting.emaMacd))
					.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")));
		}


		//Four hour rules. Use same rules as daily
		// Buying rules
		// We want to buy:
		//  - if the short SMA over long SMA
		//  - and MACD is above emaMacd
		// RSI is not oversold and overbought
		if(duration == 240)
		{
			exitRule = ( new OverIndicatorRule(timeFrameSetting.shortSMA, timeFrameSetting.longSMA).or(new OverIndicatorRule(timeFrameSetting.closePrice, timeFrameSetting.shortSMA)))
					.and( new OverIndicatorRule(timeFrameSetting.macd, timeFrameSetting.emaMacd))
					.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")));
		}

		// Selling rules
		// We want to sell:
		//  - if the short SMA under long SMA
		//  - and MACD is below emaMacd
		if(duration == 240)
		{
			entryRule = ( new UnderIndicatorRule(timeFrameSetting.shortSMA, timeFrameSetting.longSMA).or(new UnderIndicatorRule(timeFrameSetting.closePrice, timeFrameSetting.shortSMA)))
					.and( new UnderIndicatorRule(timeFrameSetting.macd, timeFrameSetting.emaMacd))
					.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")));
		}


		//Four hour rules. Use same rules as daily now. But we should fine tune it later.
		// Buying rules
		// We want to buy:
		//  - if the short SMA over long SMA
		//  - and MACD is above emaMacd
		// RSI is not oversold and overbought
		if(duration == 60)
		{
			exitRule = ( new OverIndicatorRule(timeFrameSetting.shortSMA, timeFrameSetting.longSMA).or(new OverIndicatorRule(timeFrameSetting.closePrice, timeFrameSetting.shortSMA)))
					.and( new OverIndicatorRule(timeFrameSetting.macd, timeFrameSetting.emaMacd))
					.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")));
		}

		// Selling rules
		// We want to sell:
		//  - if the short SMA under long SMA
		//  - and MACD is below emaMacd
		if(duration == 60)
		{
			entryRule = ( new UnderIndicatorRule(timeFrameSetting.shortSMA, timeFrameSetting.longSMA).or(new UnderIndicatorRule(timeFrameSetting.closePrice, timeFrameSetting.shortSMA)))
					.and( new UnderIndicatorRule(timeFrameSetting.macd, timeFrameSetting.emaMacd))
					.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")));
		}


		//Use pattern to recognize reversal on 15 and 5 minutes time frame.



		//15 minutes and 5 minutes
		// Buying rules
		// We want to entery the trade:
		//  use patern to enter the trade
		if(duration == 15 || duration == 5 )
		{
			entryRule = ( new BooleanIndicatorRule(timeFrameSetting.bearishEngulfingIndicator)
					.or(new BooleanIndicatorRule(timeFrameSetting.bearishHaramiIndicator))
					.or(new BooleanIndicatorRule(timeFrameSetting.dojiIndicator))
					.or(new BooleanIndicatorRule(timeFrameSetting.threeBlackCrowsIndicator))		

					)
					//							.and( new OverIndicatorRule(timeFrameSetting.macd, timeFrameSetting.emaMacd))
					//							.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
					//							.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")))
					;
		}	
		
		if(duration == 15 || duration == 5 )
		{	
		exitRule = ( new OverIndicatorRule(timeFrameSetting.shortSMA, timeFrameSetting.longSMA).or(new OverIndicatorRule(timeFrameSetting.closePrice, timeFrameSetting.shortSMA)))
				.and( new OverIndicatorRule(timeFrameSetting.macd, timeFrameSetting.emaMacd))
				.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
				.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")));
		}
		
		Strategy buySellSignals = new Strategy(entryRule, exitRule);
		return buySellSignals;
	}



}

