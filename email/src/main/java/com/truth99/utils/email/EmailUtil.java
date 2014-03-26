package com.truth99.utils.email;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.SendFailedException;
import javax.mail.Authenticator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailUtil {
	
	
	public static enum EncryptionTypes{
		Default, TLS, SSL
	}
	
	static final Log logger = LogFactory.getLog(EmailUtil.class);
	
	private String default_charset;
	private String mail_host;
	private int mail_port;
	private int encryptionType = EncryptionTypes.Default.ordinal();
	private boolean auth = false;
	private String mail_host_account;
	private String mail_host_password;
	private String senderAddress;
	private String senderName;
	private boolean isHtml = true;

	public EmailUtil(){}
	
	public EmailUtil(String mail_host){
		this.mail_host = mail_host;
	}	
	
	public EmailUtil(String mail_host, boolean auth, String account, String password){
		this(mail_host, 25, EncryptionTypes.Default.ordinal(), auth, account, password);
	}
	
	public EmailUtil(String mail_host,int mail_port, int encryptionType, boolean auth, String account, String password){
		this.mail_host = mail_host;
		this.mail_port = mail_port;
		this.encryptionType = encryptionType;
		this.auth = auth;
		this.mail_host_account = account;
		this.mail_host_password = password;
	}
	
	public EmailUtil(String mail_host, boolean auth, String account, String password, boolean isHtml){
		this(mail_host, 25, EncryptionTypes.Default.ordinal(), auth, account, password, isHtml);
	}
	
	public EmailUtil(String mail_host,int mail_port, int encryptionType, boolean auth, String account, String password, boolean isHtml){
		this.mail_host = mail_host;
		this.mail_port = mail_port;
		this.encryptionType = encryptionType;
		this.auth = auth;
		this.mail_host_account = account;
		this.mail_host_password = password;
		this.isHtml = isHtml;
	}

	/** 
	* @Title: sendEmail
	* @Description: 批量发送无附件邮件
	* @param receiverAddress	收件箱地址字符串，已“;”分隔
	* @param sub				邮件主题
	* @param msg				邮件内容
	* @throws Exception void
	* @author Xuehan.Li
	* @date 2014年3月21日 下午12:18:13
	*/ 
	public void sendEmail(String receiverAddress, String sub, String msg)throws Exception {
		this.sendEmail(receiverAddress, sub, msg,null);
	}
	
	
	/** 
	* @Title: sendEmail
	* @Description: 批量发送无附件邮件
	* @param recipients			收件箱地址列表
	* @param sub				邮件主题
	* @param msg				邮件内容
	* @throws SendFailedException void
	* @author Xuehan.Li
	* @date 2014年3月21日 下午12:19:45
	*/ 
	public void sendEmail(List<String> recipients, String sub, String msg)throws SendFailedException {
		this.sendEmail(recipients, sub, msg, null);
	}
	
	
	/** 
	* @Title: sendEmail
	* @Description: 批量发送有附件邮件
	* @param receiverAddress	收件箱地址字符串，已“;”分隔
	* @param sub				邮件主题
	* @param msg				邮件内容
	* @param attachments		邮件附件列表
	* @throws Exception void
	* @author Xuehan.Li
	* @date 2014年3月21日 下午12:18:13
	*/ 
	public void sendEmail(String receiverAddress, String sub, String msg, Collection<Object> attachments)throws Exception {
		String[] address = receiverAddress.split(";");
		List<String> recipients = new ArrayList<String>(); 
		for(int i=0;i<address.length;i++){
			if(address[i].trim().length()>0){
				recipients.add(address[i]);
			}
		}
		this.sendEmail(recipients, sub, msg, attachments);
	}

	/** 
	* @Title: sendEmail
	* @Description: 批量发送有附件邮件
	* @param recipients			收件箱地址列表
	* @param sub				邮件主题
	* @param msg				邮件内容
	* @param attachments		附件列表
	* @throws SendFailedException void
	* @author Xuehan.Li
	* @date 2014年3月21日 上午11:56:27
	*/ 
	public void sendEmail(List<String> recipients, String sub, String msg, Collection<Object> attachments)throws SendFailedException {
		logger.debug("subject: "+sub
				+" port: "+this.mail_port
				+" encryptionType: "+this.encryptionType
				+" auth: "+this.auth
				+" account: "+this.mail_host_account
				+" password: "+this.mail_host_password
				+" senderName : "+this.senderName);
		Transport transport = null;
		try {
			Properties props = this.getProperties();
			Session session = this.getSession(props);
			MimeMessage message = new MimeMessage(session);
			//判断是html页面还是纯文本，并设置类型
			if(this.getDefaultIsHtml()){
				message.addHeader("Content-type", "text/html");
			}else{
				message.addHeader("Content-type", "text/plain");
			}
			message.setSubject(sub,default_charset);
			message.setFrom(new InternetAddress(senderAddress, senderName));

			for (Iterator<String> it = recipients.iterator(); it.hasNext();) {
				String email = it.next();
				message.addRecipients(Message.RecipientType.TO, email);
			}
			
			Multipart mp = new MimeMultipart();
			
			//内容
			MimeBodyPart contentPart = new MimeBodyPart();
			
			//设置内容编码
			if(this.getDefaultIsHtml()){
				contentPart.setContent("<meta http-equiv=Content-Type content=text/html; charset="+default_charset+">"+msg,"text/html;charset="+default_charset);
			}else{
				contentPart.setText(msg,default_charset);
			}
			mp.addBodyPart(contentPart);
			
			//附件
			if(attachments != null){
				MimeBodyPart attachPart;
                for(Iterator<Object> it = attachments.iterator(); it.hasNext();){
                	attachPart = new MimeBodyPart();
                    FileDataSource fds = new FileDataSource(it.next().toString().trim());
                    attachPart.setDataHandler(new DataHandler(fds));
                    if(fds.getName().indexOf("$") != -1){
                    	attachPart.setFileName(fds.getName().substring(fds.getName().indexOf("$")+1,fds.getName().length()));
                    }else{
                    	attachPart.setFileName(fds.getName());
                    }
                    mp.addBodyPart(attachPart);
                }
			}
			
			message.setContent(mp);
			
			message.setSentDate(new Date());
			
			if(this.getDefaultEncryptionType() == EncryptionTypes.SSL.ordinal()){
				Transport.send(message);
			}else{
				transport = session.getTransport("smtp");
				transport.connect(this.mail_host,this.mail_port,this.mail_host_account,this.mail_host_password);
				transport.sendMessage(message, message.getAllRecipients());
			}
		} catch (Exception e) {
			logger.error("send mail error", e);
			throw new SendFailedException(e.toString());
		} finally{
			if(transport != null){
				try{
					transport.close();
				}catch(Exception ex){
				}
			}
		}
	}
	
	/** 
	* @Title: getProperties
	* @Description: 获取发送邮件所需参数
	* @return Properties
	* @author Xuehan.Li
	* @date 2014年3月21日 下午12:16:14
	*/ 
	private Properties getProperties(){
		Properties props = System.getProperties();
		
		int defaultEncryptionType = this.getDefaultEncryptionType();
		
		if(defaultEncryptionType == EncryptionTypes.TLS.ordinal()){
			props.put("mail.smtp.auth", String.valueOf(this.auth));
			props.put("mail.smtp.starttls.enable", "true");
		}else if(defaultEncryptionType == EncryptionTypes.SSL.ordinal()){
			props.put("mail.smtp.host", this.mail_host);
			props.put("mail.smtp.socketFactory.port", this.mail_port);
			props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			props.put("mail.smtp.debug", "true");
			props.put("mail.smtp.auth", String.valueOf(this.auth));
			props.put("mail.smtp.port", this.mail_port);
		}else{
			props.put("mail.smtp.host", this.mail_host);
			props.put("mail.smtp.auth", String.valueOf(this.auth));
		}
		return props;
	}
	
	
	/** 
	* @Title: getSession
	* @Description: 获取邮件session
	* @param props
	* @return Session
	* @author Xuehan.Li
	* @date 2014年3月21日 下午12:15:34
	*/ 
	private Session getSession(Properties props){
		Session session = null;
		
		if(this.getDefaultEncryptionType() == EncryptionTypes.TLS.ordinal()){
			session = Session.getInstance(props);
		}else if(this.getDefaultEncryptionType() == EncryptionTypes.SSL.ordinal()){
			session = Session.getInstance(props, new MyAuthenticator(this.mail_host_account, this.mail_host_password));
		}else{
			session = Session.getDefaultInstance(props,null);		
		}
		return session;
	}
	
	//获取内容是否为html页面
	private boolean getDefaultIsHtml(){
		return this.isHtml;
	}
	
	//邮件认证类
	private class MyAuthenticator extends Authenticator{
		String user;
		String password;
		
		public MyAuthenticator(String user, String password){
			this.user = user;
			this.password = password;
		}
		protected PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(this.user, this.password);
		}
	}
	
	/**
	 * 获取默认加密类型,
	 * for 465, SSL
	 * for 587, TLS
	 * @return
	 */
	private int getDefaultEncryptionType(){
		int rst = this.encryptionType;
		if(this.encryptionType == EncryptionTypes.Default.ordinal()){
			if(this.mail_port==465){
				rst = EncryptionTypes.SSL.ordinal();
			}else if(this.mail_port==587){
				rst = EncryptionTypes.TLS.ordinal();
			}
		}
		return rst;
	}
	
	public static void main(String[] args){
//		EmailUtil email = new EmailUtil("smtp.gmail.com", 587, 0, true, "<account>","<password>",false);
		
		System.out.println("EncryptionTypes.SSL.ordinal(): "+EncryptionTypes.SSL.ordinal());
		//添加附件
		Collection<Object> col = new ArrayList<Object>();
		col.add("C:/Users/LD/Desktop/LD.png");
		EmailUtil email = new EmailUtil();
		try {
			email.sendEmail("xuehan.li@tendcloud.com", "测试一下","<div style=\"color:red;\">测试内容</div>",col);
			System.out.println("成功");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	
	public String getMail_host() {
		return mail_host;
	}

	public void setMail_host(String mail_host) {
		this.mail_host = mail_host;
	}

	public int getMail_port() {
		return mail_port;
	}

	public void setMail_port(int mail_port) {
		this.mail_port = mail_port;
	}

	public boolean isAuth() {
		return auth;
	}

	public void setAuth(boolean auth) {
		this.auth = auth;
	}

	public String getMail_host_account() {
		return mail_host_account;
	}

	public void setMail_host_account(String mail_host_account) {
		this.mail_host_account = mail_host_account;
	}

	public String getMail_host_password() {
		return mail_host_password;
	}

	public void setMail_host_password(String mail_host_password) {
		this.mail_host_password = mail_host_password;
	}

	public String getSenderAddress() {
		return senderAddress;
	}

	public void setSenderAddress(String senderAddress) {
		this.senderAddress = senderAddress;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}

	public boolean isHtml() {
		return isHtml;
	}

	public void setHtml(boolean isHtml) {
		this.isHtml = isHtml;
	}
	public String getDefault_charset() {
		return default_charset;
	}
	public void setDefault_charset(String default_charset) {
		this.default_charset = default_charset;
	}
	
	
	
}
