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
import javax.swing.WindowConstants;
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
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				MainWindow.this.quit();
			}
		});

		this.popup = new JPopupMenu(i.getString("fileActions"));
		this.deleteItem = new JMenuItem(i.getString("delete"));
		this.deleteItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						ServerConnector sc = ClientMain.getServerConnector();
						if (sc != null) {
							System.out.println("delete: "
									+ MainWindow.this.clickedCloudFile);
							try {
								I18n i = I18n.getInstance();
								MainWindow.this.clickedCloudFile.delete();
								JOptionPane.showMessageDialog(
										MainWindow.this,
										i.getString("deletionSuccessMessage")
												+ MainWindow.this.clickedCloudFile
														.getName(),
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
		this.downloadItem = new JMenuItem(i.getString("download"));
		this.downloadItem.addActionListener(new ActionListener() {
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
							System.out.println("download: "
									+ MainWindow.this.clickedCloudFile);
							try {
								I18n i = I18n.getInstance();
								System.out.println(fc.getSelectedFile()
										.getAbsolutePath());
								MainWindow.this.clickedCloudFile.downloadTo(fc
										.getSelectedFile());
								JOptionPane.showMessageDialog(
										MainWindow.this,
										i.getString("downloadSuccessMessage")
												+ MainWindow.this.clickedCloudFile
														.getName(),
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
		this.popup.add(this.deleteItem);
		this.popup.add(this.downloadItem);

		this.menuBar = new JMenuBar();

		this.fileMenu = new JMenu(i.getString("mainMenuTitle"));
		this.fileMenu.setMnemonic('c');

		this.connectItem = new JMenuItem(i.getString("connect") + "...");
		this.connectItem.setMnemonic('c');
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

		this.userCreationItem = new JMenuItem(i.getString("createUser"));
		this.userCreationItem.setMnemonic('u');
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

		this.disconnectItem = new JMenuItem(i.getString("disconnect"));
		this.disconnectItem.setMnemonic('d');
		this.disconnectItem.addActionListener(new ActionListener() {
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
		this.disconnectItem.setEnabled(false);

		this.closeItem = new JMenuItem(i.getString("quit") + "...");
		this.closeItem.setMnemonic('q');
		this.closeItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MainWindow.this.quit();
			}
		});

		this.fileMenu.add(this.connectItem);
		this.fileMenu.add(this.userCreationItem);
		this.fileMenu.add(this.disconnectItem);
		this.fileMenu.add(this.closeItem);

		this.uploadItem = new JMenuItem(i.getString("upload"));
		this.uploadItem.setMnemonic('u');
		this.uploadItem.addActionListener(new ActionListener() {
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
		this.uploadItem.setEnabled(false);

		this.refreshItem = new JMenuItem(i.getString("refresh"));
		this.refreshItem.setMnemonic('r');
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
		this.scrollPane.setBounds(0, 0, 600, 400);
		this.table.setFillsViewportHeight(true);

		this.getContentPane().add(this.scrollPane);

		this.setResizable(false);
		this.setJMenuBar(this.menuBar);

		this.setVisible(true);
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

	/**
	 * Removes all data from the table containing the file list.
	 */
	private void emptyTable() {
		this.refreshTable(new Object[][] { { "", "", "", null } });
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
