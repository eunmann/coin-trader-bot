package coin.trader.exchange.bittrex;

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
import coin.trader.exchange.bittrex.BittrexOpenOrder.BittrexOpenOrderResponse;
import coin.trader.logger.Logger;

public class BittrexExchange extends Exchange {
	private static final Logger LOGGER = new Logger( BittrexExchange.class.getSimpleName() );
	private static final int REQUESTS_PER_SECOND = 10;
	private static final BittrexExchange INSTANCE = new BittrexExchange();
	private static final String BASE_URL = "https://bittrex.com/api/v1.1/";
	private static final String BASE_URL_PUBLIC = BittrexExchange.BASE_URL + "public/";
	private static final String BASE_URL_ACCOUNT = BittrexExchange.BASE_URL + "account/";
	private static final String BASE_URL_MARKET = BittrexExchange.BASE_URL + "market/";
	private static final double TRANSACTION_FEE = 0.0025;

	private BittrexExchange() {
	}

	@Override
	protected Map<CurrencyPair, Ticker> getTickers() throws Exception {

		final String jsonStr = this.sendCommandPublic( "getmarketsummaries?" );
		final Type listType = new TypeToken<BittrexTickers>() {
		}.getType();
		final Gson gson = new Gson();
		final BittrexTickers tickers = gson.fromJson( jsonStr, listType );
		if ( tickers.success ) {
			return tickers.toMap();
		}
		else {
			throw new Exception( tickers.message );
		}
	}

	@Override
	protected Volumes get24HourVolume() throws Exception {
		return null;
	}

	@Override
	protected OrderBook getOrderBook( final CurrencyPair currencyPair, final int depth ) throws Exception {

		final String jsonStr = this.sendCommandPublic( "getorderbook?market=" + currencyPair + "&type=both" );
		final Type listType = new TypeToken<BittrexOrderBook>() {
		}.getType();
		final Gson gson = new Gson();
		final BittrexOrderBook orderBook = gson.fromJson( jsonStr, listType );
		if ( orderBook.success ) {
			return orderBook;
		}
		else {
			throw new Exception( orderBook.message );
		}
	}

	@Override
	protected Balances getBalances() throws Exception {
		final String jsonStr = this.sendCommandAccount( "getbalances?" );
		final Type listType = new TypeToken<BittrexBalances>() {
		}.getType();
		final Gson gson = new Gson();
		final BittrexBalances balances = gson.fromJson( jsonStr, listType );
		if ( balances.success ) {
			return balances;
		}
		else {
			throw new Exception( balances.message );
		}

	}

	@Override
	protected String getTraderHistory( final CurrencyPair currencyPair, final long start, final long end ) throws Exception {
		return null;
	}

	@Override
	protected List<DataPoint> getChartData( final CurrencyPair currencyPair, final long start, final long end, final int period ) throws Exception {
		return null;
	}

	@Override
	protected String getCurrencies() throws Exception {
		return null;
	}

	@Override
	protected String getLoanOrders( final Currency currency ) throws Exception {
		return null;
	}

	@Override
	protected OpenOrder getOpenOrder( final String orderID ) throws Exception {
		final String jsonStr = this.sendCommandAccount( "getorder?uuid=" + orderID );
		final Type listType = new TypeToken<BittrexOpenOrderResponse>() {
		}.getType();
		final Gson gson = new Gson();
		final BittrexOpenOrderResponse openOrder = gson.fromJson( jsonStr, listType );
		if ( openOrder.success ) {
			return openOrder.toOpenOrder();
		}
		else {
			throw new Exception( openOrder.message );
		}
	}

	@Override
	protected OpenOrders getOpenOrders( final CurrencyPair currencyPair ) throws Exception {
		final String jsonStr = this.sendCommandMarket( "getopenorders?market=" + currencyPair );
		final Type listType = new TypeToken<BittrexOpenOrders>() {
		}.getType();
		final Gson gson = new Gson();
		final BittrexOpenOrders openOrders = gson.fromJson( jsonStr, listType );
		if ( openOrders.success ) {
			return openOrders;
		}
		else {
			throw new Exception( openOrders.message );
		}
	}

