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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
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

import de.dhbw_mannheim.cloudraid.client.api.IncompatibleApiVersionException;
import de.dhbw_mannheim.cloudraid.client.api.ServerConnection;

public class UserCreationDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5030263674078790548L;

	private JButton connectButton, abortButton;
	private JTextField hostAddress, portNumber, userName;
	private JPasswordField passWord, confirmation;
	private JLabel hostAddressLabel, portNumberLabel, userNameLabel,
			passWordLabel, confirmationLabel;

	private KeyListener returnKeyListener = new KeyListener() {
		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				if (e.getSource() != abortButton) {
					UserCreationDialog.this.connect();
				} else {
					UserCreationDialog.this.abort();
				}
			} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				UserCreationDialog.this.abort();
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}
	};

	public UserCreationDialog(MainWindow parent) {
		super(parent, I18n.getInstance().getString("userCreationDialogTitle"));
		I18n i = I18n.getInstance();
		this.setModalityType(ModalityType.APPLICATION_MODAL);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setResizable(false);
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(6, 2, 10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		/**
		 * connect button
		 */
		connectButton = new JButton(i.getString("create"));
		connectButton.addKeyListener(returnKeyListener);
		connectButton.setPreferredSize(new Dimension(150, 30));
		connectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				UserCreationDialog.this.connect();
			}
		});

		/**
		 * abort button
		 */
		abortButton = new JButton(i.getString("cancel"));
		abortButton.addKeyListener(returnKeyListener);
		abortButton.setPreferredSize(new Dimension(150, 30));
		abortButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				UserCreationDialog.this.abort();
			}
		});

		/**
		 * textfield for host
		 */
		hostAddressLabel = new JLabel(i.getString("server"));
		hostAddressLabel.setLabelFor(hostAddress);
		hostAddress = new JTextField("http://localhost");
		hostAddress.addKeyListener(returnKeyListener);
		hostAddress.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent arg0) {
				UserCreationDialog.this.hostAddress.selectAll();
			}

			@Override
			public void focusLost(FocusEvent e) {
				UserCreationDialog.this.hostAddress.select(0, 0);
			}
		});

		/**
		 * textfield for the port
		 */
		portNumberLabel = new JLabel(i.getString("port"));
		portNumberLabel.setLabelFor(portNumber);
		portNumber = new JTextField("8080");
		portNumber.addKeyListener(returnKeyListener);
		portNumber.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent arg0) {
				UserCreationDialog.this.portNumber.selectAll();
			}

			@Override
			public void focusLost(FocusEvent e) {
				UserCreationDialog.this.portNumber.select(0, 0);
			}
		});

		/**
		 * textfield for the username
		 */
		userNameLabel = new JLabel(i.getString("user"));
		userNameLabel.setLabelFor(userName);
		userName = new JTextField("test");
		userName.addKeyListener(returnKeyListener);
		userName.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent arg0) {
				UserCreationDialog.this.userName.selectAll();
			}

			@Override
			public void focusLost(FocusEvent e) {
				UserCreationDialog.this.userName.select(0, 0);
			}
		});

		/**
		 * password field
		 */
		passWordLabel = new JLabel(i.getString("password"));
		passWordLabel.setLabelFor(passWord);
		passWord = new JPasswordField("test");
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
				UserCreationDialog.this.passWord.selectAll();
			}

			@Override
			public void focusLost(FocusEvent e) {
				UserCreationDialog.this.passWord.select(0, 0);
			}
		});

		/**
		 * password confirmation field
		 */
		confirmationLabel = new JLabel(i.getString("password"));
		confirmationLabel.setLabelFor(confirmation);
		confirmation = new JPasswordField("wxyz");
		confirmation.addKeyListener(returnKeyListener);
		confirmation.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE
						&& e.isControlDown()) {
					confirmation.setText("");
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}
		});
		confirmation.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent arg0) {
				UserCreationDialog.this.confirmation.selectAll();
			}

			@Override
			public void focusLost(FocusEvent e) {
				UserCreationDialog.this.confirmation.select(0, 0);
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
		panel.add(confirmationLabel);
		panel.add(confirmation);
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
		if (!String.valueOf(this.passWord.getPassword()).equals(
				String.valueOf(this.confirmation.getPassword()))) {
			JOptionPane.showMessageDialog(this,
					i.getString("notMatchingPasswords"));
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

	/**
	 * Returns the content of the password confirmation field.
	 * 
	 * @return The password confirmation to be sent to the CloudRAID server.
	 */
	public String getPasswordConfirmation() {
		return String.valueOf(this.confirmation.getPassword());
	}
}
