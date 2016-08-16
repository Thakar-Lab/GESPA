/*GESPA  Copyright (C) 2013  Jay Khurana
 * See GESPA License for additional information.
 */
package DNAFinderMain;

import java.util.*;
import java.sql.*;
import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 *
 * @author Jay Khurana
 */
public class Batch {
    private List<String> geneURLs;
    private List<String> genes;
    private List<List<String>> batchSNPs;
    private List<String>genesInSQL;
    private String errorReport;
    private BatchManager b;
    public static boolean activeBatch=false;

    
    public Batch(String fileText, BatchManager b) throws Exception{
        this.b=b;
        geneURLs = new ArrayList<>();
        batchSNPs=new ArrayList<>();
        genes=new ArrayList<>();
        setGenesInSQL();
        errorReport="";
        String tempGene="";
        Framework geneCheck=new Framework();
        geneCheck.setBatchManager(b);
        try{
            while(true){
                try{
                    fileText=fileText.substring(fileText.indexOf("Gene:"));
                    tempGene=fileText.substring(5,fileText.indexOf("\r")).trim();
                }catch(Exception exe){
                    if(geneURLs.isEmpty()){
                        b.addError("No genes entered!\n");
                        throw exe;
                    } else if(fileText.indexOf("\r")==-1)
                        break;
                    else
                        try{
                            fileText=fileText.substring(1,fileText.substring(1).indexOf("Gene:"));
                        }catch(StringIndexOutOfBoundsException exe2){
                            break;
                        }    
                }
                JFrameMain.progLabel.setText("Validating gene: "+tempGene);                
                boolean inSQL=false;
                for(String s: genesInSQL){
                    if(tempGene.toUpperCase().replace("-", "").equals(s)){
                        inSQL=true;
                        geneURLs.add(null);
                        break;
                    }
                }
                if(!inSQL){
                    System.out.print("Not in cloud: ");
                    try{
                        String check=geneCheck.checkGene(tempGene);
                        if(check==null){
                            throw new Exception();
                        }
                        else
                            geneURLs.add(check);
                    }catch(Exception exe){
                        if(exe instanceof SocketTimeoutException){
                            b.addError("Timeout when validating gene: "+tempGene+".\r");
                        }else if(exe instanceof IOException)
                            throw exe;
                        else
                            b.addError(tempGene+" is not a valid gene.\r");
                        try{    
                            fileText=fileText.substring(1);
                        }catch(Exception exe2){
                            break;
                        }
                        continue;
                    }
                }
                genes.add(tempGene);
                int SNPCounter=0;
                List<String>SNPs=new ArrayList<>();
                while(true){
                    String tempSNP;
                    try{
                       int nextGeneIndex=fileText.substring(1).indexOf("Gene:")-1;
                       if(nextGeneIndex==-2)
                           nextGeneIndex=fileText.length();
                       if(fileText.indexOf("\r")>nextGeneIndex)
                           throw new StringIndexOutOfBoundsException();
                       fileText=fileText.substring(fileText.indexOf("\r")+1);
                       tempSNP=fileText.substring(0,fileText.indexOf("\r"));
                       SNPCounter++;
                       SNPs.add(tempSNP);
                    }catch(StringIndexOutOfBoundsException exe){
                        if(SNPCounter==0){
                            b.addError("Could not find any "
                                + "valid SNPs for"+tempGene+"\r");
                           throw exe;
                        }
                        batchSNPs.add(SNPs);
                        break;                       
                    }
                }
            }                    
            
        }catch(Exception exe){
            System.out.println("Caught Exception:");
            exe.printStackTrace();
            throw exe;
        }
    }
    
    public List<String> getGeneUrls(){
        return geneURLs;
    }
    
    public List<String> getGenes(){
        return genes;
    }
    
    public List<List<String>> getSNPs(){
        return batchSNPs;
    }
    
    public String getErrorReport(){
        return errorReport;
    }
    
    private void setGenesInSQL(){
        genesInSQL=new ArrayList<>();
        Connection con=null;
        Statement stmt=null;
        ResultSet rs=null;        
        String connectionUrl="jdbc:sqlserver://genes.cedf0sccpt06.us-east-1.rds.amazonaws.com:1433;"
               + "databaseName=Genes;user=allAccess;password=xFz23d4a3Ln5dZ5sW";       
        try{            
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            con = DriverManager.getConnection(connectionUrl);            
            if(con==null)
                throw new SQLException();
            String sqlQuery =
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                    + "WHERE TABLE_TYPE='BASE TABLE'";  
            stmt = con.createStatement();
            rs=stmt.executeQuery(sqlQuery);
            while(rs.next()){
                genesInSQL.add(rs.getString(1).toUpperCase());
            }            
        }catch(SQLException | ClassNotFoundException | NumberFormatException exe){
        }finally{
            if(rs!=null)
                try{
                    rs.close();
                }catch(SQLException exe){}
            if(stmt!=null)
                try{
                    stmt.close();
                }catch(SQLException exe){}
            if(con!=null)
                try{
                    con.close();
                }catch(SQLException exe){}            
        }
    }
    
}
