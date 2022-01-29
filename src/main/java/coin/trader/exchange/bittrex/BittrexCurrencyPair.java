
package coin.trader.exchange.bittrex;

import coin.trader.exchange.Currency;
import coin.trader.exchange.CurrencyPair;

class BittrexCurrencyPair extends CurrencyPair {
	public BittrexCurrencyPair( final String str ) {
		super();
		final String[] split = str.split( "-" );
		this.base = new Currency( split[0] );
		this.quote = new Currency( split[1] );
	}

	@Override
	public String toString() {
		return this.base + "-" + this.quote;
	}
}
