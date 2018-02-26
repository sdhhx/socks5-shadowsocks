package cc.litstar.encrypt;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.litstar.encrypt.impl.AesCrypt;
import cc.litstar.encrypt.impl.BlowFishCrypt;
import cc.litstar.encrypt.impl.CamelliaCrypt;
import cc.litstar.encrypt.impl.Chacha20Crypt;
import cc.litstar.encrypt.impl.Rc4Md5Crypt;
import cc.litstar.encrypt.impl.SeedCrypt;

public class CryptFactory {

	private static Logger logger = LoggerFactory.getLogger(CryptFactory.class);

	private static Map<String, String> crypts = new HashMap<String, String>();

	static {
		crypts.putAll(AesCrypt.getCiphers());
		crypts.putAll(CamelliaCrypt.getCiphers());
		crypts.putAll(BlowFishCrypt.getCiphers());
		crypts.putAll(SeedCrypt.getCiphers());
		crypts.putAll(Chacha20Crypt.getCiphers());
		crypts.putAll(Rc4Md5Crypt.getCiphers());
	}

	public static ICrypt get(String name, String password) {
		String className = crypts.get(name);
		if (className == null) {
			return null;
		}

		try {
			Class<?> clazz = Class.forName(className);
			Constructor<?> constructor = clazz.getConstructor(String.class, String.class);
			return (ICrypt) constructor.newInstance(name, password);
		} catch (Exception e) {
			logger.error("get crypt error", e);
		}

		return null;
	}
}