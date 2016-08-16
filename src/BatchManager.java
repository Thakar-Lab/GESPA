/*GESPA  Copyright (C) 2013  Jay Khurana
 * See GESPA License for additional information.
 */
package DNAFinderMain;

import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import java.net.ConnectException;
import java.io.IOException;

/**
 *
 * @author Jay Khurana
 */
public class BatchManager implements Runnable {
    
    private String fileText;
    private Thread[] threads;
    private List<String> geneURLs;
    private List<String> genes;
    private List<List<String>> SNPs;
    private List<Framework> current;
    private List<Framework> all;
    private List<String> report;
    private List<Integer> rowsPerGene;
    private List<String[]> geneProperties;
    private List<String> PSICScores;
    private List<String> toleratedSubst;
    private BatchManager b;
    private String errorReport;
    private int maxThreads=JFrameMain.MAX_CONCURRENT_THREADS;
    private JPanel errorTab;
    private GenePanel batchPanel;
    private int processAttempts=3;
    private boolean retryRequest;    
    public static int numBatchesIntThread;
    
    public BatchManager(String fileText){
        this.fileText=convertToGeneFormat(fileText);
        current = new ArrayList<>();
        all= new ArrayList<>();
        report=new ArrayList<>();
        rowsPerGene=new ArrayList<>();
        errorReport="";
        retryRequest=false;
    }
    
    //Converts batch text to a format readable by GESPA
    public final String convertToGeneFormat(String fileTextTemp){
        if(fileTextTemp.indexOf("Gene")!=-1)
            return fileTextTemp;
        int index=0;
        List<String> genes = new ArrayList<>();
        List<List<String>> SNPs = new ArrayList<>();
        List<String> tempSNPs=new ArrayList<>();
        Framework f = new Framework();
        while(index!=-1){
            int p =index+fileTextTemp.substring(index).indexOf("\t");
            boolean noCheck=false;
            String temp;
            try{
                temp=fileTextTemp.substring(index,p);
            }catch(Exception exe){
                SNPs.add(tempSNPs);
                break;
            }
            if(genes.size()>0){
                if(!temp.equals(genes.get(genes.size()-1))){
                    genes.add(temp);
                    SNPs.add(tempSNPs);
                    tempSNPs=new ArrayList<>();
                }
            }else
                genes.add(temp);
            String temp2=fileTextTemp.substring(p+1,p+fileTextTemp.substring(p).indexOf("\r"));
            tempSNPs.add(temp2);
            index=index+fileTextTemp.substring(index).indexOf("\r")+1;
        }
        fileTextTemp="";
        for(int i=0; i<genes.size();i++){
            if(i>0)
                fileTextTemp=fileTextTemp+"\rGene: "+genes.get(i);
            else 
                fileTextTemp=fileTextTemp+"Gene: "+genes.get(i);                
            //System.out.println("Gene: "+genes.get(i));
            try{    
                for(int j=0; j<SNPs.get(i).size(); j++){
                    fileTextTemp=fileTextTemp+"\r"+SNPs.get(i).get(j);                    
                    //System.out.println(SNPs.get(i).get(j));
                }
            }catch(Exception exe){}
        }
        try{
            return fileTextTemp+"\r";
        }catch(StringIndexOutOfBoundsException exe){
            return "";
        }
        
    }
    
    public void setBatchManager(BatchManager b){
        this.b=b;
    }
    
    //Starts batch manager thread
    @Override
    public void run(){
        Batch batch;
        try{
            batch = new Batch(fileText,this.b);
        }catch(Exception exe){
            JFrameMain.progLabel.setText("");
            JOptionPane.showMessageDialog(null,"There was an error in your batch "
                    + "text file. Please view the batch error report for more "
                    + "details.", "Warning",JOptionPane.WARNING_MESSAGE);
            JFrameMain.processingBatch=false;
            return;
        }
        geneURLs=batch.getGeneUrls();
        genes=batch.getGenes();
        SNPs=batch.getSNPs();
        threads=new Thread[geneURLs.size()];
        for(int i=0; i<geneURLs.size(); i++){
            current.add(new Framework());
            current.get(i).setBatchManager(b);
        }
       threadManager();
       JFrameMain.progLabel.setText("");
    }
    
