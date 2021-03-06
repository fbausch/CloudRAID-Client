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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

import de.dhbw_mannheim.cloudraid.client.api.CloudFile;
import de.dhbw_mannheim.cloudraid.client.api.DataPresenter;
import de.dhbw_mannheim.cloudraid.client.api.HTTPException;
import de.dhbw_mannheim.cloudraid.client.api.ServerConnector;

public class MainWindow extends JFrame implements DataPresenter {

	/**
	 * A {@link SwingWorker} implementation for downloading or deleting a
	 * {@link CloudFile}. The action that will be executed is dependent on the
	 * constructor used.
	 * 
	 * @author Florian Bausch
	 * 
	 */
	private class DownloadDeleteWorker extends SwingWorker<Exception, String> {
		private ServerConnector sc = ClientMain.getServerConnector();
		private CloudFile cf;
		private File to;
		private boolean download;
		private String msg;

		/**
		 * Creates a worker deleting a {@link CloudFile}.
		 * 
		 * @param delete
		 *            The {@link CloudFile} to be deleted.
		 */
		public DownloadDeleteWorker(CloudFile delete) {
			this.cf = delete;
			this.download = false;
			this.msg = I18n.getInstance().getString("deletionSuccessMessage")
					+ this.cf.getName();
			System.out.println("delete: " + this.cf);
		}

		/**
		 * Creates a worker downloading a {@link CloudFile}.
		 * 
		 * @param download
		 *            The {@link CloudFile} to be downloaded.
		 * @param to
		 *            The File were it is written to.
		 */
		public DownloadDeleteWorker(CloudFile download, File to) {
			this.cf = download;
			this.to = to;
			this.download = true;
			this.msg = I18n.getInstance().getString("downloadSuccessMessage")
					+ this.cf.getName();
			System.out.println("download: " + this.cf);
		}

		@Override
		protected Exception doInBackground() throws Exception {
			if (this.sc == null) {
				return new Exception();
			}
			try {
				if (this.download) {
					this.cf.downloadTo(this.to);
				} else {
					this.cf.delete();
				}
			} catch (IOException e1) {
				return e1;
			} catch (HTTPException e1) {
				return e1;
			}
			return null;
		}

		@Override
		protected void done() {
			Exception e = new Exception();
			try {
				e = get();
			} catch (InterruptedException e1) {
			} catch (ExecutionException e1) {
			}
			if (e == null) {
				JOptionPane.showMessageDialog(MainWindow.this, this.msg, I18n
						.getInstance().getString("success"),
						JOptionPane.INFORMATION_MESSAGE);
				try {
					this.sc.getFileList();
				} catch (Exception e1) {
				}
			} else if (e instanceof IOException) {
				showError((IOException) e);
			} else if (e instanceof HTTPException) {
				showError((HTTPException) e);
			}
			registerThread(false);
		}
	}

	private static final long serialVersionUID = 7714408179804838679L;
	private JMenuBar menuBar;
	private JMenu fileMenu = new JMenu();
	private JMenuItem connectItem = new JMenuItem(),
			disconnectItem = new JMenuItem(), closeItem = new JMenuItem(),
			uploadItem = new JMenuItem(), refreshItem = new JMenuItem(),
			deleteItem = new JMenuItem(), downloadItem = new JMenuItem(),
			userCreationItem = new JMenuItem(), changePwItem = new JMenuItem(),
			versionItem = new JMenuItem(), creditsItem = new JMenuItem();
	private JTable table;
	private JPopupMenu popup;
	private JScrollPane scrollPane;
	private CloudFile clickedCloudFile;

	private static final String[] HEADINGS = new String[] {
			I18n.getInstance().getString("name"),
			I18n.getInstance().getString("state"),
			I18n.getInstance().getString("date"), "File" };
	private Timer listUpdater;

	private int runningThreads = 0;

