package coin.trader.exchange;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import coin.trader.concurrency.ResourceManager;
import coin.trader.exchange.BuyOrder.BuyOrderJobArgs;
import coin.trader.exchange.CancelOrder.CancelOrderJobArgs;
import coin.trader.exchange.ChartData.ChartDataJobArgs;
import coin.trader.exchange.ChartData.DataPoint;
import coin.trader.exchange.OpenOrder.OpenOrderJobArgs;
import coin.trader.exchange.OpenOrders.OpenOrdersJobArgs;
import coin.trader.exchange.OrderBook.OrderBookJobArgs;
import coin.trader.exchange.SellOrder.SellOrderJobArgs;
import coin.trader.logger.Logger;
import coin.trader.utilities.Callback.Return;

/**
 * This object controls the rate at which requests are sent out based on the
 * exchange. The ExchangeMonitor thread must handle the requests itself.
 * 
 * @author Evan
 *
 */
public class ExchangeMonitor extends Thread {
	private static final Logger LOGGER = new Logger( ExchangeMonitor.class.getSimpleName() );
	protected Queue<Job<?>> normalJobQueue = new ConcurrentLinkedQueue<Job<?>>();
	protected Queue<Job<?>> highJobQueue = new ConcurrentLinkedQueue<Job<?>>();
	private final Exchange exchange;
	private final Object emptyListLock = new Object();

	public ExchangeMonitor( final Exchange exchange ) {
		this.exchange = exchange;
	}

	public void submitJob( final Job<?> job ) {
		switch ( job.priority ) {
			case NORMAL: {
				this.normalJobQueue.add( job );
				break;
			}
			case HIGH: {
				this.highJobQueue.add( job );
				break;
			}
			default: {
				this.normalJobQueue.add( job );
			}
		}

		synchronized ( this.emptyListLock ) {
			this.emptyListLock.notifyAll();
		}
	}

	private Job<?> getJob() {
		Job<?> job = this.highJobQueue.poll();

		if ( job == null ) {
			job = this.normalJobQueue.poll();
		}

		return job;
	}