	@Override
	protected BuyOrder placeBuyOrder( final CurrencyPair currencyPair, final double quantity, final double rate ) throws Exception {
		final String jsonStr = this.sendCommandMarket( "buylimit?market=" + currencyPair + "&quantity=" + quantity + "&rate=" + rate );
		final Type listType = new TypeToken<BittrexBuyOrder>() {
		}.getType();
		final Gson gson = new Gson();
		final BittrexBuyOrder buyOrder = gson.fromJson( jsonStr, listType );
		if ( buyOrder.success ) {
			return buyOrder;
		}
		else {
			throw new Exception( buyOrder.message );
		}
	}

	@Override
	protected SellOrder placeSellOrder( final CurrencyPair currencyPair, final double quantity, final double rate ) throws Exception {

		final String jsonStr = this.sendCommandMarket( "selllimit?market=" + currencyPair + "&quantity=" + quantity + "&rate=" + rate );
		final Type listType = new TypeToken<BittrexSellOrder>() {
		}.getType();
		final Gson gson = new Gson();
		final BittrexSellOrder buyOrder = gson.fromJson( jsonStr, listType );
		if ( buyOrder.success ) {
			return buyOrder;
		}
		else {
			throw new Exception( buyOrder.message );
		}

	}

	@Override
	protected CancelOrder cancelOpenOrder( final String orderID ) throws Exception {
		final String jsonStr = this.sendCommandMarket( "cancel?uuid=" + orderID );
		final Type listType = new TypeToken<BittrexCancelOrder>() {
		}.getType();
		final Gson gson = new Gson();
		final BittrexCancelOrder cancelOrder = gson.fromJson( jsonStr, listType );
		if ( cancelOrder.success ) {
			return cancelOrder;
		}
		else {
			throw new Exception( cancelOrder.message );
		}
	}

	@Override
	public int getRequestsPerSecondLimit() {
		return BittrexExchange.REQUESTS_PER_SECOND;
	}

	@Override
	public String getName() {
		return "Bittrex";
	}

	public static Exchange getInstance() {
		return BittrexExchange.INSTANCE;
	}

	private String sendCommandPublic( final String queryString ) throws Exception {
		return HTTPRequest.sendHTTPRequest( BittrexExchange.BASE_URL_PUBLIC + queryString );
	}

	private String sendCommandAccount( final String queryString ) throws Exception {
		return HTTPRequest.sendHTTPRequestWithHMAC_SHA_512(
				BittrexExchange.BASE_URL_ACCOUNT + queryString + "&apikey=" + Config.BittrexConfig.API_KEY_PUBLIC.getValue() + "&nonce=" + System.currentTimeMillis(),
				Config.BittrexConfig.API_KEY_PRIVATE.getValue().getBytes() );
	}

	private String sendCommandMarket( final String queryString ) throws Exception {
		return HTTPRequest.sendHTTPRequestWithHMAC_SHA_512(
				BittrexExchange.BASE_URL_MARKET + queryString + "&apikey=" + Config.BittrexConfig.API_KEY_PUBLIC.getValue() + "&nonce=" + System.currentTimeMillis(),
				Config.BittrexConfig.API_KEY_PRIVATE.getValue().getBytes() );
	}

	@Override
	public double getFee() {
		return BittrexExchange.TRANSACTION_FEE;
	}

	@Override
	public boolean checkConfig() {
		final boolean containsKeys = !Config.BittrexConfig.API_KEY_PUBLIC.getValue().isEmpty() && !Config.BittrexConfig.API_KEY_PRIVATE.getValue().isEmpty();

		return containsKeys;
	}
}
