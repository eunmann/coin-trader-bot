package coin.trader.math;

public class SampleHistory {
	private final double[] samples;
	private double sum = 0;
	private double sumSquared = 0;
	private double average = 0;
	private int oldestSampleIndex = 0;
	private int newestSampleIndex = 0;
	private final Object LOCK = new Object();

	public SampleHistory( final int sampleSize ) {
		this.samples = new double[sampleSize];
	}

	public void addSample( final double sample ) {
		synchronized ( this.LOCK ) {
			/* Get the oldest sample */
			final double oldestSampleValue = this.samples[this.oldestSampleIndex];

			/* Remove the sample from the sums */
			this.sum -= oldestSampleValue;
			this.sumSquared -= oldestSampleValue * oldestSampleValue;

			/* Add the new sample to the sums */
			this.sum += sample;
			this.sumSquared += sample * sample;
			this.average = this.sum / this.samples.length;

			/* Add the new sample to the array */
			this.samples[this.oldestSampleIndex] = sample;

			/* Update the oldest index */
			this.newestSampleIndex = this.oldestSampleIndex;
			this.oldestSampleIndex = (this.oldestSampleIndex + 1) % this.samples.length;
		}
	}

	public double getSum() {
		synchronized ( this.LOCK ) {
			return this.sum;
		}
	}

	public double getSumSquared() {
		synchronized ( this.LOCK ) {
			return this.sumSquared;
		}
	}

	public double getAverage() {
		synchronized ( this.LOCK ) {
			return this.average;
		}
	}

	public double getRSI() {
		synchronized ( this.LOCK ) {
			return Math.getRSI( this.samples );
		}
	}

	/**
	 * Returns the newest sample
	 * 
	 * @return
	 */
	public double getNewestSample() {
		synchronized ( this.LOCK ) {
			return this.samples[this.newestSampleIndex];
		}
	}

	/**
	 * Returns the oldest sample
	 * 
	 * @return
	 */
	public double getOldestSample() {
		synchronized ( this.LOCK ) {
			return this.samples[this.oldestSampleIndex];
		}
	}

	/**
	 * Returns the difference between the last two samples.
	 * 
	 * @return
	 */
	public double getDelta() {
		synchronized ( this.LOCK ) {
			final int secondNewestIndex = (this.oldestSampleIndex + this.samples.length - 2) % this.samples.length;
			return this.samples[this.newestSampleIndex] - this.samples[secondNewestIndex];
		}
	}

	/**
	 * Returns the standard deviation of the samples
	 * 
	 * @return
	 */
	public double getStandardDeviation() {
		synchronized ( this.LOCK ) {
			return Math.standardDeviation( this.samples, this.average );
		}
	}

	/**
	 * Returns the average sample value as a percent of the oldest sample.
	 * 
	 * @return
	 */
	public double getAverageToOldestRatio() {
		synchronized ( this.LOCK ) {
			final double oldest = this.samples[this.oldestSampleIndex];
			final double rv = this.average / oldest;
			return Double.isFinite( rv ) ? rv : 0;
		}
	}

	/**
	 * Returns the newest sample value as a percent of the average.
	 * 
	 * @return
	 */
	public double getNewestToAverageRatio() {
		synchronized ( this.LOCK ) {
			final double newest = this.samples[this.newestSampleIndex];
			final double rv = newest / this.average;
			return Double.isFinite( rv ) ? rv : 0;
		}
	}

	/**
	 * Returns the newest sample value as a percent of the oldest
	 * 
	 * @return
	 */
	public double getNewestToOldestRatio() {
		synchronized ( this.LOCK ) {
			final double newest = this.samples[this.newestSampleIndex];
			final double rv = newest / this.samples[this.oldestSampleIndex];
			return Double.isFinite( rv ) ? rv : 0;
		}
	}

	public double getPeriodicAverage( final int period ) {
		synchronized ( this.LOCK ) {
			final int length = this.samples.length;
			final int samplesPerPeriod = length / period;
			double average = 0;
			int index = this.newestSampleIndex;

			for ( int i = 0; i < samplesPerPeriod; i++ ) {
				average += this.samples[index];
				index = (index - samplesPerPeriod + length) % length;
			}

			return average / samplesPerPeriod;
		}
	}

	public double getMinimum() {
		return Math.getMinimum( this.samples );
	}

	public double getMaximum() {
		return Math.getMaximum( this.samples );
	}
}
