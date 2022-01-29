package coin.trader.exchange;

public class TradeStatistic {
	private final Currency currency;
	private final double oldAmount;
	private final double newAmount;

	public TradeStatistic( final Currency currency, final double oldAmount, final double newAmount ) {
		this.currency = currency;
		this.oldAmount = oldAmount;
		this.newAmount = newAmount;
	}

	public double getRatioGains() {
		return (this.newAmount - this.oldAmount) / this.oldAmount;
	}

	public double getAmountGains() {
		return this.newAmount - this.oldAmount;
	}

	public Currency getCurrency() {
		return this.currency;
	}

	@Override
	public String toString() {
		return "[ currencyPair: " + this.currency + ", old amount: " + this.oldAmount + ", new amount: " + this.newAmount + ", amount gains: " + this.getAmountGains()
				+ ", ratio gains: " + this.getRatioGains() + " ]";
	}
}