	public MainWindow() {
		super(I18n.getInstance().getString("mainWindowTitle"));
		this.setLayout(null);
		this.setBounds(50, 50, 600, 430);
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				MainWindow.this.quit();
			}
		});

		I18n i = I18n.getInstance();
		this.popup = new JPopupMenu(i.getString("fileActions"));
		this.deleteItem.setText(i.getString("delete"));
		this.deleteItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MainWindow.this.performDelete();
			};
		});
		this.downloadItem.setText(i.getString("download"));
		this.downloadItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MainWindow.this.performDownload();
			}
		});
		this.popup.add(this.deleteItem);
		this.popup.add(this.downloadItem);

		this.menuBar = new JMenuBar();

		setMnemonic(this.fileMenu, "mainMenuTitle", 'c');

		setMnemonic(this.connectItem, "connect", 'c', "...");
		this.connectItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MainWindow.this.emptyTable();
				ClientMain.resetServerConnection();
				new ConnectionDialog(MainWindow.this);
				ServerConnector sc = ClientMain.getServerConnector();
				if (sc != null) {
					try {
						sc.login();
						MainWindow.this.deActivateComponents(true);
					} catch (IOException e1) {
						MainWindow.this.showError(e1);
						ClientMain.resetServerConnection();
						return;
					} catch (HTTPException e1) {
						MainWindow.this.showError(e1);
						ClientMain.resetServerConnection();
						return;
					}
					try {
						sc.getFileList();
					} catch (IOException e1) {
						MainWindow.this.showError(e1);
					} catch (HTTPException e1) {
						MainWindow.this.showError(e1);
					}
				}
			}
		});

		setMnemonic(this.userCreationItem, "createUser", 'u', "...");
		this.userCreationItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MainWindow.this.emptyTable();
				ClientMain.resetServerConnection();
				UserCreationDialog ucd = new UserCreationDialog(MainWindow.this);
				ServerConnector sc = ClientMain.getServerConnector();
				if (sc != null) {
					try {
						sc.createUser(ucd.getPasswordConfirmation());
						JOptionPane.showMessageDialog(
								MainWindow.this,
								I18n.getInstance().getString(
										"userCreatedSuccessMessage"), I18n
										.getInstance().getString("success"),
								JOptionPane.INFORMATION_MESSAGE);
					} catch (IOException e1) {
						MainWindow.this.showError(e1);
						ClientMain.resetServerConnection();
						return;
					} catch (HTTPException e1) {
						MainWindow.this.showError(e1);
						ClientMain.resetServerConnection();
						return;
					}
				}
			}
		});

		setMnemonic(this.disconnectItem, "disconnect", 'd');
		this.disconnectItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					ServerConnector sc = ClientMain.getServerConnector();
					if (sc != null) {
						sc.logout();
					}
					MainWindow.this.deActivateComponents(false);
					MainWindow.this.emptyTable();
					ClientMain.resetServerConnection();
				} catch (IOException e1) {
					MainWindow.this.showError(e1);
				} catch (HTTPException e1) {
					MainWindow.this.showError(e1);
				}
			}
		});
		this.disconnectItem.setEnabled(false);

		setMnemonic(this.closeItem, "quit", 'q', "...");
		this.closeItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MainWindow.this.quit();
			}
		});

		setMnemonic(this.changePwItem, "changePw", 'p', "...");
		this.changePwItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new ChangePasswordDialog(MainWindow.this);
			}
		});
		this.changePwItem.setEnabled(false);

		setMnemonic(this.versionItem, "version", 'v', "...");
		this.versionItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				I18n i = I18n.getInstance();
				JOptionPane.showMessageDialog(
						MainWindow.this,
						"CloudRAID Client GUI " + ClientMain.VERSION + "\n"
								+ i.getString("supportedApiVersion") + ": "
								+ ServerConnector.API_VERSION,
						i.getString("version"), JOptionPane.INFORMATION_MESSAGE);
			}
		});

		setMnemonic(this.creditsItem, "credits", 'i', "...");
		this.creditsItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				I18n i = I18n.getInstance();
				JOptionPane.showMessageDialog(MainWindow.this,
						i.getString("creditsText"), i.getString("credits"),
						JOptionPane.INFORMATION_MESSAGE);
			}
		});

		this.fileMenu.add(this.connectItem);
		this.fileMenu.add(this.disconnectItem);
		this.fileMenu.add(this.userCreationItem);
		this.fileMenu.add(this.changePwItem);
		this.fileMenu.add(this.versionItem);
		this.fileMenu.add(this.creditsItem);
		this.fileMenu.add(this.closeItem);

		setMnemonic(this.uploadItem, "upload", 'u');
		this.uploadItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser();
				int state = fc.showOpenDialog(MainWindow.this);
				File f = fc.getSelectedFile();
				if (state == JFileChooser.APPROVE_OPTION) {
					ServerConnector sc = ClientMain.getServerConnector();
					try {
						sc.putFile(f.getName(), f, false);
						sc.getFileList();
					} catch (IOException e1) {
						MainWindow.this.showError(e1);
					} catch (HTTPException e1) {
						if (e1.getHTTPCode() == 409) {
							try {
								sc.putFile(f.getName(), f, true);
								sc.getFileList();
							} catch (IOException e2) {
								MainWindow.this.showError(e2);
							} catch (HTTPException e2) {
								MainWindow.this.showError(e2);
							}
						} else {
							MainWindow.this.showError(e1);
						}
					}
				}
			}
		});
		this.uploadItem.setEnabled(false);

		setMnemonic(this.refreshItem, "refresh", 'r');
		this.refreshItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ServerConnector sc = ClientMain.getServerConnector();
				if (sc != null) {
					try {
						sc.getFileList();
					} catch (IOException e1) {
						MainWindow.this.showError(e1);
					} catch (HTTPException e1) {
						MainWindow.this.showError(e1);
					}
				}
			}
		});
		this.refreshItem.setEnabled(false);

		this.menuBar.add(this.fileMenu);
		this.menuBar.add(this.uploadItem);
		this.menuBar.add(this.refreshItem);

		Object[][] content = new Object[][] { { "", "", "", null } };

		DefaultTableModel model = new DefaultTableModel(content,
				MainWindow.HEADINGS);
		this.table = new JTable(model) {
			private static final long serialVersionUID = 1110008116372652220L;

			@Override
			public boolean isCellEditable(int rowIndex, int colIndex) {
				return false; // Disallow the editing of any cell
			}
		};
		this.table.removeColumn(this.table.getColumnModel().getColumn(3));
		this.table.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() != MouseEvent.BUTTON3) {
					return;
				}
				int row = MainWindow.this.table.rowAtPoint(e.getPoint());
				if (row < 0) {
					return;
				}
				MainWindow.this.table.getSelectionModel().setSelectionInterval(
						row, row);
				Object o = MainWindow.this.table.getModel().getValueAt(row, 3);
				CloudFile file;
				if (o != null) {
					file = (CloudFile) o;
				} else {
					return;
				}
				MainWindow.this.clickedCloudFile = file;
				MainWindow.this.popup.show(e.getComponent(), e.getX(), e.getY());
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}
		});

		this.scrollPane = new JScrollPane(this.table);
		this.scrollPane.setBounds(0, 0, 598, 384);
		this.table.setFillsViewportHeight(true);

		this.getContentPane().add(this.scrollPane);

		this.setResizable(false);
		this.setJMenuBar(this.menuBar);

		this.setVisible(true);

		this.listUpdater = new Timer(30000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					ClientMain.getServerConnector().getFileList();
				} catch (Exception ignore) {
				}
			}
		});
		this.listUpdater.start();
	}

	/**
	 * Activates or deactivates components on the GUI depending on whether the
	 * client is logged in or not.
	 * 
	 * @param loggedIn
	 *            <code>true</code>, if the client is logged in to the server.
	 */
	private void deActivateComponents(boolean loggedIn) {
		this.connectItem.setEnabled(!loggedIn);
		this.disconnectItem.setEnabled(loggedIn);
		this.refreshItem.setEnabled(loggedIn);
		this.uploadItem.setEnabled(loggedIn);
		this.userCreationItem.setEnabled(!loggedIn);
		this.changePwItem.setEnabled(loggedIn);
	}

	@Override
	public void dispose() {
		this.listUpdater.stop();
		super.dispose();
	}

	/**
	 * Removes all data from the table containing the file list.
	 */
	private void emptyTable() {
		this.refreshTable(new Object[][] { { "", "", "", null } });
	}

	@Override
	public synchronized void giveFileList(Vector<CloudFile> fileList) {
		Object[][] data = new Object[fileList.size()][4];
		if (fileList.size() == 0) {
			this.emptyTable();
		}
		int i = 0;
		for (CloudFile cf : fileList) {
			Object[] d = new Object[] { cf.getName(), cf.getState(),
					cf.getLastModAsString(), cf };
			data[i++] = d;
		}
		this.refreshTable(data);
	}

	/**
	 * Performs the actual deletion of a {@link CloudFile};
	 */
	private void performDelete() {
		I18n i = I18n.getInstance();
		int state = JOptionPane.showConfirmDialog(this,
				i.getString("deleteConfirm"),
				i.getString("deleteConfirmTitle"), JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);
		if (state != JOptionPane.YES_OPTION) {
			return;
		}
		registerThread(true);
		new DownloadDeleteWorker(this.clickedCloudFile).execute();
	}

	/**
	 * Performs the actual download of a {@link CloudFile};
	 */
	private void performDownload() {
		CloudFile cf = this.clickedCloudFile;
		I18n i = I18n.getInstance();
		JFileChooser fc = new JFileChooser();
		fc.setSelectedFile(new File(cf.getName()));
		int state = fc.showSaveDialog(this);
		if (state != JFileChooser.APPROVE_OPTION) {
			return;
		}
		if (fc.getSelectedFile().exists()) {
			state = JOptionPane.showConfirmDialog(this,
					i.getString("overwriteConfirm"),
					i.getString("overwriteConfirmTitle"),
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (state != JOptionPane.YES_OPTION) {
				return;
			}
		}
		registerThread(true);
		new DownloadDeleteWorker(cf, fc.getSelectedFile()).execute();
	}

	/**
	 * Closes the MainWindow and exits the application, if no thread is running
	 * (e.g. for deleting and downloading).
	 */
	private void quit() {
		if (this.runningThreads > 0) {
			return;
		}
		I18n i = I18n.getInstance();
		int r = JOptionPane.showConfirmDialog(MainWindow.this,
				i.getString("exitConfirmationQuestion"),
				i.getString("exitConfirmationTitle"),
				JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
		if (r != JOptionPane.YES_OPTION) {
			return;
		}
		ServerConnector sc = ClientMain.getServerConnector();
		if (sc != null) {
			try {
				sc.logout();
			} catch (IOException ignore) {
			} catch (HTTPException e1) {
				showError(e1);
			}
		}
		this.dispose();
	}

	/**
	 * Refreshes the table containing the file list with the given data.
	 * 
	 * @param newContent
	 *            The new data.
	 */
	private void refreshTable(Object[][] newContent) {
		DefaultTableModel model = new DefaultTableModel(newContent,
				MainWindow.HEADINGS);
		this.table.setModel(model);
		this.table.removeColumn(this.table.getColumnModel().getColumn(3));
	}

	/**
	 * Registers a running thread or unregisters a finished thread and disables
	 * the fileMenuItem, if at least one thread is running.
	 * 
	 * @param register
	 *            true for registering; false for unregistering.
	 */
	private synchronized void registerThread(boolean register) {
		this.runningThreads += register ? 1 : -1;
		if (this.runningThreads == 0) {
			this.fileMenu.setEnabled(true);
		} else {
			this.fileMenu.setEnabled(false);
		}
	}

	/**
	 * Sets the text and the mnemonic of an {@link AbstractButton}. As text of
	 * the {@link AbstractButton} the localization text ({@link I18n}) is used.
	 * <em>s</em> is the regarding key. The mnemonic character is appended
	 * (within parentheses) to that String, if the character is not contained in
	 * the localization text.
	 * 
	 * @see #setMnemonic(AbstractButton, String, char, String).
	 * @param ab
	 *            The abstract button.
	 * @param s
	 *            The key of the String to be used in the
	 *            Messages_xx.properties.xml.
	 * @param mn
	 *            The mnemonic character to be set.
	 */
	private void setMnemonic(AbstractButton ab, String s, char mn) {
		this.setMnemonic(ab, s, mn, "");
	}

	/**
	 * Sets the text and the mnemonic of an {@link AbstractButton}. As text of
	 * the {@link AbstractButton} the localization text ({@link I18n}) is used.
	 * <em>s</em> is the regarding key. <em>add</em> is constant and is appended
	 * to the localization text. The mnemonic character is appended (within
	 * parentheses) to that String, if the character is not contained in the
	 * localization text.
	 * 
	 * @see #setMnemonic(AbstractButton, String, char)
	 * @param ab
	 *            The abstract button.
	 * @param s
	 *            The key of the String to be used in the
	 *            Messages_xx.properties.xml.
	 * @param mn
	 *            The mnemonic character to be set.
	 * @param add
	 *            A constant postfix, that is appended to the String retrieved
	 *            from the {@link I18n}.
	 */
	private void setMnemonic(AbstractButton ab, String s, char mn, String add) {
		s = I18n.getInstance().getString(s) + add;
		if (!s.toLowerCase().contains(String.valueOf(mn).toLowerCase())) {
			s += " (" + mn + ")";
		}
		ab.setText(s);
		ab.setMnemonic(mn);
	}

	/**
	 * Shows the error message via a pop-up when an {@link HTTPException} is
	 * thrown.
	 * 
	 * @param e
	 *            The {@link HTTPException}.
	 */
	private void showError(HTTPException e) {
		JOptionPane.showMessageDialog(this,
				e.getHTTPCode() + ": " + e.getHTTPErrorMessage(),
				I18n.getInstance().getString("error") + " " + e.getHTTPCode(),
				JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Shows the error message via a pop-up when an {@link IOException} is
	 * thrown.
	 * 
	 * @param e
	 *            The {@link IOException}.
	 */
	private void showError(IOException e) {
		JOptionPane.showMessageDialog(this, e.getMessage(), I18n.getInstance()
				.getString("connectionError"), JOptionPane.ERROR_MESSAGE);
	}
}
