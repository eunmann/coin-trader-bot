package coin.trader.logger;

public class MultiLogger implements ILog {
	private final ILog fileLogger;
	private final ILog consoleLogger;

	public MultiLogger() {
		this.fileLogger = new FileLogger();
		this.consoleLogger = new ConsoleLogger();
	}

	@Override
	public void log( final LogEvent logEvent ) {
		this.consoleLogger.log( logEvent );
		this.fileLogger.log( logEvent );
	}

	@Override
	public String toString() {
		return MultiLogger.class.getSimpleName();
	}
}
