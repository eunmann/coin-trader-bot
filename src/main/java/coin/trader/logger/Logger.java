package coin.trader.logger;

import java.text.SimpleDateFormat;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import coin.trader.config.Config;

/* TODO(EMU): Print the throwables in the message */
public class Logger {
	/* Default formatting */
	protected static final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS XXX" );
	protected static final String FORMAT_STRING = "[ %s | %-2d | %-6s ]: %s";
	protected static final String FORMAT_STRING_EXCEPTION = "[ %s | %-2d | %-6s ]: %s%s";

	/* Logging thread */
	private static final LogThread logThread = new LogThread();

	/* Instance variables */
	protected Level level = Level.OFF;
	protected final String name;

	public Logger( final String name, final Level level ) {
		this.name = name;
		this.level = level;
	}

	public Logger( final String name ) {
		this.name = name;
		this.level = Config.LoggerConfig.LOGGING_LEVEL.getValue();
	}

	/**
	 * Log an error message. This is typically an error that cannot be recovered
	 * from.
	 * 
	 * @param message
	 */
	public void error( final String message ) {
		Logger.logThread.addLogEvent( new LogEvent( message, this.level, Level.ERROR, null ) );
	}

	/**
	 * Log a warning message. This is typically an error but the system can
	 * recover.
	 * 
	 * @param message
	 */
	public void warn( final String message ) {
		Logger.logThread.addLogEvent( new LogEvent( message, this.level, Level.WARN, null ) );
	}

	/**
	 * Log a configuration message. This is typically a message that outputs
	 * once and shows information about the system.
	 * 
	 * @param message
	 */
	public void config( final String message ) {
		Logger.logThread.addLogEvent( new LogEvent( message, this.level, Level.CONFIG, null ) );
	}

	/**
	 * Log a information message. This is typically a message that will occur
	 * multiple times in the program that shows meaningful information.
	 * 
	 * @param message
	 */
	public void info( final String message ) {
		Logger.logThread.addLogEvent( new LogEvent( message, this.level, Level.INFO, null ) );
	}

	/**
	 * Logs a debug message. This is typically a message that detail a lot of
	 * information about the program that is useful for debugging issues.
	 * 
	 * @param message
	 */
	public void debug( final String message ) {
		Logger.logThread.addLogEvent( new LogEvent( message, this.level, Level.DEBUG, null ) );
	}

	/**
	 * Logs a trace message. This is the lowest level message and is used to
	 * provided the most information.
	 * 
	 * @return
	 */
	public void trace( final String message ) {
		Logger.logThread.addLogEvent( new LogEvent( message, this.level, Level.TRACE, null ) );
	}

	/**
	 * Log an error message. This is typically an error that cannot be recovered
	 * from.
	 * 
	 * @param message
	 */
	public void error( final String message, final Throwable t ) {
		Logger.logThread.addLogEvent( new LogEvent( message, this.level, Level.ERROR, t ) );
	}

	/**
	 * Log a warning message. This is typically an error but the system can
	 * recover.
	 * 
	 * @param message
	 */
	public void warn( final String message, final Throwable t ) {
		Logger.logThread.addLogEvent( new LogEvent( message, this.level, Level.WARN, t ) );
	}

	/**
	 * Log a configuration message. This is typically a message that outputs
	 * once and shows information about the system.
	 * 
	 * @param message
	 */
	public void config( final String message, final Throwable t ) {
		Logger.logThread.addLogEvent( new LogEvent( message, this.level, Level.CONFIG, t ) );
	}

	/**
	 * Log a information message. This is typically a message that will occur
	 * multiple times in the program that shows meaningful information.
	 * 
	 * @param message
	 */
	public void info( final String message, final Throwable t ) {
		Logger.logThread.addLogEvent( new LogEvent( message, this.level, Level.INFO, t ) );
	}

	/**
	 * Logs a debug message. This is typically a message that detail a lot of
	 * information about the program that is useful for debugging issues.
	 * 
	 * @param message
	 */
	public void debug( final String message, final Throwable t ) {
		Logger.logThread.addLogEvent( new LogEvent( message, this.level, Level.DEBUG, t ) );
	}

	/**
	 * Logs a trace message. This is the lowest level message and is used to
	 * provided the most information.
	 * 
	 * @return
	 */
	public void trace( final String message, final Throwable t ) {
		Logger.logThread.addLogEvent( new LogEvent( message, this.level, Level.TRACE, t ) );
	}

	public boolean isErrorEnabled() {
		return this.level.compareTo( Level.ERROR ) >= 0;
	}

	public boolean isWarnEnabled() {
		return this.level.compareTo( Level.WARN ) >= 0;
	}

	public boolean isConfigEnabled() {
		return this.level.compareTo( Level.CONFIG ) >= 0;
	}

	public boolean isInfoEnabled() {
		return this.level.compareTo( Level.INFO ) >= 0;
	}

	public boolean isDebugEnabled() {
		return this.level.compareTo( Level.DEBUG ) >= 0;
	}

	public boolean isTraceEnabled() {
		return this.level.compareTo( Level.TRACE ) >= 0;
	}

	public enum Level {
		OFF,
		ERROR,
		WARN,
		CONFIG,
		INFO,
		DEBUG,
		TRACE
	}

	private static void start() {
		if ( !Logger.logThread.isAlive() ) {
			Logger.logThread.start();
		}
	}

	protected static class LogThread extends Thread {
		/* TODO( EMU ): Maybe implement a ring buffer for better performance */
		protected Queue<LogEvent> logEventQueue = new ConcurrentLinkedQueue<LogEvent>();
		private final Object EMPTY_LIST_LOCK = new Object();

		protected void addLogEvent( final LogEvent logEvent ) {
			Logger.start();

			this.logEventQueue.add( logEvent );

			synchronized ( this.EMPTY_LIST_LOCK ) {
				this.EMPTY_LIST_LOCK.notifyAll();
			}
		}

		@Override
		public void run() {
			while ( true ) {
				while ( this.logEventQueue.isEmpty() ) {
					synchronized ( this.EMPTY_LIST_LOCK ) {
						try {
							this.EMPTY_LIST_LOCK.wait();
						}
						catch ( final InterruptedException e ) {
						}
					}
				}

				final LogEvent logEvent = this.logEventQueue.poll();
				if ( logEvent != null ) {
					if ( logEvent.loggerLevel.compareTo( logEvent.outputLevel ) >= 0 ) {
						Config.LoggerConfig.LOGGER_IMPL.getValue().log( logEvent );
					}
				}
			}
		}
	}
}
