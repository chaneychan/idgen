import java.io.File;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

public class RunOnJetty {
	
	public static void main(String[] args) throws Exception {
		
		
		Server server = new Server(8088); // 也可以改成其它端口
		File rootDir = new File(RunOnJetty.class.getResource("/").getPath()).getParentFile().getParentFile();
		String cfgFilePath =  new File(rootDir, "src/main/resources").getPath();
		String webAppPath = new File(rootDir, "src/main/webapp").getPath();
		new WebAppContext(server, webAppPath, "/");
		server.start();  
	}
	
}
