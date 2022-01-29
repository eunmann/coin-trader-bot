package coin.trader.algorithm.bollinger;

class IndicatorFlags {
	boolean isBetweenAverageAndUpperBollingerBand = false;
	boolean isAboveMinVolume = false;
	boolean isAboveUpperBollingerBand;

	IndicatorFlags() {

	}

	boolean buySignal() {
		return this.isAboveMinVolume && this.isBetweenAverageAndUpperBollingerBand;
	}

	boolean sellSignal() {
		return this.isAboveUpperBollingerBand;
	}
}
