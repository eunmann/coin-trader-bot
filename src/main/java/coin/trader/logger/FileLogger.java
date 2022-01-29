package coin.trader.logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import coin.trader.Info;

/*
 * The current way loggers are created makes this logger not output from the
 * Info class.
 */
public class FileLogger implements ILog {
	private final static File file = FileLogger.getFile();
	private final static Object FILE_LOCK = new Object();

	public FileLogger() {
	}

	@Override
	public void log( final LogEvent logEvent ) {
		synchronized ( FileLogger.FILE_LOCK ) {
			FileWriter fileWriter = null;
			BufferedWriter bufferWriter = null;

			try {
				fileWriter = new FileWriter( FileLogger.file.getAbsolutePath(), true );
				bufferWriter = new BufferedWriter( fileWriter );
				bufferWriter.write( ILog.formatString( logEvent ) + "\n" );
				bufferWriter.flush();
				fileWriter.flush();
			}
			catch ( final IOException e ) {
				System.err.println( "Error writing to log file " + FileLogger.class.getSimpleName() + ". Could not create file." );
			}
			finally {
				try {
					if ( bufferWriter != null ) {
						bufferWriter.close();
					}

					if ( fileWriter != null ) {
						fileWriter.close();
					}
				}
				catch ( final Exception e ) {
					/* Swallow this error */
				}
			}
		}
	}

	private static File getFile() {
		/* Create the log file if it already doesn't exist */
		final File file = new File( Info.LOG_PATH );

		if ( !file.exists() ) {
			try {
				file.createNewFile();
			}
			catch ( final IOException e ) {
				System.err.println( "Error starting " + FileLogger.class.getSimpleName() + ". Could not create file." );
			}
		}

		return file;
	}

	@Override
	public String toString() {
		return FileLogger.class.getSimpleName();
	}
}
