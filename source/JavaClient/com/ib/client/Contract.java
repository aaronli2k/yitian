/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import com.ib.client.Types.Right;
import com.ib.client.Types.SecIdType;
import com.ib.client.Types.SecType;
import com.ib.controller.Bar;

public class Contract implements Cloneable {
    private int     m_conid;
    private String  m_symbol;
    private String  m_secType;
    private String  m_lastTradedateOrContractMonth;
    private double  m_strike;
    private String  m_right;
    private String  m_multiplier; // should be double
    private String  m_exchange;
    private String  m_primaryExch; // pick a non-aggregate (ie not the SMART exchange) exchange that the contract trades on.  DO NOT SET TO SMART.
    private String  m_currency;
    private String  m_localSymbol;
    private String  m_tradingClass;
    private String  m_secIdType; // CUSIP;SEDOL;ISIN;RIC
    private String  m_secId; 
    
    private DeltaNeutralContract m_underComp;
    private boolean m_includeExpired;  // can not be set to true for orders
    // COMBOS
    private String m_comboLegsDescrip; // received in open order version 14 and up for all combos 
    private ArrayList<ComboLeg> m_comboLegs = new ArrayList<ComboLeg>(); // would be final except for clone

    private double m_bidPrice = 0.0;
    private double m_askPrice = 0.0;
    private double m_lastPrice = 0.0;
    private double m_closePrice = 0.0;
    private double m_openPrice = 0.0;
    private double m_smaPrice = 0.0;
    public double m_maxPrice = 0.0;
    public double m_minPrice = 0.0;
    public ConcurrentHashMap<String, Bar> historicalBarMap = new ConcurrentHashMap<String, Bar>();
    
public void    putHistoricalBar(String timeString, Bar historicalBar){
	historicalBarMap.put(timeString, historicalBar);
}

public Bar    getHistoricalBar(String timeString){
	return historicalBarMap.get(timeString);
}
    private MovingAverage sma = new MovingAverage(20);
    
    // Get
    public double strike()          { return m_strike; }
    public int conid()              { return m_conid; }
    public SecIdType secIdType()    { return SecIdType.get(m_secIdType); }
    public String getSecIdType()    { return m_secIdType; }
    public SecType secType()        { return SecType.get(m_secType); }
    public String getSecType()      { return m_secType; }
    public String currency()        { return m_currency; }
    public String exchange()        { return m_exchange; }
    public String primaryExch()     { return m_primaryExch; }
    public String lastTradeDateOrContractMonth()          { return m_lastTradedateOrContractMonth; }
    public String localSymbol()     { return m_localSymbol; }
    public String tradingClass()    { return m_tradingClass; }
    public String multiplier()      { return m_multiplier; }
    public Right right()            { return Right.get(m_right); }
    public String getRight()        { return m_right; }
    public String secId()           { return m_secId; }
    public String symbol()          { return m_symbol; }
    public boolean includeExpired() { return m_includeExpired; }
    public DeltaNeutralContract underComp() { return m_underComp; }
    public ArrayList<ComboLeg> comboLegs()  { return m_comboLegs; }
    public String comboLegsDescrip()        { return m_comboLegsDescrip; }

    // Set
    public void conid(int v)            { m_conid = v; }
    public void currency(String v)      { m_currency = v; }
    public void exchange(String v)      { m_exchange = v; }
    public void lastTradeDateOrContractMonth(String v)        { m_lastTradedateOrContractMonth = v; }
    public void localSymbol(String v)   { m_localSymbol = v; }
    public void tradingClass(String v)  { m_tradingClass = v; }
    public void multiplier(String v)    { m_multiplier = v; }
    public void primaryExch(String v)   { m_primaryExch = v; }
    public void right(Right v)          { m_right = ( v == null ) ? null : v.getApiString(); }
    public void right(String v)         { m_right = v; }
    public void secId(String v)         { m_secId = v; }
    public void secIdType(SecIdType v)  { m_secIdType = ( v == null ) ? null : v.getApiString(); }
    public void secIdType(String v)     { m_secIdType = v; }
    public void secType(SecType v)      { m_secType = ( v == null ) ? null : v.getApiString(); }
    public void secType(String v)       { m_secType = v; }
    public void strike(double v)        { m_strike = v; }
    public void symbol(String v)        { m_symbol = v; }
    public void underComp(DeltaNeutralContract v) { m_underComp = v; }
    public void includeExpired(boolean v)         { m_includeExpired = v; }
    public void comboLegs(ArrayList<ComboLeg> v)  { m_comboLegs = v; }
    public void comboLegsDescrip(String v)        { m_comboLegsDescrip = v; }
    
