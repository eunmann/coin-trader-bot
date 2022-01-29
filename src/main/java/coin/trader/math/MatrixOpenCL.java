package coin.trader.math;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;
import org.jocl.cl_queue_properties;

import coin.trader.logger.Logger;
import coin.trader.math.opencl.OpenCLResourceLoader;
import coin.trader.utilities.Utils;

public class MatrixOpenCL extends Matrix {
	private static final Logger LOGGER = new Logger( MatrixOpenCL.class.getSimpleName(), Logger.Level.INFO );
	private static cl_context GPU_CONTEXT;
	private static cl_device_id GPU_DEVICE_ID;
	private static cl_program PROGRAM;

	private final int paddedRows;
	private final int paddedCols;
	private cl_mem matrix_P;
	private boolean readFromGPU;
	private boolean writeToGPU;
	private ISyncHostWithGPU syncWithHost;

	public MatrixOpenCL( final int rows, final int cols ) {
		super( rows, cols );
		this.paddedRows = MatrixOpenCL.padDimension( rows, TileSizes.TSM );
		this.paddedCols = MatrixOpenCL.padDimension( cols, TileSizes.TSN );
		this.matrix = new float[this.paddedRows * this.paddedCols];
		this.matrix_P = MatrixOpenCL.createGPUBuffer( this.matrix );
		this.syncWithHost = null;
		this.readFromGPU = false;
		this.writeToGPU = false;
	}

	public MatrixOpenCL( final float[] a, final cl_mem a_P, final int rows, final int cols, final int paddedRows, final int paddedCols, final ISyncHostWithGPU syncWithHost ) {
		super( rows, cols );

		Utils.ASSERT( paddedRows % TileSizes.TSM == 0, "The amount of padded rows is not a proper multiple." );
		Utils.ASSERT( paddedCols % TileSizes.TSN == 0, "The amount of padded cols is not a proper multiple." );

		this.paddedRows = paddedRows;
		this.paddedCols = paddedCols;
		this.matrix = a;
		this.matrix_P = a_P;
		this.syncWithHost = syncWithHost;
		this.readFromGPU = true;
		this.writeToGPU = false;
	}

	private void readFromGPU() {
		if ( this.readFromGPU ) {
			this.syncWithHost.syncMemory();
			this.readFromGPU = false;
		}
	}

	public void writeToGPU() {
		if ( this.writeToGPU ) {
			CL.clReleaseMemObject( this.matrix_P );
			this.matrix_P = MatrixOpenCL.createGPUBuffer( this.matrix );
			this.writeToGPU = false;
		}
	}

	@Override
	public void set( final int row, final int col, final float val ) {
		this.assertResources();
		this.readFromGPU();
		this.writeToGPU = true;
		this.matrix[this.paddedRows * col + row] = val;
	}

	@Override
	public float get( final int row, final int col ) {
		this.assertResources();
		this.readFromGPU();
		return this.matrix[this.paddedRows * col + row];
	}

	public int getPaddedRows() {
		return this.paddedRows;
	}

	public int getPaddedCols() {
		return this.paddedCols;
	}

