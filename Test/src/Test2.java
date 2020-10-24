import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
// Get Mapper DATA	
public class Test2 {
  
	public static void main(String[] args) {
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
   
            System.out.println(content.toString());
   
            con.disconnect();
            in.close();
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
	
	

