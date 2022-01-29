package coin.trader.logger;

import java.util.Date;

import coin.trader.logger.Logger.Level;

class LogEvent {
	final long threadID;
	final Date date;
	final String message;
	final Level loggerLevel;
	final Level outputLevel;
	final Throwable t;

	LogEvent( final String message, final Level loggerLevel, final Level outputLevel, final Throwable t ) {
		this.threadID = Thread.currentThread().getId();
		this.date = new Date();
		this.message = message;
		this.loggerLevel = loggerLevel;
		this.outputLevel = outputLevel;
		this.t = t;
	}
}