    public void setBidPrice(double bidPrice) {m_bidPrice = bidPrice;}
    public void setAskPrice(double askPrice) {m_askPrice = askPrice;}
    public void setLastPrice(double lastPrice) {m_lastPrice = lastPrice;}
    public void setClosePrice(double closePrice) {m_closePrice = closePrice;}
    public void setOpenPrice(double openPrice) {m_openPrice = openPrice;}

    public double getBidPrice() { return m_bidPrice;}
    public double getAskPrice() { return m_askPrice;}
    public double getLastPrice() { return m_lastPrice;}
    public double getClosePrice() { return m_closePrice;}
    public double getOpenPrice() { return m_openPrice;}
    public double getMaxPrice() { return m_maxPrice;}
    public double getMinPrice() { return m_minPrice;}
    
    
    public Contract() {
    	m_conid = 0;
        m_strike = 0;
        m_includeExpired = false;
    }

    @Override public Contract clone() {
        try {
            Contract copy = (Contract)super.clone();
            if ( copy.m_comboLegs != null ) {
                copy.m_comboLegs = new ArrayList<ComboLeg>( copy.m_comboLegs);
            }
            else {
                copy.m_comboLegs = new ArrayList<ComboLeg>();
            }
            return copy;
        }
        catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    //Aaron for Forex
    public Contract(String p_symbol, String p_secType, 
            String p_exchange, String p_currency, Double maxPrice, Double minPrice){
    	 m_symbol = p_symbol;
         m_secType = p_secType;
         m_exchange = p_exchange;
         m_currency = p_currency;
         m_maxPrice =  maxPrice;
         m_minPrice =  minPrice;
         m_bidPrice = 0.0;
         m_askPrice = 0.0;
    }
    
    public Contract(int p_conId, String p_symbol, String p_secType, String p_lastTradeDateOrContractMonth,
                    double p_strike, String p_right, String p_multiplier,
                    String p_exchange, String p_currency, String p_localSymbol, String p_tradingClass,
                    ArrayList<ComboLeg> p_comboLegs, String p_primaryExch, boolean p_includeExpired,
                    String p_secIdType, String p_secId) {
    	m_conid = p_conId;
        m_symbol = p_symbol;
        m_secType = p_secType;
        m_lastTradedateOrContractMonth = p_lastTradeDateOrContractMonth;
        m_strike = p_strike;
        m_right = p_right;
        m_multiplier = p_multiplier;
        m_exchange = p_exchange;
        m_currency = p_currency;
        m_includeExpired = p_includeExpired;
        m_localSymbol = p_localSymbol;
        m_tradingClass = p_tradingClass;
        m_comboLegs = p_comboLegs;
        m_primaryExch = p_primaryExch;
        m_secIdType = p_secIdType;
        m_secId = p_secId ;
    }

    @Override
    public boolean equals(Object p_other) {
    	if (this == p_other) {
    		return true;
    	}

    	if (p_other == null || !(p_other instanceof Contract)) {
    		return false;
    	}

        Contract l_theOther = (Contract)p_other;

        if (m_conid != l_theOther.m_conid) {
        	return false;
        }

        if (Util.StringCompare(m_secType, l_theOther.m_secType) != 0) {
        	return false;
        }

        if (Util.StringCompare(m_symbol, l_theOther.m_symbol) != 0 ||
        	Util.StringCompare(m_exchange, l_theOther.m_exchange) != 0 ||
        	Util.StringCompare(m_primaryExch, l_theOther.m_primaryExch) != 0 ||
        	Util.StringCompare(m_currency, l_theOther.m_currency) != 0) {
        	return false;
        }

        if (!Util.NormalizeString(m_secType).equals("BOND")) {

        	if (m_strike != l_theOther.m_strike) {
        		return false;
        	}

        	if (Util.StringCompare(m_lastTradedateOrContractMonth, l_theOther.m_lastTradedateOrContractMonth) != 0 ||
        		Util.StringCompare(m_right, l_theOther.m_right) != 0 ||
        		Util.StringCompare(m_multiplier, l_theOther.m_multiplier) != 0 ||
        		Util.StringCompare(m_localSymbol, l_theOther.m_localSymbol) != 0 ||
        		Util.StringCompare(m_tradingClass, l_theOther.m_tradingClass) != 0) {
        		return false;
        	}
        }

        if (Util.StringCompare(m_secIdType, l_theOther.m_secIdType) != 0) {
        	return false;
        }

        if (Util.StringCompare(m_secId, l_theOther.m_secId) != 0) {
        	return false;
        }

    	// compare combo legs
        if (!Util.ArrayEqualsUnordered(m_comboLegs, l_theOther.m_comboLegs)) {
        	return false;
        }

        if (m_underComp != l_theOther.m_underComp) {
        	if (m_underComp == null || l_theOther.m_underComp == null) {
        		return false;
        	}
        	if (!m_underComp.equals(l_theOther.m_underComp)) {
        		return false;
        	}
        }
        return true;
    }

    /** Returns a text description that can be used for display. */
    public String description() {
        StringBuilder sb = new StringBuilder();

        if (isCombo() ) {
            int i = 0;
            for (ComboLeg leg : m_comboLegs) {
                if (i++ > 0) {
                    sb.append( "/");
                }
                sb.append( leg.toString() );
            }
        }
        else {
            sb.append( m_symbol);
            app( sb, m_secType);
            app( sb, m_exchange);

            if (m_exchange != null && m_exchange.equals( "SMART") && m_primaryExch != null) {
                app( sb, m_primaryExch);
            }

            app( sb, m_lastTradedateOrContractMonth);

            if (m_strike != 0) {
                app( sb, m_strike);
            }

            if( !Util.StringIsEmpty(m_right) ) {
                app( sb, m_right);
            }
        }
        return sb.toString();
    }

    private static void app(StringBuilder buf, Object obj) {
        if (obj != null) {
            buf.append( " ");
            buf.append( obj);
        }
    }

    public boolean isCombo() {
        return m_comboLegs.size() > 0;
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();

        add( sb, "conid", m_conid);
        add( sb, "symbol", m_symbol);
        add( sb, "secType", m_secType);
        add( sb, "lastTradeDateOrContractMonth", m_lastTradedateOrContractMonth);
        add( sb, "strike", m_strike);
        add( sb, "right", m_right);
        add( sb, "multiplier", m_multiplier);
        add( sb, "exchange", m_exchange);
        add( sb, "currency", m_currency);
        add( sb, "localSymbol", m_localSymbol);
        add( sb, "tradingClass", m_tradingClass);
        add( sb, "primaryExch", m_primaryExch);
        add( sb, "secIdType", m_secIdType);
        add( sb, "secId", m_secId);

        return sb.toString();
    }

    public static void add(StringBuilder sb, String tag, Object val) {
        if (val == null || val instanceof String && ((String)val).length() == 0) {
            return;
        }

        sb.append( tag);
        sb.append( '\t');
        sb.append( val);
        sb.append( '\n');
    }
   
    
    public void add(double price){
    	sma.newNum((price));
    }

    public double ma(){
    	return sma.getAvg();
    }


     class MovingAverage {
        private final Queue<Double> window = new LinkedList<Double>();
        private final int period;
        private double sum;

        public MovingAverage(int period) {
            assert period > 0 : "Period must be a positive integer";
            this.period = period;
        }

        public void newNum(double num) {
            sum += num;
            window.add(num);
            if (window.size() > period) {
                sum -= window.remove();
            }
        }

        public double getAvg() {
            if (window.isEmpty()) return 0; // technically the average is undefined
            return sum / window.size();
        }

 
    }
    
     public static void main(String[] args) {
    	    double[] testData = {1,2,3,4,5,5,4,3,2,1};
    	    int[] windowSizes = {3,5};
    	    for (int windSize : windowSizes) {
    	    	Contract newContract = new Contract();
    	        ;
    	        for (double x : testData) {
    	            newContract.sma.newNum(x);
    	            System.out.println("Next number = " + x + ", SMA = " + newContract.sma.getAvg());
    	        }
    	        System.out.println();
    	    }
    	}
}

