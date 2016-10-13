package apidemo;

import eu.verdelhan.ta4j.Tick;

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

import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.helpers.AverageTrueRangeIndicator;
import eu.verdelhan.ta4j.indicators.statistics.StandardDeviationIndicator;
import eu.verdelhan.ta4j.indicators.oscillators.PPOIndicator;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.simple.PriceVariationIndicator;
import eu.verdelhan.ta4j.indicators.simple.TypicalPriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.EMAIndicator;
import eu.verdelhan.ta4j.indicators.trackers.ROCIndicator;
import eu.verdelhan.ta4j.indicators.trackers.RSIIndicator;
import eu.verdelhan.ta4j.indicators.trackers.SMAIndicator;
import eu.verdelhan.ta4j.indicators.trackers.WilliamsRIndicator;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.Instant;

import com.ib.controller.Bar;
import com.opencsv.CSVReader;

import ta4jexamples.loaders.CsvTradesLoader;

/**
 * This class builds a CSV file containing values from indicators.
 */
public class TicksAccesser extends Thread{
	TimeSeries series;
	String fileNameLocal;
	public ConcurrentHashMap<Long, Bar> historicalBarMap = new ConcurrentHashMap<Long, Bar>();
	public TicksAccesser(TimeSeries seriesIn){
		series = seriesIn;
	}
	
	public ConcurrentHashMap<Long, Bar> readFromCsv(String fileName){
		fileNameLocal = fileName;
		return historicalBarMap;
	}
		
