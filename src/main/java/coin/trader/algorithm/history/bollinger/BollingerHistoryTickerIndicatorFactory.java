package coin.trader.algorithm.history.bollinger;

import coin.trader.algorithm.history.HistoryMonitor;
import coin.trader.algorithm.history.HistoryTickerIndicator;
import coin.trader.algorithm.history.HistoryTickerIndicatorFactory;
import coin.trader.exchange.CurrencyPair;
import coin.trader.exchange.Exchange;

public class BollingerHistoryTickerIndicatorFactory implements HistoryTickerIndicatorFactory {

	@Override
	public HistoryTickerIndicator createHistoryTickerIndicator( final CurrencyPair currencyPair ) {
		return new BollingerHistoryTickerIndicator( currencyPair );
	}

	@Override
	public HistoryMonitor createHistoryMonitor( final Exchange exchange ) {
		return new HistoryMonitor( exchange, this );
	}

	@Override
	public int candlePeriod() {
		return BollingerHistoryTickerIndicator.CANDLE_PERIOD;
	}

	@Override
	public int amountOfCandles() {
		return BollingerHistoryTickerIndicator.AMOUNT_OF_CANDLES;
	}

}
