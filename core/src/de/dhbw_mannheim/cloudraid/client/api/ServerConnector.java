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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

/**
 * Manages the connection to a CloudRAID server.
 * 
 * @author Florian Bausch
 * 
 */
public class ServerConnector {
	/**
	 * The date format used by the CloudRAID server.
	 */
	public static final SimpleDateFormat CLOUDRAID_DATE_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd hh:mm:ss.S");

	/**
	 * The top-level path to the programs config.
	 */
	private static String CLOUDRAID_HOME = System.getProperty("os.name")
			.contains("windows") ? System.getenv("APPDATA")
			+ "\\cloudraid-client\\" : System.getProperty("user.home")
			+ "/.config/cloudraid-client/";
	private static final String GET = "GET", POST = "POST", DELETE = "DELETE",
			PUT = "PUT";
	private static final String HTTP401 = "not logged in",
			HTTP404 = "file not found", HTTP405 = "session not transmitted",
			HTTP406 = "already logged in", HTTP409 = "conflict",
			HTTP411 = "content-length required",
			HTTP503 = "session does not exist", HTTP_UNKNOWN = "unknown error";
	private static final String SET_COOKIE = "Set-Cookie", COOKIE = "Cookie";

	private static final String USER = "X-Username", PASSW = "X-Password",
			CONTENT_LENGTH = "Content-Length", CONFIRM = "X-Confirm";

	/**
	 * Restores the session data from a file.
	 */
	public static ServerConnector restoreSession() {
		BufferedReader br = null;
		ServerConnector newCon = null;
		try {
			File sessionFile = new File(CLOUDRAID_HOME + "/session");
			br = new BufferedReader(new FileReader(sessionFile));
			ServerConnection sc = new ServerConnection(br.readLine(),
					br.readLine(), br.readLine(), Short.parseShort(br
							.readLine()));
			newCon = new ServerConnector(sc);
			newCon.setSession(br.readLine());
		} catch (IOException e) {
			// TODO
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException ignore) {
			}
		}
		return newCon;
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
	 */
	public ServerConnector(ServerConnection sc) {
		this.sc = sc;
	}

