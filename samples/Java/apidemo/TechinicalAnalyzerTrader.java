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
			Strategy dailyLongStrategy = buildLongStrategy(dailyTimeFrameSetting.series, dailyTimeFrameSetting);
			Strategy dailyShortStrategy = buildShortStrategy(dailyTimeFrameSetting.series, dailyTimeFrameSetting);

			Strategy extraLongStrategy = buildLongStrategy(extraTimeFrameSetting.series, extraTimeFrameSetting);
			Strategy extraShortStrategy = buildShortStrategy(extraTimeFrameSetting.series, extraTimeFrameSetting);
			
			Strategy longLongStrategy = buildLongStrategy(longTimeFrameSetting.series, longTimeFrameSetting);
			Strategy longShortStrategy = buildShortStrategy(longTimeFrameSetting.series, longTimeFrameSetting);
			
			Strategy medianLongStrategy = buildLongStrategy(medianTimeFrameSetting.series, medianTimeFrameSetting);
			Strategy medianShortStrategy = buildShortStrategy(medianTimeFrameSetting.series, medianTimeFrameSetting);
			
			Strategy shortLongStrategy = buildLongStrategy(shortTimeFrameSetting.series, shortTimeFrameSetting);
			Strategy shortShortStrategy = buildShortStrategy(shortTimeFrameSetting.series, shortTimeFrameSetting);

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
				
			
				
				
				System.out.println(lastDailyTick.getDateName() + " ******Technical analysis Daily result for LONG**** " +  " Signal: " + dailyTAResult.technicalSignalUp + " longSMA:  " + dailyTAResult.longSMA + " shortSMA: " + dailyTAResult.shortSMA);
				System.out.println(lastExtraTick.getDateName() + " ******Technical analysis Extra result for LONG**** " +  " Signal: " + extraTAResult.technicalSignalUp + " longSMA:  " + extraTAResult.longSMA + " shortSMA: " + extraTAResult.shortSMA);
				System.out.println(lastLongTick.getDateName() + " ******Technical analysis Long result for LONG **** " +  " Signal: " + longTAResult.technicalSignalUp + " longSMA:  " + longTAResult.longSMA + " shortSMA: " + longTAResult.shortSMA);
				System.out.println(lastMedianTick.getDateName() + " ******Technical analysis Medium result for LONG **** " +  " Signal: " + medianTAResult.technicalSignalUp + " longSMA:  " + medianTAResult.longSMA + " shortSMA: " + medianTAResult.shortSMA);
				System.out.println(lastShortTick.getDateName() + " ******Technical analysis Short result for LONG **** " +  " Signal: " + shortTAResult.technicalSignalUp + " longSMA:  " + shortTAResult.longSMA + " shortSMA: " + shortTAResult.shortSMA);

				

				System.out.println(lastDailyTick.getDateName() + " ******Technical analysis Daily result for SHORT**** " +  " Signal: " + dailyTAResult.technicalSignalDown + " longSMA:  " + dailyTAResult.longSMA + " shortSMA: " + dailyTAResult.shortSMA);
				System.out.println(lastExtraTick.getDateName() + " ******Technical analysis Extra result for SHORT**** " +  " Signal: " + extraTAResult.technicalSignalDown + " longSMA:  " + extraTAResult.longSMA + " shortSMA: " + extraTAResult.shortSMA);
				System.out.println(lastLongTick.getDateName() + " ******Technical analysis Long result for SHORT **** " +  " Signal: " + longTAResult.technicalSignalDown + " longSMA:  " + longTAResult.longSMA + " shortSMA: " + longTAResult.shortSMA);
				System.out.println(lastMedianTick.getDateName() + " ******Technical analysis Medium result for SHORT **** " +  " Signal: " + medianTAResult.technicalSignalDown + " longSMA:  " + medianTAResult.longSMA + " shortSMA: " + medianTAResult.shortSMA);
				System.out.println(lastShortTick.getDateName() + " ******Technical analysis Short result for SHORT **** " +  " Signal: " + shortTAResult.technicalSignalDown + " longSMA:  " + shortTAResult.longSMA + " shortSMA: " + shortTAResult.shortSMA);
				
				
				

			}

				
				//Entry condition for long.
				//Extra in Up trend.
				//Long from up to down, then to up again. Now it is time to buy.
				//Then use medium or short as entry point.
				
				
				//if extra change direction or first time get its value. Reset medium up trend to no change.
				if(extraTAResult.technicalSignalUp.equals(currencyContractHost.m_currentTechnicalSignal240MUp) == false){
					currencyContractHost.isLongUpTrendTouchednReversed = 0;
					currencyContractHost.isMediumUpTrendTouchednReversed = 0;
					currencyContractHost.isShortUpTrendTouchednReversed = 0;
				}
				
