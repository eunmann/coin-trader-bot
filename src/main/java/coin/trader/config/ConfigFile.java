package coin.trader.config;

public interface ConfigFile {
	public static boolean applyConfig() {
		return JsonConfig.applyConfig();
	}

	public static String getTemplateAsString() {
		return JsonConfig.getTemplateAsString();
	}
}
