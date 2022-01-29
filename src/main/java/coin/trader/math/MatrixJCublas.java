package coin.trader.math;

import coin.trader.logger.Logger;
import coin.trader.utilities.Utils;
import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.jcublas.JCublas;
import jcuda.jcublas.JCublas2;
import jcuda.jcublas.cublasHandle;
import jcuda.jcublas.cublasOperation;

/*
 * TODO(EMU): Everything that is easily done on the CPU ( Vector Operations ),
 * do on the CPU. TODO(EMU): Set up the GPU memory and keep everything in memory
 * and free stuff when done.
 */

/**
 * @author Evan
 *
 */
public class MatrixJCublas extends Matrix {
	private static final Logger LOGGER = new Logger( MatrixJCublas.class.getSimpleName() );
	private static final float ALPHA = 1.0f;
	private static final float[] ALPHA_P = new float[] { MatrixJCublas.ALPHA };
	private static final float BETA = 0.0f;
	private static final float[] BETA_P = new float[] { MatrixJCublas.BETA };
	private static final cublasHandle HANDLE = new cublasHandle();

	private Pointer matrix_P;
	private boolean memoryInSync = false;

	static {
		MatrixJCublas.LOGGER.config( "Initializing JCublas." );
		JCublas.cublasInit();
		JCublas.setExceptionsEnabled( true );
		JCublas2.cublasCreate( MatrixJCublas.HANDLE );
		JCublas2.setExceptionsEnabled( true );
		MatrixJCublas.LOGGER.config( "Initialized JCublas." );
	}

	public MatrixJCublas( final int rows, final int cols ) {
		super( rows, cols );
		this.matrix = new float[this.getNumberOfElements()];
		this.matrix_P = new Pointer();

		/* Allocate and set memory on the GPU */
		JCublas.cublasAlloc( this.getNumberOfElements(), Sizeof.FLOAT, this.matrix_P );
	}

	private MatrixJCublas( final float[] a, final Pointer p, final int rows, final int cols ) {
		super( rows, cols );

		this.matrix = a;
		this.matrix_P = p;
	}

	private MatrixJCublas( final Matrix A ) {
		super( A.rows, A.cols );
		this.matrix = A.matrix;
		this.matrix_P = new Pointer();

		/* Allocate and set memory on the GPU */
		JCublas.cublasAlloc( this.getNumberOfElements(), Sizeof.FLOAT, this.matrix_P );
	}

	@Override
	public void set( final int row, final int col, final float val ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();

		this.memoryInSync = false;
		this.matrix[this.rows * col + row] = val;
	}

	@Override
	public float get( final int row, final int col ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();

		return this.matrix[this.rows * col + row];
	}

	private void syncMemory() {
		if ( !this.memoryInSync ) {
			JCublas2.cublasSetVector( this.getNumberOfElements(), Sizeof.FLOAT, Pointer.to( this.matrix ), 1, this.matrix_P, 1 );
			this.memoryInSync = true;
		}
	}

	@Override
	public Matrix dot( final Matrix B ) {
		/* Assert Matrices are of the correct dimensions */
		Utils.ASSERT( this.cols == B.rows, "The amount of columns is not equal to the number of rows of the input matrix." );
		Utils.ASSERT( B instanceof MatrixJCublas, "The input matrix was not of the correct type." );

		final MatrixJCublas B_ = (MatrixJCublas) B;

		/* Must make sure this matrix is fit for computations */
		this.assertResources();
		B.assertResources();

		/* Ensure the GPU and host memory are in sync */
		this.syncMemory();
		B_.syncMemory();

		/* Calculate the number of elements in C */
		final int numOfElements = this.rows * B.cols;

		/* Create the output array */
		final float[] c = new float[numOfElements];

		/* Set up the Pointers */
		final Pointer Cp = new Pointer();

		/* Allocate device memory */
		JCublas.cublasAlloc( numOfElements, Sizeof.FLOAT, Cp );

		/* Copy the host memory to the device memory */
		JCublas2.cublasSetVector( numOfElements, Sizeof.FLOAT, Pointer.to( c ), 1, Cp, 1 );

		/* Calculate */
		JCublas2.cublasSgemm( MatrixJCublas.HANDLE, cublasOperation.CUBLAS_OP_N, cublasOperation.CUBLAS_OP_N, this.rows, B.cols, this.cols, Pointer.to( MatrixJCublas.ALPHA_P ),
				this.matrix_P, this.rows, B_.matrix_P, B.rows, Pointer.to( MatrixJCublas.BETA_P ), Cp, this.rows );

		/* Read the result back */
		JCublas2.cublasGetVector( numOfElements, Sizeof.FLOAT, Cp, 1, Pointer.to( c ), 1 );

		return new MatrixJCublas( c, Cp, this.rows, B.cols );
	}

