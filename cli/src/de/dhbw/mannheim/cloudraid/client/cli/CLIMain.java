package de.dhbw.mannheim.cloudraid.client.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Vector;

import de.dhbw.mannheim.cloudraid.client.api.CloudFile;
import de.dhbw.mannheim.cloudraid.client.api.HTTPException;
import de.dhbw.mannheim.cloudraid.client.api.ServerConnection;
import de.dhbw.mannheim.cloudraid.client.api.ServerConnector;

public class CLIMain {

	public static void main(String[] args) {
		// Check for arguments
		if (args.length == 0) {
			System.err.println("No arguments given.");
			System.exit(1);
		}
		// Check for login
		if ("login".equals(args[0])) {
			if (args.length == 4) {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						System.in));
				System.out.println("Enter password:");
				try {
					String pw = in.readLine();
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
				} catch (MalformedURLException e) {
					System.err.println("Invalid URL.");
				} catch (IOException e) {
					System.err.println("Could not log in.");
				} catch (HTTPException e) {
					System.err.println("Error " + e.getHTTPCode() + ": "
							+ e.getHTTPErrorMessage());
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
		if ("logout".equals(args[0])) {
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
		if ("list".equals(args[0])) {
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
		if ("get".equals(args[0])) {
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
		if ("delete".equals(args[0])) {
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
	}

}
