package coin.trader.algorithm.history;

import coin.trader.exchange.CurrencyPair;
import coin.trader.exchange.Exchange;

public interface HistoryTickerIndicatorFactory {
	public HistoryTickerIndicator createHistoryTickerIndicator( final CurrencyPair currencyPair );

	public HistoryMonitor createHistoryMonitor( final Exchange exchange );

	public int candlePeriod();

	public int amountOfCandles();
}
