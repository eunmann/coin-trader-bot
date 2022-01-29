package coin.trader.exchange;

public class Balance {
	private final Currency currency;
	private final double amount;

	public Balance( final Currency currency, final double amount ) {
		this.currency = currency;
		this.amount = amount;
	}

	public Currency getCurrency() {
		return this.currency;
	}

	public double getAmount() {
		return this.amount;
	}

	@Override
	public boolean equals( final Object obj ) {
		return obj instanceof Balance && this.currency.equals( ((Balance) obj).currency );
	}
}
