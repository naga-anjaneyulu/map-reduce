package com.cloud.master;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Operation;

@RestController
public class MasterRest {
	
	@Autowired
	RequestParser requestParser;
	
	@Autowired
	MasterService masterService;
	
	
	private static HashMap<Integer,String> mapStatusMap = new HashMap<>();
	private static HashMap<Integer,String> redStatusMap = new HashMap<>();
	private static int  mappers = 0;
	private static int  reducers = 0;
	private static String mapFunction = "";
	private static String reduceFunction = "";
	private static Queue<Integer> mapQueue = new LinkedList<>();
	private static Queue<Integer> redQueue = new LinkedList<>();

	
	@RequestMapping(value = "/mapred")
    public ResponseEntity<?> initCluster(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, String> result = new HashMap<>();
        Map<String, String> reqBody = requestParser.processRequest(request);
       
        mappers = Integer.parseInt(reqBody.get("mappers"));
        reducers = Integer.parseInt(reqBody.get("reducers"));
        mapFunction = reqBody.get("mapFunction");
        reduceFunction = reqBody.get("reduceFunction");
        reducers = reducers-1;
        if(mapFunction.equals("wc")) {
        	masterService.processWCInput(mappers);
		}else {
			masterService.processIIInput(mappers);
		}

        
        
       for(int i = 1; i <= mappers ;i++) {
    	   System.out.println("Creating mappers");
    	   Compute compute = ComputeEngine.getComputeEngine();
           Operation op = ComputeEngine.startInstance(compute,"mapper"+i,"mapper.sh");
           Operation.Error error = ComputeEngine.blockUntilComplete(compute, op, 60*1000);
           if (error == null) {
        	   System.out.println("Success!");
        	   result.put(Integer.toString(i),"Success");
        	   mapQueue.add(i);
           } else {
        	   result.put("Status",error.toPrettyString());
        	   System.out.println(error.toPrettyString());
           }
    	   
       }
       
       
       checkStatus("mapper",mapStatusMap,mappers);
       
    
       for(int i = 0; i <= reducers;i++) {
    	   Compute compute = ComputeEngine.getComputeEngine();
           Operation op = ComputeEngine.startInstance(compute,"reducer"+i,"reducer.sh");
           Operation.Error error = ComputeEngine.blockUntilComplete(compute, op, 60*1000);
           if (error == null) {
        	   System.out.println("Success!");
        	   result.put("status","Success");
           } else {
        	   result.put("Status",error.toPrettyString());
        	   System.out.println(error.toPrettyString());
           }
       }
        
       
       checkStatus("reducer",redStatusMap,reducers);
       
        return ResponseEntity.ok(result);
    }
	
	
	private void checkStatus(String processType,HashMap<Integer,String> statusMap,int count) throws Exception {
		
	       while(count > 0) {
	    	   
	    	   for(Map.Entry<Integer,String> map :statusMap.entrySet()) {
	    		   if(map.getValue().equals("Completed")) count--;
	    		   else if(map.getValue().equals("Error")) {
	    			   System.out.println("Deleting Instance because of error");
	    			   Compute compute = ComputeEngine.getComputeEngine();
	    	           Operation op = ComputeEngine.deleteInstance(compute,processType+map.getKey().toString());
	    	           Operation.Error error = ComputeEngine.blockUntilComplete(compute, op, 60*1000);
	    	           if (error == null) {
	    	        	   System.out.println("Success!");
	    	           } else {
	    	        	   System.out.println(error.toPrettyString());
	    	           }
	    			   startNewInstance(map.getKey(),processType);
	    		   }
	    	   }
	       }
		
	}


	private void startNewInstance(Integer key, String processType) throws Exception {
		 Compute compute = ComputeEngine.getComputeEngine();
		 Operation op ;
		 if(processType.equals("mapper")) {
			  op = ComputeEngine.startInstance(compute,"mapper"+key,"mapper.sh");
			 mapQueue.add(key);
		 }else {
			  op = ComputeEngine.startInstance(compute,"reducer"+key,"reducer.sh");
			 redQueue.add(key);
		 }
         
         Operation.Error error = ComputeEngine.blockUntilComplete(compute, op, 60*1000);
         if (error == null) {
      	   System.out.println("Success!");
      	   
         } else {
      	   System.out.println(error.toPrettyString());
         }
		
	}


