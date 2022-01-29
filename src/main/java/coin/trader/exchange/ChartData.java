package coin.trader.exchange;

import java.util.List;

import coin.trader.exchange.ExchangeMonitor.Job.JobArgs;

/**
 * 
 * @author Evan
 *
 */
public interface ChartData {
	public List<? extends DataPoint> getDataPoints();

	/**
	 * 
	 * @author Evan
	 *
	 */
	public interface DataPoint {
		public double getDate();

		public double getHigh();

		public double getLow();

		public double getOpen();

		public double getClose();

		public double getVolume();

		public double getQuoteVolume();

		public double getWeightedAverage();
	}

	/**
	 * 
	 * @author Evan
	 *
	 */
	public static class ChartDataJobArgs extends JobArgs {
		final CurrencyPair currencyPair;
		final long start;
		final long end;
		final int period;

		public ChartDataJobArgs( final CurrencyPair currency, final long start, final long end, final int period ) {
			this.currencyPair = currency;
			this.start = start;
			this.end = end;
			this.period = period;
		}

		@Override
		public String toString() {
			return "[ currency pair: " + this.currencyPair + ", start: " + this.start + ", end: " + this.end + ", period: " + this.period + " ]";
		}
	}
}
