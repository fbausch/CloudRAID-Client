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
import java.util.Vector;

import de.dhbw_mannheim.cloudraid.client.api.CloudFile;
import de.dhbw_mannheim.cloudraid.client.api.HTTPException;
import de.dhbw_mannheim.cloudraid.client.api.IncompatibleApiVersionException;
import de.dhbw_mannheim.cloudraid.client.api.ServerConnection;
import de.dhbw_mannheim.cloudraid.client.api.ServerConnector;

/**
 * A class using the CloudRAID-Client core to provide a command-line interface
 * for the CloudRAID server application.
 * 
 * @author Florian Bausch
 * 
 */
public class CLIMain {

	private static Vector<CloudFile> fileList = new Vector<CloudFile>();

	private static ServerConnector sc = null;
	private static final String VERSION = "0.1";

	/**
	 * Starts the interactive console.
	 * 
	 * @throws IncompatibleApiVersionException
	 */
	private static void interactive() throws IncompatibleApiVersionException {
		CLIMain.printLicense();
		Console c = System.console();
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String[] commands;
		Runnable lister = new Runnable() {
			@Override
			public void run() {
				while (true) {
					if (sc != null) {
						try {
							fileList = sc.getFileList();
							System.out.println("Lister got file list.");
						} catch (IOException ignore) {
						} catch (HTTPException ignore) {
						} catch (NullPointerException ignore) {
						}
					} else {
						System.out.println("Stopped lister.");
						return;
					}
					try {
						Thread.sleep(1000L * 60 * 60);
					} catch (InterruptedException e) {
						System.out.println("Lister interrupted.");
					}
				}
			}
		};
		Thread listerThread = null;
		while (true) {
			System.out.print("craid> ");
			String command = "";
			try {
				command = in.readLine().trim();
			} catch (IOException e1) {
				System.out.println("Error while reading your command.");
				return;
			}
			if ("".equals(command)) {
				// Empty command. Do nothing.
			} else if (command.startsWith("login ") && sc == null) {
				commands = command.split(" ", 4);
				if (commands.length != 4) {
					System.out.println("Invalid syntax.");
				} else {
					System.out.println("Enter password:");
					String pw = "";
					if (c == null) {
						try {
							pw = in.readLine();
						} catch (IOException e) {
							System.out
									.println("Error while reading the password.");
							return;
						}
					} else {
						pw = String.valueOf(c.readPassword());
					}
					try {
						sc = new ServerConnector(new ServerConnection(
								commands[2], commands[1], pw,
								Short.parseShort(commands[3])));
						sc.login();
						listerThread = new Thread(lister);
						listerThread.start();
					} catch (HTTPException e) {
						System.out.println("Could not log in. Try again.");
						System.out.println(e.getHTTPCode() + ": "
								+ e.getHTTPErrorMessage());
						sc = null;
					} catch (IOException e) {
						System.out
								.println("No connection to server. Try again.");
						sc = null;
					} catch (NumberFormatException e) {
						System.out.println("Invalid port number.");
						sc = null;
					}
				}
			} else if ("exit".equals(command) || "quit".equals(command)) {
				System.out.println("Exit CloudRAID client.");
				try {
					if (sc != null) {
						sc.logout();
					}
				} catch (HTTPException ignore) {
				} catch (IOException ignore) {
				}
				break;
			} else if ("license".equals(command)) {
				CLIMain.printLicense();
			} else if (sc == null) {
				System.out.println("Invalid command.");
				CLIMain.printUsage();
			} else {
				// Check for logout
				if ("logout".equals(command)) {
					try {
						sc.logout();
						listerThread.interrupt();
					} catch (Exception ignore) {
					}
					sc = null;
				} // Check for list command
				else if ("list".equals(command)) {
					try {
						for (CloudFile file : sc.getFileList()) {
							System.out.println(file.getName() + " ("
									+ file.getHashedName() + "), "
									+ file.getState() + ", "
									+ file.getLastModAsString());
						}
					} catch (HTTPException e) {
						System.out.println(e.getHTTPCode() + ": "
								+ e.getHTTPErrorMessage());
					} catch (IOException e) {
						System.out.println("Could not connect to server.");
					}
				} // Check for download of file
				else if (command.startsWith("get ")) {
					commands = command.split(" ", 2);
					if (commands.length != 2) {
						System.out.println("Invalid syntax.");
					} else {
						try {
							boolean found = false;
							for (CloudFile file : fileList) {
								if (file.getName().equals(commands[1])) {
									file.downloadTo(new File(commands[1]));
									found = true;
									break;
								}
							}
							if (!found) {
								System.err
										.println("File not found in file list.");
							}
						} catch (HTTPException e) {
							System.out.println(e.getHTTPCode() + ": "
									+ e.getHTTPErrorMessage());
						} catch (IOException e) {
							System.out.println("Could not connect to server.");
						}
					}
				} // Check for upload of file
				else if (command.startsWith("put ")) {
					upload(command.split(" ", 3), false, sc);
				} // Check for update of file
				else if (command.startsWith("update ")) {
					upload(command.split(" ", 3), true, sc);
				} // Check for deletion of file {
				else if (command.startsWith("delete ")) {
					commands = command.split(" ", 2);
					if (commands.length != 2) {
						System.out.println("Invalid syntax.");
					} else {
						try {
							sc.deleteFile(commands[1]);
						} catch (HTTPException e) {
							System.out.println(e.getHTTPCode() + ": "
									+ e.getHTTPErrorMessage());
						} catch (IOException e) {
							System.out.println("Could not connect to server.");
						}
					}
				} // Check for changing the password
				else if ("changepw".equals(command)) {
					try {
						String pw, pw2;
						System.out.println("Enter new password:");
						if (c == null) {
							pw = in.readLine();
						} else {
							pw = String.valueOf(c.readPassword());
						}
						System.out.println("Confirm new password:");
						if (c == null) {
							pw2 = in.readLine();
						} else {
							pw2 = String.valueOf(c.readPassword());
						}
						sc.changePassword(pw, pw2);
					} catch (IOException e) {
						System.out.println("Could not change password.");
					} catch (HTTPException e) {
						System.out.println(e.getHTTPCode() + ": "
								+ e.getHTTPErrorMessage());
					}
				} else {
					System.out.println("Invalid command.");
					CLIMain.printUsage();
				}
			}
			if (listerThread != null) {
				listerThread.interrupt();
			}
		}
	}

