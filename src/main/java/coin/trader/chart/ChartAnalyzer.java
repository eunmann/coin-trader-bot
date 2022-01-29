package coin.trader.chart;

import java.util.List;

import coin.trader.exchange.ChartData.DataPoint;
import coin.trader.exchange.CurrencyPair;
import coin.trader.exchange.Exchange;
import coin.trader.logger.Logger;
import coin.trader.math.SampleHistory;
import coin.trader.utilities.Callback.Return;

public class ChartAnalyzer {
	private static final Logger LOGGER = new Logger( ChartAnalyzer.class.getSimpleName() );
	private final Exchange exchange;
	private final CurrencyPair currencyPair;

	public ChartAnalyzer( final Exchange exchange, final CurrencyPair currencyPair ) {
		this.exchange = exchange;
		this.currencyPair = currencyPair;
	}

	public void anaylize( final long start, final long end, final int period ) {
		ChartAnalyzer.LOGGER.info( "Start analysis of " + this.currencyPair + " on " + this.exchange + " from " + start + " to " + end + " with period of " + period );
		this.exchange.getChartData( this.currencyPair, start, end, period, new Return<List<? extends DataPoint>>() {

			@Override
			public void succeeded( final List<? extends DataPoint> rv ) {
				final int length = rv.size();

				final SampleHistory lowSH = new SampleHistory( length );
				final SampleHistory closeSH = new SampleHistory( length );
				final SampleHistory highSH = new SampleHistory( length );
				final SampleHistory openSH = new SampleHistory( length );
				final SampleHistory volumeSH = new SampleHistory( length );

				for ( final DataPoint dataPoint : rv ) {
					lowSH.addSample( dataPoint.getLow() );
					closeSH.addSample( dataPoint.getClose() );
					highSH.addSample( dataPoint.getHigh() );
					openSH.addSample( dataPoint.getOpen() );
					volumeSH.addSample( dataPoint.getVolume() );
				}

				ChartAnalyzer.LOGGER.info( "Metric: [Average, Standard Deviation, Min, Max, Spread, Spread Percentage ( Spread / Average ) ]" );
				this.printStats( "Low", lowSH );
				this.printStats( "High", highSH );
				this.printStats( "Open", openSH );
				this.printStats( "Close", closeSH );
				this.printStats( "Volume", volumeSH );

			}

			private void printStats( final String message, final SampleHistory sampleHistory ) {
				final double average = sampleHistory.getAverage();
				final double stdDeviation = sampleHistory.getStandardDeviation();
				final double min = sampleHistory.getMinimum();
				final double max = sampleHistory.getMaximum();
				final double spread = max - min;
				final double spreadPercentage = spread / average;

				ChartAnalyzer.LOGGER.info( message + ": [" + average + ", " + stdDeviation + ", " + min + ", " + max + ", " + spread + ", " + spreadPercentage + "]" );
			}

			@Override
			public void error( final Throwable t ) {
				ChartAnalyzer.LOGGER.warn( "Failed to get chart data, trying again", t );
				ChartAnalyzer.this.exchange.getChartData( ChartAnalyzer.this.currencyPair, start, end, period, this );
			}
		} );
	}
}
