package coin.trader.neuralnetwork;

import coin.trader.math.Matrix;

/* TODO(EMU): Optimize this */
public class NeuralNetwork {
	public static float MIN_VALUE = 0.001f;
	public static float MAX_VALUE = 0.999f;
	public static float DEFAULT_LEARNING_RATE = 0.01f;
	final int inputNodes;
	final int hiddenNodes;
	final int outputNodes;
	final float learningRate;
	final int numOfHiddenLayers;

	final Matrix[] layers;

	public NeuralNetwork( final int inputNodes, final int hiddenNodes, final int outputNodes, final int numOfHiddenLayers, final float learningRate ) {
		this.inputNodes = inputNodes;
		this.hiddenNodes = hiddenNodes;
		this.outputNodes = outputNodes;
		this.learningRate = learningRate;
		this.numOfHiddenLayers = numOfHiddenLayers;

		this.layers = new Matrix[1 + numOfHiddenLayers];

		/* Input to first hidden layer */
		this.layers[0] = Matrix.create( this.hiddenNodes, this.inputNodes );

		/* Last hidden to the output layer */
		this.layers[this.layers.length - 1] = Matrix.create( this.outputNodes, this.hiddenNodes );

		/* Add additional hidden layers */
		for ( int i = 1; i < numOfHiddenLayers; i++ ) {
			this.layers[i] = Matrix.create( this.hiddenNodes, this.hiddenNodes );
		}

		/*
		 * Init the values of the layers, the values are chosen from the range
		 * of +,- 1/sqrt(rows)
		 */
		for ( int i = 0; i < this.layers.length; i++ ) {
			final Matrix A = this.layers[i];
			final double v = (float) Math.pow( A.getRows(), -0.5 );

			for ( int r = 0; r < A.getRows(); r++ ) {
				for ( int c = 0; c < A.getCols(); c++ ) {
					final double randomValue = 2 * (Math.random() - 0.5);
					A.set( r, c, (float) (randomValue * v) );
				}
			}
		}
	}

	public Matrix query( final Matrix input ) {
		Matrix A = input;

		/*
		 * Loop through all of the layers in the network. This is simply just
		 * matrix multiplication, then applying the sigmoid function to each
		 * element in the resulting matrix. The resulting matrix is passed to
		 * the next layer, until we have gone through all of the layers. The
		 * final result is the output of the neural network.
		 */
		for ( int i = 0, len = this.layers.length; i < len; i++ ) {
			A = this.layers[i].dot( A ).applySigmoid();
		}

		return A;
	}

	public void train( final Matrix input, final Matrix outputTarget ) {
		/*
		 * Each layers input and output needs to be recorded for the back
		 * propagation of the error later on
		 */
		final Matrix[] layerInputs = new Matrix[this.layers.length];
		final Matrix[] layerOutputs = new Matrix[this.layers.length];

		/* Query the network */
		Matrix A = input;
		for ( int i = 0, len = this.layers.length; i < len; i++ ) {
			layerInputs[i] = A;
			final Matrix T = this.layers[i].dot( A );
			A = T.applySigmoid();
			layerOutputs[i] = A;

			T.free();
		}

		/* Calculate error from target */
		final Matrix outputError = outputTarget.subtract( layerOutputs[layerOutputs.length - 1] );

		final Matrix[] layerErrors = new Matrix[this.layers.length];
		layerErrors[layerErrors.length - 1] = outputError;

		/* Calculate the error for each hidden layer */
		Matrix B = outputError;
		for ( int i = layerErrors.length - 2; i >= 0; i-- ) {
			final Matrix T = this.layers[i + 1].transpose();
			B = T.dot( B );
			T.free();
			layerErrors[i] = B;
		}

		/*
		 * Calculate error for each layer and update the weights of the matrices
		 */
		for ( int i = layerErrors.length - 1; i >= 0; i-- ) {
			final Matrix layerInput = layerInputs[i];
			final Matrix layerOutput = layerOutputs[i];
			final Matrix layerError = layerErrors[i];

			final Matrix temp1 = layerOutput.constantByElementSubtraction( 1 );
			final Matrix temp2 = layerError.multiply( layerOutput );
			final Matrix temp3 = temp1.multiply( temp2 );
			final Matrix temp4 = layerInput.transpose();
			final Matrix temp5 = temp3.dot( temp4 );
			final Matrix temp6 = temp5.multiply( this.learningRate );

			this.setLayer( i, this.layers[i].add( temp6 ) );

			/* Free Resources */
			temp1.free();
			temp2.free();
			temp3.free();
			temp4.free();
			temp5.free();
			temp6.free();
		}

		/* Free Resources */
		for ( int i = 0, len = this.layers.length; i < len; i++ ) {
			layerOutputs[i].free();
			layerErrors[i].free();
		}

		/*
		 * The first element in layerInputs is the input to this function, we do
		 * not want to call free on that matrix
		 */
		for ( int i = 1, len = this.layers.length; i < len; i++ ) {
			layerInputs[i].free();
		}

	}

	private void setLayer( final int i, final Matrix A ) {
		this.layers[i].free();
		this.layers[i] = A;
	}

	public void free() {
		for ( final Matrix A : this.layers ) {
			A.free();
		}
	}
}
