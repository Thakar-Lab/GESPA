/*GESPA  Copyright (C) 2013  Jay Khurana
 * See GESPA License for additional information.
 */

package DNAFinderMain;

/**
 *
 * @author Jay Khurana
 */
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.*;
import javax.swing.*;
import java.net.URI;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.RefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.IncorrectnessListener; 
import java.io.IOException;
import java.net.URISyntaxException;
import java.awt.Rectangle;
import org.w3c.dom.*;
import org.biojava.bio.BioException;
import java.net.SocketTimeoutException;
import java.io.*;
import java.sql.*;
public class Framework implements Runnable{
          
    /**
     * Creates a new instance of <code>Framework</code>.
     */
    private URLReader urlReader1;
    private String gene;
    private String geneSearchURL;
    private String geneURL;
    private JTable table;
    private GenePanel panel;
    private int threadNum;
    private int numHomologs;//or number of orthologs
    private int numParalogs;
    private String nFasta; 
    private String pFasta;
    private String nFasta2;
    private String pFasta2;
    private String tempDNA;
    private String aFasta;
    private String report;
    private String[] reports;
    private int reportCounter;
    private SNP[] allSNPs;
    private SNP[] tempSNPs;
    private DefaultTableModel model;
    private Framework f;    
    private Gene gene1;
    private Gene geneTemp;
    private BatchManager b;
    private String[] rowURL;
    private String[] geneProperties;
    private int numClinicalSNPs;
    private boolean existingSNPs;
    private boolean noExistingSNPs=false;
    private boolean batchComplete=false;
    private boolean activeBatch=false;
    private boolean saveDataToCloud=true;
    private boolean SQLError = false;
    private boolean geneInSQL;
    private boolean repeatCheck=false;
    private boolean sqlTimeCheck=true;
    private Connection con;    
    private List<String> SNPs;
    private List<String> PSICScore;
    private List<String> toleratedSubst;
    
    public static int exception=0;
   
    //Initializes some instance variables
    public Framework(){
        this.existingSNPs = true;
        geneSearchURL=nFasta=pFasta=nFasta2=pFasta2=tempDNA=aFasta=report="";
        reports=new String[5000];
        rowURL=new String[5000];
        geneProperties=new String[9];
        for(int i=0; i<5000; i++)
            reports[i]="";
        reportCounter=threadNum=numClinicalSNPs=numHomologs=numParalogs=0;
        PSICScore=new ArrayList<>();
        toleratedSubst=new ArrayList<>();
        geneInSQL=false;
    }
    
    public void nullFramework(){
     urlReader1=null;
     gene=null;
     geneSearchURL=null;
     geneURL=null;
     table=null;
     panel=null;
     nFasta=null; 
     pFasta=null;
     nFasta2=null;
     pFasta2=null;
     tempDNA=null;
     aFasta=null;
     report=null;
     reports=null;
     for(SNP s:allSNPs)
         s=null;
     for(SNP s:tempSNPs)
         s=null;
     allSNPs=null;
     tempSNPs=null;
     model=null;
     gene1.nullGene();
     geneTemp.nullGene();
     gene1=null;
     geneTemp=null;
     b=null;
     for(String s: geneProperties)
         s=null;
     for(String s: rowURL)
         s=null;
     geneProperties=null;
     rowURL=null;
     con=null;    
     SNPs=null;
     PSICScore=null;
     toleratedSubst=null;        
    }
    
    public boolean getActiveBatch(){
        return activeBatch;
    }
    
    public void setFramework(Framework f){
        this.f=f;
        geneTemp= new Gene(f);
        //System.out.print("");
    }
    
    public void setActiveBatch(boolean b){
        activeBatch=b;
    }
    
    public void setSNPs(List<String> SNPs){
        this.SNPs=SNPs;
    }
    
    public void setBatchManager(BatchManager b){
        this.b=b;
    }
    
    public void setTable(JTable t){
        table=t;
        model=(DefaultTableModel)table.getModel();
    }
    
    public void setReports(List<String> r){
        reports=r.toArray(new String[r.size()]);
    }
    
    public void setPSICScores(List<String> p){
        PSICScore=p;
    }
    
    public BatchManager getBatchManager(){
        return b;
    }
    
    public Framework getFramework(){
        return f;
    }
    
    public String[] getGeneProperties(){
        return geneProperties;
    }
    
    public String[] getReport(){
        return reports;
    }
    
    public List<String> getToleratedSubst(){
        return toleratedSubst;
    }
    
    public void setToleratedSubst(List<String> t){
        toleratedSubst=t;
    }
    
    public List<String> getPSICScores(){
        return PSICScore; 
    }
    
    public int getNumHomologs(){
        return numHomologs;
    }
    
    public int getNumParalogs(){
        return numParalogs;
    }
    
    public boolean activeBatch(){
        return activeBatch;
    }
    
    //Checks gene via either genes in cloud or NCBI Gene Database Search
    public String checkGene(String geneTemp) throws Exception{ 
        if(b==null&&!repeatCheck){            
            try{
                connectToSQL();
                gene=geneTemp;
                if(checkSQLForGene()){
                    geneInSQL=true;
                    return geneTemp;
                }else
                    System.out.println("Gene not Found");
            }catch(Exception exe){}
        }
        urlReader1= new URLReader();
        geneSearchURL="http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=gene&term="+geneTemp+"[pref]AND homo sapiens[orgn]";        
        geneSearchURL=geneSearchURL.replace(" ", "+");
        try{
            if(urlReader1.findStringinURL(geneSearchURL, "PhraseNotFound")){
                if(b==null)
                    JOptionPane.showMessageDialog(null, "Could not find HUGO gene symbol for "+geneTemp+".", "Warning", JOptionPane.WARNING_MESSAGE);
                return null;
            }else if(urlReader1.getHTML().contains("Failed to translate")){
                JOptionPane.showMessageDialog(null, "Part of the NCBI servers are currently down, please try again later!", "Warning", JOptionPane.WARNING_MESSAGE);
            }else{
		try{
                    String geneId=urlReader1.getHTML().substring(urlReader1.getHTML().indexOf("<Id>")+4,urlReader1.getHTML().indexOf("</Id"));
                    geneSearchURL="http://www.ncbi.nlm.nih.gov/gene/"+geneId;
                    geneURL=geneSearchURL;
                    gene=geneTemp;
                   
                    return geneSearchURL;
		}catch(Exception exe){
	            if(b==null)
                        JOptionPane.showMessageDialog(null, "Invalid Gene!");
                    return null;
                }
	    }
	}catch(Exception exe){
            if(!repeatCheck){
                repeatCheck=true;
                return checkGene(geneTemp);
            }            
            if(exe instanceof SocketTimeoutException)
                throw exe;
            if(b==null)    
                JOptionPane.showMessageDialog(null, "This application requires a "
                    + "valid internet connection!", "Warning", JOptionPane.WARNING_MESSAGE);
            else{
                b.addError("No internet connection found/internet connection lost!");
                throw new IOException();
            }
            return null;
	}        
       if(b==null)
            JOptionPane.showMessageDialog(null, "Invalid Gene!");
        return null;
    }
    
    public void setGeneURL(String g) throws Exception{
        urlReader1=new URLReader();
        try{    
            urlReader1.findStringinURL(g, "a");
        }catch(Exception exe){
            throw exe;
        }
        geneURL=geneSearchURL=g;
    }
    
    public void setGene(String gene){
        this.gene=gene;
    }
    
    public void setSaveToCloud(boolean save){
        saveDataToCloud=save;
    }
    
