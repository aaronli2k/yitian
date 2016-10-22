package apidemo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;

import com.ib.client.Contract;
import com.ib.controller.Bar;
import com.ib.controller.ApiController.IHistoricalDataHandler;

public class histortyDataHandler implements IHistoricalDataHandler{
	
    private static final DateTimeZone UTC = DateTimeZone.UTC;
        private static final DateTimeZone PARIS = DateTimeZone.forID("Europe/Paris");
        private static final DateTimeZone LONDON = DateTimeZone.forID("Europe/London");
        private static final DateTimeZone TOKYO = DateTimeZone.forID("Asia/Tokyo");
        private static final DateTimeZone NEWYORK = DateTimeZone.forID("America/New_York");
//        private static final Chronology ISO_UTC = ISOChronology.getInstanceUTC();
//        private static final Chronology ISO_PARIS = ISOChronology.getInstance(PARIS);
	
	
	private Contract m_currencyContract;
	private int durationHost = 0;
	private 	ConcurrentHashMap<String, Contract> contractMap;

	public histortyDataHandler(Contract currencyContract, int duration, 	ConcurrentHashMap<String, Contract> contractMapIn) {
		// TODO Auto-generated constructor stub
		m_currencyContract = currencyContract;
		durationHost = duration;
		contractMap = contractMapIn;
	}

	@Override
	public void historicalData(Bar bar, boolean hasGaps) {
		// TODO Auto-generated method stub
		if(bar != null){
			if(durationHost == 5)
				m_currencyContract.historical5MBarMap.put((long)(bar.time()), bar);
			else if(durationHost == 15)
				m_currencyContract.historical15MBarMap.put((long)(bar.time()), bar);
			else if(durationHost == 60)
				m_currencyContract.historicalHourBarMap.put((long)(bar.time()), bar);
			else if(durationHost == 240)
				m_currencyContract.historical4HourBarMap.put((long)(bar.time()), bar);
			else if(durationHost == 1440)
				m_currencyContract.historicalDailyBarMap.put((long)(bar.time()), bar);
		}

		//Update this hence no need to request longer history data.
		m_currencyContract.isHistoryReqFirstTime = false;
		contractMap.put(m_currencyContract.symbol() + m_currencyContract.currency(), m_currencyContract);

	}

	@Override
	public void historicalDataEnd() {
		// TODO Auto-generated method stub
		//			System.out.println(new Date() + " end of bar high: ");
		System.out.println(new Date() + " Historical Data end: "+ m_currencyContract.symbol() + m_currencyContract.currency());

		if(durationHost == 1440){
			m_currencyContract.tickLatch60M.countDown();
			
			printOutBarMap(m_currencyContract.historicalDailyBarMap);
		}
		else if(durationHost == 240){
			m_currencyContract.tickLatch60M.countDown();
			printOutBarMap(m_currencyContract.historical4HourBarMap);
		}
		else if(durationHost == 60){
			m_currencyContract.tickLatch60M.countDown();
			printOutBarMap(m_currencyContract.historicalHourBarMap);
		}
		else if(durationHost == 15){
			m_currencyContract.tickLatch15M.countDown();
			printOutBarMap(m_currencyContract.historical15MBarMap);
		}
		else if(durationHost == 5){
			//				printOutBarMap(m_currencyContract.historical5MBarMap);
			//				printOutBarMap(m_currencyContract.historical15MBarMap);
			//				printOutBarMap(m_currencyContract.historicalHourBarMap);
			//			Calculate 15m and hourly bar chart from 15 minutes bar.
			calculate15MnHourBarFrom5MBarMap();	
			CalDailyChartFromHourlyBar();
			printOutBarMap(m_currencyContract.historicalHourBarMap);
			printOutBarMap(m_currencyContract.historicalDailyBarMap);
			m_currencyContract.tickLatch5M.countDown();
						//	printOutBarMap(m_currencyContract.historical15MBarMap);
			//				printOutBarMap(m_currencyContract.historical4HourBarMap);

		}
	}


