/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package apidemo;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.Execution;
import com.ib.client.ExecutionFilter;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.TickType;
import com.ib.client.Types.Action;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.NewsType;
import com.ib.client.Types.TimeInForce;
import com.ib.client.Types.WhatToShow;
import com.ib.controller.ApiConnection.ILogger;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IBulletinHandler;
import com.ib.controller.ApiController.IConnectionHandler;
import com.ib.controller.ApiController.IHistoricalDataHandler;
import com.ib.controller.ApiController.ILiveOrderHandler;
import com.ib.controller.ApiController.IOrderHandler;
import com.ib.controller.ApiController.IPositionHandler;
import com.ib.controller.ApiController.IRealTimeBarHandler;
import com.ib.controller.ApiController.ITimeHandler;
import com.ib.controller.ApiController.ITradeReportHandler;
import com.ib.controller.ApiController.TopMktDataAdapter;
import com.ib.controller.Bar;
import com.ib.controller.Formats;

import apidemo.util.HtmlButton;
import apidemo.util.IConnectionConfiguration;
import apidemo.util.IConnectionConfiguration.DefaultConnectionConfiguration;
import apidemo.util.NewLookAndFeel;
import apidemo.util.NewTabbedPanel;
import apidemo.util.VerticalPanel;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;
import samples.testbed.orders.OrderSamples;

public class ApiDemo implements IConnectionHandler, Runnable {
	static { NewLookAndFeel.register(); }
	public static ApiDemo INSTANCE;

	//	private volatile static Thread timer;       // The thread that displays clock


	private final IConnectionConfiguration m_connectionConfiguration;
	private final JTextArea m_inLog = new JTextArea();
	private final JTextArea m_outLog = new JTextArea();
	private final Logger m_inLogger = new Logger( m_inLog);
	private final Logger m_outLogger = new Logger( m_outLog);
	private ApiController m_controller;
	private final ArrayList<String> m_acctList = new ArrayList<>();
	private final JFrame m_frame = new JFrame();
	private final NewTabbedPanel m_tabbedPanel = new NewTabbedPanel(true);
	private final ConnectionPanel m_connectionPanel;
	private final MarketDataPanel m_mktDataPanel = new MarketDataPanel();
	private final ContractInfoPanel m_contractInfoPanel = new ContractInfoPanel();
	private final TradingPanel m_tradingPanel = new TradingPanel();
	private final AccountInfoPanel m_acctInfoPanel = new AccountInfoPanel();
	private final AccountPositionsMultiPanel m_acctPosMultiPanel = new AccountPositionsMultiPanel();
	private final OptionsPanel m_optionsPanel = new OptionsPanel();
	private final AdvisorPanel m_advisorPanel = new AdvisorPanel();
	private final ComboPanel m_comboPanel = new ComboPanel();
	private final StratPanel m_stratPanel = new StratPanel();
	private final JTextArea m_msg = new JTextArea();

	//private final static PricesPanel m_pricesPanel =new PricesPanel();

	//Use below to store all contract <currencyPair, contract>
	ConcurrentHashMap<String, Contract> contractMap = new ConcurrentHashMap<String, Contract>();
	//Use below to store all contract <currencyPair, contract>
	ConcurrentHashMap<String, TechinicalAnalyzerTrader> contractTechAnalyzerMap = new ConcurrentHashMap<String, TechinicalAnalyzerTrader>();

	//Use below to store all submitted order <orderId, Order>
	ConcurrentHashMap<Integer, Order> submittedOrderHashMap = new ConcurrentHashMap<Integer, Order>();

	public final Contract m_contract_NZDUSD = new Contract("NZD", "CASH", "IDEALPRO", "USD", 2.0, 0.3);
	public final Contract m_contract_AUDUSD = new Contract("AUD", "CASH", "IDEALPRO", "USD", 2.0, 0.3);
	public final Contract m_contract_GBPUSD = new Contract("GBP", "CASH", "IDEALPRO", "USD", 3.0, 0.3);
	public final Contract m_contract_EURUSD = new Contract("EUR", "CASH", "IDEALPRO", "USD", 3.0, 0.3);	
	public final Contract m_contract_EURCHF = new Contract("EUR", "CASH", "IDEALPRO", "CHF", 2.0, 0.3);
	public final Contract m_contract_EURGBP = new Contract("EUR", "CASH", "IDEALPRO", "GBP", 2.0, 0.3);
	public final Contract m_contract_EURJPY = new Contract("EUR", "CASH", "IDEALPRO", "JPY", 300.0, 30.0);
	public final Contract m_contract_USDCAD = new Contract("USD", "CASH", "IDEALPRO", "CAD", 3.0, 0.3);
	public final Contract m_contract_USDCHF = new Contract("USD", "CASH", "IDEALPRO", "CHF", 3.0, 0.3);
	public final Contract m_contract_USDJPY = new Contract("USD", "CASH", "IDEALPRO", "JPY", 300.0, 30.0);
	public final Contract m_contract_CADJPY = new Contract("CAD", "CASH", "IDEALPRO", "JPY", 200.0, 30.0);
	public final Contract m_contract_CADCHF = new Contract("CAD", "CASH", "IDEALPRO", "CHF", 3.0, 0.3);
	public final Contract m_contract_CHFJPY = new Contract("CHF", "CASH", "IDEALPRO", "JPY", 300.0, 30.0);
	public final Contract m_contract_EURCAD = new Contract("EUR", "CASH", "IDEALPRO", "CAD", 3.0, 0.3);
	public final Contract m_contract_EURSGD = new Contract("EUR", "CASH", "IDEALPRO", "SGD", 3.0, 0.3);
	public final Contract m_contract_GBPCAD = new Contract("GBP", "CASH", "IDEALPRO", "CAD", 3.0, 0.3);
	public final Contract m_contract_GBPJPY = new Contract("GBP", "CASH", "IDEALPRO", "JPY", 300.0, 30.0);
	//	public final Contract m_contract_GBPSGD = new Contract("GBP", "CASH", "IDEALPRO", "SGD", 3.0, 0.3);
	public final Contract m_contract_USDSGD = new Contract("USD", "CASH", "IDEALPRO", "SGD", 3.0, 0.3);
	public final Contract m_contract_AUDCAD = new Contract("AUD", "CASH", "IDEALPRO", "CAD", 3.0, 0.3);	
	public final Contract m_contract_EURAUD = new Contract("EUR", "CASH", "IDEALPRO", "AUD", 3.0, 0.3);
	//	public final Contract m_contract_CHFAUD = new Contract("CHF", "CASH", "IDEALPRO", "AUD", 3.0, 0.3);
	public final Contract m_contract_AUDJPY = new Contract("AUD", "CASH", "IDEALPRO", "JPY", 300.0, 20.0);
	public final Contract m_contract_AUDSGD = new Contract("AUD", "CASH", "IDEALPRO", "SGD", 3.0, 0.3);
	public final Contract m_contract_EURNZD = new Contract("EUR", "CASH", "IDEALPRO", "NZD", 3.0, 0.3);
	public final Contract m_contract_AUDNZD = new Contract("AUD", "CASH", "IDEALPRO", "NZD", 3.0, 0.3);
	public final Contract m_contract_GBPAUD = new Contract("GBP", "CASH", "IDEALPRO", "AUD", 3.0, 0.3);
	public final Contract m_contract_GBPNZD = new Contract("GBP", "CASH", "IDEALPRO", "NZD", 3.0, 0.3);
	public final Contract m_contract_NZDJPY = new Contract("NZD", "CASH", "IDEALPRO", "JPY", 300.0, 30.0);
	public final Contract m_contract_AUDCNH = new Contract("AUD", "CASH", "IDEALPRO", "CNH", 20.0, 2.0);
	public final Contract m_contract_CNHJPY = new Contract("CNH", "CASH", "IDEALPRO", "JPY", 50.0, 3.0);
	public final Contract m_contract_USDCNH = new Contract("USD", "CASH", "IDEALPRO", "CNH", 30.0, 3.0);
	public final Contract m_contract_EURCNH = new Contract("EUR", "CASH", "IDEALPRO", "CNH", 30.0, 3.0);
	public final Contract m_contract_GBPCNH = new Contract("GBP", "CASH", "IDEALPRO", "CNH", 30.0, 3.0);



	ForexListner m_stockListener_NZDUSD = new ForexListner(m_contract_NZDUSD);		
	ForexListner m_stockListener_AUDUSD = new ForexListner(m_contract_AUDUSD);	
	ForexListner m_stockListener_GBPUSD = new ForexListner(m_contract_GBPUSD);	
	ForexListner m_stockListener_EURUSD = new ForexListner(m_contract_EURUSD);		
	ForexListner m_stockListener_EURCHF = new ForexListner(m_contract_EURCHF);	
	ForexListner m_stockListener_EURGBP = new ForexListner(m_contract_EURGBP);	
	ForexListner m_stockListener_EURJPY = new ForexListner(m_contract_EURJPY);		
	ForexListner m_stockListener_USDCAD = new ForexListner(m_contract_USDCAD);	
	ForexListner m_stockListener_USDCHF = new ForexListner(m_contract_USDCHF);	
	ForexListner m_stockListener_USDJPY = new ForexListner(m_contract_USDJPY);   				
	ForexListner m_stockListener_CADJPY = new ForexListner(m_contract_CADJPY);	
	ForexListner m_stockListener_CADCHF = new ForexListner(m_contract_CADCHF);	
	ForexListner m_stockListener_CHFJPY = new ForexListner(m_contract_CHFJPY);		
	ForexListner m_stockListener_EURCAD = new ForexListner(m_contract_EURCAD);	
	ForexListner m_stockListener_EURSGD = new ForexListner(m_contract_EURSGD);	
	ForexListner m_stockListener_GBPCAD = new ForexListner(m_contract_GBPCAD);		
	ForexListner m_stockListener_GBPJPY = new ForexListner(m_contract_GBPJPY);	
	//	ForexListner m_stockListener_GBPSGD = new ForexListner(m_contract_GBPSGD);	
	ForexListner m_stockListener_USDSGD = new ForexListner(m_contract_USDSGD);		
	ForexListner m_stockListener_AUDCAD = new ForexListner(m_contract_AUDCAD);			
	ForexListner m_stockListener_EURAUD = new ForexListner(m_contract_EURAUD);	
	//	ForexListner m_stockListener_CHFAUD = new ForexListner(m_contract_CHFAUD);		
	ForexListner m_stockListener_AUDJPY = new ForexListner(m_contract_AUDJPY);	
	ForexListner m_stockListener_AUDSGD = new ForexListner(m_contract_AUDSGD);	
	ForexListner m_stockListener_EURNZD = new ForexListner(m_contract_EURNZD);		
	ForexListner m_stockListener_AUDNZD = new ForexListner(m_contract_AUDNZD);	
	ForexListner m_stockListener_GBPAUD = new ForexListner(m_contract_GBPAUD);	
	ForexListner m_stockListener_GBPNZD = new ForexListner(m_contract_GBPNZD);		
	ForexListner m_stockListener_NZDJPY = new ForexListner(m_contract_NZDJPY);	
	ForexListner m_stockListener_AUDCNH = new ForexListner(m_contract_AUDCNH);		
	ForexListner m_stockListener_CNHJPY = new ForexListner(m_contract_CNHJPY);	
	ForexListner m_stockListener_USDCNH = new ForexListner(m_contract_USDCNH);	
	ForexListner m_stockListener_EURCNH = new ForexListner(m_contract_EURCNH);	
	ForexListner m_stockListener_GBPCNH = new ForexListner(m_contract_GBPCNH);	




	ConcurrentHashMap<String, ForexListner> forexListenerHashMap = new ConcurrentHashMap<String, ForexListner>();
	ConcurrentHashMap<String, ForexListner> currentListeningMap = new ConcurrentHashMap<String, ForexListner>();
	ArrayList<String> currentMarketDataList = new ArrayList<String>();
	ConcurrentHashMap<String, ForexListner> historyListeningMap = new ConcurrentHashMap<String, ForexListner>();

	long tickCounter = 100;
	Calendar serverTimeCalendar = Calendar.getInstance();


	//	Timer m_timer = new Timer( 5000, this); //1 seconds timer

	ReadExcel excelInput = new ReadExcel();
	WriteExcel excelOutput = new WriteExcel();
	String inputFileName = "Forex.xls";

	ArrayList<String> historicalDataReq = new ArrayList<String>();

	ConcurrentHashMap<Long, forex> orderHashMap = new ConcurrentHashMap<Long, forex>();



	//All live order in system.
	ConcurrentHashMap<Integer, Order> liveOrderMap = new ConcurrentHashMap<Integer, Order>();

	//Execution report map
	ConcurrentHashMap<Integer, forex> executedOrderMap = new ConcurrentHashMap<Integer, forex>();
	ConcurrentHashMap<Integer, forex> liveForexOrderMap = new ConcurrentHashMap<Integer, forex>();
	Double totalCommissionPaid = 0.0, bufferCommissionPaid = 0.0, totalProfitNLoss = 0.0, bufferProfitNLoss = 0.0;
	String baseCurrency;

	int fileReadingCounter = 301;

	private int nextOrderId = 0, currentMaxOrderId = 0;

