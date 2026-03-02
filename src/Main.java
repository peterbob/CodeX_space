import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame());
    }

    public static class LoginFrame extends JFrame {
        private JTextField usernameField;
        private JPasswordField passwordField;
        private JButton loginButton;
        private JButton cancelButton;

        public LoginFrame() {
            setTitle("登陆");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(300, 200);
            setLocationRelativeTo(null);
            setResizable(false);

            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(3, 2, 10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JLabel usernameLabel = new JLabel("用户名:");
            usernameField = new JTextField();
            panel.add(usernameLabel);
            panel.add(usernameField);

            JLabel passwordLabel = new JLabel("密码:");
            passwordField = new JPasswordField();
            panel.add(passwordLabel);
            panel.add(passwordField);

            loginButton = new JButton("登陆");
            cancelButton = new JButton("取消");
            panel.add(loginButton);
            panel.add(cancelButton);

            loginButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String username = usernameField.getText();
                    String password = new String(passwordField.getPassword());
                    if (username.isEmpty() || password.isEmpty()) {
                        JOptionPane.showMessageDialog(null, "用户名和密码不能为空!");
                    } else {
                        JOptionPane.showMessageDialog(null, "登陆成功!");
                    }
                }
            });

            cancelButton.addActionListener(e -> System.exit(0));

            add(panel);
            setVisible(true);
        }
    }
}
