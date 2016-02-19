package me.ele.idgen.support.utils;

import java.security.Key;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


/**
 * DES安全编码组件
 * 
 * <pre>
 * 支持 DES、DESede(TripleDES,就是3DES)、AES、Blowfish、RC2、RC4(ARCFOUR)
 * DES          		key size must be equal to 56
 * DESede(TripleDES) 	key size must be equal to 112 or 168
 * AES          		key size must be equal to 128, 192 or 256,but 192 and 256 bits may not be available
 * Blowfish     		key size must be multiple of 8, and can only range from 32 to 448 (inclusive)
 * RC2          		key size must be between 40 and 1024 bits
 * RC4(ARCFOUR) 		key size must be between 40 and 1024 bits
 * 具体内容 需要关注 JDK Document http://.../docs/technotes/guides/security/SunProviders.html
 * </pre>
 * 
 * @author chaney.chan
 * @date 2015年6月17日
 */
public abstract class AESCoder extends Coder {
	/**
	 * ALGORITHM 算法 <br>
	 * 可替换为以下任意一种算法，同时key值的size相应改变。
	 * 
	 * <pre>
	 * DES          		key size must be equal to 56
	 * DESede(TripleDES) 	key size must be equal to 112 or 168
	 * AES          		key size must be equal to 128, 192 or 256,but 192 and 256 bits may not be available
	 * Blowfish     		key size must be multiple of 8, and can only range from 32 to 448 (inclusive)
	 * RC2          		key size must be between 40 and 1024 bits
	 * RC4(ARCFOUR) 		key size must be between 40 and 1024 bits
	 * </pre>
	 * 
	 * 在Key toKey(byte[] key)方法中使用下述代码
	 * <code>SecretKey secretKey = new SecretKeySpec(key, ALGORITHM);</code> 替换
	 * <code>
	 * DESKeySpec dks = new DESKeySpec(key);
	 * SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
	 * SecretKey secretKey = keyFactory.generateSecret(dks);
	 * </code>
	 */
	public static final String ALGORITHM = "AES";
	
	private static String seed = "chaney.chan";
	
	private static String key;


	/**
	 * 生成密钥
	 * 
	 * @return
	 * @throws Exception
	 */
	static{
		try {
			key = initKey(seed);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 转换密钥<br>
	 * 
	 * @param key
	 * @return
	 * @throws Exception
	 */
	private static Key toKey(byte[] key) throws Exception {
//		DESKeySpec dks = new DESKeySpec(key);
//		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
//		SecretKey secretKey = keyFactory.generateSecret(dks);

		// 当使用其他对称加密算法时，如AES、Blowfish等算法时，用下述代码替换上述三行代码
		 SecretKey secretKey = new SecretKeySpec(key, ALGORITHM);

		return secretKey;
		
//		KeyGenerator _generator=KeyGenerator.getInstance(ALGORITHM);
//		SecureRandom secureRandom=SecureRandom.getInstance("SHA1PRNG");
//		secureRandom.setSeed(key);
//		_generator.init(128,secureRandom);
//		return _generator.generateKey();
	}

	/**
	 * 解密
	 * 
	 * @param data
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public static String decrypt(byte[] data) throws Exception {
		Key k = toKey(base64Decode(key));

		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, k);

		return new String(cipher.doFinal(data),"UTF-8");
	}

	/**
	 * 加密
	 * 
	 * @param data
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public static byte[] encrypt(String data) throws Exception {
		Key k = toKey(base64Decode(key));
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, k);

		return cipher.doFinal(data.getBytes());
	}
	
	/**
	 * 生成密钥
	 * 
	 * @param seed
	 * @return
	 * @throws Exception
	 */
	public static String initKey(String seed) throws Exception {
		SecureRandom secureRandom = null;

//		if (seed != null) {
//			secureRandom = new SecureRandom(base64Decode(seed));
//		} else {
//			secureRandom = new SecureRandom();
//		}
		secureRandom = SecureRandom.getInstance("SHA1PRNG");

		KeyGenerator kg = KeyGenerator.getInstance(ALGORITHM);
		secureRandom.setSeed(seed.getBytes());
		
		kg.init(secureRandom);

		SecretKey secretKey = kg.generateKey();

		return base64Encode(secretKey.getEncoded());
	}
}
