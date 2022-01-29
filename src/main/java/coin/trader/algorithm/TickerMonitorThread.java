package coin.trader.algorithm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import coin.trader.exchange.CurrencyPair;
import coin.trader.exchange.Exchange;
import coin.trader.logger.Logger;

/**
 * This thread makes sure our samples never get stale.
 * 
 * TODO( EMU ): This should only be applied to 1 exchange at a time, just like
 * the alg. Need to enforce this somehow. Maybe subclasses should be forced to
 * do this.
 * 
 * 
 * @author Evan
 *
 */
public abstract class TickerMonitorThread extends Thread {
	protected static final Logger LOGGER = new Logger( TickerMonitorThread.class.getSimpleName() );

	protected final SampleListenerManager sampleListenerManager = new SampleListenerManager();
	protected final Map<CurrencyPair, TickerIndicator> tickerIndicatorMap = new ConcurrentHashMap<CurrencyPair, TickerIndicator>( 128 );
	protected final Exchange exchange;
	protected final TickerIndicatorFactory tickerIndicatorFactory;

	public TickerMonitorThread( final Exchange exchange, final TickerIndicatorFactory tickerIndicatorFactory ) {
		this.exchange = exchange;
		this.tickerIndicatorFactory = tickerIndicatorFactory;
	}
}
