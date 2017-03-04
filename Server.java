import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;


	class Question {
		String name;
		char type = 0x0000;
		char Class = 0x0000;
		int offset;
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(name + " ");
			if (type == 0x0001)
				sb.append("A ");
			else if (type == 0x001c)
				sb.append("AAAA ");
			else if (type == 0x0005)
				sb.append("CNAME ");
			else if (type == 0x0006)
				sb.append("SOA ");
			else if (type == 0x0015)
				sb.append("MX ");
			else if (type == 0x000c)
				sb.append("PTR ");
			else sb.append("type=0x" + Integer.toHexString((int)type) + " ");
			return sb.toString();
		}
		
		Question() {
			
		}
		
		Question(String name, char type, char Class) {
			this.name = name;
			this.type = type;
			this.Class = Class;
		}
		
		byte[] toByte(int m) {
			byte[] temp = new byte[512];
			int pointer = writeName(temp, 0);
			temp[pointer++] = 0x00;
			temp[pointer++] = (byte) (type >> 8);
			temp[pointer++] = (byte) (type & 0x0f);
			temp[pointer++] = (byte) (Class >> 8);
			temp[pointer++] = (byte) (Class & 0x0f);
			return temp;
		}
		
		int findName(byte[] packet, int offset) {
			//判断是否压缩
			int pointer;
			byte first = packet[offset];
			if ((first & 0xc0) == 0xc0) {
				char temp = 0;
				temp |= (first & 0x000000ff);
				temp <<= 8;
				temp |= (packet[offset + 1] & 0x000000ff);
				pointer = (temp & 0x3fff);
			}
			else pointer = offset;
			//name
			int[] tempPointer = new int[1];
			tempPointer[0] = pointer;
			this.name = readName(packet, tempPointer);
			if (pointer < offset) {
				pointer = offset + 2;
				return pointer;
			}
			return tempPointer[0];
		}
		
		String readName(byte[] packet, int[] pointer) {
			StringBuilder tempName = new StringBuilder();
			int j = 0;
			do {
				for (; j > 0; j--) {
					tempName.append((char)packet[pointer[0]++]);
				}
				j = packet[pointer[0]++];
				if (j > 0)
					tempName.append('.');
				if (((byte)j & 0xc0) == 0xc0) {
					//尾部压缩
					char temp = 0;
					temp |= (j & 0x000000ff);
					temp <<= 8;
					temp |= (packet[pointer[0]++] & 0x000000ff);
					int[] tempPointer = new int[1];
					tempPointer[0] = (temp & 0x3fff);
					return tempName.substring(1) + '.' + (readName(packet, tempPointer)).toString();
				}
			} while (j > 0);
			if (tempName.length() <= 1)
				return tempName.toString();
			return tempName.substring(1);
		}
		
		int writeName(byte[] target, int offset) {
			int j = offset;
			int k = 0;
			for (int i = 0; i < name.length(); i++) {
				if (name.charAt(i) != '.' && i != name.length() - 1)
					target[j + ++k] = (byte)name.charAt(i);
				else {
					if (i == name.length() - 1)
						target[j + ++k] = (byte)name.charAt(i);
					target[j] = (byte)k;
					j = j + k + 1;
					k = 0;
				}
			}
			return name.length() + 1 + offset;
		}
		
		Question getQuestion(byte[] packet, int offset) {
			int pointer = findName(packet, offset);
			//type
			//big endian
			type |= (packet[pointer++] & 0x000000ff);
			type <<= 8;
			type |= (packet[pointer++] & 0x000000ff);
			//class
			Class |= (packet[pointer++] & 0x000000ff);
			Class <<= 8;
			Class |= (packet[pointer++] & 0x000000ff);
			this.offset = pointer;
			return this;
		}
	}
	
	class ResourceRecord extends Question {
		int ttl = 0;
		byte[] rdata;
		
		public String toString() {
			String s = null;
			try {
				s = InetAddress.getByAddress(rdata).getHostAddress();
			} catch (UnknownHostException e) {
				
			}
			return super.toString() + (s == null ? "" : s) + " " + "ttl=" + ttl + " ";
		}
		
		ResourceRecord(String name, char type, char Class, int ttl, byte[] rdata) {
			super(name, type, Class);
			this.ttl = ttl;
			this.rdata = rdata;
		}
		
		ResourceRecord() {
			
		}
		
		byte[] toByte() {
			byte[] temp = new byte[512];
			int pointer = writeName(temp, 0);
			temp[pointer++] = 0x00;
			temp[pointer++] = (byte) (type >> 8);
			temp[pointer++] = (byte) (type & 0x000000ff);
			temp[pointer++] = (byte) (Class >> 8);
			temp[pointer++] = (byte) (Class & 0x0000000ff);
			temp[pointer++] = (byte) ((ttl >> 24) & 0x000000ff);
			temp[pointer++] = (byte) ((ttl >> 16) & 0x000000ff);
			temp[pointer++] = (byte) ((ttl >> 8) & 0x000000ff);
			temp[pointer++] = (byte) (ttl & 0x000000ff);
			temp[pointer++] = (byte) ((rdata.length >> 8) & 0x000000ff);
			temp[pointer++] = (byte) (rdata.length & 0x000000ff);
			for (int i = 0; i < rdata.length; i++) {
				temp[pointer++] = rdata[i];
				if (pointer == 500)
					return null;//太长
			}
			return Arrays.copyOf(temp, pointer);
		}
		
		ResourceRecord getResourceRecord(byte[] packet, int offset) {
			getQuestion(packet, offset);
			int pointer = this.offset;
			//ttl
			for (int j = 0; j < 3; j++) {
				ttl |= (packet[pointer++] & 0x000000ff);
				ttl <<= 8;
			}
			ttl |= (packet[pointer++] & 0x000000ff);
			//rdlength
			char length = 0;
			length |= (packet[pointer++] & 0x000000ff);
			length <<= 8;
			length |= (packet[pointer++] & 0x000000ff);
			rdata = new byte[length];
			for (int j = 0; j < length; j++)
				rdata[j] = packet[pointer++];
			this.offset = pointer;
			return this;
		}
	}
	
	final class Header {
		char id = 0;
		boolean qr;//1=response,0=request
		byte opcode = 0;//0=standard query,1=inverse query
		boolean authAnsw;
		boolean truncated;
		boolean recuisionDesired;
		boolean recuisionAvailable;
		byte rcode = 0;
		char qcount = 0;
		char ancount = 0;
		char nscount = 0;
		char arcount = 0;
		int offset;
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (opcode == 0)
				sb.append("\tstandard query ");
			else if (opcode == 1)
				sb.append("\tinverse query");
			else sb.append("\tunknown query:" + opcode + " ");
			if (qr == true)
				sb.append("response ");
			sb.append("0x" + Integer.toHexString((int)id) + " ");
			return sb.toString();
		}
		
		Header getHeader(byte[] packet, int offset) {
			int pointer = offset;
			//id
			id |= (packet[pointer++] & 0x000000ff);
			id <<= 8;
			id |= (packet[pointer++] & 0x000000ff);
			byte temp = packet[pointer++];
			//qr
			if ((temp & 0x80) == 0x80)
				qr = true;
			else
				qr = false;
			//opcode
			opcode = (byte)((temp & 0x78) >> 3);
			//aa
			if ((temp & 0x04) == 0x04)
				authAnsw = true;
			else authAnsw = false;
			//tc，暂时不知道怎么处理，暂时忽略
			if ((temp & 0x02) == 0x02)
				truncated = true;
			else truncated = false;
			//rd
			if ((temp & 0x01) == 0x01)
				recuisionDesired = true;
			else recuisionDesired = false;
			temp = packet[pointer++];
			//ra
			if ((temp & 0x80) == 0x80)
				recuisionAvailable = true;
			else recuisionAvailable = false;
			//rocde
			rcode = (byte) (temp & 0x0f);
			//count
			char[] count = new char[4];
			for (int i = 0; i < 4; i++) {
				count[i] |= (packet[pointer++] & 0x000000ff);
				count[i] <<= 8;
				count[i] |= (packet[pointer++] & 0x000000ff);
			}
			qcount = count[0];
			ancount = count[1];
			nscount = count[2];
			arcount = count[3];
			this.offset = pointer;
			return this;
		}
	}


