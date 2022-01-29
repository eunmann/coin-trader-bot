package coin.unittest;

import coin.trader.exchange.CurrencyPair;
import coin.trader.exchange.Ticker;

public class MockTicker implements Ticker {
	public double last = 0;
	public double lowestAsk = 0;
	public double highestBid = 0;
	public double baseVolume = 50000;
	public double quoteVolume = 0;
	public CurrencyPair currencyPair = null;

	public MockTicker() {
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
