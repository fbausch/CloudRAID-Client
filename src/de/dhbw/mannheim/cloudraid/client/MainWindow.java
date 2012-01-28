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

package de.dhbw.mannheim.cloudraid.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class MainWindow extends JFrame {

	private static final long serialVersionUID = 7714408179804838679L;

	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenuItem connectItem, closeItem, uploadItem, refreshItem;
	private JTable table;
	private JScrollPane scrollPane;
	private String[] headings = new String[] { "Name", "Path", "Date" };

	public MainWindow() {
		super("CloudRAID");
		this.setLayout(null);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setBounds(50, 50, 300, 230);

		menuBar = new JMenuBar();

		fileMenu = new JMenu("CloudRAID");

		connectItem = new JMenuItem("Connect...");
		connectItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new ConnectionDialog(MainWindow.this);
				if (ClientMain.getServerConnection() != null) {
					ServerConnector sc = new ServerConnector(ClientMain
							.getServerConnection());
					sc.getFileList();
				} else {
					MainWindow.this.emptyTable();
				}
			}
		});

		closeItem = new JMenuItem("Close");

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

		DefaultTableModel model = new DefaultTableModel(content, this.headings);
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
	}

	private void emptyTable() {
		this.refreshTable(new String[][] { { "", "", "" } });
	}

	public void refreshTable(Object[][] newContent) {
		DefaultTableModel model = new DefaultTableModel(newContent, this.headings);
		table.setModel(model);
	}

}
