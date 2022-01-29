package coin.trader.algorithm.history;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import coin.trader.algorithm.history.HistoryListenerManager.HistoryIndicatorListener;
import coin.trader.exchange.Balance;
import coin.trader.exchange.Balances;
import coin.trader.exchange.BuyOrder;
import coin.trader.exchange.CancelOrder;
import coin.trader.exchange.CurrencyPair;
import coin.trader.exchange.Exchange;
import coin.trader.exchange.OpenOrder;
import coin.trader.exchange.SellOrder;
import coin.trader.exchange.Ticker;
import coin.trader.exchange.Trade;
import coin.trader.exchange.TradeStatistic;
import coin.trader.logger.Logger;
import coin.trader.utilities.Callback.Return;

public class HistoryAlgExecutor {
	private static final int RETRY_TIME_PERIOD = 2;
	protected static final Logger LOGGER = new Logger( HistoryAlgExecutor.class.getSimpleName(), Logger.Level.INFO );
	protected final LogicThread thread = new LogicThread();
	protected final Exchange exchange;
	protected final HistoryMonitor historyMonitor;
	protected Balances balances = null;

	public HistoryAlgExecutor( final Exchange exchange, final HistoryTickerIndicatorFactory historyTickerIndicatorFactory ) {
		this.exchange = exchange;
		this.historyMonitor = historyTickerIndicatorFactory.createHistoryMonitor( this.exchange );
	}

	public void start() {
		if ( !this.thread.isAlive() ) {
			this.thread.start();
		}

		this.historyMonitor.start();
	}

	protected void updateBalances() throws InterruptedException {
		/* Get your current balances */
		final CountDownLatch balanceLatch = new CountDownLatch( 1 );
		this.exchange.getBalances( new Return<Balances>() {
			@Override
			public void succeeded( final Balances balances ) {
				HistoryAlgExecutor.this.balances = balances;
				balanceLatch.countDown();
			}

			@Override
			public void error( final Throwable t ) {
				HistoryAlgExecutor.this.exchange.getBalances( this );
			}
		} );

		/* Wait for the balances */
		balanceLatch.await();
	}

	protected HistoryTickerIndicator monitorForCandidates() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch( 1 );
		final HistoryTickerIndicator[] historyTickerIndicatorArray = new HistoryTickerIndicator[1];

		this.historyMonitor.getHistoryListenerManager().add( new HistoryIndicatorListener() {
			@Override
			public void onBuyTrigger( final List<HistoryTickerIndicator> historyTickerIndicatorList ) {
				/*
				 * TODO(EMU): Do something with the weight ( probably use a risk
				 * parameter )
				 */
				historyTickerIndicatorArray[0] = historyTickerIndicatorList.get( 0 );
				HistoryAlgExecutor.this.historyMonitor.getHistoryListenerManager().remove( this );
				latch.countDown();
			}

			@Override
			public void onSellTrigger( final HistoryTickerIndicator historyTickerIndicator ) {
				/* Ignore, we are trying to buy */
			}
		} );

		latch.await();

