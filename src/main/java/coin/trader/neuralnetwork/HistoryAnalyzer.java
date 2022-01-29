package coin.trader.neuralnetwork;

import java.util.ArrayList;
import java.util.List;

import coin.trader.exchange.ChartData.DataPoint;
import coin.trader.exchange.CurrencyPair;
import coin.trader.exchange.Exchange;
import coin.trader.logger.Logger;
import coin.trader.utilities.Callback.Return;

public class HistoryAnalyzer extends Thread {
	private static final Logger LOGGER = new Logger( HistoryAnalyzer.class.getSimpleName(), Logger.Level.INFO );
	private static final int EPOCH = 20000;
	private final int numOfDataPoints;
	private final Exchange exchange;
	private final CurrencyPair currencyPair;
	private final int period;

	public HistoryAnalyzer( final Exchange exchange, final CurrencyPair currencyPair, final int numOfDataPoints, final int period ) {
		this.numOfDataPoints = numOfDataPoints;
		this.exchange = exchange;
		this.period = period;
		this.currencyPair = currencyPair;
	}

	@Override
	public void run() {
		HistoryAnalyzer.LOGGER.info( "Getting chart data..." );
		/* Get data from a long time ago */
		final long ts = System.currentTimeMillis() / 1000;
		final long startT = ts - (60 * 60 * 24 * 90 * 1);
		final long midT = ts - (60 * 60 * 24 * 30 * 1);
		this.exchange.getChartData( this.currencyPair, startT, midT, this.period, new Return<List<? extends DataPoint>>() {
			@Override
			public void succeeded( final List<? extends DataPoint> chartData ) {
				HistoryAnalyzer.LOGGER.info( "Chart Data acquired. Training Neural Network..." );
				final DataPointAnalyzer analyzer = new DataPointAnalyzer( HistoryAnalyzer.this.numOfDataPoints );

				final int lim = chartData.size() - HistoryAnalyzer.this.numOfDataPoints - 1;

				/*
				 * Train the neural network EPOCH amount of times with the same
				 * data
				 */
				for ( int e = 0; e < HistoryAnalyzer.EPOCH; e++ ) {
					HistoryAnalyzer.LOGGER.info( "Training epoch: " + e + " of " + HistoryAnalyzer.EPOCH );

					for ( int i = 0; i < lim; i++ ) {
						final List<DataPoint> input = new ArrayList<DataPoint>( HistoryAnalyzer.this.numOfDataPoints );

						/* Grab a subset of sequential candles */
						for ( int j = i; j < i + HistoryAnalyzer.this.numOfDataPoints; j++ ) {
							input.add( chartData.get( j ) );
						}

						final DataPoint lastDataPoint = chartData.get( i + HistoryAnalyzer.this.numOfDataPoints - 1 );
						final DataPoint postDataPoint = chartData.get( i + HistoryAnalyzer.this.numOfDataPoints );

						/* Train the neural network */
						analyzer.train( input, lastDataPoint.getLow() < postDataPoint.getLow() );
					}
				}

				HistoryAnalyzer.LOGGER.info( "Neural Network trained. Getting more chart data." );

				HistoryAnalyzer.this.exchange.getChartData( HistoryAnalyzer.this.currencyPair, midT, ts, HistoryAnalyzer.this.period, new Return<List<? extends DataPoint>>() {
					@Override
					public void succeeded( final List<? extends DataPoint> chartData ) {
						HistoryAnalyzer.LOGGER.info( "More chart data acquired. Quering neural network..." );
						final DataPointAnalyzer analyzer = new DataPointAnalyzer( HistoryAnalyzer.this.numOfDataPoints );

						final int lim = chartData.size() - HistoryAnalyzer.this.numOfDataPoints - 1;

						int correct = 0;
						int total = 0;

						for ( int i = 0; i < lim; i++ ) {
							final List<DataPoint> input = new ArrayList<DataPoint>( HistoryAnalyzer.this.numOfDataPoints );

							/* Grab a subset of sequential candles */
							for ( int j = i; j < i + HistoryAnalyzer.this.numOfDataPoints; j++ ) {
								input.add( chartData.get( j ) );
							}

							final DataPoint lastDataPoint = chartData.get( i + HistoryAnalyzer.this.numOfDataPoints - 1 );
							final DataPoint postDataPoint = chartData.get( i + HistoryAnalyzer.this.numOfDataPoints );

							final boolean shouldBuy = lastDataPoint.getLow() < postDataPoint.getLow();

							final boolean answer = analyzer.query( input );

							/* Check the neural networks correctness */
							total++;
							if ( shouldBuy == answer ) {
								correct++;
							}
						}

						HistoryAnalyzer.LOGGER.info( "Neural Network Statistics: Correct: " + correct + ", Total: " + total + ", Accuracy: " + (double) correct / (double) total );
						System.exit( 0 );
					}

					@Override
					public void error( final Throwable t ) {
						// TODO Auto-generated method stub

					}
				} );
			}

			@Override
			public void error( final Throwable t ) {
				// TODO Auto-generated method stub

			}
		} );
	}
}
