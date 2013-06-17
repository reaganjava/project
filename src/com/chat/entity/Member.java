package com.chat.entity;

import java.io.Serializable;

import org.apache.catalina.websocket.MessageInbound;

public class Member implements Serializable {

	private MessageInbound clientInbound;
	
	private String username;

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
	
	
}
