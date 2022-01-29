package coin.trader.algorithm;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import coin.trader.algorithm.SampleListenerManager.SampleListener;
import coin.trader.exchange.CurrencyPair;
import coin.trader.exchange.Exchange;
import coin.trader.exchange.Ticker;
import coin.trader.exchange.Trade;
import coin.trader.exchange.Transaction;
import coin.trader.exchange.TransactionHistory;
import coin.trader.logger.Logger;

public class AlgTester extends AlgExecutor {
	private static final Logger LOGGER = new Logger( AlgTester.class.getSimpleName() );

	private final TransactionHistory transactionHistory = new TransactionHistory();

	public AlgTester( final Exchange exchange, final TickerIndicatorFactory factory ) {
		super( exchange, factory );
	}

	@Override
	public void run() {
		try {
			AlgTester.LOGGER.config( "Using " + this.tickerIndicatorFactory.getAlgorithmName() + " algorithm on " + this.exchange.getName() + " exchange" );

			/* Initialize the data */
			AlgTester.LOGGER.info( "Initializing samples for " + this.exchange.getName() + " exchange..." );
			this.initSamples();
			AlgTester.LOGGER.info( "Initializing complete for " + this.exchange.getName() + " exchange" );

			/* Overwrite the balance in case we currently don't own any coins */
			this.balances = new FakeBalances( 1.0 );

			/* Loop forever */
			while ( true ) {
				/* Monitor the exchange for candidates */
				AlgTester.LOGGER.info( "Monitoring " + this.exchange.getName() + " exchange for potential currency pair candidates..." );
				final TickerIndicator tickerIndicatorCandidate = this.monitorForCandidates();
				final CurrencyPair currencyPair = tickerIndicatorCandidate.getTicker().getCurrencyPair();
				AlgTester.LOGGER.info( "Currency pair candidate " + currencyPair + " choosen on " + this.exchange.getName() + " exchange" );

				/* Execute the buy order */
				AlgTester.LOGGER.info( "Executing buy order for " + currencyPair + " on " + this.exchange.getName() + " exchange..." );
				final Trade buyOrder = this.executeBuy( tickerIndicatorCandidate );
				AlgTester.LOGGER.info( "Buy order for " + buyOrder + " completed on " + this.exchange.getName() + " exchange..." );

				/* Monitor the ticker for selling point */
				AlgTester.LOGGER.info( "Monitor exchange for selling point for " + currencyPair + "on " + this.exchange.getName() + " exchange..." );
				final Trade sellOrder = this.monitorAndExecuteSell( tickerIndicatorCandidate, buyOrder );
				AlgTester.LOGGER.info( "Sell Order for " + sellOrder + " completed on " + this.exchange.getName() + " exchange..." );

				/* Printout information about the sell */
				final Transaction transaction = new Transaction( buyOrder, sellOrder );
				AlgTester.LOGGER.info( this.exchange.getName() + " exchange transaction statistics: " + transaction );

				/* Add to transaction history */
				this.transactionHistory.addTransaction( transaction );
				AlgTester.LOGGER.info( this.exchange.getName() + " exchange transaction history: " + this.transactionHistory );

				/* Update the fake balances */
				((FakeBalances) this.balances).updateBTCAmount( this.transactionHistory.getWallet() );
			}
		}
		catch ( final Throwable t ) {
		}
	}

	@Override
	protected Trade executeBuy( final TickerIndicator tickerIndicator ) {
		/* Just pretend we bought the last price with all of our money */
		final Ticker ticker = tickerIndicator.getTicker();
		return new Trade( ticker.getCurrencyPair(), ticker.getLast(), this.transactionHistory.getWallet() / ticker.getBid(), this.exchange.getFee() );
	}

	@Override
	protected Trade monitorAndExecuteSell( final TickerIndicator tickerIndicator, final Trade buyOrder ) {
		/* Monitor the exchange for the sell signal */
		final CountDownLatch sellSignalLatch = new CountDownLatch( 1 );
		final Trade sellOrder[] = new Trade[1];
		this.tickerMonitorThread.sampleListenerManager.addSampleListener( new SampleListener() {
			@Override
			public void onNewSample( final List<TickerIndicator> tickers ) {
				if ( tickerIndicator.shouldSell( buyOrder.getRate(), AlgTester.this.exchange.getFee() ) ) {
					/* Remove this listener */
					AlgTester.this.tickerMonitorThread.sampleListenerManager.removeSampleListener( this );

					final Ticker ticker = tickerIndicator.getTicker();

					/* Pretend we sold at the current ticker price */
					sellOrder[0] = new Trade( ticker.getCurrencyPair(), ticker.getLast(), buyOrder.getQuantity() / ticker.getLast(), AlgTester.this.exchange.getFee() );

					/* Unblock the thread */
					sellSignalLatch.countDown();
				}
			}
		} );

		try {
			/* Wait for sell signal */
			sellSignalLatch.await();
		}
		catch ( final Throwable t ) {
		}

		return sellOrder[0];
	}
}
