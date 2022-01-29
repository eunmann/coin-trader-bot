package coin.trader.exchange.poloniex;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import coin.trader.exchange.Balance;
import coin.trader.exchange.Balances;
import coin.trader.exchange.Currency;

public class PoloniexBalances implements Balances {

	final Map<String, Double> balanceMap;
	final List<Balance> balances;

	PoloniexBalances( final String jsonStr ) {
		final Type listType = new TypeToken<Map<String, Double>>() {
		}.getType();
		final Gson gson = new Gson();
		this.balanceMap = gson.fromJson( jsonStr, listType );
		this.balances = new ArrayList<Balance>( 128 );

		final Iterator<Entry<String, Double>> iterator = this.balanceMap.entrySet().iterator();
		while ( iterator.hasNext() ) {
			final Entry<String, Double> entry = iterator.next();
			this.balances.add( new Balance( new Currency( entry.getKey() ), entry.getValue() ) );
		}
	}

	@Override
	public Balance getBalance( final Currency currency ) {
		final Double value = this.balanceMap.get( currency.getName() );
		if ( value != null ) {
			return new Balance( currency, value );
		}
		else {
			return new Balance( currency, 0 );
		}
	}

	@Override
	public List<Balance> getAllBalances() {
		return this.balances;
	}

}
