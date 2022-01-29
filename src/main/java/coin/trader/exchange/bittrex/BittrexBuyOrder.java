package coin.trader.exchange.bittrex;

import java.io.Serializable;

import coin.trader.exchange.BuyOrder;

public class BittrexBuyOrder implements BuyOrder, Serializable {
	private static final long serialVersionUID = 9021067091786407185L;

	boolean success = false;
	String message = "";

	BittrexBuyOrderResult result = null;

	@Override
	public boolean isPlaced() {
		return this.success;
	}

	@Override
	public boolean isFilled() {
		return false;
	}

	@Override
	public String getID() {
		return this.result.uuid;
	}

	static class BittrexBuyOrderResult implements Serializable {
		private static final long serialVersionUID = 7997025796803416223L;
		String uuid = "";
	}

}
