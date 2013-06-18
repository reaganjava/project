var HEADER = "<===header===>";
var BODY = "<===body===>";
var RESPONSE = "<===response===>";
var ws = null;
var isAll = true;
var task = null;
var name = '';
var chatKey = '';
var username = "";
var domain = "";
var ipaddre = "";
$(document)
		.ready(
				function() {
					init();
					function init() {
						var bodyContent = $("body").html();
						bodyContent += "<div id=\"chatBtn\" class=\"btn-class\"><button id=\"showDialog\">对话</button></div><div id=\"dialog\" class=\"dialog-class\"><p><button id=\"hideDialog\">关闭</button></p><div style=\"display:inline;\"><div class=\"msg-class\"><textarea id=\"viewsMsg\" cols=\"28\" rows=\"6\" disabled=\"disabled\"></textarea></div><div class=\"member-class\"><select id=\"memberList\" size=\"10\" style=\"width:130px\"><option value=\"\">ALL</option></select></div></div><div><textarea id=\"privateMsg\" cols=\"28\" rows=\"3\" disabled=\"disabled\"></textarea></div><div><textarea id=\"msg\" cols=\"32\" rows=\"1\"></textarea><button id=\"send\">Send</button></div></div>";
						$("body").html(bodyContent);
						$.ajax({
							type:'get',
							url:'/web/route',
							dataType:'text',
							success:function(data){
                                ipaddre = "ws://" + data + "/web/chat";
                                alert(ipaddre);
							},error : function(){
								alert('异常');
							}
						});
					}
					;
					$("#dialog").hide();
					$("#showDialog").click(function() {

						$("#dialog").show();
						$("#dialog").css("top", 10);
						$("#dialog").css("left", 30);
						$("#chatBtn").hide();
						initWebSocket();

					});
					$("#hideDialog").click(function() {

						$("#dialog").hide();
						var closeData = "CLOSE" + HEADER + domain + BODY + name;
						sendMsg(closeData);
						$("#chatBtn").show();
						if (task) {
							clearInterval(task);
						}
					});
					$("#send").click(
							function() {
								var msg = $("#msg").val();
								var chatData = "";
								if (isAll) {
									var value = $("#viewsMsg").text();
									value += "您说:" + msg + "\n";
									$("#viewsMsg").text(value);
									chatData = "MSGBRODACAST" + HEADER + domain + BODY + name
											+ BODY + msg;
									
								} else {
									var value = $("#privateMsg").text();
									value += "您说:" + msg + "\n";
									$("#privateMsg").text(value);
									chatData = "MSGPRIVATE" + HEADER + domain + BODY + name
											+ BODY + username + BODY + msg;
								}
								sendMsg(chatData);
								$("#msg").val("");
							});
					$("#memberList").click(
							function() {
								username = $("#memberList").find(
										"option:selected").text();
								if (username != 'ALL') {
									isAll = false;
								} else {
									isAll = true;
								}
							});
				});

function initWebSocket() {
	//ws = new WebSocket("ws://localhost:8080/web/chat");
	ws = new WebSocket(ipaddre);
	$("#msg").text("建立连接中");
	ws.onopen = function() {
		var tip = "请输入您的名字";
		name = prompt(tip);
		if (name == null) {
			name = "NULL";
		}
		domain = getURL();
		var connectData = "CONNECT" + HEADER + name + BODY + domain;
		sendMsg(connectData);
		$("#msg").text("");
		task = setInterval(function() {
			var keepaliveData = "KEEPALIVE" + HEADER + domain + BODY + name;
			sendMsg(keepaliveData);
		}, 1000);
	};

	ws.onmessage = function(event) {
		var values = event.data.split(RESPONSE);
		if (values[0] == 'KEEPALIVE') {

		}
		if (values[0] == 'MSGBRODACAST') {
			var value = $("#viewsMsg").text();
			value += values[1] + "\n";
			$("#viewsMsg").text(value);
		}
		if (values[0] == 'MEMBERLIST') {
			value = values[1];
			$("#memberList").empty();
			$("#memberList").append("<option value=\"\">ALL</option>" + value);
		}
		if (values[0] == 'SUCCESS') {
			if (name == "NULL") {
				name = values[1];
			}
		}
		if (values[0] == 'MSGPRIVATE') {
			var value = $("#privateMsg").text();
			value += values[1] + "\n";
			$("#privateMsg").text(value);
		}
	};
	

}

function sendMsg(data) {
	ws.send(data);
}

function getURL(){ 
    var curWebPath = window.document.location.href; 
    //获取主机地址之后的目录，如： cis/website/meun.htm 
    var pathName = window.document.location.pathname; 
    var pos = curWebPath.indexOf(pathName); //获取主机地址，如： http://localhost:8080 
    var localhostPaht = curWebPath.substring(0, pos); //获取带"/"的项目名，如：/cis 
    var projectName = pathName.substring(0, pathName.substr(1).indexOf('/') + 1); 
    var rootPath = localhostPaht + projectName; 
    return rootPath;  
} 
