package coin.trader.algorithm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import coin.trader.algorithm.SampleListenerManager.SampleListener;
import coin.trader.exchange.Balance;
import coin.trader.exchange.Balances;
import coin.trader.exchange.BuyOrder;
import coin.trader.exchange.CancelOrder;
import coin.trader.exchange.CurrencyPair;
import coin.trader.exchange.Exchange;
import coin.trader.exchange.OpenOrder;
import coin.trader.exchange.SellOrder;
import coin.trader.exchange.Trade;
import coin.trader.exchange.TradeStatistic;
import coin.trader.logger.Logger;
import coin.trader.utilities.ArrayUtils;
import coin.trader.utilities.Callback.Return;

/**
 * @author Evan
 */
public class AlgExecutor extends Thread {
	private static final Logger LOGGER = new Logger( AlgExecutor.class.getSimpleName(), Logger.Level.DEBUG );
	private static final int RETRY_TIME_PERIOD = 2;

	/* Algorithm */
	protected final TickerMonitorThread tickerMonitorThread;
	protected final TickerIndicatorFactory tickerIndicatorFactory;

	protected final Exchange exchange;

	protected Balances balances = null;

	public AlgExecutor( final Exchange exchange, final TickerIndicatorFactory tickerIndicatorFactory ) {
		this.exchange = exchange;
		this.tickerIndicatorFactory = tickerIndicatorFactory;
		this.tickerMonitorThread = tickerIndicatorFactory.getTickerMonitorThread( exchange );
	}

	@Override
	public void run() {
		/*
		 * Check the configuration settings of the exchange to ensure we can run
		 */
		if ( !this.exchange.checkConfig() ) {
			AlgExecutor.LOGGER.error( this.exchange.getName() + " configuration parameters are empty or invalid. Please input value for settings. Aborting "
					+ AlgExecutor.class.getSimpleName() + " for exchange " + this.exchange.getName() );
			return;
		}

		try {
			AlgExecutor.LOGGER.config( "Using " + this.tickerIndicatorFactory.getAlgorithmName() + " algorithm on " + this.exchange.getName() + " exchange" );

			/* Retrieve the balances */
			AlgExecutor.LOGGER.info( this.formatLog( "Retrieving balances..." ) );
			this.updateBalances();
			AlgExecutor.LOGGER.info( this.formatLog( "Balances retrieved" ) );

			/* Initialize the data */
			AlgExecutor.LOGGER.info( this.formatLog( "Initializing samples..." ) );
			this.initSamples();
			AlgExecutor.LOGGER.info( this.formatLog( "Initializing complete" ) );

			/* Loop forever */
			while ( true ) {
				/* Monitor the exchange for candidates */
				AlgExecutor.LOGGER.info( this.formatLog( "Monitoring potential currency pair candidates..." ) );
				final TickerIndicator tickerIndicatorCandidate = this.monitorForCandidates();
				final CurrencyPair currencyPair = tickerIndicatorCandidate.getTicker().getCurrencyPair();
				AlgExecutor.LOGGER.info( this.formatLog( "Currency pair candidate " + currencyPair + " choosen" ) );

				/* Execute the buy order */
				AlgExecutor.LOGGER.info( this.formatLog( "Executing buy order for " + currencyPair + "..." ) );
				final Trade buyOrder = this.executeBuy( tickerIndicatorCandidate );

				if ( buyOrder != null ) {
					AlgExecutor.LOGGER.info( this.formatLog( "Buy order completed " + buyOrder ) );

					/* Monitor the ticker for selling point */
					AlgExecutor.LOGGER.info( this.formatLog( "Monitoring exchange for selling point for " + currencyPair + "..." ) );
					final Trade sellOrder = this.monitorAndExecuteSell( tickerIndicatorCandidate, buyOrder );
					AlgExecutor.LOGGER.info( this.formatLog( "Sell order completed " + sellOrder ) );

					/* Update the balances */

					/*
					 * Balances do not update fast enough, need to sleep here so
					 * server updates
					 */
					Thread.sleep( 2000 );
					final Balances oldBalances = this.balances;
					AlgExecutor.LOGGER.info( this.formatLog( "Updating balances..." ) );
					this.updateBalances();
					AlgExecutor.LOGGER.info( this.formatLog( "Balances updated" ) );

					/* Print the statistics of the trade */
					this.printStatistics( oldBalances );
				}
				else {
					AlgExecutor.LOGGER.info( this.formatLog( "Missed buy price for " + currencyPair ) );
				}
			}
		}
		catch ( final Throwable t ) {
			AlgExecutor.LOGGER.warn( "Exiting logic thread", t );
		}
	}