	@Override
	public Matrix dot( final Matrix B ) {
		/* Assert Matrices are of the correct dimensions */
		Utils.ASSERT( this.cols == B.rows, "The amount of columns is not equal to the number of rows of the input matrix." );
		Utils.ASSERT( B instanceof MatrixOpenCL, "The input matrix was not of the correct type." );
		final MatrixOpenCL B_ = (MatrixOpenCL) B;
		final MatrixOpenCL B_T = (MatrixOpenCL) B_.transpose();

		/* Must make sure this matrix is fit for computations */
		this.assertResources();
		B_.assertResources();

		this.writeToGPU();
		B_.writeToGPU();

		final float[] c = new float[this.paddedRows * B_.paddedCols];
		final Pointer dst = Pointer.to( c );
		final Pointer mPtr = Pointer.to( new int[] { this.paddedRows } );
		final Pointer kPtr = Pointer.to( new int[] { this.paddedCols } );
		final Pointer nPtr = Pointer.to( new int[] { B_.paddedCols } );

		// Allocate the memory objects for the input- and output data
		final cl_mem memObjects[] = new cl_mem[4];
		memObjects[0] = CL.clCreateBuffer( MatrixOpenCL.GPU_CONTEXT, CL.CL_MEM_READ_WRITE, Sizeof.cl_float * c.length, null, null );
		memObjects[1] = CL.clCreateBuffer( MatrixOpenCL.GPU_CONTEXT, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float, mPtr, null );
		memObjects[2] = CL.clCreateBuffer( MatrixOpenCL.GPU_CONTEXT, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float, kPtr, null );
		memObjects[3] = CL.clCreateBuffer( MatrixOpenCL.GPU_CONTEXT, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float, nPtr, null );

		// Create the kernel
		final cl_kernel kernel = CL.clCreateKernel( MatrixOpenCL.PROGRAM, "matrixMultiplication", null );

		// Create a command-queue for the selected device
		final cl_command_queue commandQueue = CL.clCreateCommandQueueWithProperties( MatrixOpenCL.GPU_CONTEXT, MatrixOpenCL.GPU_DEVICE_ID, new cl_queue_properties(), null );

		// Set the arguments for the kernel
		CL.clSetKernelArg( kernel, 0, Sizeof.cl_mem, Pointer.to( this.matrix_P ) );
		CL.clSetKernelArg( kernel, 1, Sizeof.cl_mem, Pointer.to( B_T.matrix_P ) );
		CL.clSetKernelArg( kernel, 2, Sizeof.cl_mem, Pointer.to( memObjects[0] ) );
		CL.clSetKernelArg( kernel, 3, Sizeof.cl_mem, Pointer.to( memObjects[1] ) );
		CL.clSetKernelArg( kernel, 4, Sizeof.cl_mem, Pointer.to( memObjects[2] ) );
		CL.clSetKernelArg( kernel, 5, Sizeof.cl_mem, Pointer.to( memObjects[3] ) );

		// Set the work-item dimensions
		final long global_work_size[] = new long[] { this.paddedRows / TileSizes.WPTM, B_.paddedCols / TileSizes.WPTN };
		final long local_work_size[] = new long[] { TileSizes.RTSM, TileSizes.RTSN };

		// Execute the kernel
		CL.clEnqueueNDRangeKernel( commandQueue, kernel, 2, null, global_work_size, local_work_size, 0, null, null );

		return new MatrixOpenCL( c, memObjects[0], this.rows, B_.cols, this.paddedRows, B_.paddedCols, new ISyncHostWithGPU() {

			@Override
			public void syncMemory() {
				// Read the output data
				CL.clEnqueueReadBuffer( commandQueue, memObjects[0], CL.CL_TRUE, 0, c.length * Sizeof.cl_float, dst, 0, null, null );
			}

			@Override
			public void free() {
				// Release kernel, program, and memory objects
				CL.clReleaseMemObject( memObjects[1] );
				CL.clReleaseMemObject( memObjects[2] );
				CL.clReleaseMemObject( memObjects[3] );
				CL.clReleaseKernel( kernel );
				CL.clReleaseCommandQueue( commandQueue );
			}
		} );
	}

