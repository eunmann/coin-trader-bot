package coin.trader.algorithm.bollinger;

import coin.trader.algorithm.TickerIndicator;
import coin.trader.algorithm.TickerIndicatorFactory;
import coin.trader.algorithm.TickerLastMonitorThread;
import coin.trader.algorithm.TickerMonitorThread;
import coin.trader.exchange.Exchange;

public class BollingerTickerIndicatorFactory implements TickerIndicatorFactory {
	private final int sampleSize;

	public BollingerTickerIndicatorFactory( final int requestsPerSecond ) {
		this.sampleSize = requestsPerSecond * 60 * BollingerTickerIndictor.ANALYSIS_INTERVAL_MINS * BollingerTickerIndictor.ANALYSIS_NUM_OF_PERIODS;
	}

	@Override
	public TickerIndicator createTickerIndicator() {
		return new BollingerTickerIndictor( this.sampleSize );
	}

	@Override
	public TickerMonitorThread getTickerMonitorThread( final Exchange exchange ) {
		return new TickerLastMonitorThread( exchange, this );
	}

	@Override
	public String getAlgorithmName() {
		return "Bollinger";
	}

	@Override
	public int getAmountOfSamples() {
		return this.sampleSize;
	}
}