    //Called by all Framework threads, initiates another thread while another is removed
    public synchronized void threadManager(){
        int numRemaining=geneURLs.size();
        if(JFrameMain.numConcurrentThreads>JFrameMain.MAX_CONCURRENT_THREADS)
            return;
        if(numRemaining>JFrameMain.MAX_CONCURRENT_THREADS)
            numRemaining=JFrameMain.MAX_CONCURRENT_THREADS;
        if(numRemaining==0)
            return;
        int maxThreadsTemp=maxThreads;
        if(retryRequest)
            maxThreadsTemp=1;
        for(int i=0; i<maxThreadsTemp;i++){
            if(SNPs.isEmpty())
                break;
            if(!current.get(0).getActiveBatch()){
                try{  
                    JPanel batchPanel=new GenePanel();
                    current.get(0).setBatchManager(b);
                    current.get(0).setActiveBatch(true);
                    current.get(0).setPanel(batchPanel);
                    current.get(0).setFramework(current.get(0));
                    //JFrameMain.addTab(genes.get(0), batchPanel);                
                    current.get(0).setGene(genes.get(0));
                    current.get(0).setSNPs(SNPs.get(0));  
                    if(geneURLs.get(0)!=null)
                        current.get(0).setGeneURL(geneURLs.get(0));
                }catch(Exception exe){
                    if(exe instanceof ConnectException||exe instanceof IOException){
                        if(processAttempts!=1)
                            this.addError("Connection time out on gene "+genes.get(0)+". Will attempt to process this gene "+processAttempts--+" more times.");
                        else
                            this.addError("Connection time out on gene "+genes.get(0)+". Will attempt to process this gene "+processAttempts--+" more time.");
                        if(processAttempts>0){
                            retryRequest=true;                            
                            current.get(0).setActiveBatch(false);
                            threadManager();
                            if(--processAttempts==-2){
                                processAttempts=3;
                                i--;
                                continue;
                            }
                        }else{
                            retryRequest=false;
                            addError("Could not process gene "+genes.get(0)+ " due to too many connection timeouts.");
                            genes.remove(0);
                            geneURLs.remove(0);
                            SNPs.remove(0);
                            current.remove(0);
                            if(maxThreads==1){
                                processAttempts=3;
                                b.threadManager();
                            }else
                                processAttempts=0;
                        }                           
                        return;
                    }else{
                        this.addError("This application requires"
                                + "a valid internet connection!");
                        JFrameMain.processingBatch=false;
                        exe.printStackTrace();
                        return;
                    }
                }
                geneURLs.remove(0);
                genes.remove(0);
                SNPs.remove(0);
                JFrameMain.numConcurrentThreads++;                
                new Thread(current.get(0)).start();
                current.remove(0);
                retryRequest=false;
            }           
        }   
            maxThreads=1;
            JFrameMain.processingBatch=false;
    }
    