	protected void updateBalances() throws InterruptedException {
		/* Get your current balances */
		final CountDownLatch balanceLatch = new CountDownLatch( 1 );
		this.exchange.getBalances( new Return<Balances>() {
			@Override
			public void succeeded( final Balances balances ) {
				AlgExecutor.this.balances = balances;
				balanceLatch.countDown();
			}

			@Override
			public void error( final Throwable t ) {
				AlgExecutor.this.exchange.getBalances( this );
			}
		} );

		/* Wait for the balances */
		balanceLatch.await();
	}

	/**
	 * Prints the statistics of the inputed balances to the current balances
	 *
	 * 
	 * @param oldBalances
	 */
	private void printStatistics( final Balances oldBalances ) {
		final List<Balance> oldBalancesList = oldBalances.getAllBalances();
		final List<Balance> newBalancesList = this.balances.getAllBalances();
		final List<TradeStatistic> diffBalanceList = new ArrayList<TradeStatistic>();

		for ( int i = 0, len1 = oldBalancesList.size(); i < len1; i++ ) {
			final Balance oldBalance = oldBalancesList.get( i );
			for ( int j = 0, len2 = newBalancesList.size(); j < len2; j++ ) {
				final Balance newBalance = newBalancesList.get( j );
				if ( oldBalance.equals( newBalance ) && oldBalance.getAmount() != newBalance.getAmount() ) {
					diffBalanceList.add( new TradeStatistic( oldBalance.getCurrency(), oldBalance.getAmount(), newBalance.getAmount() ) );
				}
			}
		}

		AlgExecutor.LOGGER.info( this.formatLog( "Balance Transaction Statistics: " + ArrayUtils.toString( diffBalanceList ) ) );
	}

	/**
	 * Init the data.
	 * 
	 * @return
	 */
	protected void initSamples() {
		/* Initialize the samples */
		final CountDownLatch latch = new CountDownLatch( this.tickerIndicatorFactory.getAmountOfSamples() );
		this.tickerMonitorThread.sampleListenerManager.addSampleListener( new SampleListener() {
			@Override
			public void onNewSample( final List<TickerIndicator> indicators ) {
				latch.countDown();

				if ( latch.getCount() == 0 ) {
					AlgExecutor.this.tickerMonitorThread.sampleListenerManager.removeSampleListener( this );
				}
			}
		} );

		/* Start the TickerMonitorThread */
		this.tickerMonitorThread.start();

		try {
			/* Wait for initialization to complete */
			latch.await();
		}
		catch ( final Throwable t ) {
		}
	}

	/**
	 * Using the given map, monitor and update the map. When candidates are
	 * found, the best one is returned.
	 * 
	 * @param smaMap
	 * @return
	 */
	protected TickerIndicator monitorForCandidates() {
		final List<TickerIndicator> candidateList = new ArrayList<TickerIndicator>();
		final CountDownLatch latch = new CountDownLatch( 1 );
		this.tickerMonitorThread.sampleListenerManager.addSampleListener( new SampleListener() {
			@Override
			public void onNewSample( final List<TickerIndicator> indicators ) {
				final Iterator<TickerIndicator> iter = indicators.iterator();
				while ( iter.hasNext() ) {
					final TickerIndicator indicator = iter.next();
					if ( indicator.shouldBuy() && AlgExecutor.this.balances.getBalance( indicator.getTicker().getCurrencyPair().getBase() ).getAmount() > 0.0
							&& indicator.getTicker().getCurrencyPair().getBase().getName().equals( "BTC" ) ) {
						candidateList.add( indicator );
					}
				}

				/* If we found candidates, unlock the thread */
				if ( !candidateList.isEmpty() ) {
					/* Remove this listener */
					AlgExecutor.this.tickerMonitorThread.sampleListenerManager.removeSampleListener( this );
					latch.countDown();
				}
			}
		} );

		try {
			/* Wait for buy signal */
			latch.await();
		}
		catch ( final Throwable t ) {
		}

		/* Get best candidates */
		TickerIndicator candidate = candidateList.get( 0 );
		double candidacyWeight = candidate.getCandidacyWeight();

		for ( final TickerIndicator indicator : candidateList ) {
			final double candidacyWeight2 = indicator.getCandidacyWeight();
			if ( candidacyWeight2 > candidacyWeight ) {
				candidate = indicator;
				candidacyWeight = candidacyWeight2;
			}
		}

		AlgExecutor.LOGGER.debug( this.formatLog( "Choosing candadite with weight of " + candidacyWeight ) );

		return candidate;
	}

