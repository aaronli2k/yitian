package apidemo;
/*
 * Copyright (c) 1997, 2011, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */



import java.applet.Applet;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.OrderType;
import com.ib.client.Types.SecType;
import com.ib.controller.ApiController.ILiveOrderHandler;

import apidemo.util.HtmlButton;
import apidemo.util.VerticalPanel;

/**
 * Time!
 *
 * @author Rachel Gollub
 * @author Daniel Peek replaced circle drawing calculation, few more changes
 */
@SuppressWarnings("serial")
public class forex {

	public String Date;
	public String Time;
	public String Symbol;
	public String Quantity;
	public String TradeMethod;
	public String EntryMethod;
	public String TriggerPct;
	public String LossPct;
	public String ProfitPct;
	public String ExitMethod;
	public String ValidDuration;
	public String Importance;
	
	public String OrderID;
	public String OrderStatus;
	public String MarketPrice;
	public String TriggeredPrice;
	public String ActualPrice;
	public String MaxDrawdown;
	public String PeakGain;
	public String LossGain;
	public String ClosedPrice;
	
	public String OpenTime;
	public String CloseTime;
	
	public long orderSeqNo;
	
	
	public String comment;
	
	public boolean OCA;

	
	public ArrayList<Integer> orderIdList = new ArrayList<Integer>();
	
//	public final Contract m_contract_AUDUSD = new Contract("AUD", "CASH", "IDEALPRO", "USD");
//	public final Contract m_contract_AUDNZD = new Contract("AUD", "CASH", "IDEALPRO", "NZD");
//	public final Contract m_contract_AUDJPY = new Contract("AUD", "CASH", "IDEALPRO", "JPY");
	
	
    private volatile Thread timer;       // The thread that displays clock
    private int lastxs, lastys, lastxm,
            lastym, lastxh, lastyh;  // Dimensions used to draw hands
    private SimpleDateFormat formatter;  // Formats the date displayed
    private String lastdate;             // String to hold date displayed
    private Font clockFaceFont;          // Font for number display on clock
    private Date currentDate;            // Used to get date to display
    private Color handColor;             // Color of main hands and dial
    private Color numberColor;           // Color of second hand and numbers
    private int xcenter = 80, ycenter = 55; // Center position
	public Long groupId;

    public void forex() {
        lastxs = lastys = lastxm = lastym = lastxh = lastyh = 0;
        formatter = new SimpleDateFormat("yyyyMMMdd hh:mm:ss",
                Locale.getDefault());
        currentDate = new Date();
        lastdate = formatter.format(currentDate);
        clockFaceFont = new Font("Serif", Font.PLAIN, 14);
        handColor = Color.blue;
        numberColor = Color.darkGray;
        OCA = false;

    }

  

   

    public void stop() {
        timer = null;
    }

 

    public String getAppletInfo() {
        return "Title: A Clock \n"
                + "Author: Rachel Gollub, 1995 \n"
                + "An analog clock.";
    }

    public String[][] getParameterInfo() {
        String[][] info = {
            { "bgcolor", "hexadecimal RGB number",
                "The background color. Default is the color of your browser." },
            { "fgcolor1", "hexadecimal RGB number",
                "The color of the hands and dial. Default is blue." },
            { "fgcolor2", "hexadecimal RGB number",
                "The color of the second hand and numbers. Default is dark gray." }
        };
        return info;
    }
    
    public void doTrading(){
    	
//    	updateContract(String exchange, String SymbolPair, SecType Security, String BaseCurrency)
  //  	log();
  //  	System.out.println(new Date().toString());
    }
    
}
