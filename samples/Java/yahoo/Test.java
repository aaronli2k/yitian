package yahoo;

import java.io.IOException;
import java.text.ParseException;

//import junit.framework.TestCase;

public class Test {
    private static String symbolForTest = "GOOG";
    private static int nineDaysPeriod = 9;
    private static int fourteenDaysPeriod = 14;
    private static int twentyFiveDaysPeriod = 25;

    public void testGetPrices() throws Exception {
        YahooFeederUtil.getPrices(symbolForTest).size() ;
    }

    public static void main(String[] args) throws Exception {
        try {
            RSI rsi = new RSI(nineDaysPeriod, symbolForTest);
            System.out.println("RSI for a " + rsi.getPeriodLength()
                    + " days period is: " + rsi.calculate());

            rsi = new RSI(fourteenDaysPeriod, symbolForTest);
            System.out.println("RSI for a " + rsi.getPeriodLength()
                    + " days period is: " + rsi.calculate());

            rsi = new RSI(twentyFiveDaysPeriod, symbolForTest);
            System.out.println("RSI for a " + rsi.getPeriodLength()
                    + " days period is: " + rsi.calculate());
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
