/*
 * Copyright 2011 - 2012 by the CloudRAID Team
 * see AUTHORS for more details
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.dhbw_mannheim.cloudraid.client.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.SSLException;

/**
 * Manages the connection to a CloudRAID server.
 * 
 * @author Florian Bausch
 * 
 */
public class ServerConnector {
	/**
	 * Indicates the compatible CloudRAID API version.
	 */
	public static final String API_VERSION = "0.3";

	/**
	 * The date format used by the CloudRAID server.
	 */
	private SimpleDateFormat cloudraidDateFormat = new SimpleDateFormat(
			"yyyy-MM-dd hh:mm:ss.S");

	private static final String GET = "GET", POST = "POST", DELETE = "DELETE",
			PUT = "PUT";
	private static final String HTTP401 = "not logged in",
			HTTP404 = "file not found", HTTP405 = "session not transmitted",
			HTTP406 = "already logged in", HTTP409 = "conflict",
			HTTP411 = "content-length required",
			HTTP503 = "session does not exist", HTTP_UNKNOWN = "unknown error";
	private static final String POWERED_BY = "X-Powered-By";

	private static final String SET_COOKIE = "Set-Cookie", COOKIE = "Cookie";

	private static final String USER = "X-Username", PASSW = "X-Password",
			CONTENT_LENGTH = "Content-Length", CONFIRM = "X-Confirm";

	private static final String ENCODING = "utf-8";

	/**
	 * Encodes a file name so that it can be sent to the CloudRAID server.
	 * 
	 * @param fileName
	 *            The file name to be encoded.
	 * @return The encoded file name.
	 * @throws UnsupportedEncodingException
	 */
	private static String urlEncodeFileNames(String fileName)
			throws UnsupportedEncodingException {
		return URLEncoder.encode(fileName, ServerConnector.ENCODING).replace(
				"+", "%20");
	}

	/**
	 * A {@link Vector} of registered {@link DataPresenter} implementations.
	 * They will be triggered, if a new file list is retrieved from the
	 * CloudRAID server.
	 */
	private Vector<DataPresenter> dataPresenters = new Vector<DataPresenter>();

	/**
	 * The {@link ServerConnection} used by this {@link ServerConnector}.
	 */
	private ServerConnection sc;

	/**
	 * The current session ID. Either retrieved from the server or restored from
	 * the file system.
	 */
	private String session = null;

	/**
	 * Creates a {@link ServerConnector} basing on the credentials in a
	 * {@link ServerConnection}.
	 * 
	 * @param sc
	 *            A {@link ServerConnection}.
	 * @throws IncompatibleApiVersionException
	 * @throws IOException
	 */
	public ServerConnector(ServerConnection sc)
			throws IncompatibleApiVersionException, IOException {
		this.sc = sc;
		if (validateProtocol() != 200) {
			throw new IOException("Unexpected response from server.");
		}
		if (!validateApi()) {
			throw new IncompatibleApiVersionException();
		}
	}

	/**
	 * Creates a {@link ServerConnector} basing on the credentials in a
	 * {@link ServerConnection}. The {@link DataPresenter} is also registered.
	 * 
	 * @param sc
	 *            A {@link ServerConnection}.
	 * @param dp
	 *            A {@link DataPresenter}.
	 * @throws IncompatibleApiVersionException
	 * @throws IOException
	 */
	public ServerConnector(ServerConnection sc, DataPresenter dp)
			throws IncompatibleApiVersionException, IOException {
		this(sc);
		this.dataPresenters.add(dp);
	}

