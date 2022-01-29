package coin.trader.algorithm;

import coin.trader.exchange.Exchange;

public interface TickerIndicatorFactory {
	public TickerIndicator createTickerIndicator();

	public TickerMonitorThread getTickerMonitorThread( final Exchange exchange );

	public String getAlgorithmName();

	public int getAmountOfSamples();
}
