package me.ele.idgen.support.http;

import me.ele.elog.Log;
import me.ele.elog.LogFactory;
import me.ele.idgen.support.utils.AESCoder;
import me.ele.idgen.support.utils.Coder;

import org.apache.commons.lang3.StringUtils;

/**
 * 生成user的token
 *
 */
public class UserToken {

	private static Log log = LogFactory.getLog(UserToken.class);
	private final static String SPLITSTR = "#";
	private final static String USERNAME = "admin";
	private final static String PWD = "eleme789";
	private final static int SESSION_TIME = 30*60*1000;
	
	
	public static boolean checkUserPwd(String userNameParams,String pwdParams){
		if(StringUtils.isNotBlank(userNameParams) && StringUtils.isNotBlank(pwdParams)
				&& userNameParams.equals(USERNAME) && pwdParams.equals(PWD)){
			return true;
		}
		return false;
	}
	
	
	public static String getUserToken(String userName,String pwd){
		log.info("getUserToken: userName: {}, password: {}" ,userName,pwd);
		StringBuffer sb = new StringBuffer();
		sb.append(userName);
		sb.append(SPLITSTR);
		sb.append(pwd);
		sb.append(SPLITSTR);
		sb.append(System.currentTimeMillis());
		String encode = "";
		try {
			encode = Coder.hexEncode(AESCoder.encrypt(sb.toString()));
		} catch (Exception e) {
		}
		return encode;
	}
	
	public static boolean checkUserToken(String token){
		try {
			String str = AESCoder.decrypt(Coder.hexDecode(token));
			if(StringUtils.isNotBlank(str)){
				if(str.indexOf(SPLITSTR)!=-1){
					String [] s = str.split(SPLITSTR);
					if(s[0].equals(USERNAME) && s[1].equals(PWD)
							&& (System.currentTimeMillis()-Long.parseLong(s[2])<SESSION_TIME)){
						return true;
					}
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			return false;
		}
		return false;
	}
	
	public static void main(String[] args) throws Exception {
		String ds = getUserToken(USERNAME, PWD);
		System.out.println(ds);
		String ddd = AESCoder.decrypt(Coder.hexDecode(ds));
		System.out.println(ddd);
	}
}
