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

	/**
	 * Creates an object holding the relevant data to establish a connection to
	 * a CloudRAID server.
	 * 
	 * @param server
	 *            The server's address. It does not matter, whether it starts
	 *            with "http://" or not.
	 * @param user
	 *            The user name.
	 * @param password
	 *            The user's password.
	 * @param port
	 *            The server port.
	 * @throws MalformedURLException
	 */
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

	/**
	 * Returns the user's password.
	 * 
	 * @return The password.
	 */
	protected String getPassword() {
		return this.password;
	}

	/**
	 * Returns the port of CloudRAID on the server.
	 * 
	 * @return The port.
	 */
	protected short getPort() {
		return this.port;
	}

	/**
	 * Returns the server address.
	 * 
	 * @return The server address.
	 */
	protected String getServer() {
		return this.server;
	}

	/**
	 * Returns a URL to a resource on the CloudRAID server.
	 * 
	 * @param path
	 *            The path of the resource on the server.
	 * @return The {@link URL} object to the resource.
	 */
	protected URL getURL(String path) {
		try {
			return new URL(this.server + ":" + this.port + path);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns the user name of the connection.
	 * 
	 * @return The user name.
	 */
	protected String getUser() {
		return this.user;
	}

	@Override
	public String toString() {
		return this.user + "@" + this.server + ":" + this.port;
	}
}