	/**
	 * Executes a buy order and returns when complete
	 * 
	 * @param currencyPair
	 */
	protected Trade executeBuy( final TickerIndicator tickerIndicator ) {
		final CurrencyPair currencyPair = tickerIndicator.getTicker().getCurrencyPair();

		/* Loop until we got the buy or should opt out */
		while ( true ) {
			/* Place the order for everything that we got */
			final CountDownLatch placeBuyOrderLatch = new CountDownLatch( 1 );
			final double rate = tickerIndicator.getTicker().getLast();
			final double quantity = (this.balances.getBalance( currencyPair.getBase() ).getAmount() / rate) / (1.0 + this.exchange.getFee());
			final BuyOrder[] buyOrderRef = new BuyOrder[1];

			AlgExecutor.LOGGER.debug( this.formatLog( "Placing buy order [currency pair, quantity, rate] = [ " + currencyPair + ", " + quantity + ", " + rate + " ]" ) );
			AlgExecutor.this.exchange.placeBuyOrder( currencyPair, quantity, rate, new Return<BuyOrder>() {
				@Override
				public void succeeded( final BuyOrder buyOrder ) {
					if ( buyOrder.isPlaced() ) {
						buyOrderRef[0] = buyOrder;
						placeBuyOrderLatch.countDown();
					}
					else {
						AlgExecutor.this.exchange.placeBuyOrder( currencyPair, quantity, rate, this );
					}
				}

				@Override
				public void error( final Throwable t ) {
					AlgExecutor.this.interrupt();
				}
			} );

			try {
				/* Wait until we have placed the order */
				placeBuyOrderLatch.await();
			}
			catch ( final Throwable t ) {

				return null;
			}

			/* Wait for the order to complete */
			final CountDownLatch watchBuyOrderLatch = new CountDownLatch( 1 );
			final Trade buyOrder[] = new Trade[1];

			AlgExecutor.LOGGER.debug( this.formatLog( "Buy order placed. Waiting for buy order to fill..." ) );
			this.exchange.getOpenOrder( buyOrderRef[0].getID(), new Return<OpenOrder>() {
				final AtomicInteger retryCounter = new AtomicInteger( AlgExecutor.this.exchange.getRequestsPerSecondLimit() * AlgExecutor.RETRY_TIME_PERIOD );

				@Override
				public void succeeded( final OpenOrder openOrder ) {
					if ( openOrder.isClosed() ) {
						buyOrder[0] = new Trade( currencyPair, rate, quantity, AlgExecutor.this.exchange.getFee() );
						watchBuyOrderLatch.countDown();
					}
					else {
						if ( this.retryCounter.decrementAndGet() <= 0 && !openOrder.isPartiallyFilled() ) {
							AlgExecutor.LOGGER.debug( AlgExecutor.this.formatLog( "Buy order timeout occured, canceling order" ) );
							AlgExecutor.this.exchange.cancelOpenOrder( buyOrderRef[0].getID(), new Return<CancelOrder>() {
								@Override
								public void succeeded( final CancelOrder cancelOrder ) {
									/*
									 * TODO( EMU ): Should add more logic to not
									 * stall
									 */
									if ( tickerIndicator.shouldBuy() ) {
										/*
										 * Let the logic thread loop again and
										 * try to buy
										 */
										AlgExecutor.LOGGER.debug( AlgExecutor.this.formatLog( "Buy indicator still suggests buy, trying to buy again" ) );
										watchBuyOrderLatch.countDown();
									}
									else {
										/*
										 * Let the logic loop opt out of this
										 * buy
										 */
										AlgExecutor.LOGGER.debug( AlgExecutor.this.formatLog( "Buy indicator does not suggest buying, opting out of buying" ) );
										AlgExecutor.this.interrupt();
									}
								}

								@Override
								public void error( final Throwable t ) {
									buyOrder[0] = new Trade( currencyPair, rate, quantity, AlgExecutor.this.exchange.getFee() );
								}
							} );
						}
						else {
							AlgExecutor.this.exchange.getOpenOrder( buyOrderRef[0].getID(), this );
						}
					}
				}

				@Override
				public void error( final Throwable t ) {
					AlgExecutor.this.exchange.getOpenOrder( buyOrderRef[0].getID(), this );
				}
			} );

			try {
				/* Wait until the order has been completed */
				watchBuyOrderLatch.await();
			}
			catch ( final Throwable t ) {
				return null;
			}

			if ( buyOrder[0] != null ) {
				return buyOrder[0];
			}
		}
	}

