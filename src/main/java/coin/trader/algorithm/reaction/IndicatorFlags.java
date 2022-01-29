package coin.trader.algorithm.reaction;

class IndicatorFlags {
	boolean postiveLinearRegression = false;
	boolean overMinCandidacyWeight = false;
	boolean aboveMinVolume = false;

	IndicatorFlags() {
	}

	public boolean buySignal() {
		return this.postiveLinearRegression && this.overMinCandidacyWeight && this.aboveMinVolume;
	}

	public boolean sellSignal() {
		return !this.postiveLinearRegression;
	}

	@Override
	public String toString() {
		return "[ " + this.postiveLinearRegression + ". " + this.overMinCandidacyWeight + " ]";
	}
}
