package coin.trader.config;

import java.io.Serializable;

public class ConfigItem<T> implements Serializable {
	private static final long serialVersionUID = -770471661504153571L;
	private final String name;
	private T value;
	private final SecurityLevel securityLevel;

	ConfigItem( final String name, final T value, final SecurityLevel securityLevel ) {
		this.name = name;
		this.value = value;
		this.securityLevel = securityLevel;
	}

	void setValue( final T value ) {
		this.value = value;
	}

	public String getName() {
		return this.name;
	}

	public T getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.name + " = " + (this.securityLevel.equals( SecurityLevel.PUBLIC ) ? this.value : "PRIVATE_INFORMATION");
	}

	enum SecurityLevel {
		PUBLIC,
		PRIVATE,
		SECRET
	}
}
