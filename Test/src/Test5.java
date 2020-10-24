import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

// Kill mapper after failed mapper job and start again
public class Test5 {
	public static void main(String[] args) {
		HttpURLConnection con = null;
        BufferedReader in = null;
        try {	
    		URL url = new URL("http://34.95.152.19:8080/killMapper");
    		con = (HttpURLConnection) url.openConnection();
    		HttpURLConnection http = (HttpURLConnection)con;
    		http.setRequestMethod("POST"); 
    		http.setDoOutput(true);
    		String msg = "1 0";
    		byte[] out = ("{\"key\":\""+msg+"\"}").getBytes(StandardCharsets.UTF_8);
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