	@Override
	public Matrix transpose() {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();
		this.writeToGPU();

		final float[] c = new float[this.matrix.length];
		final Pointer dst = Pointer.to( c );
		final Pointer rowsPtr = Pointer.to( new int[] { this.paddedRows } );
		final Pointer colsPtr = Pointer.to( new int[] { this.paddedCols } );

		// Allocate the memory objects for the input- and output data
		final cl_mem memObjects[] = new cl_mem[3];
		memObjects[0] = CL.clCreateBuffer( MatrixOpenCL.GPU_CONTEXT, CL.CL_MEM_READ_WRITE, Sizeof.cl_float * c.length, null, null );
		memObjects[1] = CL.clCreateBuffer( MatrixOpenCL.GPU_CONTEXT, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float, rowsPtr, null );
		memObjects[2] = CL.clCreateBuffer( MatrixOpenCL.GPU_CONTEXT, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float, colsPtr, null );

		// Create the kernel
		final cl_kernel kernel = CL.clCreateKernel( MatrixOpenCL.PROGRAM, "transpose", null );

		// Create a command-queue for the selected device
		final cl_command_queue commandQueue = CL.clCreateCommandQueueWithProperties( MatrixOpenCL.GPU_CONTEXT, MatrixOpenCL.GPU_DEVICE_ID, new cl_queue_properties(), null );

		// Set the arguments for the kernel
		CL.clSetKernelArg( kernel, 0, Sizeof.cl_mem, Pointer.to( this.matrix_P ) );
		CL.clSetKernelArg( kernel, 1, Sizeof.cl_mem, Pointer.to( memObjects[0] ) );
		CL.clSetKernelArg( kernel, 2, Sizeof.cl_mem, Pointer.to( memObjects[1] ) );
		CL.clSetKernelArg( kernel, 3, Sizeof.cl_mem, Pointer.to( memObjects[2] ) );

		// Set the work-item dimensions
		final long global_work_size[] = new long[] { this.paddedRows, this.paddedCols };
		final long local_work_size[] = new long[] { TileSizes.TRANSPOSEX, TileSizes.TRANSPOSEY };

		// Execute the kernel
		CL.clEnqueueNDRangeKernel( commandQueue, kernel, 2, null, global_work_size, local_work_size, 0, null, null );

		return new MatrixOpenCL( c, memObjects[0], this.cols, this.rows, this.paddedCols, this.paddedRows, new ISyncHostWithGPU() {

			@Override
			public void syncMemory() {
				// Read the output data
				CL.clEnqueueReadBuffer( commandQueue, memObjects[0], CL.CL_TRUE, 0, c.length * Sizeof.cl_float, dst, 0, null, null );
			}

			@Override
			public void free() {
				// Release kernel, program, and memory objects
				CL.clReleaseMemObject( memObjects[1] );
				CL.clReleaseMemObject( memObjects[2] );
				CL.clReleaseKernel( kernel );
				CL.clReleaseCommandQueue( commandQueue );
			}
		} );
	}

	@Override
	public Matrix add( final Matrix B ) {
		Utils.ASSERT( this.rows == B.rows, "The amount of rows are not equal." );
		Utils.ASSERT( this.cols == B.cols, "The amount of columns are not equal." );
		Utils.ASSERT( B instanceof MatrixOpenCL, "The input matrix was not of the correct type." );
		final MatrixOpenCL B_ = (MatrixOpenCL) B;

		/* Must make sure this matrix is fit for computations */
		this.assertResources();
		B_.assertResources();

		this.writeToGPU();
		B_.writeToGPU();

		final int n = this.matrix.length;
		final float[] c = new float[n];
		final Pointer dst = Pointer.to( c );

		// Allocate the memory objects for the input- and output data
		final cl_mem memObjects[] = new cl_mem[1];
		memObjects[0] = CL.clCreateBuffer( MatrixOpenCL.GPU_CONTEXT, CL.CL_MEM_READ_WRITE, Sizeof.cl_float * n, null, null );

		// Create the kernel
		final cl_kernel kernel = CL.clCreateKernel( MatrixOpenCL.PROGRAM, "addElements", null );

		// Create a command-queue for the selected device
		final cl_command_queue commandQueue = CL.clCreateCommandQueueWithProperties( MatrixOpenCL.GPU_CONTEXT, MatrixOpenCL.GPU_DEVICE_ID, new cl_queue_properties(), null );

		// Set the arguments for the kernel
		CL.clSetKernelArg( kernel, 0, Sizeof.cl_mem, Pointer.to( this.matrix_P ) );
		CL.clSetKernelArg( kernel, 1, Sizeof.cl_mem, Pointer.to( B_.matrix_P ) );
		CL.clSetKernelArg( kernel, 2, Sizeof.cl_mem, Pointer.to( memObjects[0] ) );

		// Set the work-item dimensions
		final long global_work_size[] = new long[] { n };
		final long local_work_size[] = new long[] { 1 };

		// Execute the kernel
		CL.clEnqueueNDRangeKernel( commandQueue, kernel, 1, null, global_work_size, local_work_size, 0, null, null );

		return new MatrixOpenCL( c, memObjects[0], this.rows, this.cols, this.paddedRows, this.paddedCols, new ISyncHostWithGPU() {

			@Override
			public void syncMemory() {
				// Read the output data
				CL.clEnqueueReadBuffer( commandQueue, memObjects[0], CL.CL_TRUE, 0, n * Sizeof.cl_float, dst, 0, null, null );
			}

			@Override
			public void free() {
				// Release kernel, program, and memory objects
				CL.clReleaseKernel( kernel );
				CL.clReleaseCommandQueue( commandQueue );
			}
		} );
	}

