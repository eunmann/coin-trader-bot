package coin.unittest.math;

import org.junit.Assert;
import org.junit.Test;

import coin.trader.math.Matrix;
import coin.trader.math.MatrixCPU;
import coin.trader.math.MatrixJCublas;
import coin.trader.utilities.StopWatch;
import coin.trader.utilities.Utils;
import coin.unittest.opencl.TestOpenCL;
import jcuda.jcublas.JCublas;

public class TestMatrix {
	private static final double delta = 0.0001;
	private static final int M = 100;
	private static final int N = 100;
	private static final int K = 200;

	@Test
	public void testMatrixSet() {
		final Matrix A = new MatrixCPU( 2, 2 );

		for ( int i = 0; i < 2; i++ ) {
			for ( int j = 0; j < 2; j++ ) {
				A.set( i, j, 2 * i + j );
			}
		}

		final float val1 = A.get( 0, 0 );
		final float val2 = A.get( 0, 1 );
		final float val3 = A.get( 1, 0 );
		final float val4 = A.get( 1, 1 );

		Assert.assertEquals( "First element", 0.0f, val1, TestMatrix.delta );
		Assert.assertEquals( "First element", 1.0f, val2, TestMatrix.delta );
		Assert.assertEquals( "First element", 2.0f, val3, TestMatrix.delta );
		Assert.assertEquals( "First element", 3.0f, val4, TestMatrix.delta );
	}

	@Test
	public void testMatrixMult_1() {
		final int rows = 10;
		final int cols = 10;
		final Matrix A = new MatrixCPU( rows, cols );
		final Matrix B = new MatrixCPU( rows, cols );

		for ( int i = 0; i < rows; i++ ) {
			for ( int j = 0; j < cols; j++ ) {
				A.set( i, j, 1 );
				B.set( i, j, 1 );
			}
		}

		final Matrix C = A.dot( B );

		for ( int i = 0; i < rows; i++ ) {
			for ( int j = 0; j < cols; j++ ) {
				Assert.assertEquals( "Element was wrong", rows, C.get( i, j ), TestMatrix.delta );
			}
		}
	}

	@Test
	public void testMatrixMult_2() {
		final Matrix A = new MatrixCPU( 2, 3 );
		final Matrix B = new MatrixCPU( 3, 2 );

		/* Set up values of A */
		A.set( 0, 0, 1 );
		A.set( 0, 1, 2 );
		A.set( 0, 2, 3 );
		A.set( 1, 0, 4 );
		A.set( 1, 1, 5 );
		A.set( 1, 2, 6 );

		/* Set up the values for B */
		B.set( 0, 0, 7 );
		B.set( 0, 1, 8 );
		B.set( 1, 0, 9 );
		B.set( 1, 1, 10 );
		B.set( 2, 0, 11 );
		B.set( 2, 1, 12 );

		final Matrix C = A.dot( B );

		/* Assert the values */
		Assert.assertEquals( "Element was wrong", 58, C.get( 0, 0 ), TestMatrix.delta );
		Assert.assertEquals( "Element was wrong", 64, C.get( 0, 1 ), TestMatrix.delta );
		Assert.assertEquals( "Element was wrong", 139, C.get( 1, 0 ), TestMatrix.delta );
		Assert.assertEquals( "Element was wrong", 154, C.get( 1, 1 ), TestMatrix.delta );
	}

	@Test
	public void testMatrixMult_3() {
		final int K = 50;
		final Matrix A = new MatrixCPU( 12, K );
		final Matrix B = new MatrixCPU( K, 13 );

		TestOpenCL.setValuesToOne( A );
		TestOpenCL.setValuesToOne( B );

		final Matrix C = A.dot( B );

		/* Assert the values */
		for ( int i = 0; i < C.getRows(); i++ ) {
			for ( int j = 0; j < C.getCols(); j++ ) {
				Utils.ASSERT( C.get( i, j ) == K, "Value was not correct." );
			}
		}
	}

	@Test
	public void testMatrixTranspose() {
		/* Initialize JCublas */
		JCublas.cublasInit();

		final Matrix A = new MatrixCPU( 2, 3 );

		/* Set up values of A */
		A.set( 0, 0, 1 );
		A.set( 0, 1, 2 );
		A.set( 0, 2, 3 );
		A.set( 1, 0, 4 );
		A.set( 1, 1, 5 );
		A.set( 1, 2, 6 );

		final Matrix B = A.transpose();

		/* Assert the values */
		Assert.assertEquals( "Element was wrong", 1, B.get( 0, 0 ), TestMatrix.delta );
		Assert.assertEquals( "Element was wrong", 2, B.get( 1, 0 ), TestMatrix.delta );
		Assert.assertEquals( "Element was wrong", 3, B.get( 2, 0 ), TestMatrix.delta );
		Assert.assertEquals( "Element was wrong", 4, B.get( 0, 1 ), TestMatrix.delta );
		Assert.assertEquals( "Element was wrong", 5, B.get( 1, 1 ), TestMatrix.delta );
		Assert.assertEquals( "Element was wrong", 6, B.get( 2, 1 ), TestMatrix.delta );

		final Matrix A_2 = new MatrixCPU( 2, 3 );

		/* Set up values of A */
		A_2.set( 0, 0, 1 );
		A_2.set( 0, 1, 2 );
		A_2.set( 0, 2, 3 );
		A_2.set( 1, 0, 4 );
		A_2.set( 1, 1, 5 );
		A_2.set( 1, 2, 6 );

		final Matrix B_2 = A_2.transpose();

		/* Assert the values */
		Assert.assertEquals( "Element was wrong", 1, B_2.get( 0, 0 ), TestMatrix.delta );
		Assert.assertEquals( "Element was wrong", 2, B_2.get( 1, 0 ), TestMatrix.delta );
		Assert.assertEquals( "Element was wrong", 3, B_2.get( 2, 0 ), TestMatrix.delta );
		Assert.assertEquals( "Element was wrong", 4, B_2.get( 0, 1 ), TestMatrix.delta );
		Assert.assertEquals( "Element was wrong", 5, B_2.get( 1, 1 ), TestMatrix.delta );
		Assert.assertEquals( "Element was wrong", 6, B_2.get( 2, 1 ), TestMatrix.delta );
	}

