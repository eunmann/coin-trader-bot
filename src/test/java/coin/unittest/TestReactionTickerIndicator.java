package coin.unittest;

import org.junit.Assert;
import org.junit.Test;

import coin.trader.algorithm.TickerIndicator;
import coin.trader.algorithm.reaction.ReactionTickerIndicator;
import coin.trader.exchange.Currency;
import coin.trader.exchange.CurrencyPair;

public class TestReactionTickerIndicator {
	@Test
	public void testAmazingBuy() {
		final int size = 100;
		final MockTicker mockTicker = this.createMockTicker();
		final TickerIndicator indicator = new ReactionTickerIndicator( 1 );

		for ( int i = 0; i < size; i++ ) {
			mockTicker.last = i * i;
			indicator.addSample( mockTicker );
		}

		Assert.assertEquals( "The ticker did not indicate a buy signal", true, indicator.shouldBuy() );
	}

	@Test
	public void testHorrificSell() {
		final int size = 100;
		final MockTicker mockTicker = this.createMockTicker();
		final TickerIndicator indicator = new ReactionTickerIndicator( 1 );

		for ( int i = 0; i < size; i++ ) {
			mockTicker.last = size - i;
			indicator.addSample( mockTicker );
		}

		Assert.assertEquals( "The ticker did not indicate a sell signal", true, indicator.shouldSell( 100.0, 0.0 ) );
	}

	@Test
	public void testIncreaseThenStableThenDecrease() {
		final int size = 100;
		final MockTicker mockTicker = this.createMockTicker();
		final TickerIndicator indicator = new ReactionTickerIndicator( 1 );

		/* Steady Increase */
		for ( int i = 0; i < size; i++ ) {
			mockTicker.last = i;
			indicator.addSample( mockTicker );
		}

		Assert.assertEquals( "The ticker did not indicate a buy signal", true, indicator.shouldBuy() );

		/* Steady price */
		for ( int i = 0; i < size; i++ ) {
			indicator.addSample( mockTicker );
		}

		Assert.assertEquals( "The ticker indicated a sell signal", false, indicator.shouldSell( 100.0, 0.0 ) );

		/* Steady price */
		final int len = size / 2;
		for ( int i = 0; i < len; i++ ) {
			mockTicker.last = size - i;
			indicator.addSample( mockTicker );
		}

		Assert.assertEquals( "The ticker did not indicate a sell signal", true, indicator.shouldSell( 100.0, 0.0 ) );
	}

	private MockTicker createMockTicker() {
		final MockTicker mockTicker = new MockTicker();
		mockTicker.baseVolume = 1000;
		mockTicker.currencyPair = new CurrencyPair( new Currency( "BTC" ), new Currency( "ETH" ) );
		return mockTicker;
	}
}
