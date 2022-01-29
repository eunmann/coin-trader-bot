package coin.trader.algorithm;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import coin.trader.exchange.CurrencyPair;
import coin.trader.exchange.Exchange;
import coin.trader.exchange.Ticker;
import coin.trader.utilities.Callback.Return;

public class TickerLastMonitorThread extends TickerMonitorThread {
	public TickerLastMonitorThread( final Exchange exchange, final TickerIndicatorFactory tickerIndicatorFactory ) {
		super( exchange, tickerIndicatorFactory );
	}

	@Override
	public void run() {
		/* Loop forever */
		while ( true ) {
			final CountDownLatch latch = new CountDownLatch( 1 );
			this.exchange.getTickers( new Return<Map<CurrencyPair, Ticker>>() {
				@Override
				public void succeeded( final Map<CurrencyPair, Ticker> tickers ) {
					try {

						final List<TickerIndicator> indicatorList = new CopyOnWriteArrayList<TickerIndicator>();

						for ( final Entry<CurrencyPair, Ticker> pair : tickers.entrySet() ) {
							final CurrencyPair key = pair.getKey();
							TickerIndicator indicator = TickerLastMonitorThread.this.tickerIndicatorMap.get( key );

							if ( indicator == null ) {
								indicator = TickerLastMonitorThread.this.tickerIndicatorFactory.createTickerIndicator();
								TickerLastMonitorThread.this.tickerIndicatorMap.put( key, indicator );
							}

							/* Update sma */
							indicator.addSample( pair.getValue() );
							indicatorList.add( indicator );
						}

						/* Broadcast the updates to listeners */
						TickerLastMonitorThread.this.sampleListenerManager.broadcast( indicatorList );
					}
					catch ( final Throwable t ) {
					}
					finally {
						latch.countDown();
					}
				}

				@Override
				public void error( final Throwable t ) {
					latch.countDown();
				}
			} );

			try {
				/* Wait for ticker to return */
				latch.await();
			}
			catch ( final Throwable t ) {
			}
		}
	}
}
