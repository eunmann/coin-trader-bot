package coin.trader.exchange.bittrex;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import coin.trader.exchange.CurrencyPair;
import coin.trader.exchange.Ticker;
import coin.trader.exchange.Tickers;

public class BittrexTickers implements Tickers, Serializable {
	private static final long serialVersionUID = 97576816398726350L;
	private List<BittrexTicker> result;

	boolean success;
	String message;

	@Override
	public List<? extends Ticker> getTickers() {
		return this.result;
	}

	@Override
	public Map<CurrencyPair, Ticker> toMap() {
		final HashMap<CurrencyPair, Ticker> map = new HashMap<CurrencyPair, Ticker>();
		for ( final BittrexTicker ticker : this.result ) {
			map.put( ticker.getCurrencyPair(), ticker );
		}
		return map;
	}
}