	@Override
	public Matrix transpose() {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();

		this.syncMemory();

		/* Create the output array */
		final float[] b = new float[this.getNumberOfElements()];

		/* Set up the Pointers */
		final Pointer Bp = new Pointer();

		/* Allocate device memory */
		JCublas.cublasAlloc( b.length, Sizeof.FLOAT, Bp );

		/* Copy the host memory to the device memory */
		JCublas2.cublasSetVector( b.length, Sizeof.FLOAT, Pointer.to( this.matrix ), 1, this.matrix_P, 1 );

		/* Calculate */
		JCublas2.cublasSgeam( MatrixJCublas.HANDLE, cublasOperation.CUBLAS_OP_T, cublasOperation.CUBLAS_OP_N, this.rows, this.cols, Pointer.to( MatrixJCublas.ALPHA_P ),
				this.matrix_P, this.cols, Pointer.to( MatrixJCublas.BETA_P ), this.matrix_P, this.rows, Bp, this.rows );

		/* Read the result back */
		JCublas2.cublasGetVector( b.length, Sizeof.FLOAT, Bp, 1, Pointer.to( b ), 1 );

		return new MatrixJCublas( b, Bp, this.cols, this.rows );
	}

	@Override
	public Matrix add( final Matrix B ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();
		B.assertResources();

		Utils.ASSERT( B instanceof MatrixJCublas, "The input matrix was not of the correct type." );

		/* Do the computation on the CPU */
		final Matrix C = (new MatrixCPU( this ).add( new MatrixCPU( B ) ));

		return new MatrixJCublas( C );
	}

	@Override
	public Matrix subtract( final Matrix B ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();
		B.assertResources();

		Utils.ASSERT( B instanceof MatrixJCublas, "The input matrix was not of the correct type." );

		/* Do the computation on the CPU */
		final Matrix C = (new MatrixCPU( this ).subtract( new MatrixCPU( B ) ));

		return new MatrixJCublas( C );
	}

	@Override
	public Matrix multiply( final Matrix B ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();
		B.assertResources();

		Utils.ASSERT( B instanceof MatrixJCublas, "The input matrix was not of the correct type." );

		/* Do the computation on the CPU */
		final Matrix C = (new MatrixCPU( this ).multiply( new MatrixCPU( B ) ));

		return new MatrixJCublas( C );
	}

	@Override
	public Matrix multiply( final float val ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();

		/* Do the computation on the CPU */
		final Matrix C = (new MatrixCPU( this ).multiply( val ));

		return new MatrixJCublas( C );
	}

	@Override
	public Matrix constantByElementSubtraction( final float val ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();

		/* Do the computation on the CPU */
		final Matrix C = (new MatrixCPU( this ).constantByElementSubtraction( val ));

		return new MatrixJCublas( C );
	}

	@Override
	public Matrix elementByConstantSubtraction( final float val ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();

		/* Do the computation on the CPU */
		final Matrix C = (new MatrixCPU( this ).elementByConstantSubtraction( val ));

		return new MatrixJCublas( C );
	}

	@Override
	public Matrix applySigmoid() {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();

		/* Do the computation on the CPU */
		final Matrix C = (new MatrixCPU( this ).applySigmoid());

		return new MatrixJCublas( C );
	}

	@Override
	public void free() {
		if ( !this.isFreed ) {
			this.matrix = null;
			JCublas.cublasFree( this.matrix_P );
			this.matrix_P = null;
			this.isFreed = true;
		}
	}

	public static void shutdownJCublas() {
		JCublas2.cublasDestroy( MatrixJCublas.HANDLE );
		JCublas.cublasShutdown();
	}
}
