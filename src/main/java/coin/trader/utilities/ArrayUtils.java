package coin.trader.utilities;

import java.util.List;

public class ArrayUtils {
	public static <T> String toString( final T[] array ) {
		final StringBuilder rv = new StringBuilder();

		final int len = array.length - 1;
		for ( int i = 0; i < len; i++ ) {
			rv.append( array[i] );
			rv.append( ", " );
		}
		rv.append( array[len] );

		return rv.toString();
	}

	public static String toString( final float[] array ) {
		final StringBuilder rv = new StringBuilder();

		final int len = array.length - 1;
		for ( int i = 0; i < len; i++ ) {
			rv.append( array[i] );
			rv.append( ", " );
		}
		rv.append( array[len] );

		return rv.toString();
	}

	public static String toString( final int[] array ) {
		final StringBuilder rv = new StringBuilder();

		final int len = array.length - 1;
		for ( int i = 0; i < len; i++ ) {
			rv.append( array[i] );
			rv.append( ", " );
		}
		rv.append( array[len] );

		return rv.toString();
	}

	public static String toString( final List<?> list ) {
		final StringBuilder rv = new StringBuilder();

		final int len = list.size() - 1;
		for ( int i = 0; i < len; i++ ) {
			rv.append( list.get( i ) );
			rv.append( ", " );
		}
		rv.append( list.get( len ) );

		return rv.toString();
	}
}
