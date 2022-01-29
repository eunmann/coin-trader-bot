package coin.unittest.neuralnetwork;

import org.junit.Test;

import coin.trader.math.Matrix;
import coin.trader.neuralnetwork.NeuralNetwork;
import coin.trader.utilities.StopWatch;

public class TestNeuralNetwork {
	@Test
	public void testNeuralNetwork() throws Exception {
		final NeuralNetwork network = new NeuralNetwork( 2, 3, 2, 1, 0.01f );

		final Matrix input = Matrix.create( 2, 1 );

		final Matrix output = network.query( input );
	}

	@Test
	public void testNeuralNetworkTraining() throws Exception {
		Matrix.setMatrixCalculationImplementation( Matrix.MatrixImplemention.GPU_JCUBLAS );

		final NeuralNetwork network = new NeuralNetwork( 200, 1024, 2, 3, 0.01f );

		final Matrix input = Matrix.create( 200, 1 );
		TestNeuralNetwork.set( input, NeuralNetwork.MIN_VALUE );
		input.set( 0, 0, NeuralNetwork.MAX_VALUE );

		final Matrix target = Matrix.create( 2, 1 );
		TestNeuralNetwork.set( target, NeuralNetwork.MIN_VALUE );
		target.set( 0, 0, NeuralNetwork.MAX_VALUE );

		final StopWatch sw = new StopWatch( StopWatch.TimeUnit.MILLISECONDS );

		sw.start();
		for ( int i = 0; i < 100; i++ ) {
			network.train( input, target );
		}
		final long timeElapsed = sw.end();

		final Matrix output = network.query( input );

		System.out.println( "Time (ms): " + timeElapsed );
	}

	public static void set( final Matrix A, final float val ) {
		for ( int i = 0; i < A.getRows(); i++ ) {
			for ( int j = 0; j < A.getCols(); j++ ) {
				A.set( i, j, val );
			}
		}
	}
}
