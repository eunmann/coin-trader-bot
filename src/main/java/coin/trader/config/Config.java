package coin.trader.config;

import java.io.Serializable;

import coin.trader.config.ConfigItem.SecurityLevel;
import coin.trader.logger.ConsoleLogger;
import coin.trader.logger.ILog;
import coin.trader.logger.Logger;
import coin.trader.logger.Logger.Level;

public class Config implements Serializable {
	private static final Logger LOGGER = new Logger( Config.class.getSimpleName(), Logger.Level.INFO );
	private static final long serialVersionUID = 8807958850645597838L;

	/* Static Class */
	private Config() {
	}

	public static class LoggerConfig implements Serializable {
		private static final long serialVersionUID = 626896407700076549L;
		public static final String NAME = "LoggerConfig";
		public static final ConfigItem<ILog> LOGGER_IMPL = new ConfigItem<ILog>( "LOGGER_IMPL", new ConsoleLogger(), SecurityLevel.PUBLIC );
		public static final ConfigItem<Level> LOGGING_LEVEL = new ConfigItem<Level>( "LOGGING_LEVEL", Level.INFO, SecurityLevel.PUBLIC );

		public static void print() {
			Config.tabbedPrint( LoggerConfig.NAME, 1 );
			Config.tabbedPrint( LoggerConfig.LOGGER_IMPL.toString(), 2 );
			Config.tabbedPrint( LoggerConfig.LOGGING_LEVEL.toString(), 2 );
		}
	}

	public static class BittrexConfig implements Serializable {
		private static final long serialVersionUID = -3371314101990236730L;
		public static final String NAME = "BittrexConfig";
		public static final ConfigItem<String> API_KEY_PUBLIC = new ConfigItem<String>( "API_KEY_PUBLIC", "", SecurityLevel.PRIVATE );
		public static final ConfigItem<String> API_KEY_PRIVATE = new ConfigItem<String>( "API_KEY_PRIVATE", "", SecurityLevel.PRIVATE );

		public static void print() {
			Config.tabbedPrint( BittrexConfig.NAME, 1 );
			Config.tabbedPrint( BittrexConfig.API_KEY_PUBLIC.toString(), 2 );
			Config.tabbedPrint( BittrexConfig.API_KEY_PRIVATE.toString(), 2 );
		}
	}

	public static class PoloniexConfig implements Serializable {
		private static final long serialVersionUID = -1697672367371460835L;
		public static final String NAME = "PoloniexConfig";
		public static final ConfigItem<String> API_KEY_PUBLIC = new ConfigItem<String>( "API_KEY_PUBLIC", "", SecurityLevel.PRIVATE );
		public static final ConfigItem<String> API_KEY_PRIVATE = new ConfigItem<String>( "API_KEY_PRIVATE", "", SecurityLevel.PRIVATE );

		public static void print() {
			Config.tabbedPrint( PoloniexConfig.NAME, 1 );
			Config.tabbedPrint( PoloniexConfig.API_KEY_PUBLIC.toString(), 2 );
			Config.tabbedPrint( PoloniexConfig.API_KEY_PRIVATE.toString(), 2 );
		}
	}

	private static void tabbedPrint( final String str, final int numOfTabs ) {
		final StringBuilder sb = new StringBuilder();
		for ( int i = 0; i < numOfTabs; i++ ) {
			sb.append( "\t" );
		}
		Config.LOGGER.config( sb.append( str ).toString() );
	}

	public static void print() {
		LoggerConfig.print();
		BittrexConfig.print();
		PoloniexConfig.print();
	}
}