public class Server {
	static DatagramSocket serverSocket;
	static DatagramSocket clientSocket;
	static DatagramPacket clientPacket;
	private static Server server;
	//hashtable线程安全
	static Hashtable<Character, SocketAddress> idTable = new Hashtable<Character, SocketAddress>();
	
	//单例
	private Server() {
		
	}

	
	//抛出异常：端口有问题
	static void init(InetAddress recursiveServer) throws IOException, ClassNotFoundException, SQLException {
		if (server == null) {
			server = new Server();
			Database.init();
			//监听端口
			serverSocket = new DatagramSocket(53);
			new log("端口监听成功",true).print();
			byte[] temp = new byte[512];
			DatagramPacket serverPacket = new DatagramPacket(temp, 512);
			
			//绑定发送端口
			clientSocket = new DatagramSocket();
			byte[] temp1 = new byte[512];
			clientPacket = new DatagramPacket(temp1, 512, recursiveServer, 53);
			
			//启动客户端
			new Thread(new RunnableClient()).start();
			
			//服务器端
			while (true) {
				serverSocket.receive(serverPacket);
				new Thread(new RunnableServer(Arrays.copyOf(temp, serverPacket.getLength()), serverPacket.getSocketAddress())).start();
				//new RunnableServer(Arrays.copyOf(temp, 512), serverPacket.getSocketAddress()).run();
			}
		}
	}
}

