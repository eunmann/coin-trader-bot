package coin.trader.logger;

import coin.trader.logger.Logger.Level;

public class ConsoleLogger implements ILog {
	public ConsoleLogger() {
	}

	@Override
	public void log( final LogEvent logEvent ) {
		final String output = ILog.formatString( logEvent );

		if ( logEvent.outputLevel.equals( Level.ERROR ) ) {
			System.err.println( output );
		}
		else {
			System.out.println( output );
		}
	}

	@Override
	public String toString() {
		return ConsoleLogger.class.getSimpleName();
	}
}
