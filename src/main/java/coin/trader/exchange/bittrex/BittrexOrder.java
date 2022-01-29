package coin.trader.exchange.bittrex;

import java.io.Serializable;

import coin.trader.exchange.Order;

class BittrexOrder implements Order, Serializable {
	private static final long serialVersionUID = -4328495109088490653L;

	double Quantity;
	double Rate;

	@Override
	public double getQuantity() {
		return this.Quantity;
	}

	@Override
	public double getRate() {
		return this.Rate;
	}
}
