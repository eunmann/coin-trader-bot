package coin.trader.algorithm;

import java.util.ArrayList;
import java.util.List;

import coin.trader.exchange.Balance;
import coin.trader.exchange.Balances;
import coin.trader.exchange.Currency;

class FakeBalances implements Balances {
	private double btcAmmount;

	FakeBalances( final double btcAmmount ) {
		this.btcAmmount = btcAmmount;
	}

	void updateBTCAmount( final double ammount ) {
		this.btcAmmount = ammount;
	}

	@Override
	public Balance getBalance( final Currency currency ) {
		if ( currency.getName().equals( "BTC" ) ) {
			return new Balance( currency, this.btcAmmount );
		}
		else {
			return new Balance( currency, 0.0 );
		}
	}

	@Override
	public List<Balance> getAllBalances() {
		final List<Balance> balances = new ArrayList<Balance>();
		balances.add( new Balance( new Currency( "BTC" ), this.btcAmmount ) );
		return balances;
	}

}