	@Override
	public void run() {
		final long interval = 1000 / this.exchange.getRequestsPerSecondLimit();
		ExchangeMonitor.LOGGER.config( "Starting ExchangeMontior thread for " + this.exchange.getName() + " with interval of " + interval + " ms" );
		while ( true ) {
			/* Start timestamp */
			final long startTS = System.currentTimeMillis();

			/*
			 * Performance enhancement, if there is nothing in the list, suspend
			 * the thread until an element is added.
			 */
			synchronized ( this.emptyListLock ) {
				while ( this.normalJobQueue.isEmpty() ) {
					try {
						this.emptyListLock.wait();
					}
					catch ( final InterruptedException e ) {
					}
				}
			}

			/* Execute Job */
			final Job<?> job = this.getJob();
			if ( job != null ) {
				try {
					Runnable workTask = null;
					ExchangeMonitor.LOGGER.trace( "Executing job [ " + job + " ] for " + ExchangeMonitor.this.exchange.getClass().getSimpleName() );
					switch ( job.getJobID() ) {
						case GET_TICKERS: {
							final Map<CurrencyPair, Ticker> tickers = ExchangeMonitor.this.exchange.getTickers();
							workTask = new Runnable() {
								@Override
								@SuppressWarnings( "unchecked" )
								public void run() {
									try {
										((Job<Map<CurrencyPair, Ticker>>) job).callback.succeeded( tickers );
									}
									catch ( final Throwable t ) {
										ExchangeMonitor.LOGGER.warn( "Throwable: " + t.getClass().getSimpleName() + " " + t.getMessage() );
									}
								}
							};
							break;
						}
						case GET_24_HOUR_VOLUME: {
							final Volumes tickers = ExchangeMonitor.this.exchange.get24HourVolume();
							workTask = new Runnable() {

								@Override
								@SuppressWarnings( "unchecked" )
								public void run() {
									try {
										((Job<Volumes>) job).callback.succeeded( tickers );
									}
									catch ( final Throwable t ) {
										ExchangeMonitor.LOGGER.warn( "Throwable: " + t.getClass().getSimpleName() + " " + t.getMessage() );
									}
								}
							};
							break;
						}
						case GET_ORDER_BOOK: {
							final OrderBookJobArgs jobArgs = (OrderBookJobArgs) job.getJobArgs();
							final OrderBook orderBook = ExchangeMonitor.this.exchange.getOrderBook( jobArgs.currencyPair, jobArgs.depth );
							workTask = new Runnable() {
								@Override
								@SuppressWarnings( "unchecked" )
								public void run() {
									try {
										((Job<OrderBook>) job).callback.succeeded( orderBook );
									}
									catch ( final Throwable t ) {
										ExchangeMonitor.LOGGER.warn( "Throwable: " + t.getClass().getSimpleName() + " " + t.getMessage() );
									}
								}
							};
							break;
						}
						case GET_TRADE_HISTORY: {
							/* TODO( EMU ): Implement */
							break;
						}
						case GET_CHART_DATA: {
							final ChartDataJobArgs jobArgs = (ChartDataJobArgs) job.getJobArgs();
							final List<? extends DataPoint> chartData = ExchangeMonitor.this.exchange.getChartData( jobArgs.currencyPair, jobArgs.start, jobArgs.end,
									jobArgs.period );
							workTask = new Runnable() {
								@Override
								@SuppressWarnings( "unchecked" )
								public void run() {
									try {
										((Job<List<? extends DataPoint>>) job).callback.succeeded( chartData );
									}
									catch ( final Throwable t ) {
										ExchangeMonitor.LOGGER.warn( "Throwable: " + t.getClass().getSimpleName() + " " + t.getMessage() );
									}
								}
							};
							break;
						}
						case GET_CURRENCIES: {
							/* TODO( EMU ): Implement */
							break;
						}
						case GET_LOAN_ORDERS: {
							/* TODO( EMU ): Implement */
							break;
						}
						case GET_OPEN_ORDER: {
							final OpenOrderJobArgs jobArgs = (OpenOrderJobArgs) job.getJobArgs();
							final OpenOrder openOrder = ExchangeMonitor.this.exchange.getOpenOrder( jobArgs.orderID );
							workTask = new Runnable() {
								@Override
								@SuppressWarnings( "unchecked" )
								public void run() {
									try {
										((Job<OpenOrder>) job).callback.succeeded( openOrder );
									}
									catch ( final Throwable t ) {
										ExchangeMonitor.LOGGER.warn( "Throwable: " + t.getClass().getSimpleName() + " " + t.getMessage() );
									}
								}
							};
							break;
						}
						case GET_OPEN_ORDERS: {
							final OpenOrdersJobArgs jobArgs = (OpenOrdersJobArgs) job.getJobArgs();
							final OpenOrders openOrders = ExchangeMonitor.this.exchange.getOpenOrders( jobArgs.currencyPair );
							workTask = new Runnable() {
								@Override
								@SuppressWarnings( "unchecked" )
								public void run() {
									try {
										((Job<OpenOrders>) job).callback.succeeded( openOrders );
									}
									catch ( final Throwable t ) {
										ExchangeMonitor.LOGGER.warn( "Throwable: " + t.getClass().getSimpleName() + " " + t.getMessage() );
									}

								}
							};
							break;
						}
						case PLACE_BUY_ORDER: {
							final BuyOrderJobArgs jobArgs = (BuyOrderJobArgs) job.getJobArgs();
							final BuyOrder buyOrder = ExchangeMonitor.this.exchange.placeBuyOrder( jobArgs.currencyPair, jobArgs.quantity, jobArgs.rate );
							workTask = new Runnable() {
								@Override
								@SuppressWarnings( "unchecked" )
								public void run() {
									try {
										((Job<BuyOrder>) job).callback.succeeded( buyOrder );
									}
									catch ( final Throwable t ) {
										ExchangeMonitor.LOGGER.warn( "Throwable: " + t.getClass().getSimpleName() + " " + t.getMessage() );
									}
								}
							};
							break;
						}
						case PLACE_SELL_ORDER: {
							final SellOrderJobArgs jobArgs = (SellOrderJobArgs) job.getJobArgs();
							final SellOrder sellOrder = ExchangeMonitor.this.exchange.placeSellOrder( jobArgs.currencyPair, jobArgs.quantity, jobArgs.rate );
							workTask = new Runnable() {
								@Override
								@SuppressWarnings( "unchecked" )
								public void run() {
									try {
										((Job<SellOrder>) job).callback.succeeded( sellOrder );
									}
									catch ( final Throwable t ) {
										ExchangeMonitor.LOGGER.warn( "Throwable: " + t.getClass().getSimpleName() + " " + t.getMessage() );
									}
								}
							};
							break;
						}
						case GET_BALANCES: {
							final Balances balances = ExchangeMonitor.this.exchange.getBalances();
							workTask = new Runnable() {
								@Override
								@SuppressWarnings( "unchecked" )
								public void run() {
									try {
										((Job<Balances>) job).callback.succeeded( balances );
									}
									catch ( final Throwable t ) {
										ExchangeMonitor.LOGGER.warn( "Throwable: " + t.getClass().getSimpleName() + " " + t.getMessage() );
									}
								}
							};
							break;
						}
						case CANCEL_ORDER: {
							final CancelOrderJobArgs jobArgs = (CancelOrderJobArgs) job.getJobArgs();
							final CancelOrder cancelOrder = ExchangeMonitor.this.exchange.cancelOpenOrder( jobArgs.orderID );
							workTask = new Runnable() {
								@Override
								@SuppressWarnings( "unchecked" )
								public void run() {
									try {
										((Job<CancelOrder>) job).callback.succeeded( cancelOrder );
									}
									catch ( final Throwable t ) {
										ExchangeMonitor.LOGGER.warn( "Throwable: " + t.getClass().getSimpleName() + " " + t.getMessage() );
									}
								}
							};
							break;
						}
						default: {
							job.callback.error( new Exception( "JobID was not found" ) );
							break;
						}
					}

					/* Submit worker thread */
					if ( workTask != null ) {
						ResourceManager.sumbitLightRunnable( workTask );
					}
				}
				catch ( final Exception e ) {
					job.callback.error( e );
				}
			}

			/* End timestamp */
			final long endTS = System.currentTimeMillis();

			/* Sleep if job took less than interval */
			final long elapsedTime = endTS - startTS;
			if ( elapsedTime < interval ) {
				try {
					Thread.sleep( interval - elapsedTime );
				}
				catch ( final Throwable t ) {
				}
			}
		}
	}

