package com.cloud.master;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.springframework.stereotype.Service;

@Service
public class MasterService {

	
	public String storeInputData(StringBuilder data, String key) throws IOException {
		System.out.println("Splitting data and storing in keyvalue store");
		 String inputData = data.toString();
		 
		 Socket socket = new Socket("10.158.0.11",9092); 
		 DataOutputStream out = new DataOutputStream(socket.getOutputStream()); 
		 DataInputStream input = new DataInputStream(socket.getInputStream());
		 byte[] bytes = inputData.getBytes("UTF-8");
		 int sizeInBytes = bytes.length;
		 inputData = inputData.replace("\n"," ").replace("\r"," ").replace(","," ");
		 String line = "set \r\n master \r\n "+key +" \r\n "+sizeInBytes+" \r\n "+inputData+" \r\n ";
		 String ack = "";
			while (ack.length() == 0) 
			{ 
				out.writeUTF(line); 
				ack = input.readUTF();	
				System.out.println(ack);
			} 
			socket.close();
		return ack;
	}
	
	public void processWCInput(int mappers) throws IOException {
		System.out.println("Processing WC Input data");
		 String filePath = System.getProperty("user.dir");
		 FileReader file = new FileReader("/home/nakopa/map-reduce/master/src/main/resources/data/input.txt");
		 Scanner scanner = new Scanner(file);
		 List<String> lines = new ArrayList<>();
		 while(scanner.hasNext()) lines.add(scanner.nextLine());
		 StringBuilder data = new StringBuilder(); int count = 1;
		 int splitSize = lines.size()/mappers;
		 System.out.println("Lines -->" + lines.size() +" SplitSize --->" + splitSize);
		 for(int i = 0 ; i<lines.size();i++) {
			 if( i == splitSize*count) {
				 String ack = storeInputData(data,"mapwc"+count);
				 count++;
				 if(ack.length() > 0) {
					 data = new StringBuilder();
				 }
				
			 }else {
				 data.append(lines.get(i)); } }
		 if(data.length() > 0) {
			 String ack = storeInputData(data,"mapwc"+count);
			 count++;
			 if(ack.length() > 0) {
				 data = new StringBuilder();
			 }
		 }
		 System.out.println("Finished processing WC Input data");
		
	}

	public void processIIInput(int mappers) throws IOException {
		System.out.println("Processing II Input data");
        String filePath = System.getProperty("user.dir");
		File path = new File("/home/nakopa/map-reduce/master/src/main/resources/data");
	    File [] files = path.listFiles();
	    int splitSize = files.length/mappers;int count =1;
	    StringBuilder fileList = new StringBuilder();
	    for (int i = 0; i < files.length; i++){
	    	 if( i == splitSize*count) {
				 String ack = storeInputData(fileList,"mapii"+count++);
				 if(ack.length() > 0) {
					 fileList = new StringBuilder();}
			 }else {
				 fileList.append(files[i].getName());
				 fileList.append("-"); } }
		 if(fileList.length() > 0) {
			 String ack = storeInputData(fileList,"mapii"+count++);
			 if(ack.length() > 0) {
				 fileList = new StringBuilder();
			 }
	    }
		
		 System.out.println("Finished processing II Input data");
		
	}

}
