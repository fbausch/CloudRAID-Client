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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;

public class ServerConnector {

	private static final String GET = "GET", POST = "POST", DELETE = "DELETE";

	private ServerConnection sc;

	public ServerConnector(ServerConnection sc) {
		this.sc = sc;
	}

	public InputStream getFile(String path) throws IOException {
		HttpURLConnection con = (HttpURLConnection) sc.getURL()
				.openConnection();
		con.setRequestMethod(GET);
		con.setRequestProperty("user", sc.getUser());
		con.setRequestProperty("password", sc.getPassword());
		con.setDoInput(true);
		return con.getInputStream();
	}

	public void putFile(String path, InputStream is) throws IOException {
		HttpURLConnection con = (HttpURLConnection) sc.getURL()
				.openConnection();
		con.setRequestMethod(POST);
		con.setRequestProperty("user", sc.getUser());
		con.setRequestProperty("password", sc.getPassword());
		con.setRequestProperty("path", path);
		con.setDoOutput(true);
		con.connect();
		OutputStream os = con.getOutputStream();
		int x;
		while ((x = is.read()) > 0) {
			os.write(x);
		}
		con.disconnect();
	}

	public void deleteFile(String path) throws IOException {
		HttpURLConnection con = (HttpURLConnection) sc.getURL()
				.openConnection();
		con.setRequestMethod(DELETE);
		con.setRequestProperty("user", sc.getUser());
		con.setRequestProperty("password", sc.getPassword());
		con.setRequestProperty("path", path);
		con.connect();
		con.disconnect();
	}

	public HashMap<String, Long> getFileList() {
		return null;
	}

}
