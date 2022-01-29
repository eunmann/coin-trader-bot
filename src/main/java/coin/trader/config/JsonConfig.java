package coin.trader.config;

import java.io.FileReader;
import java.io.Serializable;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import coin.trader.Info;
import coin.trader.logger.ILog;
import coin.trader.logger.Logger;

public class JsonConfig {
	private JsonConfig() {
	}

	static boolean applyConfig() {
		try {
			final Type type = new TypeToken<JsonConfigStructure>() {
			}.getType();
			final Gson gson = new Gson();
			final JsonReader reader = new JsonReader( new FileReader( Info.CONFIG_PATH ) );
			final JsonConfigStructure jsonConfig = gson.fromJson( reader, type );

			/* Logger */
			Config.LoggerConfig.LOGGER_IMPL.setValue( ILog.fromString( jsonConfig.LoggerConfig.LOGGER_IMPL ) );
			Config.LoggerConfig.LOGGING_LEVEL.setValue( Logger.Level.valueOf( jsonConfig.LoggerConfig.LOGGING_LEVEL ) );

			/* Bittrex Config */
			Config.BittrexConfig.API_KEY_PUBLIC.setValue( jsonConfig.BittrexConfig.API_KEY_PUBLIC );
			Config.BittrexConfig.API_KEY_PRIVATE.setValue( jsonConfig.BittrexConfig.API_KEY_PRIVATE );

			/* Poloniex Config */
			Config.PoloniexConfig.API_KEY_PUBLIC.setValue( jsonConfig.PoloniexConfig.API_KEY_PUBLIC );
			Config.PoloniexConfig.API_KEY_PRIVATE.setValue( jsonConfig.PoloniexConfig.API_KEY_PRIVATE );
		}
		catch ( final Exception e ) {
			return false;
		}

		return true;
	}

	private static class JsonConfigStructure implements Serializable {
		private static final long serialVersionUID = -8908109157495685119L;
		JsonLoggerStructure LoggerConfig = new JsonLoggerStructure();
		BittrexConfigStructure BittrexConfig = new BittrexConfigStructure();
		PoloniexConfigStructure PoloniexConfig = new PoloniexConfigStructure();

		private class JsonLoggerStructure implements Serializable {
			private static final long serialVersionUID = 9143294577035907647L;

			String LOGGER_IMPL = "ConsoleLogger";
			String LOGGING_LEVEL = "INFO";
		}

		private class BittrexConfigStructure implements Serializable {
			private static final long serialVersionUID = 1197387554888392901L;
			String API_KEY_PUBLIC = "";
			String API_KEY_PRIVATE = "";
		}

		private class PoloniexConfigStructure implements Serializable {
			private static final long serialVersionUID = 4011399195362330539L;
			String API_KEY_PUBLIC = "";
			String API_KEY_PRIVATE = "";
		}
	}

	static String getTemplateAsString() {
		final JsonConfigStructure config = new JsonConfigStructure();
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson( config );
	}
}
