package coin.unittest.opencl;

import org.junit.Test;

import coin.trader.math.Matrix;
import coin.trader.math.MatrixCPU;
import coin.trader.math.MatrixOpenCL;
import coin.trader.utilities.StopWatch;

public class TestOpenCL {
	private static final int M = 128;
	private static final int N = 128;
	private static final int K = 80;

	@Test
	public void testMatrixMultiplication() throws Exception {
		final double GFLOP = (2.0 * TestOpenCL.M * TestOpenCL.K * TestOpenCL.N) / 1e9;
		final StopWatch sw = new StopWatch( StopWatch.TimeUnit.MILLISECONDS );

		/* CPU */
		final MatrixCPU A = new MatrixCPU( TestOpenCL.M, TestOpenCL.K );
		final MatrixCPU B = new MatrixCPU( TestOpenCL.K, TestOpenCL.N );
		TestOpenCL.setValuesToOne( A );
		TestOpenCL.setValuesToOne( B );

		sw.start();
		final Matrix C = A.dot( B );
		A.free();
		B.free();
		C.free();
		final long cpuTime = sw.end();

		/* GPU */
		final MatrixOpenCL D = new MatrixOpenCL( TestOpenCL.M, TestOpenCL.K );
		final MatrixOpenCL E = new MatrixOpenCL( TestOpenCL.K, TestOpenCL.N );
		TestOpenCL.setValuesToOne( D );
		TestOpenCL.setValuesToOne( E );

		sw.start();
		final MatrixOpenCL F = (MatrixOpenCL) D.dot( E );
		D.free();
		E.free();
		F.free();
		final long gpuTime = sw.end();

		System.out.println( "\nMatrix Multiplication" );
		System.out.println( "[ M, K, N ]: [ " + TestOpenCL.M + ", " + TestOpenCL.K + ", " + TestOpenCL.N + " ]" );
		System.out.println( "CPU Time (ms): " + cpuTime );
		System.out.println( "CPU GFLOPS: " + GFLOP / (cpuTime / 1000.0) );
		System.out.println( "GPU Time (ms): " + gpuTime );
		System.out.println( "GPU GFLOPS: " + GFLOP / (gpuTime / 1000.0) );

		if ( !C.equals( F ) ) {
			for ( int i = 0; i < F.getPaddedRows(); i++ ) {
				for ( int j = 0; j < F.getPaddedCols(); j++ ) {
					final float val = F.get( i, j );

					if ( val != 0 ) {
						System.out.println( "[ X, Y, Val ]: [ " + i + ", " + j + ", " + val + " ]" );
					}
				}
			}
			throw new Exception( "Matrices were not equal" );
		}
	}

	@Test
	public void testMatrixMultiplication_2() throws Exception {
		final int iterations = 50;
		final double GFLOP = (2.0 * TestOpenCL.M * TestOpenCL.K * TestOpenCL.N) / 1e9;
		final StopWatch sw = new StopWatch( StopWatch.TimeUnit.MILLISECONDS );

		/* CPU */
		final MatrixCPU A = new MatrixCPU( TestOpenCL.M, TestOpenCL.K );
		final MatrixCPU B = new MatrixCPU( TestOpenCL.K, TestOpenCL.N );
		TestOpenCL.setValuesToOne( A );
		TestOpenCL.setValuesToOne( B );

		sw.start();

		for ( int i = 0; i < iterations; i++ ) {
			A.dot( B ).free();
		}

		A.free();
		B.free();
		final long cpuTime = sw.end();

		/* GPU */
		final MatrixOpenCL D = new MatrixOpenCL( TestOpenCL.M, TestOpenCL.K );
		final MatrixOpenCL E = new MatrixOpenCL( TestOpenCL.K, TestOpenCL.N );
		TestOpenCL.setValuesToOne( D );
		TestOpenCL.setValuesToOne( E );

		sw.start();
		for ( int i = 0; i < iterations; i++ ) {
			D.dot( E ).free();
		}
		D.free();
		E.free();
		final long gpuTime = sw.end();

		System.out.println( "\nMatrix Multiplication 2" );
		System.out.println( "[ M, K, N ]: [ " + TestOpenCL.M + ", " + TestOpenCL.K + ", " + TestOpenCL.N + " ]" );
		System.out.println( "CPU Time (ms): " + cpuTime );
		System.out.println( "CPU GFLOPS: " + GFLOP / (cpuTime / 1000.0) );
		System.out.println( "GPU Time (ms): " + gpuTime );
		System.out.println( "GPU GFLOPS: " + GFLOP / (gpuTime / 1000.0) );
	}