	/**
	 * Changes the password of the user currently logged in.
	 * 
	 * @param newPassword
	 *            The new password.
	 * @param newPasswordConfirm
	 *            The confirmation of the password. Never use the same variable
	 *            as for newPassword.
	 * @throws IOException
	 * @throws HTTPException
	 */
	public void changePassword(String newPassword, String newPasswordConfirm)
			throws IOException, HTTPException {
		HttpURLConnection con = (HttpURLConnection) this.sc.getURL(
				"/user/chgpw/").openConnection();
		con.setRequestMethod(ServerConnector.POST);
		con.setRequestProperty(ServerConnector.COOKIE, this.session);
		con.setRequestProperty(ServerConnector.USER, this.sc.getUser());
		con.setRequestProperty(ServerConnector.PASSW, newPassword);
		con.setRequestProperty(ServerConnector.CONFIRM, newPasswordConfirm);
		con.connect();
		try {
			switch (con.getResponseCode()) {
			case 200:
				break;
			case 400:
				throw new HTTPException(400,
						"changePW: user name and/or password and/or confirmation missing/wrong");
			case 401:
				throw new HTTPException(401, "changePW: "
						+ ServerConnector.HTTP401);
			case 405:
				throw new HTTPException(401, "changePW: "
						+ ServerConnector.HTTP405);
			case 500:
				throw new HTTPException(500,
						"changePW: error while updating the user record");
			case 503:
				throw new HTTPException(503, "changePW: "
						+ ServerConnector.HTTP503);
			default:
				throw new HTTPException(con.getResponseCode(), "changePW: "
						+ ServerConnector.HTTP_UNKNOWN);
			}
		} finally {
			con.disconnect();
		}
	}

	/**
	 * Sends the request for the creation of a new user to the server.
	 * 
	 * @param conf
	 *            The confirmation of the password in the constructor's
	 *            {@link ServerConnection}.
	 * @throws IOException
	 * @throws HTTPException
	 */
	public void createUser(String conf) throws IOException, HTTPException {
		HttpURLConnection con = (HttpURLConnection) this.sc
				.getURL("/user/add/").openConnection();
		con.setRequestMethod(ServerConnector.POST);
		con.setRequestProperty(ServerConnector.USER, this.sc.getUser());
		con.setRequestProperty(ServerConnector.PASSW, this.sc.getPassword());
		con.setRequestProperty(ServerConnector.CONFIRM, conf);
		con.connect();
		try {
			switch (con.getResponseCode()) {
			case 200:
				break;
			case 400:
				throw new HTTPException(400,
						"user name and/or password and/or confirmation missing/wrong");
			case 406:
				throw new HTTPException(406, ServerConnector.HTTP406);
			case 500:
				throw new HTTPException(500, "error while "
						+ "adding user to database or the creation of "
						+ "new users is disabled by the server administrator");
			default:
				throw new HTTPException(con.getResponseCode(),
						ServerConnector.HTTP_UNKNOWN);
			}
		} finally {
			con.disconnect();
		}
	}

	/**
	 * Deletes a file on the server.
	 * 
	 * @param path
	 *            The path of the file on the server.
	 * @throws IOException
	 * @throws HTTPException
	 */
	public void deleteFile(String path) throws IOException, HTTPException {
		path = urlEncodeFileNames(path);
		HttpURLConnection con = (HttpURLConnection) this.sc.getURL(
				"/file/" + path + "/").openConnection();
		con.setRequestMethod(ServerConnector.DELETE);
		con.setRequestProperty(ServerConnector.COOKIE, this.session);
		con.connect();
		try {
			switch (con.getResponseCode()) {
			case 200:
				break;
			case 401:
				throw new HTTPException(401, "delete: "
						+ ServerConnector.HTTP401);
			case 404:
				throw new HTTPException(404, "delete: "
						+ ServerConnector.HTTP404);
			case 405:
				throw new HTTPException(405, "delete: "
						+ ServerConnector.HTTP405);
			case 500:
				throw new HTTPException(500, "delete: error deleting the file");
			case 503:
				throw new HTTPException(503, "delete: "
						+ ServerConnector.HTTP503);
			default:
				throw new HTTPException(con.getResponseCode(), "delete: "
						+ ServerConnector.HTTP_UNKNOWN);
			}
		} finally {
			con.disconnect();
		}
	}

