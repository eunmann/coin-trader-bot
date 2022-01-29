package coin.trader.algorithm.history;

import java.util.List;

import coin.trader.exchange.ChartData;
import coin.trader.exchange.ChartData.DataPoint;
import coin.trader.exchange.CurrencyPair;

public abstract class HistoryTickerIndicator {
	protected final CurrencyPair currencyPair;
	protected ChartData chartData;
	protected final Object LOCK = new Object();
	protected double sellWeight;
	protected double buyWeight;

	protected HistoryTickerIndicator( final CurrencyPair currencyPair ) {
		this.currencyPair = currencyPair;
	}

	public CurrencyPair getCurrencyPair() {
		return this.currencyPair;
	}

	public ChartData getLatestChartData() {
		synchronized ( this.LOCK ) {
			return this.chartData;
		}
	}

	public double shouldBuy() {
		synchronized ( this.LOCK ) {
			return this.buyWeight;
		}
	}

	public double shouldSell() {
		synchronized ( this.LOCK ) {
			return this.sellWeight;
		}
	}

	public abstract void anaylize( final List<? extends DataPoint> chartData, final double lastPrice );
}
