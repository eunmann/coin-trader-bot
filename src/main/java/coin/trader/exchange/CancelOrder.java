package coin.trader.exchange;

import coin.trader.exchange.ExchangeMonitor.Job.JobArgs;

public interface CancelOrder {
	public boolean isCanceled();

	static class CancelOrderJobArgs extends JobArgs {
		final String orderID;

		public CancelOrderJobArgs( final String orderId ) {
			this.orderID = orderId;
		}
	}
}
