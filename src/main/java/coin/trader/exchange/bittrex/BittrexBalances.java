package coin.trader.exchange.bittrex;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import coin.trader.exchange.Balance;
import coin.trader.exchange.Balances;
import coin.trader.exchange.Currency;

public class BittrexBalances implements Balances, Serializable {
	private static final long serialVersionUID = -5431783389935909565L;

	boolean success = false;
	String message = "";

	List<BittrexBalance> result = null;

	@Override
	public Balance getBalance( final Currency currency ) {
		for ( final BittrexBalance balance : this.result ) {
			if ( balance.Currency.equals( currency.getName() ) ) {
				return new Balance( currency, balance.Available );
			}
		}

		return new Balance( currency, 0.0 );
	}

	@Override
	public List<Balance> getAllBalances() {
		final List<Balance> balances = new ArrayList<Balance>( this.result.size() );

		for ( final BittrexBalance balance : this.result ) {
			balances.add( new Balance( new Currency( balance.Currency ), balance.Available ) );
		}

		return balances;
	}

	static class BittrexBalance implements Serializable {
		private static final long serialVersionUID = 2271259667118441884L;

		String Currency = "";
		double Balance = 0.0;
		double Available = 0.0;
		double Pending = 0.0;
		String CryptoAddress = "";
		boolean Requested = false;
		String Uuid = "";
	}

}
