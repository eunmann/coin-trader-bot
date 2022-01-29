package coin.trader.algorithm;

import coin.trader.Info;
import coin.trader.algorithm.history.HistoryAlgExecutor;
import coin.trader.algorithm.history.HistoryAlgTester;
import coin.trader.algorithm.history.HistoryTickerIndicatorFactory;
import coin.trader.exchange.Exchange;

public class AlgFactory {
	public static AlgExecutor getAlgExecutor( final Exchange exchange, final TickerIndicatorFactory factory ) {
		if ( Info.isTestRun ) {
			return new AlgTester( exchange, factory );
		}
		else {
			return new AlgExecutor( exchange, factory );
		}
	}

	public static HistoryAlgExecutor getHistoryAlgExecutor( final Exchange exchange, final HistoryTickerIndicatorFactory factory ) {
		if ( Info.isTestRun ) {
			return new HistoryAlgTester( exchange, factory );
		}
		else {
			return new HistoryAlgExecutor( exchange, factory );
		}
	}
}
