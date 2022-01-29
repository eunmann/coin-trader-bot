package coin.trader.exchange.poloniex;

import java.io.Serializable;

import coin.trader.exchange.CancelOrder;

public class PoloniexCancelOrder implements Serializable, CancelOrder {
	private static final long serialVersionUID = 7052593408252555276L;

	int success = 0;

	@Override
	public boolean isCanceled() {
		return this.success == 1;
	}
}
