package coin.trader.exchange;

/**
 * Generic ticker object
 * 
 * @author Evan
 *
 */
public interface Ticker {
	/**
	 * Get the currency pair for this ticker
	 * 
	 * @return
	 */
	public CurrencyPair getCurrencyPair();

	/**
	 * Get the last trade price
	 * 
	 * @return
	 */
	public double getLast();

	/**
	 * Get the loweset asking price
	 */
	public double getAsk();

	/**
	 * Get the lowest asking price
	 */
	public double getBid();

	/**
	 * Get the base volume
	 */
	public double getBaseVolume();

	/**
	 * Get the quote volume
	 */
	public double getQuoteVolume();
}