	/**
	 * Creates a {@link ServerConnector} basing on the credentials in a
	 * {@link ServerConnection}. The {@link DataPresenter} is also registered.
	 * 
	 * @param sc
	 *            A {@link ServerConnection}.
	 * @param dp
	 *            A {@link DataPresenter}.
	 */
	public ServerConnector(ServerConnection sc, DataPresenter dp) {
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
		HttpURLConnection con = (HttpURLConnection) sc.getURL("/user/chgpw/")
				.openConnection();
		con.setRequestMethod(POST);
		con.setRequestProperty(COOKIE, session);
		con.setRequestProperty(USER, sc.getUser());
		con.setRequestProperty(PASSW, newPassword);
		con.setRequestProperty(CONFIRM, newPasswordConfirm);
		con.connect();
		try {
			switch (con.getResponseCode()) {
			case 200:
				System.out.println("changePW: success");
				break;
			case 400:
				throw new HTTPException(400,
						"changePW: user name and/or password and/or confirmation missing/wrong");
			case 401:
				throw new HTTPException(401, "changePW: " + HTTP401);
			case 405:
				throw new HTTPException(401, "changePW: " + HTTP405);
			case 500:
				throw new HTTPException(500,
						"changePW: error while updating the user record");
			case 503:
				throw new HTTPException(503, "changePW: " + HTTP503);
			default:
				throw new HTTPException(con.getResponseCode(), "changePW: "
						+ HTTP_UNKNOWN);
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
		HttpURLConnection con = (HttpURLConnection) sc.getURL("/user/add/")
				.openConnection();
		con.setRequestMethod(POST);
		con.setRequestProperty(USER, sc.getUser());
		con.setRequestProperty(PASSW, sc.getPassword());
		con.setRequestProperty(CONFIRM, conf);
		con.connect();
		try {
			switch (con.getResponseCode()) {
			case 200:
				System.out.println("createUser: successful");
				break;
			case 400:
				throw new HTTPException(400,
						"user name and/or password and/or confirmation missing/wrong");
			case 406:
				throw new HTTPException(406, HTTP406);
			case 500:
				throw new HTTPException(500,
						"error while adding user to database");
			default:
				throw new HTTPException(con.getResponseCode(), HTTP_UNKNOWN);
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
		HttpURLConnection con = (HttpURLConnection) sc.getURL(
				"/file/" + path + "/").openConnection();
		con.setRequestMethod(DELETE);
		con.setRequestProperty(COOKIE, session);
		con.connect();
		try {
			switch (con.getResponseCode()) {
			case 200:
				System.out.println("delete: success");
				break;
			case 401:
				throw new HTTPException(401, "delete: " + HTTP401);
			case 404:
				throw new HTTPException(404, "delete: " + HTTP404);
			case 405:
				throw new HTTPException(405, "delete: " + HTTP405);
			case 500:
				throw new HTTPException(500, "delete: error deleting the file");
			case 503:
				throw new HTTPException(503, "delete: " + HTTP503);
			default:
				throw new HTTPException(con.getResponseCode(), "delete: "
						+ HTTP_UNKNOWN);
			}
		} finally {
			con.disconnect();
		}
	}

	/**
	 * Gets a file from the server.
	 * 
	 * @param path
	 *            The path of the file on the server.
	 * @param destination
	 *            The File object representing the downloaded file will be
	 *            written to.
	 * @throws IOException
	 * @throws HTTPException
	 */
	public void getFile(String path, File destination) throws IOException,
			HTTPException {
		InputStream is = null;
		OutputStream os = null;
		destination.getParentFile().mkdirs();
		HttpURLConnection con = (HttpURLConnection) sc.getURL(
				"/file/" + path + "/").openConnection();
		con.setRequestMethod(GET);
		con.setRequestProperty(COOKIE, session);
		con.setDoInput(true);
		con.connect();
		try {
			switch (con.getResponseCode()) {
			case 200:
				System.out.println("get: success");
				is = con.getInputStream();
				os = new FileOutputStream(destination);
				byte[] buf = new byte[4096];
				int len,
				sum = 0;
				try {
					while ((len = is.read(buf)) != -1) {
						os.write(buf, 0, len);
						sum += len;
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
				System.out.println("get: read " + sum + " bytes");
				break;
			case 401:
				throw new HTTPException(401, "get: " + HTTP401);
			case 404:
				throw new HTTPException(404, "get: " + HTTP404);
			case 405:
				throw new HTTPException(405, "get: " + HTTP405);
			case 503:
				throw new HTTPException(503, "get: " + HTTP503);
			default:
				throw new HTTPException(con.getResponseCode(), "get: "
						+ HTTP_UNKNOWN);
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
		HttpURLConnection con = (HttpURLConnection) sc.getURL("/list/")
				.openConnection();
		con.setRequestMethod(GET);
		con.setRequestProperty(COOKIE, session);
		con.connect();
		try {

			switch (con.getResponseCode()) {
			case 200:
				System.out.println("list: successful");
				br = new BufferedReader(new InputStreamReader(
						con.getInputStream()));
				String line;
				System.out.println("File list:");
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
					System.out.println(line);
					Date date;
					try {
						date = CLOUDRAID_DATE_FORMAT.parse(parts[2]);
					} catch (ParseException e) {
						continue;
					}
					ret.add(new CloudFile(this, parts[0], parts[3], date
							.getTime(), parts[1]));
				}
				break;
			case 401:
				throw new HTTPException(401, "list: " + HTTP401);
			case 405:
				throw new HTTPException(405, "list: " + HTTP405);
			case 500:
				throw new HTTPException(500,
						"list: error getting the file information");
			case 503:
				throw new HTTPException(503, "list: " + HTTP503);
			default:
				throw new HTTPException(con.getResponseCode(), "list: "
						+ HTTP_UNKNOWN);
			}
		} finally {
			try {
				br.close();
			} catch (Exception ignore) {
			}
			con.disconnect();
		}
		for (DataPresenter dp : dataPresenters) {
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
		HttpURLConnection con = (HttpURLConnection) sc.getURL("/user/auth/")
				.openConnection();
		con.setRequestMethod(POST);
		con.setRequestProperty(USER, sc.getUser());
		con.setRequestProperty(PASSW, sc.getPassword());
		con.connect();
		try {
			switch (con.getResponseCode()) {
			case 202:
				System.out.println("login: success");
				for (String s : con.getHeaderField(SET_COOKIE).split(";")) {
					if (s.startsWith("JSESSIONID=")) {
						session = s;
					}
				}
				System.out.println(session);
				break;
			case 403:
				throw new HTTPException(403, "login: pw/user wrong");
			case 406:
				throw new HTTPException(406, "login: " + HTTP406);
			case 503:
				throw new HTTPException(503,
						"login: session could not be created");
			default:
				throw new HTTPException(con.getResponseCode(), "login: "
						+ HTTP_UNKNOWN);
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
		HttpURLConnection con = (HttpURLConnection) sc.getURL(
				"/user/auth/logout/").openConnection();
		con.setRequestMethod(GET);
		con.setRequestProperty(COOKIE, session);
		con.connect();
		try {
			switch (con.getResponseCode()) {
			case 200:
				removeSession();
				System.out.println("logout: success");
				break;
			case 401:
				removeSession();
				throw new HTTPException(401, "logout: " + HTTP401);
			case 405:
				resetSession = false;
				throw new HTTPException(405, "logout: " + HTTP405);
			case 503:
				removeSession();
				throw new HTTPException(503, "logout: " + HTTP503);
			default:
				resetSession = false;
				throw new HTTPException(con.getResponseCode(), "logout: "
						+ HTTP_UNKNOWN);
			}
		} finally {
			if (resetSession) {
				session = null;
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
	 *            Set to <code>true</code, if the file shall be
	 *            updated/overwritten.
	 * @throws IOException
	 * @throws HTTPException
	 */
	public void putFile(String path, File inFile, boolean update)
			throws IOException, HTTPException {
		String u = update ? "/update/" : "/";
		HttpURLConnection con = (HttpURLConnection) sc.getURL(
				"/file/" + path + u).openConnection();
		System.out.println(con.getURL().toString());
		con.setRequestMethod(PUT);
		con.setRequestProperty(COOKIE, session);
		con.setRequestProperty(CONTENT_LENGTH, String.valueOf(inFile.length()));
		con.setDoOutput(true);
		con.connect();
		InputStream is = new FileInputStream(inFile);
		System.out.println("put: start uploading " + path);
		OutputStream os = con.getOutputStream();
		try {
			byte[] buf = new byte[4096];
			int len;
			while ((len = is.read(buf)) > 0) {
				os.write(buf, 0, len);
			}
			os.close();
			switch (con.getResponseCode()) {
			case 201:
				System.out.println("put: upload done");
				break;
			case 401:
				throw new HTTPException(401, "put: " + HTTP401);
			case 404:
				throw new HTTPException(404, "put: " + HTTP404);
			case 405:
				throw new HTTPException(405, "put: " + HTTP405);
			case 409:
				throw new HTTPException(409, "put: " + HTTP409);
			case 411:
				throw new HTTPException(411, "put: " + HTTP411);
			case 503:
				throw new HTTPException(503, "put: " + HTTP503);
			default:
				throw new HTTPException(con.getResponseCode(), "put: "
						+ HTTP_UNKNOWN);
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
	 * Removes a stored session from the file system.
	 */
	private void removeSession() {
		File sessionFile = new File(CLOUDRAID_HOME + "/session");
		if (sessionFile.exists()) {
			sessionFile.delete();
		}
	}

	/**
	 * Sets the session for the {@link ServerConnector}. Use this to restore a
	 * session.
	 * 
	 * @param session
	 */
	private void setSession(String session) {
		this.session = session;
	}

	/**
	 * Saves the session data to a file.
	 */
	public void storeSession() {
		if (session == null) {
			return;
		}
		BufferedWriter bw = null;
		try {
			File sessionFile = new File(CLOUDRAID_HOME + "/session");
			sessionFile.getParentFile().mkdirs();
			bw = new BufferedWriter(new FileWriter(sessionFile));
			bw.write(this.sc.getServer());
			bw.newLine();
			bw.write(this.sc.getUser());
			bw.newLine();
			bw.write(this.sc.getPassword());
			bw.newLine();
			bw.write(String.valueOf(this.sc.getPort()));
			bw.newLine();
			bw.write(this.session);
			bw.newLine();
		} catch (IOException e) {
			// TODO
			e.printStackTrace();
		} finally {
			try {
				bw.close();
			} catch (IOException ignore) {
			}
		}
	}

	@Override
	public String toString() {
		return "[ServerConnection: " + this.sc + "]. Stored session: "
				+ session;
	}
}
