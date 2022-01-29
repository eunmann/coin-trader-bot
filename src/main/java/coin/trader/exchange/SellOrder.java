package coin.trader.exchange;

import coin.trader.exchange.ExchangeMonitor.Job.JobArgs;

public interface SellOrder {
	public boolean isPlaced();

	public boolean isFilled();

	public String getID();

	public static class SellOrderJobArgs extends JobArgs {
		final CurrencyPair currencyPair;
		final double quantity;
		final double rate;

		public SellOrderJobArgs( final CurrencyPair currencyPair, final double quantity, final double rate ) {
			this.currencyPair = currencyPair;
			this.quantity = quantity;
			this.rate = rate;
		}
	}
}