	class ForexListner extends TopMktDataAdapter implements IRealTimeBarHandler{
		Contract m_contract_listener;
		int counter = 0;
		ForexListner(Contract p_contract){
			m_contract_listener = p_contract;
		}
		@Override public void tickPrice(TickType tickType, double price, int canAutoExecute) {

			counter++;
			//			 	
			if (tickType == TickType.LAST) {
				m_contract_listener.setLastPrice(price);
				System.out.println("Last price: " + price);
			}else if (tickType == TickType.BID) {
				tickCounter++;

				if(price > m_contract_listener.getMinPrice() && price < m_contract_listener.getMaxPrice() )
					m_contract_listener.add(price);	
				if(counter % 1000 == 0){	
					System.out.println(m_contract_listener.symbol() + "." + m_contract_listener.currency() + " BID: " + price + " SMA: " + m_contract_listener.ma());
					show(m_contract_listener.symbol() + "." + m_contract_listener.currency() + " BID: " + price + " SMA: " + m_contract_listener.ma());                    
				}
				//     	 m_contract_listener.add(price);

				if((price > m_contract_listener.ma() * 1.10) || price < m_contract_listener.ma() * 0.9){ 
					if(counter == 0)	
						System.out.println("ILLEGAL " + m_contract_listener.symbol() + m_contract_listener.currency() + " MID: " + price + " MA: " + m_contract_listener.ma()  + " CURRENT BID: " + m_contract_listener.getBidPrice() + " Close: " + m_contract_listener.getClosePrice());

					return;
				}

				//		 System.out.println("Moving average: " + m_contract_listener.symbol() + m_contract_listener.currency() + " MID: " + contractMap.get(m_contract_listener.symbol() + m_contract_listener.currency()).getClosePrice());


				// 			 m_contract_listener.setClosePrice(price);



				m_contract_listener.setBidPrice(price);
				//           System.out.println("BID price: " + price);
				contractMap.put(m_contract_listener.symbol() + m_contract_listener.currency(), m_contract_listener);

				//          System.out.println(m_contract_listener.symbol() + m_contract_listener.currency() + " Ask: " + contractMap.get(m_contract_listener.symbol() + m_contract_listener.currency()).getAskPrice() +  " Bid price: " + contractMap.get(m_contract_listener.symbol() + m_contract_listener.currency()).getBidPrice());

			}else if (tickType == TickType.ASK) {
				tickCounter++;
				if(price > m_contract_listener.getMinPrice() && price < m_contract_listener.getMaxPrice() )
					m_contract_listener.add(price);
				if(counter % 1000 == 0){	
					System.out.println(m_contract_listener.symbol() + "." + m_contract_listener.currency() + " ASK: " + price  + " SMA: " + m_contract_listener.ma());
					show(m_contract_listener.symbol() + "." + m_contract_listener.currency() + " ASK: " + price  + " SMA: " + m_contract_listener.ma());       
				}
				//     	m_contract_listener.add(price);

				if((price > m_contract_listener.ma() * 1.10) || price < m_contract_listener.ma() * 0.9){ 
					//    				 if(counter % 1000 == 0)	
					System.out.println("ILLEGAL " + m_contract_listener.symbol() + m_contract_listener.currency() + " MID: " + price + " MA: " + m_contract_listener.ma() + " CURRENT ASK: " + m_contract_listener.getAskPrice() + " Close: " + m_contract_listener.getClosePrice());

					return;
				}

				//		 System.out.println("Moving average: " + m_contract_listener.symbol() + m_contract_listener.currency() + " MID: " + contractMap.get(m_contract_listener.symbol() + m_contract_listener.currency()).getClosePrice());


				//		 m_contract_listener.setClosePrice(price);

				m_contract_listener.setAskPrice(price);
				//                  System.out.println("ASK price: " + price);
				contractMap.put(m_contract_listener.symbol() + m_contract_listener.currency(), m_contract_listener);

				//            System.out.println(m_contract_listener.symbol() + m_contract_listener.currency() + " Ask: " + contractMap.get(m_contract_listener.symbol() + m_contract_listener.currency()).getAskPrice() +  " Bid price: " + contractMap.get(m_contract_listener.symbol() + m_contract_listener.currency()).getBidPrice());

			}else if (tickType == TickType.CLOSE) {



				if(( m_contract_listener.ma() != 0) &&( (price > m_contract_listener.ma() * 1.10) || price < m_contract_listener.ma() * 0.9)){ 
					System.out.println("ILLEGAL " + m_contract_listener.symbol() + m_contract_listener.currency() + " MID: " + price + " MA: " + m_contract_listener.ma() + " CURRENT ASK: " + m_contract_listener.getAskPrice() + " Close: " + m_contract_listener.getClosePrice());

					return;
				}
				m_contract_listener.setClosePrice(price);
				if(counter % 1000 == 0)
					System.out.println(m_contract_listener.symbol() + m_contract_listener.currency() + " Close price: " + price + " MA: " + " MA: " + m_contract_listener.ma() );
			}else if (tickType == TickType.OPEN) {
				m_contract_listener.setOpenPrice(price);
				if(counter % 1000 == 0)
					System.out.println("Open price: " + price);
			}


		}

		@Override
		public void realtimeBar(Bar bar) {
			// TODO Auto-generated method stub


			if((bar.close() > m_contract_listener.ma() * 1.10) || (bar.close() < m_contract_listener.ma() * 0.9)){ 
				System.out.println("ILLEGAL " + m_contract_listener.symbol() + m_contract_listener.currency() + " MID: " + bar.close() + " MA: " + m_contract_listener.ma());

				return;}



			m_contract_listener.setClosePrice(bar.close());


			contractMap.put(m_contract_listener.symbol() + m_contract_listener.currency(), m_contract_listener);


		}



	}




	// getter methods
	public ArrayList<String> accountList() 	{ return m_acctList; }
	public JFrame frame() 					{ return m_frame; }
	public ILogger getInLogger()            { return m_inLogger; }
	public ILogger getOutLogger()           { return m_outLogger; }

	public static void main(String[] args) {
		start( new ApiDemo( new DefaultConnectionConfiguration() ) );
	}

	public static void start( ApiDemo apiDemo ) {


		INSTANCE = apiDemo;
		INSTANCE.run();


	}

	public ApiDemo( IConnectionConfiguration connectionConfig ) {
		m_connectionConfiguration = connectionConfig; 
		m_connectionPanel = new ConnectionPanel(); // must be done after connection config is set



	}

