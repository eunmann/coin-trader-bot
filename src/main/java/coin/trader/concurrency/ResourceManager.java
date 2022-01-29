package coin.trader.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import coin.trader.logger.Logger;

public class ResourceManager {
	private static final Logger LOGGER = new Logger( ResourceManager.class.getSimpleName() );

	public static final int OPTIMAL_NUMBER_OF_THREADS = Runtime.getRuntime().availableProcessors();
	/**
	 * Used for heavy calculations that will take some time.
	 */
	private static ExecutorService heavyLoadThreadpool = null;

	/**
	 * Used for light calculations that will that little time.
	 */
	private static ExecutorService lightLoadThreadpool = null;

	private static void ensureInit() {
		if ( ResourceManager.heavyLoadThreadpool == null || ResourceManager.lightLoadThreadpool == null ) {
			ResourceManager.init();
		}
	}

	private static void init() {
		ResourceManager.LOGGER.config( "Initializing " + ResourceManager.class.getSimpleName() );
		ResourceManager.LOGGER.config( "Optimal Number of Threads: " + ResourceManager.OPTIMAL_NUMBER_OF_THREADS );
		ResourceManager.LOGGER.config( "Starting Heavy Load Thread Pool with " + ResourceManager.OPTIMAL_NUMBER_OF_THREADS + " threads." );
		ResourceManager.heavyLoadThreadpool = Executors.newFixedThreadPool( ResourceManager.OPTIMAL_NUMBER_OF_THREADS );
		ResourceManager.LOGGER.config( "Starting Light Load Thread Pool with " + ResourceManager.OPTIMAL_NUMBER_OF_THREADS * 4 + " threads." );
		ResourceManager.lightLoadThreadpool = Executors.newFixedThreadPool( ResourceManager.OPTIMAL_NUMBER_OF_THREADS * 4 );
		ResourceManager.LOGGER.config( "Initialized " + ResourceManager.class.getSimpleName() );
	}

	public static void sumbitHeavyRunnable( final Runnable Runnable ) {
		ResourceManager.ensureInit();
		ResourceManager.heavyLoadThreadpool.submit( Runnable );
	}

	public static void sumbitLightRunnable( final Runnable Runnable ) {
		ResourceManager.ensureInit();
		ResourceManager.lightLoadThreadpool.submit( Runnable );
	}
}
