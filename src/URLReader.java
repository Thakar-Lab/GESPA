/*GESPA  Copyright (C) 2013  Jay Khurana
 * See GESPA License for additional information.
 */
package DNAFinderMain;

/**
 *
 * @author Jay Khurana
 */
import java.net.*;
import java.io.*;

public class URLReader {
    private String URLHTML;
    
    //opens connection to URL and searches for string
    public boolean findStringinURL(String URL, String s) throws Exception {
        URLHTML="";
	StringBuilder htmlBuilder = new StringBuilder();
        URL link = new URL(URL);
        URLConnection c=link.openConnection();
        c.setDoInput(true);
        c.setConnectTimeout(90000);
        c.setReadTimeout(90000);
        BufferedReader in=null;
        try{    
            in = new BufferedReader(
            new InputStreamReader(c.getInputStream()));
        }catch(Exception exe){
            if(in!=null){
                in.close();
            }
            in = new BufferedReader(
                new InputStreamReader(c.getInputStream()));
        }
        String inputLine;
        try{
            while ((inputLine = in.readLine()) != null)
                htmlBuilder.append(inputLine);
        }finally{
            try{
                in.close();
            }catch(IOException exe){
                try{
                    in.close();
                }catch(IOException exe2){
                    throw exe2;
                }    
            }            
        }
        URLHTML=htmlBuilder.toString();
        URLHTML=URLHTML.replaceAll("</em>", "");  
        URLHTML=URLHTML.replaceAll("<em>", "");  
        return URLHTML.contains(s);
    }
      
    //finds hyperlink un URLHTML
    public String extractHyperLink(int backTrack, String s, String link){
    	String sub = URLHTML.substring(URLHTML.indexOf(s)-backTrack, URLHTML.indexOf(s));
		int stop1, stop2;
		stop1=sub.indexOf(link);
		String subSub=sub.substring(stop1);
		stop2=stop1+subSub.indexOf("\"");
                return sub.substring(stop1,stop2);	
    }
    
    //returns string in URL at specified location
    public String extractString(String s, String stop){
    	String sub = URLHTML.substring(URLHTML.indexOf(s),URLHTML.indexOf(s)+500);
    	return sub.substring(0,sub.indexOf(stop));	
    }
    
    public String getHTML(){
    	return URLHTML;
    }
}