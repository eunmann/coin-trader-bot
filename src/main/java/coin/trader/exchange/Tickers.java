package coin.trader.exchange;

import java.util.List;
import java.util.Map;

/**
 * Generic ticker container object
 * 
 * @author Evan
 *
 */
public interface Tickers {
	public List<? extends Ticker> getTickers();

	public Map<CurrencyPair, Ticker> toMap();
}
