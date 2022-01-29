package coin.trader.exchange;

import coin.trader.exchange.ExchangeMonitor.Job.JobArgs;

public interface BuyOrder {
	public boolean isPlaced();

	public boolean isFilled();

	public String getID();

	public static class BuyOrderJobArgs extends JobArgs {
		final CurrencyPair currencyPair;
		final double quantity;
		final double rate;

		public BuyOrderJobArgs( final CurrencyPair currencyPair, final double quantity, final double rate ) {
			this.currencyPair = currencyPair;
			this.quantity = quantity;
			this.rate = rate;
		}

		@Override
		public String toString() {
			return "[ currency pair: " + this.currencyPair + ", quantity: " + this.quantity + ", rate: " + this.rate + " ]";
		}
	}
}
