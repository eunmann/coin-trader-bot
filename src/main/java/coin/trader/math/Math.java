package coin.trader.math;

public class Math {
	public static final float e = 2.71828182845904523536028747135266f;

	public static double average( final double[] array ) {
		final double sum = Math.getSum( array );
		return sum / array.length;
	}

	public static double standardDeviation( final double[] array ) {
		final double average = Math.average( array );
		return Math.standardDeviation( array, average );
	}

	public static double standardDeviation( final double[] array, final double average ) {
		double sum = 0;
		int i = 0;
		final int len = array.length;
		for ( ; i < len - 4; i += 4 ) {
			final double a = array[i];
			final double b = array[i + 1];
			final double c = array[i + 2];
			final double d = array[i + 3];

			final double diff1 = a - average;
			final double diff2 = b - average;
			final double diff3 = c - average;
			final double diff4 = d - average;

			final double a2 = diff1 * diff1;
			final double b2 = diff2 * diff2;
			final double c2 = diff3 * diff3;
			final double d2 = diff4 * diff4;

			sum += a2 + b2 + c2 + d2;
		}

		for ( ; i < len; i++ ) {
			final double diff = array[i] - average;
			sum += diff * diff;
		}

		final double variance = sum / len;
		final double std = java.lang.Math.sqrt( variance );
		return std;
	}

	/**
	 * Returns the Bollinger Bands for the given array.
	 * 
	 * @param array
	 *            - array of samples
	 * 
	 * @return The first element is the low band, the second element is the high
	 *         band
	 */
	public static double[] getBollingerBands( final double[] array ) {
		final double average = Math.average( array );
		final double std = Math.standardDeviation( array, average );
		return Math.getBollingerBands( average, std );
	}

	/**
	 * Returns the Bollinger Bands for the given array.
	 * 
	 * @param array
	 *            - The array of samples
	 * @param average
	 *            - The average of the array
	 * @param std
	 *            - The standard deviation of the array
	 * @return
	 */
	public static double[] getBollingerBands( final double average, final double std ) {
		final double[] bollingerBands = new double[2];
		final double std2 = std * 2;
		bollingerBands[0] = average - std2;
		bollingerBands[1] = average + std2;
		return bollingerBands;
	}

	public static double normalize( final double value, final double min, final double max ) {
		return (value - min) / (max - min);
	}

	public static double getRSI( final double[] array ) {
		double gains = 0;
		double losses = 0;

		for ( int i = 0, len = array.length; i < len; i++ ) {
			final double value = array[i];

			if ( value >= 0.0 ) {
				gains += value;
			}
			else {
				losses += value;
			}
		}

		gains /= array.length;
		losses /= array.length;

		return (100 - (100.0 / (1 + gains / losses)));
	}

	public static double getSum( final double[] array ) {
		double sum = 0;

		for ( final double value : array ) {
			sum += value;
		}

		return sum;
	}

	public static double getSquaredSum( final double[] array ) {
		double sum = 0;

		for ( final double value : array ) {
			sum += value * value;
		}

		return sum;
	}

	public static double getProductSum( final double[] array1, final double[] array2 ) {
		double sum = 0;
		for ( int i = 0, len = array1.length; i < len; i++ ) {
			sum += array1[i] * array2[i];
		}
		return sum;
	}

	public static double getMaximum( final double[] array ) {
		double max = array[0];

		for ( int i = 1, len = array.length; i < len; i++ ) {
			final double value = array[i];

			if ( value > max ) {
				max = value;
			}
		}

		return max;
	}

	public static double getMinimum( final double[] array ) {
		double min = array[0];

		for ( int i = 1, len = array.length; i < len; i++ ) {
			final double value = array[i];

			if ( value < min ) {
				min = value;
			}
		}

		return min;
	}
}
