package coin.trader.math;

import java.util.concurrent.CountDownLatch;

import coin.trader.concurrency.ResourceManager;
import coin.trader.utilities.Utils;

public class MatrixCPU extends Matrix {
	public MatrixCPU( final int rows, final int cols ) {
		super( rows, cols );
		this.matrix = new float[rows * cols];
	}

	MatrixCPU( final Matrix A ) {
		super( A.rows, A.cols );
		this.matrix = A.matrix;
	}

	@Override
	public void set( final int row, final int col, final float val ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();

		this.matrix[this.cols * row + col] = val;
	}

	@Override
	public float get( final int row, final int col ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();

		return this.matrix[this.cols * row + col];
	}

	@Override
	public Matrix dot( final Matrix B ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();
		B.assertResources();

		/* Check Matrices */
		Utils.ASSERT( this.cols == B.rows, "The amount of columns is not equal to the number of rows of the input matrix." );
		Utils.ASSERT( B instanceof MatrixCPU, "The input matrix is not of the correct type." );

		final MatrixCPU C = new MatrixCPU( this.rows, B.cols );

		/* Set up the concurrency objects */
		final Runnable[] workers = new Runnable[ResourceManager.OPTIMAL_NUMBER_OF_THREADS];
		final CountDownLatch latch = new CountDownLatch( ResourceManager.OPTIMAL_NUMBER_OF_THREADS );

		/* Threads do work across rows ( iterate through columns ) */
		final int remainder = this.rows % ResourceManager.OPTIMAL_NUMBER_OF_THREADS;
		final int rowsPerThread = this.rows / ResourceManager.OPTIMAL_NUMBER_OF_THREADS;

		for ( int i = 0; i < ResourceManager.OPTIMAL_NUMBER_OF_THREADS; i++ ) {
			/* Last thread handles remainder */
			final int rowStartIndex = rowsPerThread * i;
			final int rowEndIndex = (i + 1) != ResourceManager.OPTIMAL_NUMBER_OF_THREADS ? rowsPerThread * (i + 1) : rowsPerThread * (i + 1) + remainder;

			workers[i] = new Runnable() {
				@Override
				public void run() {
					try {
						for ( int i = rowStartIndex; i < rowEndIndex; i++ ) {
							final int in = i * MatrixCPU.this.cols;

							for ( int j = 0; j < B.cols; j++ ) {
								float sum = 0;

								for ( int k = 0; k < B.rows; k++ ) {
									final int iB = k * B.cols + j;
									final int iA = in + k;
									sum += MatrixCPU.this.matrix[iA] * B.matrix[iB];
								}

								C.matrix[i * B.cols + j] = sum;
							}
						}
					}
					catch ( final Throwable t ) {

					}
					finally {
						latch.countDown();
					}
				}
			};
		}

		for ( int i = 0; i < workers.length; i++ ) {
			ResourceManager.sumbitHeavyRunnable( workers[i] );
		}

		try {
			latch.await();
		}
		catch ( final InterruptedException e ) {
			throw new RuntimeException( e );
		}

		return C;
	}

	@Override
	public Matrix transpose() {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();

		final MatrixCPU B = new MatrixCPU( this.cols, this.rows );

		/* Set up the concurrency objects */
		final Runnable[] workers = new Runnable[ResourceManager.OPTIMAL_NUMBER_OF_THREADS];
		final CountDownLatch latch = new CountDownLatch( ResourceManager.OPTIMAL_NUMBER_OF_THREADS );

		/* Threads do work across rows ( iterate through columns ) */
		final int remainder = this.rows % ResourceManager.OPTIMAL_NUMBER_OF_THREADS;
		final int rowsPerThread = this.rows / ResourceManager.OPTIMAL_NUMBER_OF_THREADS;

		for ( int i = 0; i < ResourceManager.OPTIMAL_NUMBER_OF_THREADS; i++ ) {
			/* Last thread handles remainder */
			final int rowStartIndex = rowsPerThread * i;
			final int rowEndIndex = (i + 1) != ResourceManager.OPTIMAL_NUMBER_OF_THREADS ? rowsPerThread * (i + 1) : rowsPerThread * (i + 1) + remainder;

			workers[i] = new Runnable() {
				@Override
				public void run() {
					try {
						for ( int i = rowStartIndex; i < rowEndIndex; i++ ) {
							final int offset = MatrixCPU.this.cols * i;
							for ( int j = 0; j < MatrixCPU.this.cols; j++ ) {
								B.matrix[MatrixCPU.this.rows * j + i] = MatrixCPU.this.matrix[offset + j];
							}
						}
					}
					catch ( final Throwable t ) {

					}
					finally {
						latch.countDown();
					}
				}
			};
		}

		for ( int i = 0; i < workers.length; i++ ) {
			ResourceManager.sumbitHeavyRunnable( workers[i] );
		}

		try {
			latch.await();
		}
		catch ( final InterruptedException e ) {
			throw new RuntimeException( e );
		}

		return B;
	}

