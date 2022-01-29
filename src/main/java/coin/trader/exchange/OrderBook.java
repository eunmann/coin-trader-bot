package coin.trader.exchange;

import java.util.List;

import coin.trader.exchange.ExchangeMonitor.Job.JobArgs;

public interface OrderBook {
	public List<? extends Order> getBuyOrders();

	public List<? extends Order> getSellOrders();

	public static class OrderBookJobArgs extends JobArgs {
		final CurrencyPair currencyPair;
		final int depth;

		public OrderBookJobArgs( final CurrencyPair currencyPair, final int depth ) {
			this.currencyPair = currencyPair;
			this.depth = depth;
		}
	}
}
