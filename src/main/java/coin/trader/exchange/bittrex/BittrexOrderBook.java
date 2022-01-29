package coin.trader.exchange.bittrex;

import java.io.Serializable;
import java.util.List;

import coin.trader.exchange.Order;
import coin.trader.exchange.OrderBook;

class BittrexOrderBook implements OrderBook, Serializable {
	private static final long serialVersionUID = -3886364013829933267L;

	boolean success = false;
	String message = null;

	OrderBookResult result = null;

	@Override
	public List<? extends Order> getBuyOrders() {
		return this.result.buy;
	}

	@Override
	public List<? extends Order> getSellOrders() {
		return this.result.sell;
	}

	static class OrderBookResult implements Serializable {
		private static final long serialVersionUID = 7278925692668806664L;

		List<BittrexOrder> buy;
		List<BittrexOrder> sell;
	}
}