	@RequestMapping(value = "/mapData")
    public ResponseEntity<?> getMapperData(HttpServletRequest request, HttpServletResponse response) throws Exception {
		 System.out.println("Responding to get mapData");
        Map<String, String> result = new HashMap<>();
        while(mapQueue.size() == 0) continue;
        result.put("key",Integer.toString(mapQueue.poll()));
        result.put("file","master.txt");
        result.put("reducers", Integer.toString(reducers));
        result.put("mapFunction", mapFunction);
        result.put("keyStoreAddress","35.199.88.215");
        result.put("keyStorePort","8080");
        System.out.println("Finished Responding to get mapData");
        return ResponseEntity.ok(result);
    }
	
	@RequestMapping(value = "/killMapper")
    public ResponseEntity<?> killMapper(HttpServletRequest request, HttpServletResponse response) throws Exception {
		System.out.println("Responding to KillMapper");
		  Map<String, String> result = new HashMap<>();
	      Map<String, String> reqBody = requestParser.processRequest(request);
	      
	      for(Map.Entry<String,String> map : reqBody.entrySet()) {
	    	  System.out.println("Recieved msg ==>"+map.getValue());
	    	  String[] arr = map.getValue().trim().split("\\s");
	    	  if(arr[1].trim().equals("1"))
	    	     mapStatusMap.put(Integer.parseInt(arr[0].trim()),"Completed");
	    	  else {
	    		  mapStatusMap.put(Integer.parseInt(arr[0].trim()),"Error");
	    		 System.out.println("Deleting Instance because of error");
   			     Compute compute = ComputeEngine.getComputeEngine();
   	             Operation op = ComputeEngine.deleteInstance(compute,"mapper"+map.getKey().toString());
   	             Operation.Error error = ComputeEngine.blockUntilComplete(compute, op, 60*1000);
   	           if (error == null) {
   	        	   System.out.println("Success!");
   	           } else {
   	        	   System.out.println(error.toPrettyString());
   	           }
	    		  startNewInstance(Integer.parseInt(arr[0].trim()),"mapper");
	    	  }
	    		
	    	   
	      }
	      System.out.println("Finshed Responding to KillMapper");
	      
        return ResponseEntity.ok(result);
    }
	

	@RequestMapping(value = "/redData")
    public ResponseEntity<?> getReducerrData(HttpServletRequest request, HttpServletResponse response) throws Exception {
		System.out.println("Responding to RedData");
        Map<String, String> result = new HashMap<>();
        while(redQueue.size() == 0) continue;
        result.put("key",Integer.toString(redQueue.poll()));
        result.put("redFunction", reduceFunction);
        result.put("keyStoreAddress","35.199.88.215");
        result.put("keyStorePort","8080");
        System.out.println("Finished Responding to redData");
        return ResponseEntity.ok(result);
    }
	
	@RequestMapping(value = "/killReducer")
    public ResponseEntity<?> killReducer(HttpServletRequest request, HttpServletResponse response) throws Exception {
		System.out.println("Responding to KillReducer");
		  Map<String, String> result = new HashMap<>();
	      Map<String, String> reqBody = requestParser.processRequest(request);
	      
	      for(Map.Entry<String,String> map : reqBody.entrySet()) {
	    	  System.out.println("Recieved msg ==>"+map.getValue());
	    	  String[] arr = map.getValue().trim().split("\\s");
	    	  if(arr[1].trim().equals("1"))
	    	     redStatusMap.put(Integer.parseInt(arr[0].trim()),"Completed");
	    	  else {
	    		  redStatusMap.put(Integer.parseInt(arr[0].trim()),"Error");
	    		  System.out.println("Deleting Instance because of error");
	   			     Compute compute = ComputeEngine.getComputeEngine();
	   	             Operation op = ComputeEngine.deleteInstance(compute,"reducer"+map.getKey().toString());
	   	             Operation.Error error = ComputeEngine.blockUntilComplete(compute, op, 60*1000);
	   	           if (error == null) {
	   	        	   System.out.println("Success!");
	   	           } else {
	   	        	   System.out.println(error.toPrettyString());
	   	           }
	    		  startNewInstance(Integer.parseInt(arr[0].trim()),"reducer");
	    	  }
	    		  
	    		  
	      }
			System.out.println("Finished Responding to KillReducer");

	      
        return ResponseEntity.ok(result);
    }

}
