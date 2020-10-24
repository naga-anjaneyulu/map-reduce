package com.cloud.reducer;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

@Component("reduce")
public class Reducer {

	
	private int key;
	private String function;

	public Reducer() {
		
	}
//	public Reducer(int key,String function) {
//		this.key = key;
//		this.function = function;
//	}
	

	public HashMap<String,String> loadInput(String address,String port) throws IOException {
		System.out.println("Reducing Loading input data with key =>"+this.key);
		Socket socket = new Socket(address, Integer.parseInt(port));   
		DataOutputStream out = new DataOutputStream(socket.getOutputStream()); 
		DataInputStream input = new DataInputStream(socket.getInputStream());
		out.writeUTF("get \r\n reducer \r\n  red"+this.function+this.key+" \r\n "); 
		String value = input.readUTF();
		HashMap<String,String> keyValues = new HashMap<>();
		while(value.length() > 0) {
			System.out.println("Retrieved msg "+value);
			value = input.readUTF();
			String[] arr = value.split(",");
			if(arr.length == 2)
				keyValues.put(arr[0], arr[1]);
			}
	   socket.close();
	   return keyValues;
	}
	
	
	private void invertedIndex(HashMap<String, String> inputData,String address,String port) throws NumberFormatException, UnknownHostException, IOException {
		
		HashMap<String,Set<String>> docList = new HashMap<>();
		HashMap<String,Integer> wordCount = new HashMap<>();
		
		for(Map.Entry<String,String> map : inputData.entrySet()) {
			String[] arr = map.getValue().split("-");
			wordCount.put(map.getKey(),wordCount.getOrDefault(map.getKey(),0) + 1);
			docList.computeIfAbsent(map.getKey(),k -> new HashSet<>()).add(arr[0]);
		}
		
		
		for(Map.Entry<String,Integer> map : wordCount.entrySet()) {
			Socket socket = new Socket(address, Integer.parseInt(port));  
			DataOutputStream out = new DataOutputStream(socket.getOutputStream()); 
			DataInputStream input = new DataInputStream(socket.getInputStream());
			Set<String> docsList = docList.get(map.getKey());
			String docs = "";
			for(String s: docsList) docs+= s+"|"; 
			String line = "set \r\n reducer \r\n  out"+this.function+this.key+" \r\n "+map.getKey()+" \r\n "+map.getValue()+ "|"+docs+" \r\n";
			out.writeUTF(line); 
			socket.close();
					
		}
	
		
	}


	private void wordCount(HashMap<String, String> inputData,String address,String port) throws NumberFormatException, UnknownHostException, IOException {
		System.out.println("Reducer Word Count function");
		HashMap<String,Integer> wordCount = new HashMap<>();
		
		for(Map.Entry<String,String> map : inputData.entrySet()) {
			wordCount.put(map.getKey(),wordCount.getOrDefault(map.getKey(),0) + 1);
		}
		
		for(Map.Entry<String,Integer> map : wordCount.entrySet()) {
			Socket socket = new Socket(address, Integer.parseInt(port)); 
			DataOutputStream out = new DataOutputStream(socket.getOutputStream()); 
			DataInputStream input = new DataInputStream(socket.getInputStream());
			String line = "set \r\n reducer \r\n  out"+this.function+this.key+" \r\n "+map.getKey()+" \r\n "+map.getValue()+" \r\n";
			out.writeUTF(line); 
			socket.close();
				} 	
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
	
	
	public void start() throws IOException {

		System.out.println("Reducer Started");
		HashMap<String,String> response = new HashMap<>();
		HttpURLConnection con = null;
        BufferedReader in = null;
        try {
            URL urlObj = new URL("http://34.95.152.19:8080/redData");
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
            
            int key = Integer.parseInt(response.get("key").trim());
    		String function = response.get("redFunction");
    		String address = response.get("keyStoreAddress");
    		String port = response.get("keyStorePort");
    		System.out.println("function @ reducer ---> "+function  +"with key --->"+ key);
    		this.key = key;
    		this.function = function.trim();
    		HashMap<String,String>  inputData = loadInput(address,port);
    		if(this.function.trim().equals("wc")) {
    			wordCount(inputData,address,port);
    		}else {
    			invertedIndex(inputData,address,port);
    		}
    		
    		
    		URL url = new URL("http://34.95.152.19:8080/killReducer");
    		con = (HttpURLConnection) url.openConnection();
    		HttpURLConnection http = (HttpURLConnection)con;
    		http.setRequestMethod("POST"); 
    		http.setDoOutput(true);
    		byte[] out = ("{\"key\":\""+this.key+" 1\"}").getBytes(StandardCharsets.UTF_8);
    		int length = out.length;
    		http.setFixedLengthStreamingMode(length);
    		http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    		http.connect();

    		
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	
        	URL url = new URL("http://34.95.152.19:8080/killReducer");
    		con = (HttpURLConnection) url.openConnection();
    		HttpURLConnection http = (HttpURLConnection)con;
    		http.setRequestMethod("POST"); 
    		http.setDoOutput(true);
    		byte[] out = ("{\"key\":\""+this.key+" 0\"}").getBytes(StandardCharsets.UTF_8);
    		int length = out.length;
    		http.setFixedLengthStreamingMode(length);
    		http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        	
        	
            try {
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

