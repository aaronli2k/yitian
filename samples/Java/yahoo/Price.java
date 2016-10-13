package yahoo;
import java.util.Date;

public class Price {
    private final Date date;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final double volume;

    public Date getDate() {
        return date;
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

    public double getVolume() {
        return volume;
    }

    public Price(Date date, double open, double high, double low, double close,
            double volume) {
        super();
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }
}