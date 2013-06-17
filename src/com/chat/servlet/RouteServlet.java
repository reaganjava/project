package com.chat.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

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

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public RouteServlet() {
		super();
		try {
			String[][] servs = new String[][] { 
			   { "localhost", "11211" },
			// {"localhost", "11212"}
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
		Properties prop = new Properties();
		InputStream in = getClass().getResourceAsStream(
				"/config/server.properties");
		prop.load(in);
		Set keyValue = prop.keySet();
		for (Iterator it = keyValue.iterator(); it.hasNext();) {
			String key = (String) it.next();
			String value = prop.getProperty(key);
			String temp = (String) manager.get("TEMP");
			if(temp != null) {
				if(value.equals("temp"));
			}
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
	}

}
