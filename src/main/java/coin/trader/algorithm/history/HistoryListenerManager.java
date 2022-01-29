package coin.trader.algorithm.history;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HistoryListenerManager {
	private final List<HistoryIndicatorListener> listeners = new CopyOnWriteArrayList<HistoryIndicatorListener>();

	public HistoryListenerManager() {

	}

	public void add( final HistoryIndicatorListener listener ) {
		this.listeners.add( listener );
	}

	public void remove( final HistoryIndicatorListener listener ) {
		this.listeners.remove( listener );
	}

	public void notifyBuyTrigger( final List<HistoryTickerIndicator> historyTickerIndicator ) {
		for ( final HistoryIndicatorListener listener : this.listeners ) {
			try {
				listener.onBuyTrigger( historyTickerIndicator );
			}
			catch ( final Throwable t ) {

			}
		}
	}

	public void notifySellTrigger( final HistoryTickerIndicator historyTickerIndicator ) {
		for ( final HistoryIndicatorListener listener : this.listeners ) {
			try {
				listener.onSellTrigger( historyTickerIndicator );
			}
			catch ( final Throwable t ) {

			}
		}
	}

	public static interface HistoryIndicatorListener {
		public void onBuyTrigger( final List<HistoryTickerIndicator> historyTickerIndicator );

		public void onSellTrigger( final HistoryTickerIndicator historyTickerIndicator );
	}
}
