package coin.trader.exchange.bittrex;

import java.io.Serializable;

import coin.trader.exchange.SellOrder;

public class BittrexSellOrder implements SellOrder, Serializable {
	private static final long serialVersionUID = 6212711298835254902L;

	boolean success = false;
	String message = "";
	BittrexSellOrderResult result = null;

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

	static class BittrexSellOrderResult implements Serializable {
		private static final long serialVersionUID = 7997025796803416223L;
		String uuid = "";
	}
}