	/**
	 * Returns information about the CloudRAID server.
	 * 
	 * @return The information.
	 * @throws IOException
	 * @throws HTTPException
	 */
	public String getApiInfo() throws IOException, HTTPException {
		HttpURLConnection con = null;
		con = (HttpURLConnection) this.sc.getURL("/api/info/").openConnection();
		con.setRequestMethod(ServerConnector.GET);
		con.setRequestProperty("Accept-Encoding", "gzip");
		con.setDoInput(true);
		InputStream is = null;
		StringBuilder sb = new StringBuilder();
		try {
			con.connect();
			if (con.getResponseCode() != 200) {
				throw new HTTPException(con.getResponseCode(),
						ServerConnector.HTTP_UNKNOWN);
			}
			is = new GZIPInputStream(con.getInputStream());
			int c;
			while ((c = is.read()) != -1) {
				sb.append((char) c);
			}
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException ignore) {
			}
			con.disconnect();
		}
		return sb.toString();
	}

	/**
	 * Gets a file from the server.
	 * 
	 * @param path
	 *            The path of the file on the server.
	 * @param destination
	 *            The file where the CloudRAID file will be written to.
	 * @throws IOException
	 * @throws HTTPException
	 */
	public void getFile(String path, File destination) throws IOException,
			HTTPException {
		path = urlEncodeFileNames(path);
		InputStream is = null;
		OutputStream os = null;
		destination.getParentFile().mkdirs();
		HttpURLConnection con = (HttpURLConnection) this.sc.getURL(
				"/file/" + path + "/").openConnection();
		con.setRequestMethod(ServerConnector.GET);
		con.setRequestProperty("Accept-Encoding", "gzip");
		con.setRequestProperty(ServerConnector.COOKIE, this.session);
		con.setDoInput(true);
		con.connect();
		try {
			switch (con.getResponseCode()) {
			case 200:
				if (destination.exists()) {
					destination.delete();
				}
				is = new GZIPInputStream(con.getInputStream());
				os = new FileOutputStream(destination);
				byte[] buf = new byte[4096];
				int len;
				try {
					while ((len = is.read(buf)) != -1) {
						os.write(buf, 0, len);
					}
				} finally {
					try {
						is.close();
					} catch (IOException ignore) {
					}
					try {
						os.close();
					} catch (IOException ignore) {
					}
				}
				break;
			case 401:
				throw new HTTPException(401, "get: " + ServerConnector.HTTP401);
			case 404:
				throw new HTTPException(404, "get: " + ServerConnector.HTTP404);
			case 405:
				throw new HTTPException(405, "get: " + ServerConnector.HTTP405);
			case 503:
				throw new HTTPException(503, "get: " + ServerConnector.HTTP503);
			default:
				throw new HTTPException(con.getResponseCode(), "get: "
						+ ServerConnector.HTTP_UNKNOWN);
			}
		} finally {
			con.disconnect();
		}
	}

	/**
	 * Retrieves a file list from the server. The file list is automated given
	 * to every {@link DataPresenter} registered with this
	 * {@link ServerConnector}.
	 * 
	 * @return An {@link ArrayList} of {@link CloudFile}s.
	 * @throws IOException
	 * @throws HTTPException
	 */
	public Vector<CloudFile> getFileList() throws IOException, HTTPException {
		Vector<CloudFile> ret = new Vector<CloudFile>();
		BufferedReader br = null;
		HttpURLConnection con = (HttpURLConnection) this.sc.getURL("/list/")
				.openConnection();
		con.setRequestProperty("Accept-Encoding", "gzip");
		con.setRequestMethod(ServerConnector.GET);
		con.setRequestProperty(ServerConnector.COOKIE, this.session);
		con.connect();
		try {

			switch (con.getResponseCode()) {
			case 200:
				br = new BufferedReader(new InputStreamReader(
						new GZIPInputStream(con.getInputStream())));
				String line;
				while ((line = br.readLine()) != null) {
					if ("".equals(line) || line.length() < 3) {
						continue;
					}
					// "name","hash","1970-01-01 01:00:00.0","STATE"
					String[] parts = line.substring(1, line.length() - 1)
							.split("\",\"");
					if (parts.length != 4) {
						continue;
					}
					for (int i = 0; i < parts.length; i++) {
						parts[i] = parts[i].replaceAll("&quot;", "\"")
								.replaceAll("&amp;", "&");
					}
					Date date;
					try {
						date = this.cloudraidDateFormat.parse(parts[2]);
					} catch (ParseException e) {
						continue;
					}
					ret.add(new CloudFile(this, parts[0], parts[3], date
							.getTime(), parts[1]));
				}
				break;
			case 401:
				throw new HTTPException(401, "list: " + ServerConnector.HTTP401);
			case 405:
				throw new HTTPException(405, "list: " + ServerConnector.HTTP405);
			case 500:
				throw new HTTPException(500,
						"list: error getting the file information");
			case 503:
				throw new HTTPException(503, "list: " + ServerConnector.HTTP503);
			default:
				throw new HTTPException(con.getResponseCode(), "list: "
						+ ServerConnector.HTTP_UNKNOWN);
			}
		} finally {
			try {
				br.close();
			} catch (Exception ignore) {
			}
			con.disconnect();
		}
		for (DataPresenter dp : this.dataPresenters) {
			dp.giveFileList(ret);
		}
		return ret;
	}

	/**
	 * Logs in to the server with the data from the constructor's
	 * {@link ServerConnection}. If the login is <b>not</b> successful an
	 * {@link HTTPException} will be thrown. If there is a problem as an
	 * unreachable server, an {@link IOException} will be thrown.
	 * 
	 * @throws IOException
	 * @throws HTTPException
	 */
	public void login() throws IOException, HTTPException {
		HttpURLConnection con = (HttpURLConnection) this.sc.getURL(
				"/user/auth/").openConnection();
		con.setRequestMethod(ServerConnector.POST);
		con.setRequestProperty(ServerConnector.USER, this.sc.getUser());
		con.setRequestProperty(ServerConnector.PASSW, this.sc.getPassword());
		con.connect();
		try {
			switch (con.getResponseCode()) {
			case 202:
				for (String s : con.getHeaderField(ServerConnector.SET_COOKIE)
						.split(";")) {
					if (s.startsWith("JSESSIONID=")) {
						this.session = s;
					}
				}
				break;
			case 403:
				throw new HTTPException(403, "login: pw/user wrong");
			case 406:
				throw new HTTPException(406, "login: "
						+ ServerConnector.HTTP406);
			case 503:
				throw new HTTPException(503,
						"login: session could not be created");
			default:
				throw new HTTPException(con.getResponseCode(), "login: "
						+ ServerConnector.HTTP_UNKNOWN);
			}
		} finally {
			con.disconnect();
		}
	}

	/**
	 * Ends a session on the server. An {@link HTTPException} will be thrown, if
	 * there is another HTTP status than 200.
	 * 
	 * @throws IOException
	 * @throws HTTPException
	 */
	public void logout() throws IOException, HTTPException {
		boolean resetSession = true;
		HttpURLConnection con = (HttpURLConnection) this.sc.getURL(
				"/user/auth/logout/").openConnection();
		con.setRequestMethod(ServerConnector.GET);
		con.setRequestProperty(ServerConnector.COOKIE, this.session);
		con.connect();
		try {
			switch (con.getResponseCode()) {
			case 200:
				break;
			case 401:
				throw new HTTPException(401, "logout: "
						+ ServerConnector.HTTP401);
			case 405:
				resetSession = false;
				throw new HTTPException(405, "logout: "
						+ ServerConnector.HTTP405);
			case 503:
				throw new HTTPException(503, "logout: "
						+ ServerConnector.HTTP503);
			default:
				resetSession = false;
				throw new HTTPException(con.getResponseCode(), "logout: "
						+ ServerConnector.HTTP_UNKNOWN);
			}
		} finally {
			if (resetSession) {
				this.session = null;
			}
			con.disconnect();
		}
	}

	/**
	 * Sends a file to the server.
	 * 
	 * @param path
	 *            The path of the file on the server.
	 * @param inFile
	 *            The file to read the data from.
	 * @param update
	 *            Set to <code>true</code>, if the file shall be
	 *            updated/overwritten.
	 * @throws IOException
	 * @throws HTTPException
	 */
	public void putFile(String path, File inFile, boolean update)
			throws IOException, HTTPException {
		path = urlEncodeFileNames(path);
		HttpURLConnection con = (HttpURLConnection) this.sc.getURL(
				"/file/" + path + "/").openConnection();
		String kind;
		if (update) {
			con.setRequestMethod(ServerConnector.PUT);
			kind = "put: ";
		} else {
			con.setRequestMethod(ServerConnector.POST);
			kind = "post: ";
		}
		con.setRequestProperty("Accept-Encoding", "gzip");
		con.setRequestProperty(ServerConnector.COOKIE, this.session);
		con.setRequestProperty(ServerConnector.CONTENT_LENGTH,
				String.valueOf(inFile.length()));
		con.setDoOutput(true);
		con.connect();
		InputStream is = new FileInputStream(inFile);
		OutputStream os = new GZIPOutputStream(con.getOutputStream());
		try {
			byte[] buf = new byte[4096];
			int len;
			while ((len = is.read(buf)) > 0) {
				os.write(buf, 0, len);
			}
			os.close();
			switch (con.getResponseCode()) {
			case 200:
			case 201:
				break;
			case 401:
				throw new HTTPException(401, kind + ServerConnector.HTTP401);
			case 404:
				throw new HTTPException(404, kind + ServerConnector.HTTP404);
			case 405:
				throw new HTTPException(405, kind + ServerConnector.HTTP405);
			case 409:
				throw new HTTPException(409, kind + ServerConnector.HTTP409);
			case 411:
				throw new HTTPException(411, kind + ServerConnector.HTTP411);
			case 503:
				throw new HTTPException(503, kind + ServerConnector.HTTP503);
			default:
				throw new HTTPException(con.getResponseCode(), "put: "
						+ ServerConnector.HTTP_UNKNOWN);
			}
		} finally {
			try {
				is.close();
			} catch (IOException ignore) {
			}
			try {
				os.close();
			} catch (IOException ignore) {
			}
			con.disconnect();
		}
	}

	/**
	 * Registers a {@link DataPresenter} with this {@link ServerConnector}.
	 * 
	 * @param dp
	 *            A {@link DataPresenter}.
	 */
	public void registerDataPresenter(DataPresenter dp) {
		this.dataPresenters.add(dp);
	}

	/**
	 * Sends a dummy request to the CloudRAID server and returns the regarding
	 * HTTP status code.
	 * 
	 * @return The HTTP status code.
	 * @throws IOException
	 * @throws SSLException
	 */
	private int sendDummyRequest() throws IOException, SSLException {
		HttpURLConnection con = (HttpURLConnection) this.sc
				.getURL("/api/info/").openConnection();
		try {
			con.connect();
			return con.getResponseCode();
		} finally {
			con.disconnect();
		}
	}

	@Override
	public String toString() {
		return "[ServerConnection: " + this.sc + "]. Session: " + this.session;
	}

	/**
	 * Validates the API version of the CloudRAID server.
	 * 
	 * @return true, if the server's API version matches the API version
	 *         supported by this {@link ServerConnector}; false, if not.
	 * @throws IOException
	 */
	private boolean validateApi() throws IOException {
		HttpURLConnection con = null;
		con = (HttpURLConnection) this.sc.getURL("/api/info/").openConnection();
		con.setRequestMethod(ServerConnector.GET);
		con.setDoInput(true);
		try {
			con.connect();
			String apiVersion = con.getHeaderField(ServerConnector.POWERED_BY);
			String wantedVersion = "CloudRAID/" + ServerConnector.API_VERSION;
			return wantedVersion.equals(apiVersion);
		} finally {
			con.disconnect();
		}
	}

	/**
	 * Checks, if the protocol used can be used with the CloudRAID server. If
	 * HTTP is given, but HTTPS would work, the protocol is changed to HTTPS. If
	 * HTTPS is given, but would not work, the protocol is <em>not</em> changed
	 * to HTTP for security reasons.
	 * 
	 * @return Returns the status code of the last successful request to the
	 *         server.
	 * @throws IOException
	 */
	private int validateProtocol() throws IOException {
		int resp = this.sendDummyRequest();
		if (!this.sc.isSecureConnection()) {
			// Check, if HTTPS can be used.
			this.sc.setSecureConnection(true);
			try {
				resp = this.sendDummyRequest();
			} catch (SSLException e) {
				this.sc.setSecureConnection(false);
			}
		}
		return resp;
	}
}
