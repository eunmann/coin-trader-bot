package coin.trader.connection;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import coin.trader.config.Config;

public class HTTPRequest {
	public static String sendHTTPRequest( final String urlStr ) throws IOException {
		final URL url = new URL( urlStr );
		final HttpURLConnection con = (HttpURLConnection) url.openConnection();

		/* GET */
		con.setRequestMethod( "GET" );

		/* Add headers */
		con.setRequestProperty( "User-Agent", "Mozilla/5.0" );

		/* Send and wait for response */
		final int responseCode = con.getResponseCode();

		if ( responseCode == 200 ) {
			/* Read response */
			final BufferedReader in = new BufferedReader( new InputStreamReader( con.getInputStream() ) );
			String inputLine;
			final StringBuilder response = new StringBuilder();

			while ( (inputLine = in.readLine()) != null ) {
				response.append( inputLine );
			}

			in.close();

			return response.toString();
		}
		else {
			return "";
		}
	}

	/**
	 * For Bittrex
	 * 
	 * @param urlStr
	 * @param secretBytes
	 * @return
	 * @throws Exception
	 */
	public static String sendHTTPRequestWithHMAC_SHA_512( final String urlStr, final byte[] secretBytes ) throws Exception {
		final URL url = new URL( urlStr );
		final HttpURLConnection con = (HttpURLConnection) url.openConnection();

		/* GET */
		con.setRequestMethod( "GET" );

		/* Add headers */
		con.setRequestProperty( "User-Agent", "Mozilla/5.0" );

		/* Calculate the HMAC_512 hash */
		final Mac hmac_sha_512 = Mac.getInstance( "HmacSHA512" );
		final SecretKeySpec keySpec = new SecretKeySpec( secretBytes, "HmacSHA512" );
		hmac_sha_512.init( keySpec );
		final byte[] bytes = hmac_sha_512.doFinal( urlStr.getBytes( "UTF-8" ) );

		/* Convert byte array to a hex string */
		final StringBuilder sb = new StringBuilder();
		for ( int i = 0, len = bytes.length; i < len; i++ ) {
			sb.append( String.format( "%02x", bytes[i] ) );
		}

		/* Add the signature to the headers */
		con.setRequestProperty( "apisign", sb.toString() );

		/* Send and wait for response */
		final int responseCode = con.getResponseCode();

		if ( responseCode == 200 ) {
			/* Read response */
			final BufferedReader in = new BufferedReader( new InputStreamReader( con.getInputStream() ) );
			String inputLine;
			final StringBuilder response = new StringBuilder();

			while ( (inputLine = in.readLine()) != null ) {
				response.append( inputLine );
			}

			in.close();

			return response.toString();
		}
		else {
			return "";
		}
	}

	/**
	 * For Poloniex
	 * 
	 * @param urlStr
	 * @param secretBytes
	 * @return
	 * @throws Exception
	 */
	public static String sendHTTPRequestWithHMAC_SHA_512POST( final String urlStr, final String postParams, final byte[] secretBytes ) throws Exception {
		final URL url = new URL( urlStr );
		final HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setDoOutput( true );

		/* GET */
		con.setRequestMethod( "POST" );

		/* Add headers */
		con.setRequestProperty( "User-Agent", "Mozilla/5.0" );

		/* Calculate the HMAC_512 hash */
		final Mac hmac_sha_512 = Mac.getInstance( "HmacSHA512" );
		final SecretKeySpec keySpec = new SecretKeySpec( secretBytes, "HmacSHA512" );
		hmac_sha_512.init( keySpec );
		final byte[] bytes = hmac_sha_512.doFinal( postParams.getBytes( "UTF-8" ) );

		/* Convert byte array to a hex string */
		final StringBuilder sb = new StringBuilder();
		for ( int i = 0, len = bytes.length; i < len; i++ ) {
			sb.append( String.format( "%02x", bytes[i] ) );
		}

		/* Add the signature to the headers */
		con.setRequestProperty( "Key", Config.PoloniexConfig.API_KEY_PUBLIC.getValue() );
		con.setRequestProperty( "Sign", sb.toString() );

		/* Write the POST parameters */
		try ( final DataOutputStream wr = new DataOutputStream( con.getOutputStream() ) ) {
			wr.write( postParams.toString().getBytes() );
		}

		/* Send and wait for response */
		final int responseCode = con.getResponseCode();

		if ( responseCode == 200 ) {
			/* Read response */
			final BufferedReader in = new BufferedReader( new InputStreamReader( con.getInputStream() ) );
			String inputLine;
			final StringBuilder response = new StringBuilder();

			while ( (inputLine = in.readLine()) != null ) {
				response.append( inputLine );
			}

			in.close();

			return response.toString();
		}
		else {
			return "";
		}
	}
}
