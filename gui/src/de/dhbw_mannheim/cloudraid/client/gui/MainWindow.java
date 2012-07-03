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
import java.io.IOException;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import de.dhbw_mannheim.cloudraid.client.api.CloudFile;
import de.dhbw_mannheim.cloudraid.client.api.DataPresenter;
import de.dhbw_mannheim.cloudraid.client.api.HTTPException;
import de.dhbw_mannheim.cloudraid.client.api.ServerConnector;

public class MainWindow extends JFrame implements DataPresenter {

	private static final long serialVersionUID = 7714408179804838679L;

	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenuItem connectItem, disconnectItem, closeItem, uploadItem,
			refreshItem, deleteItem, downloadItem, userCreationItem;
	private JTable table;
	private JPopupMenu popup;
	private JScrollPane scrollPane;
	private CloudFile clickedCloudFile;
	private static final String[] HEADINGS = new String[] {
			I18n.getInstance().getString("name"),
			I18n.getInstance().getString("state"),
			I18n.getInstance().getString("date"), "File" };

	public MainWindow() {
		super(I18n.getInstance().getString("mainWindowTitle"));
		I18n i = I18n.getInstance();
		this.setLayout(null);
		this.setBounds(50, 50, 600, 430);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				MainWindow.this.quit();
			}
		});

		popup = new JPopupMenu(i.getString("fileActions"));
		deleteItem = new JMenuItem(i.getString("delete"));
		deleteItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						ServerConnector sc = ClientMain.getServerConnector();
						if (sc != null) {
							System.out.println("delete: " + clickedCloudFile);
							try {
								I18n i = I18n.getInstance();
								clickedCloudFile.delete();
								JOptionPane.showMessageDialog(MainWindow.this,
										i.getString("deletionSuccessMessage")
												+ clickedCloudFile.getName(),
										i.getString("success"),
										JOptionPane.INFORMATION_MESSAGE);
								sc.getFileList();
							} catch (IOException e1) {
								MainWindow.this.showError(e1);
							} catch (HTTPException e1) {
								MainWindow.this.showError(e1);
							}
						}
					}
				});
			};
		});
		downloadItem = new JMenuItem(i.getString("download"));
		downloadItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						JFileChooser fc = new JFileChooser();
						int state = fc.showSaveDialog(MainWindow.this);
						if (state != JFileChooser.APPROVE_OPTION) {
							return;
						}
						ServerConnector sc = ClientMain.getServerConnector();
						if (sc != null) {
							System.out.println("download: " + clickedCloudFile);
							try {
								I18n i = I18n.getInstance();
								System.out.println(fc.getSelectedFile()
										.getAbsolutePath());
								clickedCloudFile.downloadTo(fc
										.getSelectedFile());
								JOptionPane.showMessageDialog(MainWindow.this,
										i.getString("downloadSuccessMessage")
												+ clickedCloudFile.getName(),
										i.getString("success"),
										JOptionPane.INFORMATION_MESSAGE);
							} catch (IOException e1) {
								MainWindow.this.showError(e1);
							} catch (HTTPException e1) {
								MainWindow.this.showError(e1);
							}
						}
					}
				});
			}
		});
		popup.add(deleteItem);
		popup.add(downloadItem);

		menuBar = new JMenuBar();

		fileMenu = new JMenu(i.getString("mainMenuTitle"));
		fileMenu.setMnemonic('c');

		connectItem = new JMenuItem(i.getString("connect") + "...");
		connectItem.setMnemonic('c');
		connectItem.addActionListener(new ActionListener() {
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

		userCreationItem = new JMenuItem(i.getString("createUser"));
		userCreationItem.setMnemonic('u');
		userCreationItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MainWindow.this.emptyTable();
				ClientMain.resetServerConnection();
				UserCreationDialog ucd = new UserCreationDialog(MainWindow.this);
				ServerConnector sc = ClientMain.getServerConnector();
				if (sc != null) {
					try {
						sc.createUser(ucd.getPasswordConfirmation());
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

		disconnectItem = new JMenuItem(i.getString("disconnect"));
		disconnectItem.setMnemonic('d');
		disconnectItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					ClientMain.getServerConnector().logout();
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
		disconnectItem.setEnabled(false);

		closeItem = new JMenuItem(i.getString("quit") + "...");
		closeItem.setMnemonic('q');
		closeItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MainWindow.this.quit();
			}
		});

		fileMenu.add(connectItem);
		fileMenu.add(userCreationItem);
		fileMenu.add(disconnectItem);
		fileMenu.add(closeItem);

		uploadItem = new JMenuItem(i.getString("upload"));
		uploadItem.setMnemonic('u');
		uploadItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser();
				int state = fc.showOpenDialog(MainWindow.this);
				if (state == JFileChooser.APPROVE_OPTION) {
					ServerConnector sc = ClientMain.getServerConnector();
					try {
						sc.putFile(fc.getSelectedFile().getName(),
								fc.getSelectedFile(), false);
						sc.getFileList();
					} catch (IOException e1) {
						MainWindow.this.showError(e1);
					} catch (HTTPException e1) {
						if (e1.getHTTPCode() == 409) {
							try {
								sc.putFile(fc.getSelectedFile().getName(),
										fc.getSelectedFile(), true);
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
		uploadItem.setEnabled(false);

		refreshItem = new JMenuItem(i.getString("refresh"));
		refreshItem.setMnemonic('r');
		refreshItem.addActionListener(new ActionListener() {
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
		refreshItem.setEnabled(false);

		menuBar.add(fileMenu);
		menuBar.add(uploadItem);
		menuBar.add(refreshItem);

		Object[][] content = new Object[][] { { "", "", "", null } };

		DefaultTableModel model = new DefaultTableModel(content, HEADINGS);
		table = new JTable(model) {
			private static final long serialVersionUID = 1110008116372652220L;

			public boolean isCellEditable(int rowIndex, int colIndex) {
				return false; // Disallow the editing of any cell
			}
		};
		table.removeColumn(table.getColumnModel().getColumn(3));
		table.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() != MouseEvent.BUTTON3) {
					return;
				}
				int row = MainWindow.this.table.rowAtPoint(e.getPoint());
				if (row < 0) {
					return;
				}
				Object o = MainWindow.this.table.getModel()
						.getValueAt(row, 3);
				CloudFile file;
				if (o != null) {
					file = (CloudFile) o;
				} else {
					return;
				}
				MainWindow.this.clickedCloudFile = file;
				MainWindow.this.popup.show(e.getComponent(), e.getX(), e.getY());
			}
		});

		scrollPane = new JScrollPane(table);
		scrollPane.setBounds(0, 0, 600, 400);
		table.setFillsViewportHeight(true);

		this.getContentPane().add(scrollPane);

		this.setResizable(false);
		this.setJMenuBar(menuBar);

		this.setVisible(true);
	}

	/**
	 * Removes all data from the table containing the file list.
	 */
	private void emptyTable() {
		this.refreshTable(new Object[][] { { "", "", "", null } });
	}

	/**
	 * Refreshes the table containing the file list with the given data.
	 * 
	 * @param newContent
	 *            The new data.
	 */
	private void refreshTable(Object[][] newContent) {
		DefaultTableModel model = new DefaultTableModel(newContent, HEADINGS);
		table.setModel(model);
		this.table.removeColumn(this.table.getColumnModel().getColumn(3));
	}

	@Override
	public void giveFileList(Vector<CloudFile> fileList) {
		Object[][] data = new Object[fileList.size()][4];
		int i = 0;
		for (CloudFile cf : fileList) {
			Object[] d = new Object[] { cf.getName(), cf.getState(),
					cf.getLastModAsString(), cf };
			data[i++] = d;
		}
		this.refreshTable(data);
	}

	/**
	 * Closes the MainWindow and exits the application.
	 */
	private void quit() {
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
		MainWindow.this.dispose();
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
	}
}