    //Adds data from a framework to a batch panel
    public synchronized void batchPanelManager(Framework f, DefaultTableModel model){
        if(batchPanel==null){
            batchPanel=new GenePanel();
            batchPanel.setBatchManager(this);
            JFrameMain.addTab("Batch Summary",batchPanel,true);
        }
        if(PSICScores==null)
            PSICScores=new ArrayList<>();
        if(toleratedSubst==null)
            toleratedSubst=new ArrayList<>();
        DefaultTableModel batchModel = (DefaultTableModel)batchPanel.getTable().getModel();
        int numRows=0;
        int counter=0;            
        List<String>temp=f.getPSICScores();
        List<String> temp2=f.getToleratedSubst();
        for(int i=0; i<model.getRowCount();i++){
            try{
            Object[] rowDataTest= new Object[5];
            Object test=f.getReport()[i];
            temp.get(i);
            temp2.get(i);
            for(int j=0; j<model.getColumnCount(); j++){
               counter++;
               if(j!=1)     
                    rowDataTest[j]=model.getValueAt(i, j);
               else
                    rowDataTest[j]=f.getGene().getName()+": "+model.getValueAt(i, j);                   
            }
            }catch(Exception exe){
                continue;
            }
            Object[] rowData= new Object[5];
            report.add(f.getReport()[i]);
            PSICScores.add(temp.get(i));
            toleratedSubst.add(temp2.get(i));
            for(int j=0; j<model.getColumnCount(); j++){
               counter++;
               if(j!=1)     
                    rowData[j]=model.getValueAt(i, j);
               else
                    rowData[j]=f.getGene().getName()+": "+model.getValueAt(i, j);                   
            }
            batchModel.addRow(rowData);
            numRows++;
        }
        if(numRows==0){
            numRows++;
            batchModel.addRow(new Object[]{false,f.getGene().getName()+": " + "No SNPs","N/A","N/A","N/A"});
            report.add("");
            PSICScores.add("N/A");
            toleratedSubst.add("N/A");
        }
        rowsPerGene.add(numRows);
        all.add(f);
        if(batchPanel.getTable().getSelectedRow()==-1&&batchModel.getRowCount()!=-1)
            updateBatchPanel();
    }
    
    //Adds error to batch, can be called from framework or batch thread
    public synchronized void addError(String error){
        if(error==null)
            return;
        errorReport+=error+"\n";
        if(!(errorReport.equals(""))&&errorTab==null){
            errorTab= new ReportTab("Batch Error Report",errorReport);
            JFrameMain.addTab("Batch Errors", errorTab,true);
        }else if(!(errorReport.equals(""))&&errorTab!=null){
            ReportTab t=(ReportTab)errorTab;
                t.changeReport(errorReport);
        }       
    }
    
    //Updates batch panel information when user selects SNP
    public synchronized void updateBatchPanel(){
        int selection=batchPanel.getTable().getSelectedRow();
        if(selection==-1)
            selection=0;
        int totalRows=0;
        for(int i=0; i<rowsPerGene.size(); i++){
            totalRows+=rowsPerGene.get(i);
            if(selection<totalRows){
                batchPanel.updateForBatch(all.get(i));                
                batchPanel.setFramework(all.get(i));
                return;
            }
        }
    }
        
    public int getSelectedOnSub(){
        int selection=batchPanel.getTable().getSelectedRow();
        if(selection==-1)
            selection=0;
        int totalRows=0;
        for(int i=0; i<rowsPerGene.size(); i++){
            totalRows+=rowsPerGene.get(i);
            if(selection<totalRows){
                return rowsPerGene.get(i)-totalRows+selection;
            }
        }
        return -1;
    }
    
    //Generates report for batch
    public void generateReport(){
        Framework temp= new Framework();
        temp.setTable(batchPanel.getTable());
        temp.setReports(report);
        temp.setActiveBatch(true);
        temp.generateReport();
    }
    
    public void generateConservationReport(){
        Framework temp= new Framework();
        temp.setTable(batchPanel.getTable());
        temp.setReports(report);
        temp.setPSICScores(PSICScores);
        temp.setToleratedSubst(toleratedSubst);
        temp.setActiveBatch(true);
        temp.setBatchManager(b);
        temp.generateConservationReport();        
    }
    
    public int getNumHomologs(int selection){
        int totalRows=0;
        for(int i=0; i<rowsPerGene.size(); i++){
            totalRows+=rowsPerGene.get(i);
            if(selection<totalRows){
                return all.get(i).getNumHomologs();
            }
        }
        return 0;
    }
    
    public int getNumParalogs(int selection){
        int totalRows=0;
        for(int i=0; i<rowsPerGene.size(); i++){
            totalRows+=rowsPerGene.get(i);
            if(selection<totalRows){
                return all.get(i).getNumParalogs();
            }
        }
        return 0;
    }
    
}
