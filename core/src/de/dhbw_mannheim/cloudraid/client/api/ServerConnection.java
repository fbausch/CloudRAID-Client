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

import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class stores the connection information to a CloudRAID server.
 * 
 * @author Florian Bausch
 * 
 */
public class ServerConnection {
	private String user;
	private String password;
	private String server;
	private short port;

	public ServerConnection(String server, String user, String password,
			short port) throws MalformedURLException {
		if (!server.startsWith("http://")) {
			server = "http://" + server;
		}
		new URL(server + ":" + port); // Test, if server and port are valid
		this.server = server;
		this.port = port;
		this.user = user;
		this.password = password;
	}

	protected URL getURL(String path) {
		try {
			return new URL(server + ":" + port + path);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}

	protected String getUser() {
		return this.user;
	}

	protected String getPassword() {
		return this.password;
	}

	protected String getServer() {
		return this.server;
	}

	protected short getPort() {
		return this.port;
	}

	public String toString() {
		return user + ":" + password + "@" + server + ":" + port;
	}
}