	public static class Job<T> {
		private final static AtomicLong counter = new AtomicLong( 0 );
		private final JobID jobID;
		private final Return<T> callback;
		private final long count;
		private final JobArgs jobArgs;
		private final JobPriority priority;

		public Job( final JobID jobID, final JobPriority priority, final JobArgs jobArgs, final Return<T> callback ) {
			this.jobID = jobID;
			this.callback = callback;
			this.jobArgs = jobArgs;
			this.count = Job.counter.incrementAndGet();
			this.priority = priority;
		}

		public JobID getJobID() {
			return this.jobID;
		}

		public Return<T> getCallback() {
			return this.callback;
		}

		public enum JobID {
			GET_TICKERS,
			GET_24_HOUR_VOLUME,
			GET_ORDER_BOOK,
			GET_TRADE_HISTORY,
			GET_CHART_DATA,
			GET_CURRENCIES,
			GET_LOAN_ORDERS,
			GET_OPEN_ORDER,
			GET_OPEN_ORDERS,
			PLACE_BUY_ORDER,
			PLACE_SELL_ORDER,
			GET_BALANCES,
			CANCEL_ORDER
		}

		public JobArgs getJobArgs() {
			return this.jobArgs;
		}

		@Override
		public String toString() {
			return "JobID: " + this.jobID + ":" + this.count + ", " + this.jobArgs;
		}

		public static abstract class JobArgs {
		}

		enum JobPriority {
			NORMAL,
			HIGH
		}
	}
}
