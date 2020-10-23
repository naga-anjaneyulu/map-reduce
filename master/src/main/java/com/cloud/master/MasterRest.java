package com.cloud.master;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
	
	private static int  mappers = 0;
	private static int  reducers = 0;
	private static String mapFunction = "";
	private static String reduceFunction = "";
	
	
	@RequestMapping(value = "/init_cluster")
    public ResponseEntity<?> initCluster(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, String> result = new HashMap<>();
        Map<String, String> reqBody = requestParser.processRequest(request);
       
        mappers = Integer.parseInt(reqBody.get("mappers"));
        reducers = Integer.parseInt(reqBody.get("reducers"));
        mapFunction = reqBody.get("mapFunction");
        reduceFunction = reqBody.get("reduceFunction");
        
        if(mapFunction.equals("wc")) {
        	masterService.processWCInput(mappers);
		}else {
			masterService.processIIInput(mappers);
		}

       //Compute compute = ComputeEngine.getComputeEngine();
       //Operation op = ComputeEngine.startInstance(compute,"mapper");
      // Operation.Error error = ComputeEngine.blockUntilComplete(compute, op, 60*1000);
        Operation.Error error = null;
       if (error == null) {
    	   System.out.println("Success!");
    	   result.put("Status","Success");
       } else {
    	   result.put("Status",error.toPrettyString());
    	   System.out.println(error.toPrettyString());
       }

        return ResponseEntity.ok(result);
    }
	
	
	@RequestMapping(value = "/mapData")
    public ResponseEntity<?> getMapperId(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, String> result = new HashMap<>();
        result.put("id",Integer.toString(mappers--));
        result.put("fileName","master");

        return ResponseEntity.ok(result);
    }
	
	
	

}
