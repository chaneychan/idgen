package me.ele.idgen.support.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.UUID;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

/**
 * 加密,编码
 * @author chaney.chan
 * @date 2015年6月17日
 */
public abstract class Coder {
    private static final String UTF8 = "UTF-8";
	private static final String KEY_SHA = "SHA-1";
	private static final String KEY_MD5 = "MD5";
	private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";  
    private static final String DEFAULT_URL_ENCODING = "UTF-8";  

	/**
	 * MAC算法可选以下多种算法
	 * 
	 * <pre>
	 * HmacMD5 
	 * HmacSHA1 
	 * HmacSHA256 
	 * HmacSHA384 
	 * HmacSHA512
	 * </pre>
	 */
	public static final String KEY_MAC = "HmacMD5";

	/**
	 * 初始化HMAC密钥
	 * 
	 * @return
	 * @throws Exception
	 */
	public static String initMacKey() throws Exception {
		KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_MAC);

		SecretKey secretKey = keyGenerator.generateKey();
		return base64Encode(secretKey.getEncoded());
	}

	public static String[] chars = new String[] { "a", "b", "c", "d", "e", "f",
			"g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s",
			"t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5",
			"6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I",
			"J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
			"W", "X", "Y", "Z" };

	/**
	 * 生成短uuid
	 * @return
	 */
	public static String generateShortUuid(UUID u) {
		StringBuffer shortBuffer = new StringBuffer();
		String uuid = u.toString().replace("-", "");
		for (int i = 0; i < 8; i++) {
			String str = uuid.substring(i * 4, i * 4 + 4);
			int x = Integer.parseInt(str, 16);
			shortBuffer.append(chars[x % 0x3E]);
		}
		return shortBuffer.toString();
	}
	 
	/**
	 * 压缩uuid
	 * @param uuid
	 * @return
	 */
    public static String compressedUUID(UUID uuid) {
        byte[] byUuid = new byte[16];
        long least = uuid.getLeastSignificantBits();
        long most = uuid.getMostSignificantBits();
        long2bytes(most, byUuid, 0);
        long2bytes(least, byUuid, 8);
        String compressUUID = base64UrlSafeEncode((byUuid));
        return compressUUID;
    }
 
    private static void long2bytes(long value, byte[] bytes, int offset) {
        for (int i = 7; i > -1; i--) {
            bytes[offset++] = (byte) ((value >> 8 * i) & 0xFF);
        }
    }


    @Deprecated
    public static String utf8Encoding(String value, String sourceCharsetName) {
        try {
            return new String(value.getBytes(sourceCharsetName), UTF8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * sha加密
     * @param data
     * @return
     * @throws IOException
     */
    public static byte[] getSHA1Digest(String data) throws IOException {
        byte[] bytes = null;
        try {
            MessageDigest md = MessageDigest.getInstance(KEY_SHA);
            bytes = md.digest(data.getBytes(UTF8));
        } catch (GeneralSecurityException gse) {
            throw new IOException(gse.getMessage());
        }
        return bytes;
    }

    /**
     * MD5加密
     * @param data
     * @return
     * @throws IOException
     */
    public static byte[] getMD5Digest(String data) throws IOException {
        byte[] bytes = null;
        try {
            MessageDigest md = MessageDigest.getInstance(KEY_MD5);
            bytes = md.digest(data.getBytes(UTF8));
        } catch (GeneralSecurityException gse) {
            throw new IOException(gse.getMessage());
        }
        return bytes;
    }

    /**
     * 二进制转十六进制字符串
     *
     * @param bytes
     * @return
     */
    public static String byte2hex(byte[] bytes) {
        StringBuilder sign = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                sign.append("0");
            }
            sign.append(hex.toUpperCase());
        }
        return sign.toString();
    }
    
	/**
	 * HMAC加密
	 * 
	 * @param data
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public static byte[] HmacEncode(byte[] data, String key) throws Exception {

		SecretKey secretKey = new SecretKeySpec(base64Decode(key), KEY_MAC);
		Mac mac = Mac.getInstance(secretKey.getAlgorithm());
		mac.init(secretKey);

		return mac.doFinal(data);
	}
  
    /** 
     * Hex编码, byte[]->String. 
     */  
    public static String hexEncode(byte[] input) {  
        return Hex.encodeHexString(input);  
    }  
  
    /** 
     * Hex解码, String->byte[]. 
     */  
    public static byte[] hexDecode(String input) {  
        try {  
            return Hex.decodeHex(input.toCharArray());  
        } catch (DecoderException e) {  
            throw new IllegalStateException("Hex Decoder exception", e);  
        }  
    }  
  
    /** 
     * Base64编码, byte[]->String. 
     */  
    public static String base64Encode(byte[] input) {  
        return Base64.encodeBase64String(input);  
    }  
  
    /** 
     * Base64编码, URL安全(将Base64中的URL非法字符'+'和'/'转为'-'和'_', 见RFC3548). 
     */  
    public static String base64UrlSafeEncode(byte[] input) {  
        return Base64.encodeBase64URLSafeString(input);  
    }  
  
    /** 
     * Base64解码, String->byte[]. 
     */  
    public static byte[] base64Decode(String input) {  
        return Base64.decodeBase64(input);  
    }  
  
    /** 
     * Base36(0_9A_Z)编码, long->String. 
     */  
    public static String base36Encode(long num) {  
        return alphabetEncode(num, 36);  
    }  
  
    /** 
     * Base36(0_9A_Z)解码, String->long. 
     */  
    public static long base36Decode(String str) {  
        return alphabetDecode(str, 36);  
    }  
  
    /** 
     * Base62(0_9A_Za_z)编码, long->String. 
     */  
    public static String base62Encode(long num) {  
        return alphabetEncode(num, 62);  
    }  
  
    /** 
     * Base62(0_9A_Za_z)解码, String->long. 
     */  
    public static long base62Decode(String str) {  
        return alphabetDecode(str, 62);  
    }  
  
    private static String alphabetEncode(long num, int base) {  
        num = Math.abs(num);  
        StringBuilder sb = new StringBuilder();  
        for (; num > 0; num /= base) {  
            sb.append(ALPHABET.charAt((int) (num % base)));  
        }  
  
        return sb.toString();  
    }  
  
    private static long alphabetDecode(String str, int base) {  
        long result = 0;  
        for (int i = 0; i < str.length(); i++) {  
            result += ALPHABET.indexOf(str.charAt(i)) * Math.pow(base, i);  
        }  
        return result;  
    }  
  
    /** 
     * URL 编码, Encode默认为UTF-8.  
     */  
    public static String urlEncode(String input) {  
        try {  
            return URLEncoder.encode(input, DEFAULT_URL_ENCODING);  
        } catch (UnsupportedEncodingException e) {  
            throw new RuntimeException("Unsupported Encoding Exception", e);  
        }  
    }  
  
    /** 
     * URL 解码, Encode默认为UTF-8.  
     */  
    public static String urlDecode(String input) {  
        try {  
            return URLDecoder.decode(input, DEFAULT_URL_ENCODING);  
        } catch (UnsupportedEncodingException e) {  
            throw new RuntimeException("Unsupported Encoding Exception", e);  
        }  
    }  
  
    
}
