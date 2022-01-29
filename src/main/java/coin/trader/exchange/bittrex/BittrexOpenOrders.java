package coin.trader.exchange.bittrex;

import java.io.Serializable;
import java.util.List;

import coin.trader.exchange.OpenOrder;
import coin.trader.exchange.OpenOrders;

public class BittrexOpenOrders implements OpenOrders, Serializable {
	private static final long serialVersionUID = -3418858971770715429L;

	boolean success;
	String message;
	List<BittrexOpenOrder> result;

	@Override
	public List<? extends OpenOrder> getOpenOrders() {
		return this.result;
	}
}
