package coin.trader.exchange.poloniex;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import coin.trader.exchange.CurrencyPair;
import coin.trader.exchange.Ticker;
import coin.trader.exchange.Tickers;

public class PoloniexTickers implements Tickers {
	final private List<PoloniexTicker> tickers;
	final private Map<CurrencyPair, Ticker> tickerMap;

	public PoloniexTickers( final String jsonStr ) {
		final Type listType = new TypeToken<Map<String, PoloniexTicker>>() {
		}.getType();
		final Gson gson = new Gson();
		final Map<String, PoloniexTicker> tickersMap = gson.fromJson( jsonStr, listType );
		this.tickers = new ArrayList<PoloniexTicker>( 128 );
		this.tickerMap = new HashMap<CurrencyPair, Ticker>();

		for ( final Entry<String, PoloniexTicker> pair : tickersMap.entrySet() ) {
			this.tickerMap.put( new PoloniexCurrencyPair( pair.getKey() ), pair.getValue() );
		}
	}

	@Override
	public List<? extends Ticker> getTickers() {
		return this.tickers;
	}

	@Override
	public Map<CurrencyPair, Ticker> toMap() {
		return this.tickerMap;
	}
}
