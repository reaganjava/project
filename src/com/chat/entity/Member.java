package com.chat.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import org.apache.catalina.websocket.MessageInbound;

public class Member implements Serializable {

	private MessageInbound clientInbound;
	
	private String username;
	
	private Map<String, Message> MessageMap = new HashMap<String, Message>();

	public MessageInbound getClientInbound() {
		return clientInbound;
	}

	public void setClientInbound(MessageInbound clientInbound) {
		this.clientInbound = clientInbound;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Map<String, Message> getMessageMap() {
		return MessageMap;
	}

	public void setMessageMap(Map<String, Message> messageMap) {
		MessageMap = messageMap;
	}

}
