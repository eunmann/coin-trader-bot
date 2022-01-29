package coin.trader.algorithm;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import coin.trader.logger.Logger;

/**
 * This class is used to broadcast after adding a new sample
 * 
 * @author Evan
 *
 */
public class SampleListenerManager {
	private static final Logger LOGGER = new Logger( SampleListenerManager.class.getSimpleName() );
	private final List<SampleListener> listeners = new CopyOnWriteArrayList<SampleListener>();

	public SampleListenerManager() {

	}

	public void addSampleListener( final SampleListener listener ) {
		this.listeners.add( listener );
	}

	public void removeSampleListener( final SampleListener listener ) {
		this.listeners.remove( listener );
	}

	public void broadcast( final List<TickerIndicator> tickers ) {
		for ( final SampleListener listener : this.listeners ) {
			try {
				listener.onNewSample( tickers );
			}
			catch ( final Throwable t ) {
			}
		}
	}

	public static interface SampleListener {
		public void onNewSample( final List<TickerIndicator> indicators );
	}
}
