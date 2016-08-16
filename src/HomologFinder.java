/*GESPA  Copyright (C) 2013  Jay Khurana
 * See GESPA License for additional information.
 */
package DNAFinderMain;

/**
 *
 * @author Jay Khurana
 */



public class HomologFinder {

    private Gene [] homolog;
    private String URL1;
    private String homologURLHTML;
    private String[] possibleHomologs={"M.oryzae" , "M.mulatta" , "P.troglodytes" , "C.lupus" , "B.taurus" , "M.musculus" , "R.norvegicus" , "G.gallus" , "D.rerio" , "D.melanogaster" , "A.gambiae" , "C.elegans" , "S.pombe" , "S.cerevisiae" , "K.lactis" , "E.gossypii" , "N.crassa" , "A.thaliana" , "O.sativa"};
    private String[] homologCommon={"fungus","Rhesus monkey","chimpanzee","dog","cow","mouse","rat","chicken","zebrafish","drosophila","mosquito","nematode","fission yeast","common yeast","kluy. yeast","mold","nerve mold","flower", "rice"};
    private Framework f;
    private int numHomologs;
    
    public HomologFinder(String homologURLHTML, String URL1, Framework f) {
        this.f=f;
    	this.homologURLHTML = homologURLHTML;
    	homolog = new Gene[500];
    	this.URL1=URL1;
    	homolog[0]=f.getGene();
        numHomologs=0;
    }
    