//				//if long change direction or first time get its value. Reset medium up trend to no change.
//				if(longTAResult.technicalSignalUp.equals(currencyContractHost.m_currentTechnicalSignal60MUp) == false){
//					currencyContractHost.isMediumUpTrendTouchednReversed = 0;
//				}
				
				if(PRINT_OUT_MESSAGE)
					System.out.println("Current currencyContractHost.isMediumUpTrendTouchednReversed: " + currencyContractHost.isMediumUpTrendTouchednReversed);
				
				//IF both extra and long is up, but medium is down, it is stage 1. reverse.
				if(extraTAResult.technicalSignalUp.equals(TechnicalSignalTrend.ENTER_LONG)){
					
					//if current extra trend is up and long trend is down. seek a good position to enter the trade.
					if(longTAResult.technicalSignalUp.equals(TechnicalSignalTrend.EXIT_LONG) && currencyContractHost.m_currentTechnicalSignal60MUp.equals(TechnicalSignalTrend.ENTER_LONG)){
						if(PRINT_OUT_MESSAGE)
							System.out.println(lastShortTick.getDateName() + " Up wait a good postion to Buy");
						currencyContractHost.isLongUpTrendTouchednReversed = 1;
					}
					
					//if current long trend is up again and it has been reversed. Time to buy
					if(longTAResult.technicalSignalUp.equals(TechnicalSignalTrend.ENTER_LONG) && currencyContractHost.isLongUpTrendTouchednReversed == 1)
						currencyContractHost.isLongUpTrendTouchednReversed = 2;
					}
					
					if(longTAResult.technicalSignalUp.equals(TechnicalSignalTrend.ENTER_LONG)){
						if(PRINT_OUT_MESSAGE)
							System.out.println(lastShortTick.getDateName() + " Up wait a good postion to Buy");
						if(medianTAResult.technicalSignalUp.equals(TechnicalSignalTrend.EXIT_LONG))
							currencyContractHost.isMediumUpTrendTouchednReversed = 1;
						}
				
					//if current medium trend is up again and it has been reversed. Time to buy
					if(medianTAResult.technicalSignalUp.equals(TechnicalSignalTrend.ENTER_LONG) && currencyContractHost.isMediumUpTrendTouchednReversed == 1)
						currencyContractHost.isMediumUpTrendTouchednReversed = 2;
					
					
					if(longTAResult.technicalSignalUp.equals(TechnicalSignalTrend.ENTER_LONG) && medianTAResult.technicalSignalUp.equals(TechnicalSignalTrend.ENTER_LONG) ){
						if(PRINT_OUT_MESSAGE)
						System.out.println(lastShortTick.getDateName() + " Up wait a good postion to Buy");
						if(shortTAResult.technicalSignalUp.equals(TechnicalSignalTrend.EXIT_LONG))
							currencyContractHost.isShortUpTrendTouchednReversed = 1;
						}
				
					//if current medium trend is up again and it has been reversed. Time to buy
					if(shortTAResult.technicalSignalUp.equals(TechnicalSignalTrend.ENTER_LONG) && currencyContractHost.isShortUpTrendTouchednReversed == 1)
						currencyContractHost.isShortUpTrendTouchednReversed = 2;
					
					

				//currencyContractHost.isShortUpTrendTouchednReversed == 2 || 
		//	if(currencyContractHost.isMediumUpTrendTouchednReversed == 2 || currencyContractHost.isLongUpTrendTouchednReversed == 2)
					if(extraTAResult.technicalSignalUp.equals(TechnicalSignalTrend.ENTER_LONG) && longTAResult.technicalSignalUp.equals(TechnicalSignalTrend.ENTER_LONG) && currencyContractHost.m_currentTechnicalSignal60MUp.equals(TechnicalSignalTrend.EXIT_LONG))
					{
					
						
						if(longtradingRecord.isClosed())
						{
							System.out.println(lastShortTick.getDateName() + " place order to Buy now @ " + lastShortTick.getClosePrice());
							placeTestMarketOrder("BUY", lastShortTick);
							longtradingRecord.enter(shortTAResult.endIndex, lastShortTick.getClosePrice(), Decimal.valueOf(25000));
							currencyContractHost.isShortUpTrendTouchednReversed = 0;
							currencyContractHost.isMediumUpTrendTouchednReversed = 0;
							currencyContractHost.isLongUpTrendTouchednReversed = 0;
							
						}

			
					}
				
				//If extra change trend, time to close
				if(extraTAResult.technicalSignalUp.equals(TechnicalSignalTrend.EXIT_LONG) && currencyContractHost.m_currentTechnicalSignal240MUp.equals(TechnicalSignalTrend.ENTER_LONG)){
					if(!longtradingRecord.isClosed()){
						placeTestMarketOrder("CLOSE", lastShortTick);
						longtradingRecord.exit(shortTAResult.endIndex, lastShortTick.getClosePrice(), Decimal.valueOf(25000));
						System.out.println(lastShortTick.getDateName() + " Close Long order to now @ " + lastShortTick.getClosePrice());
						}
				}
				
				
				//Entry condition for Short.
				//Extra in Down trend.
				//Long from up to down, then to up again. Now it is time to sell.
				//Then use medium or short as entry point.
				
				
				//if extra change direction or first time get its value. Reset medium up trend to no change.
				if(extraTAResult.technicalSignalDown.equals(currencyContractHost.m_currentTechnicalSignal240MDown) == false){
					currencyContractHost.isLongDownTrendTouchednReversed = 0;
					currencyContractHost.isMediumDownTrendTouchednReversed = 0;
					currencyContractHost.isShortDownTrendTouchednReversed = 0;
				}
				
