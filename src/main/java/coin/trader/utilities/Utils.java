package coin.trader.utilities;

public class Utils {
	public static void ASSERT( final boolean eval, final String reason ) {
		if ( !eval ) {
			throw new RuntimeException( reason );
		}
	}

	public static void ASSERT_Param( final boolean eval, final String reason ) {
		if ( !eval ) {
			throw new IllegalArgumentException( reason );
		}
	}

	public static long getUnixTimestamp() {
		return System.currentTimeMillis() / 1000L;
	}
}
