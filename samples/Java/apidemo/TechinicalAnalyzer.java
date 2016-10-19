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
import eu.verdelhan.ta4j.indicators.simple.MedianPriceIndicator;
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
	private String currentTechnicalSignal;

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
	private MedianPriceIndicator medPrice;
	private SMAIndicator longMedSma;

	private int fastSMAPeriod = 7;
	private int slowSMAPeriod = 13;


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

	public Tick initDB(){
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

		// Getting the simple moving average (SMA) of the close price over the last 5 ticks
		shortSma = new SMAIndicator(closePrice, fastSMAPeriod);

		// Here is the 5-ticks-SMA value at the 42nd index
		System.out.println("5-ticks-SMA value at the 42nd index: " + shortSma.getValue(42).toDouble());

		// Getting a longer SMA (e.g. over the 30 last ticks)
		longSma = new SMAIndicator(closePrice, slowSMAPeriod);


		// Relative strength index
		rsi = new RSIIndicator(closePrice, 14);

		sofStoch = new StochasticOscillatorKIndicator(series, 14);
		smaStoch = new SMAIndicator(sofStoch, 3);
		sosStoch = new StochasticOscillatorDIndicator(smaStoch);

		medPrice = new MedianPriceIndicator(series);
		longMedSma = new SMAIndicator(medPrice, 10);

		maxPrice = new MaxPriceIndicator(series);
		longMaxSma = new SMAIndicator(maxPrice, 10);


		// Building the trading strategy
		longStrategy = buildLongStrategy(series);
		shortStrategy = buildShortStrategy(series);


		System.out.println("********************Finish initialize Database****************************************");
		return series.getLastTick();
	}


	public   TechnicalAnalyzerResult analyze(Date lastProcessedtime ){
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
		System.out.println("-----------Techinical Analyzer " + currencyContractHost.symbol() + currencyContractHost.currency() + "------\n"
				+ "Tick "+ newTick.getDateName() +" added, close price = " + newTick.getClosePrice().toDouble()
				+ " open price = " + newTick.getOpenPrice().toDouble() + " High price = " + newTick.getMaxPrice().toDouble()  
				 + " Low price = " + newTick.getMinPrice().toDouble() );
		//                series.addTick(newTick);



		endIndex = series.getEnd();


		System.out.println(series.getLastTick().getDateName() + " "  + durationHost + " minutes" + "------Techinical Analyzer begin" + currencyContractHost.symbol() + currencyContractHost.currency() + "------");
		System.out.println(series.getLastTick().getDateName() + " Closed Price: " + closePrice.getValue(endIndex));
		System.out.println(series.getLastTick().getDateName() + durationHost + " minutes SMA 5: " + shortSma.getValue(endIndex));
		System.out.println(series.getLastTick().getDateName() + durationHost  + " minutes SMA 10: " + longSma.getValue(endIndex));

		System.out.println(series.getLastTick().getDateName() + durationHost  + " minutes RSI: " + rsi.getValue(endIndex));
		System.out.println(series.getLastTick().getDateName() + durationHost  + " minutes Stoch K: " + sofStoch.getValue(endIndex) + " D: " + sosStoch.getValue(endIndex));
		System.out.println(series.getLastTick().getDateName() + durationHost  + " minutes Stoch K corss down D  is " + new CrossedDownIndicatorRule(sofStoch, sosStoch).isSatisfied(endIndex));
		System.out.println(series.getLastTick().getDateName() + durationHost  + " minutes Stoch K under D  is " + new UnderIndicatorRule(sofStoch, sosStoch).isSatisfied(endIndex));
		System.out.println(series.getLastTick().getDateName().toString()+ " " + durationHost + " minutes" +  "-----Techinical Analyzer end" + currencyContractHost.symbol() + currencyContractHost.currency() + "------");


		if (longStrategy.shouldEnter(endIndex)) {
			// Our strategy should enter
			System.out.println(series.getLastTick().getDateName() + " " + durationHost + " minutes Strategy should ENTER LONG on " + endIndex);
			currentTechnicalSignal = "UP";




		} else if (longStrategy.shouldExit(endIndex)) {
			// Our strategy should exit
			System.out.println(series.getLastTick().getDateName() + " " + durationHost + " minutes  Strategy should EXIT LONG on " + endIndex);
			currentTechnicalSignal = "DOWN";





		}else{
			// Our strategy should WAIT
			System.out.println(series.getLastTick().getDateName() + " " + durationHost + " minutes LONG Strategy should WAIT  on " + endIndex);

		}

		//If it is a short
		endIndex = series.getEnd();
		if (shortStrategy.shouldEnter(endIndex)) {
			// Our strategy should enter
			System.out.println(series.getLastTick().getDateName() + " " + durationHost + " minutes  Strategy should ENTER SHORT on " + endIndex);
			currentTechnicalSignal = "DOWN";





		} else if (shortStrategy.shouldExit(endIndex)) {
			// Our strategy should exit
			System.out.println(series.getLastTick().getSimpleDateName() + " " + durationHost + " minutes  Strategy should EXIT SHORT on " + endIndex);
			currentTechnicalSignal = "UP";




		}else{
			// Our strategy should WAIT
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
		return new TechnicalAnalyzerResult(newTick, currentTechnicalSignal, longSma.getValue(endIndex).toDouble(), shortSma.getValue(endIndex).toDouble());
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

			//Let's keep 1 day data to process
			if(new DateTime(bar.time() * 1000).toDate().after(cal.getTime())){
				System.out.println("Tick time: " + cal.getTime());
				break;
			}
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
	 * @return a dummy strategy
	 */
	private Strategy buildLongStrategy(TimeSeries series) {
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
		if(durationHost == 5){
			buyingRule = ( new CrossedUpIndicatorRule(shortSma, longSma).or(new CrossedUpIndicatorRule(closePrice, shortSma)))
					.and( new OverIndicatorRule(sofStoch, sosStoch))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
		}else{
			buyingRule = ( new OverIndicatorRule(shortSma, longSma).or(new OverIndicatorRule(closePrice, shortSma)))
					.and( new OverIndicatorRule(sofStoch, sosStoch))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
		}

		// Selling rules
		// We want to sell:
		//  - if the 5-ticks SMA crosses under 30-ticks SMA
		//  - or if if the price looses more than 3%
		//  - or if the price earns more than 2%
		Rule sellingRule = null;
		if(durationHost == 5){
			sellingRule = ( new CrossedDownIndicatorRule(shortSma, longSma).or(new CrossedDownIndicatorRule(closePrice, shortSma)))
					.and( new UnderIndicatorRule(sofStoch, sosStoch))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
		}else{
			sellingRule = ( new UnderIndicatorRule(shortSma, longSma).or(new UnderIndicatorRule(closePrice, shortSma)))
					.and( new UnderIndicatorRule(sofStoch, sosStoch))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
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
	 * @return a dummy strategy
	 */
	private Strategy buildShortStrategy(TimeSeries series) {
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
		if(durationHost == 5){
			buyingRule = ( new CrossedUpIndicatorRule(shortSma, longSma).or(new CrossedUpIndicatorRule(closePrice, shortSma)))
					.and( new OverIndicatorRule(sofStoch, sosStoch))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
		}else{
			buyingRule = ( new OverIndicatorRule(shortSma, longSma).or(new OverIndicatorRule(closePrice, shortSma)))
					.and( new OverIndicatorRule(sofStoch, sosStoch))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
		}

		// Selling rules
		// We want to sell:
		//  - if the 5-ticks SMA crosses under 30-ticks SMA
		//  - or if if the price looses more than 3%
		//  - or if the price earns more than 2%
		Rule sellingRule = null;
		if(durationHost == 5){
			sellingRule = ( new CrossedDownIndicatorRule(shortSma, longSma).or(new CrossedDownIndicatorRule(closePrice, shortSma)))
					.and( new UnderIndicatorRule(sofStoch, sosStoch))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
		}else{
			sellingRule = ( new UnderIndicatorRule(shortSma, longSma).or(new UnderIndicatorRule(closePrice, shortSma)))
					.and( new UnderIndicatorRule(sofStoch, sosStoch))
					.and(new UnderIndicatorRule(rsi, Decimal.valueOf("70")))
					.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
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

