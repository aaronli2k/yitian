package apidemo;



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
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.analysis.CashFlow;
import eu.verdelhan.ta4j.analysis.criteria.AverageProfitableTradesCriterion;
import eu.verdelhan.ta4j.analysis.criteria.RewardRiskRatioCriterion;
import eu.verdelhan.ta4j.analysis.criteria.TotalProfitCriterion;
import eu.verdelhan.ta4j.analysis.criteria.VersusBuyAndHoldCriterion;
import eu.verdelhan.ta4j.indicators.oscillators.StochasticOscillatorDIndicator;
import eu.verdelhan.ta4j.indicators.oscillators.StochasticOscillatorKIndicator;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.simple.MaxPriceIndicator;
import eu.verdelhan.ta4j.indicators.simple.MinPriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.RSIIndicator;
import eu.verdelhan.ta4j.indicators.trackers.SMAIndicator;
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
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.joda.time.DateTime;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.WhatToShow;
import com.ib.controller.Bar;
import com.ib.controller.ApiController.IHistoricalDataHandler;

import ta4jexamples.loaders.CsvTradesLoader;

/**
 * This class is an example of a dummy trading bot using ta4j.
 * <p>
 */
public class TechinicalAnalyzerTrader extends Thread{

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
	final SimpleDateFormat TIMEOnly_FORMAT = new SimpleDateFormat("HH:mm:ss");
	private ConcurrentHashMap<Long, Bar> shortBarHashMap, mediumBarHashMap, longBarHashMap;
	private String shortTechnicalSignal, mediumTechnicalSignal, longTechnicalSignal, extraTechnicalSignal;
	private ConcurrentHashMap<Long, Bar> extraBarHashMap;
  


	public TechinicalAnalyzerTrader(ApiDemo apiDemo, Contract currencyContract, ConcurrentHashMap<String, Contract> contractHashMap , ConcurrentHashMap<Long, forex> orderHashMap){
		currencyContractHost = currencyContract;
		apiDemoHost = apiDemo;
		orderHashMapHost = orderHashMap;
		contractHashMapHost = contractHashMap;
		
		shortBarHashMap = currencyContract.historical5MBarMap;
		mediumBarHashMap = currencyContract.historical15MBarMap;
		longBarHashMap = currencyContract.historicalHourBarMap;
		extraBarHashMap = currencyContract.historical4HourBarMap;
		
		shortTechnicalSignal = currencyContract.m_currentTechnicalSignal5M;
		
		mediumTechnicalSignal = currencyContract.m_currentTechnicalSignal15M;
	
		longTechnicalSignal = currencyContract.m_currentTechnicalSignal60M;

		extraTechnicalSignal = currencyContract.m_currentTechnicalSignal240M;

	
		System.out.println("**********************Techinical Analyzer Trader for " +currencyContract.symbol() + currencyContract.currency() + " Initialization **********************");

	}

	private DateTime nextTickRunTime(Tick currentTick, int duration){
		
		return currentTick.getEndTime().plusMinutes(duration);	

		
	}
	