    public String[] findHomologsEut(String homologID) throws Exception{
        String[] tempURLs = new String[500];
	NCBIFinder n = new NCBIFinder();
    	URLReader url = new URLReader();
        url.findStringinURL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/"
                + "efetch.fcgi?db=homologene&id="+homologID,"");
        String homologURLHTMLC=homologURLHTML=url.getHTML();
        int numHomologsTemp=0;
        while(true){
            if(!homologURLHTMLC.contains("geneid "))
                break;
            String taxID=homologURLHTMLC.substring(homologURLHTMLC.indexOf("prot-acc")-250,
                    homologURLHTMLC.indexOf("prot-acc"));
            homologURLHTMLC=homologURLHTMLC.substring(homologURLHTMLC.indexOf("prot-acc ")+1);            
            taxID=taxID.substring(taxID.indexOf("taxid"));
            taxID=taxID.substring(6,taxID.indexOf(","));
            String protID=homologURLHTMLC.substring(0,100);
            protID=protID.substring(9,protID.indexOf(",")-1);
            String nucID=homologURLHTMLC.substring(homologURLHTMLC.indexOf("nuc-acc"),homologURLHTMLC.indexOf("nuc-acc")+100);
            nucID=nucID.substring(9,nucID.indexOf(",")-1);
            if(numHomologsTemp==0){
                numHomologsTemp++;
                continue;        
            }
            n.waitComplete();
            URLReader taxaReader=new URLReader();
            taxaReader.findStringinURL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=taxonomy&id="+taxID, "");
            String taxaHTML=taxaReader.getHTML();
            String taxaName=nucID;
            if(taxaHTML.contains("GenbankCommonName")){
               taxaName=taxaHTML.substring(taxaHTML.indexOf("GenbankCommonName"),taxaHTML.indexOf("GenbankCommonName")+100);
                taxaName=taxaName.substring(18,taxaName.indexOf("<"));
            }else if(taxaHTML.contains("ScientificName")){
                taxaName=taxaHTML.substring(taxaHTML.indexOf("ScientificName"),taxaHTML.indexOf("ScientificName")+100);
                taxaName=taxaName.substring(15,taxaName.indexOf("<"));                
            }
            taxaName=taxaName.replaceAll(" ", "_");
            String fasta=">"+taxaName+"\n"+n.nucleoFinder(nucID)+"\n";
            String protFasta=">"+taxaName+"\n"+n.protFinder(protID)+"\n";            
            homolog[numHomologsTemp]=new Gene(taxaName,nucID,protID,fasta,protFasta);
           // String name, String nucleoID, String protID, String fasta, String protFasta;                
            numHomologsTemp++;
        }
        numHomologs=numHomologsTemp;
        Exception throwable= new Exception();
        for(int i=0; i<8; i++)    
            try{
                String nFasta=Gene.alignFasta(homolog, true);
                String pFasta=Gene.alignFasta(homolog, false);
                String[] fastas = {nFasta,pFasta}; 			
                if(fastas!=null)
                    return fastas;
            } catch(Exception exe){throwable=exe;}
//          JOptionPane.showMessageDialog(null, "Error: Unable to align fasta sequences. Please try again later.", "Error", JOptionPane.ERROR_MESSAGE); 
        throw throwable;       
    }
    
    public String[] findHomologs() throws Exception{
        String[] tempURLs = new String[500];
	NCBIFinder n = new NCBIFinder();
    	URLReader url = new URLReader();
    	String tempURL;
    	try{
    		url.findStringinURL(URL1,"");
    	}catch(Exception exe){
    		System.out.print("");
    	}
    	int hlIndex=1;
    	int commonCounter=0;
        for(String s: possibleHomologs){
    			String h=homologURLHTML;
    			int numTimes=0;
    			while(h.indexOf(s)!=-1){
    				numTimes++;
    				h=h.substring(h.indexOf(s)+1);    				
    			}
    			numTimes /=3;
    			for(int i=0; i<numTimes; i++){
                            
                                h=homologURLHTML.substring(homologURLHTML.indexOf("("+s)-30,homologURLHTML.indexOf("("+s));
    				h=h.substring(h.indexOf(">")+1,h.length()-1);
    				if(!(h.equals(""))){
                                            homolog[hlIndex]=new Gene(homologCommon[commonCounter],"",h,"",n.protFinder(h));
 						//System.out.println(s);
                                                try{
                                                    tempURL = url.extractHyperLink(500,s, "/gene/").trim();
                                                }catch(Exception exe){continue;}
                                                tempURL = "http://www.ncbi.nlm.nih.gov"+tempURL;
 						tempURLs[hlIndex]=tempURL;
 						hlIndex++;
                                                break;
    				}

    			}
                        commonCounter++;
    		}
    		int hlLogicalSize=hlIndex;
    		hlIndex=1;
   			System.out.println("Setting Fastas");
   			for(String s2: tempURLs){
    			try{
                            if(s2!=null){
                                url.findStringinURL(s2,"");
					homolog[hlIndex].setName(homolog[hlIndex].getName().replaceAll(" ", "_"));
                                        if(homolog[hlIndex].getName().length()>15){
						homolog[hlIndex].setName(homolog[hlIndex].getName().substring(0,16));
					}
					String tempID = url.getHTML().substring(url.getHTML().indexOf("mRNA and Protein"),url.getHTML().indexOf("mRNA and Protein")+250);
					String nucleoID = tempID.substring(tempID.indexOf("core")+5,tempID.indexOf("core")+ tempID.substring(tempID.indexOf("core")).indexOf("\""));
					homolog[hlIndex].setFasta(">"+homolog[hlIndex].getName()+"\n"+n.nucleoFinder(nucleoID)+"\n");
					homolog[hlIndex].setProtFasta(">"+homolog[hlIndex].getName()+"\n"+homolog[hlIndex].getProtFasta()+"\n");
					hlIndex++;
                            }
                        }catch(Exception exe){
    				System.out.print("");
    			}
    		}
                for(int i=0; i<homolog.length;i++){
                    if(homolog[i]!=null)
                        numHomologs++;
                }
    		System.out.println("Please wait while your sequences are aligned");               
                Exception throwable= new Exception();
                for(int i=0; i<8; i++)    
                    try{
                      	String nFasta=Gene.alignFasta(homolog, true);
    			String pFasta=Gene.alignFasta(homolog, false);
                        String[] fastas = {nFasta,pFasta}; 			
    			if(fastas!=null)
                            return fastas;
                    } catch(Exception exe){throwable=exe;}
      //          JOptionPane.showMessageDialog(null, "Error: Unable to align fasta sequences. Please try again later.", "Error", JOptionPane.ERROR_MESSAGE); 
                throw throwable;
            }
    
    public int getNumHomolgs(){
        return numHomologs;
    }
    
    private String getSpeciesName(){
        new NCBIFinder().waitComplete();
        return null;
    }

    
    private String getHomologNucFasta(){
        return null;
    }
    
    private String getHomologProtFasta(){
        return null;
    }
}
    
    