	private void CalDailyChartFromHourlyBar() {

		//Daily chart ends at 17:00 New York time.
		
		ConcurrentHashMap<Long, Bar> historicalBarMap = m_currencyContract.historicalHourBarMap;

		TreeSet<Long> keys = new TreeSet<Long>(historicalBarMap.keySet());
		//		TreeSet<Long> treereverse = (TreeSet<Long>) keys.descendingSet();
		double open = 0, high = 0, low = 0, close;
		int hourCount = 0;
		DateTime barTime;

		Bar bar = null, new15MBar = null, hourlyBar;
		Calendar cal = Calendar.getInstance();

		boolean tradeDayEnd = false, timeRetrieved = false;;
		Iterator<Long> iterator = keys.iterator();
		Bar new24HourBar = null;
		
		hourlyBar = historicalBarMap.get(keys.first());
		new24HourBar = hourlyBar;
		Calendar newyorkCalendar = Calendar.getInstance();
		newyorkCalendar.setTimeZone(TimeZone.getTimeZone("America/New_York"));
		if(hourlyBar == null) return ;
		
		DateTime barTimeLocal = new DateTime(hourlyBar.time() * 1000);
		//			LocalDateTime nowUTC = nowLocal.withZone(DateTimeZone.UTC).toLocalDateTime();
		DateTime barTimeNewYork = barTimeLocal.withZone(NEWYORK);
		newyorkCalendar.setTime(barTimeNewYork.toDate());
	
		long timeInMillis = 0;
		timeRetrieved = false;
		
		while(iterator.hasNext()){
		DateTimeZone tz;
		DateTime nowLocal;
		DateTime nowUTC2 = null;
	
		tradeDayEnd = false;
		
		for (hourCount = 0;  hourCount < 24; hourCount++){
			Long key = null;
			if(iterator.hasNext())
				key = iterator.next();
			if(key == null)
				return;
			hourlyBar = historicalBarMap.get(key);

			//How about a daily bar from hourly bar
			//Daily chart time is base on UTC. It is too hard to calculate, better just stick with current time. it is samze as MT4.
			
			tz = DateTimeZone.getDefault();
			nowLocal = new DateTime(hourlyBar.time() * 1000);
			//			LocalDateTime nowUTC = nowLocal.withZone(DateTimeZone.UTC).toLocalDateTime();
			nowUTC2 = nowLocal.withZone(NEWYORK);

		//	Date dLocal = nowLocal.toDate();
			//			Date dUTC = nowUTC.toDate();
			Date dUTC2 = nowUTC2.toDate();
			
	//		hourCount++;
			DateTime tickTime = new DateTime(hourlyBar.time() * 1000);
			DateTime currentBarTime = new DateTime(hourlyBar.time() * 1000);

			cal.setTime(tickTime.toDate());
//			//If bar time is 15 times, then it is a start of new bar. Or it is continuous of previous 15 minutes bar.
//			if((tickTime.getHourOfDay()) % 24 != 0)
//				cal.add(Calendar.HOUR, -1 * tickTime.getHourOfDay() % 24);
//			//				else
//			//					System.out.println("Time to calculate a new 4 hour time bar");
//			tickTime = new DateTime(cal.getTime());
			
			if(!timeRetrieved){
				if((tickTime.getHourOfDay()) % 24 != 0)
				cal.add(Calendar.HOUR, -1 * tickTime.getHourOfDay() % 24);
			//				else
			//					System.out.println("Time to calculate a new 4 hour time bar");
			tickTime = new DateTime(cal.getTime());
				timeInMillis = tickTime.getMillis();
				timeRetrieved = true;
			}
			
			
			//				System.out.println("60 m " + tickTime.toString());

		    SimpleDateFormat TimeOnly_FORMAT = new SimpleDateFormat("HH:mm");

		    DateTime NY17StartNewDay = tickTime.withZone(NEWYORK);
		    barTimeNewYork = (new DateTime(newyorkCalendar.getTime())).withZone(NEWYORK);
		    System.out.println("Day of week is " + nowUTC2.getDayOfWeek());
		    //if new york is Sunday, doesn't close market until new york is monday. If it is friday, doesn't close market at all.
		    
		    DateTime new24HourBarTime = new DateTime(new24HourBar.time() * 1000);
		    boolean newWeek = new24HourBarTime.plus(Period.days(2)).isBefore(currentBarTime);
		    
		    if((nowUTC2.getHourOfDay()) == 15  && nowUTC2.getDayOfWeek() != 7 && nowUTC2.getDayOfWeek() != 5 ||  newWeek)	//It should be closed at 17:00. But don't know why everyone is using 14:00	    
//			if(dUTC2.compareTo(NY17StartNewDay) >= 0)
			{
				tradeDayEnd = true;
				newyorkCalendar.add(Calendar.HOUR, 24);
//				hourCount = 0;
				tickTime = new DateTime(timeInMillis);
				cal.setTime(currentBarTime.toDate());
				//If bar time is 15 times, then it is a start of new bar. Or it is continuous of previous 15 minutes bar.
				if((currentBarTime.getHourOfDay()) % 24 != 0)
					cal.add(Calendar.HOUR,  -1 * currentBarTime.getHourOfDay() % 24);
				
//				//				else
//				//					System.out.println("Time to calculate a new 4 hour time bar");
				tickTime = new DateTime(cal.getTime());
//				tickTime = new DateTime(cal.getTime());
				timeInMillis = tickTime.getMillis();
		//		break;
			}
			

			new24HourBar = m_currencyContract.historicalDailyBarMap.get(timeInMillis/1000);
			
			open = hourlyBar.open();
			high = hourlyBar.high();
			low = hourlyBar.low();
			close = hourlyBar.close();

			if(new24HourBar == null){
				new24HourBar = new Bar(timeInMillis/1000, high, low, open, close, 0, 0, 0);		
				
			}else
			{

				if(high < new24HourBar.high())
					high = new24HourBar.high(); 
				if(low > new24HourBar.low())
					low = new24HourBar.low(); 
				open = new24HourBar.open();
				close = hourlyBar.close();
				new24HourBar = new Bar(timeInMillis/1000, high, low, open, close, 0, 0, 0);
			}

			
			if(new24HourBar != null){
				m_currencyContract.historicalDailyBarMap.put(timeInMillis / 1000, new24HourBar);				
			}


		}
	
		}
	}

