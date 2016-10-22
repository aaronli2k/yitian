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
import eu.verdelhan.ta4j.Indicator;
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
import eu.verdelhan.ta4j.indicators.CachedIndicator;
import eu.verdelhan.ta4j.indicators.candles.BearishEngulfingIndicator;
import eu.verdelhan.ta4j.indicators.candles.BearishHaramiIndicator;
import eu.verdelhan.ta4j.indicators.candles.BullishEngulfingIndicator;
import eu.verdelhan.ta4j.indicators.candles.BullishHaramiIndicator;
import eu.verdelhan.ta4j.indicators.candles.DojiIndicator;
import eu.verdelhan.ta4j.indicators.candles.LowerShadowIndicator;
import eu.verdelhan.ta4j.indicators.candles.RealBodyIndicator;
import eu.verdelhan.ta4j.indicators.candles.ThreeBlackCrowsIndicator;
import eu.verdelhan.ta4j.indicators.candles.ThreeWhiteSoldiersIndicator;
import eu.verdelhan.ta4j.indicators.candles.UpperShadowIndicator;
import eu.verdelhan.ta4j.indicators.oscillators.StochasticOscillatorDIndicator;
import eu.verdelhan.ta4j.indicators.oscillators.StochasticOscillatorKIndicator;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.simple.MaxPriceIndicator;
import eu.verdelhan.ta4j.indicators.simple.MedianPriceIndicator;
import eu.verdelhan.ta4j.indicators.simple.MinPriceIndicator;
import eu.verdelhan.ta4j.indicators.simple.PriceVariationIndicator;
import eu.verdelhan.ta4j.indicators.simple.TypicalPriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.EMAIndicator;
import eu.verdelhan.ta4j.indicators.trackers.MACDIndicator;
import eu.verdelhan.ta4j.indicators.trackers.RSIIndicator;
import eu.verdelhan.ta4j.indicators.trackers.SMAIndicator;
import eu.verdelhan.ta4j.trading.rules.BooleanIndicatorRule;
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
public class TechinicalAnalyzer{

	/** Close price of the last tick */
	private static Decimal LAST_TICK_CLOSE_PRICE;
	private Contract currencyContractHost;
	private static Boolean historicalDataEnd;
	private ApiDemo apiDemoHost;
	private boolean newTickAvailable;
	private ConcurrentHashMap<Long, forex> orderHashMapHost;
	private ConcurrentHashMap<String, Contract> ContractHashMapHost;
	private ConcurrentHashMap<Long, Bar> barHashMap;

	private int durationHost = 0;
	private int TICK_LIMIT = 5000;

	final SimpleDateFormat DATEOnly_FORMAT = new SimpleDateFormat("yyyyMMdd");
	final SimpleDateFormat TIMEOnly_FORMAT = new SimpleDateFormat("HH:mm:ss");

	private TimeSeries series;
	private Strategy longStrategy = null;
	private Strategy shortStrategy = null;
	ClosePriceIndicator closePrice = null;
	SMAIndicator shortSma = null;
	SMAIndicator longSma = null;
	private RSIIndicator rsi;
	private StochasticOscillatorKIndicator sofStoch;
	private SMAIndicator smaStoch;
	private StochasticOscillatorDIndicator sosStoch;
	private MinPriceIndicator minPrice;
	private SMAIndicator longMinSma;
	private MaxPriceIndicator maxPrice;
	private SMAIndicator longMaxSma;
	private MedianPriceIndicator medianPrice;
	private SMAIndicator longMedSma;

//	
	private boolean currentTechnicalSignalLongEntry = false;
	private boolean currentTechnicalSignalLongExit = false;

	private boolean currentTechnicalSignalShortEntry = false;
	private boolean currentTechnicalSignalShortExit = false;
	private TypicalPriceIndicator typicalPrice;
	private PriceVariationIndicator priceVariation;
	private MACDIndicator macd;
	private EMAIndicator emaMacd;

	
	//various pattern recornizor 
	private BullishEngulfingIndicator bullishEngulfingIndicator;
	private BearishEngulfingIndicator bearishEngulfingIndicator;
	private UpperShadowIndicator upperShadowIndicator;
	private ThreeWhiteSoldiersIndicator threeWhiteSoldiersIndicator;
	private ThreeBlackCrowsIndicator threeBlackCrowsIndicator; 
	private RealBodyIndicator realBodyIndicator;
	private LowerShadowIndicator lowerShadowIndicator;
	private DojiIndicator dojiIndicator;
	private BullishHaramiIndicator bullishHaramiIndicator;
	private BearishHaramiIndicator bearishHaramiIndicator;

	