	@Override
	public Matrix subtract( final Matrix B ) {
		Utils.ASSERT( this.rows == B.rows, "The amount of rows are not equal." );
		Utils.ASSERT( this.cols == B.cols, "The amount of columns are not equal." );
		Utils.ASSERT( B instanceof MatrixOpenCL, "The input matrix was not of the correct type." );
		final MatrixOpenCL B_ = (MatrixOpenCL) B;

		/* Must make sure this matrix is fit for computations */
		this.assertResources();
		B_.assertResources();

		this.writeToGPU();
		B_.writeToGPU();

		final int n = this.matrix.length;
		final float[] c = new float[n];
		final Pointer dst = Pointer.to( c );

		// Allocate the memory objects for the input- and output data
		final cl_mem memObjects[] = new cl_mem[1];
		memObjects[0] = CL.clCreateBuffer( MatrixOpenCL.GPU_CONTEXT, CL.CL_MEM_READ_WRITE, Sizeof.cl_float * n, null, null );

		// Create the kernel
		final cl_kernel kernel = CL.clCreateKernel( MatrixOpenCL.PROGRAM, "subtractElements", null );

		// Create a command-queue for the selected device
		final cl_command_queue commandQueue = CL.clCreateCommandQueueWithProperties( MatrixOpenCL.GPU_CONTEXT, MatrixOpenCL.GPU_DEVICE_ID, new cl_queue_properties(), null );

		// Set the arguments for the kernel
		CL.clSetKernelArg( kernel, 0, Sizeof.cl_mem, Pointer.to( this.matrix_P ) );
		CL.clSetKernelArg( kernel, 1, Sizeof.cl_mem, Pointer.to( B_.matrix_P ) );
		CL.clSetKernelArg( kernel, 2, Sizeof.cl_mem, Pointer.to( memObjects[0] ) );

		// Set the work-item dimensions
		final long global_work_size[] = new long[] { n };
		final long local_work_size[] = new long[] { 1 };

		// Execute the kernel
		CL.clEnqueueNDRangeKernel( commandQueue, kernel, 1, null, global_work_size, local_work_size, 0, null, null );

		return new MatrixOpenCL( c, memObjects[0], this.rows, this.cols, this.paddedRows, this.paddedCols, new ISyncHostWithGPU() {

			@Override
			public void syncMemory() {
				// Read the output data
				CL.clEnqueueReadBuffer( commandQueue, memObjects[0], CL.CL_TRUE, 0, n * Sizeof.cl_float, dst, 0, null, null );
			}

			@Override
			public void free() {
				// Release kernel, program, and memory objects
				CL.clReleaseKernel( kernel );
				CL.clReleaseCommandQueue( commandQueue );
			}
		} );
	}

	@Override
	public Matrix multiply( final Matrix B ) {
		Utils.ASSERT( this.rows == B.rows, "The amount of rows are not equal." );
		Utils.ASSERT( this.cols == B.cols, "The amount of columns are not equal." );
		Utils.ASSERT( B instanceof MatrixOpenCL, "The input matrix was not of the correct type." );
		final MatrixOpenCL B_ = (MatrixOpenCL) B;

		/* Must make sure this matrix is fit for computations */
		this.assertResources();
		B_.assertResources();

		this.writeToGPU();
		B_.writeToGPU();

		final int n = this.matrix.length;
		final float[] c = new float[n];
		final Pointer dst = Pointer.to( c );

		// Allocate the memory objects for the input- and output data
		final cl_mem memObjects[] = new cl_mem[1];
		memObjects[0] = CL.clCreateBuffer( MatrixOpenCL.GPU_CONTEXT, CL.CL_MEM_READ_WRITE, Sizeof.cl_float * n, null, null );

		// Create the kernel
		final cl_kernel kernel = CL.clCreateKernel( MatrixOpenCL.PROGRAM, "multiplyElements", null );

		// Create a command-queue for the selected device
		final cl_command_queue commandQueue = CL.clCreateCommandQueueWithProperties( MatrixOpenCL.GPU_CONTEXT, MatrixOpenCL.GPU_DEVICE_ID, new cl_queue_properties(), null );

		// Set the arguments for the kernel
		CL.clSetKernelArg( kernel, 0, Sizeof.cl_mem, Pointer.to( this.matrix_P ) );
		CL.clSetKernelArg( kernel, 1, Sizeof.cl_mem, Pointer.to( B_.matrix_P ) );
		CL.clSetKernelArg( kernel, 2, Sizeof.cl_mem, Pointer.to( memObjects[0] ) );

		// Set the work-item dimensions
		final long global_work_size[] = new long[] { n };
		final long local_work_size[] = new long[] { 1 };

		// Execute the kernel
		CL.clEnqueueNDRangeKernel( commandQueue, kernel, 1, null, global_work_size, local_work_size, 0, null, null );

		return new MatrixOpenCL( c, memObjects[0], this.rows, this.cols, this.paddedRows, this.paddedCols, new ISyncHostWithGPU() {

			@Override
			public void syncMemory() {
				// Read the output data
				CL.clEnqueueReadBuffer( commandQueue, memObjects[0], CL.CL_TRUE, 0, n * Sizeof.cl_float, dst, 0, null, null );
			}

			@Override
			public void free() {
				// Release kernel, program, and memory objects
				CL.clReleaseKernel( kernel );
				CL.clReleaseCommandQueue( commandQueue );
			}
		} );
	}

