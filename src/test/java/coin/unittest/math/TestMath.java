package coin.unittest.math;

import org.junit.Assert;
import org.junit.Test;

import coin.trader.math.Math;

public class TestMath {
	private static final double delta = 0.0001;

	@Test
	public void testStandardDeviation_1() {
		final double[] array = { 2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0 };
		final double expected = 2.0;
		Assert.assertEquals( "The standard deviation was not correct", expected, Math.standardDeviation( array ), TestMath.delta );
	}

	@Test
	public void testAverage_1() {
		final int SIZE = 1000;
		final double[] array = new double[SIZE];

		for ( int i = 0; i < SIZE; i++ ) {
			array[i] = i + 1;
		}

		final double expected = ((SIZE * (SIZE + 1)) / 2.0) / SIZE;

		Assert.assertEquals( "The average was not correct", expected, Math.average( array ), TestMath.delta );
	}

}
