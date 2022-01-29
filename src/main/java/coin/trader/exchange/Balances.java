package coin.trader.exchange;

import java.util.List;

public interface Balances {
	public abstract Balance getBalance( final Currency currency );

	public abstract List<Balance> getAllBalances();
}
