package coin.trader;

import java.io.File;

import coin.trader.config.Config;
import coin.trader.logger.Logger;

/**
 * Used to store information about the program
 * 
 * @author Evan
 *
 */
public class Info {
	private static final Logger LOGGER = new Logger( Info.class.getSimpleName(), Logger.Level.DEBUG );

	/* Information about the program */
	public static final String PROGRAM_NAME = "CoinTraderBot";
	public static final String VERSION = "0.8.0";
	public static final boolean isTestRun = true;
	public static final long START_TIME_STAMP = System.currentTimeMillis();
	public static final String DIRECTORY_PATH = System.getProperty( "user.home" ) + File.separator + "CoinTraderBot" + File.separator;
	public static final String CONFIG_PATH = Info.DIRECTORY_PATH + "config.json";
	public static final String LOG_PATH = Info.DIRECTORY_PATH + "CoinTraderBot.log";

	public static void print() {
		Info.LOGGER.info( Info.PROGRAM_NAME );
		Info.LOGGER.info( "Version: " + Info.VERSION );
		Info.LOGGER.config( "Default directory: " + Info.DIRECTORY_PATH );
		Info.LOGGER.config( "Configuration Path: " + Info.CONFIG_PATH );
		Info.LOGGER.config( "Log Path: " + Info.LOG_PATH );
		Info.LOGGER.config( "Configuration Settings:" );
		Config.print();

		if ( Info.isTestRun ) {
			Info.LOGGER.info( "Test Run Enabled. Orders will not be executed." );
		}
	}
}
