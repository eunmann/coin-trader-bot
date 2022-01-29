package coin.trader.exchange.poloniex;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import coin.trader.exchange.OpenOrder;
import coin.trader.exchange.OpenOrders;

public class PoloniexOpenOrders implements OpenOrders {

	final List<PoloniexOpenOrder> openOrders;

	PoloniexOpenOrders( final String jsonStr ) {
		final Type listType = new TypeToken<Map<String, PoloniexOpenOrder>>() {
		}.getType();
		final Gson gson = new Gson();
		final Map<String, PoloniexOpenOrder> openOrdersMap = gson.fromJson( jsonStr, listType );
		this.openOrders = new ArrayList<PoloniexOpenOrder>( 128 );

		final Iterator<Entry<String, PoloniexOpenOrder>> iterator = openOrdersMap.entrySet().iterator();
		while ( iterator.hasNext() ) {
			final Entry<String, PoloniexOpenOrder> entry = iterator.next();
			this.openOrders.add( entry.getValue() );
		}
	}

	PoloniexOpenOrder getOpenOrder( final String id ) {
		/* Search for the open order */
		for ( int i = 0, len = this.openOrders.size(); i < len; i++ ) {
			final PoloniexOpenOrder openOrder = this.openOrders.get( i );
			if ( openOrder.orderNumber.equals( id ) ) {
				return openOrder;
			}
		}

		/*
		 * If the open order was not found, it is closed So, mock up an object
		 * that represents that it is closed
		 */
		return new PoloniexOpenOrder();
	}

	@Override
	public List<? extends OpenOrder> getOpenOrders() {
		return this.openOrders;
	}

}
