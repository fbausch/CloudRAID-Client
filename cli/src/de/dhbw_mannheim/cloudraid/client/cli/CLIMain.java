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

import static java.io.File.separator;

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

	/**
	 * A cache containing the {@link CloudFile}s on the CloudRAID server (at the
	 * moment of the last request).
	 */
	private static Vector<CloudFile> fileList = new Vector<CloudFile>();

	/**
	 * The {@link ServerConnector} used by the client.
	 */
	private static ServerConnector sc = null;

	/**
	 * The version of this command-line client.
	 */
	public static final String VERSION = "1.0.0-alpha.1";

	/**
	 * The working directory of this application. Will be used to download
	 * {@link CloudFile}s to.
	 */
	private static File workingDir = new File("./");

	/**
	 * The String representation of the working directory {@link File} object.
	 */
	private static String workDir;

	/**
	 * Starts the interactive console.
	 * 
	 * @throws IncompatibleApiVersionException
	 */
	private static void interactive() throws IncompatibleApiVersionException {
		CLIMain.printLicense();
		System.out.println("Type \"help\" for help.");
		Console c = System.console();
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String[] commands;
		Runnable lister = new Runnable() {
			@Override
			public void run() {
				while (true) {
					if (CLIMain.sc != null) {
						try {
							Thread.sleep(1000L * 60 * 60);
						} catch (InterruptedException e) {
						}
						try {
							CLIMain.fileList = CLIMain.sc.getFileList();
						} catch (IOException ignore) {
						} catch (HTTPException ignore) {
						} catch (NullPointerException ignore) {
						}
					} else {
						return;
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
			} else if ("help".equals(command)) {
				CLIMain.printUsage();
				continue;
			} else if ("version".equals(command)) {
				System.out.println("CloudRAID-Client " + CLIMain.VERSION);
				System.out.println("Supported CloudRAID server API version: "
						+ ServerConnector.API_VERSION);
			} else if (command.startsWith("login ") && CLIMain.sc == null) {
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
						CLIMain.sc = new ServerConnector(new ServerConnection(
								commands[2], commands[1], pw,
								Short.parseShort(commands[3])));
						CLIMain.sc.login();
						listerThread = new Thread(lister);
						listerThread.start();
					} catch (HTTPException e) {
						System.out.println("Could not log in. Try again.");
						System.out.println(e.getHTTPCode() + ": "
								+ e.getHTTPErrorMessage());
						CLIMain.sc = null;
					} catch (IOException e) {
						System.out
								.println("No connection to server. Try again.");
						CLIMain.sc = null;
					} catch (NumberFormatException e) {
						System.out.println("Invalid port number. Try again.");
						CLIMain.sc = null;
					}
				}
			} // Check for user creation
			else if (command.startsWith("create ") && CLIMain.sc == null) {
				commands = command.split(" ", 4);
				if (commands.length != 4) {
					System.out.println("Invalid syntax.");
				} else {
					System.out.println("Enter password:");
					String pw = "";
					String pw2 = "";
					if (c == null) {
						try {
							pw = in.readLine();
							System.out.println("Confirm password:");
							pw2 = in.readLine();
						} catch (IOException e) {
							System.out
									.println("Error while reading the password.");
							return;
						}
					} else {
						pw = String.valueOf(c.readPassword());
						System.out.println("Confirm password:");
						pw2 = String.valueOf(c.readPassword());
					}
					try {
						CLIMain.sc = new ServerConnector(new ServerConnection(
								commands[2], commands[1], pw,
								Short.parseShort(commands[3])));
						CLIMain.sc.createUser(pw2);
					} catch (HTTPException e) {
						System.out
								.println("Could not create user account. Try again.");
						System.out.println(e.getHTTPCode() + ": "
								+ e.getHTTPErrorMessage());
						CLIMain.sc = null;
					} catch (IOException e) {
						System.out
								.println("No connection to server. Try again.");
						CLIMain.sc = null;
					} catch (NumberFormatException e) {
						System.out.println("Invalid port number. Try again.");
						CLIMain.sc = null;
					}
				}
			} else if ("exit".equals(command) || "quit".equals(command)) {
				System.out.println("Exit CloudRAID client.");
				try {
					if (CLIMain.sc != null) {
						CLIMain.sc.logout();
					}
				} catch (HTTPException ignore) {
				} catch (IOException ignore) {
				}
				break;
			} else if ("license".equals(command)) {
				CLIMain.printLicense();
			} else if (CLIMain.sc == null) {
				System.out.println("Invalid command.");
				CLIMain.printUsage();
			} else {
				// Check for logout
				if ("logout".equals(command)) {
					try {
						CLIMain.sc.logout();
						listerThread.interrupt();
					} catch (Exception ignore) {
					}
					CLIMain.sc = null;
				} // Check for list command
				else if ("list".equals(command) || "ls".equals(command)) {
					try {
						Vector<CloudFile> list = CLIMain.sc.getFileList();
						if (list.size() == 0) {
							System.out
									.println("There are no files on the CloudRAID server.");
						}
						for (CloudFile file : list) {
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
							for (CloudFile file : CLIMain.fileList) {
								if (file.getName().equals(commands[1])) {
									file.downloadTo(new File(workDir
											+ commands[1]));
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
					upload(command.split(" ", 3), false, CLIMain.sc);
				} // Check for update of file
				else if (command.startsWith("update ")) {
					upload(command.split(" ", 3), true, CLIMain.sc);
				} // Check for deletion of file {
				else if (command.startsWith("delete ")
						|| command.startsWith("rm ")) {
					commands = command.split(" ", 2);
					if (commands.length != 2) {
						System.out.println("Invalid syntax.");
					} else {
						try {
							CLIMain.sc.deleteFile(commands[1]);
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
						CLIMain.sc.changePassword(pw, pw2);
					} catch (IOException e) {
						System.out.println("Could not change password.");
					} catch (HTTPException e) {
						System.out.println(e.getHTTPCode() + ": "
								+ e.getHTTPErrorMessage());
					}
				} // Check for server information
				else if ("server".equals(command)) {
					try {
						System.out.println(CLIMain.sc.getApiInfo());
					} catch (IOException e) {
						System.out.println("Could not connect to server.");
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
	 *            The first element of the args array is interpreted as the
	 *            working directory of the application. If no path is specified
	 *            or the specified path does not exist, the current directory is
	 *            used.
	 */
	public static void main(String[] args) {
		if (args.length >= 1) {
			File tmp = new File(args[0]);
			if (tmp.exists() && tmp.isDirectory()) {
				workingDir = tmp;
			}
		}
		workDir = workingDir.getAbsolutePath() + separator;
		// Start interactive mode
		try {
			CLIMain.interactive();
			System.exit(0);
		} catch (IncompatibleApiVersionException e) {
			System.out.println("Incompatible API version.");
			System.exit(1);
		}
	}

	/**
	 * Sends license information to stdout.
	 */
	private static void printLicense() {
		System.out.println("CloudRAID-Client " + CLIMain.VERSION);
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
		System.out.println("* help");
		System.out.println("  - prints this help.\n");
		System.out.println("* license");
		System.out.println("  - prints the CloudRAID license information.\n");
		System.out.println("* version");
		System.out.println("  - prints the version of this software.\n");
		System.out.println("* exit|quit");
		System.out.println("  - quits the CloudRAID command-line client.\n");
		System.out.println("Can be used after startup or after logout:");
		System.out.println("* login <user> <server> <port>");
		System.out.println("  - creates a session with a CloudRAID server.\n");
		System.out.println("* create <user> <server> <port>");
		System.out.println("  - creates a user on a CloudRAID server.\n");
		System.out.println("Can be used after login:");
		System.out.println("* logout");
		System.out
				.println("  - closes an existing session with a CloudRAID server.\n");
		System.out.println("* list|ls");
		System.out
				.println("  - lists all files of the current user on the server.\n");
		System.out.println("* get <filename>");
		System.out.println("  - downloads a file from the server.\n");
		System.out.println("* delete|rm <filename>");
		System.out.println("  - deletes a file on the server.\n");
		System.out.println("* put <filename> <path_to_file>");
		System.out.println("  - uploads a file to the server.\n");
		System.out.println("* update <filename> <path_to_file>");
		System.out
				.println("  - uploads _and_ overwrites an existing file on the server.\n");
		System.out.println("* changepw");
		System.out
				.println("  - changes the password for the user currently logged in.\n");
		System.out.println("* server");
		System.out.println("  - prints server information.");
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
