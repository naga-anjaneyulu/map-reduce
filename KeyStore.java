

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;import java.util.Map;
import java.util.Map.Entry;

public class KeyStore implements Runnable {

	private Socket socket = null; 
	private int port;
	private String keyAddress;
	
	public KeyStore(String keyAddress,int port) throws InterruptedException 
	{    
		this.keyAddress = keyAddress;
		this.port = port;
	}

	// Key Store Thread
	public class KeyStoreThread implements Runnable{
		private Socket clientSocket;
		private DataInputStream in	 = null; 
		private DataOutputStream out = null;
		private HashMap<String,String> keyStore = new HashMap<String,String>();


		public  KeyStoreThread(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}


		public void getKeyStore(String fileName){
			System.out.println("Loading keystore");
			BufferedReader br = null;
			String line = null;
			try {
				br = new BufferedReader(new FileReader(fileName));
				if(br != null) {
					line = br.readLine();
				}
				while(line != null && line.length() > 0) {
					String[] arr = line.split(",");
					if(arr.length == 2)
						this.keyStore.put(arr[0],arr[1]);
					line = br.readLine();
				}
				br.close();
			} catch (IOException e ) {
				System.out.println("Failed Loading key store");
			}
		}



		@Override
		public void run()
		{
			try {
				this.in = new DataInputStream( new BufferedInputStream(this.clientSocket.getInputStream()));
				this.out = new DataOutputStream(this.clientSocket.getOutputStream());
			} catch (IOException e1) {
				e1.printStackTrace();
			} 
			String line  = ""; 			
			try
			{  
				System.out.println("Reading message on server");
				line = in.readUTF();
				System.out.println("Recieved message on server =>"+line);
				String[] arr = line.split("\\r\\n");				
				
				if(arr[0].trim().equals("set")) {
					if (arr[1].trim().equals("master")) serveMasterRequest(arr,"set");
				    else if(arr[1].trim().equals("mapper")) serveMapperRequest(arr,"set");
					else serveReducerRequest(arr,"set");
				}else {
					if (arr[1].trim().equals("master")) serveMasterRequest(arr,"get");
					else if(arr[1].trim().equals("mapper")) serveMapperRequest(arr,"get");
					else serveReducerRequest(arr,"get");
				}
			} 
			catch(IOException i) 
			{  System.out.println("Exception heyy");
				System.out.println(i); 
			} 	
		}


		private void writeToDisk(String fileName) throws IOException {
			try {
				
				FileWriter myWriter = new FileWriter(fileName);
				String lines ="";
				for(Entry<String, String> map:this.keyStore.entrySet()) {
					myWriter.write(map.getKey()+","+map.getValue()+"\n");
				}
				this.out.writeUTF("STORED \r\n"); 
			    myWriter.close();
			   }catch (IOException i) {
				   this.out.writeUTF("NOT STORED \r\n");
			  }
		}
		
		
		private void serveReducerRequest(String[] arr, String method) throws IOException {
			  System.out.println("Serving Reducer Request");
			if(method.equals("set")) {
				  System.out.println("Serving Reducer SET Request");
				  getKeyStore(arr[2].trim()+".txt");
				this.keyStore.put(arr[3].trim(),arr[4].trim());
				writeToDisk(arr[2].trim()+".txt");
				
			}else {
				  System.out.println("Serving Reducer GET Request");
				getKeyStore(arr[2].trim()+".txt");
				for(Map.Entry<String,String> map : this.keyStore.entrySet()) {
					this.out.writeUTF(map.getKey()+","+map.getValue());
				}
				this.out.writeUTF("");
			}

		}
		
		
		private void serveMasterRequest(String[] arr, String method) throws IOException {
           System.out.println("Serving Master's Request");
			if(method.equals("set")) {
				getKeyStore(arr[1].trim()+".txt");
				this.keyStore.put(arr[2].trim(),arr[4].trim());
				writeToDisk(arr[1].trim()+".txt");
			}else {

                if(this.keyStore.containsKey(arr[2].trim())) {
                	this.out.writeUTF(this.keyStore.get(arr[2].trim()));
                }

			}
		}


		private void serveMapperRequest(String[] arr, String method) throws IOException  {
		
			if(method.equals("set")) {
				System.out.println("Mapper set request");
				System.out.println(arr[2].trim()+".txt");
				getKeyStore(arr[2].trim()+".txt");
				this.keyStore.put(arr[3].trim(),arr[4].trim());
				writeToDisk(arr[2].trim()+".txt");
				System.out.println("Finised SET");
			}else {
				
				System.out.println("Mapper get request");
				System.out.println(arr[3].trim());
				getKeyStore(arr[3].trim());
				if(this.keyStore.containsKey(arr[2].trim())) {
					if(arr[4].trim().equals("wc")) {
						this.out.writeUTF(this.keyStore.get(arr[2].trim()));
						this.out.writeUTF("");
					}else {
						String[] docsList =  this.keyStore.get(arr[2].trim()).split("-");
						StringBuilder sb = new StringBuilder();
						
						for(String doc :docsList ) {
							System.out.println("Document being sent -->" + doc);
							String line = null;
							sb.append(doc);
							sb.append(",");
						try { 
							BufferedReader br = new BufferedReader(new FileReader(doc.trim()));
							if(br != null) {
								line = br.readLine();
							}
							while(line != null && line.length() > 0) {
								sb.append(line.replace("-"," ").replace(",", " ").replace("\r\n"," ").replace("\n"," ").replace("\r"," "));
								line = br.readLine();
							}
						} catch(IOException ie) {
							sb.append("Error in reading document");
							sb.append(",");
							continue;
						}
							sb.append(",");
						}
						
						this.out.writeUTF(sb.toString());
					}
				}else {
					this.out.writeUTF("");
				}
			}
		}

	}



	@Override
	public void run() {
		try
		{ 
			ServerSocket server = new ServerSocket(); 
			InetAddress inetAddress=InetAddress.getByName(this.keyAddress);  
			SocketAddress socketAddress=new InetSocketAddress(inetAddress, this.port);  
			server.bind(socketAddress);  
			while(true) {
				this.socket = server.accept(); 
				Thread t = new Thread(new KeyStoreThread(this.socket));
				t.start();
			} }
		catch(IOException i) 
		{ 
			System.out.println(i); 
		} 

	} 



	public static void main(String args[]) throws InterruptedException 
	{ 
		// 9029
		//String keyAddress = args[0];
		//String port = args[1];
		Thread server = new Thread(new KeyStore("localhost",9090));
		server.start();
		System.out.println("Server started");
	}

} 