	@Override
	public Matrix multiply( final float val ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();
		this.writeToGPU();

		final int n = this.matrix.length;
		final float[] c = new float[n];
		final Pointer srcB = Pointer.to( new float[] { val } );
		final Pointer dst = Pointer.to( c );

		// Allocate the memory objects for the input- and output data
		final cl_mem memObjects[] = new cl_mem[2];
		memObjects[0] = CL.clCreateBuffer( MatrixOpenCL.GPU_CONTEXT, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float, srcB, null );
		memObjects[1] = CL.clCreateBuffer( MatrixOpenCL.GPU_CONTEXT, CL.CL_MEM_READ_WRITE, Sizeof.cl_float * n, null, null );

		// Create the kernel
		final cl_kernel kernel = CL.clCreateKernel( MatrixOpenCL.PROGRAM, "multiplyElementsByConstant", null );

		// Create a command-queue for the selected device
		final cl_command_queue commandQueue = CL.clCreateCommandQueueWithProperties( MatrixOpenCL.GPU_CONTEXT, MatrixOpenCL.GPU_DEVICE_ID, new cl_queue_properties(), null );

		// Set the arguments for the kernel
		CL.clSetKernelArg( kernel, 0, Sizeof.cl_mem, Pointer.to( this.matrix_P ) );
		CL.clSetKernelArg( kernel, 1, Sizeof.cl_mem, Pointer.to( memObjects[0] ) );
		CL.clSetKernelArg( kernel, 2, Sizeof.cl_mem, Pointer.to( memObjects[1] ) );

		// Set the work-item dimensions
		final long global_work_size[] = new long[] { n };
		final long local_work_size[] = new long[] { 1 };

		// Execute the kernel
		CL.clEnqueueNDRangeKernel( commandQueue, kernel, 1, null, global_work_size, local_work_size, 0, null, null );

		return new MatrixOpenCL( c, memObjects[1], this.rows, this.cols, this.paddedRows, this.paddedCols, new ISyncHostWithGPU() {

			@Override
			public void syncMemory() {
				// Read the output data
				CL.clEnqueueReadBuffer( commandQueue, memObjects[1], CL.CL_TRUE, 0, n * Sizeof.cl_float, dst, 0, null, null );
			}

			@Override
			public void free() {
				// Release kernel, program, and memory objects
				CL.clReleaseMemObject( memObjects[0] );
				CL.clReleaseKernel( kernel );
				CL.clReleaseCommandQueue( commandQueue );
			}
		} );
	}

