package apidemo;

import com.ib.client.Types.TechnicalSignalTrend;

import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;

public class TechnicalAnalyzerResult {
	public Tick processedTick = null;
	public boolean technicalSignalLongEntry = false;
	public boolean technicalSignalLongExit = false;

	public boolean technicalSignalShortEntry = false;
	public boolean technicalSignalShortExit = false;
	
	public double longSMA = 0.0;
	public double shortSMA = 0.0;
	public int endIndex;
	public TimeSeries series;
	
	TechnicalAnalyzerResult(Tick tick, int endIndexIn, TimeSeries seriesIn, boolean technicalSignalLongEntry, boolean technicalSignalLongExit, boolean technicalSignalShortEntry, boolean technicalSignalShortExit, double lSMA, double sSMA){
		processedTick = tick;
		this.technicalSignalLongEntry = technicalSignalLongEntry;
		this.technicalSignalLongExit = technicalSignalLongExit;
		
		this.technicalSignalShortEntry = technicalSignalShortEntry;
		this.technicalSignalShortExit = technicalSignalShortExit;
		
		longSMA = lSMA;
		shortSMA = sSMA;
		endIndex = endIndexIn;
		series = seriesIn;
	}
}
