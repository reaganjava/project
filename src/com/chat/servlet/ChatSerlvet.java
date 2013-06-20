package com.chat.servlet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;

import com.chat.cached.SpyMemcachedManager;
import com.chat.cached.SpyMemcachedServer;
import com.chat.entity.ChatType;
import com.chat.entity.Member;
import com.chat.entity.Message;

@WebServlet("/chat")
public class ChatSerlvet extends WebSocketServlet {

	private static final long serialVersionUID = 1L;
	
	private static Map<String, Map<String, Member>> domainMemberListMap = new ConcurrentHashMap<String, Map<String, Member>>();
	
	private static List<String> domainList = new CopyOnWriteArrayList<String>();
	
	private static Queue<Member> checkMemberList = new LinkedList<Member>();

	private static SpyMemcachedManager manager = null;

	private final String HEADER = "<===header===>";

	private final String BODY = "<===body===>";

	private final String RESPONSE = "<===response===>";

	public ChatSerlvet() {
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
	
	

	@Override
	protected StreamInbound createWebSocketInbound(String subProtocol,
			HttpServletRequest request) {
		System.out.println(request.getLocalAddr());
		return new ChatMessageInbound();
	}

	private class ChatMessageInbound extends MessageInbound {
		
		private WsOutbound outbound = null;
		
		private Thread thread = null;
		
		public ChatMessageInbound() {
			//消息发送线程
			thread = new Thread(new SendTask());
			thread.start();
		}
		
		@Override
		protected void onOpen(WsOutbound outbound) {
			this.outbound = outbound;
			System.out.println("websocket open");
		}

		@Override
		protected void onClose(int status) {
			System.out.println("webSocket close");
		}

		@Override
		protected void onBinaryMessage(ByteBuffer buffer) throws IOException {

		}

		@Override
		protected void onTextMessage(CharBuffer buffer) throws IOException {
			String[] requestData = buffer.toString().split(HEADER);
			switch (requestData[0]) {
			case "CONNECT": {
				System.out.println("CONNECT");
				connect(requestData[1]);
				break;
			}
			case "MSGBRODACAST": {
				System.out.println("MSGBRODACAST");
				broadcast(requestData[1]);
				break;
			}
			case "MSGPRIVATE": {
				System.out.println("MSGPRIVATE");
				privateChat(requestData[1]);
				break;
			}
			case "KEEPALIVE": {
				// System.out.println("KEEPALIVE");
				keepAlive(requestData[1]);
				break;
			}
			case "FEEDBACK" : {
				System.out.println("MSGSUCCESS");
				feedback(requestData[1]);
			}
			case "CLOSE": {
				System.out.println("CLOSE");
				close(requestData[1]);
				break;
			}
			}
		}

		private void connect(String body) {
			String[] connectData = body.split(BODY);
			String username = connectData[0];
			String domain = connectData[1];
			System.out.println(username + ":" + domain);
			//用户列表同步各个服务器的
			List<String> memberList = null;
			List<String> randNameList = (List<String>) manager.get(domain + "RANDNAMELIST");
			if(randNameList == null) {
				randNameList = new ArrayList<String>();
			}
			//随机名字
			if (username.equals("NULL")) {
				username = "网站访客" + (randNameList.size() + 1);
				randNameList.add(username);
			}
			manager.set(domain + "RANDNAMELIST", randNameList, 3000);
			Member member = new Member();
			member.setUsername(username);
			member.setClientInbound(this);
			//从中间缓存中读出用户列表
			memberList = (List<String>) manager.get(domain + "MEMBERLIST");
			//用户列表为空时创建
			if(memberList == null) {
				memberList = new ArrayList<String>();
			} 
			memberList.add(username);
			//每个网站用户列表
			manager.set(domain + "MEMBERLIST", memberList, 3000);
			manager.set(domain + "MEMBERSIZE", memberList.size(), 3000);
			Map<String, Member> onlineMemberMap = domainMemberListMap.get(domain);
			if(onlineMemberMap == null) {
				onlineMemberMap = new ConcurrentHashMap<String, Member>();
			}
			
			//用户信息放入本地在线用户MAP
			onlineMemberMap.put(username, member);
			//访问在服务器的网站域名
			domainList.add(domain);
			//改网站正在聊天人的列表
			domainMemberListMap.put(domain, onlineMemberMap);
			
			System.out.println(onlineMemberMap.getClass().hashCode() + ":" + onlineMemberMap);
			String sendMsg = "SUCCESS" + RESPONSE + username;
			updateMemberList(domain);
			try {
				sendClientMessage(sendMsg);
			} catch (IOException e) {
				e.printStackTrace();
				checkMemberList.offer(member);
			}
		}
		
		private void feedback(String body) {
			System.out.println(body);
			String[] feedData = body.split(BODY);
			String domain = feedData[0];
			String msgId = feedData[1];
			String username = feedData[2];
			Map<String, Member> onlineMemberMap = domainMemberListMap.get(domain);
			if(onlineMemberMap != null) {
				onlineMemberMap.get(username).getMessageMap().get(msgId).setStatus(2);
			}
			domainMemberListMap.put(domain, onlineMemberMap);
		}

		private void privateChat(String body) {
			System.out.println(body);
			String[] privateData = body.split(BODY);
			String domain = privateData[0];
			String formMemberName = privateData[1];
			String toMemberName = privateData[2];
			String message = privateData[3];
			SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");
			String dateFormat = format.format(new Date());
			String msgId = UUID.randomUUID().toString();
			String content = "MSGPRIVATE" + RESPONSE + msgId + BODY + formMemberName + " " + dateFormat + " 说:" + message;
			Message msg = new Message();
			
			msg.setMsgId(msgId);
			//设置对话为私有类型
			msg.setType(ChatType.MSGPRIVATE);
			msg.setDomain(domain);
			msg.setFormMember(formMemberName);
			//发送到该用户
			msg.setToMember(toMemberName);
			msg.setContent(content);
			msg.setDateFormat(dateFormat);
			msg.setStatus(0);
			//私有对话中间缓存队列
			LinkedList<Message> privateMsgQueue = (LinkedList<Message>) manager.get(domain + ChatType.MSGPRIVATE);
			if(privateMsgQueue == null) {
				privateMsgQueue = new LinkedList<Message>();
			}
			privateMsgQueue.offer(msg);
			manager.set(domain + ChatType.MSGPRIVATE, privateMsgQueue, 3000);
			
		}

		public void sendClientMessage(String msg) throws IOException {
			if (msg != null) {
				CharBuffer msgBuffer = CharBuffer.wrap(msg.toCharArray());
				outbound.writeTextMessage(msgBuffer);
				outbound.flush();
			}
		}

		private void broadcast(String body) {
			String[] broadData = body.split(BODY);
			String domain = broadData[0];
			String formMemberName = broadData[1];
			String message = broadData[2];
			SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");
			String dateFormat = format.format(new Date());
			String msgId = UUID.randomUUID().toString();
			String content = "MSGBRODACAST" + RESPONSE + msgId + BODY + formMemberName + " " + dateFormat + " 说:" + message;
			Message msg = new Message();
			msg.setMsgId(msgId);
			//公有对话类型
			msg.setType(ChatType.MSGBRODACAST);
			msg.setDomain(domain);
			msg.setFormMember(formMemberName);
			msg.setContent(content);
			msg.setDateFormat(dateFormat);
			LinkedList<Message> broadMsgQueue = (LinkedList<Message>) manager.get(domain + ChatType.MSGBRODACAST);
			if(broadMsgQueue == null) {
				broadMsgQueue = new LinkedList<Message>();
			}
			broadMsgQueue.offer(msg);
			manager.set(domain + ChatType.MSGBRODACAST, broadMsgQueue, 3000);
		}

		private void keepAlive(String body) {
			String[] keepAliveData = body.split(BODY);
			String domain = keepAliveData[0];
			String sendMsg = "KEEPALIVE" + RESPONSE + 1;
			
			Map<String, Member> onlineMemberMap = domainMemberListMap.get(domain);
			if(onlineMemberMap != null) {
				for (Member member : onlineMemberMap.values()) {
					try {
						((ChatMessageInbound) member.getClientInbound())
								.sendClientMessage(sendMsg);
					} catch (IOException e) {
						e.printStackTrace();
						checkMemberList.offer(member);
					}
				}
			}
		}

		

		private void close(String body) {
			String[] closeData = body.split(BODY);
			String domain = closeData[0];
			String memberName = closeData[1];
			Map<String, Member> onlineMemberMap = domainMemberListMap.get(domain);
			onlineMemberMap.remove(memberName);
			domainMemberListMap.put(domain, onlineMemberMap);
		}

		
	}
	
	class SendTask implements Runnable {

		@Override
		public void run() {
			while(true) {
				for(String domain : domainList) {
					Map<String, Member> onlineMemberMap = domainMemberListMap.get(domain);
					//消息发送还有优化的地方是否可以将私有消息和公有消息放到一个队列里面通过消息类型来区分
					//读出私有消息
					LinkedList<Message> privateMsgQueue = (LinkedList<Message>) manager.get(domain + ChatType.MSGPRIVATE);
					if(privateMsgQueue != null) {
						for(int i = 0; i < privateMsgQueue.size(); i++) {
							Message msg = privateMsgQueue.getLast();
							
							for(Member member : onlineMemberMap.values()) {
								//接收人匹配时发送
								if(msg.getToMember().equals(member.getUsername())) {
									if(member.getMessageMap().get(msg.getMsgId()) == null) {
										msg.setStatus(1);
										member.getMessageMap().put(msg.getMsgId(), msg);
										onlineMemberMap.put(member.getUsername(), member);
									}
								}
							}
						}
						manager.set(domain + ChatType.MSGPRIVATE, privateMsgQueue, 3000);
					}
					//读出广播消息
					LinkedList<Message> broadMsgQueue = (LinkedList<Message>) manager.get(domain + ChatType.MSGBRODACAST);
					if(broadMsgQueue != null) {
						for(int i = 0; i < broadMsgQueue.size(); i++) {
							Message msg = broadMsgQueue.getLast();
							for(Member member : onlineMemberMap.values()) {
								//接受人不为自己时发送
								if(!msg.getFormMember().equals(member.getUsername())) {
								
									if(member.getMessageMap().get(msg.getMsgId()) == null) {
										//消息放入用户的消息发送列表
										member.getMessageMap().put(msg.getMsgId(), msg);
										onlineMemberMap.put(member.getUsername(), member);
									}
								}
							}
						manager.set(domain + ChatType.MSGBRODACAST, broadMsgQueue, 3000);
						}
					}
					for(String username : onlineMemberMap.keySet()) {
						for(String msgId : onlineMemberMap.get(username).getMessageMap().keySet()) {
							if(onlineMemberMap.get(username).getMessageMap().get(msgId).getStatus() == 0) {
								String msg = onlineMemberMap.get(username).getMessageMap().get(msgId).getContent();
								try {
									System.out.println(msg);
									((ChatMessageInbound) onlineMemberMap.get(username).getClientInbound()).sendClientMessage(msg);
									onlineMemberMap.get(username).getMessageMap().get(msgId).setStatus(1);
								} catch (IOException e) {
									e.printStackTrace();
									checkMemberList.offer(onlineMemberMap.get(username));
								}
							}
						}
					}
				    //用户列表检测还有BUG
					checkMember(domain);	
					updateMemberList(domain);
					domainMemberListMap.put(domain, onlineMemberMap);
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void checkMember(String domain) {
		List<String> memberList = (List<String>) manager.get(domain + "MEMBERLIST");
		if(checkMemberList.size() != 0) {
			for (int i = 0; i < checkMemberList.size(); i++) {
				Member member = checkMemberList.poll();
				Map<String, Member> onlineMemberMap = domainMemberListMap.get(domain);
				onlineMemberMap.remove(member.getUsername());
				domainMemberListMap.put(domain, onlineMemberMap);
				memberList.remove(member.getUsername());
			}
			manager.set(domain + "MEMBERLIST", memberList, 3000);
			manager.set(domain, memberList.size(), 3000);
			checkMemberList.clear();
			//更新用户列表
			
		}
	}
	
	private void updateMemberList(String domain) {
		//用户列表同步各个服务器的
		List<String> memberList = null;
		String memberListInfo = "";
		memberList = (List<String>) manager.get(domain + "MEMBERLIST");
		//System.out.println(memberList);
		//组织用户列表数据
		for (String memberName : memberList) {
			memberListInfo += "<option value=\"" + memberName
					+ "\">" + memberName + "</option>";
		}
		String sendMsg = "MEMBERLIST" + RESPONSE + memberListInfo;
		//得到该网站下面的用户信息
		Map<String, Member> onlineMemberMap = domainMemberListMap.get(domain);
		for(Member member : onlineMemberMap.values()) {
			try {
				((ChatMessageInbound) member.getClientInbound())
						.sendClientMessage(sendMsg);
			} catch (IOException e) {
				e.printStackTrace();
				checkMemberList.offer(member);
			}
		} 
	}

}
