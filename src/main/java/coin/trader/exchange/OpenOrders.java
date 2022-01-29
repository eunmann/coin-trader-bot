package coin.trader.exchange;

import java.util.List;

import coin.trader.exchange.ExchangeMonitor.Job.JobArgs;

public interface OpenOrders {
	public List<? extends OpenOrder> getOpenOrders();

	public static class OpenOrdersJobArgs extends JobArgs {
		final CurrencyPair currencyPair;

		public OpenOrdersJobArgs( final CurrencyPair currencyPair ) {
			this.currencyPair = currencyPair;
		}
	}
}
