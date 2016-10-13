package yahoo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class YahooFeederUtil {
public static ArrayList getPrices(String symbol)
       throws ParseException, IOException {
   Calendar oneYearAgo = Calendar.getInstance();
   oneYearAgo.add(Calendar.DATE, -366);

   Date now = new Date();
   URL yahoo = new URL(getFeederURL(symbol, new Date(oneYearAgo
           .getTimeInMillis()), now));
   URLConnection yc = yahoo.openConnection();
   BufferedReader in = new BufferedReader(new InputStreamReader(yc
           .getInputStream()));
   String line;

   ArrayList prices = new ArrayList();
   while ((line = in.readLine()) != null) {
       if (!Character.isDigit(line.charAt(0))) {
           continue;
       }
       String[] elements = line.split(",");
       Date date = (Date) new SimpleDateFormat("yyyy-MM-dd")
               .parse(elements[0]);

       prices.add(new Price(date, new Double(elements[1]).doubleValue(),
               new Double(elements[2]).doubleValue(), new Double(
                       elements[3]).doubleValue(), new Double(elements[4])
                       .doubleValue(), new Double(elements[5])
                       .doubleValue()));
   }
   in.close();
   return prices;
}

private static String getFeederURL(String symbol, Date from, Date to) {

   Calendar fromDate = Calendar.getInstance();
   fromDate.setTime(from);

   Calendar toDate = Calendar.getInstance();
   toDate.setTime(to);
   return "http://ichart.finance.yahoo.com/table.csv?s=" + symbol + "&a="
           + String.valueOf(fromDate.get(Calendar.MONTH)) + "&b="
           + String.valueOf(fromDate.get(Calendar.DAY_OF_MONTH)) + "&c="
           + String.valueOf(fromDate.get(Calendar.YEAR)) + "&d="
           + String.valueOf(toDate.get(Calendar.MONTH)) + "&e="
           + String.valueOf(toDate.get(Calendar.DAY_OF_MONTH)) + "&f="
           + String.valueOf(toDate.get(Calendar.YEAR)) + "&g=d"
           + "&ignore=.csv";

}
}
