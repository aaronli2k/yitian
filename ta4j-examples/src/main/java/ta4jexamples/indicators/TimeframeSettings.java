package ta4jexamples.indicators;

import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
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
import eu.verdelhan.ta4j.indicators.simple.MedianPriceIndicator;
import eu.verdelhan.ta4j.indicators.simple.PriceVariationIndicator;
import eu.verdelhan.ta4j.indicators.simple.TypicalPriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.EMAIndicator;
import eu.verdelhan.ta4j.indicators.trackers.MACDIndicator;
import eu.verdelhan.ta4j.indicators.trackers.RSIIndicator;
import eu.verdelhan.ta4j.indicators.trackers.SMAIndicator;

public class TimeframeSettings {
	
//Oscillators	
public StochasticOscillatorDIndicator sosDStoch;
public StochasticOscillatorKIndicator sofKStoch;

//prices
public ClosePriceIndicator closePrice; 
public MedianPriceIndicator medianPrice; 
public PriceVariationIndicator priceVariation;
public TypicalPriceIndicator typicalPrice;

//trackers
public MACDIndicator macd;
public EMAIndicator emaMacd;


public SMAIndicator shortSMA;
public SMAIndicator longSMA;
public RSIIndicator RSI;

//various pattern recornizor 
public BullishEngulfingIndicator bullishEngulfingIndicator;
public BearishEngulfingIndicator bearishEngulfingIndicator;
public UpperShadowIndicator upperShadowIndicator;
public ThreeWhiteSoldiersIndicator threeWhiteSoldiersIndicator;
public ThreeBlackCrowsIndicator threeBlackCrowsIndicator; 
public RealBodyIndicator realBodyIndicator;
public LowerShadowIndicator lowerShadowIndicator;
public DojiIndicator dojiIndicator;
public BullishHaramiIndicator bullishHaramiIndicator;
public BearishHaramiIndicator bearishHaramiIndicator;


public Tick lastTick;
public TimeSeries series;

public TimeframeSettings(StochasticOscillatorDIndicator sosDStoch, StochasticOscillatorKIndicator sofKStoch,

//prices
ClosePriceIndicator closePrice, MedianPriceIndicator medianPrice, PriceVariationIndicator priceVariation,TypicalPriceIndicator typicalPrice,

//trackers
MACDIndicator macd, EMAIndicator emaMacd, SMAIndicator shortSMA,SMAIndicator longSMA,RSIIndicator RSI,

Tick lastTick, TimeSeries series,
BullishEngulfingIndicator bullishEngulfingIndicator,
BearishEngulfingIndicator bearishEngulfingIndicator,
UpperShadowIndicator upperShadowIndicator,
ThreeWhiteSoldiersIndicator threeWhiteSoldiersIndicator,
ThreeBlackCrowsIndicator threeBlackCrowsIndicator,
RealBodyIndicator realBodyIndicator,
LowerShadowIndicator lowerShadowIndicator,
DojiIndicator dojiIndicator,
BullishHaramiIndicator bullishHaramiIndicator,
BearishHaramiIndicator bearishHaramiIndicator)
{
	this.sosDStoch = sosDStoch;
	this.sofKStoch = sofKStoch;

	//prices
	this.closePrice = closePrice; 
	this.medianPrice = medianPrice; 
	this.priceVariation = priceVariation;
	this.typicalPrice = typicalPrice;

	//trackers
	this.macd = macd;
	this.shortSMA = shortSMA;
	this.longSMA = longSMA;
	this.RSI = RSI;;
	this.lastTick = lastTick;
	this.series = series;
	this.emaMacd = emaMacd;
	
	//pattern indiator
	//various pattern recornizor 
	this.bullishEngulfingIndicator = bullishEngulfingIndicator;
	this.bearishEngulfingIndicator = bearishEngulfingIndicator;
	this.upperShadowIndicator = upperShadowIndicator;
	this.threeWhiteSoldiersIndicator = threeWhiteSoldiersIndicator;
	this.threeBlackCrowsIndicator = threeBlackCrowsIndicator; 
	this.realBodyIndicator = realBodyIndicator;
	this.lowerShadowIndicator = lowerShadowIndicator;
	this.dojiIndicator = dojiIndicator;
	this.bullishHaramiIndicator = bullishHaramiIndicator;
	this.bearishHaramiIndicator = bearishHaramiIndicator;
	
	
}
	
}