	/**
	 * Creates an interactive console for CloudRAID.
	 * 
	 * @param args
	 *            Not interpreted.
	 */
	public static void main(String[] args) {
		// Start interactive mode
		try {
			CLIMain.interactive();
			System.exit(0);
		} catch (IncompatibleApiVersionException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Sends license information to stdout.
	 */
	private static void printLicense() {
		System.out.println("CloudRAID-Client " + VERSION);
		System.out.println("This software and the corresponding server"
				+ " application is published under the Apache License 2.0 "
				+ "(https://www.apache.org/licenses/LICENSE-2.0.txt).\n");
		System.out.println("For source code and documentation visit:");
		System.out
				.println(" * Server: https://github.com/Markush2010/CloudRAID/");
		System.out
				.println(" * Client: https://github.com/fbausch/CloudRAID-Client/\n");
		System.out.println("Authors: Florian Bausch and Markus Holtermann\n");
	}

	/**
	 * Prints the usage of this command-line client to stdout.
	 */
	private static void printUsage() {
		System.err.flush();
		System.out.println("Usage of CloudRAID command-line client:\n");
		System.out.println("Can always be used:");
		System.out.println("* license");
		System.out.println("  - prints the CloudRAID license information.\n");
		System.out.println("Can be used after startup or after logout:");
		System.out.println("* login <user> <server> <port>");
		System.out.println("  - creates a session with a CloudRAID server.\n");
		System.out.println("Can be used after login:");
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
		System.out.println("* put <filename> <path_to_file>");
		System.out.println("  - uploads a file to the server.\n");
		System.out.println("* update <filename> <path_to_file>");
		System.out
				.println("  - uploads _and_ overwrites a file on the server.\n");
		System.out.println("* changepw");
		System.out
				.println("  - changes the password for the user currently logged in.");
	}

	/**
	 * Executes the upload command and handles {@link Exception}s.
	 * 
	 * @param args
	 *            The argument Array.
	 * @param update
	 *            indicates, if new upload or update.
	 * @param sc
	 *            The {@link ServerConnector} holding the connection
	 *            information.
	 */
	private static void upload(String[] args, boolean update, ServerConnector sc) {
		if (args.length == 3) {
			File f = new File(args[2]);
			if (!f.exists()) {
				System.out.println("File does not exist. ("
						+ f.getAbsolutePath() + ")");
				return;
			}
			try {
				sc.putFile(args[1], f, update);
			} catch (IOException e) {
				System.out.println("Could not upload your file.");
			} catch (HTTPException e) {
				System.out.println(e.getHTTPCode() + ": "
						+ e.getHTTPErrorMessage());
			}
		} else {
			System.out.println("Invalid syntax.");
		}
	}
}
