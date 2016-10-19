package apidemo;

import eu.verdelhan.ta4j.Tick;

public class TechnicalAnalyzerResult {
	public Tick processedTick = null;
	public String technicalSignal = "None";
	public double longSMA = 0.0;
	public double shortSMA = 0.0;
	
	TechnicalAnalyzerResult(Tick tick, String signal, double lSMA, double sSMA){
		processedTick = tick;
		technicalSignal = signal;
		longSMA = lSMA;
		shortSMA = sSMA;
	}
}
