package coin.trader.exchange;

/**
 * Generic class for currency pairs.
 * 
 * @author Evan
 *
 */
public class CurrencyPair {
	protected Currency base;
	protected Currency quote;

	protected CurrencyPair() {

	}

	public CurrencyPair( final Currency base, final Currency quote ) {
		this.base = base;
		this.quote = quote;
	}

	public Currency getBase() {
		return this.base;
	}

	public Currency getQuote() {
		return this.quote;
	}

	@Override
	public int hashCode() {
		return (this.base.toString() + this.quote.toString()).hashCode();
	}

	@Override
	public boolean equals( final Object obj ) {
		if ( obj instanceof CurrencyPair ) {
			final CurrencyPair currencyPair = (CurrencyPair) obj;
			return this.base.equals( currencyPair.getBase() ) && this.quote.equals( currencyPair.getQuote() );
		}
		else {
			return false;
		}

	}
}