//				//if short change direction or first time get its value. Reset medium up trend to no change.
//				if(longTAResult.technicalSignalDown.equals(currencyContractHost.m_currentTechnicalSignal60MDown) == false){
//					currencyContractHost.isMediumDownTrendTouchednReversed = 0;
//				}
				if(PRINT_OUT_MESSAGE)
				System.out.println("Current currencyContractHost.isMediumDownTrendTouchednReversed: " + currencyContractHost.isMediumDownTrendTouchednReversed);
				
				//IF both extra and long is down, but medium is long, it is stage 1. reverse.
				if(extraTAResult.technicalSignalDown.equals(TechnicalSignalTrend.ENTER_SHORT)){
					
					//if current extra trend is up and long trend is down. seek a good position to enter the trade.
					if(longTAResult.technicalSignalDown.equals(TechnicalSignalTrend.EXIT_SHORT) && currencyContractHost.m_currentTechnicalSignal60MDown.equals(TechnicalSignalTrend.EXIT_SHORT)){
						if(PRINT_OUT_MESSAGE)
						System.out.println(lastShortTick.getDateName() + " Down wait a good postion to Buy");
						currencyContractHost.isLongDownTrendTouchednReversed = 1;
					}
					
					//if current long trend is up again and it has been reversed. Time to sell
					if(longTAResult.technicalSignalDown.equals(TechnicalSignalTrend.ENTER_SHORT) && currencyContractHost.isLongDownTrendTouchednReversed == 1)
						currencyContractHost.isLongDownTrendTouchednReversed = 2;
					}
					
					if(longTAResult.technicalSignalDown.equals(TechnicalSignalTrend.ENTER_SHORT)){
						if(PRINT_OUT_MESSAGE)
						System.out.println(lastShortTick.getDateName() + " Down wait a good postion to sell");
						if(medianTAResult.technicalSignalDown.equals(TechnicalSignalTrend.EXIT_SHORT))
							currencyContractHost.isMediumDownTrendTouchednReversed = 1;
						}
				
					//if current medium trend is up again and it has been reversed. Time to buy
					if(medianTAResult.technicalSignalDown.equals(TechnicalSignalTrend.ENTER_SHORT) && currencyContractHost.isMediumDownTrendTouchednReversed == 1)
						currencyContractHost.isMediumDownTrendTouchednReversed = 2;
					
					
					if(longTAResult.technicalSignalDown.equals(TechnicalSignalTrend.ENTER_SHORT) && medianTAResult.technicalSignalDown.equals(TechnicalSignalTrend.ENTER_SHORT) ){
						if(PRINT_OUT_MESSAGE)
						System.out.println(lastShortTick.getDateName() + " Down wait a good postion to Sell");
						if(shortTAResult.technicalSignalDown.equals(TechnicalSignalTrend.EXIT_SHORT))
							currencyContractHost.isShortDownTrendTouchednReversed = 1;
						}
				
					//if current medium trend is up again and it has been reversed. Time to sell
					if(shortTAResult.technicalSignalDown.equals(TechnicalSignalTrend.ENTER_SHORT) && currencyContractHost.isShortDownTrendTouchednReversed == 1)
						currencyContractHost.isShortDownTrendTouchednReversed = 2;
					
					

				//currencyContractHost.isShortDownTrendTouchednReversed == 2 || 
	//			if(currencyContractHost.isMediumDownTrendTouchednReversed == 2 || currencyContractHost.isLongDownTrendTouchednReversed == 2)
					if(extraTAResult.technicalSignalDown.equals(TechnicalSignalTrend.ENTER_SHORT) && longTAResult.technicalSignalDown.equals(TechnicalSignalTrend.ENTER_SHORT) && currencyContractHost.m_currentTechnicalSignal60MDown.equals(TechnicalSignalTrend.EXIT_SHORT))
				{
					
					
						if(shorttradingRecord.isClosed())
						{
							placeTestMarketOrder("SELL", lastShortTick);
							shorttradingRecord.enter(shortTAResult.endIndex, lastShortTick.getClosePrice(), Decimal.valueOf(25000));
							currencyContractHost.isShortDownTrendTouchednReversed = 0;
							currencyContractHost.isMediumDownTrendTouchednReversed = 0;
							currencyContractHost.isLongDownTrendTouchednReversed = 0;
							System.out.println(lastShortTick.getDateName() + " place order to Sell now @ price " + lastShortTick.getClosePrice());					
						}

			
					}
				
				//If extra change trend, time to close
				if(extraTAResult.technicalSignalDown.equals(TechnicalSignalTrend.EXIT_SHORT) && currencyContractHost.m_currentTechnicalSignal240MDown.equals(TechnicalSignalTrend.ENTER_SHORT))
				{
					if(!shorttradingRecord.isClosed()){
					System.out.println(lastShortTick.getDateName() + " Close short order now @ price " + lastShortTick.getClosePrice());
					placeTestMarketOrder("CLOSE", lastShortTick);
					shorttradingRecord.exit(shortTAResult.endIndex, lastShortTick.getClosePrice(), Decimal.valueOf(25000));
					}
				}
				
					
				
				
				
				currencyContractHost.m_currentTechnicalSignal240MUp = extraTAResult.technicalSignalUp;
				currencyContractHost.m_currentTechnicalSignal60MUp = longTAResult.technicalSignalUp;
				currencyContractHost.m_currentTechnicalSignal15MUp = medianTAResult.technicalSignalUp;
				currencyContractHost.m_currentTechnicalSignal5MUp = shortTAResult.technicalSignalUp;

				currencyContractHost.m_currentTechnicalSignal240MDown = extraTAResult.technicalSignalDown;
				currencyContractHost.m_currentTechnicalSignal60MDown = longTAResult.technicalSignalDown;
				currencyContractHost.m_currentTechnicalSignal15MDown = medianTAResult.technicalSignalDown;
				currencyContractHost.m_currentTechnicalSignal5MDown = shortTAResult.technicalSignalDown;				
				
				currencyContractHost.extraMedSma = extraTAResult.longSMA;
				currencyContractHost.longMedSma = longTAResult.longSMA;
				currencyContractHost.mediumMedSma = medianTAResult.longSMA;
				currencyContractHost.shortMedSma = shortTAResult.longSMA;
				
				contractHashMapHost.put(currencyContractHost.symbol() + currencyContractHost.currency(), currencyContractHost);
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
			endDate = DATE_FORMAT.parse("20161001 00:00");
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
	private Strategy buildLongStrategy(TimeSeries series, TimeframeSettings timeFrameSetting) {
		if (series == null) {
			throw new IllegalArgumentException("Series cannot be null");
		}

		//		MinPriceIndicator minPrice = new MinPriceIndicator(series);
		//		SMAIndicator longMinSma = new SMAIndicator(minPrice, 10);
		//
		//
		//		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
		//
		//		// Getting the simple moving average (SMA) of the close price over the last 5 ticks
		//		SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
		//		SMAIndicator shortSmaHourly = new SMAIndicator(closePrice, 5 * 12);
		//
		//		// Here is the 5-ticks-SMA value at the 42nd index
		//		System.out.println("5-ticks-SMA value at the 42nd index: " + shortSma.getValue(42).toDouble());
		//
		//		// Getting a longer SMA (e.g. over the 30 last ticks)
		//		SMAIndicator longSma = new SMAIndicator(closePrice, 10);
		//		SMAIndicator longSmaHourly = new SMAIndicator(closePrice, 10 * 12);
		//
		//
		//		// Relative strength index
		//		RSIIndicator rsi = new RSIIndicator(closePrice, 168);
		//
		//		StochasticOscillatorKIndicator sof = new StochasticOscillatorKIndicator(series, 14);
		//		SMAIndicator sma = new SMAIndicator(sof, 3);
		//		StochasticOscillatorDIndicator sos = new StochasticOscillatorDIndicator(sma);



		// Ok, now let's building our trading rules!

		// Buying rules
		// We want to buy:
		//  - if the 5-ticks SMA crosses over 10-ticks SMA
		//  - and if the K(sof) goes above D(sos)
		Rule buyingRule = null;

{
			buyingRule = ( new OverIndicatorRule(timeFrameSetting.shortSMA, timeFrameSetting.longSMA).or(new OverIndicatorRule(timeFrameSetting.closePrice, timeFrameSetting.shortSMA)))
					.and( new OverIndicatorRule(timeFrameSetting.sofKStoch, timeFrameSetting.sosDStoch))
					.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")));
		}

		// Selling rules
		// We want to sell:
		//  - if the 5-ticks SMA crosses under 30-ticks SMA
		//  - or if if the price looses more than 3%
		//  - or if the price earns more than 2%
		Rule sellingRule = null;
//		if(durationHost == 5){
//			sellingRule = ( new CrossedDownIndicatorRule(shortSma, longSma).or(new CrossedDownIndicatorRule(closePrice, shortSma)))
//					.and( new UnderIndicatorRule(sofStoch, sosStoch))
//					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
//					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
//		}else
		{
			sellingRule = ( new UnderIndicatorRule(timeFrameSetting.shortSMA, timeFrameSetting.longSMA).or(new UnderIndicatorRule(timeFrameSetting.closePrice, timeFrameSetting.shortSMA)))
					.and( new OverIndicatorRule(timeFrameSetting.sofKStoch, timeFrameSetting.sosDStoch))
					.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")));
		}

		// Running our juicy trading strategy...
		//     TradingRecord tradingRecord = series.run(new Strategy(buyingRule, sellingRule));        



		// Signals
		// Buy when SMA goes over close price
		// Sell when close price goes over SMA
		Strategy buySellSignals = new Strategy(buyingRule, sellingRule);
		return buySellSignals;
	}
	
	
	
	/**
	 * @param series a time series
	 * @param timeFrameSetting 
	 * @return a dummy strategy
	 */
	private Strategy buildShortStrategy(TimeSeries series, TimeframeSettings timeFrameSetting) {
		if (series == null) {
			throw new IllegalArgumentException("Series cannot be null");
		}

		//		MaxPriceIndicator maxPrice = new MaxPriceIndicator(series);
		//		SMAIndicator longMaxSma = new SMAIndicator(maxPrice, 10);
		//
		//
		//		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
		//
		//
		//
		//		// Getting the simple moving average (SMA) of the close price over the last 5 ticks
		//		SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
		//
		//		// Getting the simple moving average (SMA) of the close price over the last 5 ticks
		//		SMAIndicator shortSmaHourly = new SMAIndicator(closePrice, 5 * 12);    
		//
		//		// Here is the 5-ticks-SMA value at the 42nd index
		//		System.out.println("5-ticks-SMA value at the 42nd index: " + shortSma.getValue(42).toDouble());
		//
		//		// Getting a longer SMA (e.g. over the 30 last ticks)
		//		SMAIndicator longSma = new SMAIndicator(closePrice, 10);
		//		SMAIndicator longSmaHourly = new SMAIndicator(closePrice, 10 * 12);
		//
		//
		//		// Relative strength index
		//		RSIIndicator rsi = new RSIIndicator(closePrice, 14);
		//
		//		StochasticOscillatorKIndicator sof = new StochasticOscillatorKIndicator(series, 168);
		//		SMAIndicator sma = new SMAIndicator(sof, 3);
		//		StochasticOscillatorDIndicator sos = new StochasticOscillatorDIndicator(sma);



		// Ok, now let's building our trading rules!

		// Buying rules
		// We want to buy:
		//  - if the 5-ticks SMA crosses over 30-ticks SMA
		//  - and if the K(sof) goes above D(sos)
		Rule buyingRule = null;
//		if(durationHost == 5){
//			buyingRule = ( new CrossedUpIndicatorRule(shortSma, longSma).or(new CrossedUpIndicatorRule(closePrice, shortSma)))
//					.and( new OverIndicatorRule(sofStoch, sosStoch))
//					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
//					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
//		}else
		{
			buyingRule = ( new OverIndicatorRule(timeFrameSetting.shortSMA, timeFrameSetting.longSMA).or(new OverIndicatorRule(timeFrameSetting.closePrice, timeFrameSetting.shortSMA)))
					.and( new OverIndicatorRule(timeFrameSetting.sofKStoch, timeFrameSetting.sosDStoch))
					.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")));;
		}

		// Selling rules
		// We want to sell:
		//  - if the 5-ticks SMA crosses under 30-ticks SMA
		//  - or if if the price looses more than 3%
		//  - or if the price earns more than 2%
		Rule sellingRule = null;
//		if(durationHost == 5){
//			sellingRule = ( new CrossedDownIndicatorRule(shortSma, longSma).or(new CrossedDownIndicatorRule(closePrice, shortSma)))
//					.and( new UnderIndicatorRule(sofStoch, sosStoch))
//					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
//					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
//		}else
		{
			sellingRule = ( new UnderIndicatorRule(timeFrameSetting.shortSMA, timeFrameSetting.longSMA).or(new UnderIndicatorRule(timeFrameSetting.closePrice, timeFrameSetting.shortSMA)))
					.and( new OverIndicatorRule(timeFrameSetting.sofKStoch, timeFrameSetting.sosDStoch))
					.and(new UnderIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(timeFrameSetting.RSI, Decimal.valueOf("20")));
		}


		// Running our juicy trading strategy...
		//     TradingRecord tradingRecord = series.run(new Strategy(buyingRule, sellingRule));        



		// Signals
		// Buy when SMA goes over close price
		// Sell when close price goes over SMA
		Strategy buySellSignals = new Strategy(sellingRule, buyingRule);
		return buySellSignals;
	}
	
	

}

