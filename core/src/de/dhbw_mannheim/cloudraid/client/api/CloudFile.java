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

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Class representing the files on CloudRAID.
 * 
 * @author Florian Bausch
 * 
 */
public class CloudFile {

	private Date lastMod;
	private String name, state, hashedName;
	private ServerConnector sc;

	/**
	 * Creates a {@link CloudFile} object.
	 * 
	 * @param sc
	 *            The {@link ServerConnector} connected with this file.
	 * @param name
	 *            The name of the file.
	 * @param state
	 *            The state of the file.
	 * @param lastMod
	 *            The last modification date of the file in milliseconds from
	 *            1/1/1970 00:00:00.
	 * @param hashedName
	 *            The hash of the file name.
	 */
	protected CloudFile(ServerConnector sc, String name, String state,
			long lastMod, String hashedName) {
		this.sc = sc;
		this.name = name;
		this.lastMod = new Date(lastMod);
		this.state = state;
		this.hashedName = hashedName;
	}

	/**
	 * Deletes the file represented by this {@link CloudFile} object from the
	 * CloudRAID server.
	 * 
	 * @throws IOException
	 * @throws HTTPException
	 */
	public void delete() throws IOException, HTTPException {
		this.sc.deleteFile(this.name);
	}

	/**
	 * Downloads the file represented by this {@link CloudFile} object to the
	 * file represented by the <code>destination</code> parameter.
	 * 
	 * @param destination
	 *            The destination of the file.
	 * @throws IOException
	 * @throws HTTPException
	 */
	public void downloadTo(File destination) throws IOException, HTTPException {
		this.sc.getFile(this.name, destination);
	}

	/**
	 * Returns the hashed file name.
	 * 
	 * @return The hashed file name.
	 */
	public String getHashedName() {
		return hashedName;
	}

	/**
	 * Returns the last modification date of the file as {@link Date} object.
	 * 
	 * @return The last modification date.
	 */
	public Date getLastMod() {
		return lastMod;
	}

	/**
	 * Returns the last modification date of the file as String formatted using
	 * the {@link ServerConnector#CLOUDRAID_DATE_FORMAT}.
	 * 
	 * @return The last modification date as String.
	 */
	public String getLastModAsString() {
		return ServerConnector.CLOUDRAID_DATE_FORMAT.format(this.lastMod);
	}

	/**
	 * Returns the name of the file.
	 * 
	 * @return The name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the {@link ServerConnector}.
	 * 
	 * @return The {@link ServerConnector}.
	 */
	public ServerConnector getSc() {
		return sc;
	}

	/**
	 * Returns the file's state.
	 * 
	 * @return The state.
	 */
	public String getState() {
		return state;
	}

	/**
	 * Creates a String representation of the object.
	 * 
	 * @return The String representation.
	 */
	public String toString() {
		return name + " (" + hashedName + "), updated on "
				+ ServerConnector.CLOUDRAID_DATE_FORMAT.format(lastMod) + ", "
				+ state + ". ServerConnector: " + sc;
	}
}
