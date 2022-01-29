package coin.trader.exchange;

/**
 * Generic class for a currency
 * 
 * @author Evan
 *
 */
public class Currency {
	private final String name;

	public Currency( final String name ) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public boolean equals( final Object obj ) {
		if ( obj instanceof Currency ) {
			return this.name.equals( ((Currency) obj).getName() );
		}
		else {
			return false;
		}
	}
}