    //starting place for new threads
    @Override
    public void run(){
        try{
            start();
        }catch(OutOfMemoryError err){
            JFrameMain.numConcurrentThreads--;
            try{    
                closeSQL();
            }catch(SQLException exe){}
//            JOptionPane.showMessageDialog(null, "Fatal Error: JVM has run out of memory!", "Fatal Error", JOptionPane.ERROR_MESSAGE);
        }catch(Exception exe){
            exe.printStackTrace();
            try{
                System.out.println("Exception in gene " + gene);
            }catch(Exception exe2){}            
            if(!(exe instanceof StringIndexOutOfBoundsException||exe instanceof ArrayIndexOutOfBoundsException))    
                try{
                    clearAll();
                    start();
                    return;
                }catch(Exception exe2){}   
                try{
                    clearAll();
                    JFrameMain.useSQL=true;
                    start();
                    JFrameMain.useSQL=false;
                    if(activeBatch||b!=null){
                        b.addError("Error: An error was encountered while conventionally retrieving data. \nData was retrieved from the cloud.");
                    }else{
                        JOptionPane.showMessageDialog(null, "Error: An error was encountered while conventionally retrieving data. \nData was retrieved from the cloud.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    return;
                }catch(Exception exe2){
                    exe2.printStackTrace();
                }
            try{    
                closeSQL();
            }catch(SQLException exe2){}            
            exe.printStackTrace();                
            StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                exe.printStackTrace(pw);
                String errorMessage=sw.toString();                
                if(activeBatch||b!=null){
                    if(errorMessage.indexOf("eutils")!=-1||errorMessage.indexOf("NCBIFinder")==-1)
                        b.addError("Error: GenBank accession error. ");
                        try{
                            b.addError("for "+getGene().getName());
                        }catch(Exception exe2){}
                    activeBatch=false;
                    JFrameMain.numConcurrentThreads--;                      
                    this.b.threadManager();                    
                    return;
                }
                if(errorMessage.indexOf("eutils")!=-1||errorMessage.indexOf("NCBIFinder")==-1)
                    JOptionPane.showMessageDialog(null, "Error: GenBank accession error. ", "Error", JOptionPane.ERROR_MESSAGE);
                JFrameMain.numConcurrentThreads--;                    
                //else
                    //JOptionPane.showMessageDialog(null, "An unknown error occured! Please try again later.\n"+errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                //Thread t=Thread.currentThread();
         //       Framework.okInternet=true;
    }catch(Error e){
        JFrameMain.numConcurrentThreads--;
        try{    
            closeSQL();
        }catch(SQLException exe){}        
        JOptionPane.showMessageDialog(null, "An unknown error "
                + "occured!", "Error", JOptionPane.ERROR_MESSAGE);        
    }
}
    
    public void setPanel(JPanel p){
        panel=(GenePanel)p;
    }
    
    /*All Framework threads go through this method which collects the data via 
     * either accessing the data through clouds or initiating collection through 
     * websites if data is not availible on the cloud. 
     */
    public void start() throws Exception{
        panel.setFramework(f);
   //    Framework.okInternet=false;
        threadNum=JFrameMain.getThreadCounter();
        gene=gene.toUpperCase();
        table=panel.getTable();
        geneSearchURL="";
        //Scanner reader = new Scanner(System.in);
        boolean b=false;
        if(JFrameMain.useSQL&&sqlTimeCheck){
            if(geneInSQL){
                try{
                    saveDataToCloud=false;                    
                    retrieveDataFromSQL();                
                }catch(Exception exe){
                    exe.printStackTrace();
                    saveDataToCloud=true;
                    SQLError=true;
                }
            }else if(this.b!=null)
                try{
                    connectToSQL();
                    if(checkSQLForGene()){
                        System.out.println("Gene Found");
                        saveDataToCloud=false;
                        this.retrieveDataFromSQL();
                    }else
                        System.out.println("Gene not Found");
                }catch(Exception exe){
                    exe.printStackTrace();
                    saveDataToCloud=true;
                    SQLError=true;
            }   
        }
        if(!JFrameMain.useSQL||saveDataToCloud||!sqlTimeCheck)
            collectData();
        JFrameMain.disableSettings();
        int numRows=table.getRowCount();
        geneProperties[7]=numHomologs+"";
        geneProperties[8]=numParalogs+"";
        if(JFrameMain.useSQL&&saveDataToCloud&&!JFrameMain.askForParalog)
            try{
                this.saveDataToSQL();
            }catch(Exception exe){
                exe.printStackTrace();
                //didn't work code
            }
        try{    
            closeSQL();
        }catch(SQLException exe){}  
        JFrameMain.enableSettings();
        existingSNPs=true;
        for(int j=0;j<numRows;j++){
            String s=(String)model.getValueAt(j, 1);
            enterCustomSNP(s);
        }        
        existingSNPs=false;
        JFrameMain.numConcurrentThreads--;          
        if(activeBatch){
            for(String s: SNPs){
                try{
                    enterCustomSNP(s);
                }catch(Exception exe){}
            }
            this.b.batchPanelManager(f, model);
            activeBatch=false;                      
            this.b.threadManager(); 
        }
        clearFinishedReferences(false);
    }
    
    //calls methods to collect data from various websites
    private void collectData() throws Exception{
        model=(DefaultTableModel)table.getModel();        
        NCBIFinder finder = new NCBIFinder();        
        //String MIM = (urlReader1.extractString(">MIM:", ";")).substring(5);
        //String geneID = (urlReader1.extractString(">Gene ID: ", ",")).substring(10);
        if(SQLError||!JFrameMain.useSQL){
            repeatCheck=true;            
            checkGene(gene);
        }
        finder.waitComplete();
        String nucleoURL="http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=gene&id="
                +geneURL.substring(geneURL.lastIndexOf("/")+1);
        urlReader1.findStringinURL(nucleoURL, "a");
        finder.waitComplete();
        String urlHTML=urlReader1.getHTML();
        String tempID, nucleoID;
        while(true){
            urlHTML=urlHTML.substring(urlHTML.indexOf("products")+1);
            tempID=urlHTML.substring(0,300);
            if(tempID.contains("mRNA")&&tempID.contains("Reference")){
                try{
                    nucleoID=tempID.substring(tempID.indexOf("accession")+11);
                    String nucleoVersion=nucleoID.substring(nucleoID.indexOf("version")+8);
                    nucleoVersion=nucleoVersion.substring(0, nucleoVersion.indexOf(","));
                    nucleoID=nucleoID.substring(0,nucleoID.indexOf(",")-1)+"."+nucleoVersion;
                    break;
                }catch(Exception exe){}
            }
        }
       // String tempID = urlReader1.getHTML().substring(urlReader1.getHTML().indexOf("mRNA and Protein"),urlReader1.getHTML().indexOf("mRNA and Protein")+250);
        //String nucleoID = tempID.substring(tempID.indexOf("core")+5,tempID.indexOf("core")+ tempID.substring(tempID.indexOf("core")).indexOf("\""));
        //String protID = tempID.substring(tempID.indexOf("ein/")+4,tempID.indexOf("ein/") + tempID.substring(tempID.indexOf("ein/")).indexOf("\"")); 
        String protID;
        while(true){
            urlHTML=urlHTML.substring(urlHTML.indexOf("products")+1);
            tempID=urlHTML.substring(0,400);
            if(tempID.contains("peptide")&&tempID.contains("Reference")){
                try{
                    protID=tempID.substring(tempID.indexOf("accession")+11);
                    String protVersion=protID.substring(protID.indexOf("version")+8);
                    protVersion=protVersion.substring(0, protVersion.indexOf(","));
                    protID=protID.substring(0,protID.indexOf(",")-1)+"."+protVersion;
                    break;
                }catch(Exception exe){}
            }
        }
        try{    
            tempDNA = finder.nucleoFinder(nucleoID);
        }catch(Exception exe){
            throw exe;
        }
        try{
            aFasta = finder.protFinder(protID);
        }catch(Exception exe){
            throw exe;          
        }
        String nucleoFasta = ">" + gene +"\n" + tempDNA + "\n";
        String protFasta =">" + gene +"\n" + aFasta + "\n";
        String tempHTML=urlReader1.getHTML();
        String chromosome=tempHTML.substring(tempHTML.indexOf("subtype chromosome,"),tempHTML.indexOf("subtype chromosome,")+200);
        chromosome=chromosome.substring(chromosome.indexOf("name")+6,chromosome.indexOf("}")-7);
        String tempStartEnd=tempHTML.substring(tempHTML.indexOf("seqs {"),tempHTML.indexOf("seqs {")+250);//tempHTML.substring(tempHTML.indexOf(">Chr<")-1000, tempHTML.indexOf(">Chr<")+1000);
        //tempStartEnd=tempStartEnd.substring(tempStartEnd.indexOf("..")-25);
        //String tempStartEnd = urlReader1.extractString(urlReader">Chromosome", "<");
        String start = tempStartEnd.substring(tempStartEnd.indexOf("from ")+5,tempStartEnd.indexOf("to ")-11);//Integer.parseInt(tempStartEnd.substring(tempStartEnd.indexOf("(")+1,tempStartEnd.indexOf("(")+tempStartEnd.substring(tempStartEnd.indexOf("(")).indexOf(".")));
        String end = tempStartEnd.substring(tempStartEnd.indexOf("to ")+3,tempStartEnd.indexOf("strand ")-11);;
        try{
            start=""+(Integer.parseInt(start)+1);
            end=""+(Integer.parseInt(end)+1);            
        }catch(Exception exe){
            
        }
        /* try{
            if(tempStartEnd.indexOf("complement")==-1)
		end = Integer.parseInt(tempStartEnd.substring(tempStartEnd.indexOf("..")+2,tempStartEnd.indexOf(")")));
            else 
                end = Integer.parseInt(tempStartEnd.substring(tempStartEnd.indexOf("..")+2,tempStartEnd.indexOf(",")));
        }catch(Exception exe){}*/
        //String chromosome = tempStartEnd.substring(tempStartEnd.indexOf("Chromosome")+10,tempStartEnd.indexOf("Chromosome")+tempStartEnd.substring(tempStartEnd.indexOf("Chromosome")).indexOf("-"));
     	panel.setLabels(gene,chromosome,""+start,""+end,nucleoID,protID);
        panel.enableGeneSequences();
        geneProperties[0]=gene;
        geneProperties[1]=chromosome; 
        geneProperties[2]= start;
        geneProperties[3]= end;
        geneProperties[4]= nucleoID;
        geneProperties[5]=protID;
        geneProperties[6]=geneURL;        
        gene1=new Gene(gene,nucleoID, protID, nucleoFasta, protFasta);        
        Gene[] homologs = new Gene[67];
        String[] fastas=null;        
        int SNPFoundCounter=0;
        SNP:
        while(true){
    	   // System.out.println("Obtaining SNPs");            
            allSNPs = new SNP[5000]; 
            SNP snp=new SNP(f);
            snp.setAttempts(0);
            int attempts=0;
            while(true){
                attempts++;
                if(attempts>5){
                    tempSNPs=null;
                    saveDataToCloud=false;                    
                    break SNP;
                }                    
                try{    
                    tempSNPs=snp.getAllSNPsNew();
                }catch(Exception exe){
                    exe.printStackTrace();
                    snp=new SNP(f);
                    snp.setAttempts(attempts);
                    continue;
                }
                break;
            }
            int counter=0;
            //tempSNPs=null;//REMOVE LATER
            if(tempSNPs!=null){    
                for(SNP s:tempSNPs){
                    if(s!=null){
                        try{     
                            allSNPs[counter]=s;
                            String SNPID=aFasta.substring(s.getProtLoc()-1,s.getProtLoc())+s.getProtLoc()+s.getProtChange();
                            String SNPPheno=s.getPhenotype();
                            boolean verified=s.getMultipleConfirmed();
                            String SNPVerified="";
                            if(verified)
                                SNPVerified="Multiple Publications";
                            else
                                SNPVerified="Single Publication";
                            model.addRow(new Object[]{false,SNPID,SNPPheno,SNPVerified,"Please Wait"});
                            int x=model.getRowCount();
                            rowURL[model.getRowCount()-1]=allSNPs[counter].getInfoUrl();
                            counter++;
                        }catch(StringIndexOutOfBoundsException exe){}
                    }
                }
                numClinicalSNPs=model.getRowCount();
                break SNP;
            }else{
                if(SNPFoundCounter==3){
                    if(!activeBatch)
                        JOptionPane.showMessageDialog(null, "There are no "
                                + "previously idenified clinically related SNPs "
                                + "for "+getGene().getName()+"! However, feel free"
                                + " to enter your own SNPs.", "Warning", 
                                JOptionPane.WARNING_MESSAGE);
                    noExistingSNPs=true;
                    break SNP;
                }
                SNPFoundCounter++;
            }
        }
        if(model==null)
            model=(DefaultTableModel)table.getModel();            
        panel.infoReady();   
        if(JFrameMain.useParalogs)
            try{
                fastas=geneTemp.getFamily();
                numParalogs=geneTemp.getNumParalogs();
            }catch(Exception exe){
                fastas=geneTemp.getFamily();
            }
        else
            saveDataToCloud=false;
        try{    
            nFasta2=fastas[0];
            pFasta2=fastas[1];
        }catch(NullPointerException exe){}
        panel.enableParalogs();
        int homologLoc=-1;
        try{
	//		geneSearchURL = "http://www.ncbi.nlm.nih.gov" + urlReader1.extractHyperLink(150, "Homologs of the", "/homologene");
	//		//System.out.println(geneSearchURL);	
            urlReader1.findStringinURL(nucleoURL,"");
            homologLoc=urlHTML.indexOf("db \"HomoloGene\",");
            if(homologLoc==-1)
                throw new Exception();
        }catch(Exception exe){
    		//System.out.print("No Homologs");
    		homologs=null;
    	}
        String homologID="-1";
    	if(homologs!=null){
                homologID=urlHTML.substring(homologLoc,homologLoc+100);
                homologID=homologID.substring(homologID.indexOf("id")+3,homologID.indexOf("}")-1).trim();               
                try{
                    finder.waitComplete();
                    urlReader1.findStringinURL(nucleoURL,"");
    		}catch(Exception exe){
    		//	System.out.print("");
    		}
    		HomologFinder h = new HomologFinder(urlReader1.getHTML(), nucleoURL, f);
                try{
                    fastas = h.findHomologsEut(homologID);
                    //fastas = h.findHomologs();
                }catch(Exception exe){
                    fastas=h.findHomologs();
                }
                numHomologs=h.getNumHomolgs();
    		if(fastas==null)
                    return;
                nFasta=fastas[0];
    		pFasta=fastas[1];
    	}
        panel.stopWait();
        panel.enableMutationChecking();
        panel.stopWait2();
        panel.enableReport();
        panel.enableHomologs();        
    }
    
    //nulls instance variables containing unnecessary references
    private void clearFinishedReferences(boolean exception){
        if(!exception){
            urlReader1=null;
            gene=null;
            panel=null;           
        }
    }
    
    //Takes a mutation (SNP) as input and adds it to a table of SNPs
    public synchronized void enterCustomSNP(String cb){
        if(numParalogs==1)
            numParalogs=0;
        boolean subConservation=false;
    	if(cb==null)
            return;
        String cbSave=cb;
        boolean snpFound=false;
        int tempPercentage;
    	int snpNucleoLoc=-1;
    	int snpProtLoc=-1;
        double percentage=0;
        double percentageOriginal=0;
        int validCounterOriginal=0;
        int validCounter=0;
        String tempReport;
    	boolean notFound1 = false;
    	boolean notFound2=false;
    	boolean tooShort = false;
    	//tempDNA=tempDNA.substring(0, tempDNA.length()-45);	
    	String temp=tempDNA;
    	String nucleoSeq;
    	String codon="";
    	String protSeq;
        String cbOrigin;
        cbOrigin = cb.trim().toLowerCase();
        cb=cbOrigin;
        if(cb.substring(0,2).equals("rs"))
            cb=rsConvert(cb);
        if(cb==null)
            return;
        String nucleoChange="";
        try{
          cb=cb.replace(" ", "");
          int loc=Integer.parseInt(cb.substring(0,cb.indexOf(">")));
          nucleoChange=cb.substring(cb.length()-1);
          cb=tempDNA.substring(loc-1,loc+45).toLowerCase();
          cbOrigin=cb;
        }catch(Exception exe){}
    	if(cb.length()>2)
            codon = cb.substring(0,3); 	
        if((cb.indexOf("a")!=-1||cb.indexOf("c")!=-1||cb.indexOf("g")!=-1||cb.indexOf("t")!=-1)&&!(cb.substring(1,2).equals("1")||cb.substring(1,2).equals("2")||cb.substring(1,2).equals("3")||cb.substring(1,2).equals("4")||cb.substring(1,2).equals("5")||cb.substring(1,2).equals("6")||cb.substring(1,2).equals("7")||cb.substring(1,2).equals("8")||cb.substring(1,2).equals("9")||cb.substring(1,2).equals("0"))){
            cb=cb.toUpperCase();
            if(cb.indexOf(">")!=-1){
                cb=cb.replace(" ", "");
                nucleoChange=cb.substring(cb.length()-1);
                cb=cb.substring(0,cb.indexOf(">"));
            }
            if(!checkInput(cb))
                return;
            int index = tempDNA.indexOf(cb);
            nucleoSeq=findInProtein(index);
            if(nucleoSeq==null){
                if(!activeBatch)
                    JOptionPane.showMessageDialog(null,"The nucleotide at the "
                            + "location entered is on an intron! "
                            + "Please enter a different SNP.", 
                            "Warning", JOptionPane.WARNING_MESSAGE);
                else
                    b.addError("SNP:"+cbSave+" The nucleotide"
                        + " at the location of this SNP is "
                        + "on an intron! Please enter a "
                        + "different SNP.");
                    return;
            }
            String nucleoSeqOrigin=tempDNA.substring(index,index+40);
            index=tempDNA.indexOf(cb);
            snpProtLoc=aFasta.indexOf(geneTemp.convertToAmino(nucleoSeq))+1;
            //ADD BACKWARDS
            protSeq=aFasta.substring(snpProtLoc-1,snpProtLoc+14);
            String codon1=nucleoSeq.substring(0,3).toUpperCase();
            String codon2="";
            String protChange;
            snpNucleoLoc=index+1;
            String nAtLoc=tempDNA.substring(snpNucleoLoc-1,snpNucleoLoc).toUpperCase();
             int optionOriginal=nucleoSeq.indexOf(nucleoSeqOrigin);            
             if(optionOriginal==0)
                 codon2=nucleoChange+codon1.substring(1,2)+codon1.substring(2,3).toUpperCase();
             else if(optionOriginal==1)
                 codon2=codon1.substring(0,1)+nucleoChange+codon1.substring(2,3);
             else if(optionOriginal==2)
                 codon2=codon1.substring(0,1)+codon1.substring(1,2)+nucleoChange;
             codon2=codon2.toUpperCase();
             protChange=geneTemp.convertToAmino(codon2);
             SNP input = new SNP(null);
             protChange=protChange.replace("Stop", "*");
             input.setProtChange(protChange);
             input.setProtLoc(snpProtLoc);
             input.setNucleoChangeCodon(codon2);
             if(codon2.equals(codon1)){
                if(b==null)
                    JOptionPane.showMessageDialog(null,"No nucleotide change in entered SNP!",
                            "Warning", JOptionPane.WARNING_MESSAGE); 
                else
                    b.addError("No nucleotide change for SNP "+
                    aFasta.substring(input.getProtLoc()-1,input.getProtLoc())
                       +input.getProtLoc()+input.getProtChange());
                return;
             }             
             tempReport=geneTemp.findRelatedSNP(nFasta,snpNucleoLoc);                                                        
             tempPercentage=Integer.parseInt(tempReport.substring(tempReport.indexOf("(")+1,
                     tempReport.indexOf(")")-1));
             percentage+=tempPercentage;
             validCounter++;     
             percentageOriginal+=Integer.parseInt(tempReport.substring(tempReport.indexOf("(")+1,tempReport.indexOf(")")-1));
             validCounterOriginal++;                                                        
             reports[reportCounter]+="\n   DNA "+"("+snpNucleoLoc+","+codon1+"-->"+input.getNucleoChangeCodon()+")\n   Species:" +tempReport;
             if(!(pFasta2.equals(""))){
                tempReport=geneTemp.findRelatedSNP(nFasta2,snpNucleoLoc);
                tempPercentage=Integer.parseInt(tempReport.substring(tempReport.indexOf("(")+1,tempReport.indexOf(")")-1));
                percentage+=tempPercentage;
                validCounter++;
                percentageOriginal+=Integer.parseInt(tempReport.substring(tempReport.indexOf("(")+1,tempReport.indexOf(")")-1));
                validCounterOriginal++;                                                            
                reports[reportCounter]+="\n   Paralogous: "+tempReport;                                                                                     
             }else
                reports[reportCounter]+="\n   No reports available for paralogous DNA in "+getGene().getName();             
             percentage=0;
             tempReport=geneTemp.findRelatedSNP(pFasta,snpProtLoc);
             tempPercentage=Integer.parseInt(tempReport.substring(tempReport.indexOf("(")+1,tempReport.indexOf(")")-1));
             percentage+=tempPercentage*(numHomologs/(numHomologs+numParalogs+0.0));
             //percentage+=tempPercentage;
             percentageOriginal+=tempPercentage;
             validCounterOriginal++;                                                    
             validCounter++;

                              tempReport=tempReport.replaceAll("nucleotides","amino acids");
                              reports[reportCounter]+="\n   Protein "+"("+snpProtLoc+","+aFasta.substring(snpProtLoc-1,snpProtLoc)+"-->"+input.getProtChange()+")\n   Species: "+tempReport;
                            if(!(pFasta2.equals(""))){   
                                tempReport=geneTemp.findRelatedSNP(pFasta2,snpProtLoc);
                                tempPercentage=Integer.parseInt(tempReport.substring(tempReport.indexOf("(")+1,tempReport.indexOf(")")-1));
                                //percentage+=tempPercentage;
                                percentage+=tempPercentage*(numParalogs/(numHomologs+numParalogs+0.0));
                                validCounter++;
                                percentageOriginal+=tempPercentage;
                                validCounterOriginal++;                                                    
                                reports[reportCounter]+="\n   Paralogous: "+tempReport;                                                    
                            }else
                                 reports[reportCounter]+="\n   No reports available for paralogous amino acids in "+getGene().getName();
                            reports[reportCounter]+="\n";
                            if(!existingSNPs)
                                if(checkSNPs(input)){    
                                    reportCounter++;
                                }else
                                    return;
                            else
                                reportCounter++;
                            if(JFrameMain.usePSIC)
                            try{    
                    double PSICScore1=geneTemp.PSIC(pFasta2,pFasta, 
                            snpProtLoc,input.getProtChange());
                    PSICScore.add(Math.abs(PSICScore1)+"");
                            }catch(Throwable t){
                                System.out.println("PSIC Exception in gene " + gene);
                                t.printStackTrace();
                                if(t instanceof StackOverflowError||t instanceof Exception){
                                    PSICScore.add("N/A");
                                }else 
                                    throw t;
                            }
                            else
                                PSICScore.add("N/A");
                            String e=aFasta.substring(snpProtLoc-1,snpProtLoc);
                            try{
                                toleratedSubst.add(toleratedSubst(aFasta.substring(snpProtLoc-1,snpProtLoc),input.getProtChange()));
                            }catch(Exception exe){
                                toleratedSubst.add("Normally Tolerated (5)");
                            }
                            percentageOriginal=percentageOriginal/validCounterOriginal;
                            if(!existingSNPs){
                                    String SNPName=aFasta.substring(input.getProtLoc()-1,input.getProtLoc())+input.getProtLoc()+input.getProtChange();
                                    String SNPPheno=getPossiblePhenotype(input);
                                    String SNPVerified="Not verified";
                                    double percentageOrigin=percentage;
                                    if(JFrameMain.useHotspots){    
                                        if(!(SNPPheno.equals("Unknown"))&&!input.getMultipleConfirmed()){
                                            SNPVerified="Nearby SNPs published";
                                        }else if(input.getMultipleConfirmed())
                                            SNPVerified="At least one publication";
                                    }else{
                                        SNPPheno="Enable Hotspots";
                                        SNPVerified="Enable Hotspots";
                                    }
                                    model.addRow(new Object[]{true,SNPName,SNPPheno,SNPVerified,"Please Wait"});
                                    table.getSelectionModel().setSelectionInterval(model.getRowCount(), model.getRowCount());
                                    table.scrollRectToVisible(new Rectangle(table.getCellRect(model.getRowCount(), 0, true)));
                                    for(int j=0; j<5000; j++){
                                        if(allSNPs[j]!=null)
                                            allSNPs[j]=input;
                                    }
                            }                                                 
                            int numRows=table.getRowCount();
                            int totalSNPRow[]=new int[25];
                            int numRepeats=0;
                            for(int j=0; j<numRows; j++){
                                String s=(String)model.getValueAt(j, 1);
                                String p=aFasta.substring(input.getProtLoc()-1,input.getProtLoc())+input.getProtLoc()+input.getProtChange();
                                if(s.equals(p)){
                                    totalSNPRow[numRepeats]=j;
                                    numRepeats++;
                                }
                            }
int tolSubstNum=Integer.parseInt(toleratedSubst.get(reportCounter-1).substring(
                                    toleratedSubst.get(reportCounter-1).indexOf("(")+1,
                                    toleratedSubst.get(reportCounter-1).length()-1));
                            double PSIC;
                            try{
                                PSIC=Double.parseDouble(this.PSICScore.get(reportCounter-1));
                            }catch(Exception exe){
                                PSIC=-1.0;
                            }
                             String accuracy=predictPathogenic
                                    (percentage,PSIC,
                                    tolSubstNum);
                            if(input.getProtChange().equals("*"))
                                accuracy="Pathogenic";
                            for(int j=0; j<numRepeats; j++){
                                if(((String)(model.getValueAt(totalSNPRow[j],4))).indexOf("Please")!=-1)
                                    model.setValueAt(accuracy, totalSNPRow[j],4);
                            }                           
          } else{
                try{
                    reports[reportCounter]="";
                    int i = Integer.parseInt(cb.substring(1,cb.length()-1))-1;
                    List<String> possibleCodons = new ArrayList<>();
                    if(i<=aFasta.length()){
                        String snpAA=aFasta.substring(i,i+1).toUpperCase();
                        if(snpAA.equals(cb.substring(0,1).toUpperCase())){
                            snpProtLoc=i+1;
                            SNP input = new SNP("Unknown",cb.substring(cb.length()-1).toUpperCase(),
                                    "",snpProtLoc,0,false);
                            boolean alreadyThere=false;
                            tempSNPs:
                            if(allSNPs!=null){
                                for(SNP s:allSNPs){
                                    if(s!=null){
                                        if(input.getProtChange().equals(s.getProtChange())&&input.getProtLoc()==s.getProtLoc()){
                                            input=s;
                                            break tempSNPs;
                                        } 
                                    }
                                }
                            }          
                            if(input.getProtChange().equals(aFasta.substring(input.getProtLoc()-1,input.getProtLoc()))&&!existingSNPs){
                                if(b==null)
                                    JOptionPane.showMessageDialog(null,"No amino acid change in entered SNP!", "Warning", JOptionPane.WARNING_MESSAGE);
                                else
                                    b.addError("No amino change for SNP "+
                                            aFasta.substring(input.getProtLoc()-1,input.getProtLoc())
                                            +input.getProtLoc()+input.getProtChange());
                                return;
                            }
                            String codon1="";
                            boolean backwards=false;
                            String amino1="";
                            try{    
                                amino1=aFasta.substring(input.getProtLoc()-1,input.getProtLoc()+10);
                            }catch(StringIndexOutOfBoundsException exe){
                                amino1=aFasta.substring(input.getProtLoc()-11,input.getProtLoc());
                                backwards=true;
                            }
                            String[][] codon1Chain=new String[10][0];
                            int[] rowSize= new int[10]; 
                            int codonPermutations=1;
                            for(int k=0; k<10; k++){
                                String[] codonRow=geneTemp.convertToDNACodon(amino1.substring(k,k+1));
                                codon1Chain[k]=codonRow;
                                rowSize[k]=codon1Chain[k].length;
                                codonPermutations*=rowSize[k];
                            }
                            int[] rowSizeTemp=rowSize.clone();
                            int codonLoc=getCodonLoc(codon1Chain,tempDNA);
                            boolean oneDifference=false;
                            if(backwards)
                                codonLoc+=9;
                            input.setNucleoLoc(codonLoc+1);                                                                    
                            snpNucleoLoc=codonLoc+1;
                            codon1=tempDNA.substring(codonLoc,codonLoc+3).toUpperCase();
                            String amino2=input.getProtChange();                                                       
                            String[] codon2=geneTemp.convertToDNACodon(amino2);
                            int differences=0;
                            int counter1=0;
                            int counter2=0;
                            //Multiple With 1 Difference
                            int differenceIndex = -1;
                            int tempDifferenceIndex=-1;                                            
                            outer:    
                            for(String c2:codon2){
                                    counter1++;
                                    int loc=3;
                                    for(int k=0;k<3;k++){
                                        if(!(codon1.substring(k,k+1).equals(c2.substring(k,k+1)))){
                                            loc=k;
                                            differences++;
                                            tempDifferenceIndex=k;

                                        }
                                    }
                                    if(differences==1){
                                        differenceIndex=tempDifferenceIndex;
                                        possibleCodons.add(c2);
                                        counter2++;
                                        differences=0;
                                        counter1=0;
                                        input.setNucleoChangeCodon(possibleCodons.get(0));//Change location?
                                    }else{
                                        differences=0;
                                    }
                                    if(possibleCodons.size()==1){
                                      snpNucleoLoc+=differenceIndex;                                                        
                                        break;
                                     }
                            }
                            if(possibleCodons.size()==1||existingSNPs){
                                tempReport=geneTemp.findRelatedSNP(nFasta,snpNucleoLoc);                                                        
                                tempPercentage=Integer.parseInt(tempReport.substring(tempReport.indexOf("(")+1,tempReport.indexOf(")")-1));
                                percentage+=tempPercentage;
                                validCounter++;     
                                percentageOriginal+=Integer.parseInt(tempReport.substring(tempReport.indexOf("(")+1,tempReport.indexOf(")")-1));
                                validCounterOriginal++;                                                        
                               if(!subConservation&&tempPercentage<50)
                                    subConservation=subConservation(tempReport);
                                reports[reportCounter]+="\n   DNA "+"("+snpNucleoLoc+","+codon1+"-->"
                                        +input.getNucleoChangeCodon()+")\n   Species:" 
                                        +tempReport;
                               if(!(pFasta2.equals(""))){
                                   tempReport=geneTemp.findRelatedSNP(nFasta2,snpNucleoLoc);
                                   tempPercentage=Integer.parseInt(tempReport.substring(tempReport.indexOf("(")+1,
                                           tempReport.indexOf(")")-1));
                                    percentage+=tempPercentage;
                                    validCounter++;
                                    percentageOriginal+=Integer.parseInt(tempReport.substring(tempReport.indexOf("(")+1,
                                            tempReport.indexOf(")")-1));
                                    validCounterOriginal++;                                                            
                                   reports[reportCounter]+="\n   Paralogous: "+tempReport;                                                                                     
                               }else
                                    reports[reportCounter]+="\n   No reports available for paralogous DNA in "+getGene().getName();
                              }else if(possibleCodons.size()>1){
                                   //JOptionPane.showMessageDialog(null, cb+" More than one possibleCodon", "Warning", JOptionPane.WARNING_MESSAGE);
                              }else if(possibleCodons.isEmpty()){
                                    //JOptionPane.showMessageDialog(null, "The mutation isn't possible!", "Warning", JOptionPane.WARNING_MESSAGE);
                              }
                              percentage=0;
                              tempReport=geneTemp.findRelatedSNP(pFasta,snpProtLoc);
                              tempPercentage=Integer.parseInt(tempReport.substring(tempReport.indexOf("(")+1,tempReport.indexOf(")")-1));
                              percentage+=tempPercentage*(numHomologs/(numHomologs+numParalogs+0.0));
                              //percentage+=tempPercentage;
                              percentageOriginal+=tempPercentage;
                              validCounterOriginal++;                                                    
                              validCounter++;
                              if(!subConservation&&tempPercentage<50)
                                subConservation=subConservation(tempReport);
                              tempReport=tempReport.replaceAll("nucleotides","amino acids");
                              reports[reportCounter]+="\n   Protein "+"("+snpProtLoc+","+aFasta.substring(snpProtLoc-1,snpProtLoc)+"-->"+input.getProtChange()+")\n   Species: "+tempReport;
                            if(!(pFasta2.equals(""))){   
                                tempReport=geneTemp.findRelatedSNP(pFasta2,snpProtLoc);
                                tempPercentage=Integer.parseInt(tempReport.substring(tempReport.indexOf("(")+1,tempReport.indexOf(")")-1));
                                //percentage+=tempPercentage;
                                percentage+=tempPercentage*(numParalogs/(numHomologs+numParalogs+0.0));
                                validCounter++;
                                percentageOriginal+=tempPercentage;
                                validCounterOriginal++;                                                    
                                if(!subConservation&&tempPercentage<50)
                                    subConservation=subConservation(tempReport);        
                                reports[reportCounter]+="\n   Paralogous: "+tempReport;                                                    
                            }else
                                 reports[reportCounter]+="\n   No reports available for paralogous amino acids in "+getGene().getName();
                            reports[reportCounter]+="\n";
                            if(!existingSNPs)
                                if(checkSNPs(input)){    
                                    reportCounter++;
                                }else
                                    return;
                            else
                                reportCounter++;
                            if(JFrameMain.usePSIC)
                            try{    
                                double PSICScore1=geneTemp.PSIC(pFasta2,pFasta, snpProtLoc,input.getProtChange());
                                PSICScore.add(Math.abs(PSICScore1/*-PSICScore2*/)+"");
                            }catch(Throwable t){
                                System.out.println("PSIC Exception in gene " + gene);
                                t.printStackTrace();
                                if(t instanceof StackOverflowError||t instanceof Exception){
                                    PSICScore.add("N/A");
                                }else 
                                    throw t;
                            }
                            else
                                PSICScore.add("N/A");
                            String e=aFasta.substring(snpProtLoc-1,snpProtLoc);
                            try{
                                toleratedSubst.add(toleratedSubst(aFasta.substring(snpProtLoc-1,snpProtLoc),input.getProtChange()));
                            }catch(Exception exe){
                                toleratedSubst.add("Normally Tolerated (5)");
                            }
                            percentageOriginal=percentageOriginal/validCounterOriginal;
                            if(!existingSNPs){
                                String SNPName=cb.substring(0,1).toUpperCase()
                                        +input.getProtLoc()+input.getProtChange();
                                    String SNPPheno=getPossiblePhenotype(input);
                                    String SNPVerified="Not verified";
                                    double percentageOrigin=percentage;
                                    if(JFrameMain.useHotspots){    
                                        if(!(SNPPheno.equals("Unknown"))&&!input.getMultipleConfirmed()){
                                            SNPVerified="Nearby SNPs published";
                                        }else if(input.getMultipleConfirmed())
                                            SNPVerified="At least one publication";
                                    }else{
                                        SNPPheno="Enable Hotspots";
                                        SNPVerified="Enable Hotspots";
                                    }
                                model.addRow(new Object[]{true,SNPName,SNPPheno,
                                    SNPVerified,"Please Wait"});
                                table.getSelectionModel()
                                        .setSelectionInterval(model.getRowCount(), 
                                        model.getRowCount());
                                    table.scrollRectToVisible(new Rectangle(table.getCellRect(model.getRowCount(), 0, true)));
                                    for(int j=0; j<5000; j++){
                                        if(allSNPs[j]!=null)
                                            allSNPs[j]=input;
                                    }
                            }                                                 
                            int numRows=table.getRowCount();
                            int totalSNPRow[]=new int[25];
                            int numRepeats=0;
                            for(int j=0; j<numRows; j++){
                                String s=(String)model.getValueAt(j, 1);
                                String p=aFasta.substring(input.getProtLoc()-1,input.getProtLoc())+input.getProtLoc()+input.getProtChange();
                                if(s.equals(p)){
                                    totalSNPRow[numRepeats]=j;
                                    numRepeats++;
                                }
                            }
                            int tolSubstNum=Integer.parseInt(toleratedSubst.get(reportCounter-1).substring(
                                    toleratedSubst.get(reportCounter-1).indexOf("(")+1,
                                    toleratedSubst.get(reportCounter-1).length()-1));
                            double PSIC;
                            try{
                                PSIC=Double.parseDouble(this.PSICScore.get(reportCounter-1));
                            }catch(Exception exe){
                                PSIC=-1.0;
                            }
                            String accuracy=predictPathogenic
                                    (percentage,PSIC,
                                    tolSubstNum);
                            if(input.getProtChange().equals("*"))
                                accuracy="Pathogenic";
                            for(int j=0; j<numRepeats; j++){
                                if(((String)(model.getValueAt(totalSNPRow[j],4))).indexOf("Please")!=-1)
                                    model.setValueAt(accuracy, totalSNPRow[j],4);
                            }
                         }else{
                            //System.out.print("Amino Acid "+snpAA+" isn't at "+i+" in "+getGene().getName()+"!");
                            throw new StringIndexOutOfBoundsException("");
                        }
                    }else{
                        throw new StringIndexOutOfBoundsException("");
                    }
            } catch(Exception exe){
                exe.printStackTrace();
                if(cb!=null&&!cb.equals("")&&!existingSNPs)
                        if(!activeBatch)    
                            JOptionPane.showMessageDialog(null, cb+" is an invalid SNP. "
                                    + " Please check to insure"
                                    + " that the location \nof the"
                                    + " SNP matches the location"
                                    + " on the reference sequence.", 
                                    "Warning", JOptionPane.WARNING_MESSAGE);
                        else
                            b.addError(cb+" is an invalid SNP for "
                                    +getGene().getName()
                                    + ". Please check to insure"
                                    + " \nthat the location of the"
                                    + " SNP matches the location"
                                    + " on the reference sequence.");
                        cb=cb.toLowerCase();
                        cbOrigin=cb;
            }
        }    	
    }
    
    /*Gets the location of a nucleotide in a fasta format nucleotide sequence 
     * given both the sequence and a jagged array of possible nucleotides for
     * 10 consecutive amino acidss. Uses brute force until a suitable combination
     * for the nucleotides is found and then returns this number. 
     */
    private int getCodonLoc(String[][] jagged, String fasta) {
        fasta=fasta.toUpperCase();
        List<Integer> locs=new ArrayList<>();
        total:
        for(int total=4; total<=jagged.length;total++){
            locs=new ArrayList<>();
            int count = 1;
            int[] divisors = new int[total];
            for (int j = 0; j < total; j++) {
                divisors[j] = count;
             count *= jagged[j].length;
            }
            for (int i = 0; i < count; i++) {
                String[] combination = new String[total];
                for (int j = 0; j < total; j++) {
                    int mod = jagged[j].length;
                    combination[j] = jagged[j][(i/divisors[j])%mod];
                }
                String codonSeq="";
                for(String s:combination)
                   codonSeq+=s;
                int loc=fasta.indexOf(codonSeq);
                if(loc!=-1){        
                   boolean repeat = false;
                    for(int l: locs){
                        if(l==loc){
                            repeat=true;
                            break;
                        }                    
                    }
                    if(locs.isEmpty())
                        locs.add(loc);
                    else if(!repeat)
                        continue total;
                }
            }
            if(locs.size()==1)
                break;
        }
        if(locs.size()==1)
            return locs.get(0);
        return 0;
    }
    
    //Generates text report tab
    public void generateReport(){
        report="";
        int numRows=model.getRowCount();
        for(int i=0; i<numRows;i++){
            if((Boolean)model.getValueAt(i,0)==true){
                report+=reports[i];
            }
        }
        if(!(report.equals(""))){    
            report=report.replaceAll("_"," ");
            report.replaceAll("|"," ");  
            JPanel reportTab; 
            if(!activeBatch){
                reportTab= new ReportTab(getGene().getName()+" Report",report);
                JFrameMain.addTab(getGene().getName()+" Report", reportTab,false);
            }else{
                reportTab= new ReportTab("Batch Report",report);
                JFrameMain.addTab("Batch Report", reportTab,false);
            }
        }else{
            JOptionPane.showMessageDialog(null, "Please select at least one valid SNP!", "Warning", JOptionPane.WARNING_MESSAGE);
        }
   }
    
    //Generates table report tab
    public void generateConservationReport(){
        ConservationPanel c;
        if(!activeBatch)
            c= new ConservationPanel(getGene().getName());
        else
            c=new ConservationPanel("Batch");               
        DefaultTableModel cModel=(DefaultTableModel)c.getTable().getModel();
        int numRows=model.getRowCount();
        boolean validSNP=false;
        Object[] rowVals = new String[8];
        String temp="";
        int paraProtCons=0, homoProtCons=0;
        for(int i=0; i<numRows;i++){
            if(reports[i]==null||reports[i].equals(""))
                continue;
            rowVals[0]=model.getValueAt(i,1);
            if((Boolean)model.getValueAt(i,0)==true){
                if(reports[i]!=null)
                    validSNP=true;
                else
                    continue;
                boolean noPara=false;
                if(reports[i].indexOf("No rep")!=-1){
                    rowVals[1]="N/A";
                    rowVals[2]="N/A";
                    noPara=true;
                }
                if(reports[i].indexOf("DNA")==-1){                   
                    rowVals[2]="N/A";
                    rowVals[4]="N/A";
                } else if(!noPara){
                    rowVals[0]+=(" " + reports[i].substring(reports[i].indexOf("("),
                            reports[i].indexOf(")")+1));                     
                    temp=reports[i].substring(reports[i].indexOf("Para"));
                    rowVals[2]=temp.substring(temp.indexOf("(")+1,temp.indexOf(")")-1);
                    temp=reports[i].substring(reports[i].indexOf("Species"));
                    rowVals[4]=temp.substring(temp.indexOf("(")+1,temp.indexOf(")")-1);
                }else if(noPara){
                    rowVals[0]+=(" " + reports[i].substring(reports[i].indexOf("("),
                            reports[i].indexOf(")")+1));                     
                    rowVals[2]="N/A";
                    temp=reports[i].substring(reports[i].indexOf("Species"));
                    rowVals[4]=temp.substring(temp.indexOf("(")+1,temp.indexOf(")")-1);                    
                }
                temp=reports[i].substring(reports[i].indexOf("Prot"));
                String tempTemp=temp.substring(temp.indexOf("Sp"));
                rowVals[3]=tempTemp.substring(tempTemp.indexOf("(")+1,
                        tempTemp.indexOf(")")-1);
                try{
                    homoProtCons=Integer.parseInt((String)rowVals[3]);
                }catch(NumberFormatException exe){}
                if(!noPara){
                    temp=temp.substring(temp.indexOf("Para"));                    
                    rowVals[1]=temp.substring(temp.indexOf("(")+1,
                            temp.indexOf(")")-1);
                    try{
                        paraProtCons=Integer.parseInt((String)rowVals[1]);
                    }catch(NumberFormatException exe){}
                }
                if(b!=null){
                    numHomologs=b.getNumHomologs(i);
                    numParalogs=b.getNumParalogs(i);
                }
                if(numParalogs==1)
                    numParalogs=0;
                for(int j=1;j<3; j++){    
                    if(!(rowVals[j].equals("N/A"))){
                        int numSame=numFromPercentage(numParalogs,
                                Integer.parseInt((String)rowVals[j]));
                        if(numSame!=-1)
                            rowVals[j]+=(" ("+numSame+"/"+numParalogs+")");
                    }
                }
                for(int j=3;j<5; j++){    
                    if(!(rowVals[j].equals("N/A"))){
                        int numSame=numFromPercentage(numHomologs,
                                Integer.parseInt((String)rowVals[j]));
                        if(numSame!=-1)
                            rowVals[j]+=(" ("+numSame+"/"+numHomologs+")")+"";
                    }
                }
                double totalSeqs=numHomologs+numParalogs+0.0;
                double weightedProtScore=homoProtCons*numHomologs/totalSeqs+
                        paraProtCons*numParalogs/totalSeqs;
                if(weightedProtScore==0)
                    rowVals[5]="N/A";
                else
                    rowVals[5]=weightedProtScore+"";                
                rowVals[6]=PSICScore.get(i);
                rowVals[7]=toleratedSubst.get(i);
                cModel.addRow(rowVals);                
            }
        }
        if(validSNP==false){
            JOptionPane.showMessageDialog(null, "Please select at least one valid "
                    + "SNP!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if(!activeBatch)
            JFrameMain.addTab(getGene().getName()+ " Cons. Report",c,false);
        else
            JFrameMain.addTab("Batch Cons. Report",c,false);
    }
    
    //Returns a reference to the gene object of this framework
    public Gene getGene(){
    	return gene1;
    }
    
    //Opens the browser to information on a selected SNP if availble
    public void openSNPInfo(){
        if(table.getSelectedRows().length>1){
            JOptionPane.showMessageDialog(null, "Please select only one valid SNP", "Warning", JOptionPane.WARNING_MESSAGE);
            return;            
        }
        if(table.getSelectedRow()==-1){
            JOptionPane.showMessageDialog(null, "Please select a row to get additional SNP information.", "Warning", JOptionPane.WARNING_MESSAGE);            
        }
        if(rowURL[table.getSelectedRow()]==null){
            JOptionPane.showMessageDialog(null, "No additional information available for this SNP.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try{
            java.awt.Desktop.getDesktop().browse(new URI(rowURL[table.getSelectedRow()]));
        }catch(URISyntaxException | IOException exe){
            JOptionPane.showMessageDialog(null, "No additional information available for this SNP.", "Warning", JOptionPane.WARNING_MESSAGE);            
        }
    }
    
    //Opens the browser to information on a selected Gene if availible
    public void openGeneInfo(){
        try{
            java.awt.Desktop.getDesktop().browse(new URI(geneURL));
        }catch(URISyntaxException | IOException exe){}
    }
    
    /*Calls method to do a UCSC BLAT search of an area adjacent to a SNP and 
     *opens the results of this search on the UCSC genome browser
     */
    public void openUCSCBrowser(){
        if(table.getSelectedRowCount()>1){
            JOptionPane.showMessageDialog(null, "Please select only one valid SNP", "Warning", JOptionPane.WARNING_MESSAGE);
            return;            
        }
        if(table.getSelectedRow()==-1){
            JOptionPane.showMessageDialog(null, "Please select a row first.", "Warning", JOptionPane.WARNING_MESSAGE);            
        }
        String SNPName=(String)model.getValueAt(table.getSelectedRow(), 1);
        int loc=Integer.parseInt(SNPName.substring(1,SNPName.length()-1));
        String aaChain;
        try{
            aaChain=aFasta.substring(loc-1,loc+75);
        }catch(Exception exe){
            aaChain=aFasta.substring(loc-75,loc);
        }
        String UCSCURL=geneTemp.getBrowserURL(aaChain);
        try{
            java.awt.Desktop.getDesktop().browse(new URI(UCSCURL));
        }catch(URISyntaxException | IOException exe){
            JOptionPane.showMessageDialog(null, "Could not find SNP on UCSC Genome Browser.", "Warning", JOptionPane.WARNING_MESSAGE);            
        }        
    }

    //Opens a tab for different types of sequences
    public void addSequenceTab(String type){
            switch (type) {
                case "nucleotide":
                    if(tempDNA!=null&&!(tempDNA.equals(""))){   
                        JPanel reportTab = new ReportTab(getGene().getName()+" Nucleotide Sequence",addRegularNumbers(multiLine(tempDNA)));
                        JFrameMain.addTab(getGene().getName()+" Nucleotide", reportTab,false);           
                    }else
                        JOptionPane.showMessageDialog(null, "Unable to open nucleotide sequence!","Warning", JOptionPane.WARNING_MESSAGE);
                    break;
                case "protein":
                    if(aFasta!=null&&!(aFasta.equals(""))){   
                        JPanel reportTab = new ReportTab(getGene().getName()+" Protein Sequence",addRegularNumbers(multiLine(aFasta)));
                        JFrameMain.addTab(getGene().getName()+" Protein", reportTab,false);           
                    }else
                        JOptionPane.showMessageDialog(null, "Unable to open protein sequenc!e","Warning", JOptionPane.WARNING_MESSAGE);
                    break;
                case "paranucleo":
                    if(nFasta2!=null&&!(nFasta2.equals(""))){   
                        JPanel reportTab = new ReportTab(getGene().getName()+" Paralogous Nucleotide Alignment",geneTemp.addAlignmentNumber(nFasta2),false);
                        JFrameMain.addTab(getGene().getName()+" Nucleotide Paralogs", reportTab,false);           
                    }else
                        JOptionPane.showMessageDialog(null, "Paralogous nucleotide alignment is not available!","Warning", JOptionPane.WARNING_MESSAGE);
                    break;
                case "paraprot":
                    if(pFasta2!=null&&!(pFasta2.equals(""))){   
                       JPanel reportTab = new ReportTab(getGene().getName()+" Paralogous Protein Alignment",geneTemp.addAlignmentNumber(pFasta2),true);
                       JFrameMain.addTab(getGene().getName()+" Protein Paralogs", reportTab,false);           
                   }else
                       JOptionPane.showMessageDialog(null, "Paralogous protein alignment is not available!","Warning", JOptionPane.WARNING_MESSAGE);
                    break;
                case "homonucleo":
                    if(nFasta!=null&&!(nFasta.equals(""))){   
                      JPanel reportTab = new ReportTab(getGene().getName()+" Orthologous Nucleotide Alignment",geneTemp.addAlignmentNumber(nFasta),false);
                      JFrameMain.addTab(getGene().getName()+" Nucleotide Orthologs", reportTab,false);           
                  }else
                      JOptionPane.showMessageDialog(null, "Unable to open Orthologous nucleotide alignment!","Warning", JOptionPane.WARNING_MESSAGE);
                    break;   
                case "homoprot":
                    if(pFasta!=null&&!(pFasta.equals(""))){   
                        JPanel reportTab = new ReportTab(getGene().getName()+" Orthologous Protein Alignment",geneTemp.addAlignmentNumber(pFasta),true);
                        JFrameMain.addTab(getGene().getName()+" Protein Orthologs", reportTab,false);           
                    }else
                        JOptionPane.showMessageDialog(null, "Unable to open Orthologous protein alignment!","Warning", JOptionPane.WARNING_MESSAGE);
                    break;
            }
    }
    
    //Converts dbSNP rs numbers to readable formats
    private String rsConvert(String cb){
        URLReader converter=new URLReader();
        cb=""+Integer.parseInt(cb.substring(2));
        String SNPURL ="http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=snp&id="+cb+"&report=XML";
        URLReader rs = new URLReader();
        try{    
            rs.findStringinURL(SNPURL,"");
        }catch(Exception exe){
                JOptionPane.showMessageDialog(null, "Could not find rs"+cb+" in dbSNP for "
                        +getGene().getName()+".", "Warning", JOptionPane.WARNING_MESSAGE);
                return null;
        }
        cb=rs.getHTML();
        try{    
            int index1=0;
            while(true){
                index1=cb.indexOf(gene1.getProtID());
                int index2=index1+cb.substring(index1).indexOf("<");
                String aminoAcidChange=cb.substring(index1,index2);
                if(aminoAcidChange.contains("=")){
                    cb=cb.substring(index1+1);
                    index1=0;
                    continue;
                }
                aminoAcidChange=aminoAcidChange.substring(aminoAcidChange.indexOf("p.")+2);
                aminoAcidChange=aminoConvert(aminoAcidChange.substring(0,3))+aminoAcidChange.substring(3,aminoAcidChange.length()-3)+aminoConvert(aminoAcidChange.substring(aminoAcidChange.length()-3));
                if(aminoAcidChange.contains("null")){
                    JOptionPane.showMessageDialog(null, "Could not find SNP in dbSNP in "
                            +getGene().getName()+".", "Warning", JOptionPane.WARNING_MESSAGE);
                    return null;
                }
                return aminoAcidChange;
            }
        }catch(StringIndexOutOfBoundsException exe){
            JOptionPane.showMessageDialog(null, "Could not find specified SNP in "
                    + "dbSNP in "+getGene().getName()+" refSeq.\n"
                    + "The SNP may be located on a non-coding gene segment.", 
                    "Warning", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }
    
    //Converts 3 letter amino acids into single letter codes
    public synchronized static String aminoConvert(String amino3){
        switch(amino3.toLowerCase()){
            case "ala":
                return "A";
            case "arg":
                return "R";
            case "asn":
                return "N";
            case "asp":
                return "D";
            case "asx":
                return "B";
            case "cys":
                return "C";
            case "glu":
                return "E";
            case "gln":
                return "Q";
            case "glx":
                return "Z";
            case "gly":
                return "G";
            case "his":
                return "H";
            case "ile":
                return "I";
            case "leu":
                return "L";
            case "lys":
                return "K";
            case "met":
                return "M";
            case "phe":
                return "F";
            case "pro":
                return "P";
            case "ser":
                return "S";                
            case "thr":
                return "T";
            case "trp":
                return "W";               
            case "tyr":
                return "Y";
            case "val":
                return "V";
            case "ter":
                return "*";  
        }
        return null;
    }
    
    //Makes a nucleotide/protein sequence into a multiline sequence
    private String multiLine(String sequenceOriginal){
        int sequenceLength=sequenceOriginal.length();
        String sequence="";
        for(int i=1;81*i<sequenceLength;i++)
            sequence+=sequenceOriginal.substring((i-1)*81,i*81+1)+"\n";
        sequence+=sequenceOriginal.substring(sequenceOriginal.length()
                -(sequenceOriginal.length()%81));
        return sequence;
    }
    
    //Checks to see if SNP has already been added and notifies the user
    private boolean checkSNPs(SNP input){
        String SNPName=aFasta.substring(input.getProtLoc()-1,input.getProtLoc());
        SNPName+=input.getProtLoc()+input.getProtChange();
        for(int i=0; i<model.getRowCount();i++){
            if(((String)model.getValueAt(i, 1)).equals(SNPName)){
                if(!activeBatch){
                    JOptionPane.showMessageDialog(null, SNPName+" has already been "
                        + "added to "+getGene().getName()+". Select the SNP and press"
                            + " \nSelected SNP Info for additional information, "
                            + "if available.", 
                        "Message", JOptionPane.PLAIN_MESSAGE);
                        table.getSelectionModel().setSelectionInterval(i, i);
                        table.scrollRectToVisible(new Rectangle(table.getCellRect(i, 0, true)));                    
                        return false;
                 }else
                    for(SNP s: tempSNPs)
                        if(s!=null&&SNPName.substring(1).equals(s.getProtLoc()+s.getProtChange())){
                            rowURL[model.getRowCount()]=(s.getInfoUrl());
                            input.setMultipleConfirmed(true);
                            break;
                }
            }
        }
        return true;
    }
    
    /*Calculates and returns possible phenotypes of a SNP based on surrounding
     * SNPs and user specified bouundaries
     */     
    private String getPossiblePhenotype(SNP input){
        int protLoc=input.getProtLoc();
        List<String> phenotypes = new ArrayList<>();
        List<String> samePheno=new ArrayList<>();
        for(int i=0; i<numClinicalSNPs;i++){
            String protInfo=((String)model.getValueAt(i, 1));
            int phenoProtLoc=Integer.parseInt(protInfo.substring(1,protInfo.length()-1));
            if((input.getProtLoc()+input.getProtChange()).equals(protInfo.substring(1)))
                samePheno.add((String)model.getValueAt(i, 2));
            if(phenoProtLoc>protLoc-JFrameMain.getLow()&&phenoProtLoc<protLoc+JFrameMain.getHigh())
                phenotypes.add((String)model.getValueAt(i, 2));
        }
        if(samePheno.size()>0)
            if(samePheno.size()==1&&!samePheno.get(0).equals("not provided"))
                return samePheno.get(0);
            else{
                String allPheno="";
                for(int i=0; i<samePheno.size(); i++)
                    if(i==samePheno.size()-1&&!samePheno.get(i).equals("not provided"))
                        allPheno+=samePheno.get(i);
                    else if(!samePheno.get(i).equals("not provided"))
                        allPheno+=samePheno.get(i)+" OR ";
                if(!allPheno.equals(""))
                    return allPheno;
            }
        if(phenotypes.size()>0){
            List<String> distinctPhenotypes=new ArrayList<>();
            distinctPhenotypes.add(phenotypes.get(0));
            for(int i=1; i<phenotypes.size();i++){
                //boolean notDistinct=false;
                boolean addPhenotype=true;
                for(int j=0; j<distinctPhenotypes.size();j++){
                    if(phenotypes.get(i).equals(distinctPhenotypes.get(j))){
                        addPhenotype=false;
                        break;
                    }
                }
                if(addPhenotype)
                    distinctPhenotypes.add(phenotypes.get(i));                   
            }
            int[] frequency=new int[distinctPhenotypes.size()];
            for(int i=0; i<distinctPhenotypes.size();i++){
                for(int j=0;j<phenotypes.size();j++){
                    if(phenotypes.get(j).equals(distinctPhenotypes.get(i)))
                        frequency[i]++;
                }
            }
            int maxFreqIndex=0;
            boolean repeat=false;
            for(int i=1; i<frequency.length;i++){
                if(frequency[i]>frequency[i-1]){
                    maxFreqIndex=i;
                    repeat = false;
                }else if(frequency[i]==frequency[maxFreqIndex])
                    repeat=true;
            }
            if(!repeat)
                if(distinctPhenotypes.get(maxFreqIndex).equals("not provided"))
                    return "Unknown";
                else
                    return "Possibly "+distinctPhenotypes.get(maxFreqIndex);
        }
        return "Unknown";
    }
    
    //Adds numbers to regular sequences
    private String addRegularNumbers(String sequence){
        String newSequence=sequence;
        sequence+="\n";
        int index=0;
        int counter=0;
        String row;
        while(true){
            try{
                row=sequence.substring(index,index+sequence.substring(index).indexOf("\n"));
            }catch(StringIndexOutOfBoundsException exe){
                return newSequence;
            }
            counter+=row.length()-1;
            try{
                newSequence=newSequence.substring(0,newSequence.indexOf(row))
                    +row.substring(0,row.length()-1)+"   "
                    +counter+sequence.substring(sequence.indexOf(row)+row.length());
            }catch(Exception exe){
                System.out.println();
            }
            index=index+row.length()+1;
        }
    }
    
    /*Iteratively searches through reading frames of DNA on the + strand until
     * a reading frame is found that works.
     */
    private String findInProtein(int index){
        if(index<3)
            return null;
        String nucleoSeq=tempDNA.substring(index,index+45);
        for(int i=0; i<3; i++){
            String protein = geneTemp.convertToAmino(nucleoSeq);
            if(aFasta.indexOf(protein)!=-1)
                return nucleoSeq;
            nucleoSeq=tempDNA.substring(tempDNA.indexOf(nucleoSeq)-1,tempDNA.indexOf(nucleoSeq)+45);
        }
        return null;
    }
    
    //Checks nucleotide sequence inputs
    private boolean checkInput(String nucleoSeq){
        int i=tempDNA.indexOf(nucleoSeq);
        if(i==-1){
            JOptionPane.showMessageDialog(null, "Could not find location/sequence"
                    + " at entered location!", "Warning", JOptionPane.WARNING_MESSAGE);            
            return false;
        }else
            if(tempDNA.substring(i+1).indexOf(nucleoSeq)!=-1){
                JOptionPane.showMessageDialog(null, "There are multiple occurences"
                        + " of your sequence in the DNA.\n Please enter a longer"
                        + " portion of the flanking sequence.", 
                        "Warning", JOptionPane.WARNING_MESSAGE);
                return false;
            } else 
                return true;
    }
    
    @Deprecated
    private String percentageToProbability(double percentage){
        if(percentage>89){
            return "Very likely";
        } else if(percentage>77){
            return "Likely";
        } else if(percentage>70){
            return "Possible";
        } else if(percentage>34){
            return "Unlikely";
        } else{
            return "Very unlikely";
        }            
    }
     
    /*Detects if protein has high subconservation in paralogs or orthologs (ie.
     * human allele is recessive so pathogenicity accuracy decreases)
     */
    private boolean subConservation(String otherReport){
        try{
            List<String> replacements=new ArrayList<>();
            int index=0;
            while(true){
                try{    
                    String s=otherReport.substring(index,index+otherReport.substring(index).indexOf("\n"));
                }catch(Exception exe){
                    break;
                }
                if(otherReport.substring(index,index+otherReport.substring(index).indexOf("\n")).indexOf("ins")==-1){
                    index=index+otherReport.substring(index).indexOf("\n")+1;
                    continue;
                }
                String tempReport=otherReport.substring(index,index+otherReport.substring(index).indexOf("ins")).trim();
                replacements.add(tempReport.substring(0,tempReport.indexOf(" ")));
                if(otherReport.substring(index).indexOf("\n")==-1)
                    break;
                index=index+otherReport.substring(index).indexOf("\n")+1;
            }
            if(replacements.size()>5){
                Collections.sort(replacements);
                double maxCounter=0;
                int tempCounter=0;
                for(int i=1; i<replacements.size(); i++)
                    if(replacements.get(i).equals(replacements.get(i-1)))
                        tempCounter++;
                    if(tempCounter>maxCounter)
                        maxCounter=tempCounter;
                double p=(maxCounter+1)/replacements.size();
                 if(((maxCounter+1)/replacements.size())>0.87)
                    return true;
            }
            return false;
        }catch(Exception exe){
            return false;
        }
    }
    
    //Checks to see if a SNP conservation at x position contains any deletions/insertions
    private boolean checkDeletions(String otherReport){
        try{
            List<String> replacements=new ArrayList<>();
            int index=0;
            if(otherReport.indexOf("\n")==-1){
                return false;
            }
            while(true){
                try{    
                    String s=otherReport.substring(index,index+otherReport.substring(index).indexOf("\n"));
                }catch(Exception exe){
                    break;
                }
                if(otherReport.substring(index,index+otherReport.substring(index).indexOf("\n")).indexOf("ins")==-1){
                    index=index+otherReport.substring(index).indexOf("\n")+1;
                    continue;
                }
                String tempReport=otherReport.substring(index,index+otherReport.substring(index).indexOf("ins")).trim();
                replacements.add(tempReport.substring(0,tempReport.indexOf(" ")));
                if(otherReport.substring(index).indexOf("\n")==-1)
                    break;
                index=index+otherReport.substring(index).indexOf("\n")+1;
            }
            for(String s: replacements)
                if(s.trim().indexOf("Deletion")==-1)
                    return false;            
            return true;
        }catch(Exception exe){
            return false;
        }        
    }
    
    //Obtains generation accepted mutation in jagged matrix (Blosum 62)
    private String toleratedSubst(String amino1, String amino2){
        int amino1Loc=getMatrixNumber(amino1);
        int amino2Loc=getMatrixNumber(amino2);
        int temp;
        if(amino1Loc<amino2Loc){
            temp=amino1Loc;
            amino1Loc=amino2Loc;
            amino2Loc=temp;
        }
        int[][] tolMatrix=																				
            {{},
            { 0},																			
            {-2,-3},																		
            {-1,-4, 2},																	
            {-2,-2,-3,-3},																
            { 0,-3,-1,-2,-3},															
            {-2,-3,-1, 0,-1,-2},														
            {-1,-1,-3,-3, 0,-4,-3},													
            {-1,-3,-1, 1,-3,-2,-1,-3},												
            {-1,-1,-4,-3, 0,-4,-3, 2,-2},											
            {-1,-1,-3,-2, 0,-3,-2, 1,-1, 2},										
            {-2,-3, 1, 0,-3, 0, 1,-3, 0,-3,-2},									
            {-1,-3,-1,-1,-4,-2,-2,-3,-1,-3,-2,-2},								
            {-1,-3, 0, 2,-3,-2, 0,-3, 1,-2, 0, 0,-1},							
            {-1,-3,-2, 0,-3,-2, 0,-3, 2,-2,-1, 0,-2, 1},						
            { 1,-1, 0, 0,-2, 0,-1,-3, 0,-2,-1, 1,-1, 0,-1},					
            { 0,-1,-1,-1,-2,-2,-2,-3,-1,-1,-1, 0,-1,-1,-1, 1},				
            { 0,-1,-3,-2,-1,-3,-3, 3,-2, 1, 1,-3,-2,-2,-3,-2, 0},			
            {-3,-2,-3,-3, 1,-2,-2,-3,-3,-2,-1,-4,-4,-2,-3,-3,-2,-3},		
            {-2,-2,-3,-2, 3,-3, 2,-3,-2,-1,-1,-2,-3,-1,-2,-2,-2,-1, 2},	
            {-8,-8,-8,-8,-8,-8,-8,-8,-8,-8,-8,-8,-8,-8,-8,-8,-8,-8,-8,-8}};
        int numAtLoc=tolMatrix[amino1Loc][amino2Loc];
        if(numAtLoc<=-3)
            return "Rarely tolerated ("+numAtLoc+")";
        else if(numAtLoc<0)
            return "Normally not tolerated ("+numAtLoc+")";
        else if(numAtLoc==0)
            return "Normally neutral ("+0+")";
        else 
            return "Usually tolerated ("+numAtLoc+")";
    }
    
    //For an amino acid, returns row/column in Blosum 62 matrix
    private int getMatrixNumber(String amino){
        switch(amino.toLowerCase()){
            case "g":
                return 5;
            case "a":
                return 0;
            case "v":
                return 17;
            case "l":
                return 9;
            case "i":
                return 7;
            case "p":
                return 12;
            case "s":
                return 15;
            case "t":
                return 16;
            case "d":
                return 2;
            case "e":
                return 3;
            case "n":
                return 11;
            case "q":
                return 13;
            case "k":
                return 8;
            case "r":
                return 14;
            case "h":
                return 6;
            case "f":
                return 4;
            case "y":
                return 19;
            case "w":
                return 18;
            case "m":
                return 10;
            case "c":
                return 1;
        }
        return 20;
    }
    
    //Obtains number of paralogs/ orthologs conserved
    private int numFromPercentage(int numMax,int percentage){
        for(int i=0;i<=numMax; i++){
           double percentageCheck=i/(numMax+0.0)*100;
           if(percentageCheck>percentage-1.5&&percentageCheck<percentage+1.5)
               return i;
        }
        return -1;
    }
    
    public void setSelected(int selected){
        table.getSelectionModel().setSelectionInterval(selected, selected);
    }
    
    //Returns a prediction for pathogenicity of SNP
    private String predictPathogenic(double protCons, double PSICScore, int substScore){
        if(!JFrameMain.useLit){
            if(protCons<81)
                return "Benign(a)";
            else
                return "Pathogenic";
        }
        if(numClinicalSNPs==0&&JFrameMain.useLit)
            return "Benign(h)";
        if(PSICScore>0&&(PSICScore<1.05||protCons<40))
            return "Benign(a)";
        else if(PSICScore>0)
            return "Pathogenic";
        else if(PSICScore<0&&protCons<41)
            return "Benign(a)";
        else
            return "Pathogenic";
    }
    
    //Connects to SQL cloud
    private void connectToSQL() throws Exception{
        String connectionUrl="jdbc:sqlserver://genes.cedf0sccpt06.us-east-1.rds.amazonaws.com:1433;"
               + "databaseName=Genes;user=allAccess;password=xFz23d4a3Ln5dZ5sW";      
        //String connectionUrl="jdbc:sqlserver://localhost:1433;"
         //       + "databaseName=Genes;user=sa;password=12345";        
        try{    
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            con = DriverManager.getConnection(connectionUrl);
        }catch(ClassNotFoundException | SQLException exe){
            throw exe;
        }
    }
    
    //Checks to see if data is in SQL and within acceptable time limit
    private boolean checkSQLForGene() throws SQLException{
        if(con==null)
            throw new SQLException();
        Statement stmt=null;
        ResultSet rs=null;
        String sqlQuery ="SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE='BASE TABLE'";
        try {
            stmt = con.createStatement();
            rs=stmt.executeQuery(sqlQuery);
            while(rs.next()){
                if(!rs.getString(1).toUpperCase().equals(gene.toUpperCase().replaceAll("-", "")))
                    continue;
                else{
                    if(JFrameMain.SQLTimeLimit<0&&JFrameMain.SQLTimeLimit!=-17)
                        return true;
                    sqlQuery="SELECT modify_date FROM sys.tables WHERE name = '"+gene.toUpperCase()+"'";
                    rs=stmt.executeQuery(sqlQuery);
                    rs.next();
                    String modifyDate=rs.getString(1);
                    String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").
                            format(Calendar.getInstance().getTime());
                    sqlTimeCheck=determineDateGap(currentDate,modifyDate,JFrameMain.SQLTimeLimit);
                    if(!sqlTimeCheck){
                        repeatCheck=true;
                        checkGene(gene.toUpperCase());
                    }
                        
                    return true;
                }
            }
        }catch(Exception exe){
            throw new SQLException();
        }finally{
            if(stmt!=null)
                stmt.close();
            else if(rs!=null)
                rs.close();
         }    
        return false;
    }
    
    //Retrieves data from SQL
    private void retrieveDataFromSQL() throws Exception{
        String sqlQuery="SELECT PROT_FASTA, NUCLEO_FASTA, "
                + "PARA_PROT_ALI, PARA_NUCLEO_ALI, HOMO_PROT_ALI, "
                + "HOMO_NUCLEO_ALI FROM dbo." +gene.toUpperCase().replace("-", "") +" ORDER BY PRIM_KEY";
        Statement stmt;
        ResultSet rs;
        stmt=con.createStatement();
        rs=stmt.executeQuery(sqlQuery);
        String protFasta=null, nucleoFasta=null, nID=null, pID=null;
        try{
           rs.next();
           protFasta=rs.getString(1);
           nucleoFasta=rs.getString(2);
           aFasta=protFasta.substring(protFasta.indexOf("\n")+1,protFasta.length()-1);
           tempDNA=nucleoFasta.substring(nucleoFasta.indexOf("\n")+1,nucleoFasta.length()-1);           
           pFasta2=rs.getString(3);
           nFasta2=rs.getString(4);
           pFasta=rs.getString(5);
           nFasta=rs.getString(6);
           if(pFasta==null||pFasta.equals("")){
               if(b!=null)
                   b.addError(gene.toUpperCase()+" has neither orthologous or "
                           + "paralogous genes that could be found. "
                           + "The nucleotide and protein sequences and other "
                           + "gene information is still availible, however.");
               else
                  JOptionPane.showMessageDialog(null, gene.toUpperCase()+" has neither orthologous or "
                           + "paralogous genes that could be found. "
                           + "The nucleotide and protein sequences and other "
                           + "gene information is still availible, however.", 
                           "Warning", JOptionPane.WARNING_MESSAGE);
           }
           sqlQuery="SELECT GENE_PROPS FROM dbo." + gene.toUpperCase().replace("-", "") + 
                   " ORDER BY PRIM_KEY";
           rs=stmt.executeQuery(sqlQuery);
           geneProperties[0]=gene;
           for(int i=1; i<9; i++){
               rs.next();
               geneProperties[i]=rs.getString(1);
           }
           gene1=new Gene(gene,geneProperties[4],geneProperties[5],nucleoFasta,protFasta);
           model=(DefaultTableModel)table.getModel();           
           tempSNPs=new SNP[5000];
           sqlQuery="SELECT SNP, SNP_PHENO, SNP_MULT, SNP_URL "
                   + "FROM dbo." + gene.toUpperCase().replace("-", "") + 
                   " ORDER BY PRIM_KEY";
           rs=stmt.executeQuery(sqlQuery);
           while(rs.next()){
               String SNPID=rs.getString(1);
               String SNPPheno=rs.getString(2);
               String SNPMult=rs.getString(3);
               String SNPURL=rs.getString(4);               
               if(SNPID==null)
                   continue;
               if(SNPPheno==null)
                   SNPPheno="Unknown";
               if(SNPMult==null)
                   SNPMult="Not verified";
               try{
                    SNP s = new SNP(SNPPheno, SNPID.substring(SNPID.length()-1, SNPID.length()),
                       "",Integer.parseInt(SNPID.substring(1,SNPID.length()-1)),
                       -1, false);
                    s.setInfoURL(SNPURL);
                    if(b==null)
                        model.addRow(new Object[]{
                            true,SNPID,SNPPheno,SNPMult,"Please Wait"});
                    else
                        model.addRow(new Object[]{
                            false,SNPID,SNPPheno,SNPMult,"Please Wait"});                        
                    tempSNPs[numClinicalSNPs++]=s;                     
                    rowURL[numClinicalSNPs-1]=tempSNPs[numClinicalSNPs-1].getInfoUrl();                                         
                }catch(NumberFormatException exe){
                    continue;
                }
           }
           allSNPs = new SNP[5000];             
           System.arraycopy(tempSNPs, 0, allSNPs, 0, numClinicalSNPs);
           geneURL=geneSearchURL=geneProperties[6];
           numHomologs=Integer.parseInt(geneProperties[7]);
           numParalogs=Integer.parseInt(geneProperties[8]);                     
           panel.setLabels(gene,geneProperties[1],""+geneProperties[2],
                   geneProperties[3],geneProperties[4],geneProperties[5]);
           panel.enableGeneSequences();
           panel.infoReady();   
           panel.enableParalogs();
           panel.stopWait();
           panel.enableMutationChecking();
           panel.stopWait2();
           panel.enableReport();
           panel.enableHomologs();         
        }catch(SQLException | NumberFormatException exe){
            System.out.println("Gene read error "+gene);
            clearAll();
            throw exe;
        }finally{
            if(rs!=null)
                rs.close();
            stmt.close();
        }
    }
    
    //Saves data to SQL
    private void saveDataToSQL() throws Exception{
        boolean drop=checkSQLForGene();
        String geneName=gene.toUpperCase().replaceAll("-", "");        
        if(drop){
            String dropStatement = "DROP TABLE Genes.dbo."+ geneName;
            try (Statement stmt = con.createStatement()) {
                stmt.executeUpdate(dropStatement);
            }catch(SQLException exe){
                throw exe;
            }
        }
        String cols =
        "CREATE TABLE dbo." +
        geneName +
        " ("+"PRIM_KEY INT NOT NULL, " +  
        "PROT_FASTA nvarchar(MAX), " +
        "NUCLEO_FASTA nvarchar(MAX), " +
        "GENE_PROPS nvarchar(150), " +      
        "PARA_PROT_ALI nvarchar(MAX), " + 
        "PARA_NUCLEO_ALI nvarchar(MAX), " + 
        "HOMO_PROT_ALI nvarchar(MAX), " + 
        "HOMO_NUCLEO_ALI nvarchar(MAX), " +      
        "SNP nvarchar(10), " +      
        "SNP_PHENO nvarchar(1000), " +      
        "SNP_MULT nvarchar(100), " + 
        "SNP_URL nvarchar(100), " +                 
        "PRIMARY KEY (PRIM_KEY))";
        try (Statement stmt = con.createStatement()) {
            stmt.executeUpdate(cols);
            String paraProt=pFasta2;
            String paraNucleo=nFasta2;
            String homoProt=pFasta;
            String homoNucleo=nFasta;
            if(paraProt==null)
                paraProt="NULL";
            else
                paraProt="'"+paraProt+"'";
            if(paraNucleo==null)
                paraNucleo="NULL";
            else
                paraNucleo="'"+paraNucleo+"'";
            if(homoProt==null)
                homoNucleo="NULL";
            else
                homoNucleo="'"+homoNucleo+"'";
            if(homoProt==null)
                homoProt="NULL";
            else
                homoProt="'"+homoProt+"'";            
            int maxRows=numClinicalSNPs;
            if(numClinicalSNPs<8)
                maxRows=8;
            for(int i=0; i<maxRows;i++){
                String s;
                String geneProp=null;
                if(i<8)
                    geneProp=geneProperties[i+1];
                if(geneProp==null)
                    geneProp="";
                String SNP, SNPPheno, SNPMult, SNPURL;
                try{
                    SNP=aFasta.substring(tempSNPs[i].getProtLoc()-1,
                        tempSNPs[i].getProtLoc())+tempSNPs[i].getProtLoc()+
                        tempSNPs[i].getProtChange();
                    SNPPheno=tempSNPs[i].getPhenotype();
                    SNPPheno=SNPPheno.replace("'", "''");  
                    if(tempSNPs[i].getMultipleConfirmed())
                        SNPMult="Multiple Publications";
                    else
                        SNPMult="Single Publication";  
                    SNPURL=tempSNPs[i].getInfoUrl();
                    SNPURL=SNPURL.replace("'", "''");                    
                }catch(Exception exe){
                    SNP=null;
                    SNPPheno=null;
                    SNPURL=null;
                    SNPMult=null;
                }
                if(SNP!=null)
                    SNP="'"+SNP+"'";
                if(SNPPheno!=null)
                    SNPPheno="'"+SNPPheno+"'";               
                if(SNP!=null)
                    SNPMult="'"+SNPMult+"'";                       
                if(SNPURL!=null)
                    SNPURL="'"+SNPURL+"'";                
                if(i==0)    
                    s = "INSERT INTO dbo." +
                    geneName+
                    " values('"+i+"', '"+gene1.getProtFasta()+"', " +
                    "'"+gene1.getFasta()+"', " +
                    "'"+geneProp+"', " +
                    paraProt+", " +
                    paraNucleo+", " +
                    homoProt+", " +                   
                    homoNucleo + ", " + 
                    SNP + ", " +
                    SNPPheno + ", " +
                    SNPMult + ", " +
                    SNPURL+")";
                else if(i<9)
                    s = "INSERT INTO dbo." +
                    geneName +
                    " values('"+i+"', NULL, " +
                    "NULL, " +
                    "'"+geneProp+"', " +
                    "NULL, " +
                    "NULL, " +
                    "NULL, " +                
                    "NULL," +
                    SNP + ", " +
                    SNPPheno + ", " +
                    SNPMult + ", " +
                    SNPURL+")";        
                else
                    s = "INSERT INTO dbo." +
                    geneName +
                    " values('"+i+"', NULL, " +
                    "NULL, " +
                    "NULL, " +
                    "NULL, " +
                    "NULL, " +
                    "NULL, " +                
                    "NULL," +
                    SNP + ", " +
                    SNPPheno + ", " +
                    SNPMult + ", " +
                    SNPURL+")";    
                    stmt.executeUpdate(s);
            }
        } catch (Exception exe) {
            System.out.println("Gene write error "+gene);            
            exe.printStackTrace();
            throw exe;
        }
    }
    
    //Closes SQL connection
    private void closeSQL() throws SQLException{
        if(con!=null)
            con.close();
    }
    
    private boolean determineDateGap(String current, String modify, int allowed){
        int currentNetDays=Integer.parseInt(current.substring(0,current.indexOf("-")))*365
            +Integer.parseInt(current.substring(current.indexOf("-")+1,
            current.indexOf("-")+1+current.substring(current.indexOf("-")+1)
            .indexOf("-")))*30
            +Integer.parseInt(current.substring(current.lastIndexOf("-")+1,
                current.indexOf(" ")));
        int modifyNetDays=Integer.parseInt(modify.substring(0,modify.indexOf("-")))*365
            +Integer.parseInt(modify.substring(modify.indexOf("-")+1,
                modify.indexOf("-")+1+modify.substring(modify.indexOf("-")+1)
                .indexOf("-")))*30
            +Integer.parseInt(modify.substring(modify.lastIndexOf("-")+1,
                modify.indexOf(" ")));
        int diffDays=Math.abs(currentNetDays-modifyNetDays);
        if(diffDays>allowed)
            return false;
        return true;
    }
    
    //Clears table so gene can be retried in event of internet timeout
    private void clearAll(){
        int rowCount=model.getRowCount();
        if(model!=null)
            for(int i=rowCount-1; i>=0;i--)
                model.removeRow(i);
    }
    
}