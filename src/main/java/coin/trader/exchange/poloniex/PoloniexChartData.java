package coin.trader.exchange.poloniex;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import coin.trader.exchange.ChartData;

public class PoloniexChartData implements ChartData {
	final List<DataPoint> dataPoints;

	PoloniexChartData( final String jsonStr ) {
		final Type listType = new TypeToken<List<PoloniexDataPoint>>() {
		}.getType();
		final Gson gson = new Gson();
		this.dataPoints = gson.fromJson( jsonStr, listType );
	}

	static class PoloniexDataPoint implements DataPoint {
		long date;
		double high;
		double low;
		double open;
		double close;
		double volume;
		double quoteVolume;
		double weightedAverage;

		@Override
		public double getDate() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getHigh() {
			return this.high;
		}

		@Override
		public double getLow() {
			return this.low;
		}

		@Override
		public double getOpen() {
			return this.open;
		}

		@Override
		public double getClose() {
			return this.close;
		}

		@Override
		public double getVolume() {
			return this.volume;
		}

		@Override
		public double getQuoteVolume() {
			return this.quoteVolume;
		}

		@Override
		public double getWeightedAverage() {
			return this.weightedAverage;
		}
	}

	@Override
	public List<? extends DataPoint> getDataPoints() {
		return this.dataPoints;
	}
}
