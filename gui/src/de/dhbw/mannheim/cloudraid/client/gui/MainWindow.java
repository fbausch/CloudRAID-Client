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

package de.dhbw.mannheim.cloudraid.client.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import de.dhbw.mannheim.cloudraid.client.api.CloudFile;
import de.dhbw.mannheim.cloudraid.client.api.DataPresenter;
import de.dhbw.mannheim.cloudraid.client.api.HTTPException;
import de.dhbw.mannheim.cloudraid.client.api.ServerConnector;

public class MainWindow extends JFrame implements DataPresenter {

	private static final long serialVersionUID = 7714408179804838679L;

	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenuItem connectItem, closeItem, uploadItem, refreshItem;
	private JTable table;
	private JScrollPane scrollPane;
	private static final String[] HEADINGS = new String[] { "Name", "Path",
			"Date" };

	public MainWindow() {
		super("CloudRAID Client GUI");
		this.setLayout(null);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setBounds(50, 50, 300, 230);

		menuBar = new JMenuBar();

		fileMenu = new JMenu("CloudRAID");

		connectItem = new JMenuItem("Connect...");
		connectItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ClientMain.resetServerConnection();
				ArrayList<CloudFile> fileList = null;
				new ConnectionDialog(MainWindow.this);
				ServerConnector sc = ClientMain.getServerConnector();
				if (sc != null) {
					try {
						sc.login();
						fileList = sc.getFileList();
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(MainWindow.this,
								e1.getMessage(),
								"Error while connecting to the server.",
								JOptionPane.ERROR_MESSAGE);
					} catch (HTTPException e1) {
						JOptionPane.showMessageDialog(MainWindow.this,
								e1.getHTTPErrorMessage(), e1.getHTTPCode()
										+ ": Error", ERROR);
					}

				}
				MainWindow.this.emptyTable();
				if (fileList != null) {
					String[][] data = new String[fileList.size()][3];
					for (int i = 0; i < fileList.size(); i++) {
						CloudFile cf = fileList.get(i);
						String[] d = new String[] { cf.getName(), cf.getPath(),
								new Date(cf.getLastMod()).toString() };
						data[i] = d;
					}
					MainWindow.this.giveFileList(data);
				}
			}
		});

		closeItem = new JMenuItem("Close");
		closeItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int r = JOptionPane.showConfirmDialog(MainWindow.this,
						"Are you sure to log out?", "Confirm exit",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.INFORMATION_MESSAGE);
				if (r != JOptionPane.YES_OPTION) {
					return;
				}
				ServerConnector sc = ClientMain.getServerConnector();
				if (sc != null) {
					try {
						sc.logout();
					} catch (IOException ignore) {
					} catch (HTTPException e1) {
						JOptionPane.showMessageDialog(MainWindow.this,
								e1.getHTTPErrorMessage(), e1.getHTTPCode()
										+ ": Error", JOptionPane.ERROR_MESSAGE);
					}
				}
				MainWindow.this.dispose();
			}
		});

		fileMenu.add(connectItem);
		fileMenu.add(closeItem);

		uploadItem = new JMenuItem("Upload a file");
		uploadItem.setEnabled(false);

		refreshItem = new JMenuItem("Refresh list");
		refreshItem.setEnabled(false);

		menuBar.add(fileMenu);
		menuBar.add(uploadItem);
		menuBar.add(refreshItem);

		String[][] content = new String[][] { { "name", "path", "date" } };

		DefaultTableModel model = new DefaultTableModel(content, HEADINGS);
		table = new JTable(model) {
			private static final long serialVersionUID = 1110008116372652220L;

			public boolean isCellEditable(int rowIndex, int colIndex) {
				return false; // Disallow the editing of any cell
			}
		};

		scrollPane = new JScrollPane(table);
		scrollPane.setBounds(0, 0, 300, 200);
		table.setFillsViewportHeight(true);

		this.getContentPane().add(scrollPane);

		this.setResizable(false);
		this.setJMenuBar(menuBar);

		ClientMain.registerDataPresenter(this);

		this.setVisible(true);
	}

	private void emptyTable() {
		this.refreshTable(new String[][] { { "", "", "" } });
	}

	private void refreshTable(Object[][] newContent) {
		DefaultTableModel model = new DefaultTableModel(newContent, HEADINGS);
		table.setModel(model);
	}

	@Override
	public void giveFileList(Object[][] fileList) {
		this.refreshTable(fileList);
	}

}
