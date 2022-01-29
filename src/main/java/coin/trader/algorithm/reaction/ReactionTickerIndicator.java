package coin.trader.algorithm.reaction;

import coin.trader.Info;
import coin.trader.algorithm.TickerIndicator;
import coin.trader.exchange.Ticker;
import coin.trader.math.LinearRegression;

/**
 * Class to efficiently record the simple moving average of an sample space.
 * This class is thread safe. This class also records the momentum and the
 * differential of momentum.
 * 
 * @author Evan
 *
 */
public class ReactionTickerIndicator implements TickerIndicator {
	static final int ANALYSIS_INTERVAL_SECONDS = 45;

	private Ticker ticker;
	private final LinearRegression samples;
	private final IndicatorFlags flags = new IndicatorFlags();
	private double candidacyWeight = 0;
	private final Object LOCK = new Object();

	public ReactionTickerIndicator( final int requestsPerSecond ) {
		this.samples = new LinearRegression( requestsPerSecond * ReactionTickerIndicator.ANALYSIS_INTERVAL_SECONDS );
	}

	/**
	 * Add a ticker sample. Calculates the average last price, average momentum,
	 * and average jerk.
	 * 
	 * @param ticker
	 */
	@Override
	public void addSample( final Ticker ticker ) {
		synchronized ( this.LOCK ) {
			/* Set the latest ticker */
			this.ticker = ticker;

			/* Ticker close prices */
			this.samples.addSample( System.currentTimeMillis() - Info.START_TIME_STAMP, this.ticker.getLast() );

			/* Candidacy weight */
			this.candidacyWeight = this.calculateCandidacyWeight();

			/* Set flags */
			this.flags.postiveLinearRegression = this.samples.getSlope() / this.samples.getYNewestSample() > 0.0;

			/*
			 * TODO( EMU ): Need to analyze the values that are generated for
			 * better comparison
			 */
			this.flags.overMinCandidacyWeight = this.candidacyWeight > 0.0;
			this.flags.aboveMinVolume = this.ticker.getBaseVolume() >= 540.0;
		}
	}

	/**
	 * Returns true if this indicator thinks the ticker is a good buy.
	 * 
	 * @return
	 */
	@Override
	public boolean shouldBuy() {
		synchronized ( this.LOCK ) {
			return this.flags.buySignal();
		}
	}

	/**
	 * Returns if the ticker is in a good state to sell.
	 * 
	 * @return
	 */
	@Override
	public boolean shouldSell( final double buyPrice, final double fee ) {
		synchronized ( this.LOCK ) {
			final double last = this.ticker.getLast();
			final double lowerBound = 0.995 * buyPrice;
			final double signalPrice = (last * (1.0 - fee) - buyPrice) / buyPrice;
			return last < lowerBound || signalPrice > 0.0005;
		}
	}

	/**
	 * Returns the candidacy weight for the ticker, how good of a buy is it.
	 * 
	 * @return
	 */
	@Override
	public double getCandidacyWeight() {
		synchronized ( this.LOCK ) {
			return this.candidacyWeight;
		}
	}

	private double calculateCandidacyWeight() {
		synchronized ( this.LOCK ) {
			/* How much has the sample prices grown */
			double samplesW = 0;
			{
				final double w1 = this.samples.getYAverageToOldestRatio();
				if ( w1 > 1.0 ) {
					// samplesW += w1;
				}

				final double w2 = this.samples.getYNewestToAverageRatio();
				if ( w2 > 1.0 ) {
					// samplesW += w2;
				}

				final double w3 = this.samples.getYNewestToOldestRatio();
				if ( w3 > 1.0 ) {
					// samplesW += w3;
				}

				final double w4 = this.samples.getSlope();
				samplesW += w4 / this.samples.getYNewestSample();
			}

			return samplesW;
		}
	}

	@Override
	public Ticker getTicker() {
		synchronized ( this.LOCK ) {
			return this.ticker;
		}
	}
}
