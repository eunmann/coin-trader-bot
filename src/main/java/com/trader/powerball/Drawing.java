package com.trader.powerball;

import coin.trader.math.Matrix;

public class Drawing {

	private final int[] whiteNumbers;
	private int redNumber;
	private int year;
	private int month;
	private int day;

	public Drawing( final DrawingJSON drawingJSON ) {
		this.whiteNumbers = new int[5];
		this.setWinningNumbers( drawingJSON.winning_numbers );
		this.setDate( drawingJSON.draw_date );
	}

	public Drawing( final Matrix matrix ) {
		this.whiteNumbers = new int[5];

		int i = 0;

		for ( ; i < 5; i++ ) {
			this.whiteNumbers[i] = Math.round( PowerballHistoryAnalyzerNN.denormalizeValue( matrix.get( i, 0 ), 69 ) );
		}

		this.redNumber = Math.round( PowerballHistoryAnalyzerNN.denormalizeValue( matrix.get( i++, 0 ), 26 ) );

		this.year = Math.round( PowerballHistoryAnalyzerNN.denormalizeValue( matrix.get( i++, 0 ), 3 ) ) + 2015;
		this.month = Math.round( PowerballHistoryAnalyzerNN.denormalizeValue( matrix.get( i++, 0 ), 12 ) );
		this.day = Math.round( PowerballHistoryAnalyzerNN.denormalizeValue( matrix.get( i++, 0 ), 31 ) );
	}

	private void setWinningNumbers( final String winningNumbers ) {

		final String[] split = winningNumbers.split( " " );

		for ( int i = 0; i < 5; i++ ) {
			this.whiteNumbers[i] = Integer.parseInt( split[i] );
		}

		this.redNumber = Integer.parseInt( split[5] );
	}

	private void setDate( final String dateStr ) {
		final String[] split = dateStr.substring( 0, 10 ).split( "-" );

		this.year = Integer.parseInt( split[0] );
		this.month = Integer.parseInt( split[1] );
		this.day = Integer.parseInt( split[2] );
	}

	public int[] getWhiteNumbers() {
		final int[] copy = new int[5];
		System.arraycopy( this.whiteNumbers, 0, copy, 0, 5 );
		return copy;
	}

	public int getRedNumber() {
		return redNumber;
	}

	public int getYear() {
		return year;
	}

	public int getMonth() {
		return month;
	}

	public int getDay() {
		return day;
	}

	public Matrix toMatrix() {
		final Matrix matrix = Matrix.create( 9, 1 );
		int i = 0;

		for ( ; i < 5; i++ ) {
			/* Scale white ball */
			final float whiteNumberScaled = PowerballHistoryAnalyzerNN.normalizeValue( this.whiteNumbers[i], 69 );
			matrix.set( i, 0, whiteNumberScaled );
		}

		/* Scale red ball */
		final float redNumberScaled = PowerballHistoryAnalyzerNN.normalizeValue( this.redNumber, 26 );
		matrix.set( i++, 0, redNumberScaled );

		final float yearScaled = PowerballHistoryAnalyzerNN.normalizeValue( this.year - 2015, 3 );
		final float monthScaled = PowerballHistoryAnalyzerNN.normalizeValue( this.month, 12 );
		final float dayScaled = PowerballHistoryAnalyzerNN.normalizeValue( this.day, 31 );

		matrix.set( i++, 0, yearScaled );
		matrix.set( i++, 0, monthScaled );
		matrix.set( i++, 0, dayScaled );

		return matrix;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

		sb.append( "[ " );

		for ( int i = 0; i < 5; i++ ) {
			sb.append( this.whiteNumbers[i] );
			sb.append( ", " );
		}

		sb.append( this.redNumber );
		sb.append( " ] " );

		sb.append( this.year );
		sb.append( "-" );
		sb.append( this.month );
		sb.append( "-" );
		sb.append( this.day );

		return sb.toString();
	}

	public static class DrawingJSON {
		String draw_date;
		String winning_numbers;
		String multiplier;
	}
}