	public void run(){
    	
   	 CSVReader reader = null;
		
   	
   	
       // Reading all lines of the CSV file
 //      InputStream stream = CsvTradesLoader.class.getClassLoader().getResourceAsStream("bitstamp_trades_from_20131125_usd.csv");
       InputStream stream = null;
		try {
			stream = new FileInputStream(fileNameLocal);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       CSVReader csvReader = null;
       String[] lines = null;
       
       
       try {
           csvReader = new CSVReader(new InputStreamReader(stream, Charset.forName("UTF-8")), ',');
           do{
           lines = csvReader.readNext();
           processLine(lines);
           }while(lines != null && lines.length > 0);
   //        lines.remove(0); // Removing header line
       } catch (IOException ioe) {
           Logger.getLogger(CsvTradesLoader.class.getName()).log(Level.SEVERE, "Unable to load trades from CSV", ioe);
       } finally {
           if (csvReader != null) {
               try {
                   csvReader.close();
               } catch (Exception ioe) {
       			ioe.printStackTrace();

               }
           }
       }
	}
	
	private void processLine(String[] lines)
	{
       List<Tick> ticks = null;
       if ((lines != null) && lines.length != 0) {

           // Getting the first and last trades timestamps
           DateTime beginTime = new DateTime((long)(Double.parseDouble(lines[0]) * 1000));
 //          DateTime endTime = new DateTime(Long.parseLong(lines.get(lines.size() - 1)[0]) * 1000);
 //          if (beginTime.isAfter(endTime)) 
           {
               Instant beginInstant = beginTime.toInstant();
      //         Instant endInstant = endTime.toInstant();
               beginTime = new DateTime(beginInstant);
    //           endTime = new DateTime(beginInstant);
           }
           // Building the empty ticks (every 300 seconds, yeah welcome in Bitcoin world)
      //     ticks = buildEmptyTicks(beginTime, endTime, 300);
           // Filling the ticks with trades
         //  for (String[] tradeLine : lines) 
           {
       //        DateTime tradeTimestamp = new DateTime(Long.parseLong(lines[0]) * 1000);
         //      for (Tick tick : ticks) 
               {
                   //if (tick.inPeriod(tradeTimestamp)) 
                   {
           			   long endTime = (long)(Double.parseDouble(lines[0]));
                       double openPrice = Double.parseDouble(lines[3]);
                       double closePrice = Double.parseDouble(lines[4]);
                       double maxPrice = Double.parseDouble(lines[5]);
                       double minPrice = Double.parseDouble(lines[6]);
                       Bar newBar = new Bar(endTime, maxPrice, minPrice, openPrice, closePrice, 0, 0, 0);
                       historicalBarMap.put(endTime, newBar);
                       if(historicalBarMap.size() > 7000)
                       {
                    	   try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                       }
 //                      historicalBarMap.notify();
//                       tick.addTrade(tradeAmount, tradePrice);
                   }
               }
           }
           // Removing still empty ticks
      //     removeEmptyTicks(ticks);
       }

  //     return new TimeSeries("bitstamp_trades", ticks);
   
		
//		return series;
		
	}
	
	 public void writeToCsv() {


	        /**
	         * Getting time series
	         */
	   //     TimeSeries series = CsvTradesLoader.loadBitstampSeries();

	        /**
	         * Creating indicators
	         */
			
			
			
	        // Close price
	        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
	        // Typical price
	        TypicalPriceIndicator typicalPrice = new TypicalPriceIndicator(series);
	        // Price variation
	        PriceVariationIndicator priceVariation = new PriceVariationIndicator(series);
	        // Simple moving averages
	        SMAIndicator shortSma = new SMAIndicator(closePrice, 8);
	        SMAIndicator longSma = new SMAIndicator(closePrice, 20);
	        // Exponential moving averages
	        EMAIndicator shortEma = new EMAIndicator(closePrice, 8);
	        EMAIndicator longEma = new EMAIndicator(closePrice, 20);
	        // Percentage price oscillator
	        PPOIndicator ppo = new PPOIndicator(closePrice, 12, 26);
	        // Rate of change
	        ROCIndicator roc = new ROCIndicator(closePrice, 100);
	        // Relative strength index
	        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
	        // Williams %R
	        WilliamsRIndicator williamsR = new WilliamsRIndicator(series, 20);
	        // Average true range
	        AverageTrueRangeIndicator atr = new AverageTrueRangeIndicator(series, 20);
	        // Standard deviation
	        StandardDeviationIndicator sd = new StandardDeviationIndicator(closePrice, 14);

	        /**
	         * Building header
	         */
	        BufferedWriter writer = null;

	        StringBuilder sb = new StringBuilder("timestamp,date,time,open,close,Max,Min,sma20,ema8,ema20,ppo,roc,rsi,williamsr,atr,sd\n");
	        try {
	            writer = new BufferedWriter(new FileWriter(series.getName() +  "_history.csv", true)); //Append it instead of orverwrite it.
	   //         writer.write(sb.toString());

	        /**
	         * Adding indicators values
	         */

	            final int nbTicks = series.getTickCount();
	            for (int i = 0; i < nbTicks; i++) {
	            sb.setLength(0);
		
	            sb.append(series.getTick(i).getEndTime().getMillis() / 1000d).append(',')
	            .append(series.getTick(i).getEndTime().toLocalDate()).append(',')
	            .append(series.getTick(i).getEndTime().toLocalTime()).append(',')
	            .append(series.getTick(i).getOpenPrice()).append(',')
	            .append(series.getTick(i).getClosePrice()).append(',')
	            .append(series.getTick(i).getMaxPrice()).append(',')
	            .append(series.getTick(i).getMinPrice()).append(',').append('\n');
	            writer.write(sb.toString());

//	            .append(longSma.getValue(i)).append(',')
//	            .append(shortEma.getValue(i)).append(',')
//	            .append(longEma.getValue(i)).append(',')
//	            .append(ppo.getValue(i)).append(',')
//	            .append(roc.getValue(i)).append(',')
//	            .append(rsi.getValue(i)).append(',')
//	            .append(williamsR.getValue(i)).append(',')
//	            .append(atr.getValue(i)).append(',')
//	            .append(sd.getValue(i)).append('\n');
	       //     gc(sb);
	        }

	        /**
	         * Writing CSV file
	         */
	            sb.setLength(0);
	        } catch (IOException ioe) {
	            Logger.getLogger(TicksAccesser.class.getName()).log(Level.SEVERE, "Unable to write CSV file", ioe);
	        } finally {
	            try {
	                if (writer != null) {
	                    writer.close();
	                }
	            } catch (IOException ioe) {
	            }
	        }

	    
		}
	
	
    public static void main(String[] args) {

        /**
         * Getting time series
         */
        TimeSeries series = CsvTradesLoader.loadBitstampSeries();

        /**
         * Creating indicators
         */
        // Close price
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        // Typical price
        TypicalPriceIndicator typicalPrice = new TypicalPriceIndicator(series);
        // Price variation
        PriceVariationIndicator priceVariation = new PriceVariationIndicator(series);
        // Simple moving averages
        SMAIndicator shortSma = new SMAIndicator(closePrice, 8);
        SMAIndicator longSma = new SMAIndicator(closePrice, 20);
        // Exponential moving averages
        EMAIndicator shortEma = new EMAIndicator(closePrice, 8);
        EMAIndicator longEma = new EMAIndicator(closePrice, 20);
        // Percentage price oscillator
        PPOIndicator ppo = new PPOIndicator(closePrice, 12, 26);
        // Rate of change
        ROCIndicator roc = new ROCIndicator(closePrice, 100);
        // Relative strength index
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        // Williams %R
        WilliamsRIndicator williamsR = new WilliamsRIndicator(series, 20);
        // Average true range
        AverageTrueRangeIndicator atr = new AverageTrueRangeIndicator(series, 20);
        // Standard deviation
        StandardDeviationIndicator sd = new StandardDeviationIndicator(closePrice, 14);

        /**
         * Building header
         */
        StringBuilder sb = new StringBuilder("timestamp,close,typical,variation,sma8,sma20,ema8,ema20,ppo,roc,rsi,williamsr,atr,sd\n");

        /**
         * Adding indicators values
         */
        final int nbTicks = series.getTickCount();
        for (int i = 0; i < nbTicks; i++) {
            sb.append(series.getTick(i).getEndTime().getMillis() / 1000d).append(',')
            .append(closePrice.getValue(i)).append(',')
            .append(typicalPrice.getValue(i)).append(',')
            .append(priceVariation.getValue(i)).append(',')
            .append(shortSma.getValue(i)).append(',')
            .append(longSma.getValue(i)).append(',')
            .append(shortEma.getValue(i)).append(',')
            .append(longEma.getValue(i)).append(',')
            .append(ppo.getValue(i)).append(',')
            .append(roc.getValue(i)).append(',')
            .append(rsi.getValue(i)).append(',')
            .append(williamsR.getValue(i)).append(',')
            .append(atr.getValue(i)).append(',')
            .append(sd.getValue(i)).append('\n');
        }

        /**
         * Writing CSV file
         */
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter("indicators.csv"));
            writer.write(sb.toString());
        } catch (IOException ioe) {
            Logger.getLogger(TicksAccesser.class.getName()).log(Level.SEVERE, "Unable to write CSV file", ioe);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ioe) {
            }
        }

    }
}
