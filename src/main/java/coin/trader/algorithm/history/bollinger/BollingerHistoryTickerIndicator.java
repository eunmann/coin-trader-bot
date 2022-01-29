package coin.trader.algorithm.history.bollinger;

import java.util.List;

import coin.trader.algorithm.history.HistoryTickerIndicator;
import coin.trader.exchange.ChartData.DataPoint;
import coin.trader.exchange.CurrencyPair;
import coin.trader.math.Math;

public class BollingerHistoryTickerIndicator extends HistoryTickerIndicator {
	static final int CANDLE_PERIOD = 300;
	static final int AMOUNT_OF_CANDLES = 20;

	protected BollingerHistoryTickerIndicator( final CurrencyPair currencyPair ) {
		super( currencyPair );
	}

	@Override
	public void anaylize( final List<? extends DataPoint> chartData, final double lastPrice ) {
		synchronized ( this.LOCK ) {
			final double[] samples = new double[chartData.size()];

			int i = 0;
			for ( final DataPoint dataPoint : chartData ) {
				samples[i++] = dataPoint.getClose();
			}

			final double[] bollingerBands = Math.getBollingerBands( samples );
			final double average = Math.average( samples );

			this.buyWeight = lastPrice <= bollingerBands[0] ? 1.0 : 0.0;
			this.sellWeight = lastPrice >= average ? 1.0 : 0.0;
		}
	}
}
