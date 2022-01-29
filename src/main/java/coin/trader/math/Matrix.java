package coin.trader.math;

import coin.trader.logger.Logger;
import coin.trader.utilities.Utils;

public abstract class Matrix {
	private static Logger LOGGER = new Logger( Matrix.class.getSimpleName() );
	protected static final float EPSILON = 1e-7f;

	/*
	 * For right now, our matrices are small so the CPU is be best device for
	 * calculations. JCublas is better only when the matrices are large ( M or N
	 * is > 1024 ). OpenCL is not working properly, but that offered the best
	 * potential gains.
	 */
	private static MatrixImplemention CALCULATION_IMPLEMENTATION;
	protected final int rows;
	protected final int cols;
	protected float[] matrix;
	protected boolean isFreed = false;

	static {
		Matrix.setMatrixCalculationImplementation( MatrixImplemention.CPU );
	}

	public Matrix( final int rows, final int cols ) {
		this.rows = rows;
		this.cols = cols;
	}

	public enum MemorySync {
		HOST_MEMORY_UPDATED,
		GPU_BUFFER_NOT_SYNCED,
		SYNCED
	}

	public static interface ISyncHostWithGPU {
		public void syncMemory();

		public void free();
	}

	public enum MatrixImplemention {
		CPU,
		GPU_OPEN_CL,
		GPU_JCUBLAS
	}

	public static void setMatrixCalculationImplementation( final MatrixImplemention v ) {
		if ( v != null ) {
			Matrix.LOGGER.config( "Setting Matrix Calculation Implementation to " + v );
			Matrix.CALCULATION_IMPLEMENTATION = v;
		}
	}

	public static MatrixImplemention getMatrixCalculationImplementation() {
		return Matrix.CALCULATION_IMPLEMENTATION;
	}

	public static Matrix create( final int rows, final int cols ) {
		if ( Matrix.CALCULATION_IMPLEMENTATION.equals( MatrixImplemention.GPU_JCUBLAS ) ) {
			return new MatrixJCublas( rows, cols );
		}
		else if ( Matrix.CALCULATION_IMPLEMENTATION.equals( MatrixImplemention.CPU ) ) {
			return new MatrixCPU( rows, cols );
		}
		else if ( Matrix.CALCULATION_IMPLEMENTATION.equals( MatrixImplemention.GPU_OPEN_CL ) ) {
			return new MatrixOpenCL( rows, cols );
		}
		else {
			return null;
		}
	}

	public abstract void set( final int row, final int col, final float val );

	public abstract float get( final int row, final int col );

	public int getRows() {
		return this.rows;
	}

	public int getCols() {
		return this.cols;
	}

	/**
	 * Matrix Mult
	 * 
	 * @param B
	 * @return
	 */
	public abstract Matrix dot( final Matrix B );

	/**
	 * Transpose
	 * 
	 * @return
	 */
	public abstract Matrix transpose();

	/**
	 * Element by Element addition
	 * 
	 * @param B
	 * @return
	 */
	public abstract Matrix add( final Matrix B );

	/**
	 * Element by Element subtraction
	 * 
	 * @param B
	 * @return
	 */
	public abstract Matrix subtract( final Matrix B );

	/**
	 * Element by Element multiplication
	 * 
	 * @param B
	 * @return
	 */
	public abstract Matrix multiply( final Matrix B );

	/**
	 * Element by constant multiplication
	 * 
	 * @param val
	 * @return
	 */
	public abstract Matrix multiply( final float val );

	/**
	 * Constant by Element subtraction
	 * 
	 * @param val
	 * @return
	 */
	public abstract Matrix constantByElementSubtraction( final float val );

	/**
	 * Element by Constant subtraction
	 * 
	 * @param val
	 * @return
	 */
	public abstract Matrix elementByConstantSubtraction( final float val );

	/**
	 * Applies the sigmoid function to each element
	 * 
	 * @return
	 */
	public abstract Matrix applySigmoid();

	/**
	 * Releases any resources this matrix is using. The matrix becomes unusable
	 * after this is called.
	 */
	public abstract void free();

	public boolean isFreed() {
		return this.isFreed;
	}

	@Override
	public void finalize() {
		this.free();
	}

	protected void assertResources() {
		Utils.ASSERT( !this.isFreed, "The matrix was freed and cannot be used for further calculations." );
	}

	/**
	 * @return The number of elements in the matrix
	 */
	public int getNumberOfElements() {
		return this.rows * this.cols;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

		for ( int i = 0; i < this.rows; i++ ) {
			sb.append( "[ " );
			for ( int j = 0; j < this.cols; j++ ) {
				sb.append( this.get( i, j ) );

				if ( j < this.cols - 1 ) {
					sb.append( ", " );
				}
			}

			if ( i < this.rows - 1 ) {
				sb.append( " ]\n" );
			}
		}

		sb.append( " ]" );

		return sb.toString();
	}

	@Override
	public boolean equals( final Object obj ) {
		if ( !(obj instanceof Matrix) ) {
			return false;
		}

		final Matrix matrix = (Matrix) obj;

		if ( this.isFreed == true && matrix.isFreed == true ) {
			return true;
		}
		else if ( this.isFreed != matrix.isFreed ) {
			return false;
		}

		{
			if ( !(this.rows == matrix.rows && this.cols == matrix.cols) ) {
				return false;
			}
		}

		for ( int i = 0; i < this.rows; i++ ) {
			for ( int j = 0; j < this.cols; j++ ) {
				if ( Matrix.EPSILON < java.lang.Math.abs( this.get( i, j ) - matrix.get( i, j ) ) ) {
					return false;
				}
			}
		}

		return true;
	}
}