	void printOutBarMap(ConcurrentHashMap<Long, Bar> historicalBarMap){

		TreeSet<Long> keys = new TreeSet<Long>(historicalBarMap.keySet());
		//		TreeSet<Long> treereverse = (TreeSet<Long>) keys.descendingSet();



		Bar bar = null, new15MBar = null, newHourlyBar;
		Calendar cal = Calendar.getInstance();

		for (Long key : keys){
			bar = historicalBarMap.get(key);
			if(bar != null)
				System.out.println(m_currencyContract.symbol() + m_currencyContract.currency() + " time: " + bar.formattedTime()  + " bar count: " + bar.count() + " bar open: " + bar.open() +  " bar high: " + bar.high() + " bar low: " + bar.low() + " bar close: " + bar.close());


		}
	}

	void	calculate15MnHourBarFrom5MBarMap(){

		TreeSet<Long> keys = new TreeSet<Long>(m_currencyContract.historical5MBarMap.keySet());
		//		TreeSet<Long> treereverse = (TreeSet<Long>) keys.descendingSet();

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");


		Bar bar = null, new15MBar = null, newHourlyBar;
		Calendar cal = Calendar.getInstance();

		for (Long key : keys){
			bar = m_currencyContract.historical5MBarMap.get(key);
			//			m_currencyContract.historical5MBarMap.remove(key);

			if(bar == null)
				continue;		

			DateTime tickTime = new DateTime(bar.time() * 1000);

			//				try {
			//					if(tickTime.isAfter(new DateTime(formatter.parse("2016-10-18 7:35")))){
			//						System.out.println("15 m " + tickTime.toString());
			//
			//					}
			//				} catch (ParseException e) {
			//					// TODO Auto-generated catch block
			//					e.printStackTrace();
			//				}

			cal.setTime(tickTime.toDate());
			//If bar time is 15 times, then it is a start of new bar. Or it is continuous of previous 15 minutes bar.
			if(tickTime.getMinuteOfHour() % 15 != 0)
				cal.add(Calendar.MINUTE, -1 * (tickTime.getMinuteOfHour() % 15));
			tickTime = new DateTime(cal.getTime());
			//				System.out.println("15 m " + tickTime.toString());

			new15MBar = m_currencyContract.historical15MBarMap.get((long)(cal.getTimeInMillis()/1000));			
			new15MBar = calculateNewBarFrom5MBar(bar, new15MBar, 15, new DateTime(cal.getTimeInMillis()));	

			//				try {
			//					if(tickTime.isAfter(new DateTime(formatter.parse("2016-10-18 7:35")))){
			//						
			//
			//						if(new15MBar != null)						
			//
			//						System.out.println("15 m new15MBar " + new15MBar.formattedTime());
			//
			//
			//					}
			//				} catch (ParseException e) {
			//					// TODO Auto-generated catch block
			//					e.printStackTrace();
			//				}
			if(new15MBar != null)
				new15MBar = m_currencyContract.historical15MBarMap.put((long)(new15MBar.time()), new15MBar);


			tickTime = new DateTime(bar.time() * 1000);
			cal.setTime(tickTime.toDate());
			//If bar time is 15 times, then it is a start of new bar. Or it is continuous of previous 15 minutes bar.
			if(tickTime.getMinuteOfHour() != 0)
				cal.add(Calendar.MINUTE, -1 * (tickTime.getMinuteOfHour() % 60));
			tickTime = new DateTime(cal.getTime());
			//				System.out.println("60 m " + tickTime.toString());
			newHourlyBar = m_currencyContract.historicalHourBarMap.get((long)cal.getTimeInMillis()/1000);
			newHourlyBar = calculateNewBarFrom5MBar(bar, newHourlyBar, 60, new DateTime(cal.getTimeInMillis()));



			if(newHourlyBar != null){
				m_currencyContract.historicalHourBarMap.put((long)(newHourlyBar.time()), newHourlyBar);				
			}


			//Calculate 4 hour bar from one hour bar

			DateTimeZone tz = DateTimeZone.getDefault();
			DateTime nowLocal = new DateTime(newHourlyBar.time() * 1000);
			//			LocalDateTime nowUTC = nowLocal.withZone(DateTimeZone.UTC).toLocalDateTime();
			DateTime nowUTC2 = nowLocal.withZone(DateTimeZone.UTC);

			Date dLocal = nowLocal.toDate();
			//			Date dUTC = nowUTC.toDate();
			Date dUTC2 = nowUTC2.toDate();


			tickTime = new DateTime(newHourlyBar.time() * 1000);
			cal.setTime(tickTime.toDate());
			//If bar time is 15 times, then it is a start of new bar. Or it is continuous of previous 15 minutes bar.
			if((nowUTC2.getHourOfDay()) % 4 != 0)
				cal.add(Calendar.HOUR, -1 * nowUTC2.getHourOfDay() % 4);
			//				else
			//					System.out.println("Time to calculate a new 4 hour time bar");
			tickTime = new DateTime(cal.getTime());
			//				System.out.println("60 m " + tickTime.toString());
			Bar new4HourBar = m_currencyContract.historical4HourBarMap.get((long)cal.getTimeInMillis()/1000);
			new4HourBar = calculateNewBarFromHourlyBar(newHourlyBar, new4HourBar, 4, new DateTime(cal.getTimeInMillis()), nowUTC2);
			if(new4HourBar != null){
				m_currencyContract.historical4HourBarMap.put((long)(new4HourBar.time()), new4HourBar);				
			}

//
//			//How about a daily bar from hourly bar
//			//Daily chart time is base on UTC. It is too hard to calculate, better just stick with current time. it is samze as MT4.
//			
//			tz = DateTimeZone.getDefault();
//			nowLocal = new DateTime(newHourlyBar.time() * 1000);
//			//			LocalDateTime nowUTC = nowLocal.withZone(DateTimeZone.UTC).toLocalDateTime();
//			nowUTC2 = nowLocal.withZone(NEWYORK);
//
//		//	Date dLocal = nowLocal.toDate();
//			//			Date dUTC = nowUTC.toDate();
//			dUTC2 = nowUTC2.toDate();
//			
//			
//			tickTime = nowUTC2;// new DateTime(newHourlyBar.time() * 1000);
//			cal.setTime(tickTime.toDate());
//			//If bar time is 15 times, then it is a start of new bar. Or it is continuous of previous 15 minutes bar.
//			if((tickTime.getHourOfDay()) % 24 != 0)
//				cal.add(Calendar.HOUR, -1 * tickTime.getHourOfDay() % 24);
//			//				else
//			//					System.out.println("Time to calculate a new 4 hour time bar");
//			tickTime = new DateTime(cal.getTime());
//			long timeInMillis = tickTime.getMillis();
//			//				System.out.println("60 m " + tickTime.toString());
//			Bar new24HourBar = m_currencyContract.historicalDailyBarMap.get(timeInMillis/1000);
//			new24HourBar = calculateNewBarFromHourlyBar(newHourlyBar, new24HourBar, 24, timeInMillis);
//			if(new24HourBar != null){
//				m_currencyContract.historicalDailyBarMap.put(timeInMillis / 1000, new24HourBar);				
//			}

			
			
			
		}
	}

