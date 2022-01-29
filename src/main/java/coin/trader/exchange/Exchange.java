package coin.trader.exchange;

import java.util.List;
import java.util.Map;

import coin.trader.exchange.BuyOrder.BuyOrderJobArgs;
import coin.trader.exchange.CancelOrder.CancelOrderJobArgs;
import coin.trader.exchange.ChartData.ChartDataJobArgs;
import coin.trader.exchange.ChartData.DataPoint;
import coin.trader.exchange.ExchangeMonitor.Job;
import coin.trader.exchange.ExchangeMonitor.Job.JobArgs;
import coin.trader.exchange.ExchangeMonitor.Job.JobID;
import coin.trader.exchange.ExchangeMonitor.Job.JobPriority;
import coin.trader.exchange.OpenOrder.OpenOrderJobArgs;
import coin.trader.exchange.OpenOrders.OpenOrdersJobArgs;
import coin.trader.exchange.OrderBook.OrderBookJobArgs;
import coin.trader.exchange.SellOrder.SellOrderJobArgs;
import coin.trader.logger.Logger;
import coin.trader.utilities.Callback.Return;

/*
 * TODO( EMU ): Need to create interfaces for the methods that still return a
 * String
 */

/**
 * Interface for exchanges. Public methods submit jobs to the exchanges's
 * monitor thread.
 * 
 * @author Evan
 *
 */
public abstract class Exchange {
	private static final Logger LOGGER = new Logger( Exchange.class.getSimpleName() );
	protected ExchangeMonitor monitor;

	protected Exchange() {
		this.monitor = new ExchangeMonitor( this );
		this.monitor.start();
	}

	/**
	 * 
	 * @param callback
	 */
	public final void getTickers( final Return<Map<CurrencyPair, Ticker>> callback ) {
		final Job<Map<CurrencyPair, Ticker>> job = new Job<Map<CurrencyPair, Ticker>>( JobID.GET_TICKERS, JobPriority.NORMAL, null, callback );
		this.submitJob( job );
	}

	/**
	 * 
	 * @param callback
	 */
	public final void get24HourVolume( final Return<Volumes> callback ) {
		final Job<Volumes> job = new Job<Volumes>( JobID.GET_24_HOUR_VOLUME, JobPriority.NORMAL, null, callback );
		this.submitJob( job );
	}

	/**
	 * 
	 * @param currencyPair
	 * @param start
	 *            - Unix Timestamp in seconds since epoch
	 * @param end
	 *            - Unix Timestamp in seconds since epoch
	 * @param period
	 *            - in seconds
	 * @param callback
	 */
	public final void getChartData( final CurrencyPair currencyPair, final long start, final long end, final int period, final Return<List<? extends DataPoint>> callback ) {
		final JobArgs jobArgs = new ChartDataJobArgs( currencyPair, start, end, period );
		final Job<List<? extends DataPoint>> job = new Job<List<? extends DataPoint>>( JobID.GET_CHART_DATA, JobPriority.NORMAL, jobArgs, callback );
		this.submitJob( job );
	}

	public final void getOrderBook( final CurrencyPair currencyPair, final int depth, final Return<OrderBook> callback ) {
		final Job<OrderBook> job = new Job<OrderBook>( JobID.GET_ORDER_BOOK, JobPriority.NORMAL, new OrderBookJobArgs( currencyPair, depth ), callback );
		this.submitJob( job );
	}

	public final void getOpenOrder( final String orderID, final Return<OpenOrder> callback ) {
		final Job<OpenOrder> job = new Job<OpenOrder>( JobID.GET_OPEN_ORDER, JobPriority.NORMAL, new OpenOrderJobArgs( orderID ), callback );
		this.submitJob( job );
	}

	public final void getOpenOrders( final CurrencyPair currencyPair, final Return<OpenOrders> callback ) {
		final Job<OpenOrders> job = new Job<OpenOrders>( JobID.GET_OPEN_ORDERS, JobPriority.NORMAL, new OpenOrdersJobArgs( currencyPair ), callback );
		this.submitJob( job );
	}

