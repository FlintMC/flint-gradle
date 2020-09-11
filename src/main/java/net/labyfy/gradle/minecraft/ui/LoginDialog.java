package net.labyfy.gradle.minecraft.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

/**
 * Login dialog for Minecraft login allowing online authentication.
 */
public class LoginDialog extends JDialog {
    private final JTextField emailField;
    private final JPasswordField passwordField;
    private final JButton loginButton;

    private LoginDialogResult result;

    /**
     * Constructs a new {@link LoginDialog} with an already inputted email and
     * an error text to display.
     *
     * @param email     The value to fill the email field with
     * @param errorText The error text to display beneath the dialog
     */
    public LoginDialog(String email, String errorText) {
        // Top level container
        JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.PAGE_AXIS));

        // The login panel, a 2 by 2 grid with each a label and an input
        JPanel loginPanel = new JPanel(new GridBagLayout());
        GridBagConstraints loginLayoutConstraints = new GridBagConstraints();

        // Add the email label and input
        emailField = new JTextField();
        emailField.addKeyListener(new NotifyingKeyListener(this::updateLoginButton));

        loginLayoutConstraints.gridx = 0;
        loginLayoutConstraints.gridy = 0;
        loginLayoutConstraints.weightx = 0;
        loginLayoutConstraints.fill = GridBagConstraints.BOTH;
        loginPanel.add(new JLabel("Email:", SwingConstants.RIGHT), loginLayoutConstraints);

        loginLayoutConstraints.gridx = 1;
        loginLayoutConstraints.gridy = 0;
        loginLayoutConstraints.weightx = 1;
        loginLayoutConstraints.fill = GridBagConstraints.BOTH;
        loginPanel.add(emailField, loginLayoutConstraints);

        if (email != null) {
            // An email value has been supplied, pre-fill the text field
            emailField.setText(email);
        }

        // Add the password label and input
        passwordField = new JPasswordField();
        passwordField.addKeyListener(new NotifyingKeyListener(this::updateLoginButton));

        loginLayoutConstraints.gridx = 0;
        loginLayoutConstraints.gridy = 1;
        loginLayoutConstraints.weightx = 0;
        loginLayoutConstraints.fill = GridBagConstraints.BOTH;
        loginPanel.add(new JLabel("Password:", SwingConstants.RIGHT), loginLayoutConstraints);

        loginLayoutConstraints.gridx = 1;
        loginLayoutConstraints.gridy = 1;
        loginLayoutConstraints.weightx = 1;
        loginLayoutConstraints.fill = GridBagConstraints.BOTH;
        loginPanel.add(passwordField, loginLayoutConstraints);

        // Add the login panel to the root panel
        rootPanel.add(loginPanel);

        if (errorText != null) {
            // An error text has been supplied, add it
            JLabel errorLabel = new JLabel(errorText, SwingConstants.CENTER);
            errorLabel.setForeground(Color.RED);
            errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            rootPanel.add(errorLabel);
        }

        // Create a panel containing all possible buttons
        JPanel buttons = new JPanel();
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));

        // Create the buttons and set their actions
        JButton playOfflineButton = new JButton("Play offline");
        playOfflineButton.addActionListener((event) -> complete(LoginDialogResult.CONTINUE_OFFLINE));
        buttons.add(playOfflineButton);

        JButton abortButton = new JButton("Abort");
        abortButton.addActionListener((event) -> complete(LoginDialogResult.ABORT));
        buttons.add(abortButton);

        loginButton = new JButton("Login");
        loginButton.addActionListener((event) -> complete(LoginDialogResult.ATTEMPT_LOGIN));
        buttons.add(loginButton);

        // Add the buttons to the root panel
        rootPanel.add(buttons);

        // Make sure we don't kill the program when closing the dialog
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Make sure we block when colling setVisible until the dialog is hidden again
        setModal(true);

        // Set the content pane, so the window is not just empty and
        // set the default button to the login button
        setContentPane(rootPanel);
        getRootPane().setDefaultButton(loginButton);

        // Make sure the dialog is just as big as required
        pack();
        setResizable(false);

        // Set a minimum width to ensure usable text fields
        setMinimumSize(new Dimension(400, 0));
        setLocationRelativeTo(null);

        // Set the initial button state
        updateLoginButton(null);

        // Give the window a title
        setTitle("Minecraft login");

        // Add a window focus listener
        addWindowFocusListener(new WindowAdapter() {
            /**
             * If the dialog loses focus, it is moved in front of all windows.
             *
             * @param event The window event
             */
            @Override
            public void windowLostFocus(WindowEvent event) {
                setAlwaysOnTop(true);
            }
        });
    }

    /**
     * Shows this dialog and prompts the user for action.
     *
     * @return The result the use has chosen, or {@code null}, if the user closed the window
     */
    public LoginDialogResult execute() {
        setVisible(true);
        // Brings the dialog to the front
        toFront();
        return result;
    }

    /**
     * Retrieves the text the user has put in for the login email.
     *
     * @return The text content of the email field
     */
    public String getEmail() {
        return emailField.getText();
    }

    /**
     * Retrieves the text the user has put in for the login password.
     *
     * @return The text content of the password field
     */
    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    /**
     * Enables or disables the login button based on the states of the
     * state of the email and password field.
     *
     * @param e The even that caused this check to trigger, or {@code null},
     *          if called manually
     */
    private void updateLoginButton(KeyEvent e) {
        loginButton.setEnabled(!getEmail().isEmpty() && !getPassword().isEmpty());
    }

    /**
     * Completes this dialog with the given result.
     *
     * @param result The result to complete this dialog with
     */
    private void complete(LoginDialogResult result) {
        this.result = result;
        dispose();
    }

    /**
     * Utility class for adding a key listener which invokes a consumer
     * in all cases.
     */
    private static class NotifyingKeyListener implements KeyListener {
        private final Consumer<KeyEvent> delegate;

        /**
         * Constructs a new {@link NotifyingKeyListener} with the given delegate.
         *
         * @param delegate The consumer to delegate all events to
         */
        public NotifyingKeyListener(Consumer<KeyEvent> delegate) {
            this.delegate = delegate;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void keyTyped(KeyEvent e) {
            delegate.accept(e);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void keyPressed(KeyEvent e) {
            delegate.accept(e);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void keyReleased(KeyEvent e) {
            delegate.accept(e);
        }
    }
}
