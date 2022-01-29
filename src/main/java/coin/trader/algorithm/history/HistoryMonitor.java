package coin.trader.algorithm.history;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import coin.trader.exchange.ChartData.DataPoint;
import coin.trader.exchange.CurrencyPair;
import coin.trader.exchange.Exchange;
import coin.trader.exchange.Ticker;
import coin.trader.logger.Logger;
import coin.trader.utilities.Callback.Return;
import coin.trader.utilities.Utils;

public class HistoryMonitor {
	private static final Logger LOGGER = new Logger( HistoryMonitor.class.getSimpleName() );
	private static final long END = 0x7FFFFFFFFFFFFFFFL;

	private final UpdateThread thread = new UpdateThread();
	private final Exchange exchange;
	private final HistoryTickerIndicatorFactory historyIndicatorFactory;
	private final HistoryListenerManager listenerManager = new HistoryListenerManager();

	public HistoryMonitor( final Exchange exchange, final HistoryTickerIndicatorFactory historyIndicatorFactory ) {
		this.exchange = exchange;
		this.historyIndicatorFactory = historyIndicatorFactory;
	}

	public void start() {
		if ( !this.thread.isAlive() ) {
			this.thread.start();
		}
	}

	public HistoryListenerManager getHistoryListenerManager() {
		return this.listenerManager;
	}

	private class UpdateThread extends Thread {
		private UpdateThread() {
		}

		@Override
		public void run() {
			try {
				HistoryMonitor.LOGGER.info( "Starting HistoryMonitor thread" );
				/* Get all currency pairs */
				final CountDownLatch currencyPairLatch = new CountDownLatch( 1 );
				final List<HistoryTickerIndicator> historyTickerIndicators = new ArrayList<HistoryTickerIndicator>();

				HistoryMonitor.LOGGER.debug( "Getting ticker names" );
				HistoryMonitor.this.exchange.getTickers( new Return<Map<CurrencyPair, Ticker>>() {
					@Override
					public void succeeded( final Map<CurrencyPair, Ticker> tickers ) {

						for ( final CurrencyPair currencyPair : tickers.keySet() ) {
							historyTickerIndicators.add( HistoryMonitor.this.historyIndicatorFactory.createHistoryTickerIndicator( currencyPair ) );
						}

						currencyPairLatch.countDown();
					}

					@Override
					public void error( final Throwable t ) {
						HistoryMonitor.LOGGER.warn( "", t );
					}
				} );

				currencyPairLatch.await();

				final long intervalLength = HistoryMonitor.this.historyIndicatorFactory.amountOfCandles() * HistoryMonitor.this.historyIndicatorFactory.candlePeriod();

				while ( true ) {
					final CountDownLatch latch = new CountDownLatch( 1 );

					/* Get the latest prices */
					HistoryMonitor.LOGGER.debug( "Getting ticker prices" );
					HistoryMonitor.this.exchange.getTickers( new Return<Map<CurrencyPair, Ticker>>() {

						@Override
						public void succeeded( final Map<CurrencyPair, Ticker> tickers ) {
							HistoryMonitor.LOGGER.debug( "Getting chart data for all tickers" );

							/*
							 * Issue requests to get all currency pairs char
							 * data
							 */
							final CountDownLatch chartDataLatch = new CountDownLatch( historyTickerIndicators.size() );

							final long start = Utils.getUnixTimestamp() - intervalLength;
							final List<HistoryTickerIndicator> buyList = new CopyOnWriteArrayList<HistoryTickerIndicator>();

							for ( final HistoryTickerIndicator historyTickerIndicator : historyTickerIndicators ) {
								final CurrencyPair currencyPair = historyTickerIndicator.getCurrencyPair();
								HistoryMonitor.this.exchange.getChartData( currencyPair, start, HistoryMonitor.END, HistoryMonitor.this.historyIndicatorFactory.candlePeriod(),
										new Return<List<? extends DataPoint>>() {
											@Override
											public void succeeded( final List<? extends DataPoint> chartData ) {
												/*
												 * On each return, run
												 * HistoryTickerIndicator
												 * against it
												 */
												historyTickerIndicator.anaylize( chartData, tickers.get( currencyPair ).getLast() );

												if ( historyTickerIndicator.shouldBuy() > 0.0 ) {
													buyList.add( historyTickerIndicator );
												}

												/* Notify if it's a good sell */
												final double sellWeight = historyTickerIndicator.shouldSell();
												if ( sellWeight > 0.0 ) {
													HistoryMonitor.this.listenerManager.notifySellTrigger( historyTickerIndicator );
												}

												chartDataLatch.countDown();
											}

											@Override
											public void error( final Throwable t ) {
												HistoryMonitor.LOGGER.warn( "", t );
												HistoryMonitor.this.exchange.getChartData( historyTickerIndicator.getCurrencyPair(), start, HistoryMonitor.END,
														HistoryMonitor.this.historyIndicatorFactory.candlePeriod(), this );
											}
										} );
							}

							try {
								chartDataLatch.await();
							}
							catch ( final InterruptedException e ) {
							}

							if ( buyList.size() > 0 ) {
								/* TODO(EMU): Sort these */

								/* Notify the buy triggers */
								HistoryMonitor.this.listenerManager.notifyBuyTrigger( buyList );
								HistoryMonitor.LOGGER.debug( "Notified buy triggers" );
							}

							latch.countDown();
						}

						@Override
						public void error( final Throwable t ) {
							HistoryMonitor.LOGGER.warn( "", t );

						}
					} );

					latch.await();
				}

			}
			catch ( final Throwable t ) {

			}
		}
	}
}
