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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
public class TechinicalAnalyzer extends Thread{

    /** Close price of the last tick */
    private static Decimal LAST_TICK_CLOSE_PRICE;
    private Contract currencyContractHost;
	private static Boolean historicalDataEnd;
	private ApiDemo apiDemoHost;
	private boolean newTickAvailable;
	private ConcurrentHashMap<Long, forex> orderHashMapHost;
	private ConcurrentHashMap<String, Contract> ContractHashMapHost;

	final SimpleDateFormat DATEOnly_FORMAT = new SimpleDateFormat("yyyyMMdd");
	final SimpleDateFormat TIMEOnly_FORMAT = new SimpleDateFormat("HH:mm:ss");



    public TechinicalAnalyzer(ApiDemo apiDemo, Contract currencyContract, ConcurrentHashMap<String, Contract> contractHashMap , ConcurrentHashMap<Long, forex> orderHashMap){
    	currencyContractHost = currencyContract;
    	apiDemoHost = apiDemo;
    	orderHashMapHost = orderHashMap;
    	ContractHashMapHost = contractHashMap;
    }
    
    public synchronized  void run(){
    	//();
    	
    	Tick newTick  = null;
    	Bar bar = null;
    //	while(true)
    	{


            System.out.println("********************** Initialization **********************");
            // Getting the time series
            
//            //Simulated testing for different dataset.
//            TicksAccesser ticksAccess = new TicksAccesser(null);
//            currencyContractHost.historicalBarMap = ticksAccess.readFromCsv("NZDUSD_ticks_history_2007_to_2016.csv");
//			ticksAccess.start();
            
            while(currencyContractHost.historicalBarMap.size() < 6000){
            	double randomWait = Math.random() * ( 60 - 30 );
                try {
					Thread.sleep((long) (1000));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} // I know...
 //           	currencyContractHost.historicalBarMap.notify();
            }
            
            TimeSeries series = buildTimeSeriesFromMap(6000);

            // Building the trading strategy
            Strategy longStrategy = buildLongStrategy(series);
            Strategy shortStrategy = buildShortStrategy(series);

            newTickAvailable = true;
            // Initializing the trading history
            TradingRecord longtradingRecord = new TradingRecord();
            TradingRecord shorttradingRecord = new TradingRecord();

            System.out.println("************************************************************");
            
            /**
             * We run the strategy for the 50 next ticks.
             */
            while (true) {

                // New tick
            	while(currencyContractHost.historicalBarMap.size() == 0 && !newTickAvailable){
            	 try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            	}
//				apiDemoHost.controller().reqHistoricalData(currencyContractHost, DATE_FORMAT.format(new Date()), 60*30, DurationUnit.SECOND, BarSize._5_mins, WhatToShow.MIDPOINT, true, forexHistoricalHandler);
//				historicalDataEnd = false;
//				try {
//					synchronized(TechinicalAnalyzer.historicalDataEnd){
//					historicalDataEnd.wait();
//					}
//				} catch (InterruptedException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
//			           while(newTickAvailable == false)
			           {
//			            	double randomWait = Math.random() * ( 30 - 10 );
//			                try {
//								Thread.sleep((long) (randomWait * 1000));
//							} catch (InterruptedException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							} // I know...
							
							//Insert new data into series
			        	   
							Date lastProcessedtime = series.getLastTick().getEndTime().toDate();
							
							

							{
								SortedSet<Long> keys = new TreeSet<Long>(currencyContractHost.historicalBarMap.keySet());
//								TreeSet<Long> treereverse = (TreeSet<Long>) keys.descendingSet();
			
								for (Long key : keys){
									bar = currencyContractHost.historicalBarMap.get(key);
			//					currencyContractHost.historicalBarMap.remove(key);
									
									 
									//If last acquired time is before this tick time, which means this data is acquired and should be abandoned.
									if(lastProcessedtime.compareTo(new Date(bar.time() * 1000)) >= 0){
										currencyContractHost.historicalBarMap.remove(key);
										continue;
										}
									
					                double open = bar.open();
					                double high = bar.high();
					                double low = bar.low();
					                double close = bar.close();
					                double volume = bar.volume();
					                newTick = new Tick(new DateTime(bar.time() * 1000), open, high, low, close, volume);
					                series.addTick(newTick);
									newTickAvailable = true;
									break;
									
								}
							}
							newTickAvailable = false;
							newTick = series.getLastTick();
							lastProcessedtime = newTick.getEndTime().toDate();
							
			//                Tick newTick = generateRandomTick();
			//                System.out.println("------------------------------------------------------\n"
			//                        + "Tick "+ new Date() +" added, close price = " + newTick.getClosePrice().toDouble());
			//                series.addTick(newTick);
			                
							
		            	}
                int endIndex = series.getEnd();
                if (longStrategy.shouldEnter(endIndex)) {
                    // Our strategy should enter
                    System.out.println(newTick.getSimpleDateName() + " Strategy should ENTER LONG on " + endIndex);
                    boolean entered = longtradingRecord.enter(endIndex, newTick.getClosePrice(), Decimal.TEN);
                    if (entered) {
                        Order entry = longtradingRecord.getLastEntry();
                        System.out.println("Entered on " + entry.getIndex()
                                + " (price=" + entry.getPrice().toDouble()
                                + ", amount=" + entry.getAmount().toDouble() + ")");
                    }
                    
                   
                    currencyContractHost.m_currentTechnicalSignal = "BUY";
                    ContractHashMapHost.put(currencyContractHost.symbol() + currencyContractHost.currency(), currencyContractHost);
               
                    placeTestMarketOrder("BUY");

                    
                } else if (longStrategy.shouldExit(endIndex)) {
                    // Our strategy should exit
                    System.out.println(newTick.getSimpleDateName() + " Strategy should EXIT LONG on " + endIndex);
                    boolean exited = longtradingRecord.exit(endIndex, newTick.getClosePrice(), Decimal.TEN);
                    if (exited) {
                        Order exit = longtradingRecord.getLastExit();
                        System.out.println("Exited on " + exit.getIndex()
                                + " (price=" + exit.getPrice().toDouble()
                                + ", amount=" + exit.getAmount().toDouble() + ")");
                    }
                    
                    currencyContractHost.m_currentTechnicalSignal = "CLOSE";
                    ContractHashMapHost.put(currencyContractHost.symbol() + currencyContractHost.currency(), currencyContractHost);
                    placeTestMarketOrder("CLOSE");

                    
                    
                }
                
                //If it is a short
                endIndex = series.getEnd();
                if (shortStrategy.shouldEnter(endIndex)) {
                    // Our strategy should enter
                    System.out.println(newTick.getSimpleDateName() + " Strategy should ENTER SHORT on " + endIndex);
                    boolean entered = shorttradingRecord.enter(endIndex, newTick.getClosePrice(), Decimal.TEN);
                    if (entered) {
                        Order entry = shorttradingRecord.getLastEntry();
                        System.out.println("Entered on " + entry.getIndex()
                                + " (price=" + entry.getPrice().toDouble()
                                + ", amount=" + entry.getAmount().toDouble() + ")");
                    }
                    
                    currencyContractHost.m_currentTechnicalSignal = "SELL";
                    ContractHashMapHost.put(currencyContractHost.symbol() + currencyContractHost.currency(), currencyContractHost);
                    placeTestMarketOrder("SELL");

                    
                } else if (shortStrategy.shouldExit(endIndex)) {
                    // Our strategy should exit
                    System.out.println(newTick.getSimpleDateName() + " Strategy should EXIT SHORT on " + endIndex);
                    boolean exited = shorttradingRecord.exit(endIndex, newTick.getClosePrice(), Decimal.TEN);
                    if (exited) {
                        Order exit = shorttradingRecord.getLastExit();
                        System.out.println("Exited on " + exit.getIndex()
                                + " (price=" + exit.getPrice().toDouble()
                                + ", amount=" + exit.getAmount().toDouble() + ")");
                    }
                    
                    currencyContractHost.m_currentTechnicalSignal = "CLOSE";
                    ContractHashMapHost.put(currencyContractHost.symbol() + currencyContractHost.currency(), currencyContractHost);
                    
                    placeTestMarketOrder("CLOSE");
                    
                }
                
//              SimpleDateFormat formatter = new SimpleDateFormat("yyyy");
//            	try {
//					if((formatter.parse("2015")).compareTo(new Date(bar.time() * 1000)) <= 0){
//						break;
//						}
//				} catch (ParseException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
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
    
    private TimeSeries buildTimeSeriesFromMap(int seriesLimit){
		TreeSet<Long> keys = new TreeSet<Long>(currencyContractHost.historicalBarMap.keySet());
//		TreeSet<Long> treereverse = (TreeSet<Long>) keys.descendingSet();
		boolean completed = false;
		ArrayList<Tick> ticks = new ArrayList<Tick>();

		for (Long key : keys){
		Bar bar = currencyContractHost.historicalBarMap.get(key);
		currencyContractHost.historicalBarMap.remove(key);
		System.out.println(currencyContractHost.symbol() + currencyContractHost.currency() + " time: " + bar.formattedTime()+ " bar high: " + bar.high() + " bar low: " + bar.low() + " bar close: " + bar.close() + " bar volume: " + bar.volume());
//		show(m_currencyContract.symbol() + m_currencyContract.currency() + " time: " + bar.formattedTime()+ " bar high: " + bar.high() + " bar low: " + bar.low() + " bar close: " + bar.close());
//
//		
//
//      
//       
//      String[] line;
//  //          while ((line = csvReader.readNext()) != null) 
//            {
				
				
				

			//	if(!yearKeyMap.containsKey(date.getYear()))
				{
				//	yearKeyMap.put(date.getYear(), ticks);
				}
//				ticks = yearKeyMap.get(date.getYear());

				
                double open = bar.open();
                double high = bar.high();
                double low = bar.low();
                double close = bar.close();
                double volume = bar.volume();
                Tick tick = new Tick(new DateTime(bar.time() * 1000), open, high, low, close, volume);
                ticks.add(tick);
                
            }

		if(ticks.isEmpty())
			return null;
		 TimeSeries series = new TimeSeries(currencyContractHost.symbol() + currencyContractHost.currency() + "_ticks", ticks);
		 series.setMaximumTickCount(seriesLimit);
        LAST_TICK_CLOSE_PRICE = series.getTick(series.getEnd()).getClosePrice();
		 return series;
    }
    
    
    /**
     * Builds a moving time series (i.e. keeping only the maxTickCount last ticks)
     * @param maxTickCount the number of ticks to keep in the time series (at maximum)
     * @return a moving time series
     */
    private static TimeSeries initMovingTimeSeries(int maxTickCount) {
        TimeSeries series = CsvTradesLoader.loadBitstampSeries();
        System.out.print("Initial tick count: " + series.getTickCount());
        // Limitating the number of ticks to maxTickCount
        series.setMaximumTickCount(maxTickCount);
        LAST_TICK_CLOSE_PRICE = series.getTick(series.getEnd()).getClosePrice();
        System.out.println(" (limited to " + maxTickCount + "), close price = " + LAST_TICK_CLOSE_PRICE);
        return series;
    }

    /**
     * @param series a time series
     * @return a dummy strategy
     */
    private static Strategy buildLongStrategy(TimeSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma9 = new SMAIndicator(closePrice, 9);

        SMAIndicator sma14 = new SMAIndicator(closePrice, 14);

     // Getting the simple moving average (SMA) of the close price over the last 5 ticks
        SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
        SMAIndicator shortSmaHourly = new SMAIndicator(closePrice, 5 * 12);

        // Here is the 5-ticks-SMA value at the 42nd index
        System.out.println("5-ticks-SMA value at the 42nd index: " + shortSma.getValue(42).toDouble());

        // Getting a longer SMA (e.g. over the 30 last ticks)
        SMAIndicator longSma = new SMAIndicator(closePrice, 10);
        SMAIndicator longSmaHourly = new SMAIndicator(closePrice, 10 * 12);


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
        Rule buyingRule = new CrossedUpIndicatorRule(shortSmaHourly, longSmaHourly)
        		.and( new CrossedUpIndicatorRule(shortSma, longSma))
                .and(new CrossedUpIndicatorRule(sof, sos))
//                .and(new UnderIndicatorRule(rsi, Decimal.valueOf("50")));
               .and(new OverIndicatorRule(rsi, Decimal.valueOf("20")));
        
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
    private static Strategy buildShortStrategy(TimeSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma9 = new SMAIndicator(closePrice, 9);

        SMAIndicator sma14 = new SMAIndicator(closePrice, 14);

        
        
        
     // Getting the simple moving average (SMA) of the close price over the last 5 ticks
        SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
        
        // Getting the simple moving average (SMA) of the close price over the last 5 ticks
        SMAIndicator shortSmaHourly = new SMAIndicator(closePrice, 5 * 12);    
        
        // Here is the 5-ticks-SMA value at the 42nd index
        System.out.println("5-ticks-SMA value at the 42nd index: " + shortSma.getValue(42).toDouble());

        // Getting a longer SMA (e.g. over the 30 last ticks)
        SMAIndicator longSma = new SMAIndicator(closePrice, 10);
        SMAIndicator longSmaHourly = new SMAIndicator(closePrice, 10 * 12);


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
        Rule sellingRule = new CrossedDownIndicatorRule(shortSmaHourly, longSmaHourly)
        		.and(new CrossedDownIndicatorRule(shortSma, longSma))
                .and(new CrossedDownIndicatorRule(sof, sos))
//                .and(new UnderIndicatorRule(rsi, Decimal.valueOf("50")));
               .and(new UnderIndicatorRule(rsi, Decimal.valueOf("60")));
        
        // Selling rules
        // We want to sell:
        //  - if the 5-ticks SMA crosses under 30-ticks SMA
        //  - or if if the price looses more than 3%
        //  - or if the price earns more than 2%
        Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma)
        		.and(new CrossedUpIndicatorRule(sof, sos))
        		.and(new OverIndicatorRule(rsi, Decimal.valueOf("20")))
        		.or(new CrossedUpIndicatorRule(shortSmaHourly, longSmaHourly))
                .or(new StopLossRule(closePrice, Decimal.valueOf("0.2")))
                .or(new StopGainRule(closePrice, Decimal.valueOf("1.0")));
        
        // Running our juicy trading strategy...
   //     TradingRecord tradingRecord = series.run(new Strategy(buyingRule, sellingRule));        
        
        
        
        // Signals
        // Buy when SMA goes over close price
        // Sell when close price goes over SMA
        Strategy buySellSignals = new Strategy(sellingRule, buyingRule);
        return buySellSignals;
    }
    
    
    
    /**
     * Generates a random decimal number between min and max.
     * @param min the minimum bound
     * @param max the maximum bound
     * @return a random decimal number between min and max
     */
    private static Decimal randDecimal(Decimal min, Decimal max) {
        Decimal randomDecimal = null;
        if (min != null && max != null && min.isLessThan(max)) {
            randomDecimal = max.minus(min).multipliedBy(Decimal.valueOf(Math.random())).plus(min);
        }
        return randomDecimal;
    }

   private void placeTestMarketOrder(String action){
	   
	   
	   forex orderDetail = new forex();
       orderDetail.Symbol = currencyContractHost.symbol() + currencyContractHost.currency();
       orderDetail.TradeMethod = action;
       orderDetail.EntryMethod = "MKT";
       orderDetail.Quantity = "25000";
       orderDetail.ValidDuration = "120";
       orderDetail.TriggerPct = "0";
       orderDetail.LossPct = "0.2";
       orderDetail.groupId = (long) 0;
       Date currentTime = new Date();
       orderDetail.Date = DATEOnly_FORMAT.format(currentTime);
       orderDetail.Time = TIMEOnly_FORMAT.format(currentTime);

		  SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
	  Object orderDateStr = formatter.format(currentTime);
	  long currentDateCode = orderDetail.orderSeqNo = Long.parseLong(orderDateStr + "00");
      while(orderHashMapHost.containsKey(orderDetail.orderSeqNo)){
   	   orderDetail.orderSeqNo++;
      }
      orderHashMapHost.put(orderDetail.orderSeqNo, orderDetail);
       
       apiDemoHost.placeMarketOrder(orderDetail);
   
	   }
    
}

