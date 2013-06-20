package com.chat.entity;

import java.io.Serializable;

public class Message implements Serializable {
	
	private String msgId;
	
	private ChatType type;
	
	private String formMember;
	
	private String toMember;

	private String content;
	
	private String domain;
	
	private String dateFormat;
	//0初始，1发送，2已读
	private int status;

	public String getMsgId() {
		return msgId;
	}

	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}

	public ChatType getType() {
		return type;
	}

	public void setType(ChatType type) {
		this.type = type;
	}

	public String getFormMember() {
		return formMember;
	}

	public void setFormMember(String formMember) {
		this.formMember = formMember;
	}

	public String getToMember() {
		return toMember;
	}

	public void setToMember(String toMember) {
		this.toMember = toMember;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
	
}
