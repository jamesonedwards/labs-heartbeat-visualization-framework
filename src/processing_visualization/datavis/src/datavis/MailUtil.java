package datavis;

import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import java.util.Properties;

public class MailUtil {

	private Config config;
	
	// Make default ctor uncallable externally.
	private MailUtil() {
	}
	
	public MailUtil(Config config) throws Exception {
		if (config == null)
			throw new Exception("MailUtil must be configured!");
		this.config = config;
	}

	public static class Config {
		public static final int DEFAULT_PORT = 25;
		public static final int DEFAULT_SECURE_PORT = 587;
		private String host;
		private String username;
		private String password;
		private boolean debugMode;

		public String getHost() {
			return host;
		}

		public boolean isDebugMode() {
			return debugMode;
		}

		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return password;
		}

		// Make default ctor uncallable externally.
		private Config() {
		}

		public Config(String host, String username, String password) {
			this.host = host;
			this.username = username;
			this.password = password;
			this.debugMode = false;
		}

		public Config(String host, String username, String password, boolean debugMode) {
			this.host = host;
			this.username = username;
			this.password = password;
			this.debugMode = debugMode;
		}
	}

	private class SMTPAuthenticator extends javax.mail.Authenticator {
		public PasswordAuthentication getPasswordAuthentication() {
			String username = config.getUsername();
			String password = config.getPassword();
			return new PasswordAuthentication(username, password);
		}
	}

	public void sendSecureEmail(String to, String from, String cc, String bcc, String subject, String body) throws Exception {
		Properties props = new Properties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.host", config.getHost());
		props.put("mail.smtp.port", Config.DEFAULT_SECURE_PORT);
		props.put("mail.smtp.auth", "true");

		Authenticator auth = new SMTPAuthenticator();
		Session mailSession = Session.getDefaultInstance(props, auth);
		mailSession.setDebug(config.isDebugMode());
		Transport transport = mailSession.getTransport();

		MimeMessage message = new MimeMessage(mailSession);
		Multipart multipart = new MimeMultipart("alternative");

		BodyPart part1 = new MimeBodyPart();
		part1.setContent(body, "text/html");
		multipart.addBodyPart(part1);

		message.setContent(multipart);
		message.setFrom(new InternetAddress(from));
		message.setSubject(subject);
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		if (cc != null && cc.length() > 0)
			message.addRecipient(Message.RecipientType.CC, new InternetAddress(cc));
		if (bcc != null && bcc.length() > 0)
			message.addRecipient(Message.RecipientType.BCC, new InternetAddress(bcc));

		transport.connect();
		transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
		transport.close();
	}
}
