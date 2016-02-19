package me.ele.idgen.support.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import me.ele.elog.Log;
import me.ele.elog.LogFactory;
import me.ele.idgen.common.CuratorClient;
import me.ele.idgen.common.IDConfig;
import me.ele.idgen.model.Policy;
import me.ele.idgen.model.Rule;
import me.ele.idgen.model.Seq;

import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.netflix.curator.framework.CuratorFramework;

/**
 * @Description: zk节点控制
 * @author shaoqunxi
 * @date 2015年9月16日
 */
public class ZkNodeServlet extends HttpServlet {

	private static final long serialVersionUID = -4327666335341895490L;
	
	private static final Log log = LogFactory.getLog(ZkNodeServlet.class);
	
	
	private static final String OP_POLICY_LIST= "getPolicyList";
	private static final String OP_RULE_LIST= "getRuleList";
	private static final String OP_SEQ_LIST= "getSeqList";
	private static final String OP_SAVEDATA = "saveData";
	
	public void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String op = StringUtils.defaultIfEmpty(request.getParameter("op"),"").trim();
		String token =StringUtils.defaultIfEmpty(request.getParameter("token"),"").trim();
		if(!UserToken.checkUserToken(token)){
			return ;
		}
		log.info("op:{}",op);
		CuratorFramework client = CuratorClient.getClient();
		if(OP_POLICY_LIST.equalsIgnoreCase(op)){
			byte[] c = null;
			try {
				c = (byte[]) client.getData().forPath(IDConfig.policyroot);
			} catch (Exception e) {
				log.error(e.getMessage());
			}
			response.getWriter().println(new String(c));
		}else if(OP_RULE_LIST.equalsIgnoreCase(op)){
			byte[] c = null;
			try {
				c = (byte[]) client.getData().forPath(IDConfig.ruleroot);
			} catch (Exception e) {
				log.error(e.getMessage());
			}
			response.getWriter().println(new String(c));
		}else if(OP_SEQ_LIST.equalsIgnoreCase(op)){
			byte[] c = null;
			List<Seq> seqList = new ArrayList<Seq>();
			try {
				c = (byte[]) client.getData().forPath(IDConfig.policyroot);
				List<Policy> policyList = JSON.parseArray(new String(c), Policy.class);
				if(!CollectionUtils.isEmpty(policyList)){
					for(Policy p : policyList){
						Stat stat = (Stat) client.checkExists().forPath(IDConfig.seqroot + "/" + p.getNs()  + "/" +  p.getObject());
						if (stat != null) {
							byte[] b = (byte[]) client.getData().forPath(IDConfig.seqroot+ "/" + p.getNs() + "/" + p.getObject());
							Seq seq  = JSON.parseObject(new String(b),Seq.class);
							seqList.add(seq);
						}
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage());
			}
			
			if(CollectionUtils.isEmpty(seqList)){
				response.getWriter().println("");
			}else{
				response.getWriter().println(JSON.toJSONString(seqList));
			}
			
		}else if(OP_SAVEDATA.equalsIgnoreCase(op)){
			//保存写入metrics
			response.setContentType("application/json");
			String path = StringUtils.defaultIfEmpty(request.getParameter("path"),"").trim();
			String nodeData = StringUtils.defaultIfEmpty(request.getParameter("nodeData"),"").trim();
			log.info("path:{},nodeData:{}",path,nodeData);
			JSONObject resObj  = checkNodeData(path,nodeData);
			if(resObj == null || resObj.getString("result").equals("false")){
				response.getWriter().println(resObj);
				return ;
			}
			//validate 提交的数据 
			resObj.put("result","true");
			resObj.put("data","success");
			
			List<ACL> acls = new ArrayList<ACL>(1);  
			String idgenPath = "";
			if(path.equals("policy")){
				idgenPath = IDConfig.policyroot;
			}else if(path.equals("rule")){
				idgenPath = IDConfig.ruleroot;
			}else{
				resObj.put("result","false");
				resObj.put("data","error request data");
				response.getWriter().println(resObj);
				return;
			}
			try {
				Stat stat = (Stat) client.checkExists().forPath(idgenPath);
				if(stat == null){
					Id id1 = new Id("digest", DigestAuthenticationProvider.generateDigest("admin:admin123"));
					ACL acl1 = new ACL(ZooDefs.Perms.ALL, id1);
					acls.add(acl1);
					client.create().withMode(CreateMode.PERSISTENT)
								   .withACL(acls).forPath(idgenPath, nodeData.getBytes());
				}else{
					//
					client.setData().forPath(idgenPath, nodeData.getBytes());
				}
			} catch (Exception e) {
				log.error(e.getMessage());
				resObj.put("result","false");
				resObj.put("data","error request data");
				response.getWriter().println(resObj);
				return;
			}
			response.getWriter().println(resObj);
			return;
		}else{
			log.error("error: {}" ,"操作类型不对");
		}
	}
	
	private static JSONObject checkNodeData(String path,String nodeData){
		JSONObject j = new JSONObject();
		if(StringUtils.isBlank(path) 
				||(!path.equals("policy") && !path.equals("rule"))){
			j.put("result","false");
			j.put("data","policy rule not null");
			return j;
		}
		if(StringUtils.isBlank(nodeData)){
			j.put("result","false");
			j.put("data","nodeData not null");
			return j;
		}
		if(path.equals("policy")){
			try{
				List<Policy> policyList = JSON.parseArray(nodeData, Policy.class);
				if(CollectionUtils.isEmpty(policyList)){
					j.put("result","false");
					j.put("data","policy  data not null");
					return j;
				}
				for(Policy p : policyList){
					if(p==null || StringUtils.isBlank(p.getNs())
							   || StringUtils.isBlank(p.getObject())
							   || StringUtils.isBlank(p.getRulelist())){
						j.put("result","false");
						j.put("data","policy  data  not null");
						return j;
					}
				}
			}catch(Exception e ){
				j.put("result","false");
				j.put("data","policy  format  not right");
				return j;
			}
		}else if(path.equals("rule")){
			try{
				List<Rule> ruleList = JSON.parseArray(nodeData, Rule.class);
				if(CollectionUtils.isEmpty(ruleList)){
					j.put("result","false");
					j.put("data","rule  data not null");
					return j;
				}
				for(Rule r : ruleList){
					if(r==null || StringUtils.isBlank(r.getRuleno())){
						//
						j.put("result","false");
						j.put("data","rule  data  not null");
						return j;
					}
				}
			}catch(Exception e ){
				j.put("result","false");
				j.put("data","rule  format  not right");
				return j;
			}
			
		}
		j.put("result","true");
		j.put("data","success");
		return j;
	}
	
	public static void main(String[] args) {
		String ddd = 
				"[{\"db_max\":\"200s\",\"db_min\":101,\"db_pace\":0,\"hasdbtb\":true,\"ruleno\":\"1\",\"seq_max\":90609001,\"seq_min\":60609001,\"seq_pace\":0,\"seqfirst\":false,\"tb_max\":200,\"tb_min\":101,\"tb_pace\":0}]";
		List<Rule> policyList = JSON.parseArray(ddd, Rule.class);
		for(Rule p : policyList){
			System.out.println(p);
		}
	}
}