final class Database {
	private static Database database;
	private static Connection connection;
	private Database() {
		
	}
	static void clearCache() {
		synchronized(Database.class) {
			try {
				PreparedStatement st = connection.prepareStatement("delete from a where ttl > 0");
				PreparedStatement st1 = connection.prepareStatement("delete from aaaa where ttl > 0");
				st.execute();
				st1.execute();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	static void init() throws ClassNotFoundException, SQLException {
		if (database == null) {
			//连接数据库
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection("jdbc:mysql://localhost/dns","mky0","123456");
			new log("数据库初始化成功",true).print();
		}
	}
	static Question[] inverseQuery(InetAddress address, boolean isIPv6) {
		LinkedList<ResourceRecord> list = new LinkedList<ResourceRecord>();
		try {
			synchronized(Database.class) {
				new log("数据库反向查询:\t" + address.getHostName(),false).print();
				PreparedStatement st = connection.prepareStatement("Select * from " +( isIPv6 ? "aaaa" : "a" )+ " where rdata = ?");
				st.setString(1, address.getHostAddress());
				ResultSet rs = st.executeQuery();
				if (rs.next() == false)
					//找不到
					return null;
				else {
					while (rs.next()) {
						if (rs.getInt("ttl") < System.currentTimeMillis() / 1000 && rs.getInt("ttl") != -1) {
							//超时
							st = connection.prepareStatement("delete from " + (isIPv6 ? "aaaa" : "a") + " where name = ?");
							st.setString(1, rs.getString("name"));
							st.execute();
							new log("记录\t" + rs.getString("name") + "->" + rs.getString("rdata") + "超时", true).print();
						}
						else {
							new log("命中数据库:\t" + rs.getString("name") + "->" + rs.getString("rdata"),false).print();
							list.add(new ResourceRecord(rs.getString("name"), (char)(isIPv6 ? 0x001c : 0x0001), (char)0x0001, rs.getInt("ttl") == -1 ? 9999 : (int)(rs.getInt("ttl") - System.currentTimeMillis() / 1000), InetAddress.getByName(rs.getString("rdata").trim()).getAddress()));
						}
					}
					return (ResourceRecord[])list.toArray();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	static ResourceRecord standardQuery(String domain, boolean isIPv6) {
		try {
			synchronized(Database.class) {
				new log("数据库查询:\t" + domain,false).print();
				PreparedStatement st = connection.prepareStatement("Select * from " +( isIPv6 ? "aaaa" : "a" )+ " where name = ?");
				st.setString(1, domain);
				ResultSet rs = st.executeQuery();
				if (rs.next() == false)
					//找不到
					return null;
				else if (rs.getInt("ttl") < System.currentTimeMillis() / 1000 && rs.getInt("ttl") != -1) {
					//超时
					st = connection.prepareStatement("delete from " + (isIPv6 ? "aaaa" : "a") + " where name = ?");
					st.setString(1, domain);
					st.execute();
					new log("记录\t" + rs.getString("name") + "->" + rs.getString("rdata") + "超时", true).print();
					return null;
				}
				else {
					new log("命中数据库:\t" + rs.getString("name") + "->" + rs.getString("rdata"),false).print();
					return new ResourceRecord(rs.getString("name"), (char)(isIPv6 ? 0x001c : 0x0001), (char)0x0001, rs.getInt("ttl") == -1 ? 9999 : (int)(rs.getInt("ttl") - System.currentTimeMillis() / 1000), InetAddress.getByName(rs.getString("rdata").trim()).getAddress());
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	static void update(ResourceRecord record) {
		try {
			synchronized(Database.class) {
				if (record.ttl != 0 && ((record.type == 0x01 || record.type == 0x1c))) {//ttl为0不缓存
					PreparedStatement st = connection.prepareStatement("select ttl from " + (record.type == 0x01 ? "a" : "aaaa") + " where name = ?");
					st.setString(1, record.name.toString());
					ResultSet rs = st.executeQuery();
					if (rs.next() == true && rs.getInt(1) != -1) {
						st = connection.prepareStatement("delete from " + (record.type == 0x01 ? "a" : "aaaa") + " where name = ?");
						st.setString(1, record.name.toString());
						st.execute();
					}
					if (rs.next() == false) {
						//添加
						st = connection.prepareStatement("insert into " + (record.type == 0x01 ? "a" : "aaaa") + " values (?,?,?)");
						st.setString(1, record.name.toString());
						st.setInt(2, (int) (record.ttl + System.currentTimeMillis() / 1000));
						st.setString(3, InetAddress.getByAddress(record.rdata).getHostAddress());
						st.execute();
						new log("新增记录:\t" + record.name.toString() + "->" + InetAddress.getByAddress(record.rdata).getHostAddress() + " ttl=" + record.ttl, true).print();
					}
				}
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
}

final class RunnableClient implements Runnable {

	@Override
	public void run() {
		// TODO Auto-generated method stub
		byte[] data = new byte[512];
		DatagramPacket packet = new DatagramPacket(data, 512);
		while (true) {
			try {
				StringBuilder sb = new StringBuilder();
				Server.clientSocket.receive(packet);
				Header header = new Header().getHeader(data, 0);
				sb.append("收到上行DNS:" + header.toString());
				if (header.qr == false || header.truncated == true || header.opcode != 0)
					new log("上行收到错误报文！",true).print();
				else {
					//回传
					SocketAddress addr = Server.idTable.get(header.id);
					if (addr != null) {
						//存在
						Server.serverSocket.send(new DatagramPacket(data,packet.getLength(),addr));
						//删除id
						Server.idTable.remove(header.id);
						
						int pointer = header.offset;
						//跳过query
						for (int i = 0; i < header.qcount; i++)
							pointer = new Question().getQuestion(data, pointer).offset;
						//缓存
						for (int i = 0; i < header.ancount; i++) {
							ResourceRecord record = new ResourceRecord().getResourceRecord(data, pointer);
							sb.append(record.toString());
							pointer = record.offset;
							Database.update(record);
						}
					}
					new log(sb.toString(),false).print();
				}	
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}

final class RunnableServer implements Runnable {
	byte[] data;
	SocketAddress addr;	
	static InetAddress ALL_ZEROv4;
	static InetAddress ALL_ZEROv6;
	
	void sendError() throws IOException {
		byte[] replyPacket = Arrays.copyOf(data, data.length);
		replyPacket[2] |= 0x80;
		replyPacket[2] &= 0xfb;
		replyPacket[3] = (byte) 0x82;
		Server.serverSocket.send(new DatagramPacket(replyPacket,data.length,addr));
	}
	
	RunnableServer(byte[] data, SocketAddress addr) {
		this.data = data;
		this.addr = addr;
		if (ALL_ZEROv4 == null || ALL_ZEROv6 == null) {
			try {
				ALL_ZEROv4 = InetAddress.getByAddress(new byte[]{0,0,0,0});
				ALL_ZEROv6 = InetAddress.getByName("::");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
	}
	
	void recursiveQuery(Header header) throws IOException {
		System.out.println(Server.idTable);
		if (Server.idTable.contains(header.id)) {
			//凑巧重复了
			new log("下行报文ID重复！",true).print();
			sendError();
		}
		else {
			//存放映射关系
			Server.idTable.put(header.id, addr);
			//转发
			Server.clientPacket.setData(data);
			Server.clientSocket.send(Server.clientPacket);
			new log("转发报文:" + header.toString(),false).print();
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		Header header = new Header().getHeader(data, 0);
		if (header.qr == true) //不应该是回复包
			new log("下行报文为应答报文！",true).print();
		else {
			try {
				//暂不支持多question
				if (header.qcount != 1 || header.ancount > 1 || (header.ancount == 1 && header.qcount != 0))  {
					new log("下行报文报头不被支持！",true).print();
					sendError();
				}
				else {
					if (header.opcode == 0) {
						//standard query
						Question question = new Question().getQuestion(data, header.offset);
						new log("收到下行报文:" + header.toString() + question.toString(),false).print();
						ResourceRecord address = null;
						if (question.type == 1)
							address = Database.standardQuery(question.name, false);
						else if (question.type == 0x001c)
							address = Database.standardQuery(question.name, true);
						if (address == null) 
							recursiveQuery(header);
						else if (InetAddress.getByAddress(address.rdata).equals(ALL_ZEROv4) || InetAddress.getByAddress(address.rdata).equals(ALL_ZEROv6)) {
							//屏蔽网站，返回
							byte[] replyPacket = Arrays.copyOf(data, data.length);
							replyPacket[2] |= 0x80;
							replyPacket[2] &= 0xfb;
							replyPacket[3] = (byte) 0x85;
							Server.serverSocket.send(new DatagramPacket(replyPacket,data.length,addr));
							new log("屏蔽域名:\t" + question.name.toString(),true).print();
						}
						else {
							//正常返回
							byte[] temp1 = address.toByte();
							byte[] temp = Arrays.copyOf(data, temp1.length + data.length);
							temp[2] |= 0x80;
							temp[2] &= 0xf9;
							temp[3] = (byte)0x80;
							temp[7] = 1;
							if (temp1 == null) //过长
								sendError();
							else System.arraycopy(temp1, 0, temp, data.length, temp1.length);
							Server.serverSocket.send(new DatagramPacket(temp,temp.length,addr));
							new log("回复下行报文" + new Header().getHeader(temp, 0) + address.toString(),false).print();
						}
					}
					else if (header.opcode == 1) {
						//inverse query
						ResourceRecord question = new ResourceRecord().getResourceRecord(data, header.offset);
						new log("收到下行报文:" + header.toString() + question.toString(),false).print();
						Question[] list = null;
						if (question.type == 1)
							list = Database.inverseQuery(InetAddress.getByAddress(question.rdata), false);
						else if (question.type == 0x001c)
							list = Database.inverseQuery(InetAddress.getByAddress(question.rdata), true);
						if (list == null) 
							recursiveQuery(header);
						else {
							byte[] temp = Arrays.copyOf(data, 512);
							temp[2] |= 0x80;
							temp[2] &= 0xf9;
							temp[3] = (byte)0x80;
							int i = 0;
							int pointer = data.length;
							for (; i < list.length; i++) {
								byte[] temp1 = list[i].toByte(0);
								if (temp1 != null && pointer + temp1.length < 512) {
									System.arraycopy(temp1, 0, temp, pointer, temp1.length);
									pointer += temp1.length;
								}
								else break;
							}
							temp[5] = (byte) i;
							Server.serverSocket.send(new DatagramPacket(temp,pointer,addr));
							new log("回复下行报文",false).print();
						}
					}
					else recursiveQuery(header);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}
