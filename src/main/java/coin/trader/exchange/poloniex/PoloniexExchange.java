package coin.trader.exchange.poloniex;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import coin.trader.config.Config;
import coin.trader.connection.HTTPRequest;
import coin.trader.exchange.Balances;
import coin.trader.exchange.BuyOrder;
import coin.trader.exchange.CancelOrder;
import coin.trader.exchange.ChartData.DataPoint;
import coin.trader.exchange.Currency;
import coin.trader.exchange.CurrencyPair;
import coin.trader.exchange.Exchange;
import coin.trader.exchange.OpenOrder;
import coin.trader.exchange.OpenOrders;
import coin.trader.exchange.OrderBook;
import coin.trader.exchange.SellOrder;
import coin.trader.exchange.Ticker;
import coin.trader.exchange.Volumes;
import coin.trader.logger.Logger;

public class PoloniexExchange extends Exchange {
	private static final Logger LOGGER = new Logger( PoloniexExchange.class.getSimpleName() );
	private static final PoloniexExchange INSTANCE = new PoloniexExchange();
	private static final String BASE_URL = "https://poloniex.com/";
	private static final String BASE_URL_PUBLIC = PoloniexExchange.BASE_URL + "public?";
	private static final String BASE_URL_TRADING = PoloniexExchange.BASE_URL + "tradingApi?";
	private static final int MAX_REQUESTS_PER_SECOND = 6;
	private static final double TRANSACTION_FEE = 0.0025;

	private PoloniexExchange() {
	}

	public static Exchange getInstance() {
		return PoloniexExchange.INSTANCE;
	}

	@Override
	protected Map<CurrencyPair, Ticker> getTickers() throws Exception {
		final String jsonStr = this.sendCommandPublic( "command=returnTicker" );
		return new PoloniexTickers( jsonStr ).toMap();
	}

	@Override
	protected Volumes get24HourVolume() {
		return null;
	}

	@Override
	protected OrderBook getOrderBook( final CurrencyPair currencyPair, final int depth ) throws Exception {
		this.sendCommandPublic( "command=returnOrderBook&currencyPair=" + currencyPair + "&depth=" + depth );
		return null;

	}

	@Override
	protected String getTraderHistory( final CurrencyPair currencyPair, final long start, final long end ) throws Exception {
		return this.sendCommandPublic( "command=returnTradeHistory&currencyPair=" + currencyPair + "&start=" + start + "&end=" + end );

	}

	@Override
	protected List<? extends DataPoint> getChartData( final CurrencyPair currencyPair, final long start, final long end, final int period ) throws Exception {
		final String jsonStr = this.sendCommandPublic( "command=returnChartData&currencyPair=" + currencyPair + "&start=" + start + "&end=" + end + "&period=" + period );
		return new PoloniexChartData( jsonStr ).getDataPoints();

	}

	@Override
	protected String getCurrencies() throws Exception {
		return this.sendCommandPublic( "command=returnCurrencies" );

	}

	@Override
	protected String getLoanOrders( final Currency currency ) throws Exception {
		return this.sendCommandPublic( "command=returnLoanOrders&currency=" + currency );

	}

	@Override
	protected OpenOrder getOpenOrder( final String orderID ) throws Exception {
		final String jsonStr = this.sendCommandTrading( "command=returnOpenOrders&currencyPair=all" );
		final PoloniexOpenOrders openOrders = new PoloniexOpenOrders( jsonStr );
		return openOrders.getOpenOrder( orderID );

	}

	@Override
	protected CancelOrder cancelOpenOrder( final String orderID ) throws Exception {
		final String jsonStr = this.sendCommandTrading( "command=cancelOrder" );
		final Type listType = new TypeToken<PoloniexCancelOrder>() {
		}.getType();
		final Gson gson = new Gson();
		return gson.fromJson( jsonStr, listType );

	}

	@Override
	protected OpenOrders getOpenOrders( final CurrencyPair currencyPair ) throws Exception {
		final String jsonStr = this.sendCommandTrading( "command=returnOpenOrders&currencyPair=" + currencyPair );
		return new PoloniexOpenOrders( jsonStr );

	}

	@Override
	protected BuyOrder placeBuyOrder( final CurrencyPair currencyPair, final double quantity, final double rate ) throws Exception {
		final String jsonStr = this.sendCommandTrading( "command=buy&currencyPair=" + currencyPair + "amount=" + quantity + "rate=" + rate );
		final Type listType = new TypeToken<PoloniexBuyOrder>() {
		}.getType();
		final Gson gson = new Gson();
		return gson.fromJson( jsonStr, listType );

	}

	@Override
	protected SellOrder placeSellOrder( final CurrencyPair currencyPair, final double quantity, final double rate ) throws Exception {
		final String jsonStr = this.sendCommandTrading( "command=buy&currencyPair=" + currencyPair + "amount=" + quantity + "rate=" + rate );
		final Type listType = new TypeToken<PoloniexSellOrder>() {
		}.getType();
		final Gson gson = new Gson();
		return gson.fromJson( jsonStr, listType );

	}

	@Override
	protected Balances getBalances() throws Exception {

		final String jsonStr = this.sendCommandTrading( "command=returnBalances" );
		return new PoloniexBalances( jsonStr );
	}

	private String sendCommandPublic( final String queryString ) throws Exception {
		return HTTPRequest.sendHTTPRequest( PoloniexExchange.BASE_URL_PUBLIC + queryString );
	}

	private String sendCommandTrading( final String queryString ) throws Exception {
		return HTTPRequest.sendHTTPRequestWithHMAC_SHA_512POST( PoloniexExchange.BASE_URL_TRADING, queryString + "&nonce=" + System.currentTimeMillis(),
				Config.PoloniexConfig.API_KEY_PRIVATE.getValue().getBytes() );
	}

	@Override
	public int getRequestsPerSecondLimit() {
		return PoloniexExchange.MAX_REQUESTS_PER_SECOND;
	}

	@Override
	public String getName() {
		return "Poloniex";
	}

	@Override
	public double getFee() {
		return PoloniexExchange.TRANSACTION_FEE;
	}

	@Override
	public boolean checkConfig() {
		final boolean publicAPIKey = !Config.PoloniexConfig.API_KEY_PUBLIC.getValue().isEmpty();
		final boolean privateAPIKey = !Config.PoloniexConfig.API_KEY_PRIVATE.getValue().isEmpty();

		return publicAPIKey && privateAPIKey;
	}

	@Override
	public String toString() {
		return this.getName();
	}
}
