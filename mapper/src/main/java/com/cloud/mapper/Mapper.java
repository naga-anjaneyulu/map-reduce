package com.cloud.mapper;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
@Component("mapper")
public class Mapper {
	
	String mapKey;
	String inputFile;
	String function;
	int reducers;

     public Mapper() {
    	 
     }
	
	public String loadInput(String address , String port) throws IOException {
		System.out.println("Mapper Loading input data with key =>"+this.mapKey);
		Socket socket = new Socket(address,Integer.parseInt(port));  
		DataOutputStream out = new DataOutputStream(socket.getOutputStream()); 
		DataInputStream input = new DataInputStream(socket.getInputStream());
		System.out.println("get \r\n mapper \r\n map"+this.function+this.mapKey+" \r\n "+this.inputFile+" \r\n "+ this.function + " \r\n ");
		out.writeUTF("get \r\n mapper \r\n map"+this.function+this.mapKey+" \r\n "+this.inputFile+" \r\n "+ this.function + " \r\n "); 
		String msg = "";
		String value = input.readUTF();
		msg += value;
		System.out.println("Retrieved msg "+value);
		socket.close();
		System.out.println("finised loading input");
		return msg;
	}
	
	
	
	private void wordCount(String inputData,String address, String port) throws NumberFormatException, UnknownHostException, IOException {
		System.out.println("Mapper WordCount");
		String[] arr = inputData.replace("'","").replace("`","").replace(".","").replace(":","").split("\\s");
		
		
		int count = 0;
		for(String s : arr) {
			if(count > 20) break;
			
			count++;
			if(s.length() > 0) {
				int partition = (s.hashCode() % this.reducers);
				System.out.println(s);
				Socket socket = new Socket(address, Integer.parseInt(port)); 
				DataOutputStream out = new DataOutputStream(socket.getOutputStream()); 
				DataInputStream input = new DataInputStream(socket.getInputStream());
				System.out.println("set \r\n mapper \r\n  red"+this.function+partition+" \r\n "+s+" \r\n "+1+" \r\n");
				String line = "set \r\n mapper \r\n  red"+this.function+partition+" \r\n "+s+" \r\n "+1+" \r\n";
				String ack = "";
				out.writeUTF(line); 
				socket.close();
			}
			 
		}
		
			
	}
	

	private void invertedIndex(String inputData,String address,String port) throws NumberFormatException, UnknownHostException, IOException {
		System.out.println("Mapper Inverted Index");
		
		String[] arr = inputData.split(",");
		for(int i = 0; i+1 <arr.length;i =i+2) {
			String[] words = arr[i+1].split("\\s");
			
			for(String s : words) {
				if(s.length() > 0) {
					Socket socket =  new Socket(address, Integer.parseInt(port)); 
					DataOutputStream out = new DataOutputStream(socket.getOutputStream()); 
					DataInputStream input = new DataInputStream(socket.getInputStream());
					int partition = (s.hashCode() % this.reducers);
					String line = "set \r\n mapper \r\n  red"+this.function+partition+" \r\n "+s+" \r\n "+arr[i]+"-"+1+" \r\n";
					out.writeUTF(line); 
					socket.close();
				}}	}
	}
	

	private  void processResponseBody(Map<String, String> response, String body) {
		try {
           JSONObject userDetails = (JSONObject) new JSONParser().parse(body);
           for(Object key: userDetails.keySet()) {
               if(userDetails.get(key) instanceof String) {
            	   response.put((String)key, (String)userDetails.get(key));
               }
           }
       } catch (ParseException e) {
           e.printStackTrace();
       }}
	
	
	public void start() {

		System.out.println("Mapper Started");
		HashMap<String,String> response = new HashMap<>();
		HttpURLConnection con = null;
        BufferedReader in = null;
        try {
            URL urlObj = new URL("http://34.95.152.19:8080/mapData");
            con = (HttpURLConnection) urlObj.openConnection();
            con.setRequestMethod("GET");
            in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null)
                content.append(inputLine);

            processResponseBody(response,content.toString());

            con.disconnect();
            in.close();
            
            String key = response.get("key");
    		String file = response.get("file");
    		int reducers = Integer.parseInt(response.get("reducers"));
    		String function = response.get("mapFunction");
    		String address = response.get("keyStoreAddress");
    		String port = response.get("keyStorePort");
    		System.out.println("function @ mapper ---> "+function  +"with key --->"+ key);
    		
    		this.mapKey = key.trim();
    		this.reducers = reducers;
    		this.inputFile = file.trim();
    		this.function = function.trim();
    		String inputData = loadInput(address,port);
    		if(function.trim().equals("wc")) {
    			System.out.println("Mapper Enter WC");
    			wordCount(inputData,address,port);
    		}else {
    			System.out.println("Mapper Enter II");
    			invertedIndex(inputData,address,port);
    		}
    		
    		
    		URL url = new URL("http://34.95.152.19:8080/killMapper");
    		con = (HttpURLConnection) url.openConnection();
    		HttpURLConnection http = (HttpURLConnection)con;
    		http.setRequestMethod("POST"); 
    		http.setDoOutput(true);
    		byte[] out = ("{\"key\":\""+this.mapKey+" 1\"}").getBytes(StandardCharsets.UTF_8);
    		int length = out.length;
    		http.setFixedLengthStreamingMode(length);
    		http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    		http.connect();
    		try(OutputStream os = http.getOutputStream()) {
    		    os.write(out);
    		}
    
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
            	URL url = new URL("http://34.95.152.19:8080/killMapper");
        		con = (HttpURLConnection) url.openConnection();
        		HttpURLConnection http = (HttpURLConnection)con;
        		http.setRequestMethod("POST"); 
        		http.setDoOutput(true);
        		byte[] out = ("{\"key\":\""+this.mapKey+" 0\"}").getBytes(StandardCharsets.UTF_8);
        		int length = out.length;
        		http.setFixedLengthStreamingMode(length);
        		http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        		http.connect();
        		try(OutputStream os = http.getOutputStream()) {
        		    os.write(out);
        		}
            	
            	
            	
            	
                if(in != null)
                    in.close();
                if(con != null)
                    con.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
		System.exit(0);
		
	
	}
	
	public static void main(String[] args) throws NumberFormatException, UnknownHostException, IOException {}


}
