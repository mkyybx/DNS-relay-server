import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Date;

import javax.swing.*;


public class Main {
	static void printHelp() {
		System.out.print("dnsrely [-d|-v|-h] upstream server\ncopyright 沐晓枫\n-d\tdebug模式，日志显示，前台显示，默认不开启\n-v\t详细记录模式，默认不开启\n-h\t打印帮助\n");
	}
	public static void main(String args[]) {
		//-d debug模式，日志显示，无后台，默认不开启
		//-v 详细记录模式，默认不开启
		//-h 打印帮助
		//参数是DNS服务器
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
				if (SystemTray.isSupported()) { // 判断是否支持系统托盘
					URL url = Main.class.getClass().getResource("/TrayIcon1.png"); // 获取图片所在的URL
					ImageIcon icon = new ImageIcon(url); // 实例化图像对象
					Image image = icon.getImage(); // 获得Image对象
					TrayIcon trayIcon = new TrayIcon(image); // 创建托盘图标
					trayIcon.setToolTip("DNS中继服务器"); // 添加工具提示文本
					PopupMenu popupMenu = new PopupMenu(); // 创建弹出菜单
					MenuItem exit = new MenuItem("退出"); // 创建菜单项
					MenuItem viewLog = new MenuItem("显示日志");
					MenuItem about = new MenuItem("关于");
					MenuItem clearCache = new MenuItem("清理缓存");
					MenuItem verboseLog;
					if (verbose == false)
						verboseLog = new MenuItem("启用详细日志记录");
					else verboseLog = new MenuItem("启用简单日志记录");
							
					//响应方法
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
							JOptionPane.showMessageDialog(null, "没有什么好关于的嘛……\nby:沐晓枫");
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
								verboseLog.setLabel("启用详细日志记录");
							else verboseLog.setLabel("启用简单日志记录");
						}
					});
					popupMenu.add(viewLog); // 为弹出菜单添加菜单项
					popupMenu.add(verboseLog);
					popupMenu.add(clearCache);
					popupMenu.add(viewLog); 
					popupMenu.add(about);
					popupMenu.add(exit);
					trayIcon.setPopupMenu(popupMenu); // 为托盘图标加弹出菜弹
					SystemTray systemTray = SystemTray.getSystemTray(); // 获得系统托盘对象
					try {
						systemTray.add(trayIcon); // 为系统托盘加托盘图标
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			Server.init(dns);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			new log("未找到mysql-connector!",true).print();
			JOptionPane.showMessageDialog(null, "未找到mysql-connector!");
			System.exit(1);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			printHelp();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			JOptionPane.showMessageDialog(null, "IO错误!");
			System.out.println("IO错误!");
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			new log("SQL错误!",true).print();
			JOptionPane.showMessageDialog(null, "SQL错误!");
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
