package apidemo;

import com.ib.client.Types.TechnicalSignalTrend;

import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;

public class TechnicalAnalyzerResult {
	public Tick processedTick = null;
	public TechnicalSignalTrend technicalSignalUp = TechnicalSignalTrend.NONE;
	public TechnicalSignalTrend technicalSignalDown = TechnicalSignalTrend.NONE;
	public double longSMA = 0.0;
	public double shortSMA = 0.0;
	public int endIndex;
	public TimeSeries series;
	
	TechnicalAnalyzerResult(Tick tick, int endIndexIn, TimeSeries seriesIn, TechnicalSignalTrend signalUp, TechnicalSignalTrend signalDown, double lSMA, double sSMA){
		processedTick = tick;
		technicalSignalUp = signalUp;
		technicalSignalDown = signalDown;
		longSMA = lSMA;
		shortSMA = sSMA;
		endIndex = endIndexIn;
		series = seriesIn;
	}
}
