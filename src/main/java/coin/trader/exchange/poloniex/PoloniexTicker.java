package coin.trader.exchange.poloniex;

import java.io.Serializable;

import coin.trader.exchange.CurrencyPair;
import coin.trader.exchange.Ticker;

public class PoloniexTicker implements Ticker, Serializable {
	private static final long serialVersionUID = -6212926100360778568L;

	double last = 0;
	double lowestAsk = 0;
	double highestBid = 0;
	double percentChange = 0;
	double baseVolume = 0;
	double quoteVolume = 0;
	CurrencyPair currencyPair = null;

	public PoloniexTicker() {
	}

	@Override
	public CurrencyPair getCurrencyPair() {
		return this.currencyPair;
	}

	@Override
	public double getLast() {
		return this.last;
	}

	@Override
	public double getAsk() {
		return this.lowestAsk;
	}

	@Override
	public double getBid() {
		return this.highestBid;
	}

	@Override
	public double getBaseVolume() {
		return this.baseVolume;
	}

	@Override
	public double getQuoteVolume() {
		return this.quoteVolume;
	}
}
