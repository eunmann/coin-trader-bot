#define BLOCK_DIM 16

#define TSM 128                            // The tile-size in dimension M
#define TSN 128                            // The tile-size in dimension N
#define TSK 16                             // The tile-size in dimension K
#define WPTM 8                             // The amount of work-per-thread in dimension M
#define WPTN 8                             // The amount of work-per-thread in dimension N
#define RTSM (TSM / WPTM)                  // The reduced tile-size in dimension M (== number of threads)
#define RTSN (TSN / WPTN)                  // The reduced tile-size in dimension N (== number of threads)
#define LPTA ((TSK * WPTM * WPTN) / (TSN)) // The amount of loads-per-thread for A
#define LPTB ((TSK * WPTM * WPTN) / (TSM)) // The amount of loads-per-thread for B
#define MOD2(x, y) ((x) % (y))
#define DIV2(x, y) ((x) / (y))
#define TRANSPOSEX 16
#define TRANSPOSEY 16

/*
    Element by element addition
*/
__kernel void addElements(__global const float *a, __global const float *b, __global float *c)
{
    int gid = get_global_id(0);
    c[gid] = a[gid] + b[gid];
}

/*
    Element by element multiplication
*/
__kernel void multiplyElementsByConstant(__global const float *a, __global const float *b, __global float *c)
{
    float constVal = b[0];
    int gid = get_global_id(0);
    c[gid] = a[gid] * constVal;
}

/*
    Transpose the matrix
*/
__kernel void transpose(__global const float *input, __global float *output, __global const int *rowsPtr, __global const int *colsPtr)
{
    const int P = rowsPtr[0];
    const int Q = colsPtr[0];

    // Thread identifiers
    const int tx = get_local_id(0);
    const int ty = get_local_id(1);
    const int ID0 = get_group_id(0) * TRANSPOSEX + tx; // 0..P
    const int ID1 = get_group_id(1) * TRANSPOSEY + ty; // 0..Q

    // Set-up the local memory for shuffling
    __local float buffer[TRANSPOSEX][TRANSPOSEY];

    // Swap the x and y coordinates to perform the rotation (coalesced)
    if (ID0 < P && ID1 < Q)
    {
        buffer[ty][tx] = input[ID1 * P + ID0];
    }

    // Synchronise all threads
    barrier(CLK_LOCAL_MEM_FENCE);

    // We don't have to swap the x and y thread indices here,
    // because that's already done in the local memory
    const int newID0 = get_group_id(1) * TRANSPOSEY + tx;
    const int newID1 = get_group_id(0) * TRANSPOSEX + ty;

    // Store the transposed result (coalesced)
    if (newID0 < Q && newID1 < P)
    {
        output[newID1 * Q + newID0] = buffer[tx][ty];
    }
}

/*
    Constant by element value subtraction
*/
__kernel void subtractConstantByElements(__global const float *a, __global const float *b, __global float *c)
{
    int gid = get_global_id(0);
    float constVal = a[0];
    c[gid] = constVal - b[gid];
}

/*
    Matrix by Matrix Multiplication
*/
__kernel void matrixMultiplication(__global const float *A, __global const float *B, __global float *C, __global const int *MPtr, __global const int *KPtr, __global const int *NPtr)
{
    const int M = MPtr[0];
    const int K = KPtr[0];
    const int N = NPtr[0];

    // Thread identifiers
    const int tidm = get_local_id(0);          // Local row ID (max: TSM/WPTM == RTSM)
    const int tidn = get_local_id(1);          // Local col ID (max: TSN/WPTN == RTSN)
    const int offsetM = TSM * get_group_id(0); // Work-group offset
    const int offsetN = TSN * get_group_id(1); // Work-group offset

    // Local memory to fit a tile of A and B
    __local float Asub[TSK][TSM];
    __local float Bsub[TSN][TSK + 2];

    // Allocate register space
    float Areg;
    float Breg[WPTN];
    float acc[WPTM][WPTN];

// Initialise the accumulation registers
#pragma unroll
    for (int wm = 0; wm < WPTM; wm++)
    {
#pragma unroll
        for (int wn = 0; wn < WPTN; wn++)
        {
            acc[wm][wn] = 0.0f;
        }
    }

    // Loop over all tiles
    const int numTiles = K / TSK;
    int t = 0;
    do
    {

// Load one tile of A and B into local memory
#pragma unroll
        for (int la = 0; la < LPTA; la++)
        {
            int tid = tidn * RTSM + tidm;
            volatile int id = la * RTSN * RTSM + tid;
            int row = MOD2(id, TSM);
            int col = DIV2(id, TSM);
            int tiledIndex = TSK * t + col;
            Asub[col][row] = A[tiledIndex * M + offsetM + row];
            Bsub[row][col] = B[tiledIndex * N + offsetN + row];
        }

        // Synchronise to make sure the tile is loaded
        barrier(CLK_LOCAL_MEM_FENCE);

        // Loop over the values of a single tile
        for (int k = 0; k < TSK; k++)
        {

// Cache the values of Bsub in registers
#pragma unroll
            for (int wn = 0; wn < WPTN; wn++)
            {
                int col = tidn + wn * RTSN;
                Breg[wn] = Bsub[col][k];
            }

// Perform the computation
#pragma unroll
            for (int wm = 0; wm < WPTM; wm++)
            {
                int row = tidm + wm * RTSM;
                Areg = Asub[k][row];
#pragma unroll
                for (int wn = 0; wn < WPTN; wn++)
                {
                    acc[wm][wn] += Areg * Breg[wn];
                }
            }
        }

        // Synchronise before loading the next tile
        barrier(CLK_LOCAL_MEM_FENCE);

        // Next tile
        t++;
    } while (t < numTiles);

// Store the final results in C
#pragma unroll
    for (int wm = 0; wm < WPTM; wm++)
    {
        int globalRow = offsetM + tidm + wm * RTSM;
#pragma unroll
        for (int wn = 0; wn < WPTN; wn++)
        {
            int globalCol = offsetN + tidn + wn * RTSN;
            C[globalCol * M + globalRow] = acc[wm][wn];
        }
    }
}

/*
    Element by constant value subtraction
*/
__kernel void subtractElementsByConstant(__global const float *a, __global const float *b, __global float *c)
{
    int gid = get_global_id(0);
    float constVal = b[0];
    c[gid] = a[gid] - constVal;
}

/*
    Element by element subtraction
*/
__kernel void subtractElements(__global const float *a, __global const float *b, __global float *c)
{
    int gid = get_global_id(0);
    c[gid] = a[gid] - b[gid];
}

/*
    Element by element multiplication
*/
__kernel void multiplyElements(__global const float *a, __global const float *b, __global float *c)
{
    int gid = get_global_id(0);
    c[gid] = a[gid] * b[gid];
}

/*
    Sigmoid applied to each element
*/
__kernel void applySigmoid(__global const float *a, __global float *b)
{
    int gid = get_global_id(0);
    b[gid] = (1.0f / (1.0f + pow(M_E_F, -1.0f * a[gid])));
}