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

package de.dhbw_mannheim.cloudraid.client.gui;

import de.dhbw_mannheim.cloudraid.client.api.ServerConnection;
import de.dhbw_mannheim.cloudraid.client.api.ServerConnector;

/**
 * A class using the CloudRAID-Client core to provide a graphical user interface
 * for the CloudRAID server application.
 * 
 * @author Florian Bausch
 * 
 */
public class ClientMain {

	private static ServerConnection serverConnection = null;
	private static ServerConnector serverConnector = null;
	private static MainWindow mainWindow = null;

	/**
	 * Set a new {@link ServerConnection} for this application.
	 * 
	 * @param sc
	 *            A {@link ServerConnection}.
	 */
	public static synchronized void setServerConnection(ServerConnection sc) {
		ClientMain.serverConnection = sc;
		ClientMain.serverConnector = new ServerConnector(sc, mainWindow);
	}

	/**
	 * Get the current {@link ServerConnection}.
	 * 
	 * @return A {@link ServerConnection}.
	 */
	public static synchronized ServerConnection getServerConnection() {
		return ClientMain.serverConnection;
	}

	/**
	 * Reset the {@link ServerConnection}. You will have to set a new one before
	 * executing the {@link #getServerConnector()} method.
	 */
	public static synchronized void resetServerConnection() {
		ClientMain.serverConnection = null;
		ClientMain.serverConnector = null;
	}

	/**
	 * Returns a {@link ServerConnector} that uses the current
	 * {@link ServerConnection}.
	 * 
	 * @return A {@link ServerConnector}.
	 */
	public static synchronized ServerConnector getServerConnector() {
		return ClientMain.serverConnector;
	}

	/**
	 * The applications main method.
	 * 
	 * @param args
	 *            The String array containing the command-line parameters.
	 */
	public static void main(String[] args) {
		ClientMain.mainWindow = new MainWindow();
	}

}