	public synchronized  void run(){
		//();

		Tick newTick  = null;
		Bar bar = null;
		Tick lastLongTick = null;
		Tick lastMediumTick = null;
		Tick lastShortTick = null;
		Tick lastExtraTick = null;
		
		TechnicalAnalyzerResult shortTAResult = null;
		TechnicalAnalyzerResult mediumTAResult = null;
		TechnicalAnalyzerResult longTAResult = null;
		TechnicalAnalyzerResult extraTAResult = null;
		
		String pendingAction = "WAIT";

		
		TechinicalAnalyzer techAnalyzerExtra = new TechinicalAnalyzer(ApiDemo.INSTANCE, currencyContractHost,contractHashMapHost, orderHashMapHost, 240, extraBarHashMap, 300);

		
		TechinicalAnalyzer techAnalyzerLong = new TechinicalAnalyzer(ApiDemo.INSTANCE, currencyContractHost,contractHashMapHost, orderHashMapHost, 60, longBarHashMap, 800);
		
		TechinicalAnalyzer techAnalyzerMedium = new TechinicalAnalyzer(ApiDemo.INSTANCE, currencyContractHost,contractHashMapHost, orderHashMapHost, 15, mediumBarHashMap, 2000);

	  

		TechinicalAnalyzer techAnalyzerShort = new TechinicalAnalyzer(ApiDemo.INSTANCE, currencyContractHost,contractHashMapHost, orderHashMapHost, 5, shortBarHashMap, 5000);

	
   
		
		
		
	//	while(true)
		{


			System.out.println("**********************Techinical Analyzer Trader " + currencyContractHost.symbol() + currencyContractHost.currency() + " Running **********************");
			// Getting the time series

			//            //Simulated testing for different dataset.
			//            TicksAccesser ticksAccess = new TicksAccesser(null);
			//            currencyContractHost.historicalBarMap = ticksAccess.readFromCsv("NZDUSD_ticks_history_2007_to_2016.csv");
			//			ticksAccess.start();

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
					lastExtraTick = techAnalyzerExtra.initDB();
					lastLongTick = techAnalyzerLong.initDB();
					lastMediumTick = techAnalyzerMedium.initDB();
					lastShortTick = techAnalyzerShort.initDB();
				}
				
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			
			
//			shortBarHashMap.clear();
//			mediumBarHashMap.clear();
//			longBarHashMap.clear();
			


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
				

				
				shortTAResult = techAnalyzerShort.analyze(lastShortTick.getEndTime().toDate());
				lastShortTick = shortTAResult.processedTick;
				
				if(newTickAvailable == false || lastShortTick.getEndTime().getMinuteOfHour() % 15 == 0  || nextTickRunTime(lastShortTick, 5).isAfter(nextTickRunTime(lastMediumTick, 15)))
				{	
					mediumTAResult = techAnalyzerMedium.analyze(lastMediumTick.getEndTime().toDate());
					lastMediumTick = mediumTAResult.processedTick;
				}
				if(newTickAvailable == false || lastShortTick.getEndTime().getMinuteOfHour() == 0 || nextTickRunTime(lastShortTick, 5).isAfter(nextTickRunTime(lastLongTick, 60)))
				{	
					longTAResult = techAnalyzerLong.analyze(lastLongTick.getEndTime().toDate());
					lastLongTick = longTAResult.processedTick;
				}

				if(newTickAvailable == false || lastShortTick.getEndTime().getMinuteOfHour() == 0 &&  nextTickRunTime(lastShortTick, 5).isAfter(nextTickRunTime(lastExtraTick, 240)))
				{	
					extraTAResult = techAnalyzerExtra.analyze(lastExtraTick.getEndTime().toDate());	
					lastExtraTick = extraTAResult.processedTick;
					
				}
				
				if(extraTAResult == null || longTAResult == null || mediumTAResult == null || shortTAResult == null)
				{
					continue;					
				}
				
				//Print out result for analysis purpose
				System.out.println(lastShortTick.getDateName() + " ******Technical analysis result **** " );
				
				System.out.println(lastShortTick.getDateName() + " ******Technical analysis Short result **** " +  " Signal: " + shortTAResult.technicalSignal + " longSMA:  " + shortTAResult.longSMA + " shortSMA: " + shortTAResult.shortSMA);
				
				System.out.println(lastMediumTick.getDateName() + " ******Technical analysis Medium result **** " +  " Signal: " + mediumTAResult.technicalSignal + " longSMA:  " + mediumTAResult.longSMA + " shortSMA: " + mediumTAResult.shortSMA);
				
				System.out.println(lastLongTick.getDateName() + " ******Technical analysis Long result **** " +  " Signal: " + longTAResult.technicalSignal + " longSMA:  " + longTAResult.longSMA + " shortSMA: " + longTAResult.shortSMA);
				
				System.out.println(lastExtraTick.getDateName() + " ******Technical analysis Extra result **** " +  " Signal: " + extraTAResult.technicalSignal + " longSMA:  " + extraTAResult.longSMA + " shortSMA: " + extraTAResult.shortSMA);
				

				//If processed tick is ealier, don't submit the order.
				Calendar cal = Calendar.getInstance();
				cal.setTime(lastShortTick.getEndTime().toDate());
				cal.add(Calendar.MINUTE, +5);
				if(new Date().after(cal.getTime())){
					continue;
					}
				
				//Entry condition for long.
				//Extra in Up trend.
				//Long from up to down, then to up again. Now it is time to buy.
				//Then use medium or short as entry point.
				
				
				if(extraTAResult.technicalSignal.equals("ENTER_LONG")){
					if(longTAResult.technicalSignal.equals("ENTER_LONG")){
						System.out.println(lastShortTick.getDateName() + " Up wait a good postion to Buy");

					//	if(currencyContractHost.m_currentTechnicalSignal60M.equals("DOWN"))
						{
							pendingAction = "BUY";
						}
					}
				}
				else if(extraTAResult.technicalSignal.equals("ENTER_SHORT")){
					if(longTAResult.technicalSignal.equals("ENTER_SHORT")){
						System.out.println(lastShortTick.getDateName() + " Down wait a good postion to sell");
			//			if(currencyContractHost.m_currentTechnicalSignal60M.equals("UP"))
						{
							pendingAction = "SELL";
						}
					}
				}
				
				
				
				if(pendingAction.equals("BUY")){
					if(mediumTAResult.technicalSignal.equals("ENTER_LONG")){
						if(!mediumTAResult.technicalSignal.equals("ENTER_LONG")){
							placeTestMarketOrder("BUY", lastShortTick);
						}						
					}
					{
						if(shortTAResult.technicalSignal.equals("ENTER_LONG")){
							if(!currencyContractHost.m_currentTechnicalSignal5M.equals("ENTER_SHORT")){
								placeTestMarketOrder("BUY", lastShortTick);
							}
						}
					}
				}
				else if(pendingAction.equals("SELL")){
					if(mediumTAResult.technicalSignal.equals("ENTER_SHORT")){
						if(!mediumTAResult.technicalSignal.equals("ENTER_LONG")){
							placeTestMarketOrder("SELL", lastShortTick);
						}						
					}
					{
						if(shortTAResult.technicalSignal.equals("ENTER_SHORT")){
							if(!currencyContractHost.m_currentTechnicalSignal5M.equals("ENTER_SHORT")){
								placeTestMarketOrder("SELL", lastShortTick);
							}
						}
					}
				}	
				
				if(extraTAResult.technicalSignal.equals("EXIT_LONG") && !currencyContractHost.m_currentTechnicalSignal240M.equals("EXIT_LONG")){
					placeTestMarketOrder("CLOSE", lastShortTick);

				}else if(extraTAResult.technicalSignal.equals("EXIT_SHORT") && !currencyContractHost.m_currentTechnicalSignal240M.equals("EXIT_SHORT")){
					placeTestMarketOrder("CLOSE", lastShortTick);
				}
				
				
				
				currencyContractHost.m_currentTechnicalSignal240M = extraTAResult.technicalSignal;
				currencyContractHost.m_currentTechnicalSignal60M = longTAResult.technicalSignal;
				currencyContractHost.m_currentTechnicalSignal15M = mediumTAResult.technicalSignal;
				currencyContractHost.m_currentTechnicalSignal5M = shortTAResult.technicalSignal;
				
				currencyContractHost.extraMedSma = extraTAResult.longSMA;
				currencyContractHost.longMedSma = longTAResult.longSMA;
				currencyContractHost.mediumMedSma = mediumTAResult.longSMA;
				currencyContractHost.shortMedSma = shortTAResult.longSMA;
				
	//			currencyContractHost.m_currentTechnicalSignal = pendingAction;
				contractHashMapHost.put(currencyContractHost.symbol() + currencyContractHost.currency(), currencyContractHost);
				
//				&& currencyContractHost.m_currentTechnicalSignal15M.equals("UP") && currencyContractHost.m_currentTechnicalSignal5M.equals("UP")){
//					placeTestMarketOrder("BUY", lastShortTick);
//				}else if(currencyContractHost.m_currentTechnicalSignal60M.equals("DOWN") && currencyContractHost.m_currentTechnicalSignal15M.equals("DOWN") && currencyContractHost.m_currentTechnicalSignal5M.equals("DOWN")){
//					placeTestMarketOrder("SELL", lastShortTick);}
//				else if(currencyContractHost.m_currentTechnicalSignal60M.equals("DOWN") && currencyContractHost.m_currentTechnicalSignal15M.equals("UP")){
//						placeTestMarketOrder("CLOSE", lastShortTick);	}
//				else if(currencyContractHost.m_currentTechnicalSignal60M.equals("UP") && currencyContractHost.m_currentTechnicalSignal15M.equals("DOWN")){
//						placeTestMarketOrder("CLOSE", lastShortTick);	}
				
				


				
				//                
			}
			// Analysis
			//
			//            // Getting the cash flow of the resulting trades
			//            CashFlow cashFlow = new CashFlow(series, longtradingRecord);
			//
			//            // Getting the profitable trades ratio
			//            AnalysisCriterion profitTradesRatio = new AverageProfitableTradesCriterion();
			//            System.out.println("Profitable trades ratio: " + profitTradesRatio.calculate(series, longtradingRecord));
			//            // Getting the reward-risk ratio
			//            AnalysisCriterion rewardRiskRatio = new RewardRiskRatioCriterion();
			//            System.out.println("Reward-risk ratio: " + rewardRiskRatio.calculate(series, longtradingRecord));
			//
			//            // Total profit of our strategy
			//            // vs total profit of a buy-and-hold strategy
			//            AnalysisCriterion vsBuyAndHold = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
			//            System.out.println("Our profit vs buy-and-hold profit: " + vsBuyAndHold.calculate(series, longtradingRecord));
			//            
			//            
			//            // Analysis
			//
			//            // Getting the cash flow of the resulting trades
			//             cashFlow = new CashFlow(series, shorttradingRecord);
			//
			//            // Getting the profitable trades ratio
			//            profitTradesRatio = new AverageProfitableTradesCriterion();
			//            System.out.println("Profitable trades ratio: " + profitTradesRatio.calculate(series, shorttradingRecord));
			//            // Getting the reward-risk ratio
			//            rewardRiskRatio = new RewardRiskRatioCriterion();
			//            System.out.println("Reward-risk ratio: " + rewardRiskRatio.calculate(series, shorttradingRecord));
			//
			//            // Total profit of our strategy
			//            // vs total profit of a buy-and-hold strategy
			//            vsBuyAndHold = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
			//            System.out.println("Our profit vs buy-and-hold profit: " + vsBuyAndHold.calculate(series, shorttradingRecord));
			//            

		}
	}

	


	
	private void placeTestMarketOrder(String action, Tick newTick){

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

}

