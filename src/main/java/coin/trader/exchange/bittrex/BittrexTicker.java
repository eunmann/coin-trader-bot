package coin.trader.exchange.bittrex;

import java.io.Serializable;

import coin.trader.exchange.CurrencyPair;
import coin.trader.exchange.Ticker;

public class BittrexTicker implements Ticker, Serializable {
	private static final long serialVersionUID = 7009343579834671844L;

	String MarketName = null;
	double High = 0.0;
	double Low = 0.0;
	double Volume = 0.0;
	double Last = 0.0;
	double BaseVolume = 0.0;
	String TimeStamp = null;
	double Bid = 0.0;
	double Ask = 0.0;
	int OpenBuyOrders = 0;
	int OpenSellOrders = 0;
	double PrevDay = 0.0;
	String Created = null;
	String DisplayMarketName = null;

	@Override
	public CurrencyPair getCurrencyPair() {
		return new BittrexCurrencyPair( this.MarketName );
	}

	@Override
	public double getLast() {
		return this.Last;
	}

	@Override
	public double getAsk() {
		return this.Ask;
	}

	@Override
	public double getBid() {
		return this.Bid;
	}

	@Override
	public double getBaseVolume() {
		return this.BaseVolume;
	}

	@Override
	public double getQuoteVolume() {
		return this.Volume;
	}

}
