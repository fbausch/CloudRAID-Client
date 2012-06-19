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

package de.dhbw.mannheim.cloudraid.client.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Manages the connection to a CloudRAID server.
 * 
 * @author Florian Bausch
 * 
 */
public class ServerConnector {

	private static final String GET = "GET", POST = "POST", DELETE = "DELETE",
			PUT = "PUT";
	private static final String USER = "X-Username", PASSW = "X-Password"
	/* , CONFIRM = "X-Confirm" */;
	private static final String SET_COOKIE = "Set-Cookie", COOKIE = "Cookie";
	private static final String HTTP401 = "not logged in",
			HTTP404 = "file not found", HTTP405 = "session not transmitted",
			HTTP406 = "already logged in", HTTP411 = "content-length required",
			HTTP503 = "session does not exist", HTTP_UNKNOWN = "unknown error";

	private ServerConnection sc;
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
			case 400:
				throw new HTTPException(400, "login: pw/user wrong");
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
				System.out.println("logout: success");
				break;
			case 401:
				throw new HTTPException(401, "logout: " + HTTP401);
			case 405:
				resetSession = false;
				throw new HTTPException(405, "logout: " + HTTP405);
			case 503:
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
	 * Gets a file from the server. The returned {@link File} object is a
	 * temporary file, move it to the correct directory and remove it from the
	 * temporary location.
	 * 
	 * @param path
	 *            The path of the file on the server.
	 * @return The temporary file.
	 * @throws IOException
	 * @throws HTTPException
	 */
	public File getFile(String path) throws IOException, HTTPException {
		File newFile = new File("/tmp/cloudraid-client/" + System.nanoTime()
				+ ".tmp");
		InputStream is = null;
		OutputStream os = null;
		newFile.getParentFile().mkdirs();
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
				os = new FileOutputStream(newFile);
				byte[] buf = new byte[4096];
				int len;
				while ((len = is.read(buf)) > 0) {
					os.write(buf, 0, len);
				}
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
		return newFile;
	}

	/**
	 * Sends a file to the server.
	 * 
	 * @param path
	 *            The path of the file on the server.
	 * @param inFile
	 *            The file to read the data from.
	 * @throws IOException
	 * @throws HTTPException
	 */
	public void putFile(String path, File inFile) throws IOException,
			HTTPException {
		HttpURLConnection con = (HttpURLConnection) sc.getURL(
				"/file/" + path + "/").openConnection();
		con.setRequestMethod(PUT);
		con.setRequestProperty(COOKIE, session);
		con.setDoOutput(true);
		con.connect();
		InputStream is = new FileInputStream(inFile);
		System.out.println("put: start upload");
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
	 * Retrieves a file list from the server.
	 * 
	 * @return An {@link ArrayList} of {@link CloudFile}s.
	 * @throws IOException
	 * @throws HTTPException
	 */
	public ArrayList<CloudFile> getFileList() throws IOException, HTTPException {
		ArrayList<CloudFile> ret = new ArrayList<CloudFile>();
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
					// TODO: Add data handling
					// "test","","1970-01-01 01:00:00.0","UPLOADED"
					String[] parts = line.substring(1, line.length()).split(
							"\",\"");
					if (parts.length != 4) {
						continue;
					}
					for (int i = 0; i < parts.length; i++) {
						parts[i] = parts[i].replaceAll("&quot;", "\"")
								.replaceAll("&amp;", "&");
					}
					System.out.println(line);
					SimpleDateFormat sdf = new SimpleDateFormat(
							"yyyy-MM-dd hh:mm:ss.S");
					Date date;
					try {
						date = sdf.parse(parts[2]);
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
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
		return ret;
	}
}
