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

package de.dhbw.mannheim.cloudraid.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;

public class ServerConnector {

	private static final String GET = "GET", POST = "POST", DELETE = "DELETE",
			PUT = "PUT";
	private static final String USER = "X-Username", PASSW = "X-Password"
	/* , CONFIRM = "X-Confirm" */;
	private static final String SET_COOKIE = "Set-Cookie", COOKIE = "Cookie";

	private ServerConnection sc;
	private String session = null;

	public ServerConnector(ServerConnection sc) {
		this.sc = sc;
	}

	public boolean login() {
		try {
			boolean state = true;
			HttpURLConnection con = (HttpURLConnection) sc
					.getURL("/user/auth/").openConnection();

			con.setRequestMethod(POST);

			con.setRequestProperty(USER, sc.getUser());
			con.setRequestProperty(PASSW, sc.getPassword());
			switch (con.getResponseCode()) {
			case 202:
				System.out.println("login: success");
				session = con.getHeaderField(SET_COOKIE);
				break;
			case 400:
				state = false;
				System.err.println("login: pw/user wrong");
				break;
			case 406:
				System.err.println("login: already logged in");
				break;
			case 503:
				state = false;
				System.err.println("login: session could not be created");
				break;
			default:
				state = false;
				System.err.println("login: unknown error");
				break;
			}
			con.disconnect();
			con = null;
			return state;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public void logout() {
		try {
			boolean resetSession = true;
			HttpURLConnection con = (HttpURLConnection) sc.getURL(
					"/user/auth/logout/").openConnection();
			con.setRequestMethod(GET);
			con.setRequestProperty(COOKIE, session);

			switch (con.getResponseCode()) {
			case 200:
				System.out.println("logout: success");
				break;
			case 401:
				System.err.println("logout: not logged in");
				break;
			case 405:
				resetSession = false;
				System.err.println("logout: session not submitted");
				break;
			case 503:
				System.err.println("logout: session does not exist");
				break;
			default:
				resetSession = false;
				System.err.println("logout: unknown error");
				break;
			}
			if (resetSession) {
				session = null;
			}
			con.disconnect();
			con = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public File getFile(String path) throws IOException {
		File newFile = new File("/tmp/cloudraid-client/" + System.nanoTime()
				+ ".tmp");
		newFile.getParentFile().mkdirs();
		HttpURLConnection con = (HttpURLConnection) sc.getURL(
				"/file/" + path + "/").openConnection();
		con.setRequestMethod(GET);
		con.setRequestProperty(COOKIE, session);
		con.setDoInput(true);

		switch (con.getResponseCode()) {
		case 200:
			InputStream is = con.getInputStream();
			OutputStream os = new FileOutputStream(newFile);
			byte[] buf = new byte[4096];
			int len;
			while ((len = is.read(buf)) > 0) {
				os.write(buf, 0, len);
			}
			is.close();
			os.close();
			break;
		case 401:
			System.err.println("get: not logged in");
			break;
		case 404:
			System.err.println("get: file not found");
			break;
		case 405:
			System.err.println("get: session not transmitted");
			break;
		case 503:
			System.err.println("get: session does not exist");
			break;
		default:
			System.err.println("get: unknown error");
			break;
		}
		con.disconnect();
		con = null;
		return newFile;
	}

	public void putFile(String path, File inFile) throws IOException {
		HttpURLConnection con = (HttpURLConnection) sc.getURL(
				"/file/" + path + "/").openConnection();
		con.setRequestMethod(PUT);
		con.setRequestProperty(COOKIE, session);
		con.setDoOutput(true);
		con.connect();
		switch (con.getResponseCode()) {
		case 200:
			InputStream is = new FileInputStream(inFile);
			System.out.println("put: start upload");
			OutputStream os = con.getOutputStream();
			byte[] buf = new byte[4096];
			int len;
			while ((len = is.read(buf)) > 0) {
				os.write(buf, 0, len);
			}
			os.close();
			is.close();
			System.out.println("put: upload done");
			break;
		case 401:
			System.err.println("put: not logged in");
			break;
		case 404:
			System.err.println("put: file not found");
			break;
		case 405:
			System.err.println("put: session not transmitted");
			break;
		case 503:
			System.err.println("put: session does not exist");
			break;
		default:
			System.err.println("put: unknown error");
			break;
		}
		con.disconnect();
		con = null;
	}

	public void deleteFile(String path) throws IOException {
		HttpURLConnection con = (HttpURLConnection) sc.getURL(
				"/file/" + path + "/").openConnection();
		con.setRequestMethod(DELETE);
		con.setRequestProperty(COOKIE, session);
		con.connect();
		switch (con.getResponseCode()) {
		case 200:
			System.out.println("delete: success");
			break;
		case 401:
			System.err.println("delete: not logged in");
			break;
		case 404:
			System.err.println("delete: file not found");
			break;
		case 405:
			System.err.println("delete: session not transmitted");
			break;
		case 500:
			System.err.println("delete: error deleting the file");
			break;
		case 503:
			System.err.println("delete: session does not exist");
			break;
		default:
			System.err.println("delete: unknown error");
			break;
		}
		con.disconnect();
		con = null;
	}

	public HashMap<String, Long> getFileList() {
		return null;
	}

}
