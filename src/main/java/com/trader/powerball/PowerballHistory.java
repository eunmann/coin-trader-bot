package com.trader.powerball;

import java.util.Arrays;
import java.util.Comparator;

public class PowerballHistory {
	private final Drawing[] drawings;

	public PowerballHistory( final String startDate, final String endDate ) {
		this.drawings = PowerballHistoryAnalyzerNN.getDrawings( startDate, endDate );
	}

	public NumberPair[] getSortedByWinningWhiteNumbersDecending() {
		final NumberPair[] numberPairs = new NumberPair[69];

		for ( int i = 0; i < numberPairs.length; i++ ) {
			numberPairs[i] = new NumberPair( i + 1, 0 );
		}

		for ( final Drawing drawing : this.drawings ) {
			for ( final int number : drawing.getWhiteNumbers() ) {
				numberPairs[number - 1].increment();
			}
		}

		Arrays.sort( numberPairs, new Comparator<NumberPair>() {

			@Override
			public int compare( NumberPair o1, NumberPair o2 ) {

				return o2.getCount() - o1.getCount();
			}
		} );

		return numberPairs;
	}

	public NumberPair[] getSortedByWinningWhiteNumbersAcending() {
		final NumberPair[] numberPairs = new NumberPair[69];

		for ( int i = 0; i < numberPairs.length; i++ ) {
			numberPairs[i] = new NumberPair( i + 1, 0 );
		}

		for ( final Drawing drawing : this.drawings ) {
			for ( final int number : drawing.getWhiteNumbers() ) {
				numberPairs[number - 1].increment();
			}
		}

		Arrays.sort( numberPairs, new Comparator<NumberPair>() {

			@Override
			public int compare( NumberPair o1, NumberPair o2 ) {

				return o1.getCount() - o2.getCount();
			}
		} );

		return numberPairs;
	}

	public NumberPair[] getSortedByWinningRedNumbersDecending() {
		final NumberPair[] numberPairs = new NumberPair[26];

		for ( int i = 0; i < numberPairs.length; i++ ) {
			numberPairs[i] = new NumberPair( i + 1, 0 );
		}

		for ( final Drawing drawing : this.drawings ) {
			final int number = drawing.getRedNumber();
			numberPairs[number - 1].increment();
		}

		Arrays.sort( numberPairs, new Comparator<NumberPair>() {
			@Override
			public int compare( NumberPair o1, NumberPair o2 ) {

				return o2.getCount() - o1.getCount();
			}
		} );

		return numberPairs;
	}

	public NumberPair[] getSortedByWinningRedNumbersAcending() {
		final NumberPair[] numberPairs = new NumberPair[26];

		for ( int i = 0; i < numberPairs.length; i++ ) {
			numberPairs[i] = new NumberPair( i + 1, 0 );
		}

		for ( final Drawing drawing : this.drawings ) {
			final int number = drawing.getRedNumber();
			numberPairs[number - 1].increment();
		}

		Arrays.sort( numberPairs, new Comparator<NumberPair>() {
			@Override
			public int compare( NumberPair o1, NumberPair o2 ) {

				return o1.getCount() - o2.getCount();
			}
		} );

		return numberPairs;
	}

	public static class NumberPair {
		private final int number;
		private int count;

		public NumberPair( final int number, final int count ) {
			this.number = number;
			this.count = count;
		}

		public int getNumber() {
			return number;
		}

		public int getCount() {
			return count;
		}

		public void increment() {
			this.count++;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append( "[ " ).append( this.number ).append( ", " ).append( this.count ).append( " ]" );
			return sb.toString();
		}
	}
}
