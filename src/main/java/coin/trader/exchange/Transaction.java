package coin.trader.exchange;

public class Transaction {
	private final Trade buyOrder;
	private final Trade sellOrder;

	public Transaction( final Trade buyOrder, final Trade sellOrder ) {
		this.buyOrder = buyOrder;
		this.sellOrder = sellOrder;
	}

	public Trade getBuyOrder() {
		return this.buyOrder;
	}

	public Trade getSellOrder() {
		return this.sellOrder;
	}

	public double getGains() {
		return this.sellOrder.getBaseQuantityWithFee() - this.buyOrder.getBaseQuantityWithFee();
	}

	public double getRatioGains() {
		final double buyWithFee = this.buyOrder.getRateWithFee();
		return (this.sellOrder.getRateWithFee() - buyWithFee) / buyWithFee;
	}

	@Override
	public String toString() {
		return "[ Gains: " + this.getGains() + ", Ratio Gains: " + this.getRatioGains() + " ]";
	}
}