	@Test
	public void testMatrixMultiply() throws Exception {
		final double GFLOP = (TestOpenCL.M * TestOpenCL.N) / 1e9;
		final StopWatch sw = new StopWatch( StopWatch.TimeUnit.MILLISECONDS );

		final MatrixCPU A = new MatrixCPU( TestOpenCL.M, TestOpenCL.N );
		final MatrixCPU B = new MatrixCPU( TestOpenCL.M, TestOpenCL.N );
		TestOpenCL.setValues( A );
		TestOpenCL.setValues( B );

		sw.start();
		final Matrix C = A.multiply( B );
		final long cpuTime = sw.end();

		final MatrixOpenCL D = new MatrixOpenCL( TestOpenCL.M, TestOpenCL.N );
		final MatrixOpenCL E = new MatrixOpenCL( TestOpenCL.M, TestOpenCL.N );
		TestOpenCL.setValues( D );
		TestOpenCL.setValues( E );

		sw.start();
		final Matrix F = D.multiply( E );
		final long gpuTime = sw.end();

		System.out.println( "\nMatrix Multiply" );
		System.out.println( "[ M, K, N ]: [ " + TestOpenCL.M + ", " + TestOpenCL.K + ", " + TestOpenCL.N + " ]" );
		System.out.println( "CPU Time (ms): " + cpuTime );
		System.out.println( "CPU GFLOPS: " + GFLOP / (cpuTime / 1000.0) );
		System.out.println( "GPU Time (ms): " + gpuTime );
		System.out.println( "GPU GFLOPS: " + GFLOP / (gpuTime / 1000.0) );

		if ( !C.equals( F ) ) {
			throw new Exception( "Matrices were not equal" );
		}
	}

	@Test
	public void testMatrixSubtraction() throws Exception {
		final double GFLOP = (TestOpenCL.M * TestOpenCL.N) / 1e9;
		final StopWatch sw = new StopWatch( StopWatch.TimeUnit.MILLISECONDS );

		final MatrixCPU A = new MatrixCPU( TestOpenCL.M, TestOpenCL.N );
		final MatrixCPU B = new MatrixCPU( TestOpenCL.M, TestOpenCL.N );
		TestOpenCL.setValues( A );
		TestOpenCL.setValues( B );

		sw.start();
		final Matrix C = A.subtract( B );
		final long cpuTime = sw.end();

		final MatrixOpenCL D = new MatrixOpenCL( TestOpenCL.M, TestOpenCL.N );
		final MatrixOpenCL E = new MatrixOpenCL( TestOpenCL.M, TestOpenCL.N );
		TestOpenCL.setValues( D );
		TestOpenCL.setValues( E );

		sw.start();
		final Matrix F = D.subtract( E );
		final long gpuTime = sw.end();

		System.out.println( "\nMatrix Subtractoin" );
		System.out.println( "[ M, K, N ]: [ " + TestOpenCL.M + ", " + TestOpenCL.K + ", " + TestOpenCL.N + " ]" );
		System.out.println( "CPU Time (ms): " + cpuTime );
		System.out.println( "CPU GFLOPS: " + GFLOP / (cpuTime / 1000.0) );
		System.out.println( "GPU Time (ms): " + gpuTime );
		System.out.println( "GPU GFLOPS: " + GFLOP / (gpuTime / 1000.0) );

		if ( !C.equals( F ) ) {
			throw new Exception( "Matrices were not equal" );
		}
	}

