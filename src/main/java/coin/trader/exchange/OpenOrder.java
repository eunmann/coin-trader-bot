package coin.trader.exchange;

import coin.trader.exchange.ExchangeMonitor.Job.JobArgs;

public interface OpenOrder {
	public boolean isClosed();

	public boolean isPartiallyFilled();

	public static class OpenOrderJobArgs extends JobArgs {
		String orderID;

		public OpenOrderJobArgs( final String orderID ) {
			this.orderID = orderID;
		}
	}
}
