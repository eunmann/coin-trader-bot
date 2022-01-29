package coin.trader.exchange.bittrex;

import java.io.Serializable;

import coin.trader.exchange.CancelOrder;

public class BittrexCancelOrder implements CancelOrder, Serializable {
	private static final long serialVersionUID = 1312656384485798143L;

	boolean success = false;
	String message = "";
	Object result = null;

	@Override
	public boolean isCanceled() {
		return this.success;
	}

}
