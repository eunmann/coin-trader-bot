package coin.trader.exchange;

public class TransactionHistory {
	private double wallet = 1.0;

	public TransactionHistory() {

	}

	/*
	 * TODO( EMU ): Right now, this assume we only start with one currency and
	 * end up with only that currency. This needs to be generalized to show
	 * gains for any currency pair
	 */
	public void addTransaction( final Transaction transaction ) {
		this.wallet += transaction.getGains();
	}

	public double getWallet() {
		return this.wallet;
	}

	@Override
	public String toString() {
		return "Wallet Amount: " + this.wallet;
	}
}
