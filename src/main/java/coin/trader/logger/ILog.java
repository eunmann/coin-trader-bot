package coin.trader.logger;

import java.lang.reflect.Constructor;

public interface ILog {
	abstract void log( final LogEvent logEvent );

	public static ILog fromString( final String str ) {
		try {
			final Class<?> clazz = Class.forName( "coin.trader.logger." + str );
			final Constructor<?> constructor = clazz.getConstructor();
			final ILog logger = (ILog) constructor.newInstance();
			return logger;
		}
		catch ( final Exception e ) {
			e.printStackTrace();
		}

		return new ConsoleLogger();
	}

	static String formatString( final LogEvent logEvent ) {
		if ( logEvent.t == null ) {
			return String.format( Logger.FORMAT_STRING, Logger.dateFormat.format( logEvent.date ), logEvent.threadID, logEvent.outputLevel.toString(), logEvent.message );
		}
		else {
			final StringBuilder sb = new StringBuilder();
			sb.append( "\n" );

			ILog.appendThrowable( logEvent.t, sb );

			Throwable t = logEvent.t.getCause();

			while ( t != null ) {
				sb.append( "Caused by " );
				ILog.appendThrowable( t, sb );
				t = t.getCause();
			}

			return String.format( Logger.FORMAT_STRING_EXCEPTION, Logger.dateFormat.format( logEvent.date ), logEvent.threadID, logEvent.outputLevel.toString(), logEvent.message,
					sb.toString() );
		}
	}

	static void appendThrowable( final Throwable t, final StringBuilder sb ) {
		final StackTraceElement[] stacktrace = t.getStackTrace();
		sb.append( t.getClass().getSimpleName() );
		sb.append( ": " );
		sb.append( t.getMessage() );
		sb.append( "\n" );

		for ( final StackTraceElement stackTraceElement : stacktrace ) {
			sb.append( "    " );
			sb.append( stackTraceElement.getClassName() );
			sb.append( "." );
			sb.append( stackTraceElement.getMethodName() );
			sb.append( ":" );
			sb.append( stackTraceElement.getLineNumber() );
			sb.append( "\n" );
		}
	}
}