	@Override
	public Matrix constantByElementSubtraction( final float val ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();
		this.writeToGPU();

		final int n = this.matrix.length;
		final float[] c = new float[n];
		final Pointer srcA = Pointer.to( new float[] { val } );
		final Pointer dst = Pointer.to( c );

		// Allocate the memory objects for the input- and output data
		final cl_mem memObjects[] = new cl_mem[2];
		memObjects[0] = CL.clCreateBuffer( MatrixOpenCL.GPU_CONTEXT, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float, srcA, null );
		memObjects[1] = CL.clCreateBuffer( MatrixOpenCL.GPU_CONTEXT, CL.CL_MEM_READ_WRITE, Sizeof.cl_float * n, null, null );

		// Create the kernel
		final cl_kernel kernel = CL.clCreateKernel( MatrixOpenCL.PROGRAM, "subtractConstantByElements", null );

		// Create a command-queue for the selected device
		final cl_command_queue commandQueue = CL.clCreateCommandQueueWithProperties( MatrixOpenCL.GPU_CONTEXT, MatrixOpenCL.GPU_DEVICE_ID, new cl_queue_properties(), null );

		// Set the arguments for the kernel
		CL.clSetKernelArg( kernel, 0, Sizeof.cl_mem, Pointer.to( memObjects[0] ) );
		CL.clSetKernelArg( kernel, 1, Sizeof.cl_mem, Pointer.to( this.matrix_P ) );
		CL.clSetKernelArg( kernel, 2, Sizeof.cl_mem, Pointer.to( memObjects[1] ) );

		// Set the work-item dimensions
		final long global_work_size[] = new long[] { n };
		final long local_work_size[] = new long[] { 1 };

		// Execute the kernel
		CL.clEnqueueNDRangeKernel( commandQueue, kernel, 1, null, global_work_size, local_work_size, 0, null, null );

		return new MatrixOpenCL( c, memObjects[1], this.rows, this.cols, this.paddedRows, this.paddedCols, new ISyncHostWithGPU() {

			@Override
			public void syncMemory() {
				// Read the output data
				CL.clEnqueueReadBuffer( commandQueue, memObjects[1], CL.CL_TRUE, 0, n * Sizeof.cl_float, dst, 0, null, null );
			}

			@Override
			public void free() {
				// Release kernel, program, and memory objects
				CL.clReleaseMemObject( memObjects[0] );
				CL.clReleaseKernel( kernel );
				CL.clReleaseCommandQueue( commandQueue );
			}
		} );
	}

	@Override
	public Matrix elementByConstantSubtraction( final float val ) {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();
		this.writeToGPU();

		final int n = this.matrix.length;
		final float[] c = new float[n];
		final Pointer srcB = Pointer.to( new float[] { val } );
		final Pointer dst = Pointer.to( c );

		// Allocate the memory objects for the input- and output data
		final cl_mem memObjects[] = new cl_mem[2];
		memObjects[0] = CL.clCreateBuffer( MatrixOpenCL.GPU_CONTEXT, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float, srcB, null );
		memObjects[1] = CL.clCreateBuffer( MatrixOpenCL.GPU_CONTEXT, CL.CL_MEM_READ_WRITE, Sizeof.cl_float * n, null, null );

		// Create the kernel
		final cl_kernel kernel = CL.clCreateKernel( MatrixOpenCL.PROGRAM, "subtractElementsByConstant", null );

		// Create a command-queue for the selected device
		final cl_command_queue commandQueue = CL.clCreateCommandQueueWithProperties( MatrixOpenCL.GPU_CONTEXT, MatrixOpenCL.GPU_DEVICE_ID, new cl_queue_properties(), null );

		// Set the arguments for the kernel
		CL.clSetKernelArg( kernel, 0, Sizeof.cl_mem, Pointer.to( this.matrix_P ) );
		CL.clSetKernelArg( kernel, 1, Sizeof.cl_mem, Pointer.to( memObjects[0] ) );
		CL.clSetKernelArg( kernel, 2, Sizeof.cl_mem, Pointer.to( memObjects[1] ) );

		// Set the work-item dimensions
		final long global_work_size[] = new long[] { n };
		final long local_work_size[] = new long[] { 1 };

		// Execute the kernel
		CL.clEnqueueNDRangeKernel( commandQueue, kernel, 1, null, global_work_size, local_work_size, 0, null, null );

		return new MatrixOpenCL( c, memObjects[1], this.rows, this.cols, this.paddedRows, this.paddedCols, new ISyncHostWithGPU() {

			@Override
			public void syncMemory() {
				// Read the output data
				CL.clEnqueueReadBuffer( commandQueue, memObjects[1], CL.CL_TRUE, 0, n * Sizeof.cl_float, dst, 0, null, null );
			}

			@Override
			public void free() {
				// Release kernel, program, and memory objects
				CL.clReleaseMemObject( memObjects[0] );
				CL.clReleaseKernel( kernel );
				CL.clReleaseCommandQueue( commandQueue );
			}
		} );
	}

