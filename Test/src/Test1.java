import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class Test1 {

	// Starting mapreduce 
	public static void main(String[] args) {
		HttpURLConnection con = null;
        BufferedReader in = null;
        try {	
    		URL url = new URL("http://34.95.152.19:8080/mapred");
    		con = (HttpURLConnection) url.openConnection();
    		HttpURLConnection http = (HttpURLConnection)con;
    		http.setRequestMethod("POST"); 
    		http.setDoOutput(true);
    		byte[] out = "{\"mappers\":\"1\",\"reducers\":\"2\",\"mapFunction\":\"wc\",\"reduceFunction\":\"wc\"}".getBytes(StandardCharsets.UTF_8);
    		
    		
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
                if(in != null)
                    in.close();
                if(con != null)
                    con.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
	}

}
