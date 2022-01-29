package coin.trader.utilities;

/**
 * For asynchronous calls and multithreaded calls, this provides the callbacks
 * 
 * @author Evan
 *
 */
public abstract class Callback {

	public Callback() {
	}

	/**
	 * Default error handler, override when necessary
	 * 
	 * @param e
	 */
	public abstract void error( final Throwable t );

	/**
	 * Used for callbacks that need to return something
	 * 
	 * @author Evan
	 *
	 * @param <T>
	 */
	public static abstract class Return<T> extends Callback {
		public Return() {
		}

		public abstract void succeeded( final T rv );
	}

	/**
	 * Used for callbacks that do not return anything
	 * 
	 * @author Evan
	 *
	 */
	public static abstract class NoReturn extends Callback {
		public NoReturn() {
		}

		public abstract void succeeded();
	}
}
