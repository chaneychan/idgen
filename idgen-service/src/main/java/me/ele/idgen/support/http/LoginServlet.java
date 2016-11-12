package me.ele.idgen.support.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @Description: 登录servlet
 * @author shaoqunxi
 * @date 2015年9月16日
 */
public class LoginServlet extends HttpServlet {

	private static final long serialVersionUID = 5959137829598707522L;
	private static final Logger logger = LoggerFactory.getLogger(LoginServlet.class);
	
	
	public void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String email = request.getParameter("email");
		String pwd = request.getParameter("pwd");
		logger.info("Enter login: email : {}, pwd:{}" ,email,pwd); 
		if(UserToken.checkUserPwd(email, pwd)){
			String token = UserToken.getUserToken(email, pwd);
			logger.info("token:{}" , token);
			response.sendRedirect("console.html?token="+token);
			return;
		}
	}
}
