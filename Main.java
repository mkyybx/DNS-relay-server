import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Date;

import javax.swing.*;


public class Main {
	static void printHelp() {
		System.out.print("dnsrely [-d|-v|-h] upstream server\ncopyright ������\n-d\tdebugģʽ����־��ʾ��ǰ̨��ʾ��Ĭ�ϲ�����\n-v\t��ϸ��¼ģʽ��Ĭ�ϲ�����\n-h\t��ӡ����\n");
	}
	public static void main(String args[]) {
		//-d debugģʽ����־��ʾ���޺�̨��Ĭ�ϲ�����
		//-v ��ϸ��¼ģʽ��Ĭ�ϲ�����
		//-h ��ӡ����
		//������DNS������
		try {
			InetAddress dns = InetAddress.getByAddress(new byte[]{8,8,8,8});
			boolean debug = false;
			boolean verbose = false;
			boolean helpPrinted = false;
			for (int i = 0; i < args.length; i++) {
				if (args[i].charAt(0) == '-' && args[i].length() == 2) {
					if (args[i].charAt(1) == 'd' && debug == false)
						debug = true;
					else if (args[i].charAt(1) == 'v' && verbose == false)
						verbose = true;
					else if (args[i].charAt(1) == 'h' && helpPrinted == false) {
						printHelp();
						helpPrinted = true;
						System.exit(0);
					}
					else throw new UnknownHostException();
				}
				else {
					dns = InetAddress.getByName(args[i]);
				}
			}
			log.isCriticalOnly = !verbose;
			log.showLog = debug;
			if (!debug) {
				if (SystemTray.isSupported()) { // �ж��Ƿ�֧��ϵͳ����
					URL url = Main.class.getClass().getResource("/TrayIcon1.png"); // ��ȡͼƬ���ڵ�URL
					ImageIcon icon = new ImageIcon(url); // ʵ����ͼ�����
					Image image = icon.getImage(); // ���Image����
					TrayIcon trayIcon = new TrayIcon(image); // ��������ͼ��
					trayIcon.setToolTip("DNS�м̷�����"); // ��ӹ�����ʾ�ı�
					PopupMenu popupMenu = new PopupMenu(); // ���������˵�
					MenuItem exit = new MenuItem("�˳�"); // �����˵���
					MenuItem viewLog = new MenuItem("��ʾ��־");
					MenuItem about = new MenuItem("����");
					MenuItem clearCache = new MenuItem("������");
					MenuItem verboseLog;
					if (verbose == false)
						verboseLog = new MenuItem("������ϸ��־��¼");
					else verboseLog = new MenuItem("���ü���־��¼");
							
					//��Ӧ����
					exit.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							// TODO Auto-generated method stub
							
							System.exit(0);
						}
					});
					viewLog.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							// TODO Auto-generated method stub
							try {
								Runtime.getRuntime().exec("notepad DNS.log");
							} catch (IOException e1) {
								// TODO Auto-generated catch block
							}
						}
					});
					about.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							// TODO Auto-generated method stub
							JOptionPane.showMessageDialog(null, "û��ʲô�ù��ڵ����\nby:������");
						}
					});
					clearCache.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							// TODO Auto-generated method stub
							Database.clearCache();
						}
					});
					verboseLog.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							// TODO Auto-generated method stub
							log.isCriticalOnly = !log.isCriticalOnly;
							if (log.isCriticalOnly)
								verboseLog.setLabel("������ϸ��־��¼");
							else verboseLog.setLabel("���ü���־��¼");
						}
					});
					popupMenu.add(viewLog); // Ϊ�����˵���Ӳ˵���
					popupMenu.add(verboseLog);
					popupMenu.add(clearCache);
					popupMenu.add(viewLog); 
					popupMenu.add(about);
					popupMenu.add(exit);
					trayIcon.setPopupMenu(popupMenu); // Ϊ����ͼ��ӵ����˵�
					SystemTray systemTray = SystemTray.getSystemTray(); // ���ϵͳ���̶���
					try {
						systemTray.add(trayIcon); // Ϊϵͳ���̼�����ͼ��
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			Server.init(dns);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			new log("δ�ҵ�mysql-connector!",true).print();
			JOptionPane.showMessageDialog(null, "δ�ҵ�mysql-connector!");
			System.exit(1);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			printHelp();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			JOptionPane.showMessageDialog(null, "IO����!");
			System.out.println("IO����!");
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			new log("SQL����!",true).print();
			JOptionPane.showMessageDialog(null, "SQL����!");
			System.exit(1);
		}
	}
}

class log {
	String msg;
	String time;
	boolean isCritical;
	static boolean isCriticalOnly;
	static boolean showLog = true;
	static PrintWriter output;
	
	log (String msg, boolean isCritical) {
		this.msg = msg;
		this.isCritical = isCritical;
		time = new Date().toString();
	}
	
	void print() {
		if (output == null)
			try {
				output = new PrintWriter("DNS.log");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		String s = null;
		if (!(isCriticalOnly && !isCritical))
			s = time + msg + "\n";
		if (s != null) {
			if (showLog)
				System.out.print(s);
			output.println(s);
			output.flush();
		}
	}
}