	public TechinicalAnalyzer(ApiDemo apiDemo, Contract currencyContract, ConcurrentHashMap<String, Contract> contractHashMap , ConcurrentHashMap<Long, forex> orderHashMap, int duration, ConcurrentHashMap<Long, Bar> barHashMapIn, int tickLimit){
		currencyContractHost = currencyContract;
		apiDemoHost = apiDemo;
		orderHashMapHost = orderHashMap;
		ContractHashMapHost = contractHashMap;
		durationHost = duration;

		barHashMap = barHashMapIn;
		TICK_LIMIT = tickLimit;

		System.out.println("**********************Techinical Analyzer for " +currencyContract.symbol() + currencyContract.currency() + duration +  " minutes Initialization **********************");

	}

	public TimeframeSettings initDB(int fastSMAPeriod, int slowSMAPeriod, int priceToUse){
		System.out.println("**********************Techinical Analyzer " + currencyContractHost.symbol() + currencyContractHost.currency() + durationHost +  " minutes  Running **********************");
		// Getting the time series

		//            //Simulated testing for different dataset.
		//            TicksAccesser ticksAccess = new TicksAccesser(null);
		//            barHashMap = ticksAccess.readFromCsv("NZDUSD_ticks_history_2007_to_2016.csv");
		//			ticksAccess.start();

		//while(currencyContractHost.historical5MBarMap.size() < TICK_LIMIT)


		series = buildTimeSeriesFromMap(TICK_LIMIT);
		if(series == null){
			System.out.println(new Date() + "TimeSeries series = buildTimeSeriesFromMap(TICK_LIMIT); return a null pointer");
			return null; //If series is null, there is something wrong.
		}


		closePrice = new ClosePriceIndicator(series);

		medianPrice = new MedianPriceIndicator(series);

		typicalPrice = new TypicalPriceIndicator(series);

		Indicator<Decimal> priceIndicator = closePrice;

		if(priceToUse == 0)
			priceIndicator = closePrice;
		else if(priceToUse == 1)
			priceIndicator = medianPrice;
		else if(priceToUse == 2)
			priceIndicator = typicalPrice;
		
		priceVariation = new PriceVariationIndicator(series);

		
		// Getting the simple moving average (SMA) of the close price over the last 5 ticks
		shortSma = new SMAIndicator(priceIndicator, fastSMAPeriod);

		// Here is the 5-ticks-SMA value at the 5nd index
		System.out.println("5-ticks-SMA value at the 42nd index: " + shortSma.getValue(5).toDouble());

		// Getting a longer SMA (e.g. over the 30 last ticks)
		longSma = new SMAIndicator(priceIndicator, slowSMAPeriod);


		// Relative strength index
		rsi = new RSIIndicator(priceIndicator, 14);

		sofStoch = new StochasticOscillatorKIndicator(series, 14);
		smaStoch = new SMAIndicator(sofStoch, 3);
		sosStoch = new StochasticOscillatorDIndicator(smaStoch);

		longMedSma = new SMAIndicator(priceIndicator, 10);

		maxPrice = new MaxPriceIndicator(series);
		longMaxSma = new SMAIndicator(maxPrice, 10);

		macd = new MACDIndicator(priceIndicator, 9, 26);
		emaMacd = new EMAIndicator(macd, 18);
		
		


		bullishEngulfingIndicator = new BullishEngulfingIndicator(series);
		bearishEngulfingIndicator = new BearishEngulfingIndicator(series);
		upperShadowIndicator = new UpperShadowIndicator(series);
		threeWhiteSoldiersIndicator = new ThreeWhiteSoldiersIndicator(series, 3, Decimal.valueOf("0.1"));
		threeBlackCrowsIndicator = new ThreeBlackCrowsIndicator(series, 3, Decimal.valueOf("0.1")); 
		realBodyIndicator = new RealBodyIndicator(series);
		lowerShadowIndicator = new LowerShadowIndicator(series);
		dojiIndicator = new DojiIndicator(series, 5, Decimal.valueOf(0.1)); //factor is 0.1
		bullishHaramiIndicator = new BullishHaramiIndicator(series);
		bearishHaramiIndicator = new BearishHaramiIndicator(series);

		System.out.println("********************Finish initialize Database****************************************");
		

		
		TimeframeSettings timeSetting = new TimeframeSettings(sosStoch, sofStoch, closePrice, medianPrice, priceVariation, typicalPrice, macd, emaMacd, shortSma, longSma, rsi, series.getLastTick(), series,
				bullishEngulfingIndicator,
		bearishEngulfingIndicator,
		upperShadowIndicator,
		threeWhiteSoldiersIndicator,
		threeBlackCrowsIndicator,
		realBodyIndicator,
		lowerShadowIndicator,
		dojiIndicator,
		bullishHaramiIndicator,
		bearishHaramiIndicator);

		// Building the trading strategy
		longStrategy = buildLongStrategy(series, timeSetting, durationHost);
		shortStrategy = buildShortStrategy(series, timeSetting, durationHost);
		return timeSetting;	

	}


