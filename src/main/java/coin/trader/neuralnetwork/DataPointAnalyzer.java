package coin.trader.neuralnetwork;

import java.util.List;

import coin.trader.exchange.ChartData.DataPoint;
import coin.trader.math.Matrix;
import coin.trader.utilities.Utils;

public class DataPointAnalyzer {
	private static final int PARAMS_PER_DATA_POINT = 10;
	private final NeuralNetwork neuralNetwork;
	private final int numOfDataPoints;
	private final int numOfInputs;
	private final int numOfOutputs;

	public DataPointAnalyzer( final int numOfDataPoints ) {
		this.numOfInputs = numOfDataPoints * DataPointAnalyzer.PARAMS_PER_DATA_POINT;
		this.numOfOutputs = 2;
		final int numberOfHiddenNodes = (this.numOfInputs + this.numOfOutputs) / 2;
		this.neuralNetwork = new NeuralNetwork( this.numOfInputs, numberOfHiddenNodes, this.numOfOutputs, 2, NeuralNetwork.DEFAULT_LEARNING_RATE );
		this.numOfDataPoints = numOfDataPoints;
	}

	public void train( final List<DataPoint> data, final boolean shouldBuy ) {
		Utils.ASSERT( data.size() == this.numOfDataPoints, "The amount of data points is not equal to the expected amount." );

		final Matrix input = this.candlesToMatrix( data );

		final Matrix outputTarget = Matrix.create( 2, 1 );

		if ( shouldBuy ) {
			outputTarget.set( 0, 0, NeuralNetwork.MAX_VALUE );
			outputTarget.set( 1, 0, NeuralNetwork.MIN_VALUE );
		}
		else {
			outputTarget.set( 0, 0, NeuralNetwork.MIN_VALUE );
			outputTarget.set( 1, 0, NeuralNetwork.MAX_VALUE );
		}

		this.neuralNetwork.train( input, outputTarget );
	}

	public boolean query( final List<DataPoint> data ) {
		Utils.ASSERT( data.size() == this.numOfDataPoints, "The amount of data points is not equal to the expected amount." );

		final Matrix input = this.candlesToMatrix( data );

		final Matrix output = this.neuralNetwork.query( input );

		return output.get( 0, 0 ) > output.get( 1, 0 );
	}

	private Matrix candlesToMatrix( final List<DataPoint> data ) {
		Utils.ASSERT( data.size() == this.numOfDataPoints, "The amount of data points is not equal to the expected amount." );

		final Matrix input = Matrix.create( this.numOfInputs, 1 );

		float high = (float) data.get( 0 ).getHigh();
		float totalVolume = 0.0f;

		for ( int i = 0; i < this.numOfDataPoints; i++ ) {
			final DataPoint dataPoint = data.get( i );
			if ( dataPoint.getHigh() > high ) {
				high = (float) dataPoint.getHigh();
			}
			totalVolume += dataPoint.getVolume();
		}

		for ( int i = 0; i < this.numOfDataPoints; i++ ) {
			final int index = i * DataPointAnalyzer.PARAMS_PER_DATA_POINT;
			final DataPoint dataPoint = data.get( i );
			input.set( index, 0, DataPointAnalyzer.normalizeValue( dataPoint.getHigh(), high ) );
			input.set( index + 1, 0, DataPointAnalyzer.normalizeValue( dataPoint.getLow(), high ) );
			input.set( index + 2, 0, DataPointAnalyzer.normalizeValue( dataPoint.getOpen(), high ) );
			input.set( index + 3, 0, DataPointAnalyzer.normalizeValue( dataPoint.getClose(), high ) );
			input.set( index + 4, 0, DataPointAnalyzer.normalizeValue( dataPoint.getVolume(), totalVolume ) );
			input.set( index + 5, 0, DataPointAnalyzer.normalizeValue( dataPoint.getHigh() - dataPoint.getOpen(), high ) );
			input.set( index + 6, 0, DataPointAnalyzer.normalizeValue( dataPoint.getHigh() - dataPoint.getClose(), high ) );
			input.set( index + 7, 0, DataPointAnalyzer.normalizeValue( dataPoint.getHigh() - dataPoint.getLow(), high ) );
			input.set( index + 8, 0, DataPointAnalyzer.normalizeValue( dataPoint.getOpen() - dataPoint.getLow(), high ) );
			input.set( index + 9, 0, DataPointAnalyzer.normalizeValue( dataPoint.getClose() - dataPoint.getLow(), high ) );
		}

		return input;
	}

	private static float normalizeValue( final double val, final float denom ) {
		return (float) ((val / denom) * 0.98 + 0.01);
	}
}