	private Bar calculateNewBarFromHourlyBar(Bar hourlyBar, Bar OldBarToUpdate, int i, long timeInMillis) {


		Bar newBar = null;
		Calendar cal = Calendar.getInstance();


		if(hourlyBar == null)
			return hourlyBar;
		double open = hourlyBar.open();
		double high = hourlyBar.high();
		double low = hourlyBar.low();
		double close = hourlyBar.close();
		Long volume = hourlyBar.volume();

		if(OldBarToUpdate == null){
			newBar = new Bar(timeInMillis/1000, high, low, open, close, 0, volume, 0);		
			return newBar;
		}

		//Update previous hour's close and compare its high and low.
		//	if((nowUTC2.getHourOfDay()) % duation  == 0 && nowUTC2.getMinuteOfHour() == 0){
		////		cal.setTime(tickTime.toDate());
		////		cal.add(Calendar.MINUTE, -1 * duation);
		//
		//		newBar = OldBarToUpdate;
		//	}else
		{

			if(high < OldBarToUpdate.high())
				high = OldBarToUpdate.high(); 
			if(low > OldBarToUpdate.low())
				low = OldBarToUpdate.low(); 
			open = OldBarToUpdate.open();
			close = hourlyBar.close();
			newBar = new Bar(timeInMillis/1000, high, low, open, close, 0, 0, 0);
		}


		return newBar;





	}

