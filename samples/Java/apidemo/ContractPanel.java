/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package apidemo;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;

import com.ib.client.Contract;
import com.ib.client.Types.Right;
import com.ib.client.Types.SecType;

import apidemo.util.TCombo;
import apidemo.util.UpperField;
import apidemo.util.VerticalPanel;

public class ContractPanel extends JPanel {
	protected UpperField m_symbol = new UpperField();
	protected TCombo<SecType> m_secType = new TCombo<SecType>( SecType.values() );
	protected UpperField m_lastTradeDateOrContractMonth = new UpperField();
	protected UpperField m_strike = new UpperField();
	
	protected TCombo<Right> m_right = new TCombo<Right>( Right.values() );
	protected UpperField m_multiplier = new UpperField();
	protected UpperField m_exchange = new UpperField();
	protected UpperField m_compExch = new UpperField();
	protected UpperField m_currency = new UpperField();
	protected UpperField m_localSymbol = new UpperField();
	protected UpperField m_tradingClass = new UpperField();
	
	//added by Aaron
	protected UpperField m_method = new UpperField();
	protected UpperField m_percent = new UpperField();
	protected UpperField m_effectiveDate = new UpperField();
	protected UpperField m_effectiveTime = new UpperField();
	protected UpperField m_effectiveDuration = new UpperField();
	

	private Contract m_contract;

	ContractPanel(Contract c) {
		m_contract = c;

		if (c.secType() == SecType.None) {
			m_symbol.setText( "EUR");
			m_secType.setSelectedItem( SecType.CASH);
			m_exchange.setText( "IDEALPRO");
//			m_compExch.setText( "ISLAND");
			m_currency.setText( "GBP");
			
			m_method.setText( "STOP");
			m_percent.setText( "0.1");
			m_effectiveDate.setText( "20160908");
			m_effectiveTime.setText( "22:22");
			m_effectiveDuration.setText( "60 seconds");
			
		}
		else {
			m_symbol.setText( m_contract.symbol());
			m_secType.setSelectedItem( m_contract.secType() );
			m_lastTradeDateOrContractMonth.setText( m_contract.lastTradeDateOrContractMonth());
			m_strike.setText( "" + m_contract.strike() );
			m_right.setSelectedItem( m_contract.right() ); 
			m_multiplier.setText( m_contract.multiplier() );
			m_exchange.setText( m_contract.exchange());
			m_compExch.setText( m_contract.primaryExch() );
			m_currency.setText( m_contract.currency());
			m_localSymbol.setText( m_contract.localSymbol());
			m_tradingClass.setText( m_contract.tradingClass() );
		}
		
		VerticalPanel p = new VerticalPanel();
    	p.add( "Symbol", m_symbol);
    	p.add( "Sec type", m_secType);
    	p.add( "Last trade date or contract month", m_lastTradeDateOrContractMonth);
    	p.add( "Strike", m_strike);
    	p.add( "Put/call", m_right);
    	p.add( "Multiplier", m_multiplier);
    	p.add( "Exchange", m_exchange);
    	p.add( "Comp. Exch.", m_compExch);
    	p.add( "Currency", m_currency);
    	p.add( "Local symbol", m_localSymbol);
    	p.add( "Trading class", m_tradingClass);
    	
    	p.add( "Exit Method", m_method);
    	p.add( "LOST percent", m_percent);
    	p.add( "Submit date", m_effectiveDate);
    	p.add( "Submit time", m_effectiveTime);
    	p.add( "Effective duration", m_effectiveDuration);
    	
    	setLayout( new BorderLayout() );
    	add( p);
	}
	
	@Override public Dimension getMaximumSize() {
		return super.getPreferredSize();
	}
	
	public void onOK() {
		if (m_contract.isCombo() ) {
			return;
		}
		
		// component exchange is only relevant if exchange is SMART or BEST
		String exch = m_exchange.getText().toUpperCase(); 
		String compExch = exch.equals( "SMART") || exch.equals( "BEST") ? m_compExch.getText().toUpperCase() : null; 		
		
		m_contract.symbol( m_symbol.getText().toUpperCase() ); 
		m_contract.secType( m_secType.getSelectedItem() ); 
		m_contract.lastTradeDateOrContractMonth( m_lastTradeDateOrContractMonth.getText() ); 
		m_contract.strike( m_strike.getDouble() ); 
		m_contract.right( m_right.getSelectedItem() ); 
		m_contract.multiplier( m_multiplier.getText() ); 
		m_contract.exchange( exch);
		m_contract.primaryExch( compExch);
		m_contract.currency( m_currency.getText().toUpperCase() ); 
		m_contract.localSymbol( m_localSymbol.getText().toUpperCase() );
		m_contract.tradingClass( m_tradingClass.getText().toUpperCase() );
	}
	
   public void updateContract(String exchange, String SymbolPair, SecType Security, String BaseCurrency){
	// component exchange is only relevant if exchange is SMART or BEST
			String exch = exchange.toUpperCase(); 
			String compExch = exch.equals( "SMART") || exch.equals( "BEST") ? m_compExch.getText().toUpperCase() : null; 		
			
			m_contract.symbol( SymbolPair.toUpperCase() ); 
			m_contract.secType( Security); 
			m_contract.lastTradeDateOrContractMonth( m_lastTradeDateOrContractMonth.getText() ); 
			m_contract.strike( m_strike.getDouble() ); 
			m_contract.right( m_right.getSelectedItem() ); 
			m_contract.multiplier( m_multiplier.getText() ); 
			m_contract.exchange( exch);
			m_contract.primaryExch( compExch);
			m_contract.currency( BaseCurrency.toUpperCase() ); 
			m_contract.localSymbol( m_localSymbol.getText().toUpperCase() );
			m_contract.tradingClass( m_tradingClass.getText().toUpperCase() );
	   
   }	
	
}
