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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.WindowConstants;

import de.dhbw_mannheim.cloudraid.client.api.HTTPException;
import de.dhbw_mannheim.cloudraid.client.api.ServerConnector;

public class ChangePasswordDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5030263674078790550L;

	private JButton okButton, abortButton;
	private JPasswordField passWord, confirmation;
	private JLabel passWordLabel, confirmationLabel;

	private KeyListener returnKeyListener = new KeyListener() {
		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				if (e.getSource() != ChangePasswordDialog.this.abortButton) {
					ChangePasswordDialog.this.connect();
				} else {
					ChangePasswordDialog.this.abort();
				}
			} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				ChangePasswordDialog.this.abort();
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}
	};

	public ChangePasswordDialog(MainWindow parent) {
		super(parent, I18n.getInstance().getString("passwordChangeDialogTitle"));
		I18n i = I18n.getInstance();
		this.setModalityType(ModalityType.APPLICATION_MODAL);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setResizable(false);
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(3, 2, 10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		/**
		 * connect button
		 */
		this.okButton = new JButton(i.getString("change"));
		this.okButton.addKeyListener(this.returnKeyListener);
		this.okButton.setPreferredSize(new Dimension(150, 30));
		this.okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ChangePasswordDialog.this.connect();
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
				ChangePasswordDialog.this.abort();
			}
		});

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
					ChangePasswordDialog.this.passWord.setText("");
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

		/**
		 * password confirmation field
		 */
		this.confirmationLabel = new JLabel(i.getString("confirmation"));
		this.confirmationLabel.setLabelFor(this.confirmation);
		this.confirmation = new JPasswordField("wxyz");
		this.confirmation.addKeyListener(this.returnKeyListener);
		this.confirmation.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE
						&& e.isControlDown()) {
					ChangePasswordDialog.this.confirmation.setText("");
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}
		});
		this.confirmation.addFocusListener(new TextFieldFocusListener());

		panel.add(this.passWordLabel);
		panel.add(this.passWord);
		panel.add(this.confirmationLabel);
		panel.add(this.confirmation);
		panel.add(this.okButton);
		panel.add(this.abortButton);

		this.getContentPane().add(panel);

		this.pack();
		this.setAlwaysOnTop(true);
		this.setLocationRelativeTo(this.getParent());
		this.setVisible(true);
	}

	private void abort() {
		this.dispose();
	}

	private void connect() {
		I18n i = I18n.getInstance();
		if (!String.valueOf(this.passWord.getPassword()).equals(
				String.valueOf(this.confirmation.getPassword()))) {
			JOptionPane.showMessageDialog(this,
					i.getString("notMatchingPasswords"));
			return;
		}
		ServerConnector sc = ClientMain.getServerConnector();
		if (sc == null) {
			return;
		}
		try {
			sc.changePassword(String.valueOf(this.passWord.getPassword()),
					String.valueOf(this.confirmation.getPassword()));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, i.getString("connectionError"));
			return;
		} catch (HTTPException e) {
			JOptionPane.showMessageDialog(
					this,
					e.getHTTPCode() + ": " + e.getHTTPErrorMessage(),
					I18n.getInstance().getString("error") + " "
							+ e.getHTTPCode(), JOptionPane.ERROR_MESSAGE);
		}
		this.dispose();
	}
}
