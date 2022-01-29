package coin.trader.math;

public class LinearRegression {
	private final SampleHistory xSamples;
	private final SampleHistory ySamples;
	private final SampleHistory xySamples;
	private double slope = 0;
	private double yIntercept = 0;
	private final double sampleSize;
	private final Object LOCK = new Object();

	public LinearRegression( final int sampleSize ) {
		this.xSamples = new SampleHistory( sampleSize );
		this.ySamples = new SampleHistory( sampleSize );
		this.xySamples = new SampleHistory( sampleSize );
		this.sampleSize = sampleSize;
	}

	public void addSample( final double x, final double y ) {
		synchronized ( this.LOCK ) {
			/* Add the samples */
			this.xSamples.addSample( x );
			this.ySamples.addSample( y );
			this.xySamples.addSample( x * y );

			/* Get all the parameters */
			final double xSum = this.xSamples.getSum();
			final double xxSum = this.xSamples.getSumSquared();
			final double ySum = this.ySamples.getSum();
			final double xySum = this.xySamples.getSum();

			/* Calculate the linear regression values */
			final double numeratorSlope = this.sampleSize * xySum - xSum * ySum;
			final double numeratorYIntercept = ySum * xxSum - xSum * xySum;
			final double denomerator = this.sampleSize * xxSum - xSum * xSum;

			/* Set the values */
			this.slope = numeratorSlope / denomerator;
			this.yIntercept = numeratorYIntercept / denomerator;
		}
	}

	public double getSlope() {
		synchronized ( this.LOCK ) {
			return this.slope;
		}
	}

	public double getYIntercept() {
		synchronized ( this.LOCK ) {
			return this.yIntercept;
		}
	}

	public double getYDelta() {
		synchronized ( this.LOCK ) {
			return this.ySamples.getDelta();
		}
	}

	public double getYAverage() {
		synchronized ( this.LOCK ) {
			return this.ySamples.getAverage();
		}
	}

	public double getYStandardDeviation() {
		synchronized ( this.LOCK ) {
			return this.ySamples.getStandardDeviation();
		}
	}

	public double getYNewestSample() {
		synchronized ( this.LOCK ) {
			return this.ySamples.getNewestSample();
		}
	}

	public double getYAverageToOldestRatio() {
		synchronized ( this.LOCK ) {
			return this.ySamples.getAverageToOldestRatio();
		}
	}

	public double getYNewestToAverageRatio() {
		synchronized ( this.LOCK ) {
			return this.ySamples.getNewestToAverageRatio();
		}
	}

	public double getYNewestToOldestRatio() {
		synchronized ( this.LOCK ) {
			return this.ySamples.getNewestToOldestRatio();
		}
	}
}
