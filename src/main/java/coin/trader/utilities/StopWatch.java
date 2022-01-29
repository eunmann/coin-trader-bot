package coin.trader.utilities;

import coin.trader.logger.Logger;

public class StopWatch {
	private static final Logger LOGGER = new Logger( StopWatch.class.getSimpleName() );
	private long startT = 0;
	private long endT = 0;
	private final TimeUnit units;

	public StopWatch( final TimeUnit units ) {
		this.units = units;
	}

	public void start() {
		this.startT = System.currentTimeMillis();
	}

	public long end() {
		this.endT = System.currentTimeMillis();
		return this.getElapsedTime();
	}

	public long getElapsedTime() {
		return (this.endT - this.startT) / this.units.modifier;
	}

	public void print( final String str ) {
		StopWatch.LOGGER.trace( str + " < Time Elapsed: " + this.getElapsedTime() + " " + this.units + " >" );
		System.out.println( str + " < Time Elapsed: " + this.getElapsedTime() + " " + this.units + " >" );
	}

	public enum TimeUnit {

		MILLISECONDS( 1,
				"ms" ),
		SECONDS( 1000,
				"s" );

		private final int modifier;
		private final String shortName;

		TimeUnit( final int val, final String shortName ) {
			this.modifier = val;
			this.shortName = shortName;
		}

		@Override
		public String toString() {
			return this.shortName;
		}
	}
}
