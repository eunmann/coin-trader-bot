package coin.unittest;

import org.junit.Assert;
import org.junit.Test;

import coin.trader.math.SampleHistory;

public class TestSimpleMovingAverage {
	private static final double delta = 0.0001;

	@Test
	public void testAverage_1() {
		/* Create SMA of size 5 */
		final int SIZE = 5;
		final SampleHistory sma = new SampleHistory( SIZE );

		for ( int i = 0; i < SIZE; i++ ) {
			sma.addSample( i );
		}

		final double expected = (0.0 + 1.0 + 2.0 + 3.0 + 4.0) / SIZE;

		Assert.assertEquals( "The average was incorrect", expected, sma.getAverage(), TestSimpleMovingAverage.delta );
	}

	@Test
	public void testAverage_2() {
		/* Create SMA of size 5 */
		final int SIZE = 5;
		final SampleHistory sma = new SampleHistory( SIZE );

		for ( int i = 0; i < 100; i++ ) {
			sma.addSample( i );
		}

		final double expected = (99.0 + 98.0 + 97.0 + 96.0 + 95.0) / SIZE;

		Assert.assertEquals( "The average was incorrect", expected, sma.getAverage(), TestSimpleMovingAverage.delta );
	}

	@Test
	public void testNewestSample_1() {
		/* Create SMA of size 5 */
		final int SIZE = 5;
		final SampleHistory sma = new SampleHistory( SIZE );

		for ( int i = 0; i < SIZE; i++ ) {
			sma.addSample( i );
		}

		final double expected = 4.0;

		Assert.assertEquals( "The newest was incorrect", expected, sma.getNewestSample(), TestSimpleMovingAverage.delta );
	}

	@Test
	public void testNewestSample_2() {
		/* Create SMA of size 5 */
		final int SIZE = 5;
		final SampleHistory sma = new SampleHistory( SIZE );

		for ( int i = 0; i < 100; i++ ) {
			sma.addSample( i );
		}

		final double expected = 99.0;

		Assert.assertEquals( "The newest was incorrect", expected, sma.getNewestSample(), TestSimpleMovingAverage.delta );
	}

	@Test
	public void testOldestSample_1() {
		/* Create SMA of size 5 */
		final int SIZE = 5;
		final SampleHistory sma = new SampleHistory( SIZE );

		for ( int i = 0; i < SIZE; i++ ) {
			sma.addSample( i );
		}

		final double expected = 0.0;

		Assert.assertEquals( "The oldest was incorrect", expected, sma.getOldestSample(), TestSimpleMovingAverage.delta );
	}

	@Test
	public void testOldestSample_2() {
		/* Create SMA of size 5 */
		final int SIZE = 5;
		final SampleHistory sma = new SampleHistory( SIZE );

		for ( int i = 0; i < 100; i++ ) {
			sma.addSample( i );
		}

		final double expected = 95.0;

		Assert.assertEquals( "The oldest was incorrect", expected, sma.getOldestSample(), TestSimpleMovingAverage.delta );
	}
}