	@Override
	public Matrix add( final Matrix B ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();
		B.assertResources();

		/* Check the matrices */
		Utils.ASSERT( this.rows == B.rows, "The amount of rows are not equal." );
		Utils.ASSERT( this.cols == B.cols, "The amount of columns are not equal." );
		Utils.ASSERT( B instanceof MatrixCPU, "The input matrix is not of the correct type." );

		/* Create the new Matrix */
		final Matrix C = new MatrixCPU( this.rows, this.cols );

		/* Set up the concurrency objects */
		final Runnable[] workers = new Runnable[ResourceManager.OPTIMAL_NUMBER_OF_THREADS];
		final CountDownLatch latch = new CountDownLatch( ResourceManager.OPTIMAL_NUMBER_OF_THREADS );

		/* Divide up all of the elements amongst all the threads */
		final int remainder = this.getNumberOfElements() % ResourceManager.OPTIMAL_NUMBER_OF_THREADS;
		final int elePerThread = this.getNumberOfElements() / ResourceManager.OPTIMAL_NUMBER_OF_THREADS;

		for ( int i = 0; i < ResourceManager.OPTIMAL_NUMBER_OF_THREADS; i++ ) {
			/* Last thread handles remainder */
			final int startIndex = elePerThread * i;
			final int endIndex = (i + 1) != ResourceManager.OPTIMAL_NUMBER_OF_THREADS ? elePerThread * (i + 1) : elePerThread * (i + 1) + remainder;

			workers[i] = new Runnable() {
				@Override
				public void run() {
					try {
						for ( int i = startIndex; i < endIndex; i++ ) {
							C.matrix[i] = MatrixCPU.this.matrix[i] + B.matrix[i];
						}
					}
					catch ( final Throwable t ) {

					}
					finally {
						latch.countDown();
					}
				}
			};
		}

		for ( int i = 0; i < workers.length; i++ ) {
			ResourceManager.sumbitHeavyRunnable( workers[i] );
		}

		try {
			latch.await();
		}
		catch ( final InterruptedException e ) {
			throw new RuntimeException( e );
		}

		return C;
	}

	@Override
	public Matrix subtract( final Matrix B ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();
		B.assertResources();

		/* Check the matrices */
		Utils.ASSERT( this.rows == B.rows, "The amount of rows are not equal." );
		Utils.ASSERT( this.cols == B.cols, "The amount of columns are not equal." );
		Utils.ASSERT( B instanceof MatrixCPU, "The input matrix is not of the correct type." );

		/* Create the new Matrix */
		final Matrix C = new MatrixCPU( this.rows, this.cols );

		/* Set up the concurrency objects */
		final Runnable[] workers = new Runnable[ResourceManager.OPTIMAL_NUMBER_OF_THREADS];
		final CountDownLatch latch = new CountDownLatch( ResourceManager.OPTIMAL_NUMBER_OF_THREADS );

		/* Divide up all of the elements amongst all the threads */
		final int remainder = this.getNumberOfElements() % ResourceManager.OPTIMAL_NUMBER_OF_THREADS;
		final int elePerThread = this.getNumberOfElements() / ResourceManager.OPTIMAL_NUMBER_OF_THREADS;

		for ( int i = 0; i < ResourceManager.OPTIMAL_NUMBER_OF_THREADS; i++ ) {
			/* Last thread handles remainder */
			final int startIndex = elePerThread * i;
			final int endIndex = (i + 1) != ResourceManager.OPTIMAL_NUMBER_OF_THREADS ? elePerThread * (i + 1) : elePerThread * (i + 1) + remainder;

			workers[i] = new Runnable() {
				@Override
				public void run() {
					try {
						for ( int i = startIndex; i < endIndex; i++ ) {
							C.matrix[i] = MatrixCPU.this.matrix[i] - B.matrix[i];
						}
					}
					catch ( final Throwable t ) {

					}
					finally {
						latch.countDown();
					}
				}
			};
		}

		for ( int i = 0; i < workers.length; i++ ) {
			ResourceManager.sumbitHeavyRunnable( workers[i] );
		}

		try {
			latch.await();
		}
		catch ( final InterruptedException e ) {
			throw new RuntimeException( e );
		}

		return C;
	}