	public   TechnicalAnalyzerResult analyze(Date lastProcessedtime, boolean PRINT_OUT_MESSAGE ){
		Tick newTick  = null;
		Bar bar = null;
		int endIndex = 0;
		
		
		
		SortedSet<Long> keys = new TreeSet<Long>(barHashMap.keySet());
		//								TreeSet<Long> treereverse = (TreeSet<Long>) keys.descendingSet();

		for (Long key : keys){
			bar = barHashMap.get(key);
			//Please be sure that short bar is running at last. Maybe no need.
			barHashMap.remove(key);


			//If last acquired time is before this tick time, which means this data is acquired and should be abandoned.
			if(lastProcessedtime.compareTo(new Date(bar.time() * 1000)) >= 0){
				//						barHashMap.remove(key);
				//						System.out.println(durationHost + " minutes " + " Bar " + bar.formattedTime() + " High: " +  bar.high() + " Low: " +  bar.low() + " Open: " +  bar.open() + " close: " +  bar.close());
				continue;
			}

			double open = bar.open();
			double high = bar.high();
			double low = bar.low();
			double close = bar.close();
			double volume = bar.volume();
			newTick = new Tick(new DateTime(bar.time() * 1000), open, high, low, close, volume);
			series.addTick(newTick);
			break;

		}


		//If it is same tick, don't process it. Maybe change it later.
		//			if(newTick == null)
		//				return newTick;

		newTick = series.getLastTick();
		lastProcessedtime = newTick.getEndTime().toDate();

		//                Tick newTick = generateRandomTick();
		if(PRINT_OUT_MESSAGE){
		System.out.println("-----------Techinical Analyzer " + currencyContractHost.symbol() + currencyContractHost.currency() + "------\n"
				+ "Tick "+ newTick.getDateName() +" added, close price = " + newTick.getClosePrice().toDouble()
				+ " open price = " + newTick.getOpenPrice().toDouble() + " High price = " + newTick.getMaxPrice().toDouble()  
				 + " Low price = " + newTick.getMinPrice().toDouble() );
		//                series.addTick(newTick);
		}


		endIndex = series.getEnd();

		if(PRINT_OUT_MESSAGE){

		System.out.println(series.getLastTick().getDateName() + " "  + durationHost + " minutes" + "------Techinical Analyzer begin" + currencyContractHost.symbol() + currencyContractHost.currency() + "------");
		System.out.println(series.getLastTick().getDateName() + " Closed Price: " + closePrice.getValue(endIndex));
		System.out.println(series.getLastTick().getDateName() + durationHost + " minutes SMA 5: " + shortSma.getValue(endIndex));
		System.out.println(series.getLastTick().getDateName() + durationHost  + " minutes SMA 10: " + longSma.getValue(endIndex));

		System.out.println(series.getLastTick().getDateName() + durationHost  + " minutes RSI: " + rsi.getValue(endIndex));
		System.out.println(series.getLastTick().getDateName() + durationHost  + " minutes Stoch K: " + sofStoch.getValue(endIndex) + " D: " + sosStoch.getValue(endIndex));
		System.out.println(series.getLastTick().getDateName() + durationHost  + " minutes Stoch K corss down D  is " + new CrossedDownIndicatorRule(sofStoch, sosStoch).isSatisfied(endIndex));
		System.out.println(series.getLastTick().getDateName() + durationHost  + " minutes Stoch K under D  is " + new UnderIndicatorRule(sofStoch, sosStoch).isSatisfied(endIndex));
		System.out.println(series.getLastTick().getDateName().toString()+ " " + durationHost + " minutes" +  "-----Techinical Analyzer end" + currencyContractHost.symbol() + currencyContractHost.currency() + "------");
		}

		if (currentTechnicalSignalLongEntry = longStrategy.shouldEnter(endIndex)) {
			// Our strategy should enter
			if(PRINT_OUT_MESSAGE)
				System.out.println(series.getLastTick().getDateName() + " " + durationHost + " minutes Strategy should ENTER LONG on " + endIndex);
			




		} else if (currentTechnicalSignalLongExit = longStrategy.shouldExit(endIndex)) {
			// Our strategy should exit
			if(PRINT_OUT_MESSAGE)
				System.out.println(series.getLastTick().getDateName() + " " + durationHost + " minutes  Strategy should EXIT LONG on " + endIndex);





		}else{
			// Our strategy should WAIT
			if(PRINT_OUT_MESSAGE)
				System.out.println(series.getLastTick().getDateName() + " " + durationHost + " minutes LONG Strategy should WAIT  on " + endIndex);

		}

		//If it is a short
		endIndex = series.getEnd();
		if (currentTechnicalSignalShortEntry = shortStrategy.shouldEnter(endIndex)) {
			// Our strategy should enter
			if(PRINT_OUT_MESSAGE)
				System.out.println(series.getLastTick().getDateName() + " " + durationHost + " minutes  Strategy should ENTER SHORT on " + endIndex);





		} else if (currentTechnicalSignalShortExit = shortStrategy.shouldExit(endIndex)) {
			// Our strategy should exit
			if(PRINT_OUT_MESSAGE)
				System.out.println(series.getLastTick().getSimpleDateName() + " " + durationHost + " minutes  Strategy should EXIT SHORT on " + endIndex);




		}else{
			// Our strategy should WAIT
			if(PRINT_OUT_MESSAGE)
				System.out.println(series.getLastTick().getDateName() + " " + durationHost + " minutes SHORT Strategy should WAIT  on " + endIndex);
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
		return new TechnicalAnalyzerResult(newTick, endIndex, series, currentTechnicalSignalLongEntry, currentTechnicalSignalLongExit, currentTechnicalSignalShortEntry, currentTechnicalSignalShortExit, longSma.getValue(endIndex).toDouble(), shortSma.getValue(endIndex).toDouble());
	}






	private TimeSeries buildTimeSeriesFromMap(int seriesLimit){
		TreeSet<Long> keys = new TreeSet<Long>(barHashMap.keySet());
		//		TreeSet<Long> treereverse = (TreeSet<Long>) keys.descendingSet();

		ArrayList<Tick> ticks = new ArrayList<Tick>();

		Bar bar = null;
		Calendar cal = Calendar.getInstance();

		//Let's keep 1 day data to process and make sure it is a round clock
		cal.add(Calendar.HOUR, -23);
		cal.add(Calendar.MINUTE, (2 + cal.get(Calendar.MINUTE)) * -1);
		for (Long key : keys){
			bar = barHashMap.get(key);
			//			barHashMap.remove(key);

			if(bar == null)
				continue;

//			//Let's keep 1 day data to process
//			if(new DateTime(bar.time() * 1000).toDate().after(cal.getTime())){
//				System.out.println("Tick time: " + cal.getTime());
//				break;
//			}
			
//			Date startDate = null;
//			Date endDate = null;
//			//	final SimpleDateFormat DATEOnly_FORMAT = new SimpleDateFormat("yyyyMMdd");
//			try {
//				startDate = DATEOnly_FORMAT.parse("20160101");
//				endDate = DATEOnly_FORMAT.parse("20161001");
//			} catch (ParseException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
////			//only process date between startDate and endDate
//			if(new DateTime(bar.time() * 1000).toDate().before(startDate)){
//				System.out.println("First Tick to process time: " + startDate);
//				continue;
//			}
//			
////			//only process date between startDate and endDate
//			if(new DateTime(bar.time() * 1000).toDate().after(endDate)){
//				System.out.println("Last Tick to process time: " + cal.getTime());
//				break;
//			}
			
			double open = bar.open();
			double high = bar.high();
			double low = bar.low();
			double close = bar.close();
			double volume = bar.volume();
			Tick tick = new Tick(new DateTime(bar.time() * 1000), open, high, low, close, volume);
			ticks.add(tick);

		}
		if(bar != null)
			System.out.println(currencyContractHost.symbol() + currencyContractHost.currency() + " time: " + bar.formattedTime()+ " bar high: " + bar.high() + " bar low: " + bar.low() + " bar close: " + bar.close() + " bar volume: " + bar.volume());

		if(ticks.isEmpty())
			return null;
		TimeSeries series = new TimeSeries(currencyContractHost.symbol() + currencyContractHost.currency() + "_ticks", ticks);
		series.setMaximumTickCount(seriesLimit);
		LAST_TICK_CLOSE_PRICE = series.getTick(series.getEnd()).getClosePrice();
		return series;
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
			entryRule = ( new OverIndicatorRule(shortSma, longSma).or(new OverIndicatorRule(closePrice, shortSma)))
					.and( new OverIndicatorRule(macd, emaMacd))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
		}

		// Selling rules
		// We want to sell:
		//  - if the short SMA under long SMA
		//  - and MACD is below emaMacd
		if(duration == 1440)
		{
			exitRule = ( new UnderIndicatorRule(shortSma, longSma).or(new UnderIndicatorRule(closePrice, shortSma)))
					.and( new UnderIndicatorRule(macd, emaMacd))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
		}


		//Four hour rules. Use same rules as daily
		// Buying rules
		// We want to buy:
		//  - if the short SMA over long SMA
		//  - and MACD is above emaMacd
		// RSI is not oversold and overbought
		if(duration == 240)
		{
			entryRule = ( new OverIndicatorRule(shortSma, longSma).or(new OverIndicatorRule(closePrice, shortSma)))
					.and( new OverIndicatorRule(macd, emaMacd))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
		}

		// Selling rules
		// We want to sell:
		//  - if the short SMA under long SMA
		//  - and MACD is below emaMacd
		if(duration == 240)
		{
			exitRule = ( new UnderIndicatorRule(shortSma, longSma).or(new UnderIndicatorRule(closePrice, shortSma)))
					.and( new UnderIndicatorRule(macd, emaMacd))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
		}


		//Four hour rules. Use same rules as daily now. But we should fine tune it later.
		// Buying rules
		// We want to buy:
		//  - if the short SMA over long SMA
		//  - and MACD is above emaMacd
		// RSI is not oversold and overbought
		if(duration == 60)
		{
			entryRule = ( new OverIndicatorRule(shortSma, longSma).or(new OverIndicatorRule(closePrice, shortSma)))
					.and( new OverIndicatorRule(macd, emaMacd))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
		}

		// Selling rules
		// We want to sell:
		//  - if the short SMA under long SMA
		//  - and MACD is below emaMacd
		if(duration == 60)
		{
			exitRule = ( new UnderIndicatorRule(shortSma, longSma).or(new UnderIndicatorRule(closePrice, shortSma)))
					.and( new UnderIndicatorRule(macd, emaMacd))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
		}


		//15 minutes and 5 minutes
		// Buying rules
		// We want to entery the trade:
		//  use patern to enter the trade
		if(duration == 15 || duration == 5 )
		{
			entryRule = ( new BooleanIndicatorRule(bullishEngulfingIndicator)
					.or(new BooleanIndicatorRule(bullishHaramiIndicator))
					.or(new BooleanIndicatorRule(dojiIndicator))
					.or(new BooleanIndicatorRule(threeWhiteSoldiersIndicator))		

					)
					//							.and( new OverIndicatorRule(macd, emaMacd))
					//							.and(new UnderIndicatorRule(RSI, Decimal.valueOf("70")))
					//							.and(new OverIndicatorRule(RSI, Decimal.valueOf("20")))
					;
		}		

		if(duration == 15 || duration == 5 )
		{
			exitRule = ( new UnderIndicatorRule(shortSma, longSma).or(new UnderIndicatorRule(closePrice, shortSma)))
					.and( new UnderIndicatorRule(macd, emaMacd))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
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
			exitRule = ( new OverIndicatorRule(shortSma, longSma).or(new OverIndicatorRule(closePrice, shortSma)))
					.and( new OverIndicatorRule(macd, emaMacd))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
		}

		// Selling rules
		// We want to sell:
		//  - if the short SMA under long SMA
		//  - and MACD is below emaMacd
		if(duration == 1440)
		{
			entryRule = ( new UnderIndicatorRule(shortSma, longSma).or(new UnderIndicatorRule(closePrice, shortSma)))
					.and( new UnderIndicatorRule(macd, emaMacd))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
		}


		//Four hour rules. Use same rules as daily
		// Buying rules
		// We want to buy:
		//  - if the short SMA over long SMA
		//  - and MACD is above emaMacd
		// RSI is not oversold and overbought
		if(duration == 240)
		{
			exitRule = ( new OverIndicatorRule(shortSma, longSma).or(new OverIndicatorRule(closePrice, shortSma)))
					.and( new OverIndicatorRule(macd, emaMacd))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
		}

		// Selling rules
		// We want to sell:
		//  - if the short SMA under long SMA
		//  - and MACD is below emaMacd
		if(duration == 240)
		{
			entryRule = ( new UnderIndicatorRule(shortSma, longSma).or(new UnderIndicatorRule(closePrice, shortSma)))
					.and( new UnderIndicatorRule(macd, emaMacd))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
		}


		//Four hour rules. Use same rules as daily now. But we should fine tune it later.
		// Buying rules
		// We want to buy:
		//  - if the short SMA over long SMA
		//  - and MACD is above emaMacd
		// RSI is not oversold and overbought
		if(duration == 60)
		{
			exitRule = ( new OverIndicatorRule(shortSma, longSma).or(new OverIndicatorRule(closePrice, shortSma)))
					.and( new OverIndicatorRule(macd, emaMacd))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
		}

		// Selling rules
		// We want to sell:
		//  - if the short SMA under long SMA
		//  - and MACD is below emaMacd
		if(duration == 60)
		{
			entryRule = ( new UnderIndicatorRule(shortSma, longSma).or(new UnderIndicatorRule(closePrice, shortSma)))
					.and( new UnderIndicatorRule(macd, emaMacd))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
		}


		//Use pattern to recognize reversal on 15 and 5 minutes time frame.



		//15 minutes and 5 minutes
		// Buying rules
		// We want to entery the trade:
		//  use patern to enter the trade
		if(duration == 15 || duration == 5 )
		{
			entryRule = ( new BooleanIndicatorRule(bearishEngulfingIndicator)
					.or(new BooleanIndicatorRule(bearishHaramiIndicator))
					.or(new BooleanIndicatorRule(dojiIndicator))
					.or(new BooleanIndicatorRule(threeBlackCrowsIndicator))		

					)
					//							.and( new OverIndicatorRule(macd, emaMacd))
					//							.and(new UnderIndicatorRule(RSI, Decimal.valueOf("70")))
					//							.and(new OverIndicatorRule(RSI, Decimal.valueOf("20")))
					;
		}	
		
		if(duration == 15 || duration == 5 )
		{	
		exitRule = ( new OverIndicatorRule(shortSma, longSma).or(new OverIndicatorRule(closePrice, shortSma)))
				.and( new OverIndicatorRule(macd, emaMacd))
				.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
				.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
		}
		
		Strategy buySellSignals = new Strategy(entryRule, exitRule);
		return buySellSignals;
	}



}