		return historyTickerIndicatorArray[0];
	}

	protected Trade buy( final HistoryTickerIndicator historyTickerIndicator ) throws InterruptedException {

		final CurrencyPair currencyPair = historyTickerIndicator.getCurrencyPair();

		/* Get the current trade price */
		HistoryAlgExecutor.LOGGER.debug( "Getting latest ticker" );
		final CountDownLatch tickerPriceLatch = new CountDownLatch( 1 );
		final Ticker[] tickerP = new Ticker[1];
		HistoryAlgExecutor.this.exchange.getTickers( new Return<Map<CurrencyPair, Ticker>>() {

			@Override
			public void succeeded( final Map<CurrencyPair, Ticker> tickers ) {

				tickerP[0] = tickers.get( currencyPair );
				tickerPriceLatch.countDown();
			}

			@Override
			public void error( final Throwable t ) {
				if ( historyTickerIndicator.shouldBuy() >= 0 ) {
					HistoryAlgExecutor.this.exchange.getTickers( this );
				}
			}
		} );

		tickerPriceLatch.await();

		if ( tickerP[0] == null ) {
			return null;
		}

		HistoryAlgExecutor.LOGGER.debug( "Got latest ticker" );

		/* Place the order for everything that we got */
		HistoryAlgExecutor.LOGGER.debug( "Placing buy order" );
		final CountDownLatch placeBuyOrderLatch = new CountDownLatch( 1 );
		final double rate = tickerP[0].getLast();
		final double quantity = (this.balances.getBalance( currencyPair.getBase() ).getAmount() / rate) / (1.0 + this.exchange.getFee());
		final BuyOrder[] buyOrderRef = new BuyOrder[1];

		HistoryAlgExecutor.this.exchange.placeBuyOrder( currencyPair, quantity, rate, new Return<BuyOrder>() {
			@Override
			public void succeeded( final BuyOrder buyOrder ) {
				if ( buyOrder.isPlaced() ) {
					buyOrderRef[0] = buyOrder;
					placeBuyOrderLatch.countDown();
				}
				else if ( historyTickerIndicator.shouldBuy() >= 0 ) {
					HistoryAlgExecutor.this.exchange.placeBuyOrder( currencyPair, quantity, rate, this );
				}
				else {
					placeBuyOrderLatch.countDown();
				}
			}

			@Override
			public void error( final Throwable t ) {
				if ( historyTickerIndicator.shouldBuy() >= 0 ) {
					HistoryAlgExecutor.this.exchange.placeBuyOrder( currencyPair, quantity, rate, this );
				}
				else {
					placeBuyOrderLatch.countDown();
				}
			}
		} );

		/* Wait until we have placed the order */
		placeBuyOrderLatch.await();

		if ( buyOrderRef[0] == null ) {
			return null;
		}

		HistoryAlgExecutor.LOGGER.debug( "Buy order placed" );

		/* Wait for the order to complete */
		HistoryAlgExecutor.LOGGER.debug( "Waiting for buy order to fill" );
		final CountDownLatch watchBuyOrderLatch = new CountDownLatch( 1 );
		final Trade buyOrder[] = new Trade[1];

		this.exchange.getOpenOrder( buyOrderRef[0].getID(), new Return<OpenOrder>() {
			final AtomicInteger retryCounter = new AtomicInteger( HistoryAlgExecutor.this.exchange.getRequestsPerSecondLimit() * HistoryAlgExecutor.RETRY_TIME_PERIOD );

			@Override
			public void succeeded( final OpenOrder openOrder ) {
				if ( openOrder.isClosed() ) {
					buyOrder[0] = new Trade( currencyPair, rate, quantity, HistoryAlgExecutor.this.exchange.getFee() );
					watchBuyOrderLatch.countDown();
				}
				else {
					if ( this.retryCounter.decrementAndGet() <= 0 && !openOrder.isPartiallyFilled() ) {
						HistoryAlgExecutor.LOGGER.debug( "Canceling buy order" );
						HistoryAlgExecutor.this.exchange.cancelOpenOrder( buyOrderRef[0].getID(), new Return<CancelOrder>() {
							@Override
							public void succeeded( final CancelOrder cancelOrder ) {
								/*
								 * TODO( EMU ): Should add more logic to not
								 * stall
								 */
								/* Let the logic loop opt out of this buy */
								watchBuyOrderLatch.countDown();
							}

							@Override
							public void error( final Throwable t ) {
								buyOrder[0] = new Trade( currencyPair, rate, quantity, HistoryAlgExecutor.this.exchange.getFee() );
								watchBuyOrderLatch.countDown();
							}
						} );
					}
					else {
						HistoryAlgExecutor.this.exchange.getOpenOrder( buyOrderRef[0].getID(), this );
					}
				}
			}

			@Override
			public void error( final Throwable t ) {
				HistoryAlgExecutor.this.exchange.getOpenOrder( buyOrderRef[0].getID(), this );

			}
		} );

		/* Wait until the order has been completed */
		watchBuyOrderLatch.await();

		return buyOrder[0];

	}

	protected Trade monitorAndExecuteSell( final HistoryTickerIndicator historyTickerIndicator, final Trade buyOrder ) throws InterruptedException {
		final CurrencyPair currencyPair = historyTickerIndicator.getCurrencyPair();

		/* Loop until we sold */
		while ( true ) {
			/* Monitor the exchange for the sell signal */
			final CountDownLatch sellSignalLatch = new CountDownLatch( 1 );

			this.historyMonitor.getHistoryListenerManager().add( new HistoryIndicatorListener() {

				@Override
				public void onSellTrigger( final HistoryTickerIndicator triggedHistoryTickerIndicator ) {
					if ( triggedHistoryTickerIndicator.equals( historyTickerIndicator ) ) {
						HistoryAlgExecutor.this.historyMonitor.getHistoryListenerManager().remove( this );
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
			HistoryAlgExecutor.this.exchange.getTickers( new Return<Map<CurrencyPair, Ticker>>() {

				@Override
				public void succeeded( final Map<CurrencyPair, Ticker> tickers ) {
					tickerP[0] = tickers.get( currencyPair );
					tickerPriceLatch.countDown();
				}

				@Override
				public void error( final Throwable t ) {
					HistoryAlgExecutor.this.exchange.getTickers( this );
				}
			} );

			tickerPriceLatch.await();

			/* Execute the Sell */
			HistoryAlgExecutor.LOGGER.debug( "Placing the sell order" );
			final CountDownLatch sellPlacedLatch = new CountDownLatch( 1 );
			final double rate = tickerP[0].getLast();
			final double quantity = (buyOrder.getQuantity()) / (1.0 + this.exchange.getFee());
			final SellOrder[] sellOrderRef = new SellOrder[1];

			this.exchange.placeSellOrder( currencyPair, quantity, rate, new Return<SellOrder>() {
				@Override
				public void succeeded( final SellOrder sellOrder ) {
					if ( sellOrder.isPlaced() ) {
						sellOrderRef[0] = sellOrder;
						sellPlacedLatch.countDown();
					}
					else {
						HistoryAlgExecutor.this.exchange.placeSellOrder( currencyPair, quantity, rate, this );
					}
				}

				@Override
				public void error( final Throwable t ) {
					HistoryAlgExecutor.this.exchange.placeSellOrder( currencyPair, quantity, rate, this );
				}
			} );

			/* Wait for sell signal */
			sellPlacedLatch.await();

			/* Wait for the order to complete */
			HistoryAlgExecutor.LOGGER.debug( "Waiting for sell order to fill" );
			final CountDownLatch watchSellOrderLatch = new CountDownLatch( 1 );
			final Trade sellOrder[] = new Trade[1];
			this.exchange.getOpenOrder( sellOrderRef[0].getID(), new Return<OpenOrder>() {
				final AtomicInteger retryCounter = new AtomicInteger( HistoryAlgExecutor.this.exchange.getRequestsPerSecondLimit() * HistoryAlgExecutor.RETRY_TIME_PERIOD );

				@Override
				public void succeeded( final OpenOrder openOrder ) {
					if ( openOrder.isClosed() ) {
						sellOrder[0] = new Trade( currencyPair, rate, quantity, HistoryAlgExecutor.this.exchange.getFee() );
						watchSellOrderLatch.countDown();
					}
					else {
						if ( this.retryCounter.decrementAndGet() <= 0 && !openOrder.isPartiallyFilled() ) {
							HistoryAlgExecutor.this.exchange.cancelOpenOrder( sellOrderRef[0].getID(), new Return<CancelOrder>() {
								@Override
								public void succeeded( final CancelOrder cancelOrder ) {
									/*
									 * TODO( EMU ): Should add more logic to not
									 * stall
									 */
									watchSellOrderLatch.countDown();
								}

								@Override
								public void error( final Throwable t ) {
									sellOrder[0] = new Trade( currencyPair, rate, quantity, HistoryAlgExecutor.this.exchange.getFee() );
								}
							} );
						}
						else {
							HistoryAlgExecutor.this.exchange.getOpenOrder( sellOrderRef[0].getID(), this );
						}
					}
				}

				@Override
				public void error( final Throwable t ) {
					HistoryAlgExecutor.this.exchange.getOpenOrder( sellOrderRef[0].getID(), this );
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

	protected void printStatistics( final Balances oldBalances ) {
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
	}

	private class LogicThread extends Thread {
		@Override
		public void run() {
			try {
				HistoryAlgExecutor.LOGGER.info( "Starting algorithm executor" );
				/* Check the config */
				if ( !HistoryAlgExecutor.this.exchange.checkConfig() ) {
					throw new Exception( "The exchange's config is not correct. Aborting" );
				}

				HistoryAlgExecutor.LOGGER.info( "Getting initial balances" );
				/* Get the current balances */
				HistoryAlgExecutor.this.updateBalances();
				HistoryAlgExecutor.LOGGER.info( "Got balances" );

				while ( true ) {
					/* Monitor for candidates */
					HistoryAlgExecutor.LOGGER.info( "Monitoring for candidates" );
					final HistoryTickerIndicator historyTickerIndicator = HistoryAlgExecutor.this.monitorForCandidates();
					final CurrencyPair currencyPair = historyTickerIndicator.getCurrencyPair();
					HistoryAlgExecutor.LOGGER.info( "Candidate found " + currencyPair );

					/* Try to buy */
					HistoryAlgExecutor.LOGGER.info( "Placing buy order" );
					final Trade buyTrade = HistoryAlgExecutor.this.buy( historyTickerIndicator );

					if ( buyTrade != null ) {
						HistoryAlgExecutor.LOGGER.info( "Buy order filled" );

						/* Monitor for selling point and then sell */
						HistoryAlgExecutor.LOGGER.info( "Monitoring for sell" );
						final Trade sellTrade = HistoryAlgExecutor.this.monitorAndExecuteSell( historyTickerIndicator, buyTrade );
						HistoryAlgExecutor.LOGGER.info( "Sold" );

						/* Update balances and print statistics */

						/*
						 * Balances do not update fast enough, need to sleep
						 * here so server updates
						 */
						HistoryAlgExecutor.LOGGER.info( "Updating balances" );
						Thread.sleep( 2000 );
						final Balances oldBalances = HistoryAlgExecutor.this.balances;
						HistoryAlgExecutor.this.updateBalances();

						/* Print the statistics of the trade */
						HistoryAlgExecutor.this.printStatistics( oldBalances );
					}
					else {
						HistoryAlgExecutor.LOGGER.info( "Buy order was not filled" );
					}

				}
			}
			catch ( final Throwable t ) {
				HistoryAlgExecutor.LOGGER.warn( "Stopping algorithm executor", t );
			}
		}
	}
}
