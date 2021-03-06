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
	public final Contract m_contract_GBPSGD = new Contract("GBP", "CASH", "IDEALPRO", "SGD", 3.0, 0.3);
	public final Contract m_contract_USDSGD = new Contract("USD", "CASH", "IDEALPRO", "SGD", 3.0, 0.3);
	public final Contract m_contract_AUDCAD = new Contract("AUD", "CASH", "IDEALPRO", "CAD", 3.0, 0.3);	
	public final Contract m_contract_EURAUD = new Contract("EUR", "CASH", "IDEALPRO", "AUD", 3.0, 0.3);
	public final Contract m_contract_CHFAUD = new Contract("CHF", "CASH", "IDEALPRO", "AUD", 3.0, 0.3);
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
	ForexListner m_stockListener_GBPSGD = new ForexListner(m_contract_GBPSGD);	
	ForexListner m_stockListener_USDSGD = new ForexListner(m_contract_USDSGD);		
	ForexListner m_stockListener_AUDCAD = new ForexListner(m_contract_AUDCAD);			
	ForexListner m_stockListener_EURAUD = new ForexListner(m_contract_EURAUD);	
	ForexListner m_stockListener_CHFAUD = new ForexListner(m_contract_CHFAUD);		
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
                	System.out.println(m_contract_listener.symbol() + "." + m_contract_listener.currency() + " BID: " + price);
                	show(m_contract_listener.symbol() + "." + m_contract_listener.currency() + " BID: " + price);                    
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
                	System.out.println(m_contract_listener.symbol() + "." + m_contract_listener.currency() + " ASK: " + price);
                	show(m_contract_listener.symbol() + "." + m_contract_listener.currency() + " ASK: " + price);       
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
    	contractMap.put("GBPSGD", m_contract_GBPSGD);
    	contractMap.put("USDSGD", m_contract_USDSGD);
    	contractMap.put("AUDCAD", m_contract_AUDCAD);
    	contractMap.put("EURAUD", m_contract_EURAUD);
    	contractMap.put("CHFAUD", m_contract_CHFAUD);
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
    	forexListenerHashMap.put("GBPSGD", m_stockListener_GBPSGD);
    	forexListenerHashMap.put("USDSGD", m_stockListener_USDSGD);
    	forexListenerHashMap.put("AUDCAD", m_stockListener_AUDCAD);
    	forexListenerHashMap.put("EURAUD", m_stockListener_EURAUD);
    	forexListenerHashMap.put("CHFAUD", m_stockListener_CHFAUD);
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

		 
		 
    }
	
	 
    ITimeHandler timeHandler = new ITimeHandler() {


		@Override public void currentTime(long time) {
		//	show( "Server date/time is " + Formats.fmtDate(time * 1000) );
			
//    	DateFormat formatterDate, formatterTime; 			    	      
//		
//    	formatterDate = new SimpleDateFormat("yy-mm-dd");
//    	formatterTime = new SimpleDateFormat("HH:mm:ss");			
//			
//			try {
////				Runtime.getRuntime().exec("cmd /C date " + formatterDate.format(time * 1000));//date // dd-MM-yy
////				Runtime.getRuntime().exec("cmd /C time " + formatterTime.format(time * 1000)); // hh:mm:ss
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} 

		}
	};
	 
	@Override public void connected() {
		
		//show( "connected");
	
		m_connectionPanel.m_status.setText( "connected");
		if(m_contract_NZDUSD.getAskPrice() > 0 && m_contract_NZDUSD.getBidPrice() > 0 && m_contract_AUDUSD.getAskPrice() > 0 && m_contract_AUDUSD.getBidPrice() > 0){
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
	
	
	private ForexPrices calTriggerPrice(forex orderDetail, Contract currentContract){
		Integer duration = Integer.parseInt(orderDetail.ValidDuration);
		double triggerPrice;
		
		//if duration is longer than 30 minutes, take 3 bars.
		//if duration is beblow 5 minutes. take 1 bar.
		//If duration is bewtween 5 and 30, takes 2 bars.
		TreeSet<String> keys = new TreeSet<String>(currentContract.historicalBarMap.keySet());
		TreeSet<String> treereverse = (TreeSet<String>) keys.descendingSet();
		Iterator<String> iterator =  treereverse.iterator();
		Integer counter = 0;
		Double high = 0.0, low = Double.MAX_VALUE;
		
		
		//If historical data not available yet, use real time tick price.
		if((!iterator.hasNext())){
			
			if(orderDetail.TradeMethod.equals("SELL")){
				  triggerPrice = contractMap.get(orderDetail.Symbol).getBidPrice() * (1 - Double.parseDouble(orderDetail.TriggerPct) / 100);
					 //Let's set profit taking to 0.6% and adjust it later in order managing task.
							  
				}else 
				{
					 triggerPrice = contractMap.get(orderDetail.Symbol).getAskPrice() * (1 + Double.parseDouble(orderDetail.TriggerPct) / 100);
					 //Let's set profit taking to 0.6% and adjust it later in order managing task.
				}
			
			
		}else if(duration <= 60){ //If duration is less or euqual to 60 seconds. use 5 minutes bar high and low.
			if(orderDetail.TradeMethod.equals("SELL")){
				triggerPrice = currentContract.historicalBarMap.get(treereverse.first()).low() * (1 - Double.parseDouble(orderDetail.TriggerPct) / 100);
				}else {
					triggerPrice = currentContract.historicalBarMap.get(treereverse.first()).high() * (1 + Double.parseDouble(orderDetail.TriggerPct) / 100);
				}			
		}else if(duration <= 5 * 60){//If duration is 5 minutes, let's use last 10 minutes's high and low.
			while(iterator.hasNext() && counter < 2){
				Bar bar = currentContract.historicalBarMap.get(iterator.next());
				if(high < bar.high())
					high = bar.high();
				if(low > bar.low())
					low = bar.low();
			}
			
			if(orderDetail.TradeMethod.equals("SELL")){
				triggerPrice = low * (1 - Double.parseDouble(orderDetail.TriggerPct) / 100);
				}else 
				{
					triggerPrice = high * (1 + Double.parseDouble(orderDetail.TriggerPct) / 100);
				}
		}else if(duration <= 15 * 60){//If duration is 15 minutes, let's use last 30 minutes's high and low.
			while(iterator.hasNext() && counter < 2){
				Bar bar = currentContract.historicalBarMap.get(iterator.next());
				if(high < bar.high())
					high = bar.high();
				if(low > bar.low())
					low = bar.low();
			}
			
			if(orderDetail.TradeMethod.equals("SELL")){
				triggerPrice = low * (1 - Double.parseDouble(orderDetail.TriggerPct) / 100);
				}else 
				{
					triggerPrice = high * (1 + Double.parseDouble(orderDetail.TriggerPct) / 100);
				}
		}
		else if(duration <= 30 * 60){//If duration is 30 minutes, let's use last 60 minutes's high and low.

			while(iterator.hasNext() && counter < 2){
				Bar bar = currentContract.historicalBarMap.get(iterator.next());
				if(high < bar.high())
					high = bar.high();
				if(low > bar.low())
					low = bar.low();
			}
			
			if(orderDetail.TradeMethod.equals("SELL")){
				triggerPrice = low * (1 - Double.parseDouble(orderDetail.TriggerPct) / 100);
				}else 
				{
					triggerPrice = high * (1 + Double.parseDouble(orderDetail.TriggerPct) / 100);
				}
			
		}else{//If duration is longer than 30 minutes, let's use last 90 minutes's high and low.
			while(iterator.hasNext() && counter < 2){
				Bar bar = currentContract.historicalBarMap.get(iterator.next());
				if(high < bar.high())
					high = bar.high();
				if(low > bar.low())
					low = bar.low();
			}
			
			if(orderDetail.TradeMethod.equals("SELL")){
				triggerPrice = low * (1 - Double.parseDouble(orderDetail.TriggerPct) / 100);	
				}else 
				{
					triggerPrice = high * (1 + Double.parseDouble(orderDetail.TriggerPct) / 100);
				}
			
			
		
		}
			
		ForexPrices orderPrices = new ForexPrices(); 
		
		//Make sure that trigger price is 0.1 away from current bid/ask price.
		if(orderDetail.TradeMethod.equals("SELL")){
			Double currentLowPrice = contractMap.get(orderDetail.Symbol).getBidPrice() * (1 - 0.1 / 100); 
			if(triggerPrice > currentLowPrice)
				triggerPrice = currentLowPrice;
				 //Let's set profit taking to 0.6% and adjust it later in order managing task.
			orderPrices.triggerPrice = triggerPrice;	
			orderPrices.profitPrice = triggerPrice * (1 -  1.0 / 100);
			 // profitTakingPrice = triggerPrice * (1 - Double.parseDouble(orderDetail.ProfitPct) / 100);
			orderPrices.stoprPrice = triggerPrice * (1 + (Double.parseDouble(orderDetail.LossPct)) / 100);
			}
		else 
			{
			Double currentHighPrice = contractMap.get(orderDetail.Symbol).getAskPrice() * (1 + 0.1 / 100); 
			if(triggerPrice < currentHighPrice)
				 triggerPrice = currentHighPrice;
				 //Let's set profit taking to 0.6% and adjust it later in order managing task.
			
			orderPrices.triggerPrice = triggerPrice;	
			orderPrices.profitPrice = triggerPrice * (1 + 1.0 / 100);
				// profitTakingPrice = triggerPrice * (1 + Double.parseDouble(orderDetail.ProfitPct) / 100);
			orderPrices.stoprPrice = triggerPrice * (1 - (Double.parseDouble(orderDetail.LossPct))  / 100);	
			
			}
		
		
		return orderPrices;
		
	}
	
	
	private Order bracketStopOrder(forex orderDetail, int orderID){
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
		 	 orderPrices = calTriggerPrice(orderDetail, contractMap.get(orderDetail.Symbol));	
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
			   	parent.goodAfterTime(orderDateStr);  
		   	    try {
					Date  orderTime  = (Date)formatter.parse(orderDateStr);
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
				

				//This is profit taking order. Its order is is parent Id plus one. And its parent ID is above parent Id;
				nextOrderId++;
				Order takeProfit = new Order();
				takeProfit.orderId(parent.orderId() + 1);
				takeProfit.action(action.equals("BUY") ? "SELL" : "BUY");
				takeProfit.orderType("LMT");
				takeProfit.totalQuantity(quantity);
				takeProfit.lmtPrice(fixDoubleDigi(profitTakingPrice));
				takeProfit.parentId(parent.orderId());
				takeProfit.transmit(false);				    					
				takeProfit.account(m_acctList.get(0));
				takeProfit.tif("GTC");
				
				orderTransmit(contractMap.get(orderDetail.Symbol), takeProfit, orderDetail.orderSeqNo);
				
				

				
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
		 	 orderPrices = calTriggerPrice(orderDetail, contractMap.get(orderDetail.Symbol));	
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
	   	  
	   	     //Aaron only valid after specified time
	   	     parent.goodAfterTime(orderDateStr);	   	     
	   	    try {
				Date  orderTime  = (Date)formatter.parse(orderDateStr);
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
			
			
					
			
			
			nextOrderId++;	    									
			
			//This is profit taking order. Its order is is parent Id plus one. And its parent ID is above parent Id;
			Order takeProfit = new Order();
			takeProfit.orderId(nextOrderId);
			takeProfit.action(action.equals("BUY") ? "SELL" : "BUY");
			takeProfit.orderType("LMT");
			takeProfit.totalQuantity(quantity);
			takeProfit.lmtPrice(fixDoubleDigi(profitTakingPrice));
			takeProfit.parentId(parent.orderId());
			takeProfit.transmit(true);				    					
			takeProfit.account(m_acctList.get(0));
			takeProfit.tif("GTC");
			

			
			
			
		//This is the sell side
//			 nextOrderId = 0;
			 orderDetail.TradeMethod = "SELL";
			 
		 	 orderPrices = calTriggerPrice(orderDetail, contractMap.get(orderDetail.Symbol));	
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
		   	     parentSell.goodAfterTime(orderDateStr); 
		   	    try {
					Date  orderTime  = (Date)formatter.parse(orderDateStr);
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

	
					orderTransmit(contractMap.get(orderDetail.Symbol), takeProfit, orderDetail.orderSeqNo);
					System.out.println(new Date() + "Send takeProfitBuy orderId: " + takeProfit.orderId() + " SeqNo: " + orderDetail.orderSeqNo);
					show(new Date() + "Send takeProfitBuy orderId: " + takeProfit.orderId() + " SeqNo: " + orderDetail.orderSeqNo);

							 
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

					

						orderTransmit(contractMap.get(orderDetail.Symbol), takeProfitSell, orderDetail.orderSeqNo);
						
						System.out.println(new Date() + "Send takeProfitSell orderId: " + takeProfitSell.orderId() + " SeqNo: " + orderDetail.orderSeqNo);
						show(new Date() + "Send takeProfitSell orderId: " + takeProfitSell.orderId() + " SeqNo: " + orderDetail.orderSeqNo);
						
						
						
					
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
			
			//Always submit an request for NZDUSD and AUDUSD for market data validation.
			if(currentMarketDataList.contains("NZDUSD") == false)
			{
				contractDetail = contractMap.get("NZDUSD");
				forexListenerDetail = forexListenerHashMap.get("NZDUSD");
				if(forexListenerDetail != null && contractDetail != null){			
					controller().reqTopMktData( contractDetail, "", false, forexListenerDetail);
					currentListeningMap.put("NZDUSD", forexListenerDetail);
					currentMarketDataList.add("NZDUSD");
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				
				}
			}	
			
			if(currentMarketDataList.contains("AUDUSD") == false)
			{
				contractDetail = contractMap.get("AUDUSD");
				forexListenerDetail = forexListenerHashMap.get("AUDUSD");
				if(forexListenerDetail != null && contractDetail != null){			
					controller().reqTopMktData( contractDetail, "", false, forexListenerDetail);
					currentListeningMap.put("AUDUSD", forexListenerDetail);
					currentMarketDataList.add("AUDUSD");
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
				
	 			m_connectionPanel.m_orderSubmission.setText(new Date() + " order submission task is running. Next order: " + OrderSubmittedStr);
	 			OrderSubmittedStr = null;
				
				Date systemTimePlus1M = new Date(), systemTimePlus2M = new Date(), orderTime= new Date();
					
				//loop thru all orders in HashMap;
		//	System.out.println("Looping thru all orders: " + new Date());
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
			    	 long time = System.currentTimeMillis();
			    	 systemTimePlus1M = new Date(time + 30 * 1000);	
			    	 systemTimePlus2M = new Date(time + 120 * 1000);	
			   
		    	      
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
			    	
			    	
			    	//Compare system time and order to make sure that we submit the order on time
			    	if(systemTimePlus1M.before(orderTime) && systemTimePlus2M.after(orderTime)){
			    		
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
			    		
			    		//Make sure that we would NOT submit an duplicate order with current open order;
						for (Entry<Integer, forex> entry : liveForexOrderMap.entrySet()) {
							forex order2Loop = entry.getValue();
//						    forex tmpOrder = orderHashMap.get(order2Loop.seqOrderNo());
						    
					
						    
						    if(orderDetail.Symbol.equals(order2Loop.Symbol)){
						    	{
						    		//duplicated parent order is OK. All parent order has time in force as "GTD" good to date.
						    		if(liveOrderMap.get(entry.getKey()).tif().equals("GTD"))
						    			continue;

						    		//If order's parent order has been executed. Then skip current order to avoid duplicate
//						    		if(executedOrderMap.contains(liveOrderMap.get(entry.getKey()).parentId()))
						    		{						    		
							    		needToSubmit = false;
							    		orderDetail.OrderStatus = "Cancelled";
							    		orderDetail.comment = "Cancelled due to duplicated order in open order";						    		
							    		orderHashMap.put(orderDetail.orderSeqNo, orderDetail);
							    		System.out.println(new Date() + " No need to submit Order:  " + liveOrderMap.get(entry.getKey()).parentId());	
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
			    		if(contractMap.get(orderDetail.Symbol).historicalBarMap.isEmpty()){
						    System.out.println("Historical price map is empty: " + orderDetail.Date + orderDetail.Time + " " +orderDetail.Symbol);
			    			continue;
			    		}
					    System.out.println("Preparing: " + orderDetail.Date + orderDetail.Time + " " +orderDetail.Symbol);

			    		orderDetail.OrderStatus = "Preparing";
			    		orderHashMap.put(key, orderDetail);
			    		System.out.println("Order status: " + orderHashMap.get(orderDetail.Symbol));
			    		
			    		
			    	     // Do something
			    		 if(orderDetail.TradeMethod.equals("BUY")){
			    			 if(orderDetail.EntryMethod.equals("STOP"))
			    			 {
			    				 //This is a new order
			    				 bracketStopOrder(orderDetail, 0);
			    			 }
			    			}
			    		 else if(orderDetail.TradeMethod.equals("SELL")){
			    			 if(orderDetail.EntryMethod.equals("STOP"))
			    			 {
			    				 bracketStopOrder(orderDetail, 0);
			    			 }
			    		 }
			    		 else if(orderDetail.TradeMethod.equals("BOTH")){
			    			 if(orderDetail.EntryMethod.equals("STOP")){
			    				 //let's sumit buy order first 
			    				 orderDetail.TradeMethod = "BUY";
			    				 Order tempOrder = bracketStopOrder(orderDetail, 0);
			    				 
			    				 //Let's wait for 1 second.
			    				 try {
			    						Thread.sleep(1000);
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
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
			    		 
						 System.out.println("Submiited: " + orderDetail.Date + orderDetail.Time + " " +orderDetail.Symbol);
						 System.out.println("Order status: after submision" + orderHashMap.get(orderDetail.Symbol));	
	

			    		
			    	}else if(systemTimePlus2M.before(orderTime)){
			    		break;
			    	}
			    }
			keys = null;	
	        }
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
        	//Guy, let's rest 1000ms here
   		 try {
   				Thread.sleep(1000);
   			} catch (InterruptedException e) {
   				// TODO Auto-generated catch block
   				e.printStackTrace();
   			}   		 

			m_connectionPanel.m_orderManaging.setText(new Date() + " Live Order: " + liveOrderMap.size() + " Submitted order: " + submittedOrderHashMap.size()  + " orders in " + inputFileName + orderHashMap.size());

   		 
 		//Looping thru all live orders. If an order has been filled, looking for its orderId. Then try to modify profitaking order. And stop order.;
		forex orderDetail = null;	

 		for(Entry<Integer, Order> entry : liveOrderMap.entrySet()){
 			
 			//Adjust STop price according to actual open price and current market price.
 			adjustStopPrice(entry.getKey(), entry.getValue());
 			
 			Order order = entry.getValue();
 			orderDetail = null;
 			if(order != null)
 				orderDetail = orderHashMap.get(order.seqOrderNo()); 			
 			if(orderDetail != null){
 				Contract currencyContract = contractMap.get(orderDetail.Symbol);

 				if(orderDetail.PeakGain == null || orderDetail.PeakGain.isEmpty() || Double.parseDouble(orderDetail.PeakGain) < currencyContract.getBidPrice())
 					orderDetail.PeakGain = new Double(currencyContract.getBidPrice()).toString();
 				else if(orderDetail.MaxDrawdown == null || orderDetail.MaxDrawdown.isEmpty() || Double.parseDouble(orderDetail.MaxDrawdown) > currencyContract.getAskPrice())
 					orderDetail.MaxDrawdown = new Double(currencyContract.getAskPrice()).toString();
 				orderHashMap.put(order.seqOrderNo(), orderDetail);
 	
 			}
		}
 		} 		
        }    
        
    


private void adjustStopPrice(Integer orderId, Order order){
	forex orderDetail;
	
	//If current order is parent order, just return
	if(order.parentId() == 0)
		return;
	
	order = submittedOrderHashMap.get(orderId);
	
	if(order == null)
		return;
	
	//If current order is profit taking order, just return
	if(order.orderType().equals("LMT"))
		return;
	
	orderDetail = executedOrderMap.get(order.parentId());
	if(orderDetail == null)
		return;
	Double openPrice = 0.0;
	if(orderDetail.ActualPrice != null && !orderDetail.ActualPrice.isEmpty())
		openPrice = Double.parseDouble(orderDetail.ActualPrice);
	
	Double currentBidPrice, currentAskPrice, newStopPrice;
	Contract currencyContract = contractMap.get(orderDetail.Symbol);

	if(currencyContract == null)
		return;
	
	currentBidPrice = currencyContract.getBidPrice();
	currentAskPrice = currencyContract.getAskPrice();
	Action action = order.action();
	
	//This is a short position, we need to buy it at a price higher than current ask price to stop loss and lower price to make profit
	if(action.equals(Action.BUY)){
		//If current ask price 0.3 % is bigger than actual price, adjust STOP price to current price + 0.1% 
		if(currentAskPrice < (openPrice * (1 - 0.3/100))){
			newStopPrice = currentAskPrice * (1 - 0.1/100);
		}
		//If current ask price 0.2 % is bigger than actual price, adjust STOP price to actual open price 
		else if(currentAskPrice < (openPrice * (1 - 0.2/100))){
			newStopPrice = openPrice * (1 - 0.05/100);
		}else{//defaul set stop price as 0.1 loss
			newStopPrice = openPrice * (1 + 0.1/100);
		}
		newStopPrice = fixDoubleDigi(newStopPrice);

		if(order.auxPrice() == 0.0)
			return;
		
		if (order.auxPrice() <= newStopPrice)
			return;
	}else{//This is a long position, we need to sell it at a price higher than current bid price to make profit and stop at lower price to stop loss

		//If current bid price 0.3 % is higher than actual price, adjust STOP price to current price - 0.1% 
				if(currentBidPrice > (openPrice * (1 + 0.3/100))){
					newStopPrice = currentBidPrice * (1 + 0.1/100);
				}
				//If current bid price 0.2 % is higher than actual price, adjust STOP price to actual open price 
				else if(currentAskPrice > (openPrice * (1 + 0.2/100))){
					newStopPrice = openPrice * (1 + 0.05/100);
				}else{//defaul set stop price as 0.1 loss
					newStopPrice = openPrice * (1 - 0.1/100);
				}
				newStopPrice = fixDoubleDigi(newStopPrice);

				if(order.auxPrice() == 0.0)
					return;
				
				if (order.auxPrice() >= newStopPrice)
					return;
	}	
			
			System.out.println("SeqNo: " + orderDetail.orderSeqNo + "Sending Mofidied Order: " + order.orderId() + " " + currencyContract.symbol() + currencyContract.currency() + " old STOP: " + order.auxPrice() + "Current: " + newStopPrice);
			show(new Date() + " SeqNo: " + orderDetail.orderSeqNo + "Sending Mofidied Order: " + order.orderId() + " " + currencyContract.symbol() + currencyContract.currency() + " old STOP: " + order.auxPrice() + "Current: " + newStopPrice);
			order.auxPrice(newStopPrice);
			ForexOrderHandler stporderHandler = new ForexOrderHandler(order, orderDetail.orderSeqNo);
			controller().placeOrModifyOrder( currencyContract, order, stporderHandler);	
			submittedOrderHashMap.put(order.orderId(), order);
	
}
 
    
    
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
			
			if (orderDetail == null) return;
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
				    		System.out.print(" cost@ " + orderDetail.ActualPrice);				    		
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
	//		System.out.println(execution.time() + " " +  execution.side() + " " + contract.symbol() + contract.currency() + " " + execution.cumQty() + " Filled @ " + execution.avgPrice() + " OrderId: " + execution.orderId());

			//Order status after trade report.
//			if(orderHashMap.get(order.seqOrderNo()) != null)
//				System.out.println(new Date() + " Trade Report sequence No.: " + order.seqOrderNo() + " in orderHashMap" + orderHashMap.get(order.seqOrderNo()).OrderStatus + " executed @ " + execution.time());

			
			
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
			    
			    if(orderDetail.orderIdList.contains(order.orderId())){
			    	if(orderDetail.OrderStatus.equals("Filled")){
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

	class histortyDataHandler implements IHistoricalDataHandler{
		Contract m_currencyContract;

		public histortyDataHandler(Contract currencyContract) {
			// TODO Auto-generated constructor stub
			m_currencyContract = currencyContract;
		}

		@Override
		public void historicalData(Bar bar, boolean hasGaps) {
			// TODO Auto-generated method stub
			m_currencyContract.putHistoricalBar(bar.formattedTime(), bar);
			
			contractMap.put(m_currencyContract.symbol() + m_currencyContract.currency(), m_currencyContract);
			
			
		}

		@Override
		public void historicalDataEnd() {
			// TODO Auto-generated method stub
//			System.out.println(new Date() + " end of bar high: ");
			TreeSet<String> keys = new TreeSet<String>(m_currencyContract.historicalBarMap.keySet());
			TreeSet<String> treereverse = (TreeSet) keys.descendingSet();
			for (String key : treereverse){
			Bar bar = m_currencyContract.historicalBarMap.get(key);
//			System.out.println(m_currencyContract.symbol() + m_currencyContract.currency() + " time: " + bar.formattedTime()+ " bar high: " + bar.high() + " bar low: " + bar.low() + " bar close: " + bar.close());
//			show(m_currencyContract.symbol() + m_currencyContract.currency() + " time: " + bar.formattedTime()+ " bar high: " + bar.high() + " bar low: " + bar.low() + " bar close: " + bar.close());

			}
		}
	};
	
	
	public void requestHistoricalBar(String endTime, Contract currencyContract){
		//forex orderDetail;
		
		//Only request it once.
		/*
		historicalDataReq.clear();
		SortedSet<Long> keys = new TreeSet<Long>(orderHashMap.keySet());
		for (Long key : keys)
		{
			orderDetail = orderHashMap.get(key);
		    if(orderDetail.ValidDuration.equals("60"))
		    	continue;
		    if(historicalDataReq.contains(orderDetail.Symbol))
		    	continue;
		    historicalDataReq.add(orderDetail.Symbol);
		    currencyContract = contractMap.get(orderDetail.Symbol);
		  */  
			histortyDataHandler forexHistoricalHandler = new histortyDataHandler(currencyContract);
			if(forexHistoricalHandler != null && currencyContract != null)
				controller().reqHistoricalData(currencyContract, endTime, 3600 * 2, DurationUnit.SECOND, BarSize._5_mins, WhatToShow.MIDPOINT, true, forexHistoricalHandler);
			else
			{
				System.out.println("Null pointer here, Please check your order" + currencyContract + forexHistoricalHandler);
				show(new Date() + "Null pointer here, Please check your order" + currencyContract + forexHistoricalHandler);
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
	 
  public void run() {
      	System.out.println("Hello from a market data managing thread!");
//       Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

      	Date orderTime = new Date();	
      	forex orderDetail;
      	DateFormat formatter; 
  		String orderDateStr;
  	
 //     System.out.println("Hello from a Order submission thread!");
//      Thread.currentThread().setPriority(Thread.MAX_PRIORITY - 1);
      
      
   // creating reverse set
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

 		 //Update time counter
 		fileReadingCounter++;	

 		 
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
		    		
		    		excelInput.setInputFile(inputFileName);
		    	   	orderHashMap = excelInput.read(orderHashMap);	
		    	 	show(new Date() + " File " + inputFileName + " is read back. Total size in HashMap: " + orderHashMap.size() + " orders.");

		   	        
		   	        
	    	   		excelOutput.setOutputFile(fileNameStrs[0] + "_" + "Report_" + orderDateStr + "." + fileNameStrs[1]);
	    	   		excelOutput.write(orderHashMap);
	    	   		fileReadingCounter = 0;
	    	   		show(new Date() + " File " + fileNameStrs[0] + "_" + orderDateStr + "." + fileNameStrs[1] + " write back.");
	    			
	    	   		
 	   	 }
	    	
	    } 
		catch (Exception e){
			
			e.printStackTrace();
		}	
		
		
		//Request real time tick price data if it isn't available.
		if(((m_contract_NZDUSD.getAskPrice() == 0 || m_contract_NZDUSD.getBidPrice() == 0 || m_contract_AUDUSD.getAskPrice() == 0 || m_contract_AUDUSD.getBidPrice() == 0)))
		{
			requestTickData(orderHashMap);  	
		}


		//Request historical data every 5 seconds.
		if((fileReadingCounter % 5 == 0 || (orderDetail != null && contractMap.get(orderDetail.Symbol).historicalBarMap.isEmpty()) )){
 					    	      
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
					  requestHistoricalBar(orderDateStr, contractMap.get(orderDetail.Symbol));
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
		
		//Every 60 seconds, try to clear dead orders (filled/cancelled) in submitted order hashmap.
		if(counter++ > 60)
		{
		
		
			
			
			counter = 0;
		
			//Let's loop thru and remove dead submitted orders.
			//loop thru all orders in HashMap;
			//	System.out.println("Looping thru all orders: " + new Date());
				SortedSet<Integer> keys = new TreeSet<Integer>(submittedOrderHashMap.keySet());
				for (Integer key : keys) { 					
					
					Order submittedOrder = submittedOrderHashMap.get(key);
					if(submittedOrder == null) 
						submittedOrderHashMap.remove(key);
					if(liveOrderMap.containsKey(key)) 
						continue;
					else	
						submittedOrderHashMap.remove(key);
					
				}
				keys = null;
		}
		
 		 
	
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
