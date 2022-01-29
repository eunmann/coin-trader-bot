package coin.trader.math.opencl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class OpenCLResourceLoader {
	public static OpenCLResourceLoader LOADER = new OpenCLResourceLoader();

	public static String getFileContents( final String filename ) throws Exception {
		final InputStream in = OpenCLResourceLoader.LOADER.getClass().getResourceAsStream( "OpenCLSource/" + filename );
		final BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
		final StringBuilder sb = new StringBuilder();

		while ( true ) {
			final String str = reader.readLine();

			if ( str != null ) {
				sb.append( str );
				sb.append( '\n' );
			}
			else {
				break;
			}
		}

		return sb.toString();
	}
}