	protected void onOpenFile() {
		// TODO Auto-generated method stub
		final Frame f = new Frame();
		//Create a file chooser
		FileDialog fd = new FileDialog(m_frame, "Choose a file", FileDialog.LOAD);
		// 	fd.setDirectory("C:\\");
		fd.setFile("*.xls");
		fd.setVisible(true);
		inputFileName = fd.getFile();
		if (inputFileName == null){
			System.out.println("You cancelled the choice");
			show("You cancelled the choice");
		}
		else
		{  
			System.out.println("You chose " + inputFileName);
			show("You chose " + inputFileName);
			orderHashMap.clear();

			excelInput.setInputFile(inputFileName);
			try {
				orderHashMap = excelInput.read(orderHashMap);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
			//	excelOutput.setOutputFile("Forex.xls");
			//	 excelOutput.write(orderHashMap);
			fileReadingCounter = 0;
			show(new Date() + " File " + inputFileName + " read back.");
			SortedSet<Long> keys = new TreeSet<Long>(orderHashMap.keySet());
			for (Long key : keys) {
				show("Order Seq No. " + orderHashMap.get(key).orderSeqNo + " Currency: " + orderHashMap.get(key).Symbol + " Quantity: " + orderHashMap.get(key).Quantity);


			}

		}

	}

	public ApiController controller() {
		if ( m_controller == null ) {
			m_controller = new ApiController( this, getInLogger(), getOutLogger() );
		}
		return m_controller;
	}

	public void run() {

		contractMap.put("NZDUSD", m_contract_NZDUSD);
		contractMap.put("AUDUSD", m_contract_AUDUSD);
		contractMap.put("GBPUSD", m_contract_GBPUSD);
		contractMap.put("EURUSD", m_contract_EURUSD);
		contractMap.put("EURCHF", m_contract_EURCHF);
		contractMap.put("EURGBP", m_contract_EURGBP);
		contractMap.put("EURJPY", m_contract_EURJPY);
		contractMap.put("USDCAD", m_contract_USDCAD);		
		contractMap.put("USDCHF", m_contract_USDCHF);
		contractMap.put("USDJPY", m_contract_USDJPY);
		contractMap.put("CADJPY", m_contract_CADJPY);
		contractMap.put("CADCHF", m_contract_CADCHF);
		contractMap.put("CHFJPY", m_contract_CHFJPY);
		contractMap.put("EURCAD", m_contract_EURCAD);
		contractMap.put("EURSGD", m_contract_EURSGD);
		contractMap.put("GBPCAD", m_contract_GBPCAD);		
		contractMap.put("GBPJPY", m_contract_GBPJPY);
		//    	contractMap.put("GBPSGD", m_contract_GBPSGD);
		contractMap.put("USDSGD", m_contract_USDSGD);
		contractMap.put("AUDCAD", m_contract_AUDCAD);
		contractMap.put("EURAUD", m_contract_EURAUD);
		//   	contractMap.put("CHFAUD", m_contract_CHFAUD);
		contractMap.put("AUDJPY", m_contract_AUDJPY);
		contractMap.put("AUDSGD", m_contract_AUDSGD);		
		contractMap.put("EURNZD", m_contract_EURNZD);
		contractMap.put("AUDNZD", m_contract_AUDNZD);
		contractMap.put("GBPAUD", m_contract_GBPAUD);
		contractMap.put("GBPNZD", m_contract_GBPNZD);
		contractMap.put("NZDJPY", m_contract_NZDJPY);
		contractMap.put("AUDCNH", m_contract_AUDCNH);
		contractMap.put("CNHJPY", m_contract_CNHJPY);
		contractMap.put("USDCNH", m_contract_USDCNH);
		contractMap.put("EURCNH", m_contract_EURCNH);
		contractMap.put("GBPCNH", m_contract_GBPCNH);





		forexListenerHashMap.put("NZDUSD", m_stockListener_NZDUSD);
		forexListenerHashMap.put("AUDUSD", m_stockListener_AUDUSD);
		forexListenerHashMap.put("GBPUSD", m_stockListener_GBPUSD);
		forexListenerHashMap.put("EURUSD", m_stockListener_EURUSD);
		forexListenerHashMap.put("EURCHF", m_stockListener_EURCHF);
		forexListenerHashMap.put("EURGBP", m_stockListener_EURGBP);
		forexListenerHashMap.put("EURJPY", m_stockListener_EURJPY);
		forexListenerHashMap.put("USDCAD", m_stockListener_USDCAD);		
		forexListenerHashMap.put("USDCHF", m_stockListener_USDCHF);
		forexListenerHashMap.put("USDJPY", m_stockListener_USDJPY);
		forexListenerHashMap.put("CADJPY", m_stockListener_CADJPY);
		forexListenerHashMap.put("CADCHF", m_stockListener_CADCHF);
		forexListenerHashMap.put("CHFJPY", m_stockListener_CHFJPY);
		forexListenerHashMap.put("EURCAD", m_stockListener_EURCAD);
		forexListenerHashMap.put("EURSGD", m_stockListener_EURSGD);
		forexListenerHashMap.put("GBPCAD", m_stockListener_GBPCAD);		
		forexListenerHashMap.put("GBPJPY", m_stockListener_GBPJPY);
		//    	forexListenerHashMap.put("GBPSGD", m_stockListener_GBPSGD);
		forexListenerHashMap.put("USDSGD", m_stockListener_USDSGD);
		forexListenerHashMap.put("AUDCAD", m_stockListener_AUDCAD);
		forexListenerHashMap.put("EURAUD", m_stockListener_EURAUD);
		//   	forexListenerHashMap.put("CHFAUD", m_stockListener_CHFAUD);
		forexListenerHashMap.put("AUDJPY", m_stockListener_AUDJPY);
		forexListenerHashMap.put("AUDSGD", m_stockListener_AUDSGD);		
		forexListenerHashMap.put("EURNZD", m_stockListener_EURNZD);
		forexListenerHashMap.put("AUDNZD", m_stockListener_AUDNZD);
		forexListenerHashMap.put("GBPAUD", m_stockListener_GBPAUD);
		forexListenerHashMap.put("GBPNZD", m_stockListener_GBPNZD);
		forexListenerHashMap.put("NZDJPY", m_stockListener_NZDJPY);
		forexListenerHashMap.put("AUDCNH", m_stockListener_AUDCNH);
		forexListenerHashMap.put("CNHJPY", m_stockListener_CNHJPY);
		forexListenerHashMap.put("USDCNH", m_stockListener_USDCNH);
		forexListenerHashMap.put("EURCNH", m_stockListener_EURCNH);
		forexListenerHashMap.put("GBPCNH", m_stockListener_GBPCNH);


		m_tabbedPanel.addTab( "Connection", m_connectionPanel);


		//	m_tabbedPanel.addTab( "Market Data", m_mktDataPanel);
		m_tabbedPanel.addTab( "Trading", m_tradingPanel);
		//	m_tabbedPanel.addTab( "Account Info", m_acctInfoPanel);
		//	m_tabbedPanel.addTab( "Acct/Pos Multi", m_acctPosMultiPanel);
		//	m_tabbedPanel.addTab( "Options", m_optionsPanel);
		//	m_tabbedPanel.addTab( "Combos", m_comboPanel);
		//	m_tabbedPanel.addTab( "Contract Info", m_contractInfoPanel);
		//	m_tabbedPanel.addTab( "Advisor", m_advisorPanel);
		//		m_tabbedPanel.addTab( "Prices", m_pricesPanel);

		// m_tabbedPanel.addTab( "Strategy", m_stratPanel); in progress

		m_msg.setEditable( false);
		m_msg.setLineWrap( true);
		JScrollPane msgScroll = new JScrollPane( m_msg);
		msgScroll.setPreferredSize( new Dimension( 10000, 120) );

		JScrollPane outLogScroll = new JScrollPane( m_outLog);
		outLogScroll.setPreferredSize( new Dimension( 10000, 120) );

		JScrollPane inLogScroll = new JScrollPane( m_inLog);
		inLogScroll.setPreferredSize( new Dimension( 10000, 120) );

		NewTabbedPanel bot = new NewTabbedPanel();
		bot.addTab( "Messages", msgScroll);
		bot.addTab( "Log (out)", outLogScroll);
		bot.addTab( "Log (in)", inLogScroll);


		m_frame.add( bot, BorderLayout.SOUTH);	

		m_frame.add( m_tabbedPanel);
		//     m_frame.add(m_forex);

		m_frame.setSize( 1024, 768);
		m_frame.setVisible( true);
		m_frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE);
		m_frame.setTitle("Built @ " + new Date()); 

		// make initial connection to local host, port 4001, client id 0, no connection options
		controller().connect( "127.0.0.1", 7496, 0, m_connectionConfiguration.getDefaultConnectOptions() != null ? "" : null );

		Thread me = Thread.currentThread();


		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

		// Get the date today using Calendar object.
		// Using DateFormat format method we can create a string 
		// representation of a date with the defined format.

		String orderDateStr = formatter.format(new Date());

		//Chceck whether a data-specific file exist or not. If exist, use it instead.
		File f = new File("Forex" + orderDateStr + ".xls");
		if(f.isFile()) { 
			// do something
			inputFileName = "Forex" + orderDateStr + ".xls";
		}

		//Read back file only once.
		excelInput.setInputFile(inputFileName);
		try {
			orderHashMap = excelInput.read(orderHashMap);
			SortedSet<Long> keys = new TreeSet<Long>(orderHashMap.keySet());
			for (Long key : keys) {
				show("Order Seq No. " + orderHashMap.get(key).orderSeqNo + " Currency: " + orderHashMap.get(key).Symbol + " Quantity: " + orderHashMap.get(key).Quantity);


			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

		(new OrderManagingThread()).start();
		(new OrderSubmittingThread()).start();
		(new MarketDataManagingThread()).start();

		//	 for(Entry<String, Contract> currentContract : contractMap.entrySet())
		{
			//			 Ta4J_backtest Ta4J_backtest = new Ta4J_backtest(this, contractMap, serverTimeCalendar, orderHashMap);		 
			//			 Ta4J_backtest.start();
			//			 try {
			////				Ta4J_backtest.join();
			//			} 
			//			 catch (InterruptedException e) {
			//				// TODO Auto-generated catch block
			//				e.printStackTrace();
			//			}
		}


	}


	ITimeHandler timeHandler = new ITimeHandler() {


		@Override public void currentTime(long time) {
			serverTimeCalendar.setTime(new Date(time * 1000));

		}
	};

	//order be valid before actual trading time. - is before actual time, + is after actual time.
	private int secondBeforeActualOrderTime = 0;

	@Override public void connected() {

		//show( "connected");

		m_connectionPanel.m_status.setText( "connected");
		if(m_contract_EURCNH.getAskPrice() > 0 && m_contract_EURCNH.getBidPrice() > 0 && m_contract_GBPJPY.getAskPrice() > 0 && m_contract_GBPJPY.getBidPrice() > 0){
			m_connectionPanel.m_dataLink.setText("Price information availble.");
		}else{
			m_connectionPanel.m_dataLink.setText("Price information NOT available. Please check connect and restart SW if needed.");

		}


		controller().reqCurrentTime(timeHandler);
		/**/


		//  m_timer.start();

		/*
		controller().reqBulletins( true, new IBulletinHandler() {
			@Override public void bulletin(int msgId, NewsType newsType, String message, String exchange) {
				String str = String.format( "Received bulletin:  type=%s  exchange=%s", newsType, exchange);
				show( str);
				show( message);
			}
		});
		 */
	}

	@Override public void disconnected() {
		//	show( "disconnected");
		m_connectionPanel.m_status.setText( "disconnected");
		//	m_timer.stop();
	}

	@Override public void accountList(ArrayList<String> list) {
		show( "Received account list");
		m_acctList.clear();
		m_acctList.addAll( list);
		System.out.println("All account: " + m_acctList.toString());
	}

	@Override public void show( final String str) {

		SwingUtilities.invokeLater( new Runnable() {
			@Override public void run() {
				m_msg.append(str);
				m_msg.append( "\n\n");

				Dimension d = m_msg.getSize();
				m_msg.scrollRectToVisible( new Rectangle( 0, d.height, 1, 1) );
				//			System.out.println(new Date() + " message size: " + m_msg.getSize()); 
			}
		});
		/**/
	}

	@Override public void error(Exception e) {
		show( e.toString() );
	}

	@Override public void message(int id, int errorCode, String errorMsg) {
		show( id + " " + errorCode + " " + errorMsg);
	}

	class ConnectionPanel extends JPanel {
		private final JTextField m_host = new JTextField( m_connectionConfiguration.getDefaultHost(), 10);
		private final JTextField m_port = new JTextField( m_connectionConfiguration.getDefaultPort(), 7);
		private final JTextField m_connectOptionsTF = new JTextField( m_connectionConfiguration.getDefaultConnectOptions(), 30);
		private final JTextField m_clientId = new JTextField("0", 7);
		private final JLabel m_status = new JLabel("Disconnected");
		private final JLabel m_dataLink = new JLabel("Price Data not available. Please check connection and restart");
		private final JLabel m_orderSubmission = new JLabel("Order submission task isn't running");
		private final JLabel m_orderManaging = new JLabel("Order managing task isn't running");
		private final JLabel m_marketDataManaging = new JLabel("m_marketDataManaging task isn't running");


		private final JLabel m_defaultPortNumberLabel = new JLabel("<html>Live Trading ports:<b> TWS: 7496; IB Gateway: 4001.</b><br>"
				+ "Simulated Trading ports for new installations of "
				+ "version 954.1 or newer: "
				+ "<b>TWS: 7497; IB Gateway: 4002</b></html>");

		public ConnectionPanel() {
			HtmlButton connect = new HtmlButton("Connect") {
				@Override public void actionPerformed() {
					onConnect();
				}
			};

			HtmlButton disconnect = new HtmlButton("Disconnect") {
				@Override public void actionPerformed() {
					controller().disconnect();
				}
			};


			HtmlButton openFile = new HtmlButton("Open order File") {
				@Override public void actionPerformed() {
					onOpenFile();
				}
			};

			JPanel p1 = new VerticalPanel();
			p1.add( "Host", m_host);
			p1.add( "Port", m_port);
			p1.add( "Client ID", m_clientId);
			if ( m_connectionConfiguration.getDefaultConnectOptions() != null ) {
				p1.add( "Connect options", m_connectOptionsTF);
			}
			p1.add( "", m_defaultPortNumberLabel);

			JPanel p2 = new VerticalPanel();
			p2.add( connect);
			p2.add( disconnect);
			p2.add(openFile);
			p2.add( Box.createVerticalStrut(20));


			JPanel p3 = new VerticalPanel();
			p3.setBorder( new EmptyBorder( 20, 0, 0, 0));
			p3.add( "Connection status: ", m_status);
			p3.add("Data link status: ", m_dataLink);
			p3.add("Order submission task status: ", m_orderSubmission);
			p3.add("Order managing task status: ", m_orderManaging);			
			p3.add("Market Data managing task status: ", m_marketDataManaging);

			JPanel p4 = new JPanel( new BorderLayout() );
			p4.add( p1, BorderLayout.WEST);
			p4.add( p2);
			p4.add( p3, BorderLayout.SOUTH);

			setLayout( new BorderLayout() );
			add( p4, BorderLayout.NORTH);
		}

		public void onConnect() {
			int port = Integer.parseInt( m_port.getText() );
			int clientId = Integer.parseInt( m_clientId.getText() );
			controller().connect( m_host.getText(), port, clientId, m_connectOptionsTF.getText());
		}
	}

	private static class Logger implements ILogger {
		final private JTextArea m_area;

		Logger( JTextArea area) {
			m_area = area;
		}

		@Override public void log(final String str) {
			SwingUtilities.invokeLater( new Runnable() {
				@Override public void run() {
					//					m_area.append(str);
					//					
					//					Dimension d = m_area.getSize();
					//					m_area.scrollRectToVisible( new Rectangle( 0, d.height, 1, 1) );
				}
			});
		}
	}




	private void orderTransmit(Contract contract2Send, Order order2Send, long orderSeqNo){
		show(new Date() + " Sending new Order: " + order2Send.orderId() + " " + contract2Send.symbol() + contract2Send.currency() + " STOP: " + order2Send.auxPrice() + " LMT: " + order2Send.lmtPrice());
		System.out.println("Sending Order: " + order2Send.orderId() + " " + contract2Send.symbol() + contract2Send.currency() + " STOP: " + order2Send.auxPrice() + " LMT: " + order2Send.lmtPrice());
		ForexOrderHandler orderHandler = new ForexOrderHandler(order2Send, orderSeqNo);
		ApiDemo.INSTANCE.controller().placeOrModifyOrder( contract2Send, order2Send, orderHandler);	  		

		order2Send.seqOrderNo(orderSeqNo);
		order2Send.groupId(orderHashMap.get(orderSeqNo).groupId);
		submittedOrderHashMap.put(order2Send.orderId(), order2Send);

		//Put actual OrderId into order ArrayList
		forex orderDetail = orderHashMap.get(orderSeqNo);
		orderDetail.orderIdList.add(order2Send.orderId());
		orderHashMap.put(orderSeqNo, orderDetail);	

		if(currentMaxOrderId < order2Send.orderId())
			currentMaxOrderId = order2Send.orderId();

		System.out.println("Orders in OrderHashMap: " + order2Send.orderId() + " " + contract2Send.symbol() + contract2Send.currency() + " STOP: " + order2Send.auxPrice() + " LMT: " + order2Send.lmtPrice());



		//Aaron for tracing submitted order status and forex order status
		String submittedStatus;
		if(submittedOrderHashMap.get(order2Send.orderId()) == null){
			submittedStatus = "Hey. I am not it";
		}else{
			submittedStatus = "Yes, you got it.";
		}

		System.out.println(new Date() + "Submitted orderId: " + order2Send.orderId() + " in submittedOrderHashMap " + submittedStatus + " Total submitted orders: " + submittedOrderHashMap.size());


		System.out.println(new Date() + "Submitted order sequence No.: " + order2Send.seqOrderNo() + " in orderHashMap " + orderHashMap.get(order2Send.seqOrderNo()).OrderStatus);

		/*try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

	}



	private double fixDoubleDigi(double price){

		if(price < 1.0){
			double prices = price;
			DecimalFormat df = new DecimalFormat("#.#####");      
			price = Double.valueOf(df.format(prices));
			return price = Math.round(prices * 10000.0) / 10000.0;
		}else if(price < 10.0){
			double prices = price;
			DecimalFormat df = new DecimalFormat("#.####");  
			price = Double.valueOf(df.format(prices));
			price = Math.round(prices * 1000.0) / 1000.0;
			return price;
		}else if(price < 100.0){
			double prices = price;
			DecimalFormat df = new DecimalFormat("##.###");      
			price = Double.valueOf(df.format(prices));
			price = Math.round(prices * 100.0) / 100.0;
			return price;
		}else {
			double prices = price;
			DecimalFormat df = new DecimalFormat("###.###");      
			price = Double.valueOf(df.format(prices));
			price = Math.round(prices * 100.0) / 100.0;
			return price;
		}
	}


	class ForexPrices{
		double triggerPrice = 0.0;
		double profitPrice = 0.0;
		double stoprPrice = 0.0;

	};


	private ForexPrices calTriggerPrice(forex orderDetail, Contract currentContract, boolean realTime){
		Integer duration = Integer.parseInt(orderDetail.ValidDuration);
		double triggerPrice;

		//if duration is longer than 30 minutes, take 3 bars.
		//if duration is beblow 5 minutes. take 1 bar.
		//If duration is bewtween 5 and 30, takes 2 bars.
		TreeSet<Long> keys = new TreeSet<Long>(currentContract.historical5MBarMap.keySet());
		TreeSet<Long> treereverse = (TreeSet<Long>) keys.descendingSet();
		Iterator<Long> iterator =  treereverse.iterator();
		Integer counter = 0;
		Double high = 0.0, low = Double.MAX_VALUE, TriggerPct = 0.02, TriggerPctAskBid = 0.05, stopLosspct;


		stopLosspct = Double.parseDouble(orderDetail.LossPct);
		TriggerPctAskBid = Double.parseDouble(orderDetail.TriggerPct);
		TriggerPct = TriggerPctAskBid / 2;

		//If historical data not available yet, use real time tick price.
		if((!iterator.hasNext()) || realTime){

			if(orderDetail.TradeMethod.equals("SELL")){
				triggerPrice = contractMap.get(orderDetail.Symbol).getBidPrice() * (1 - TriggerPctAskBid / 100);
				//Let's set profit taking to 0.6% and adjust it later in order managing task.

			}else 
			{
				triggerPrice = contractMap.get(orderDetail.Symbol).getAskPrice() * (1 +TriggerPctAskBid / 100);
				//Let's set profit taking to 0.6% and adjust it later in order managing task.
			}


		}else if(duration <= 60){ //If duration is less or euqual to 60 seconds. use 5 minutes bar high and low.
			if(orderDetail.TradeMethod.equals("SELL")){
				triggerPrice = currentContract.historical5MBarMap.get(treereverse.first()).low() * (1 - TriggerPct / 100);
			}else {
				triggerPrice = currentContract.historical5MBarMap.get(treereverse.first()).high() * (1 + TriggerPct / 100);
			}			
		}else if(duration <= 5 * 60){//If duration is 5 minutes, let's use last 10 minutes's high and low.
			while(iterator.hasNext() && counter < 2){
				Bar bar = currentContract.historical5MBarMap.get(iterator.next());
				if(high < bar.high())
					high = bar.high();
				if(low > bar.low())
					low = bar.low();
			}

			if(orderDetail.TradeMethod.equals("SELL")){
				triggerPrice = low * (1 - TriggerPct  / 100);
			}else 
			{
				triggerPrice = high * (1 + TriggerPct  / 100);
			}
		}else if(duration <= 15 * 60){//If duration is 15 minutes, let's use last 30 minutes's high and low.
			while(iterator.hasNext() && counter < 2){
				Bar bar = currentContract.historical5MBarMap.get(iterator.next());
				if(high < bar.high())
					high = bar.high();
				if(low > bar.low())
					low = bar.low();
			}

			if(orderDetail.TradeMethod.equals("SELL")){
				triggerPrice = low * (1 - TriggerPct  / 100);
			}else 
			{
				triggerPrice = high * (1 + TriggerPct  / 100);
			}
		}
		else if(duration <= 30 * 60){//If duration is 30 minutes, let's use last 60 minutes's high and low.

			while(iterator.hasNext() && counter < 2){
				Bar bar = currentContract.historical5MBarMap.get(iterator.next());
				if(high < bar.high())
					high = bar.high();
				if(low > bar.low())
					low = bar.low();
			}

			if(orderDetail.TradeMethod.equals("SELL")){
				triggerPrice = low * (1 - TriggerPct  / 100);
			}else 
			{
				triggerPrice = high * (1 + TriggerPct  / 100);
			}

		}else{//If duration is longer than 30 minutes, let's use last 90 minutes's high and low.
			while(iterator.hasNext() && counter < 2){
				Bar bar = currentContract.historical5MBarMap.get(iterator.next());
				if(high < bar.high())
					high = bar.high();
				if(low > bar.low())
					low = bar.low();
			}

			if(orderDetail.TradeMethod.equals("SELL")){
				triggerPrice = low * (1 - TriggerPct  / 100);	
			}else 
			{
				triggerPrice = high * (1 + TriggerPct / 100);
			}



		}

		ForexPrices orderPrices = new ForexPrices(); 

		//Make sure that trigger price is 0.1 max away from current bid/ask price.
		if(orderDetail.TradeMethod.equals("SELL")){
			Double currentLowPrice = contractMap.get(orderDetail.Symbol).getBidPrice() * (1 - TriggerPctAskBid / 100); 
			if(triggerPrice < currentLowPrice)
				triggerPrice = currentLowPrice;
			//Let's set profit taking to 0.6% and adjust it later in order managing task.
			orderPrices.triggerPrice = triggerPrice;	
			orderPrices.profitPrice = triggerPrice * (1 -  1.0 / 100);
			// profitTakingPrice = triggerPrice * (1 - Double.parseDouble(orderDetail.ProfitPct) / 100);
			if(	contractMap.get(orderDetail.Symbol).mediumMedSma > 0.0 && contractMap.get(orderDetail.Symbol).mediumMedSma < 0.5)
				orderPrices.stoprPrice = contractMap.get(orderDetail.Symbol).mediumMedSma;
			else
				orderPrices.stoprPrice = triggerPrice * (1 + stopLosspct / 100);
		}
		else 
		{
			Double currentHighPrice = contractMap.get(orderDetail.Symbol).getAskPrice() * (1 + TriggerPctAskBid / 100); 
			if(triggerPrice > currentHighPrice)
				triggerPrice = currentHighPrice;
			//Let's set profit taking to 0.6% and adjust it later in order managing task.

			orderPrices.triggerPrice = triggerPrice;	
			orderPrices.profitPrice = triggerPrice * (1 + 1.0 / 100);
			// profitTakingPrice = triggerPrice * (1 + Double.parseDouble(orderDetail.ProfitPct) / 100);
			if(	contractMap.get(orderDetail.Symbol).mediumMedSma > 0.0 && contractMap.get(orderDetail.Symbol).mediumMedSma < 0.5)
				orderPrices.stoprPrice = contractMap.get(orderDetail.Symbol).mediumMedSma;
			else					
				orderPrices.stoprPrice = triggerPrice * (1 - stopLosspct  / 100);	
			//		orderPrices.stoprPrice = contractMap.get(orderDetail.Symbol).longMinSma;

		}


		return orderPrices;

	}


	public Order bracketStopOrder(forex orderDetail, int orderID){
		{
			ApiDemo.INSTANCE.controller().client().reqIds(-1);

			if(nextOrderId < ApiDemo.INSTANCE.controller().availableId())
				nextOrderId = ApiDemo.INSTANCE.controller().availableId();

			if(currentMaxOrderId >= nextOrderId)
				nextOrderId = currentMaxOrderId + 1;


			//BRACKET ORDER
			//! [bracketsubmit]
			Double triggerPrice;
			Double profitTakingPrice;
			Double stopLossPrice;

			Double quantity = Double.parseDouble(orderDetail.Quantity);

			ForexPrices orderPrices;
			orderPrices = calTriggerPrice(orderDetail, contractMap.get(orderDetail.Symbol), false);	
			triggerPrice = orderPrices.triggerPrice;
			profitTakingPrice = orderPrices.profitPrice;
			stopLossPrice = orderPrices.stoprPrice;				

			//			 nextOrderId = 0;
			int parentOrderId = nextOrderId;
			String action = orderDetail.TradeMethod;
			//This will be our main or "parent" order
			Order parent = new Order();
			parent.orderId(parentOrderId);
			parent.parentId(0);
			parent.action(action);
			parent.orderType("STP");
			parent.totalQuantity(quantity);
			parent.auxPrice(fixDoubleDigi(triggerPrice));

			DateFormat formatter; 			    	      
			formatter = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

			// Get the date today using Calendar object.

			// Using DateFormat format method we can create a string 
			// representation of a date with the defined format.

			String orderDateStr= orderDetail.Date + " " + orderDetail.Time;		   	      



			//Aaron only valid after specified time	   	    
			try {
				Date orderTime = (Date)formatter.parse(orderDateStr);
				//Try to activate the order a few seconds before actual order time. Need to test
				Calendar cal = Calendar.getInstance();
				cal.setTime(orderTime);
				cal.add(Calendar.SECOND, secondBeforeActualOrderTime);
				orderTime = cal.getTime();
				orderDateStr = formatter.format(orderTime); 
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}    

			parent.goodAfterTime(orderDateStr);  
			try {
				Date  orderTime  = (Date)formatter.parse(orderDateStr);

				//Try to activate the order a few seconds before actual order time. Need to test
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.SECOND, secondBeforeActualOrderTime);

				Date orderPlusDuration = new Date(orderTime.getTime() + Integer.parseInt(orderDetail.ValidDuration) * 1000);
				orderDateStr = formatter.format(orderPlusDuration);
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} 

			parent.goodTillDate(orderDateStr);
			parent.tif("GTD");
			//The parent and children orders will need this attribute set to false to prevent accidental executions.
			//The LAST CHILD will have it set to true.
			parent.transmit(false);				    					
			parent.account(m_acctList.get(0));



			orderTransmit(contractMap.get(orderDetail.Symbol), parent, orderDetail.orderSeqNo);


			nextOrderId++;
			//This is Loss STOPing  order. Its order is is parent Id plus two. And its parent ID is above parent Id;
			Order stopLoss = new Order();
			stopLoss.orderId(parent.orderId() + 2);
			stopLoss.action(action.equals("BUY") ? "SELL" : "BUY");
			stopLoss.orderType("STP");
			//Stop trigger price
			stopLoss.auxPrice(fixDoubleDigi(stopLossPrice));
			stopLoss.totalQuantity(quantity);
			stopLoss.parentId(parent.orderId());
			//In this case, the low side order will be the last child being sent. Therefore, it needs to set this attribute to true 
			//to activate all its predecessors
			stopLoss.transmit(true);				    				 
			stopLoss.account(m_acctList.get(0));
			stopLoss.tif("GTC");

			orderTransmit(contractMap.get(orderDetail.Symbol), stopLoss, orderDetail.orderSeqNo);

			return parent;

		}
	}



	private void OcaOrder(forex orderDetail){

		ApiDemo.INSTANCE.controller().client().reqIds(-1);

		if(nextOrderId < ApiDemo.INSTANCE.controller().availableId())
			nextOrderId = ApiDemo.INSTANCE.controller().availableId();

		if(currentMaxOrderId >= nextOrderId)
			nextOrderId = currentMaxOrderId + 1;



		//BRACKET ORDER
		//! [bracketsubmit]
		Double triggerPrice;
		Double profitTakingPrice;
		Double stopLossPrice;
		Double quantity = Double.parseDouble(orderDetail.Quantity);;

		//This is the buy side.		 
		//		 nextOrderId = 0;
		orderDetail.TradeMethod = "BUY";

		ForexPrices orderPrices;
		orderPrices = calTriggerPrice(orderDetail, contractMap.get(orderDetail.Symbol), false);	
		triggerPrice = orderPrices.triggerPrice;
		profitTakingPrice = orderPrices.profitPrice;
		stopLossPrice = orderPrices.stoprPrice;		 


		int parentOrderId = nextOrderId;
		String action = orderDetail.TradeMethod;
		//This will be our main or "parent" order
		Order parent = new Order();
		parent.orderId(parentOrderId);
		parent.parentId(0);
		parent.action(action);
		parent.orderType("STP");
		parent.totalQuantity(quantity);
		parent.auxPrice(fixDoubleDigi(triggerPrice));
		//The parent and children orders will need this attribute set to false to prevent accidental executions.
		//The LAST CHILD will have it set to true.
		parent.transmit(true);				    					
		parent.account(m_acctList.get(0));


		DateFormat formatter; 

		formatter = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

		// Get the date today using Calendar object.

		// Using DateFormat format method we can create a string 
		// representation of a date with the defined format.

		String orderDateStr= orderDetail.Date + " " + orderDetail.Time;		   	      

		Date orderTime;
		Calendar cal;
		//Aaron only valid after specified time	   	    
		try {
			orderTime  = (Date)formatter.parse(orderDateStr);
			//Try to activate the order a few seconds before actual order time. Need to test
			cal=Calendar.getInstance();
			cal.setTime(orderTime);
			cal.add(Calendar.SECOND, secondBeforeActualOrderTime);
			orderTime = cal.getTime();
			orderDateStr = formatter.format(orderTime); 
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		parent.goodAfterTime(orderDateStr);	   	     
		try {
			orderTime  = (Date)formatter.parse(orderDateStr);


			Date orderPlusDuration = new Date(orderTime.getTime() + Integer.parseInt(orderDetail.ValidDuration) * 1000);
			orderDateStr = formatter.format(orderPlusDuration);
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 

		parent.goodTillDate(orderDateStr);
		parent.tif("GTD");

		nextOrderId++;			
		//This is Loss STOPing  order. Its order is is parent Id plus two. And its parent ID is above parent Id;
		Order stopLoss = new Order();
		stopLoss.orderId(nextOrderId);
		stopLoss.action(action.equals("BUY") ? "SELL" : "BUY");
		stopLoss.orderType("STP");
		//Stop trigger price
		stopLoss.auxPrice(fixDoubleDigi(stopLossPrice));
		stopLoss.totalQuantity(quantity);
		stopLoss.parentId(parent.orderId());
		//In this case, the low side order will be the last child being sent. Therefore, it needs to set this attribute to true 
		//to activate all its predecessors
		stopLoss.transmit(true);				    				 
		stopLoss.account(m_acctList.get(0));
		stopLoss.tif("GTC");




		//This is the sell side
		//			 nextOrderId = 0;
		orderDetail.TradeMethod = "SELL";

		orderPrices = calTriggerPrice(orderDetail, contractMap.get(orderDetail.Symbol), false);	
		triggerPrice = orderPrices.triggerPrice;
		profitTakingPrice = orderPrices.profitPrice;
		stopLossPrice = orderPrices.stoprPrice;			 

		nextOrderId++;
		parentOrderId = nextOrderId;
		action = orderDetail.TradeMethod;
		//This will be our main or "parent" order
		Order parentSell = new Order();
		parentSell.orderId(parentOrderId);
		parentSell.parentId(0);
		parentSell.action(action);
		parentSell.orderType("STP");
		parentSell.totalQuantity(quantity);
		parentSell.auxPrice(fixDoubleDigi(triggerPrice));
		//The parent and children orders will need this attribute set to false to prevent accidental executions.
		//The LAST CHILD will have it set to true.
		parentSell.transmit(true);				    					
		parentSell.account(m_acctList.get(0));


		orderDateStr= orderDetail.Date + " " + orderDetail.Time;		   	      

		//Aaron only valid after specified time	   	    
		try {
			orderTime  = (Date)formatter.parse(orderDateStr);
			//Try to activate the order a few seconds before actual order time. Need to test
			cal=Calendar.getInstance();
			cal.setTime(orderTime);
			cal.add(Calendar.SECOND, secondBeforeActualOrderTime);
			orderTime = cal.getTime();
			orderDateStr = formatter.format(orderTime); 
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			



		//Aaron only valid after specified time	   	    
		try {
			orderTime  = (Date)formatter.parse(orderDateStr);
			//Try to activate the order a few seconds before actual order time. Need to test
			cal=Calendar.getInstance();
			cal.setTime(orderTime);
			cal.add(Calendar.SECOND, secondBeforeActualOrderTime);
			orderTime = cal.getTime();
			orderDateStr = formatter.format(orderTime); 
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		parentSell.goodAfterTime(orderDateStr); 
		try {
			orderTime  = (Date)formatter.parse(orderDateStr);
			Date orderPlusDuration = new Date(orderTime.getTime() + Integer.parseInt(orderDetail.ValidDuration) * 1000);
			orderDateStr = formatter.format(orderPlusDuration);
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 

		parentSell.goodTillDate(orderDateStr);
		parentSell.tif("GTD");
		nextOrderId++;

		//This is Loss STOPing  order. Its order is is parent Id plus two. And its parent ID is above parent Id;
		Order stopLossSell = new Order();
		stopLossSell.orderId(nextOrderId);
		stopLossSell.action(action.equals("BUY") ? "SELL" : "BUY");
		stopLossSell.orderType("STP");
		//Stop trigger price
		stopLossSell.auxPrice(fixDoubleDigi(stopLossPrice));
		stopLossSell.totalQuantity(quantity);
		stopLossSell.parentId(parentSell.orderId());
		//In this case, the low side order will be the last child being sent. Therefore, it needs to set this attribute to true 
		//to activate all its predecessors
		stopLossSell.transmit(true);				    				 
		stopLossSell.account(m_acctList.get(0));	
		stopLossSell.tif("GTC");




		//Profit taking order isn't necessar
		/*
				nextOrderId++;	    									

				//This is profit taking order. Its order is is parent Id plus one. And its parent ID is above parent Id;
				Order takeProfitSell = new Order();
				takeProfitSell.orderId(nextOrderId);
				takeProfitSell.action(action.equals("BUY") ? "SELL" : "BUY");
				takeProfitSell.orderType("LMT");
				takeProfitSell.totalQuantity(quantity);
				takeProfitSell.lmtPrice(fixDoubleDigi(profitTakingPrice));
				takeProfitSell.parentId(parentSell.orderId());
				takeProfitSell.transmit(true);				    					
				takeProfitSell.account(m_acctList.get(0));
				takeProfitSell.tif("GTC");
		 */

		//OCA order
		//! [ocasubmit]
		List<Order> OcaOrders = new ArrayList<Order>();


		OcaOrders.add(parent);
		OcaOrders.add(parentSell);
		OcaOrders = OrderSamples.OneCancelsAll("TestOCA_" + nextOrderId, OcaOrders, 2);
		for (Order o : OcaOrders) {				
			if(o.orderId() == parent.orderId())
			{
				orderTransmit(contractMap.get(orderDetail.Symbol), o, orderDetail.orderSeqNo);

				System.out.println(new Date() + "Send parentBuy orderId: " + o.orderId() + " SeqNo: " + orderDetail.orderSeqNo);
				show(new Date() + "Send parentBuy orderId: " + o.orderId() + " SeqNo: " + orderDetail.orderSeqNo);


				orderTransmit(contractMap.get(orderDetail.Symbol), stopLoss, orderDetail.orderSeqNo);

				System.out.println(new Date() + "Send StopLossBuy orderId: " + stopLoss.orderId() + " SeqNo: " + orderDetail.orderSeqNo);
				show(new Date() + "Send StopLossBuy orderId: " + stopLoss.orderId() + " SeqNo: " + orderDetail.orderSeqNo);

				//Profit taking order isn't necessar
				/*
					orderTransmit(contractMap.get(orderDetail.Symbol), takeProfit, orderDetail.orderSeqNo);
					System.out.println(new Date() + "Send takeProfitBuy orderId: " + takeProfit.orderId() + " SeqNo: " + orderDetail.orderSeqNo);
					show(new Date() + "Send takeProfitBuy orderId: " + takeProfit.orderId() + " SeqNo: " + orderDetail.orderSeqNo);
				 */

			}


		}		

		for (Order o : OcaOrders) {				

			if(o.orderId() == parentSell.orderId())
			{
				orderTransmit(contractMap.get(orderDetail.Symbol), o, orderDetail.orderSeqNo);

				System.out.println(new Date() + "Send parentSell orderId: " + o.orderId() + " SeqNo: " + orderDetail.orderSeqNo);
				show(new Date() + "Send parentSell orderId: " + o.orderId() + " SeqNo: " + orderDetail.orderSeqNo);


				orderTransmit(contractMap.get(orderDetail.Symbol), stopLossSell, orderDetail.orderSeqNo);

				System.out.println(new Date() + "Send stopLossSell orderId: " + stopLossSell.orderId() + " SeqNo: " + orderDetail.orderSeqNo);
				show(new Date() + "Send stopLossSell orderId: " + stopLossSell.orderId() + " SeqNo: " + orderDetail.orderSeqNo);
			}

		}	 
		//Try to clean this object.
		OcaOrders = null;



	}

	private void requestTickData(ConcurrentHashMap<Long, forex> orderHashMap2){

		Contract contractDetail;
		ForexListner forexListenerDetail;


		//Request new necessary 
		for(Entry<Long, forex> entry : orderHashMap2.entrySet()){
			contractDetail = contractMap.get(entry.getValue().Symbol);
			if(currentMarketDataList.contains(entry.getValue().Symbol) == false )
			{
				forexListenerDetail = forexListenerHashMap.get(entry.getValue().Symbol);
				if(forexListenerDetail != null && entry.getValue().Symbol != null && contractDetail != null){			
					controller().reqTopMktData( contractDetail, "", false, forexListenerDetail);
					currentListeningMap.put(entry.getValue().Symbol, forexListenerDetail);
					currentMarketDataList.add(entry.getValue().Symbol);
					//						System.out.println(new Date() + "Request market data " + entry.getValue().Symbol); 
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}	
		}

		//Always submit an request for GBPJPY and EURCNH for market data validation.
		if(currentMarketDataList.contains("GBPJPY") == false)
		{
			contractDetail = contractMap.get("GBPJPY");
			forexListenerDetail = forexListenerHashMap.get("GBPJPY");
			if(forexListenerDetail != null && contractDetail != null){			
				controller().reqTopMktData( contractDetail, "", false, forexListenerDetail);
				currentListeningMap.put("GBPJPY", forexListenerDetail);
				currentMarketDataList.add("GBPJPY");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}	

		if(currentMarketDataList.contains("EURCNH") == false)
		{
			contractDetail = contractMap.get("EURCNH");
			forexListenerDetail = forexListenerHashMap.get("EURCNH");
			if(forexListenerDetail != null && contractDetail != null){			
				controller().reqTopMktData( contractDetail, "", false, forexListenerDetail);
				currentListeningMap.put("EURCNH", forexListenerDetail);
				currentMarketDataList.add("EURCNH");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}	

		//Use tickCounter to check whether tick price is available
		tickCounter = 0;
	}





	// TODO Auto-generated catch block	

	class OrderSubmittingThread extends Thread {
		//		 ForexPositionHandler positionHandler = new ForexPositionHandler();
		double lmtPrice = 0.0;

		public void run() {
			forex orderDetail;
			DateFormat formatter; 
			String orderDateStr = "";
			String OrderSubmittedStr = null;

			System.out.println("Hello from a Order submission thread!");
			//	        Thread.currentThread().setPriority(Thread.MAX_PRIORITY - 1);

			while(true){
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				//Check connection with server every second.
				//Check whether current connection is disconnected. If yes, connect it and skip below action
				if(m_connectionPanel.m_status.getText().toUpperCase().equals("DISCONNECTED"))
				{
					// 			m_connectionPanel.onConnect();
					continue;
				}


				m_connectionPanel.m_orderSubmission.setText(new Date() + " order submission task is running. Next order: " + OrderSubmittedStr);
				OrderSubmittedStr = null;

				Date systemTimePlus1M = new Date(), systemTimePlus2M = new Date(), orderTime= new Date();

				//loop thru all orders in HashMap;
				//	System.out.println("Looping thru all orders: " + new Date());


				//				//Don't do it if it is Sat morning after 6 or on Sunday.
				//				System.out.println(new Date() +" Week day: " + serverTimeCalendar.get(Calendar.DAY_OF_WEEK));
				//				if(((serverTimeCalendar.get(Calendar.DAY_OF_WEEK) == 1) && (serverTimeCalendar.get(Calendar.HOUR_OF_DAY) <= 7)))
				//					continue;	
				//				if(((serverTimeCalendar.get(Calendar.DAY_OF_WEEK) == 7) && (serverTimeCalendar.get(Calendar.HOUR_OF_DAY) >= 7)))
				//					continue;
				//				if((serverTimeCalendar.get(Calendar.DAY_OF_WEEK) == 7))
				//					continue;

				{				
					forex GBPJPYorder, EURCNHorder;
					GBPJPYorder = new forex();	
					EURCNHorder = new forex();


					formatter = new SimpleDateFormat("yyyyMMdd");	
					Date orderDate = new Date(serverTimeCalendar.getTimeInMillis() + 60 * 1000);


					GBPJPYorder.Date = formatter.format(orderDate);
					formatter = new SimpleDateFormat("HH:mm");				
					GBPJPYorder.Time = formatter.format(orderDate) + ":00";


					formatter = new SimpleDateFormat("yyyyMMddHHmm");
					String currentDateStr = formatter.format(orderDate);
					currentDateStr += "0001";
					Long dateCode = Long.parseLong(currentDateStr);


					boolean noNeedCNH = false;
					boolean noNeedGBPJY = false;

					while (orderHashMap.containsKey(dateCode))
					{	
						if(orderHashMap.get(dateCode).Symbol.equals("GBPJPY"))
							noNeedGBPJY = true;
						if(orderHashMap.get(dateCode).Symbol.equals("EURCNH"))
							noNeedCNH= true;
						dateCode++;
						if(noNeedGBPJY || noNeedCNH)
							break;

					}

					GBPJPYorder.Symbol = "GBPJPY";
					GBPJPYorder.Quantity = "25000";
					GBPJPYorder.TradeMethod = "ANY";
					GBPJPYorder.EntryMethod = "STOP";
					GBPJPYorder.TriggerPct = "0.2";
					GBPJPYorder.LossPct = "0.1";
					GBPJPYorder.ProfitPct = "0.2";
					GBPJPYorder.ValidDuration = "60";	
					GBPJPYorder.Importance = "Low";	
					GBPJPYorder.ExitMethod = "STOP";
					GBPJPYorder.groupId = (long) 0;


					EURCNHorder.Date = GBPJPYorder.Date;
					EURCNHorder.Symbol = "EURCNH";
					EURCNHorder.Quantity = "25000";
					EURCNHorder.TradeMethod = "ANY";
					EURCNHorder.EntryMethod = "STOP";
					EURCNHorder.TriggerPct = "0.2";
					EURCNHorder.LossPct = "0.1";
					EURCNHorder.ProfitPct = "0.5";
					EURCNHorder.ValidDuration = "60";	
					EURCNHorder.Importance = "Low";	
					EURCNHorder.ExitMethod = "STOP";
					EURCNHorder.Time = GBPJPYorder.Time;
					EURCNHorder.groupId = (long) 0;

					//				if(noNeedGBPJY == false && noNeedCNH == false){
					//					GBPJPYorder.orderSeqNo = dateCode;
					//					orderHashMap.putIfAbsent(GBPJPYorder.orderSeqNo, GBPJPYorder);
					//					EURCNHorder.orderSeqNo = GBPJPYorder.orderSeqNo + 1;
					//					orderHashMap.putIfAbsent(EURCNHorder.orderSeqNo, EURCNHorder);
					//				}else if(noNeedGBPJY == false){
					//					GBPJPYorder.orderSeqNo = dateCode;
					//					orderHashMap.putIfAbsent(GBPJPYorder.orderSeqNo, GBPJPYorder);
					//				}else if(noNeedCNH == false){
					//					EURCNHorder.orderSeqNo = dateCode;
					//					orderHashMap.putIfAbsent(EURCNHorder.orderSeqNo, EURCNHorder);
					//				}
					//			System.out.println("Size of orderHashMap: " + orderHashMap.size());
				}		
				SortedSet<Long> keys = new TreeSet<Long>(orderHashMap.keySet());
				for (Long key : keys) {



					orderDetail = orderHashMap.get(key);
					if(orderDetail == null) continue;

					if(orderDetail!=null && orderDetail.OrderStatus !=null && !orderDetail.OrderStatus.isEmpty()){
						if(fileReadingCounter % 1000 == 0){
							//			    		System.out.println(" Already submitted orders: " + orderDetail.Date + orderDetail.Time + " " +orderDetail.Symbol + orderDetail.orderIdList.toString() + "Submit status: " + orderDetail.OrderStatus + " Fill @" + orderDetail.ActualPrice + " Comment: " + orderDetail.comment);
							//			    		show(new Date() + " Already submitted orders: " + orderDetail.Date + orderDetail.Time + " " +orderDetail.Symbol + orderDetail.orderIdList.toString() + "Submit status: " + orderDetail.OrderStatus + " Fill @" + orderDetail.ActualPrice + " Comment: " + orderDetail.comment);
						}
						continue;
					}

					try{



						formatter = new SimpleDateFormat("yyyyMMdd HH:mm");

						// Get the date today using Calendar object.
						// Using DateFormat format method we can create a string 
						// representation of a date with the defined format.

						orderDateStr= orderDetail.Date + " " + orderDetail.Time;		   	      
						orderTime  = (Date)formatter.parse(orderDateStr); 

					}catch (Exception e){} 

					if(orderTime.after(new Date()) && OrderSubmittedStr == null){
						OrderSubmittedStr = orderDetail.Symbol + "@" + orderDateStr;

					}

					//			    	 long time = System.currentTimeMillis();
					systemTimePlus1M = serverTimeCalendar.getTime();	
					systemTimePlus2M = new Date(serverTimeCalendar.getTimeInMillis() + 50 * 1000);	

					//Compare system time and order to make sure that we submit the order on time or this is a market order which should be submitted immedietely
					if(systemTimePlus1M.before(orderTime) && systemTimePlus2M.after(orderTime) || (orderDetail.EntryMethod != null && orderDetail.EntryMethod.equals("MKT") && orderDetail.OrderStatus != null && !orderDetail.OrderStatus.isEmpty())){

						boolean needToSubmit = true;
						if(orderDetail.OCA == true){ //first is OCA is false. which is only a single order.

							//here is the multiple order. 
							//first check whether a valid order is there. 
							SortedSet<Long> keys1 = new TreeSet<Long>(orderHashMap.keySet());
							for (Long key1 : keys1) { 					

								forex iterOrderDetail = orderHashMap.get(key1);
								if(iterOrderDetail.groupId == orderDetail.groupId){
									if(orderDetail.OrderStatus != null && orderDetail.OrderStatus.equals("Filled")){
										needToSubmit = false;
										break;
									}
								}

							}	
							keys1 = null;
						}
						//To be confirmed later.

						if(!orderDetail.TradeMethod.equals("CLOSE"))//always close the order.
						{
						//Make sure that we would NOT submit an duplicate order with current open order;
						for (Entry<Integer, forex> entry : liveForexOrderMap.entrySet()) {
							forex order2Loop = entry.getValue();
							//						    forex tmpOrder = orderHashMap.get(order2Loop.seqOrderNo());
							Order entryOrder = submittedOrderHashMap.get(entry.getKey());
							if(entryOrder == null)
								entryOrder = liveOrderMap.get(entry.getKey());

							if(entryOrder == null)
								continue;
							if(orderDetail.Symbol.equals(order2Loop.Symbol)){
								{
									//duplicated parent order is OK. All parent order has time in force as "GTD" good to date.
									if(liveOrderMap.get(entry.getKey()).tif().equals("GTD"))
										continue;

									if(entryOrder.parentId() == 0 && entryOrder.tif().equals("GTC"))
									{	
										needToSubmit = false;
										orderDetail.OrderStatus = "Cancelled";
										orderDetail.comment = "Cancelled due to duplicated order in open order";						    		
										orderHashMap.put(orderDetail.orderSeqNo, orderDetail);
										System.out.println(new Date() + " No need to submit Order:  " + liveOrderMap.get(entry.getKey()).parentId());	
										break;
									}

									if(entryOrder.parentId() != 0 && liveOrderMap.containsKey(entryOrder.parentId()))
										continue;

									//If order's parent order has been executed. Then skip current order to avoid duplicate

									if(executedOrderMap.containsKey(entryOrder.parentId()))
									{						    		
										needToSubmit = false;
										orderDetail.OrderStatus = "Cancelled";
										orderDetail.comment = "Cancelled due to duplicated order in open order";						    		
										orderHashMap.put(orderDetail.orderSeqNo, orderDetail);
										System.out.println(new Date() + " No need to submit because of executed parent OrderId:  " + liveOrderMap.get(entry.getKey()).parentId());
										break;
									}	

									//						    		System.out.println(" I escpe hee: " + entryOrder.orderId() + entryOrder.orderType() + entryOrder.parentId());
									//						    		System.out.println("Is its parent Id in executedOrderMap ? " + executedOrderMap.containsKey(entryOrder.parentId()));
									//						    		System.out.println("Is its in submittedOrderHashMap ? " + submittedOrderHashMap.containsKey(entryOrder.orderId()));
									//						    		System.out.println("Is its in liveOrderMap ? " + liveOrderMap.containsKey(entryOrder.orderId()));


								}
							}    
						}
					}

						if(needToSubmit == false)
							continue;

						System.out.println(new Date().toString() + " " + submittedOrderHashMap.size() + " Already submitted orders in submittedOrderHashMap");
						System.out.println(new Date().toString() + " " + liveOrderMap.size() + " live orders in hashmap liveOrderMap");



						//				    System.out.println("Try to submit: " + orderDetail.Date + orderDetail.Time + " " +orderDetail.Symbol);
						//				    show(new Date() + " Try to submit: " + orderDetail.Date + orderDetail.Time + " " +orderDetail.Symbol);

						//Check whether price information is valid or not.
						System.out.println(orderDetail.Symbol + " BID price: " + contractMap.get(orderDetail.Symbol).getBidPrice() );
						if(contractMap.get(orderDetail.Symbol).historical5MBarMap.isEmpty() && (contractMap.get(orderDetail.Symbol).getAskPrice() == 0.0 || contractMap.get(orderDetail.Symbol).getBidPrice() == 0.0)){
							System.out.println("Historical price map is empty: " + orderDetail.Date + orderDetail.Time + " " +orderDetail.Symbol);
							continue;
						}
						System.out.println("Preparing: " + orderDetail.Date + orderDetail.Time + " " +orderDetail.Symbol);

						orderDetail.OrderStatus = "Preparing";
						orderHashMap.put(key, orderDetail);
						System.out.println("Order status: " + orderHashMap.get(orderDetail.Symbol));


						// Do something
						if(orderDetail.EntryMethod != null && orderDetail.EntryMethod.equals("MKT")){
							if(orderDetail.TradeMethod.equals("CLOSE"))
								closeCurrentLiveOrder(orderDetail);
							else
								placeMarketOrder(orderDetail);

						}
						else if( orderDetail.TradeMethod.equals("BUY")){
							if(orderDetail.EntryMethod != null &&orderDetail.EntryMethod.equals("STOP"))
							{
								//This is a new order
								bracketStopOrder(orderDetail, 0);
							}
						}
						else if(orderDetail.TradeMethod.equals("SELL")){
							if(orderDetail.EntryMethod != null && orderDetail.EntryMethod.equals("STOP"))
							{
								bracketStopOrder(orderDetail, 0);
							}
						}
						else if(orderDetail.TradeMethod.equals("BOTH")){
							if(orderDetail.EntryMethod != null && orderDetail.EntryMethod.equals("STOP")){
								//let's sumit buy order first 
								orderDetail.TradeMethod = "BUY";
								Order tempOrder = bracketStopOrder(orderDetail, 0);

								//Let's wait for 1 second.
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {

									e.printStackTrace();
								}
								//To make sure that this is a new order
								nextOrderId = tempOrder.orderId() + 1;
								orderDetail.TradeMethod = "SELL";		    				 
								//then sell order.
								bracketStopOrder(orderDetail, nextOrderId);
							}			    			 
						}
						else if(orderDetail.TradeMethod.equals("ANY")){			    			 
							OcaOrder(orderDetail);    					
						}

						//Wait 1 seconds after each order submission.
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						//						 System.out.println("Submiited: " + orderDetail.Date + orderDetail.Time + " " +orderDetail.Symbol);
						//						 System.out.println("Order status: after submision" + orderHashMap.get(orderDetail.Symbol));	



					}else if(systemTimePlus2M.before(orderTime)){
						break;
					}
				}
			}
		}


	}


	private void closeCurrentLiveOrder(forex orderDetailIn) {
		// TODO Auto-generated method stub
		for(Entry<Integer, Order> entry : liveOrderMap.entrySet()){

			//Guy, let's rest 500ms here
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}   


			Order order = entry.getValue();
			forex orderDetail = null;
			if(order != null)
				orderDetail = orderHashMap.get(order.seqOrderNo()); 		
			if(orderDetail == null)
				continue;
			Contract currencyContract = contractMap.get(orderDetail.Symbol);

			if(orderDetail.Symbol.equals(orderDetailIn.Symbol))
				adjustStopPrice(entry.getKey(), entry.getValue(), -0.1);
		}
	}


	//TODO Auto-generated catch block
	class OrderManagingThread extends Thread {
		double lmtPrice = 0.0;
		int counter = 0;
		ExecutionFilter exeFilterReport = new ExecutionFilter();

		public void run() {
			System.out.println("Hello from a Order managing thread!");
			//       Thread.currentThread().setPriority(Thread.MAX_PRIORITY);



			while(true){


				//Check connection with server every second.
				//Check whether current connection is disconnected. If yes, connect it and skip below action
				if(m_connectionPanel.m_status.getText().toUpperCase().equals("DISCONNECTED"))
				{
					//	m_connectionPanel.onConnect();
					continue;
				}



				m_connectionPanel.m_orderManaging.setText(new Date() + " Live Order: " + liveOrderMap.size() + " Submitted order: " + submittedOrderHashMap.size()  + " orders in " + inputFileName + orderHashMap.size());


				//Looping thru all live orders. If an order has been filled, looking for its orderId. Then try to modify profitaking order. And stop order.;
				forex orderDetail = null;	

				for(Entry<Integer, Order> entry : liveOrderMap.entrySet()){

					//Guy, let's rest 500ms here
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}   


					Order order = entry.getValue();
					orderDetail = null;
					if(order != null)
						orderDetail = orderHashMap.get(order.seqOrderNo()); 	


					if(orderDetail == null){
						orderDetail = liveForexOrderMap.get(order.orderId());
						if(orderDetail == null)
							return;	
					}
					Contract currencyContract = contractMap.get(orderDetail.Symbol);

					//					//If price information isn't available, just skip current order.
					//					if(currencyContract.historical5MBarMap.isEmpty() || currencyContract.getAskPrice() == 0.0 || currencyContract.getBidPrice() == 0.0)
					//						continue;

					//Adjust STop price according to actual open price and current market price.
					adjustStopPrice(entry.getKey(), entry.getValue(), 0.10);


					if(orderDetail != null){

						//Adjust triggere price
						adjustTriggeredPrice(entry.getKey(), entry.getValue(), orderDetail);


						if(orderDetail.PeakGain == null || orderDetail.PeakGain.isEmpty() || Double.parseDouble(orderDetail.PeakGain) < currencyContract.getBidPrice())
							orderDetail.PeakGain = new Double(currencyContract.getBidPrice()).toString();
						else if(orderDetail.MaxDrawdown == null || orderDetail.MaxDrawdown.isEmpty() || Double.parseDouble(orderDetail.MaxDrawdown) > currencyContract.getAskPrice())
							orderDetail.MaxDrawdown = new Double(currencyContract.getAskPrice()).toString();
						orderHashMap.put(order.seqOrderNo(), orderDetail);

					}
				}
			} 		
		}    
	}



	private void adjustStopPrice(Integer orderId, Order order, double Percent){
		forex orderDetail;

		//If current order is parent order, just return
		if(order.tif().equals("GTD"))
			return;


		//	System.out.println("STH happens" + orderAnother.volatility() + orderAnother.volatilityType()  + order.getVolatilityType());
		if(order == null)
			return;

		//If current order is profit taking order, just return
		if(order.orderType().equals("LMT"))
			return;



		order = submittedOrderHashMap.get(orderId);
		if(order == null){
			order = liveOrderMap.get(orderId);
			if(order == null)
				return;
		}

		//For testing purpose.
//		//If its parent Order isn't executed, don't change stop Price.
//		if(!executedOrderMap.containsKey(order.parentId()))
//			return;
//
//		//If its parent Order isn't executed, don't change stop Price.
//		if(liveOrderMap.containsKey(order.parentId()))
//			return;

		Order stopLoss = new Order();
		stopLoss.orderId(order.orderId());
		stopLoss.action(order.action());
		stopLoss.orderType(order.orderType());
		//Stop trigger price
		stopLoss.auxPrice(order.auxPrice());
		stopLoss.totalQuantity(order.totalQuantity());
		stopLoss.parentId(order.parentId());
		//In this case, the low side order will be the last child being sent. Therefore, it needs to set this attribute to true 
		//to activate all its predecessors
		stopLoss.transmit(true);				    				 
		stopLoss.account(m_acctList.get(0));
		stopLoss.tif("GTC");

		order = stopLoss;


		orderDetail = executedOrderMap.get(order.parentId());
		if(orderDetail == null){

			orderDetail = liveForexOrderMap.get(order.orderId());
			if(orderDetail == null)
				return;	
		}
		Double openPrice = 0.0;
		if(orderDetail.ActualPrice != null && !orderDetail.ActualPrice.isEmpty())
			openPrice = Double.parseDouble(orderDetail.ActualPrice);

		Double currentBidPrice, currentAskPrice, newStopPrice = 0.0;
		Contract currencyContract = contractMap.get(orderDetail.Symbol);

		if(currencyContract == null)
			return;

		currentBidPrice = currencyContract.getBidPrice();
		currentAskPrice = currencyContract.getAskPrice();
		double maPrice = currencyContract.ma();
		Action action = order.action();

		//This is a short position, we need to buy it at a price higher than current ask price to stop loss and lower price to make profit
		if(action.equals(Action.BUY)){
			//If current ask price 0.3 % is bigger than actual price, adjust STOP price to current price + 0.1% 
			if(Percent == -0.1){ //which means we need to close the order now.
				newStopPrice = maPrice * (1 + Percent/100);
			}
			else if(openPrice == 0.0){
//				if(	currencyContract.extraMedSma > 0.0 && currencyContract.extraMedSma < maPrice * (1 + Percent/100))
//					newStopPrice = currencyContract.extraMedSma;
//				else
					newStopPrice = maPrice * (1 + Percent/100);
			}
			else if(maPrice < (openPrice * (1 - 0.2/100))){
//				if(	currencyContract.extraMedSma > 0.0 && currencyContract.extraMedSma < maPrice * (1 + Percent/100))
//					newStopPrice = currencyContract.extraMedSma;
//				else						
					newStopPrice = maPrice * (1 + Percent/100);
			}
			//If current ask price 0.2 % is bigger than actual price, adjust STOP price to actual open price 
			else if(maPrice < (openPrice * (1 - 0.15/100))){
//				if(	currencyContract.extraMedSma > 0.0 && currencyContract.extraMedSma < openPrice * (1 - Percent/100))
//					newStopPrice = currencyContract.extraMedSma;
//				else						
					newStopPrice = openPrice * (1 - Percent/100);
			}else{//defaul set stop price as 0.15 loss from current price
//				if(	currencyContract.extraMedSma > 0.0 && currencyContract.extraMedSma < openPrice * (1 + Percent/100))
//					newStopPrice = currencyContract.extraMedSma;
//				else
					newStopPrice = openPrice * (1 + Percent/100);
			}
			newStopPrice = fixDoubleDigi(newStopPrice);

			if(order.auxPrice() == 0.0  && Percent != -0.1) //-0.1 is used to close the order now which means we need to close the order now.
				return;

			if (order.auxPrice() <= newStopPrice  && Percent != -0.1) //-0.1 is used to close the order now which means we need to close the order now.
				return;

			//If current ask price is higher than stop price, we shouldn't set it, otherwise, it will stop out immediately.
			if(currentAskPrice > newStopPrice  && Percent != -0.1) //-0.1 is used to close the order now which means we need to close the order now.
				return;

		}else{//This is a long position, we need to sell it at a price higher than current bid price to make profit and stop at lower price to stop loss

			//If current bid price 0.3 % is higher than actual price, adjust STOP price to current price - 0.1% 
			if(Percent == -0.1){ //-0.1 is used to close the order now which means we need to close the order now.
				newStopPrice = maPrice * (1 - Percent/100);
			}
			else if(openPrice == 0.0){
//				if(	currencyContract.extraMedSma > 0.0 && currencyContract.extraMedSma < openPrice * (1 - Percent/100))
//					newStopPrice = currencyContract.extraMedSma;
//				else						
					newStopPrice = maPrice * (1 - Percent/100);
			}
			else if(maPrice > (openPrice * (1 + 0.2/100))){
//				if(	currencyContract.extraMedSma > 0.0 && currencyContract.extraMedSma > openPrice * (1 - Percent/100))
//					newStopPrice = currencyContract.extraMedSma;
//				else						
					newStopPrice = maPrice * (1 - Percent/100);
			}
			//If current bid price 0.2 % is higher than actual price, adjust STOP price to actual open price 
			else if(maPrice > (openPrice * (1 + 0.15/100))){
//				if(	currencyContract.extraMedSma > 0.0 && currencyContract.extraMedSma > openPrice * (1 + Percent/100))
//					newStopPrice = currencyContract.extraMedSma;
//				else						
					newStopPrice = openPrice * (1 + Percent/100);
			}else{//defaul set stop price as  0.15 loss from current price
//				if(	currencyContract.extraMedSma > 0.0 && currencyContract.extraMedSma > openPrice * (1 - Percent/100))
//					newStopPrice = currencyContract.extraMedSma;
//				else						
					newStopPrice = openPrice * (1 - Percent/100);
			}
			newStopPrice = fixDoubleDigi(newStopPrice);

			if(order.auxPrice() == 0.0 && Percent != -0.1) //-0.1 is used to close the order now which means we need to close the order now.
				return;

			if (order.auxPrice() >= newStopPrice && Percent != -0.1) //-0.1 is used to close the order now which means we need to close the order now.
				return;

			//If current ask price is lower than stop price, we shouldn't set it, otherwise, it will stop out immediately.
			if(currentBidPrice < newStopPrice && Percent != -0.1) //-0.1 is used to close the order now which means we need to close the order now.
				return;
		}	

		System.out.println("SeqNo: " + orderDetail.orderSeqNo + "Sending Mofidied Order: " + order.orderId() + " " + currencyContract.symbol() + currencyContract.currency() + " old STOP: " + order.auxPrice() + " new STOP: " + newStopPrice + " BId@: " + currentBidPrice + " ask@ " + currentAskPrice + " ma: " + maPrice);
		show(new Date() + " SeqNo: " + orderDetail.orderSeqNo + "Sending Mofidied Order: " + order.orderId() + " " + currencyContract.symbol() + currencyContract.currency() + " old STOP: " + order.auxPrice() + " new STOP: " + newStopPrice  + " BId@: " + currentBidPrice + " ask@ " + currentAskPrice + " ma: " + maPrice);
		order.auxPrice(newStopPrice);
		ForexOrderHandler stporderHandler = new ForexOrderHandler(order, orderDetail.orderSeqNo);
		controller().placeOrModifyOrder( currencyContract, order, stporderHandler);	
		//			submittedOrderHashMap.put(order.orderId(), order);

	}





	private void adjustTriggeredPrice(Integer orderId, Order order, forex orderDetail){
		//	forex orderDetail;

		//		System.out.println("STH happens" + orderAnother.volatility() + orderAnother.volatilityType()  + order.getVolatilityType());
		if(order == null)
			return;

		//If current order isnot parent order, just return
		if(order.parentId() != 0)
			return;




		//If current order is profit taking order, just return
		if(order.orderType().equals("STP") == false)
			return;


		String orderDateStr;
		Date orderTime = null;
		try{



			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

			// Get the date today using Calendar object.
			// Using DateFormat format method we can create a string 
			// representation of a date with the defined format.

			orderDateStr= order.goodAfterTime();		   	      
			orderTime  = (Date)formatter.parse(orderDateStr); 

		}catch (Exception e){} 



		Date serverTimePlus1Second = new Date(serverTimeCalendar.getTimeInMillis() + 1 * 1000);	

		//Compare system time and order to make sure that we submit the order on time
		if(serverTimePlus1Second.after(orderTime))
			return;


		order = liveOrderMap.get(orderId);

		if(order == null)
			return;

		Double triggerPrice;
		Double stopLossPrice;

		//This is the buy side.		 
		//		 nextOrderId = 0;
		orderDetail.TradeMethod = order.action().toString();

		ForexPrices orderPrices;
		orderPrices = calTriggerPrice(orderDetail, contractMap.get(orderDetail.Symbol), true);	
		triggerPrice = orderPrices.triggerPrice;
		stopLossPrice = orderPrices.stoprPrice;		 

		double currentBidPrice = contractMap.get(orderDetail.Symbol).getBidPrice();
		double currentAskPrice = contractMap.get(orderDetail.Symbol).getAskPrice();
		double maPrice = contractMap.get(orderDetail.Symbol).ma();

		if(triggerPrice == order.auxPrice())
			return;
		order.auxPrice(fixDoubleDigi(triggerPrice));	 

		Contract currencyContract= contractMap.get(orderDetail.Symbol);
		//			System.out.println("SeqNo: " + orderDetail.orderSeqNo + "Sending upated parent Order: " + order.orderId() + " " + currencyContract.symbol() + currencyContract.currency() + " old STOP: " + order.auxPrice() + " new STOP: " + triggerPrice + " BId@: " + currentBidPrice + " ask@ " + currentAskPrice + " ma: " + maPrice);
		//			show(new Date() + " SeqNo: " + orderDetail.orderSeqNo + "Sending updated parent Order: " + order.orderId() + " " + currencyContract.symbol() + currencyContract.currency() + " old STOP: " + order.auxPrice() + " new STOP: " + triggerPrice  + " BId@: " + currentBidPrice + " ask@ " + currentAskPrice + " ma: " + maPrice);
		ForexOrderHandler stporderHandler = new ForexOrderHandler(order, orderDetail.orderSeqNo);
		controller().placeOrModifyOrder( currencyContract, order, stporderHandler);	
		submittedOrderHashMap.put(order.orderId(), order);


		order = liveOrderMap.get(orderId + 1);
		if(order == null)
			return;

		//Current order's parent Id isn't previous OrderId, something is wrong.
		if (order.parentId() != orderId)
			return;
		//Stop trigger price
		if(stopLossPrice == order.auxPrice())
			return;
		order.auxPrice(fixDoubleDigi(stopLossPrice));
		//		System.out.println("SeqNo: " + orderDetail.orderSeqNo + "Sending upated son STOP Order: " + order.orderId() + " " + currencyContract.symbol() + currencyContract.currency() + " old STOP: " + order.auxPrice() + " new STOP: " + triggerPrice + " BId@: " + currentBidPrice + " ask@ " + currentAskPrice + " ma: " + maPrice);
		//		show(new Date() + " SeqNo: " + orderDetail.orderSeqNo + "Sending updated son STOP Order: " + order.orderId() + " " + currencyContract.symbol() + currencyContract.currency() + " old STOP: " + order.auxPrice() + " new STOP: " + triggerPrice  + " BId@: " + currentBidPrice + " ask@ " + currentAskPrice + " ma: " + maPrice);
		stporderHandler = new ForexOrderHandler(order, orderDetail.orderSeqNo);
		controller().placeOrModifyOrder( currencyContract, order, stporderHandler);	
		submittedOrderHashMap.put(order.orderId(), order);
	}	    






	class ForexOrderHandler implements IOrderHandler{
		Order order2Send;		
		long m_orderSeqNo;
		ForexOrderHandler(Order order, long orderSeqNo){
			order2Send = order;
			m_orderSeqNo = orderSeqNo;
		}



		@Override public void orderState(OrderState orderState) {
			//	
			SwingUtilities.invokeLater( new Runnable() {
				@Override public void run() {
					//				System.out.println("Order: " + order2Send.orderId() + orderState.getStatus());
					//Put actual OrderId into order ArrayList
					forex orderDetail = orderHashMap.get(m_orderSeqNo);
					if(orderDetail == null) return;
					if(orderDetail.OrderStatus != null && !orderDetail.OrderStatus.equals("Filled")){
						orderDetail.OrderStatus = orderState.getStatus();
						orderHashMap.put(m_orderSeqNo, orderDetail);	
					}else if(orderDetail.OrderStatus == null){
						orderDetail.OrderStatus = orderState.getStatus();
						orderHashMap.put(m_orderSeqNo, orderDetail);	
					}


				}
			});
		}
		@Override public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
			//			System.out.println("Order: " + order2Send.orderId() + status + new Double(filled) + new Double(remaining) + avgFillPrice + permId, + parentId + lastFillPrice, + clientId +  whyHeld);
			//Put actual OrderId into order ArrayList



			forex orderDetail = orderHashMap.get(m_orderSeqNo);
			if(orderDetail == null) 
				return;

			if(status.equals(OrderStatus.Filled)){

				//Cancel remain order with same symbol but not STOP order.

				//Loop thru all live order and cancel it if in same groupId;
				for (Entry<Integer, forex> entry : liveForexOrderMap.entrySet()) {
					forex order2Loop = entry.getValue();

					if(order2Loop.Symbol.equals(orderDetail.Symbol) && order2Send.tif().equals("GTD")){
						{
							controller().cancelOrder(order2Send.orderId());
							System.out.println(new Date() + "No need double order so cancel Order:  " + order2Send.orderId());				    		
						}
					}    
				}



				orderDetail.ActualPrice = new Double(avgFillPrice).toString();
				//			orderDetail.Symbol = contract.symbol() + contract.currency();
				orderDetail.OrderID = new Integer(order2Send.orderId()).toString();
				//Put executed order into map;
				executedOrderMap.put(order2Send.orderId(), orderDetail);
			}


			if(orderDetail.OrderStatus == null || orderDetail.OrderStatus.isEmpty() || !orderDetail.OrderStatus.equals("Filled"))
				orderDetail.OrderStatus = status.toString();

			if(status.equals(OrderStatus.Filled)){
				orderDetail.ActualPrice = new Double(avgFillPrice).toString();
				ApiDemo.INSTANCE.controller().removeOrderHandler( this);
			}
			orderHashMap.put(m_orderSeqNo, orderDetail);	
		}
		@Override public void handle(int errorCode, final String errorMsg) {
			//	    								parent.orderId( 0);
			//			ApiDemo.INSTANCE.controller().removeOrderHandler( this);
			forex orderDetail = orderHashMap.get(m_orderSeqNo);

			if(orderDetail == null)
				return;
			orderDetail.comment = "Error code: " + errorCode + errorMsg;

			//Error code 202 means it is cancelled in system. But sometimes it is not updated in orders status report.
			if(errorCode >= 200 && errorCode <= 203){
				if(orderDetail.OrderStatus == null || !orderDetail.OrderStatus.equals("Filled"))
					orderDetail.OrderStatus = "Cancelled";
			}

			orderHashMap.put(m_orderSeqNo, orderDetail);
			SwingUtilities.invokeLater( new Runnable() {
				@Override public void run() {
					//				System.out.println("Order: " + order2Send.orderId() + errorMsg);
				}
			});
		}
	}



	class reportListener implements ITradeReportHandler{
		@Override
		public void tradeReport(String tradeKey, Contract contract, Execution execution) {
			// TODO Auto-generated method stub

			liveOrderMap.remove(execution.orderId());
			liveForexOrderMap.remove(execution.orderId());


			//Put all executed order into map for later tracking.
			forex orderDetail = new forex();			
			orderDetail.ActualPrice = new Double(execution.avgPrice()).toString();
			orderDetail.Symbol = contract.symbol() + contract.currency();
			orderDetail.OrderID = new Integer(execution.orderId()).toString();
			//Put executed order into map;
			executedOrderMap.put(execution.orderId(), orderDetail);

			Order order = submittedOrderHashMap.get(execution.orderId());
			if(order != null){



				Long groupId = null;
				groupId = order.groupId();

				if(groupId != 0)
				{


					//Cancel remain order in same group and maybe remove all oca orders.

					//Loop thru all live order and cancel it if in same groupId;
					for (HashMap.Entry<Integer, Order> entry : submittedOrderHashMap.entrySet()) {
						Order order2Loop = entry.getValue();

						if(order2Loop.groupId() == groupId && order2Loop.parentId() == 0){
							{
								controller().cancelOrder(order2Loop.orderId());
								System.out.println(new Date() + " canceld Order:  " + order2Loop.orderId());				    		
							}
						}    
					}



				}

				for (HashMap.Entry<Long, forex> entry : orderHashMap.entrySet()) {
					Long key = entry.getKey();
					orderDetail = entry.getValue();

					if(orderDetail.orderIdList.contains(order.orderId())){
						if(orderDetail.OrderStatus.equals("Filled")){
							//    		System.out.print(" cost@ " + orderDetail.ActualPrice);				    		
						}
					}

					//If one of the oca order is filled, removed all oca order.
					if(groupId == orderDetail.groupId){
						orderDetail.OrderStatus = "Filled";
						orderHashMap.put(key, orderDetail);
					}
				}		



				long seqNo = order.seqOrderNo();

				orderDetail = orderHashMap.get(seqNo);
				if(orderDetail != null)		{			    
					if(orderDetail.orderIdList.contains(execution.orderId())){			    
						orderDetail.OrderStatus = "Filled";			

						if(orderDetail.OpenTime == null || orderDetail.OpenTime.isEmpty()){
							orderDetail.OpenTime = execution.time();
							orderDetail.ActualPrice = Double.toString(execution.avgPrice());
						}
						else{
							orderDetail.CloseTime = execution.time();
							orderDetail.ClosedPrice = Double.toString(execution.avgPrice());

						}
						orderHashMap.put(seqNo, orderDetail);			
					}    
				}
			}
		}
		@Override
		public void tradeReportEnd() {
			// TODO Auto-generated method stub
			totalCommissionPaid = bufferCommissionPaid;
			totalProfitNLoss = bufferProfitNLoss;
			bufferCommissionPaid = 0.0;
			bufferProfitNLoss = 0.0;
		}
		@Override
		public void commissionReport(String tradeKey, CommissionReport commissionReport) {
			// TODO Auto-generated method stub
			//			System.out.println("CommissionReport. ["+commissionReport.m_execId+"] - ["+commissionReport.m_commission+"] ["+commissionReport.m_currency+"] RPNL ["+commissionReport.m_realizedPNL+"]");
			bufferCommissionPaid += commissionReport.m_commission;

			bufferProfitNLoss += commissionReport.m_realizedPNL;

			baseCurrency = commissionReport.m_currency;
		}
	}


	class ForexLiveOrderHandler implements ILiveOrderHandler {

		@Override
		public void openOrder(Contract contract, Order order, OrderState orderState) {
			// TODO Auto-generated method stub
			//  		System.out.println(new Date() + " Live order Current size: " + liveOrderMap.size());


			Order originalOrder = submittedOrderHashMap.get(order.orderId());
			if(originalOrder != null){
				originalOrder.auxPrice(order.auxPrice());
				originalOrder.lmtPrice(order.lmtPrice());
				liveOrderMap.put(new Integer(order.orderId()), originalOrder);
			} 
			else
				liveOrderMap.put(new Integer(order.orderId()), order);

			forex orderDetail = new forex();
			orderDetail.Symbol = contract.symbol() + contract.currency();
			orderDetail.TradeMethod = order.action().toString();
			liveForexOrderMap.put(order.orderId(), orderDetail);


			if(orderState.status().equals("Cancelled")){
				System.out.print(contract.symbol() + contract.currency() + " " + order.orderId() +  " " +orderState.getStatus());
				System.out.println("Hey, get cancelled here");
				liveOrderMap.remove(order.orderId());
				liveForexOrderMap.remove(order.orderId());
			}

			Long groupId = null;
			if(orderState.status().equals("Filled"))
			{
				System.out.println(contract.symbol() + contract.currency() + " " + order.orderId() +  " " +orderState.getStatus());
				liveOrderMap.remove(order.orderId());	
				liveForexOrderMap.remove(order.orderId());

				//Cancel remain order in same group and maybe remove all oca orders.
				groupId = submittedOrderHashMap.get(order.orderId()).groupId();

				//Loop thru all live order and cancel it if in same groupId;
				for (HashMap.Entry<Integer, Order> entry : submittedOrderHashMap.entrySet()) {
					Order order2Loop = entry.getValue();

					if(order2Loop.groupId() == groupId){
						{
							controller().cancelOrder(order2Loop.orderId());
							System.out.println(new Date() + " canceld Order:  " + order2Loop.orderId());				    		
						}
					}    
				}



			}

			for (HashMap.Entry<Long, forex> entry : orderHashMap.entrySet()) {
				Long key = entry.getKey();
				orderDetail = entry.getValue();
				if(orderDetail == null)
					continue;
				//		    if(orderDetail.OrderStatus)
				if(orderDetail.orderIdList.contains(order.orderId())){
					//		    	if(orderDetail.OrderStatus.equals("Filled"))
					{
						//		    		System.out.print(" cost@ " + orderDetail.ActualPrice);				    		
					}
				}

				//If one of the oca order is filled, removed all oca order. We should review this later.
				if(groupId == orderDetail.groupId)
					orderHashMap.remove(key);
			}	
		}

		@Override
		public void openOrderEnd() {
			// TODO Auto-generated method stub

		}

		@Override
		public void orderStatus(int orderId, OrderStatus status, double filled, double remaining, double avgFillPrice,
				long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
			// TODO Auto-generated method stub

			if(status.equals(OrderStatus.Cancelled)){
				liveOrderMap.remove(orderId);
				liveForexOrderMap.remove(orderId);
			}


			Order order = submittedOrderHashMap.get(orderId);
			forex orderDetail = null;
			if(order != null)
				orderDetail = orderHashMap.get(order.seqOrderNo());
			if(orderDetail != null)
			{
				if(orderDetail.orderIdList.contains(orderId)){
					if(orderDetail.OrderStatus == null || orderDetail.OrderStatus.isEmpty() || !orderDetail.OrderStatus.equals("Filled")){
						orderDetail.OrderStatus = status.toString();
						if(status.equals(OrderStatus.Filled)){
						}
					}
					if(status.equals(OrderStatus.Filled)){
						orderDetail.ActualPrice = new Double(avgFillPrice).toString();
						orderDetail.OrderID = new Integer(orderId).toString();
					}
					orderHashMap.put(orderDetail.orderSeqNo, orderDetail);		
				}
			}
		}

		@Override
		public void handle(int orderId, int errorCode, String errorMsg) {
			// TODO Auto-generated method stub
			for(Entry<Long, forex> entry : orderHashMap.entrySet()){

				if(entry.getValue().orderIdList.contains(orderId)){
					entry.getValue().comment = "Error code: " + errorCode + errorMsg;

					//Error code 202 means it is cancelled in system. But sometimes it is updated in live orders.
					if(errorCode >= 200 && errorCode <= 203)
						entry.getValue().OrderStatus = "Cancelled";
					orderHashMap.put(entry.getKey(), entry.getValue());						
				}	
			}
		}

	}


	ForexLiveOrderHandler liveOrderListener = new ForexLiveOrderHandler();
	reportListener tradeReportListener = new reportListener();	

	


	public void requestHistoricalBar(String endTime, Contract currencyContract, Boolean isFirstTime){
		int length = 0;

		//Request one month day in initial request, then just one hour in following request.
		DurationUnit durationToRequest;
		if(isFirstTime){
			durationToRequest = DurationUnit.MONTH;
			length = 1;
		}
		else{
			durationToRequest = DurationUnit.SECOND;
			length = 14400;			
		}

		histortyDataHandler forexHistoricalHandler = new histortyDataHandler(currencyContract, 5, contractMap);
		if(forexHistoricalHandler != null && currencyContract != null)
			controller().reqHistoricalData(currencyContract, endTime, length, durationToRequest, BarSize._5_mins, WhatToShow.MIDPOINT, true, forexHistoricalHandler);
		else
		{
			System.out.println("Null pointer here, Please check your order" + currencyContract + forexHistoricalHandler);
			show(new Date() + "Null pointer here, Please check your order" + currencyContract + forexHistoricalHandler);
		}

		//		forexHistoricalHandler = new histortyDataHandler(currencyContract, 240);
		//		if(forexHistoricalHandler != null && currencyContract != null)
		//			controller().reqHistoricalData(currencyContract, endTime, length, durationToRequest, BarSize._4_hours, WhatToShow.MIDPOINT, true, forexHistoricalHandler);
		//		else
		//		{
		//			System.out.println("Null pointer here, Please check your order" + currencyContract + forexHistoricalHandler);
		//			show(new Date() + "Null pointer here, Please check your order" + currencyContract + forexHistoricalHandler);
		//		}

		//		forexHistoricalHandler = new histortyDataHandler(currencyContract, 60);
		//		if(forexHistoricalHandler != null && currencyContract != null)
		//			controller().reqHistoricalData(currencyContract, endTime, length, durationToRequest, BarSize._1_hour, WhatToShow.MIDPOINT, true, forexHistoricalHandler);
		//		else
		//		{
		//			System.out.println("Null pointer here, Please check your order" + currencyContract + forexHistoricalHandler);
		//			show(new Date() + "Null pointer here, Please check your order" + currencyContract + forexHistoricalHandler);
		//		}

	}

	public void placeMarketOrder(forex orderDetail){		

		{
			ApiDemo.INSTANCE.controller().client().reqIds(-1);

			if(nextOrderId < ApiDemo.INSTANCE.controller().availableId())
				nextOrderId = ApiDemo.INSTANCE.controller().availableId();

			if(currentMaxOrderId >= nextOrderId)
				nextOrderId = currentMaxOrderId + 1;


			//BRACKET ORDER
			//! [bracketsubmit]
			Double triggerPrice;
			Double profitTakingPrice;
			Double stopLossPrice;

			Double quantity = Double.parseDouble(orderDetail.Quantity);

			ForexPrices orderPrices;
			orderPrices = calTriggerPrice(orderDetail, contractMap.get(orderDetail.Symbol), false);	
			triggerPrice = orderPrices.triggerPrice;
			profitTakingPrice = orderPrices.profitPrice;
			stopLossPrice = orderPrices.stoprPrice;				

			//			 nextOrderId = 0;
			int parentOrderId = nextOrderId;
			String action = orderDetail.TradeMethod;
			//This will be our main or "parent" order
			Order parent = new Order();
			parent.orderId(parentOrderId);
			parent.parentId(0);
			parent.action(action);
			parent.orderType("MKT");
			parent.totalQuantity(quantity);
			parent.tif("GTC");
			//The parent and children orders will need this attribute set to false to prevent accidental executions.
			//The LAST CHILD will have it set to true.
			parent.transmit(false);				    					
			parent.account(m_acctList.get(0));				


			orderTransmit(contractMap.get(orderDetail.Symbol), parent, orderDetail.orderSeqNo);


			nextOrderId++;
			//This is Loss STOPing  order. Its order is is parent Id plus two. And its parent ID is above parent Id;
			Order stopLoss = new Order();
			stopLoss.orderId(parent.orderId() + 2);
			stopLoss.action(action.equals("BUY") ? "SELL" : "BUY");
			stopLoss.orderType("STP");
			//Stop trigger price
			stopLoss.auxPrice(fixDoubleDigi(stopLossPrice));
			stopLoss.totalQuantity(quantity);
			stopLoss.parentId(parent.orderId());
			//In this case, the low side order will be the last child being sent. Therefore, it needs to set this attribute to true 
			//to activate all its predecessors
			stopLoss.transmit(true);				    				 
			stopLoss.account(m_acctList.get(0));
			stopLoss.tif("GTC");

			orderTransmit(contractMap.get(orderDetail.Symbol), stopLoss, orderDetail.orderSeqNo);



		}


	}


	private void requestRealtimeBar(){		

		/*
	    ApiDemo.INSTANCE.controller().reqRealTimeBars(m_contract_USDCNH, WhatToShow.MIDPOINT, false, m_stockListener_USDCNH);
		 */

	}


	//TODO Auto-generated catch block
	class MarketDataManagingThread extends Thread {
		double lmtPrice = 0.0;
		int counter = 0;
		ExecutionFilter exeFilterReport = new ExecutionFilter();
		//		private TechinicalAnalyzerTrader techAnalyzer60M;


		public void run() {
			System.out.println("Hello from a market data managing thread!");
			//       Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

			Date orderTime = new Date();	
			forex orderDetail;
			DateFormat formatter; 
			String orderDateStr;
			ConcurrentHashMap<Long, forex> savedOrderHashMap = new ConcurrentHashMap<Long, forex>(orderHashMap);



			// creating reverse set for order
			TreeSet<Long> treeadd = new TreeSet<Long>(orderHashMap.keySet());
			TreeSet<Long> treereverse=(TreeSet<Long>)treeadd.descendingSet();

			// create descending set
			Iterator<Long> iterator;
			iterator = treeadd.iterator();
			orderDetail =   orderHashMap.get(iterator);

			while(true){
				//Guy, let's rest 1000ms here
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				//Check connection with server every second.
				//Check whether current connection is disconnected. If yes, connect it and skip below action
				if(m_connectionPanel.m_status.getText().toUpperCase().equals("DISCONNECTED"))
				{
					m_connectionPanel.onConnect();
					continue;
				}

				m_connectionPanel.m_marketDataManaging.setText(new Date() + " orders executed today: " + executedOrderMap.size() + " Commission@: " + totalCommissionPaid + " " + baseCurrency);    	

				//Request order Id.		
				ApiDemo.INSTANCE.controller().client().reqIds(-1);
				nextOrderId = ApiDemo.INSTANCE.controller().availableId();

				fileReadingCounter++;
				//Read back all order information first.
				try 			    	    
				{	
					if(fileReadingCounter > 300){

						String[] fileNameStrs = inputFileName.split("\\.");	    


						/**/ 
						formatter = new SimpleDateFormat("yyyyMMdd");

						// Get the date today using Calendar object.
						// Using DateFormat format method we can create a string 
						// representation of a date with the defined format.

						orderDateStr = formatter.format(new Date());

						//Chceck whether a data-specific file exist or not. If exist, use it instead.
						File f = new File("Forex" + orderDateStr + ".xls");
						if(f.exists() && !f.isDirectory()) { 
							// do something
							inputFileName = "Forex" + orderDateStr + ".xls";
						}


						if(savedOrderHashMap.equals(orderHashMap) == false){
							excelOutput.setOutputFile(fileNameStrs[0] + "_" + "Report_" + orderDateStr + "." + fileNameStrs[1]);
							excelOutput.write(orderHashMap);
							show(new Date() + " File " + fileNameStrs[0] + "_" + orderDateStr + "." + fileNameStrs[1] + " write back.");	
							savedOrderHashMap = orderHashMap;
						}

						excelInput.setInputFile(inputFileName);
						orderHashMap = excelInput.read(orderHashMap);	
						show(new Date() + " File " + inputFileName + " is read back. Total size in HashMap: " + orderHashMap.size() + " orders.");
						fileReadingCounter = 0;

					}

				} 
				catch (Exception e){

					e.printStackTrace();
				}	


				//Request real time tick price data if it isn't available.
				if(((m_contract_GBPJPY.getAskPrice() == 0 || m_contract_GBPJPY.getBidPrice() == 0 || m_contract_EURCNH.getAskPrice() == 0 || m_contract_EURCNH.getBidPrice() == 0)))
				{
					requestTickData(orderHashMap);  	
				}


				//Request historical data every 30 seconds.
				if(fileReadingCounter % 30 == 0){ 					

					formatter = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

					// Get the date today using Calendar object.
					// Using DateFormat format method we can create a string 
					// representation of a date with the defined format.

					orderDateStr = formatter.format(new Date());

					// displaying the Tree set data
					//			   System.out.println("Tree set data in reverse order: ");     
					if (iterator.hasNext()){

						//				   System.out.println(iterator.next() + " ");
						do {
							Long key = (Long) iterator.next();
							orderDetail = orderHashMap.get(key);
							if(orderDetail == null )
								continue;
							formatter = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

							// Get the date today using Calendar object.
							// Using DateFormat format method we can create a string 
							// representation of a date with the defined format.
							try{
								orderDateStr= orderDetail.Date + " " + orderDetail.Time;		   	      
								orderTime  = (Date)formatter.parse(orderDateStr); 

							}catch (Exception e){} 

							//Compare system time and order to make sure that we submit the order on time


							if(historicalDataReq.contains(orderDetail.Symbol))
								continue;
						}while((/*orderDetail.ValidDuration.equals("60") || */orderTime.before(new Date())) && iterator.hasNext() );
						if(orderDetail != null ){
							historicalDataReq.add(orderDetail.Symbol);
							orderDateStr = formatter.format(new Date());
							requestHistoricalBar(orderDateStr, contractMap.get(orderDetail.Symbol), contractMap.get(orderDetail.Symbol).isHistoryReqFirstTime);



							TechinicalAnalyzerTrader techAnalyzer60M;
							//after we starts Historical bar request. Let's analyze it.
							if(contractTechAnalyzerMap.containsKey(orderDetail.Symbol) == false){
								techAnalyzer60M = new TechinicalAnalyzerTrader(ApiDemo.INSTANCE, contractMap.get(orderDetail.Symbol),contractMap, orderHashMap);
								contractTechAnalyzerMap.put(orderDetail.Symbol, techAnalyzer60M);
								techAnalyzer60M.start();
								//								techAnalyzer60M.setPriority(Thread.NORM_PRIORITY +3);      

								//								TechinicalAnalyzer techAnalyzer15M = new TechinicalAnalyzer(ApiDemo.INSTANCE, contractMap.get(orderDetail.Symbol),contractMap, orderHashMap, 15);
								////								contractTechAnalyzerMap.put(orderDetail.Symbol, techAnalyzer60M);
								//								techAnalyzer15M.start();
								//								techAnalyzer15M.setPriority(Thread.NORM_PRIORITY + 2);      
								//
								//								TechinicalAnalyzer techAnalyzer5M = new TechinicalAnalyzer(ApiDemo.INSTANCE, contractMap.get(orderDetail.Symbol),contractMap, orderHashMap, 5);
								//								contractTechAnalyzerMap.put(orderDetail.Symbol, techAnalyzer5M);
								//								techAnalyzer5M.start();
								//								techAnalyzer5M.setPriority(Thread.NORM_PRIORITY + 1);      

							}


						}
					}else{
						//If it is end of list, let's start it again. 
						treeadd = new TreeSet<Long>(orderHashMap.keySet());
						treereverse=(TreeSet<Long>)treeadd.descendingSet();
						iterator = treereverse.iterator(); 		
						historicalDataReq.clear();     
					}


				}


				//Register for trade report
				controller().reqExecutions( exeFilterReport, tradeReportListener);

				//Register for live order report;
				controller().reqLiveOrders(liveOrderListener); 		 

			} 		
		}    

	}     
}


// do clearing support
// change from "New" to something else
// more dn work, e.g. deltaNeutralValidation
// add a "newAPI" signature
// probably should not send F..A position updates to listeners, at least not to API; also probably not send FX positions; or maybe we should support that?; filter out models or include it 
// finish or remove strat panel
// check all ps
// must allow normal summary and ledger at the same time
// you could create an enum for normal account events and pass segment as a separate field
// check pacing violation
// newticktype should break into price, size, and string?
// give "already subscribed" message if appropriate

// BUGS
// When API sends multiple snapshot requests, TWS sends error "Snapshot exceeds 100/sec" even when it doesn't
// When API requests SSF contracts, TWS sends both dividend protected and non-dividend protected contracts. They are indistinguishable except for having different conids.
// Fundamentals financial summary works from TWS but not from API 
// When requesting fundamental data for IBM, the data is returned but also an error
// The "Request option computation" method seems to have no effect; no data is ever returned
// When an order is submitted with the "End time" field, it seems to be ignored; it is not submitted but also no error is returned to API.
// Most error messages from TWS contain the class name where the error occurred which gets garbled to gibberish during obfuscation; the class names should be removed from the error message 
// If you exercise option from API after 4:30, TWS pops up a message; TWS should never popup a message due to an API request
// TWS did not desubscribe option vol computation after API disconnect
// Several error message are misleading or completely wrong, such as when upperRange field is < lowerRange
// Submit a child stop with no stop price; you get no error, no rejection
// When a child order is transmitted with a different contract than the parent but no hedge params it sort of works but not really, e.g. contract does not display at TWS, but order is working
// Switch between frozen and real-time quotes is broken; e.g.: request frozen quotes, then realtime, then request option market data; you don't get bid/ask; request frozen, then an option; you don't get anything
// TWS pops up mkt data warning message in response to api order

// API/TWS Changes
// we should add underConid for sec def request sent API to TWS so option chains can be requested properly
// reqContractDetails needs primary exchange, currently only takes currency which is wrong; all requests taking Contract should be updated
// reqMktDepth and reqContractDetails does not take primary exchange but it needs to; ideally we could also pass underConid in request
// scanner results should return primary exchange
// the check margin does not give nearly as much info as in TWS
// need clear way to distinguish between order reject and warning

// API Improvements
// add logging support
// we need to add dividendProt field to contract description
// smart live orders should be getting primary exchange sent down

// TWS changes
// TWS sends acct update time after every value; not necessary
// support stop price for trailing stop order (currently only for trailing stop-limit)
// let TWS come with 127.0.0.1 enabled, same is IBG
// we should default to auto-updating client zero with new trades and open orders

// NOTES TO USERS
// you can get all orders and trades by setting "master id" in the TWS API config
// reqManagedAccts() is not needed because managed accounts are sent down on login
// TickType.LAST_TIME comes for all top mkt data requests
// all option ticker requests trigger option model calc and response
// DEV: All Box layouts have max size same as pref size; but center border layout ignores this
// DEV: Box layout grows items proportionally to the difference between max and pref sizes, and never more than max size

//TWS sends error "Snapshot exceeds 100/sec" even when it doesn't; maybe try flush? or maybe send 100 then pause 1 second? this will take forever; i think the limit needs to be increased

//req open orders can only be done by client 0 it seems; give a message
//somehow group is defaulting to EqualQuantity if not set; seems wrong
//i frequently see order canceled - reason: with no text
//Missing or invalid NonGuaranteed value. error should be split into two messages
//Rejected API order is downloaded as Inactive open order; rejected orders should never be sen
//Submitting an initial stop price for trailing stop orders is supported only for trailing stop-limit orders; should be supported for plain trailing stop orders as well 
//EMsgReqOptCalcPrice probably doesn't work since mkt data code was re-enabled
//barrier price for trail stop lmt orders why not for trail stop too?
//All API orders default to "All" for F; that's not good
