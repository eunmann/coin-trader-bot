package coin.trader.exchange.poloniex;

import java.io.Serializable;
import java.util.List;

import coin.trader.exchange.SellOrder;
import coin.trader.exchange.poloniex.PoloniexBuyOrder.PoloniexTrade;

public class PoloniexSellOrder implements Serializable, SellOrder {
	private static final long serialVersionUID = 196570735813442502L;

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

}
