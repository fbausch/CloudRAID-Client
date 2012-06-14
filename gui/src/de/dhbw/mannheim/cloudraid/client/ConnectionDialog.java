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

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.MalformedURLException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class ConnectionDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5030263674078790548L;

	private JButton connectButton, abortButton;
	private JTextField hostAddress, portNumber, userName;
	private JPasswordField passWord;
	private JLabel hostAddressLabel, portNumberLabel, userNameLabel,
			passWordLabel;

	private KeyListener returnKeyListener = new KeyListener() {
		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				if (e.getSource() != abortButton) {
					ConnectionDialog.this.connect();
				} else {
					ConnectionDialog.this.abort();
				}
			} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				ConnectionDialog.this.abort();
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}
	};

	public ConnectionDialog(MainWindow parent) {
		super(parent, "Connection");
		this.setModalityType(ModalityType.APPLICATION_MODAL);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setResizable(false);
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(5, 2, 10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		/**
		 * connect button
		 */
		connectButton = new JButton("Connect");
		connectButton.addKeyListener(returnKeyListener);
		connectButton.setPreferredSize(new Dimension(150, 30));
		connectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ConnectionDialog.this.connect();
			}
		});

		/**
		 * abort button
		 */
		abortButton = new JButton("Cancel");
		abortButton.addKeyListener(returnKeyListener);
		abortButton.setPreferredSize(new Dimension(150, 30));
		abortButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ConnectionDialog.this.abort();
			}
		});

		/**
		 * textfield for host
		 */
		hostAddressLabel = new JLabel("Server");
		hostAddressLabel.setLabelFor(hostAddress);
		hostAddress = new JTextField("http://localhost");
		hostAddress.addKeyListener(returnKeyListener);
		hostAddress.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent arg0) {
				ConnectionDialog.this.hostAddress.selectAll();
			}

			@Override
			public void focusLost(FocusEvent e) {
				ConnectionDialog.this.hostAddress.select(0, 0);
			}
		});

		/**
		 * textfield for the port
		 */
		portNumberLabel = new JLabel("Port");
		portNumberLabel.setLabelFor(portNumber);
		portNumber = new JTextField("1234");
		portNumber.addKeyListener(returnKeyListener);
		portNumber.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent arg0) {
				ConnectionDialog.this.portNumber.selectAll();
			}

			@Override
			public void focusLost(FocusEvent e) {
				ConnectionDialog.this.portNumber.select(0, 0);
			}
		});

		/**
		 * textfield for the username
		 */
		userNameLabel = new JLabel("User");
		userNameLabel.setLabelFor(userName);
		userName = new JTextField("username");
		userName.addKeyListener(returnKeyListener);
		userName.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent arg0) {
				ConnectionDialog.this.userName.selectAll();
			}

			@Override
			public void focusLost(FocusEvent e) {
				ConnectionDialog.this.userName.select(0, 0);
			}
		});

		/**
		 * password field
		 */
		passWordLabel = new JLabel("Password");
		passWordLabel.setLabelFor(passWord);
		passWord = new JPasswordField("password");
		passWord.addKeyListener(returnKeyListener);
		passWord.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE
						&& e.isControlDown()) {
					passWord.setText("");
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}
		});
		passWord.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent arg0) {
				ConnectionDialog.this.passWord.selectAll();
			}

			@Override
			public void focusLost(FocusEvent e) {
				ConnectionDialog.this.passWord.select(0, 0);
			}
		});

		panel.add(hostAddressLabel);
		panel.add(hostAddress);
		panel.add(portNumberLabel);
		panel.add(portNumber);
		panel.add(userNameLabel);
		panel.add(userName);
		panel.add(passWordLabel);
		panel.add(passWord);
		panel.add(connectButton);
		panel.add(abortButton);

		this.getContentPane().add(panel);

		this.pack();
		this.setAlwaysOnTop(true);
		this.setLocationRelativeTo(this.getParent());
		this.setVisible(true);
	}

	private void abort() {
		ClientMain.resetServerConnection();
		this.dispose();
	}

	private void connect() {
		short port;
		ServerConnection sc;
		try {
			port = Short.parseShort(this.portNumber.getText());
		} catch (NumberFormatException e) {
			JOptionPane
					.showMessageDialog(this, "The port number is incorrect.");
			return;
		}
		try {
			sc = new ServerConnection(this.hostAddress.getText(),
					this.userName.getText(), String.valueOf(this.passWord
							.getPassword()), port);
		} catch (MalformedURLException e) {
			JOptionPane.showMessageDialog(this, "This is not an URL.");
			return;
		}
		ClientMain.setServerConnection(sc);
		this.dispose();
	}

}
