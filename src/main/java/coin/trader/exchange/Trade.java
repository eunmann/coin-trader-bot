package coin.trader.exchange;

public class Trade {
	private final CurrencyPair currencyPair;
	private final double rate;
	private final double quantity;
	private final double fee;
	private final double feeInverse;

	public Trade( final CurrencyPair currencyPair, final double rate, final double quantity, final double fee ) {
		this.currencyPair = currencyPair;
		this.rate = rate;
		this.quantity = quantity;
		this.fee = fee;
		this.feeInverse = 1.0 - fee;
	}

	public CurrencyPair getCurrencyPair() {
		return this.currencyPair;
	}

	public double getRate() {
		return this.rate;
	}

	public double getRateWithFee() {
		return this.rate * this.feeInverse;
	}

	public double getQuantity() {
		return this.quantity;
	}

	public double getBaseQuantityWithFee() {
		return this.quantity * this.feeInverse;
	}

	public double getFee() {
		return this.fee;
	}

	@Override
	public String toString() {
		return "[ currency pair: " + this.currencyPair + ", quantity: " + this.quantity + ", rate: " + this.rate + " ]";
	}
}
