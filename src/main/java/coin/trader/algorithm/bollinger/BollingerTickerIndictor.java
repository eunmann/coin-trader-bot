package coin.trader.algorithm.bollinger;

import coin.trader.algorithm.TickerIndicator;
import coin.trader.exchange.Ticker;
import coin.trader.math.Math;
import coin.trader.math.SampleHistory;

public class BollingerTickerIndictor implements TickerIndicator {
	static final int ANALYSIS_NUM_OF_PERIODS = 1;
	static final int ANALYSIS_INTERVAL_MINS = 1;

	private final SampleHistory samples;
	private Ticker ticker;
	private final IndicatorFlags flags = new IndicatorFlags();

	private final Object LOCK = new Object();

	public BollingerTickerIndictor( final int sampleSize ) {
		this.samples = new SampleHistory( sampleSize );
	}

	@Override
	public void addSample( final Ticker ticker ) {
		synchronized ( this.LOCK ) {
			this.ticker = ticker;
			this.samples.addSample( this.ticker.getLast() );

			final double[] bollingerBands = Math.getBollingerBands( this.samples.getAverage(), this.samples.getStandardDeviation() );
			final double last = this.samples.getNewestSample();
			final double periodicAverage = this.samples.getPeriodicAverage( BollingerTickerIndictor.ANALYSIS_NUM_OF_PERIODS );
			final double midUpperAverage = (periodicAverage + bollingerBands[1]) / 2;

			this.flags.isBetweenAverageAndUpperBollingerBand = periodicAverage <= last && last <= midUpperAverage;
			this.flags.isAboveUpperBollingerBand = bollingerBands[1] <= last;
			this.flags.isAboveMinVolume = this.ticker.getBaseVolume() >= 50.0;
		}
	}

	@Override
	public boolean shouldBuy() {
		synchronized ( this.LOCK ) {
			return this.flags.buySignal();
		}
	}

	@Override
	public boolean shouldSell( final double buyPrice, final double fee ) {
		synchronized ( this.LOCK ) {
			final double last = this.ticker.getLast();
			final double lowerLimit = 0.995 * buyPrice;
			final double signalPrice = last * (1.0 - fee) - buyPrice;
			return last <= lowerLimit || (this.flags.sellSignal() && signalPrice > 0);
		}
	}

	@Override
	public Ticker getTicker() {
		synchronized ( this.LOCK ) {
			return this.ticker;
		}
	}

	@Override
	public double getCandidacyWeight() {
		synchronized ( this.LOCK ) {
			double weight = 0.0;

			final double gainW1 = this.samples.getAverageToOldestRatio();
			if ( gainW1 >= 1.0 ) {
				weight += gainW1;
			}

			final double gainW2 = this.samples.getNewestToOldestRatio();
			if ( gainW2 >= 1.0 ) {
				weight += gainW2;
			}

			final double gainW3 = this.samples.getNewestToAverageRatio();
			if ( gainW3 >= 1.0 ) {
				weight += gainW3;
			}

			return weight;
		}
	}
}
