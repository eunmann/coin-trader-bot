package coin.trader.exchange.poloniex;

import java.io.Serializable;
import java.util.List;

import coin.trader.exchange.BuyOrder;

public class PoloniexBuyOrder implements Serializable, BuyOrder {
	private static final long serialVersionUID = -148288051892410165L;

	String orderNumber = "";
	List<PoloniexTrade> tradeList;

	@Override
	public boolean isPlaced() {
		return this.orderNumber != null && !this.orderNumber.equals( "" );
	}

	@Override
	public boolean isFilled() {
		double amountSold = 0.0;
		double amount = -1.0;
		for ( final PoloniexTrade trade : this.tradeList ) {
			amountSold += trade.total;
			amount = trade.amount;
		}

		return amount == amountSold;
	}

	@Override
	public String getID() {
		return this.orderNumber;
	}

	static class PoloniexTrade {
		double amount = 0.0;
		String date = "";
		double rate = 0.0;
		double total = 0.0;
		int tradeID = 0;
		String type = "";
	}
}
