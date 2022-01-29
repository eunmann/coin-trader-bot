package coin.trader.algorithm.history;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import coin.trader.algorithm.history.HistoryListenerManager.HistoryIndicatorListener;
import coin.trader.exchange.Balances;
import coin.trader.exchange.CurrencyPair;
import coin.trader.exchange.Exchange;
import coin.trader.exchange.Ticker;
import coin.trader.exchange.Trade;
import coin.trader.utilities.Callback.Return;

public class HistoryAlgTester extends HistoryAlgExecutor {

	private Trade buy;
	private Trade sell;

	public HistoryAlgTester( final Exchange exchange, final HistoryTickerIndicatorFactory historyTickerIndicatorFactory ) {
		super( exchange, historyTickerIndicatorFactory );
	}

	@Override
	protected void updateBalances() throws InterruptedException {
	}

	@Override
	public Trade buy( final HistoryTickerIndicator historyTickerIndicator ) throws InterruptedException {
		final CurrencyPair currencyPair = historyTickerIndicator.getCurrencyPair();

		/* Get the current trade price */
		HistoryAlgExecutor.LOGGER.debug( "Getting latest ticker" );
		final CountDownLatch tickerPriceLatch = new CountDownLatch( 1 );
		final Ticker[] tickerP = new Ticker[1];
		HistoryAlgTester.this.exchange.getTickers( new Return<Map<CurrencyPair, Ticker>>() {

			@Override
			public void succeeded( final Map<CurrencyPair, Ticker> tickers ) {
				tickerP[0] = tickers.get( currencyPair );
				tickerPriceLatch.countDown();
			}

			@Override
			public void error( final Throwable t ) {
				if ( historyTickerIndicator.shouldBuy() >= 0 ) {
					HistoryAlgTester.this.exchange.getTickers( this );
				}
			}
		} );

		tickerPriceLatch.await();

		if ( tickerP[0] == null ) {
			return null;
		}
		final double rate = tickerP[0].getLast();
		final double quantity = (1.0 / rate) / (1.0 + this.exchange.getFee());

		this.buy = new Trade( currencyPair, rate, quantity, HistoryAlgTester.this.exchange.getFee() );

		return this.buy;
	}

	@Override
	protected Trade monitorAndExecuteSell( final HistoryTickerIndicator historyTickerIndicator, final Trade buyOrder ) throws InterruptedException {
		final CurrencyPair currencyPair = historyTickerIndicator.getCurrencyPair();

		/* Monitor the exchange for the sell signal */
		final CountDownLatch sellSignalLatch = new CountDownLatch( 1 );

		this.historyMonitor.getHistoryListenerManager().add( new HistoryIndicatorListener() {

			@Override
			public void onSellTrigger( final HistoryTickerIndicator triggedHistoryTickerIndicator ) {
				if ( triggedHistoryTickerIndicator.equals( historyTickerIndicator ) ) {
					HistoryAlgTester.this.historyMonitor.getHistoryListenerManager().remove( this );
					sellSignalLatch.countDown();
				}
			}

			@Override
			public void onBuyTrigger( final List<HistoryTickerIndicator> historyTickerIndicator ) {

			}
		} );

		/* Wait for sell signal */
		sellSignalLatch.await();
		HistoryAlgExecutor.LOGGER.debug( "Sell signal triggered" );

		/* Get the current trade price */
		HistoryAlgExecutor.LOGGER.debug( "Getting latest ticker" );
		final CountDownLatch tickerPriceLatch = new CountDownLatch( 1 );
		final Ticker[] tickerP = new Ticker[1];
		HistoryAlgTester.this.exchange.getTickers( new Return<Map<CurrencyPair, Ticker>>() {

			@Override
			public void succeeded( final Map<CurrencyPair, Ticker> tickers ) {

				tickerP[0] = tickers.get( currencyPair );
				tickerPriceLatch.countDown();
			}

			@Override
			public void error( final Throwable t ) {
				HistoryAlgTester.this.exchange.getTickers( this );
			}
		} );

		tickerPriceLatch.await();

		final double rate = tickerP[0].getLast();
		final double quantity = (buyOrder.getQuantity()) / (1.0 + this.exchange.getFee());

		this.sell = new Trade( currencyPair, rate, quantity, HistoryAlgTester.this.exchange.getFee() );

		return this.sell;
	}

	@Override
	protected void printStatistics( final Balances oldBalances ) {
		final boolean wasGood = this.buy.getBaseQuantityWithFee() < this.sell.getBaseQuantityWithFee();

		HistoryAlgExecutor.LOGGER.info( "Before Buy Quantity: " + this.buy.getBaseQuantityWithFee() );
		HistoryAlgExecutor.LOGGER.info( "After Sell Quantity: " + this.sell.getBaseQuantityWithFee() );
		HistoryAlgExecutor.LOGGER.info( "It was a " + (wasGood ? "GOOD" : "BAD") + " buy" );
	}
}
