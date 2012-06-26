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

package de.dhbw_mannheim.cloudraid.client.cli;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Vector;

import de.dhbw_mannheim.cloudraid.client.api.CloudFile;
import de.dhbw_mannheim.cloudraid.client.api.HTTPException;
import de.dhbw_mannheim.cloudraid.client.api.ServerConnection;
import de.dhbw_mannheim.cloudraid.client.api.ServerConnector;

public class CLIMain {

	public static void main(String[] args) {
		// Check for arguments
		if (args.length == 0) {
			System.err.println("No arguments given.");
			printUsage();
			System.exit(1);
		}
		// Check for login
		else if ("login".equals(args[0])) {
			if (args.length == 4) {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						System.in));
				System.out.println("Enter password:");
				try {
					String pw = "";
					Console c = System.console();
					if (c == null) {
						pw = in.readLine();
					} else {
						pw = String.valueOf(c.readPassword());
					}
					System.out.println("");
					ServerConnector sc;
					sc = new ServerConnector(new ServerConnection(args[2],
							args[1], pw, Short.parseShort(args[3])));
					sc.login();
					sc.storeSession();
					System.out.println("Successfully logged in.");
					System.exit(0);
				} catch (NumberFormatException e) {
					System.err.println("Invalid port number.");
					System.exit(7);
				} catch (MalformedURLException e) {
					System.err.println("Invalid URL.");
					System.exit(7);
				} catch (IOException e) {
					System.err.println("Could not log in.");
					System.exit(7);
				} catch (HTTPException e) {
					System.err.println("Error " + e.getHTTPCode() + ": "
							+ e.getHTTPErrorMessage());
					System.exit(7);
				}
			} else {
				System.err.println("Wrong number of arguments.");
				System.exit(2);
			}
		}
		ServerConnector sc = ServerConnector.restoreSession();
		if (sc == null) {
			System.err.println("Session not found or corrupt.");
			System.exit(-1);
		}
		// Check for logout
		else if ("logout".equals(args[0])) {
			try {
				sc.logout();
				System.out.println("Successfully logged out.");
			} catch (IOException e) {
				System.err.println("Could not log out.");
			} catch (HTTPException e) {
				System.err.println("Error " + e.getHTTPCode() + ": "
						+ e.getHTTPErrorMessage());
			}
		}
		// Check for file list
		else if ("list".equals(args[0])) {
			try {
				Vector<CloudFile> cfs = sc.getFileList();
				System.out.println("Files on CloudRaid server:");
				for (CloudFile cf : cfs) {
					System.out.println(cf.toString());
				}
			} catch (IOException e) {
				System.err.println("Could not get file list.");
			} catch (HTTPException e) {
				System.err.println("Error " + e.getHTTPCode() + ": "
						+ e.getHTTPErrorMessage());
			}
		}
		// Check for download of file
		else if ("get".equals(args[0])) {
			if (args.length == 2) {
				File f = new File(args[1]);
				if (f.exists()) {
					System.err.println("File already exists locally.");
					System.exit(3);
				}
				try {
					File d = sc.getFile(args[1]);
					System.out.println("Downloaded file to "
							+ d.getAbsolutePath());
					if (d.renameTo(f)) {
						System.out.println("Successfully copied file to "
								+ f.getAbsolutePath());
					} else {
						System.err.println("Could not copy to "
								+ f.getAbsolutePath());
						System.exit(4);
					}
				} catch (IOException e) {
					System.err.println("Could not get file.");
				} catch (HTTPException e) {
					System.err.println("Error " + e.getHTTPCode() + ": "
							+ e.getHTTPErrorMessage());
				}
			} else {
				System.err.println("Wrong number of arguments.");
				System.exit(2);
			}
		}
		// Check for deletion of file
		else if ("delete".equals(args[0])) {
			if (args.length == 2) {
				try {
					sc.deleteFile(args[1]);
				} catch (IOException e) {
					System.err.println("Could not delete file.");
				} catch (HTTPException e) {
					System.err.println("Error " + e.getHTTPCode() + ": "
							+ e.getHTTPErrorMessage());
				}
			} else {
				System.err.println("Wrong number of arguments.");
				System.exit(2);
			}
		}
		// Check for upload of file
		else if ("put".equals(args[0])) {
			upload(args, false, sc);
		}
		// Check for update of file
		else if ("update".equals(args[0])) {
			upload(args, true, sc);
		}
		// No known keyword given
		else {
			System.err.println("Unknown command.");
			printUsage();
			System.exit(-2);
		}
	}

	private static void printUsage() {
		System.err.flush();
		System.out.println("Usage of CloudRAID command-line client:");
		System.out.println("* login <user> <server> <port>");
		System.out.println("  - creates a session with a CloudRAID server.\n");
		System.out.println("* logout");
		System.out
				.println("  - closes an existing session with a CloudRAID server.\n");
		System.out.println("* list");
		System.out
				.println("  - lists all files of the current user on the server.\n");
		System.out.println("* get <filename>");
		System.out.println("  - downloads a file from the server.\n");
		System.out.println("* delete <filename>");
		System.out.println("  - deletes a file on the server.\n");
		System.out.println("* put <path_to_file> <filename>");
		System.out.println("  - uploads a file to the server.\n");
		System.out.println("* update <path_to_file> <filename>");
		System.out
				.println("  - uploads _and_ overwrites a file on the server.\n");
	}

	private static void upload(String[] args, boolean update, ServerConnector sc) {
		if (args.length == 3) {
			File f = new File(args[1]);
			if (!f.exists()) {
				System.err.println("File does not exist.");
				System.exit(5);
			}
			try {
				sc.putFile(args[2], f, update);
			} catch (IOException e) {
				System.err.println("Could not upload your file.");
				System.exit(6);
			} catch (HTTPException e) {
				System.err.println("Error " + e.getHTTPCode() + ": "
						+ e.getHTTPErrorMessage());
			}
		} else {
			System.err.println("Wrong number of arguments.");
			System.exit(2);
		}
	}

}
