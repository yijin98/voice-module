package model;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class window extends JFrame{

	private static final long serialVersionUID = 1L;
	private JLabel label;
	private JTextField tf;
	private JTextArea ta;
	private JTextArea tb;
	
	public window() {
		JPanel jp = new JPanel();
		jp.setLayout(new BorderLayout());
        ta = new JTextArea(10, 30);
        tb = new JTextArea(10, 30);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        tb.setLineWrap(true);
        tb.setWrapStyleWord(true);
        jp.add(ta, BorderLayout.NORTH);
        jp.add(tb, BorderLayout.SOUTH);
        ta.setText("Voice Recognition Module Started!\nWaiting for Connection\n");
        Font f = new Font("Arial", Font.PLAIN, 20);
        ta.setFont(f);
        tb.setFont(f);
        ta.setEditable(false);
        tb.setEditable(false);
        JMenuBar mb = new JMenuBar();
        JMenu m1 = new JMenu("Options");
        JMenuItem m11 = new JMenuItem("Help");
        m1.add(m11);
        mb.add(m1);
        getContentPane().add(BorderLayout.NORTH, mb);
        label = new JLabel();
        jp.add(label, BorderLayout.EAST);
        label.setText("<html>Speed:<br>NORMAL</html>");
        label.setFont(f);
        tf = new JTextField();
        jp.add(tf, BorderLayout.CENTER);
        tf.setText("Feedback area");
        tf.setFont(f);
        tf.setEditable(true);
        add(jp);
        
        event e = new event();
        m11.addActionListener(e);
	}
	
	public class event implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			JOptionPane.showMessageDialog(null, "Simple instructions:\n"
					+ "Execute Command      => Execute current command list\n"
					+ "Clear command list   => Clear current command list\n"
					+ "Program Pause        => Pause the program\n"
					+ "Program Resume       => Resume the program\n"
					+ "Change Command <num> => Change certain command\n"
					+ "Delete Command <num> => Delete certain command");
		}
	}
	
	public void appendA(String s) {
		ta.append(s);
	}
	
	public void appendB(String s) {
		tb.append(s);
	}
	
	public void setTextA(String s) {
		ta.setText(s);
	}
	
	public void setTextB(String s) {
		tb.setText(s);
	}
	
	public void setLabel(String s) {
		label.setText(s);
	}
	
	public void setTextField(String s) {
		tf.setText(s);
	}
}