	public final void placeBuyOrder( final CurrencyPair currencyPair, final double quantity, final double rate, final Return<BuyOrder> callback ) {
		final Job<BuyOrder> job = new Job<BuyOrder>( JobID.PLACE_BUY_ORDER, JobPriority.HIGH, new BuyOrderJobArgs( currencyPair, quantity, rate ), callback );
		this.submitJob( job );
	}

	public final void placeSellOrder( final CurrencyPair currencyPair, final double quantity, final double rate, final Return<SellOrder> callback ) {
		final Job<SellOrder> job = new Job<SellOrder>( JobID.PLACE_SELL_ORDER, JobPriority.HIGH, new SellOrderJobArgs( currencyPair, quantity, rate ), callback );
		this.submitJob( job );
	}

	public final void cancelOpenOrder( final String orderID, final Return<CancelOrder> callback ) {
		final Job<CancelOrder> job = new Job<CancelOrder>( JobID.CANCEL_ORDER, JobPriority.HIGH, new CancelOrderJobArgs( orderID ), callback );
		this.submitJob( job );
	}

	public final void getBalances( final Return<Balances> callback ) {
		final Job<Balances> job = new Job<Balances>( JobID.GET_BALANCES, JobPriority.NORMAL, null, callback );
		this.submitJob( job );
	}

	/*
	 * ///////////////////////////--------------------------------\\\\\\\\\\\\\\
	 * 
	 * TODO( EMU ): Implement the below functions correctly
	 * 
	 * \\\\\\\\\\\\\\\\\\\\\\\\\\\---------------------------------/////////////
	 */

	public final void getTradeHistory( final CurrencyPair currencyPair, final long start, final long end, final Return<String> callback ) {
		final Job<String> job = new Job<String>( JobID.GET_TICKERS, JobPriority.NORMAL, null, callback );
		this.submitJob( job );
	}

	public final void getCurrencies( final Return<String> callback ) {
		final Job<String> job = new Job<String>( JobID.GET_TICKERS, JobPriority.NORMAL, null, callback );
		this.submitJob( job );
	}

	public final void getLoanOrders( final Currency currency, final Return<String> callback ) {
		final Job<String> job = new Job<String>( JobID.GET_TICKERS, JobPriority.NORMAL, null, callback );
		this.submitJob( job );
	}

	private final void submitJob( final Job<?> job ) {
		Exchange.LOGGER.trace( "Submitting job [ " + job + " ] for " + this.getClass().getSimpleName() );
		this.monitor.submitJob( job );
	}

	/* The below methods do the actual requests */
	protected abstract Map<CurrencyPair, Ticker> getTickers() throws Exception;

	protected abstract Volumes get24HourVolume() throws Exception;

	protected abstract OrderBook getOrderBook( final CurrencyPair currencyPair, final int depth ) throws Exception;

	protected abstract String getTraderHistory( final CurrencyPair currencyPair, final long start, final long end ) throws Exception;

	protected abstract List<? extends DataPoint> getChartData( final CurrencyPair currencyPair, final long start, final long end, final int period ) throws Exception;

	protected abstract String getCurrencies() throws Exception;

	protected abstract String getLoanOrders( final Currency currency ) throws Exception;

	protected abstract OpenOrders getOpenOrders( final CurrencyPair currencyPair ) throws Exception;

	protected abstract OpenOrder getOpenOrder( final String orderID ) throws Exception;

	protected abstract BuyOrder placeBuyOrder( final CurrencyPair currencyPair, final double quantity, final double rate ) throws Exception;

	protected abstract SellOrder placeSellOrder( final CurrencyPair currencyPair, final double quantity, final double rate ) throws Exception;

	protected abstract CancelOrder cancelOpenOrder( final String orderID ) throws Exception;

	protected abstract Balances getBalances() throws Exception;

	/* Information about the exchange */
	public abstract int getRequestsPerSecondLimit();

	public abstract String getName();

	public abstract double getFee();

	public abstract boolean checkConfig();

	public void shutdown() {
		this.monitor.interrupt();
	}
}