	@Override
	public Matrix applySigmoid() {
		/* Must make sure this matrix is fit for computations */
		this.assertResources();
		this.writeToGPU();

		final int n = this.matrix.length;
		final float[] c = new float[n];
		final Pointer dst = Pointer.to( c );

		// Allocate the memory objects for the input- and output data
		final cl_mem memObjects[] = new cl_mem[1];
		memObjects[0] = CL.clCreateBuffer( MatrixOpenCL.GPU_CONTEXT, CL.CL_MEM_READ_WRITE, Sizeof.cl_float * n, null, null );

		// Create the kernel
		final cl_kernel kernel = CL.clCreateKernel( MatrixOpenCL.PROGRAM, "applySigmoid", null );

		// Create a command-queue for the selected device
		final cl_command_queue commandQueue = CL.clCreateCommandQueueWithProperties( MatrixOpenCL.GPU_CONTEXT, MatrixOpenCL.GPU_DEVICE_ID, new cl_queue_properties(), null );

		// Set the arguments for the kernel
		CL.clSetKernelArg( kernel, 0, Sizeof.cl_mem, Pointer.to( this.matrix_P ) );
		CL.clSetKernelArg( kernel, 1, Sizeof.cl_mem, Pointer.to( memObjects[0] ) );

		// Set the work-item dimensions
		final long global_work_size[] = new long[] { n };
		final long local_work_size[] = new long[] { 1 };

		// Execute the kernel
		CL.clEnqueueNDRangeKernel( commandQueue, kernel, 1, null, global_work_size, local_work_size, 0, null, null );

		return new MatrixOpenCL( c, memObjects[0], this.rows, this.cols, this.paddedRows, this.paddedCols, new ISyncHostWithGPU() {

			@Override
			public void syncMemory() {
				// Read the output data
				CL.clEnqueueReadBuffer( commandQueue, memObjects[0], CL.CL_TRUE, 0, n * Sizeof.cl_float, dst, 0, null, null );
			}

			@Override
			public void free() {
				// Release kernel, program, and memory objects
				CL.clReleaseKernel( kernel );
				CL.clReleaseCommandQueue( commandQueue );
			}
		} );
	}

	public static int padDimension( final int dim, final int pad ) {
		return dim % pad != 0 ? (dim / pad + 1) * pad : dim;
	}

	@Override
	public void free() {
		if ( !this.isFreed ) {
			if ( this.syncWithHost != null ) {
				this.syncWithHost.free();
			}
			this.syncWithHost = null;
			this.matrix = null;
			CL.clReleaseMemObject( this.matrix_P );
			this.matrix_P = null;
			this.isFreed = true;
		}
	}

	static {
		MatrixOpenCL.LOGGER.config( "Initalizing OpenCL..." );
		try {
			MatrixOpenCL.init();
			MatrixOpenCL.LOGGER.config( "Initalized OpenCl." );
		}
		catch ( final Exception e ) {
			MatrixOpenCL.LOGGER.config( "Initalizing OpenCl failed. " + e );
		}
	}

	public static class TileSizes {
		public static final int TSM = 128;
		public static final int TSN = 128;
		public static final int TSK = 16;
		public static final int WPTM = 8;
		public static final int WPTN = 8;
		public static final int RTSM = (TileSizes.TSM / TileSizes.WPTM);
		public static final int RTSN = (TileSizes.TSN / TileSizes.WPTN);
		public static final int LPTA = ((TileSizes.TSK * TileSizes.TSM) / (TileSizes.RTSM * TileSizes.RTSN));
		public static final int LPTB = ((TileSizes.TSK * TileSizes.TSN) / (TileSizes.RTSM * TileSizes.RTSN));
		public static final int TRANSPOSEX = 16;
		public static final int TRANSPOSEY = 16;
	}