	@Test
	public void testMatrixAddition() throws Exception {
		final Matrix A = new MatrixCPU( 4096, 4096 );
		final Matrix B = new MatrixCPU( 4096, 4096 );

		TestOpenCL.setValuesToOne( A );
		TestOpenCL.setValuesToOne( B );

		final Matrix C = A.add( B );

		/* Assert the values */
		for ( int i = 0; i < C.getRows(); i++ ) {
			for ( int j = 0; j < C.getCols(); j++ ) {
				Utils.ASSERT( C.get( i, j ) == 2, "Value was not correct" );
			}
		}
	}

	@Test
	public void testMatrixMultiplication() throws Exception {
		/* Initialize JCublas */
		JCublas.cublasInit();

		final double GFLOP = (2.0 * TestMatrix.M * TestMatrix.K * TestMatrix.N) / 1e9;
		final StopWatch sw = new StopWatch( StopWatch.TimeUnit.MILLISECONDS );

		/* CPU */
		final MatrixCPU A = new MatrixCPU( TestMatrix.M, TestMatrix.K );
		final MatrixCPU B = new MatrixCPU( TestMatrix.K, TestMatrix.N );
		TestOpenCL.setValuesToOne( A );
		TestOpenCL.setValuesToOne( B );

		sw.start();
		final Matrix C = A.dot( B );
		final long cpuTime = sw.end();

		/* GPU */
		final MatrixJCublas D = new MatrixJCublas( TestMatrix.M, TestMatrix.K );
		final MatrixJCublas E = new MatrixJCublas( TestMatrix.K, TestMatrix.N );
		TestOpenCL.setValuesToOne( D );
		TestOpenCL.setValuesToOne( E );

		sw.start();
		final Matrix F = D.dot( E );
		final long gpuTime = sw.end();

		System.out.println( "\nMatrix Multiplication" );
		System.out.println( "[ M, K, N ]: [ " + TestMatrix.M + ", " + TestMatrix.K + ", " + TestMatrix.N + " ]" );
		System.out.println( "CPU Time (ms): " + cpuTime );
		System.out.println( "CPU GFLOPS: " + GFLOP / (cpuTime / 1000.0) );
		System.out.println( "GPU Time (ms): " + gpuTime );
		System.out.println( "GPU GFLOPS: " + GFLOP / (gpuTime / 1000.0) );

		if ( !C.equals( F ) ) {
			throw new Exception( "Matrices were not equal" );
		}
	}

	@Test
	public void testMatrixMult_2_JCUBLAS() {
		final Matrix A = new MatrixJCublas( 2, 3 );
		final Matrix B = new MatrixJCublas( 3, 2 );

		/* Set up values of A */
		A.set( 0, 0, 1 );
		A.set( 0, 1, 2 );
		A.set( 0, 2, 3 );
		A.set( 1, 0, 4 );
		A.set( 1, 1, 5 );
		A.set( 1, 2, 6 );

		/* Set up the values for B */
		B.set( 0, 0, 7 );
		B.set( 0, 1, 8 );
		B.set( 1, 0, 9 );
		B.set( 1, 1, 10 );
		B.set( 2, 0, 11 );
		B.set( 2, 1, 12 );

		final Matrix C = A.dot( B );

		/* Assert the values */
		Assert.assertEquals( "Element was wrong", 58, C.get( 0, 0 ), TestMatrix.delta );
		Assert.assertEquals( "Element was wrong", 64, C.get( 0, 1 ), TestMatrix.delta );
		Assert.assertEquals( "Element was wrong", 139, C.get( 1, 0 ), TestMatrix.delta );
		Assert.assertEquals( "Element was wrong", 154, C.get( 1, 1 ), TestMatrix.delta );
	}

	@Test
	public void testMatrixTransposePerformance() throws Exception {
		/* Initialize JCublas */
		JCublas.cublasInit();

		final double GFLOP = (TestMatrix.M * TestMatrix.N) / 1e9;
		final StopWatch sw = new StopWatch( StopWatch.TimeUnit.MILLISECONDS );

		/* CPU */
		final MatrixCPU A = new MatrixCPU( TestMatrix.M, TestMatrix.N );
		TestOpenCL.setValuesToOne( A );

		sw.start();
		final Matrix B = A.transpose();
		final long cpuTime = sw.end();

		/* GPU */
		final MatrixJCublas C = new MatrixJCublas( TestMatrix.M, TestMatrix.N );
		TestOpenCL.setValuesToOne( C );

		sw.start();
		final Matrix D = C.transpose();
		final long gpuTime = sw.end();

		System.out.println( "\nMatrix Multiplication" );
		System.out.println( "[ M, K, N ]: [ " + TestMatrix.M + ", " + TestMatrix.K + ", " + TestMatrix.N + " ]" );
		System.out.println( "CPU Time (ms): " + cpuTime );
		System.out.println( "CPU GFLOPS: " + GFLOP / (cpuTime / 1000.0) );
		System.out.println( "GPU Time (ms): " + gpuTime );
		System.out.println( "GPU GFLOPS: " + GFLOP / (gpuTime / 1000.0) );

		if ( !B.equals( D ) ) {
			throw new Exception( "Matrices were not equal" );
		}
	}
}
