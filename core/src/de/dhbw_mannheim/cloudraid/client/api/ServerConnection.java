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
	private String protocol;
	private short port;

	/**
	 * Creates an object holding the relevant data to establish a connection to
	 * a CloudRAID server.
	 * 
	 * @param server
	 *            The server's address. If the address does not start with
	 *            http:// or https:// it will be assumed that https:// should be
	 *            used. The used protocol can later be changed by calling
	 *            {@link #setSecureConnection(boolean)}.
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
		this.port = port;
		this.user = user;
		this.password = password;
		server = server.toLowerCase();
		if (server.startsWith("http://")) {
			this.protocol = "http://";
			this.server = server.substring(7);
		} else if (server.startsWith("https://")) {
			this.protocol = "https://";
			this.server = server.substring(8);
		} else {
			this.protocol = "https://";
			this.server = server;
		}
		// Test, if server and port are valid
		new URL(this.protocol + this.server + ":" + this.port);
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
	 * Returns the protocol used (http:// or https://).
	 * 
	 * @return The protocol.
	 */
	protected String getProtocol() {
		return this.protocol;
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
			return new URL(this.protocol + this.server + ":" + this.port + path);
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

	/**
	 * Indicates, if the {@link ServerConnection} is secured by HTTPS.
	 * 
	 * @return true, if HTTPS is used; false otherwise.
	 */
	protected boolean isSecureConnection() {
		return "https://".equals(this.protocol);
	}

	/**
	 * Sets the used protocol either to https:// or http://.
	 * 
	 * @param secure
	 *            If true, https:// will be used; if false, http:// will be
	 *            used.
	 */
	protected void setSecureConnection(boolean secure) {
		this.protocol = secure ? "https://" : "http://";
	}

	@Override
	public String toString() {
		return this.protocol + this.user + "@" + this.server + ":" + this.port;
	}
}
