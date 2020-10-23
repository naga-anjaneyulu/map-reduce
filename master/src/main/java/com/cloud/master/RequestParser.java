package com.cloud.master;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;

@Service
public class RequestParser {

	public Map<String, String> processRequest(HttpServletRequest request) {
		
		 Map<String, String> postBody = new HashMap<>();
	        try {
	            populatePostBody(postBody, request.getReader().lines().collect(Collectors.joining()));
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        return postBody;
		
	}

	private void populatePostBody(Map<String, String> postBody, String body) {
		try {
            JSONObject userDetails = (JSONObject) new JSONParser().parse(body);
            for(Object key: userDetails.keySet()) {
                if(userDetails.get(key) instanceof String) {
                	postBody.put((String)key, (String)userDetails.get(key));
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
		
	}

}
