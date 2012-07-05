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

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import de.dhbw_mannheim.cloudraid.client.api.IncompatibleApiVersionException;
import de.dhbw_mannheim.cloudraid.client.api.ServerConnection;

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
				if (e.getSource() != ConnectionDialog.this.abortButton) {
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
		super(parent, I18n.getInstance().getString("connectionDialogTitle"));
		I18n i = I18n.getInstance();
		this.setModalityType(ModalityType.APPLICATION_MODAL);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setResizable(false);
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(5, 2, 10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		/**
		 * connect button
		 */
		this.connectButton = new JButton(i.getString("connect"));
		this.connectButton.addKeyListener(this.returnKeyListener);
		this.connectButton.setPreferredSize(new Dimension(150, 30));
		this.connectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ConnectionDialog.this.connect();
			}
		});

		/**
		 * abort button
		 */
		this.abortButton = new JButton(i.getString("cancel"));
		this.abortButton.addKeyListener(this.returnKeyListener);
		this.abortButton.setPreferredSize(new Dimension(150, 30));
		this.abortButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ConnectionDialog.this.abort();
			}
		});

		/**
		 * textfield for host
		 */
		this.hostAddressLabel = new JLabel(i.getString("server"));
		this.hostAddressLabel.setLabelFor(this.hostAddress);
		this.hostAddress = new JTextField("http://localhost");
		this.hostAddress.addKeyListener(this.returnKeyListener);
		this.hostAddress.addFocusListener(new TextFieldFocusListener());

		/**
		 * textfield for the port
		 */
		this.portNumberLabel = new JLabel(i.getString("port"));
		this.portNumberLabel.setLabelFor(this.portNumber);
		this.portNumber = new JTextField("8080");
		this.portNumber.addKeyListener(this.returnKeyListener);
		this.portNumber.addFocusListener(new TextFieldFocusListener());

		/**
		 * textfield for the username
		 */
		this.userNameLabel = new JLabel(i.getString("user"));
		this.userNameLabel.setLabelFor(this.userName);
		this.userName = new JTextField("test");
		this.userName.addKeyListener(this.returnKeyListener);
		this.userName.addFocusListener(new TextFieldFocusListener());

		/**
		 * password field
		 */
		this.passWordLabel = new JLabel(i.getString("password"));
		this.passWordLabel.setLabelFor(this.passWord);
		this.passWord = new JPasswordField("test");
		this.passWord.addKeyListener(this.returnKeyListener);
		this.passWord.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE
						&& e.isControlDown()) {
					ConnectionDialog.this.passWord.setText("");
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}
		});
		this.passWord.addFocusListener(new TextFieldFocusListener());

		panel.add(this.hostAddressLabel);
		panel.add(this.hostAddress);
		panel.add(this.portNumberLabel);
		panel.add(this.portNumber);
		panel.add(this.userNameLabel);
		panel.add(this.userName);
		panel.add(this.passWordLabel);
		panel.add(this.passWord);
		panel.add(this.connectButton);
		panel.add(this.abortButton);

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
		I18n i = I18n.getInstance();
		short port;
		ServerConnection sc;
		try {
			port = Short.parseShort(this.portNumber.getText());
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this,
					i.getString("incorrectPortNumber"));
			return;
		}
		try {
			sc = new ServerConnection(this.hostAddress.getText(),
					this.userName.getText(), String.valueOf(this.passWord
							.getPassword()), port);
		} catch (MalformedURLException e) {
			JOptionPane.showMessageDialog(this, i.getString("notAnURL") + "\n"
					+ e.getMessage());
			return;
		}
		try {
			ClientMain.setServerConnection(sc);
		} catch (IncompatibleApiVersionException e) {
			JOptionPane.showMessageDialog(this, i.getString("apiVersionError"));
			return;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, i.getString("connectionError"));
			return;
		}
		this.dispose();
	}

}
