package coin.trader.algorithm.reaction;

import coin.trader.algorithm.TickerIndicator;
import coin.trader.algorithm.TickerIndicatorFactory;
import coin.trader.algorithm.TickerLastMonitorThread;
import coin.trader.algorithm.TickerMonitorThread;
import coin.trader.exchange.Exchange;

public class ReactionTickerIndicatorFactory implements TickerIndicatorFactory {
	private final int requestsPerSecond;

	public ReactionTickerIndicatorFactory( final int requestsPerSecond ) {
		this.requestsPerSecond = requestsPerSecond;
	}

	@Override
	public TickerIndicator createTickerIndicator() {
		return new ReactionTickerIndicator( this.requestsPerSecond );
	}

	@Override
	public String getAlgorithmName() {
		return "Reaction";
	}

	@Override
	public TickerMonitorThread getTickerMonitorThread( final Exchange exchange ) {
		return new TickerLastMonitorThread( exchange, this );
	}

	@Override
	public int getAmountOfSamples() {
		return ReactionTickerIndicator.ANALYSIS_INTERVAL_SECONDS * this.requestsPerSecond;
	}
}