	@Override
	public Matrix multiply( final Matrix B ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();
		B.assertResources();

		/* Check the matrices */
		Utils.ASSERT( this.rows == B.rows, "The amount of rows are not equal." );
		Utils.ASSERT( this.cols == B.cols, "The amount of columns are not equal." );
		Utils.ASSERT( B instanceof MatrixCPU, "The input matrix is not of the correct type." );

		/* Create the new Matrix */
		final Matrix C = new MatrixCPU( this.rows, this.cols );

		/* Set up the concurrency objects */
		final Runnable[] workers = new Runnable[ResourceManager.OPTIMAL_NUMBER_OF_THREADS];
		final CountDownLatch latch = new CountDownLatch( ResourceManager.OPTIMAL_NUMBER_OF_THREADS );

		/* Divide up all of the elements amongst all the threads */
		final int remainder = this.getNumberOfElements() % ResourceManager.OPTIMAL_NUMBER_OF_THREADS;
		final int elePerThread = this.getNumberOfElements() / ResourceManager.OPTIMAL_NUMBER_OF_THREADS;

		for ( int i = 0; i < ResourceManager.OPTIMAL_NUMBER_OF_THREADS; i++ ) {
			/* Last thread handles remainder */
			final int startIndex = elePerThread * i;
			final int endIndex = (i + 1) != ResourceManager.OPTIMAL_NUMBER_OF_THREADS ? elePerThread * (i + 1) : elePerThread * (i + 1) + remainder;

			workers[i] = new Runnable() {
				@Override
				public void run() {
					try {
						for ( int i = startIndex; i < endIndex; i++ ) {
							C.matrix[i] = MatrixCPU.this.matrix[i] * B.matrix[i];
						}
					}
					catch ( final Throwable t ) {

					}
					finally {
						latch.countDown();
					}
				}
			};
		}

		for ( int i = 0; i < workers.length; i++ ) {
			ResourceManager.sumbitHeavyRunnable( workers[i] );
		}

		try {
			latch.await();
		}
		catch ( final InterruptedException e ) {
			throw new RuntimeException( e );
		}

		return C;
	}

	@Override
	public Matrix multiply( final float val ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();

		/* Create the new Matrix */
		final Matrix C = new MatrixCPU( this.rows, this.cols );

		/* Set up the concurrency objects */
		final Runnable[] workers = new Runnable[ResourceManager.OPTIMAL_NUMBER_OF_THREADS];
		final CountDownLatch latch = new CountDownLatch( ResourceManager.OPTIMAL_NUMBER_OF_THREADS );

		/* Divide up all of the elements amongst all the threads */
		final int remainder = this.getNumberOfElements() % ResourceManager.OPTIMAL_NUMBER_OF_THREADS;
		final int elePerThread = this.getNumberOfElements() / ResourceManager.OPTIMAL_NUMBER_OF_THREADS;

		for ( int i = 0; i < ResourceManager.OPTIMAL_NUMBER_OF_THREADS; i++ ) {
			/* Last thread handles remainder */
			final int startIndex = elePerThread * i;
			final int endIndex = (i + 1) != ResourceManager.OPTIMAL_NUMBER_OF_THREADS ? elePerThread * (i + 1) : elePerThread * (i + 1) + remainder;

			workers[i] = new Runnable() {
				@Override
				public void run() {
					try {
						for ( int i = startIndex; i < endIndex; i++ ) {
							C.matrix[i] = MatrixCPU.this.matrix[i] * val;
						}
					}
					catch ( final Throwable t ) {

					}
					finally {

						latch.countDown();
					}
				}
			};
		}

		for ( int i = 0; i < workers.length; i++ ) {
			ResourceManager.sumbitHeavyRunnable( workers[i] );
		}

		try {
			latch.await();
		}
		catch ( final InterruptedException e ) {
			throw new RuntimeException( e );
		}

		return C;
	}

	@Override
	public Matrix constantByElementSubtraction( final float val ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();

		/* Create the new Matrix */
		final Matrix C = new MatrixCPU( this.rows, this.cols );

		/* Set up the concurrency objects */
		final Runnable[] workers = new Runnable[ResourceManager.OPTIMAL_NUMBER_OF_THREADS];
		final CountDownLatch latch = new CountDownLatch( ResourceManager.OPTIMAL_NUMBER_OF_THREADS );

		/* Divide up all of the elements amongst all the threads */
		final int remainder = this.getNumberOfElements() % ResourceManager.OPTIMAL_NUMBER_OF_THREADS;
		final int elePerThread = this.getNumberOfElements() / ResourceManager.OPTIMAL_NUMBER_OF_THREADS;

		for ( int i = 0; i < ResourceManager.OPTIMAL_NUMBER_OF_THREADS; i++ ) {
			/* Last thread handles remainder */
			final int startIndex = elePerThread * i;
			final int endIndex = (i + 1) != ResourceManager.OPTIMAL_NUMBER_OF_THREADS ? elePerThread * (i + 1) : elePerThread * (i + 1) + remainder;

			workers[i] = new Runnable() {
				@Override
				public void run() {
					try {

						for ( int i = startIndex; i < endIndex; i++ ) {
							C.matrix[i] = val - MatrixCPU.this.matrix[i];
						}
					}
					catch ( final Throwable t ) {

					}
					finally {
						latch.countDown();
					}
				}
			};
		}

		for ( int i = 0; i < workers.length; i++ ) {
			ResourceManager.sumbitHeavyRunnable( workers[i] );
		}

		try {
			latch.await();
		}
		catch ( final InterruptedException e ) {
			throw new RuntimeException( e );
		}

		return C;
	}

