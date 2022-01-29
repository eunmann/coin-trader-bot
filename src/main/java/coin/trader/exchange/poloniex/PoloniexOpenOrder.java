package coin.trader.exchange.poloniex;

import java.io.Serializable;

import coin.trader.exchange.OpenOrder;

public class PoloniexOpenOrder implements Serializable, OpenOrder {
	private static final long serialVersionUID = 1L;

	String orderNumber = "";
	String type = "";
	double rate = 0.0;
	double amount = 0.0;
	double total = 0.0;

	@Override
	public boolean isClosed() {
		return this.amount == this.total;
	}

	@Override
	public boolean isPartiallyFilled() {
		return this.total > 0.0;
	}
}
