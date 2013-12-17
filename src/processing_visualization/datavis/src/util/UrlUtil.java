package util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;

public class UrlUtil {
	// Make ctor private so you can only use this class statically.
	private UrlUtil() {

	}

	public static String readUrl(String urlString) throws Exception {
		BufferedReader reader = null;
		try {
			URL url = new URL(urlString);
			reader = new BufferedReader(new InputStreamReader(url.openStream()));
			StringBuffer buffer = new StringBuffer();
			int read;
			char[] chars = new char[1024];
			while ((read = reader.read(chars)) != -1)
				buffer.append(chars, 0, read);

			return buffer.toString();
			/*
			 * } catch (Exception ex) { LOGGER.severe(ex.getMessage());
			 */
		} finally {
			if (reader != null)
				reader.close();
		}
	}
	
	public static String uploadFile(String filePath, String url) throws HttpException, IOException {
		return uploadFile(new File(filePath), url);
	}

	public static String uploadFile(File resourceUrl, String url) throws HttpException, IOException {
		File f = resourceUrl;
		PostMethod filePost = new PostMethod(url);
		Part[] parts = { new FilePart(f.getName(), f) };
		filePost.setRequestEntity(new MultipartRequestEntity(parts, filePost.getParams()));
		HttpClient client = new HttpClient();
		int status = client.executeMethod(filePost);
		String resultUUid = null;
		resultUUid = filePost.getResponseBodyAsString();
		filePost.releaseConnection();
		return resultUUid;
	}
}