	@Override
	public Matrix elementByConstantSubtraction( final float val ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();

		/* Create the new Matrix */
		final Matrix C = new MatrixCPU( this.rows, this.cols );

		/* Set up the concurrency objects */
		final Runnable[] workers = new Runnable[ResourceManager.OPTIMAL_NUMBER_OF_THREADS];
		final CountDownLatch latch = new CountDownLatch( ResourceManager.OPTIMAL_NUMBER_OF_THREADS );

		/* Divide up all of the elements amongst all the threads */
		final int remainder = this.getNumberOfElements() % ResourceManager.OPTIMAL_NUMBER_OF_THREADS;
		final int elePerThread = this.getNumberOfElements() / ResourceManager.OPTIMAL_NUMBER_OF_THREADS;

		for ( int i = 0; i < ResourceManager.OPTIMAL_NUMBER_OF_THREADS; i++ ) {
			/* Last thread handles remainder */
			final int startIndex = elePerThread * i;
			final int endIndex = (i + 1) != ResourceManager.OPTIMAL_NUMBER_OF_THREADS ? elePerThread * (i + 1) : elePerThread * (i + 1) + remainder;

			workers[i] = new Runnable() {
				@Override
				public void run() {
					try {
						for ( int i = startIndex; i < endIndex; i++ ) {
							C.matrix[i] = MatrixCPU.this.matrix[i] - val;
						}
					}
					catch ( final Throwable t ) {

					}
					finally {
						latch.countDown();
					}
				}
			};
		}

		for ( int i = 0; i < workers.length; i++ ) {
			ResourceManager.sumbitHeavyRunnable( workers[i] );
		}

		try {
			latch.await();
		}
		catch ( final InterruptedException e ) {
			throw new RuntimeException( e );
		}

		return C;
	}

	@Override
	public Matrix applySigmoid() {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();

		/* Create the new Matrix */
		final Matrix C = new MatrixCPU( this.rows, this.cols );

		/* Set up the concurrency objects */
		final Runnable[] workers = new Runnable[ResourceManager.OPTIMAL_NUMBER_OF_THREADS];
		final CountDownLatch latch = new CountDownLatch( ResourceManager.OPTIMAL_NUMBER_OF_THREADS );

		/* Divide up all of the elements amongst all the threads */
		final int remainder = this.getNumberOfElements() % ResourceManager.OPTIMAL_NUMBER_OF_THREADS;
		final int elePerThread = this.getNumberOfElements() / ResourceManager.OPTIMAL_NUMBER_OF_THREADS;

		for ( int i = 0; i < ResourceManager.OPTIMAL_NUMBER_OF_THREADS; i++ ) {
			/* Last thread handles remainder */
			final int startIndex = elePerThread * i;
			final int endIndex = (i + 1) != ResourceManager.OPTIMAL_NUMBER_OF_THREADS ? elePerThread * (i + 1) : elePerThread * (i + 1) + remainder;

			workers[i] = new Runnable() {
				@Override
				public void run() {
					try {
						for ( int i = startIndex; i < endIndex; i++ ) {
							C.matrix[i] = (float) (1.0 / (1.0 + java.lang.Math.pow( java.lang.Math.E, -1.0 * MatrixCPU.this.matrix[i] )));
						}
					}
					catch ( final Throwable t ) {

					}
					finally {
						latch.countDown();
					}
				}
			};
		}

		for ( int i = 0; i < workers.length; i++ ) {
			ResourceManager.sumbitHeavyRunnable( workers[i] );
		}

		try {
			latch.await();
		}
		catch ( final InterruptedException e ) {
			throw new RuntimeException( e );
		}

		return C;
	}

	@Override
	public void free() {
		if ( !this.isFreed ) {
			this.matrix = null;
			this.isFreed = true;
		}
	}
}