	/**
	 * Executes a sell order and returns when complete
	 * 
	 * @param currencyPair
	 */
	protected Trade monitorAndExecuteSell( final TickerIndicator tickerIndicator, final Trade buyOrder ) {
		final CurrencyPair currencyPair = tickerIndicator.getTicker().getCurrencyPair();

		/* Loop until we sold */
		while ( true ) {
			/* Monitor the exchange for the sell signal */
			final CountDownLatch sellSignalLatch = new CountDownLatch( 1 );
			this.tickerMonitorThread.sampleListenerManager.addSampleListener( new SampleListener() {
				@Override
				public void onNewSample( final List<TickerIndicator> tickers ) {
					if ( tickerIndicator.shouldSell( buyOrder.getRate(), AlgExecutor.this.exchange.getFee() ) ) {
						/* Remove this listener */
						AlgExecutor.this.tickerMonitorThread.sampleListenerManager.removeSampleListener( this );

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

			/* Execute the Sell */
			final CountDownLatch sellPlacedLatch = new CountDownLatch( 1 );
			final double rate = tickerIndicator.getTicker().getLast();
			final double quantity = (buyOrder.getQuantity()) / (1.0 + this.exchange.getFee());
			final SellOrder[] sellOrderRef = new SellOrder[1];

			AlgExecutor.LOGGER.debug( this.formatLog( "Placing sell order [currency pair, quantity, rate] = [ " + currencyPair + ", " + quantity + ", " + rate + " ]" ) );
			this.exchange.placeSellOrder( currencyPair, quantity, rate, new Return<SellOrder>() {
				@Override
				public void succeeded( final SellOrder sellOrder ) {
					if ( sellOrder.isPlaced() ) {
						sellOrderRef[0] = sellOrder;
						sellPlacedLatch.countDown();
					}
					else {
						AlgExecutor.this.exchange.placeSellOrder( currencyPair, quantity, rate, this );
					}
				}

				@Override
				public void error( final Throwable t ) {
					AlgExecutor.this.exchange.placeSellOrder( currencyPair, quantity, rate, this );
				}
			} );

			try {
				/* Wait for sell signal */
				sellPlacedLatch.await();
			}
			catch ( final Throwable t ) {
			}

			/* Wait for the order to complete */
			AlgExecutor.LOGGER.debug( this.formatLog( "Sell order placed. Waiting for sell order to fill..." ) );
			final CountDownLatch watchSellOrderLatch = new CountDownLatch( 1 );
			final Trade sellOrder[] = new Trade[1];
			this.exchange.getOpenOrder( sellOrderRef[0].getID(), new Return<OpenOrder>() {
				final AtomicInteger retryCounter = new AtomicInteger( AlgExecutor.this.exchange.getRequestsPerSecondLimit() * AlgExecutor.RETRY_TIME_PERIOD );

				@Override
				public void succeeded( final OpenOrder openOrder ) {
					if ( openOrder.isClosed() ) {
						sellOrder[0] = new Trade( currencyPair, rate, quantity, AlgExecutor.this.exchange.getFee() );
						watchSellOrderLatch.countDown();
					}
					else {
						if ( this.retryCounter.decrementAndGet() <= 0 && !openOrder.isPartiallyFilled() ) {
							AlgExecutor.LOGGER.debug( AlgExecutor.this.formatLog( "Sell order timeout occured, canceling order" ) );
							AlgExecutor.this.exchange.cancelOpenOrder( sellOrderRef[0].getID(), new Return<CancelOrder>() {
								@Override
								public void succeeded( final CancelOrder cancelOrder ) {
									/*
									 * TODO( EMU ): Should add more logic to not
									 * stall
									 */
									AlgExecutor.LOGGER.debug( AlgExecutor.this.formatLog( "Retrying sell order with different rate" ) );
									watchSellOrderLatch.countDown();
								}

								@Override
								public void error( final Throwable t ) {
									sellOrder[0] = new Trade( currencyPair, rate, quantity, AlgExecutor.this.exchange.getFee() );
								}
							} );
						}
						else {
							AlgExecutor.this.exchange.getOpenOrder( sellOrderRef[0].getID(), this );
						}
					}
				}

				@Override
				public void error( final Throwable t ) {
					AlgExecutor.this.exchange.getOpenOrder( sellOrderRef[0].getID(), this );
				}
			} );

			try {
				/* Wait until the order has been completed */
				watchSellOrderLatch.await();
			}
			catch ( final Throwable t ) {
			}

			if ( sellOrder[0] != null ) {
				return sellOrder[0];
			}
		}
	}

	private String formatLog( final String msg ) {
		final StringBuilder builder = new StringBuilder();
		builder.append( "{ " );
		builder.append( this.exchange.getName() );
		builder.append( " | " );
		builder.append( this.tickerIndicatorFactory.getAlgorithmName() );
		builder.append( " } " );
		builder.append( msg );
		return builder.toString();
	}
}