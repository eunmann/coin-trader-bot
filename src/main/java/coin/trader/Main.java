package coin.trader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.trader.powerball.Drawing;
import com.trader.powerball.PowerballHistory;
import com.trader.powerball.PowerballHistoryAnalyzerNN;

import coin.trader.chart.ChartAnalyzer;
import coin.trader.config.ConfigFile;
import coin.trader.exchange.poloniex.PoloniexCurrencyPair;
import coin.trader.exchange.poloniex.PoloniexExchange;
import coin.trader.logger.Logger;
import coin.trader.math.Matrix;
import coin.trader.math.MatrixJCublas;
import coin.trader.utilities.ArrayUtils;
import coin.trader.utilities.Utils;

/* TODO( EMU ): Eventually convert this to a GUI */
/* TODO( EMU ): Write API comments for everything */
/* TODO( EMU ): Add more exchanges */
/* TODO( EMU ): Fix the visibility of objects */
public class Main {
	public static final Logger LOGGER = new Logger( Main.class.getSimpleName(), Logger.Level.DEBUG );

	public static void main( final String[] args ) {
		Main.init();

		Main.runChartHistoryAnalyzer();
	}

	private static void runChartHistoryAnalyzer() {

		final ChartAnalyzer chartAnalyzer = new ChartAnalyzer( PoloniexExchange.getInstance(), new PoloniexCurrencyPair( "BTC_ETH" ) );

		final long end = Main.toUnixtimestamp( 0, 0, 0 );
		final long start = Main.toUnixtimestamp( 0, 0, 14 );
		chartAnalyzer.anaylize( start, end, 300 );
	}

	private static long toUnixtimestamp( final int mins, final int hours, final int days ) {
		final int offset = mins * 60 + hours * 60 * 60 + days * 24 * 60 * 60;
		return Utils.getUnixTimestamp() - offset;
	}

	private static void runPowerballAnalyzer() {
		final PowerballHistoryAnalyzerNN powerballHistoryAnalyzer = new PowerballHistoryAnalyzerNN();

		Main.LOGGER.info( "Analyzing powerball numbers..." );

		final String startDate = "2015-10-07";
		final String endDate = "2018-09-01";
		final Matrix output = powerballHistoryAnalyzer.analyze( startDate, endDate );
		final Drawing drawing = new Drawing( output );

		Main.LOGGER.info( "Done" );
		Main.LOGGER.info( "Ouput: " + drawing );

		final PowerballHistory powerballHistory = new PowerballHistory( startDate, endDate );

		Main.LOGGER.info( "Most Winning White Numbers: " + ArrayUtils.toString( powerballHistory.getSortedByWinningWhiteNumbersDecending() ) );
		Main.LOGGER.info( "Least Winning White Numbers: " + ArrayUtils.toString( powerballHistory.getSortedByWinningWhiteNumbersAcending() ) );
		Main.LOGGER.info( "Most Winning Red Numbers: " + ArrayUtils.toString( powerballHistory.getSortedByWinningRedNumbersDecending() ) );
		Main.LOGGER.info( "Least Winning Red Numbers: " + ArrayUtils.toString( powerballHistory.getSortedByWinningRedNumbersAcending() ) );
	}

	private static void init() {
		/* Check for CoinTraderBot directory */
		Main.checkDefaultDirectory();

		/* Apply any config file found in the default directory */
		Main.applyConfig();

		/* Print out information about the program */
		Info.print();

		/* Add shutdown hook */
		Runtime.getRuntime().addShutdownHook( new Thread() {
			@Override
			public void run() {
				Main.LOGGER.info( "Shutting down..." );

				/* JCublas */
				Main.LOGGER.info( "JCublas shutting down..." );
				MatrixJCublas.shutdownJCublas();
				Main.LOGGER.info( "JCublas shutting down" );

				/* OpenCL */
				Main.LOGGER.info( "OpenCL shutting down..." );
				Main.LOGGER.info( "OpenCL shutdown" );

				Main.LOGGER.info( "Shutdown complete" );
			}
		} );
	}

	private static void checkDefaultDirectory() {
		/* First, check for the directory */
		final File defaultDir = new File( Info.DIRECTORY_PATH );
		if ( !defaultDir.isDirectory() ) {
			Main.LOGGER.error( Info.DIRECTORY_PATH + " did not exist. Creating directory." );

			if ( defaultDir.mkdirs() ) {
				Main.LOGGER.error( "Could not create the default directory: " + Info.DIRECTORY_PATH );
			}
		}

		/* Check for a config file */
		final File configFile = new File( Info.CONFIG_PATH );
		if ( !configFile.exists() ) {
			Main.LOGGER.error( Info.CONFIG_PATH + " did not exist. Creating template config file. You will need to add confirguration properties before being able to trade." );
			FileWriter fw = null;
			BufferedWriter bw = null;
			try {
				configFile.createNewFile();
				fw = new FileWriter( configFile.getAbsolutePath() );
				bw = new BufferedWriter( fw );
				bw.write( ConfigFile.getTemplateAsString() );
				bw.flush();
				fw.flush();
			}
			catch ( final IOException e ) {
			}
			finally {
				try {
					if ( fw != null ) {
						fw.close();
					}

					if ( bw != null ) {
						bw.close();
					}
				}
				catch ( final IOException e ) {
				}
			}
		}
	}

	private static void applyConfig() {
		if ( !ConfigFile.applyConfig() ) {
			System.out.println( "Configuration from " + Info.CONFIG_PATH + " could not be applied. Ensure the configuration file is correct." );
			if ( !Info.isTestRun ) {
				throw new RuntimeException( "Aborting, the program needs all configuration values to operate." );
			}
		}
	}
}
