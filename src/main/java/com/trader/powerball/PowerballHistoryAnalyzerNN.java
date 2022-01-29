package com.trader.powerball;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.google.gson.Gson;
import com.trader.powerball.Drawing.DrawingJSON;

import coin.trader.Main;
import coin.trader.math.Matrix;
import coin.trader.neuralnetwork.NeuralNetwork;

public class PowerballHistoryAnalyzerNN {

	public PowerballHistoryAnalyzerNN() {
	}

	public Matrix analyze( final String startDate, final String endDate ) {
		final Drawing[] drawings = PowerballHistoryAnalyzerNN.getDrawings( startDate, endDate );

		final NeuralNetwork neuralNetwork = new NeuralNetwork( 13, 9, 9, 5, 0.01f );

		final Matrix[] matrices = toMatrices( drawings );

		for ( int e = 0; e < 5; e++ ) {

			if ( e % 50 == 0 ) {
				Main.LOGGER.info( "e: " + e );
			}

			for ( int i = matrices.length - 1; i > 0; i-- ) {
				neuralNetwork.train( matrices[i], drawings[i - 1].toMatrix() );
			}
		}

		final Matrix output = neuralNetwork.query( matrices[0] );
		return output;
	}

	private Matrix[] toMatrices( final Drawing[] drawings ) {

		final Matrix[] matrices = new Matrix[drawings.length];

		for ( int i = 0; i < drawings.length; i++ ) {
			final Drawing drawing = drawings[i];
			final Matrix drawingMatrix = drawing.toMatrix();

			final Matrix matrix = Matrix.create( 13, 1 );

			int j = 0;
			for ( ; j < drawingMatrix.getRows(); j++ ) {
				matrix.set( j, 0, drawingMatrix.get( j, 0 ) );
			}

			final int[] whiteNumbers = drawing.getWhiteNumbers();
			for ( int k = 0; k < whiteNumbers.length - 1; k++ ) {
				matrix.set( j++, 0, normalizeValue( whiteNumbers[k + 1] - whiteNumbers[k], 68 ) );
			}

			matrices[i] = matrix;
		}

		return matrices;
	}

	static Drawing[] getDrawings( final String startDate, final String endDate ) {
		final StringBuilder sb = new StringBuilder();

		try ( final FileReader fr = new FileReader( new File( "C:\\workdir\\numbers.json" ) ); final BufferedReader br = new BufferedReader( fr ); ) {
			String str = br.readLine();

			while ( str != null ) {
				sb.append( str );
				str = br.readLine();
			}
		}
		catch ( final FileNotFoundException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch ( final IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		final Gson gson = new Gson();
		final DrawingJSON[] drawingsJSON = gson.fromJson( sb.toString(), DrawingJSON[].class );

		int startIndex = 0;
		int endIndex = 0;

		for ( int i = 0; i < drawingsJSON.length; i++ ) {
			final DrawingJSON drawing = drawingsJSON[i];

			if ( drawing.draw_date.startsWith( startDate ) ) {
				startIndex = i;
				break;
			}
			else if ( drawing.draw_date.startsWith( endDate ) ) {
				endIndex = i;
			}
		}

		final Drawing[] drawings = new Drawing[startIndex - endIndex];

		for ( int i = endIndex, j = 0; i < startIndex; i++, j++ ) {
			drawings[j] = new Drawing( drawingsJSON[i] );
		}

		return drawings;
	}

	public static float normalizeValue( final double val, final float denom ) {
		return (float) ((val / denom) * 0.98 + 0.01);
	}

	public static float denormalizeValue( final float val, final float denom ) {
		return denom * ((val - 0.01f) / 0.98f);
	}

}