	private		Bar calculateNewBarFromHourlyBar(Bar hourlyBar, Bar OldBarToUpdate, int duation, DateTime dateTime, DateTime nowUTC2){


		Bar newBar = null;
		Calendar cal = Calendar.getInstance();


		if(hourlyBar == null)
			return hourlyBar;
		double open = hourlyBar.open();
		double high = hourlyBar.high();
		double low = hourlyBar.low();
		double close = hourlyBar.close();
		Long volume = hourlyBar.volume();

		if(OldBarToUpdate == null){
			newBar = new Bar(dateTime.withZone(DateTimeZone.UTC).getMillis()/1000, high, low, open, close, 0, volume, 0);		
			return newBar;
		}

		//Update previous hour's close and compare its high and low.
		//	if((nowUTC2.getHourOfDay()) % duation  == 0 && nowUTC2.getMinuteOfHour() == 0){
		////		cal.setTime(tickTime.toDate());
		////		cal.add(Calendar.MINUTE, -1 * duation);
		//
		//		newBar = OldBarToUpdate;
		//	}else
		{

			if(high < OldBarToUpdate.high())
				high = OldBarToUpdate.high(); 
			if(low > OldBarToUpdate.low())
				low = OldBarToUpdate.low(); 
			open = OldBarToUpdate.open();
			close = hourlyBar.close();
			newBar = new Bar(dateTime.withZone(DateTimeZone.UTC).getMillis()/1000, high, low, open, close, 0, 0, 0);
		}


		return newBar;





	}

	
	
	


	private		Bar calculateNewBarFrom5MBar(Bar fiveMBar, Bar OldBarToUpdate, int duation, DateTime dateTime){


		Bar newBar = null;
		Calendar cal = Calendar.getInstance();


		if(fiveMBar == null)
			return fiveMBar;
		double open = fiveMBar.open();
		double high = fiveMBar.high();
		double low = fiveMBar.low();
		double close = fiveMBar.close();
		Long volume = fiveMBar.volume();
		DateTime tickTime = new DateTime(fiveMBar.time() * 1000);
		cal.setTime(tickTime.toDate());
		cal.add(Calendar.MINUTE, -1 * (tickTime.getMinuteOfHour() % duation) );
		if(OldBarToUpdate == null){
			if(tickTime.getMinuteOfHour() % duation == 0){
				return newBar = fiveMBar;
			}else
				newBar = new Bar(dateTime.getMillis() / 1000, high, low, open, close, 0, volume, 0);		
			return newBar;
		}

		//Update previous hour's close and compare its high and low.
		if(tickTime.getMinuteOfHour() % duation == 0){
			//				cal.setTime(tickTime.toDate());
			//				cal.add(Calendar.MINUTE, -1 * duation);


			if(high < OldBarToUpdate.high())
				high = OldBarToUpdate.high(); 
			if(low > OldBarToUpdate.low())
				low = OldBarToUpdate.low(); 
			open = OldBarToUpdate.open();
			close = fiveMBar.close();
			newBar = OldBarToUpdate;
		}else{

			if(high < OldBarToUpdate.high())
				high = OldBarToUpdate.high(); 
			if(low > OldBarToUpdate.low())
				low = OldBarToUpdate.low(); 
			open = OldBarToUpdate.open();
			close = fiveMBar.close();
			newBar = new Bar(dateTime.getMillis() / 1000, high, low, open, close, 0, 0, 0);
		}


		return newBar;



	}



};



