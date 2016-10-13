package yahoo;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Stack;

public class RSI {
    private int periodLength;
    private final Stack<Averages> avgList;
    private final ArrayList<Price> prices;

    public RSI(int periodLength, String symbol) throws ParseException, IOException {
        super();
        this.periodLength = periodLength;
        avgList = new Stack<Averages>();
        prices = YahooFeederUtil.getPrices(symbol);
    }

    public double calculate() {
        double value = 0;
        int pricesSize = prices.size();
        int lastPrice = pricesSize - 1;
        int firstPrice = lastPrice - periodLength + 1;

        double gains = 0;
        double losses = 0;
        double avgUp = 0;
        double avgDown = 0;

        double delta = prices.get(lastPrice).getClose()
                - prices.get(lastPrice - 1).getClose();
        gains = Math.max(0, delta);
        losses = Math.max(0, -delta);

        if (avgList.isEmpty()) {
            for (int bar = firstPrice + 1; bar <= lastPrice; bar++) {
                double change = prices.get(bar).getClose()
                        - prices.get(bar - 1).getClose();
                gains += Math.max(0, change);
                losses += Math.max(0, -change);
            }
            avgUp = gains / periodLength;
            avgDown = losses / periodLength;
            avgList.push(new Averages(avgUp, avgDown));

        } else {

            Averages avg = avgList.pop();
            avgUp = avg.getAvgUp();
            avgDown = avg.getAvgDown();
            avgUp = ((avgUp * (periodLength - 1)) + gains) / (periodLength);
            avgDown = ((avgDown * (periodLength - 1)) + losses)
                    / (periodLength);
            avgList.add(new Averages(avgUp, avgDown));
        }
        value = 100 - (100 / (1 + (avgUp / avgDown)));

        return Math.round(value);
    }

    private class Averages {

        private final double avgUp;
        private final double avgDown;

        public Averages(double up, double down) {
            this.avgDown = down;
            this.avgUp = up;
        }

        public double getAvgUp() {
            return avgUp;
        }

        public double getAvgDown() {
            return avgDown;
        }
    }

    public int getPeriodLength() {
        return periodLength;
    }
}