	public static void init() throws Exception {
		/* The platform, device type and device number that will be used */
		final int platformIndex = 0;
		final long deviceType = CL.CL_DEVICE_TYPE_GPU;
		final int deviceIndex = 0;

		/* Enable exceptions and subsequently omit error checks */
		CL.setExceptionsEnabled( true );

		/* Obtain the number of platforms */
		final int[] numPlatformsArray = new int[1];
		CL.clGetPlatformIDs( 0, null, numPlatformsArray );
		final int numPlatforms = numPlatformsArray[0];

		/* Obtain a platform ID */
		final cl_platform_id[] platforms = new cl_platform_id[numPlatforms];
		CL.clGetPlatformIDs( platforms.length, platforms, null );
		final cl_platform_id platform = platforms[platformIndex];

		/* Initialize the context properties */
		final cl_context_properties contextProperties = new cl_context_properties();
		contextProperties.addProperty( CL.CL_CONTEXT_PLATFORM, platform );

		/* Obtain the number of devices for the platform */
		final int[] numDevicesArray = new int[1];
		CL.clGetDeviceIDs( platform, deviceType, 0, null, numDevicesArray );
		final int numDevices = numDevicesArray[0];

		/* Obtain a device ID */
		final cl_device_id[] devices = new cl_device_id[numDevices];
		CL.clGetDeviceIDs( platform, deviceType, numDevices, devices, null );
		MatrixOpenCL.GPU_DEVICE_ID = devices[deviceIndex];

		/* Create a context for the selected device */
		MatrixOpenCL.GPU_CONTEXT = CL.clCreateContext( contextProperties, 1, new cl_device_id[] { MatrixOpenCL.GPU_DEVICE_ID }, null, null, null );

		/* Print out all of the information */
		MatrixOpenCL.LOGGER.config( "OpenCL:" );
		MatrixOpenCL.LOGGER.config( "\tPlatform Name: " + MatrixOpenCL.getString( platform, CL.CL_PLATFORM_NAME ) );
		MatrixOpenCL.LOGGER.config( "\tPlatform Vendor: " + MatrixOpenCL.getString( platform, CL.CL_PLATFORM_VENDOR ) );
		MatrixOpenCL.LOGGER.config( "\tPlatform Version: " + MatrixOpenCL.getString( platform, CL.CL_PLATFORM_VERSION ) );
		MatrixOpenCL.LOGGER.config( "\tDevice Name: " + MatrixOpenCL.getString( MatrixOpenCL.GPU_DEVICE_ID, CL.CL_DEVICE_NAME ) );
		MatrixOpenCL.LOGGER.config( "\tDevice Vendor: " + MatrixOpenCL.getString( MatrixOpenCL.GPU_DEVICE_ID, CL.CL_DEVICE_VENDOR ) );
		MatrixOpenCL.LOGGER.config( "\tDevice Version: " + MatrixOpenCL.getString( MatrixOpenCL.GPU_DEVICE_ID, CL.CL_DEVICE_VERSION ) );

		MatrixOpenCL.PROGRAM = CL.clCreateProgramWithSource( MatrixOpenCL.GPU_CONTEXT, 1, new String[] { OpenCLResourceLoader.getFileContents( "openCLKernel.c" ) }, null, null );
		CL.clBuildProgram( MatrixOpenCL.PROGRAM, 0, null, null, null, null );
	}

	private static String getString( final cl_platform_id platform, final int paramName ) {
		// Obtain the length of the string that will be queried
		final long size[] = new long[1];
		CL.clGetPlatformInfo( platform, paramName, 0, null, size );

		// Create a buffer of the appropriate size and fill it with the info
		final byte buffer[] = new byte[(int) size[0]];
		CL.clGetPlatformInfo( platform, paramName, buffer.length, Pointer.to( buffer ), null );

		// Create a string from the buffer (excluding the trailing \0 byte)
		return new String( buffer, 0, buffer.length - 1 );
	}

	private static String getString( final cl_device_id device, final int paramName ) {
		// Obtain the length of the string that will be queried
		final long size[] = new long[1];
		CL.clGetDeviceInfo( device, paramName, 0, null, size );

		// Create a buffer of the appropriate size and fill it with the info
		final byte buffer[] = new byte[(int) size[0]];
		CL.clGetDeviceInfo( device, paramName, buffer.length, Pointer.to( buffer ), null );

		// Create a string from the buffer (excluding the trailing \0 byte)
		return new String( buffer, 0, buffer.length - 1 );
	}

	public static cl_mem createGPUBuffer( final float[] a ) {
		return CL.clCreateBuffer( MatrixOpenCL.GPU_CONTEXT, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * a.length, Pointer.to( a ), null );
	}
}
