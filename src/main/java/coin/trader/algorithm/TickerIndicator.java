package coin.trader.algorithm;

import coin.trader.exchange.Ticker;

public interface TickerIndicator {
	public void addSample( final Ticker ticker );

	public boolean shouldBuy();

	public boolean shouldSell( final double buyPrice, final double fee );

	public Ticker getTicker();

	public double getCandidacyWeight();
}
