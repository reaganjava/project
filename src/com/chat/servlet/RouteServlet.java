package com.chat.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.chat.cached.SpyMemcachedManager;
import com.chat.cached.SpyMemcachedServer;

/**
 * Servlet implementation class RouteServlet
 */
@WebServlet("/route")
public class RouteServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private static SpyMemcachedManager manager = null;
	
	private static List<String> serverKey = new CopyOnWriteArrayList<String>();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public RouteServlet() {
		super();
		try {
			String[][] servs = new String[][] { 
			   { "localhost", "11211" },
			   //{"localhost", "11212"}
			};
			List<SpyMemcachedServer> servers = new ArrayList<SpyMemcachedServer>();
			for (int i = 0; i < servs.length; i++) {
				SpyMemcachedServer server = new SpyMemcachedServer();
				server.setIp(servs[i][0]);
				server.setPort(Integer.parseInt(servs[i][1]));
				servers.add(server);
			}
			manager = new SpyMemcachedManager(servers);
			manager.connect();
			Properties prop = new Properties();
			InputStream in = getClass().getResourceAsStream(
					"/config/server.properties");
			prop.load(in);
			Set<Object> keyValue = prop.keySet();
			Map<String, Integer> serverMap = new HashMap<String, Integer>();
			for (Iterator<Object> it = keyValue.iterator(); it.hasNext();) {
				String key = (String) it.next();
				String value = prop.getProperty(key);
				serverKey.add(value);
				serverMap.put(value, 0);
			}
			System.out.println("server size:" + serverMap.size());
			manager.set("WEBSOCKETLIST", serverMap, 3000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Map<String, Integer> serverMap = (Map<String, Integer>) manager.get("WEBSOCKETLIST");
		String serverIp = serverKey.get(0);
		int serverCount = serverMap.get(serverIp);
		for(int i = 0; i < serverKey.size(); i++) {
			String key = serverKey.get(i);
			int c = serverMap.get(key);
			if(serverCount > c) {
				serverCount = c;
				serverIp = key;
			}
		}
		serverMap.put(serverIp, serverCount + 1);
		manager.set("WEBSOCKETLIST", serverMap, 3000);
		System.out.println(serverIp + ":" + serverCount);
		response.setContentType("html/text");
		response.getWriter().println(serverIp);
		response.getWriter().flush();
		response.getWriter().close();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
	}

}
