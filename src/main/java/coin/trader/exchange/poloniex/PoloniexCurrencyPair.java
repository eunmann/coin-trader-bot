package coin.trader.exchange.poloniex;

import coin.trader.exchange.Currency;
import coin.trader.exchange.CurrencyPair;

public class PoloniexCurrencyPair extends CurrencyPair {
	public PoloniexCurrencyPair( final String currencyPairStr ) {
		super();
		final String[] split = currencyPairStr.split( "_" );
		this.base = new Currency( split[0] );
		this.quote = new Currency( split[1] );
	}

	@Override
	public String toString() {
		return this.base + "_" + this.quote;
	}
}