	@Test
	public void testMatrixAddition() throws Exception {
		final double GFLOP = (TestOpenCL.M * TestOpenCL.N) / 1e9;
		final StopWatch sw = new StopWatch( StopWatch.TimeUnit.MILLISECONDS );

		final MatrixCPU A = new MatrixCPU( TestOpenCL.M, TestOpenCL.N );
		final MatrixCPU B = new MatrixCPU( TestOpenCL.M, TestOpenCL.N );
		TestOpenCL.setValues( A );
		TestOpenCL.setValues( B );

		sw.start();
		final Matrix C = A.subtract( B );
		final long cpuTime = sw.end();

		final MatrixOpenCL D = new MatrixOpenCL( TestOpenCL.M, TestOpenCL.N );
		final MatrixOpenCL E = new MatrixOpenCL( TestOpenCL.M, TestOpenCL.N );
		TestOpenCL.setValues( D );
		TestOpenCL.setValues( E );

		sw.start();
		final Matrix F = D.subtract( E );
		final long gpuTime = sw.end();

		System.out.println( "\nMatrix Addition" );
		System.out.println( "[ M, K, N ]: [ " + TestOpenCL.M + ", " + TestOpenCL.K + ", " + TestOpenCL.N + " ]" );
		System.out.println( "CPU Time (ms): " + cpuTime );
		System.out.println( "CPU GFLOPS: " + GFLOP / (cpuTime / 1000.0) );
		System.out.println( "GPU Time (ms): " + gpuTime );
		System.out.println( "GPU GFLOPS: " + GFLOP / (gpuTime / 1000.0) );

		if ( !C.equals( F ) ) {
			throw new Exception( "Matrices were not equal" );
		}
	}

	@Test
	public void testMatrixTranspose() throws Exception {
		final double GFLOP = (TestOpenCL.M * TestOpenCL.N) / 1e9;
		final StopWatch sw = new StopWatch( StopWatch.TimeUnit.MILLISECONDS );

		final MatrixCPU A = new MatrixCPU( TestOpenCL.M, TestOpenCL.N );
		TestOpenCL.setValues( A );

		sw.start();
		final Matrix C = A.transpose();
		final long cpuTime = sw.end();

		final MatrixOpenCL D = new MatrixOpenCL( TestOpenCL.M, TestOpenCL.N );
		TestOpenCL.setValues( D );

		sw.start();
		final MatrixOpenCL F = (MatrixOpenCL) D.transpose();
		final long gpuTime = sw.end();

		System.out.println( "\nMatrix Transpose" );
		System.out.println( "[ M, K, N ]: [ " + TestOpenCL.M + ", " + TestOpenCL.K + ", " + TestOpenCL.N + " ]" );
		System.out.println( "CPU Time (ms): " + cpuTime );
		System.out.println( "CPU GFLOPS: " + GFLOP / (cpuTime / 1000.0) );
		System.out.println( "GPU Time (ms): " + gpuTime );
		System.out.println( "GPU GFLOPS: " + GFLOP / (gpuTime / 1000.0) );

		if ( !C.equals( F ) ) {
			for ( int i = 0; i < F.getRows(); i++ ) {
				for ( int j = 0; j < F.getCols(); j++ ) {
					final float val_GPU = F.get( i, j );
					final float val_CPU = C.get( i, j );
					if ( val_GPU != val_CPU ) {
						System.out.println( "[ X, Y, Val_CPU, Val_GPU ]: [ " + i + ", " + j + ", " + val_CPU + ", " + val_GPU + " ]" );
					}
				}
			}
			throw new Exception( "Matrices were not equal" );
		}
	}

	public static void setValues( final Matrix A ) {
		for ( int i = 0; i < A.getRows(); i++ ) {
			for ( int j = 0; j < A.getCols(); j++ ) {
				A.set( i, j, A.getCols() * i + j );
			}
		}
	}

	public static void setValuesToOne( final Matrix A ) {
		for ( int i = 0; i < A.getRows(); i++ ) {
			for ( int j = 0; j < A.getCols(); j++ ) {
				A.set( i, j, 1 );
			}
		}
	}
}
