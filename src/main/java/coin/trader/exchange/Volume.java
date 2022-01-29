package coin.trader.exchange;

/**
 * Generic class for volume
 * 
 * @author Evan
 *
 */
public interface Volume {
	public Currency getCurrency();

	public double getVolume();
}
