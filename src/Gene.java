/*GESPA  Copyright (C) 2013  Jay Khurana
 * See GESPA License for additional information.
 */
package DNAFinderMain;

import java.io.IOException;
import java.util.*;
import javax.swing.JOptionPane;
import java.util.logging.Level;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.IncorrectnessListener; 

/**
 *
 * @author Jay Khurana
 */
public class Gene {

    private String name;
    private String nucleoID;
    private String protID;
    private String fasta;
    private String protFasta;
    private Framework f;
    private String paralogText;
    private int numParalogs;
    private int attempts;
    private String[] aminoAcids;
    
    public Gene(Framework f){
        this.f=f;
        aminoAcids= new String[]{ "A","R","N","D","C","Q","E","G","H","I","L",
            "K","M","F","P","S","T","W","Y","V"}; 
        numParalogs=0;
    }
    
    public Gene(String name, String nucleoID, String protID, String fasta, String protFasta) {
        this.name=name;
    	this.nucleoID=nucleoID;
    	this.protID=protID;
    	this.fasta=fasta;
    	this.protFasta=protFasta;
        this.f=f;
    }
    
    public void nullGene(){
        name=null;
        nucleoID=null;
        protID=null;
        fasta=null;
        protFasta=null;
        paralogText=null;
        aminoAcids=null;
    }
    
    public void setName(String n){
    	name = n;
    }
    
    public void setNucleoID(String nID){
    	nucleoID=nID;
    }
    
    public void setProtID(String pID){
    	protID=pID;
    }
    
    public void setFasta(String f){
    	fasta=f;
    }
    
    public void setProtFasta(String pF){
    	protFasta = pF;
    }
    
    public String getName(){
    	return name;
    }
    
    public String getNucleoID(){
    	return nucleoID;
    }
    
    public String getProtID(){
    	return protID;
    }
    
    public String getFasta(){
    	return fasta;
    }
    
    public String getProtFasta(){
    	return protFasta;
    }
    
    public int getNumParalogs(){
        if(numParalogs>1)
            return numParalogs;
        else return 0;
               
    }
    
   @Override
   public String toString(){
       return "Gene:"+getName()+"\n"+"Nucleotide ID:" + getNucleoID()+"\n"+"Protein ID:" + getProtID()+"\n"+getFasta()+"\n"+getProtFasta();
   }
    
   //Aligns fastas using clustalw 
   public static String alignFasta(Gene[] genes, boolean DNA) throws Exception{
	    String fasta ="";
	    for(Gene g:genes){
	    	if(g!=null){
	    		if(DNA){
	    			fasta=fasta+g.getFasta();                                
                        }else{
	    			fasta=fasta+g.getProtFasta();
                        }
                }       
            }
          WebClient webClient = new WebClient(BrowserVersion.CHROME);
          webClient.getOptions().setTimeout(900000);
		HtmlPage page1 = webClient.getPage("http://www.genome.jp/tools/clustalw/");
		HtmlForm form = page1.getFormByName("form_region");
		HtmlSubmitInput button = form.getInputByName("");
		HtmlTextArea textField = form.getTextAreaByName("sequence");
		if(DNA){
			List<HtmlRadioButtonInput> radio = form.getRadioButtonsByName("type");
			int i=0;
			for(HtmlRadioButtonInput h:radio){
				if(i==0){
					h.setChecked(false);
					i++;
				} else {
					h.setChecked(true);
				}
			}
			HtmlSelect select = (HtmlSelect) page1.getElementByName("matrix");
			List<HtmlOption> options = select.getOptions();
			for(HtmlOption o:options){
				if(o.asText().equals("BLOSUM (for PROTEIN)")){				
					o.setSelected(false);
				}else if(o.asText().equals("IUB (for DNA)"))
					o.setSelected(true);
			}
			HtmlSelect select2 = (HtmlSelect) page1.getElementByName("pwmatrix");
			List<HtmlOption> options2 = select2.getOptions();
			for(HtmlOption o:options2){
				if(o.asText().equals("BLOSUM (for PROTEIN)"))
					o.setSelected(false);
				else if(o.asText().equals("IUB (for DNA)"))
					o.setSelected(true);						
			}		
		}
		textField.setText(fasta);
		HtmlPage page2 = button.click();
		webClient.closeAllWindows();  	     	    
		String s=page2.asXml();
		s=s.substring(s.indexOf("CLUSTAL 2.1 multiple sequence alignment")+40);
                s=s.substring(0,s.indexOf("<a name=\"clustalw.dnd\"/>"));
                s=s.trim();
		return s;
	}
	
   //Gets paralogous genes
   public String[] getFamily() throws Exception{
                initializeSimilarParalog();
                int totalCounter=0;
		Gene[] family=new Gene[100];
                family[0]=f.getGene();
                Gene[] families;
                String [] tempUrl=new String[50];
		int subCounter=1;
                java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);  
                java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
                class NoLogs implements IncorrectnessListener {
			@Override
			public void notify(String arg0, Object arg1) {}
		}		
		WebClient webClient = new WebClient(BrowserVersion.CHROME);
                webClient.getOptions().setTimeout(250000);
                webClient.setIncorrectnessListener(new NoLogs());
		HtmlPage page1;
		try{
			page1 = webClient.getPage("http://genome.ucsc.edu/cgi-bin/hgBlat?command=start");
		}catch(Exception exe){
			return null;
		}
		HtmlForm form = page1.getFormByName("mainForm");
		HtmlTextArea textField = form.getTextAreaByName("userSeq");
		HtmlSubmitInput button = form.getInputByName("Submit");
		textField.setText(f.getGene().getFasta());
			HtmlPage page2=null;		
		try{
			page2 = button.click();
		}catch(Exception exe){}
		String webContent=page2.asXml().substring(page2.asXml().indexOf("SPAN"));
		int index1=webContent.indexOf("<a href")+11+1;
		int index2=webContent.indexOf("browser")+1;
		HtmlPage page3=null;
		NCBIFinder finder = new NCBIFinder();
                //System.out.print("Obtaining Links and Fastas");
                int subSubCounter=0;
                String[] url = new String[100];
                familyFinder:
                for(int i=1; i<8;i++){

			try{
                                index1=webContent.indexOf("<a href",index1+i+1);
                                index1=webContent.indexOf("<a href",index1+i+2);
                                index2=index1+webContent.substring(index1).indexOf("browser");
                                tempUrl[i]=webContent.substring(index1+11,index2).trim();
                                tempUrl[i]="http://genome.ucsc.edu"+tempUrl[i].substring(0,tempUrl[i].indexOf(">"));
				page3 = webClient.getPage(tempUrl[i]);
			}catch(Exception exe){
				//System.out.println("URL "+ tempUrl[i] + " is unreachable!");
			}
                        boolean loop=true;
			String webContent2="";
                        try{
                            webContent2=page3.asXml().substring(page3.asXml().indexOf("RefSeq"),page3.asXml().indexOf("RefSeq")+5000);
                        }catch(StringIndexOutOfBoundsException exe){
                            loop=false;
                        }catch(NullPointerException exe){
                            break;
                        }
                        int counter=0; int index3=0,index4=0;
                        currentParalog:
                        while(loop){
                            counter++;
                            try{
                                index3=webContent2.indexOf("href=",index3+1+counter);
                                index4=index3+webContent2.substring(index3).indexOf(" ");
                            }catch(Exception exe){
                                break;
                            }
                           try{ 
                                url[i] = "http://genome.ucsc.edu"+webContent2.substring(index3+8,index4-1);
                           }catch(Exception exe){
                               break;
                           } 
                            if(url[i].indexOf("Tracks")>-1)
                                break;
                            else{
                                HtmlPage page4=null;
                                try{
                                    page4 = webClient.getPage(url[i]);
                                }catch(Exception exe){
                                    break;
                                }
                                String webContents3=page4.asXml();
                                String nucleoID;
                                try{       
                                       nucleoID=webContents3.substring(webContents3.indexOf("RefSeq Summary")+webContents3.substring(webContents3.indexOf("RefSeq Summary")).indexOf("(")+1,webContents3.indexOf("RefSeq Summary")+webContents3.substring(webContents3.indexOf("RefSeq Summary")).indexOf(")"));
                                }catch(Exception exe){
                                    break;
                                }
                                 URLReader reader = new URLReader();
                                 try{   
                                   page4=webClient.getPage(url[i]);
                                   webContents3=page4.asXml();
                                 }catch(Exception exe){
                                     //System.out.println("URL could not be found");
                                 }
                                String protFasta="";
                                String url2="";
                                try{   
                                     String sub = webContents3.substring(webContents3.indexOf("Protein (")-250, webContents3.indexOf("Protein ("));
                                     int stop1, stop2;
                                     stop1=sub.indexOf("/cgi");
                                     String subSub=sub.substring(stop1);
                                     stop2=stop1+subSub.indexOf("\"");
                                    url2="http://genome.ucsc.edu"+sub.substring(stop1,stop2);
                                    url2=url2.replaceAll("amp;hgg_do","hgg_do");
                                 }catch(Exception exe){
                                     protFasta="";
                                     //System.out.println("No Protein coding region exists for this related gene!");
                                 }
                            try{
                            try{    
                                page4=webClient.getPage(url2);
                                String p=page4.asXml();
                                p=p.substring(p.indexOf("length")+7,p.indexOf("</"));
                                for(int k=0; k<15;k++){
                                    String t=p.substring(k,k+1);
                                if(!(t.equals("1")||t.equals("2")||t.equals("3")||t.equals("4")||t.equals("5")||t.equals("6")||t.equals("7")||t.equals("8")||t.equals("9")||t.equals("0"))){
                                    p=p.substring(k,p.length());
                                    break;
                                }
                            }
                            protFasta=p;
                        }catch(Exception exe){
                            //System.out.println("No protein coding region for related gene!");
                            protFasta="";
                        }    
                            
                            boolean repeat=false;
                            String finderName=finder.getName(nucleoID).toLowerCase();
                            for(Gene g:family){
                                if(g!=null){
                                    if(g.getNucleoID().equals(nucleoID)||g.getName().toLowerCase().equals(finderName)){
                                        repeat=true;
                                        break;
                                    }
                                }
                            }
                            if(!protFasta.equals("")&&!repeat){        
                                totalCounter++;
                                String name=finder.getName(nucleoID).toLowerCase();
                                for(Gene g: family)
                                    if(g!=null&&g.getName().equals(name))
                                        break;
                                if((totalCounter<15&&!JFrameMain.askForParalog)||JFrameMain.askForParalog){
                                    String nameCheck=name.replace('|',' ');
                                    if(nameCheck.indexOf(" ")!=-1)
                                        nameCheck=nameCheck.substring(0,nameCheck.indexOf(" "));
                                    if(nameCheck.trim().toLowerCase().equals(
                                            f.getGene().getName().toLowerCase()))
                                        break currentParalog;
                                    if(JFrameMain.askForParalog){
                                        Object[] paralogOpt = {"Yes", "No","Stop adding paralogs"};
                                        int opt = JOptionPane.showOptionDialog(null, "Would you like to add the paralogous gene " +name.replace('|', ' ')+" to "+f.getGene().getName()+"?", "Add Paralog", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, paralogOpt, paralogOpt);
                                        if(opt==1)
                                            break currentParalog;
                                        else if(opt==2)
                                            break familyFinder;
                                    }else{
                                        try{
                                            if(!similarParalog(name.replace('|',' ')))
                                                break currentParalog;
                                        }catch(Exception exe){
                                            break currentParalog;
                                        }
                                    }    
                                        protFasta=protFasta.trim();
                                        family[subCounter]=new Gene(name,nucleoID,null,finder.nucleoFinder(nucleoID),protFasta);
                                        family[subCounter].setFasta(">"+name+"\n"+family[subCounter].getFasta()+"\n");
                                        family[subCounter].setProtFasta(">"+name+"\n"+family[subCounter].getProtFasta()+"\n");                                    
                                        //System.out.println(family[subCounter]);
                                        subCounter++;                                        
                                }else{
                                    break familyFinder;
                                }
                            }
                                }catch (Exception exe){
                                    //System.out.print("No prot");
                                    break;
                                }
                            }
                        }			
		}
      		//System.out.println("Please wait while familial sequences are aligned");
    		int counter3=0; 
                int counterRegular=0;
                int[] counterIndex=new int[100];
                webClient.closeAllWindows(); 
                try{
                        for(Gene g: family){
                            if(g!=null){
                               numParalogs++;
                            }
                        }
                        String nFasta=Gene.alignFasta(family, true);
    			String pFasta=Gene.alignFasta(family, false);
                        String[] fastas = {nFasta,pFasta}; 			
    			return fastas;
    		} catch(Exception exe){
    			//System.out.print("Unable to align fastas");
                        return null;  
    		}  	
    }
    
   //Converts DNA to amino acid sequence
   public String convertToAmino(String DNA){
		DNA=DNA.substring(0,DNA.length()-DNA.length()%3);
                boolean converted = false;
		String aminoChain="";
		int start=0; int end = 3;
		while(!converted){
			if(end>DNA.length()){
				converted=true;
				break;
			}
			String temp=DNA.substring(start,end);
			temp=temp.toUpperCase();
			start+=3; end+=3;
			String tempTemp =temp.substring(0,2);
			if(tempTemp.equals("CT"))
				aminoChain +="L";
			else if(tempTemp.equals("GT"))
				aminoChain+="V";
			else if(tempTemp.equals("TC"))
				aminoChain+="S";
			else if(tempTemp.equals("CC"))
				aminoChain+="P";
			else if(tempTemp.equals("AC"))
				aminoChain+="T";
			else if(tempTemp.equals("GC"))
				aminoChain+="A";
			else if(tempTemp.equals("CG"))
				aminoChain+="R";
			else if(tempTemp.equals("GG"))
				aminoChain+="G";
			else if(temp.equals("ATT")||temp.equals("ATC")||temp.equals("ATA"))
				aminoChain+="I";
			else if(temp.equals("TAT")||temp.equals("TAC"))
				aminoChain+="Y";
			else if(temp.equals("TTA")||temp.equals("TTG"))
				aminoChain+="L";	
			else if(temp.equals("CAC")||temp.equals("CAT"))
				aminoChain+="H";
			else if(temp.equals("CAA")||temp.equals("CAG"))
				aminoChain+="Q";
			else if(temp.equals("AAT")||temp.equals("AAC"))
				aminoChain+="N";	
			else if(temp.equals("AAA")||temp.equals("AAG"))
				aminoChain+="K";	
			else if(temp.equals("GAT")||temp.equals("GAC"))
				aminoChain+="D";	
			else if(temp.equals("GAA")||temp.equals("GAG"))
				aminoChain+="E";
			else if(temp.equals("TGT")||temp.equals("TGC"))
				aminoChain+="C";	
			else if(temp.equals("AGT")||temp.equals("AGC"))
				aminoChain+="S";	
			else if(temp.equals("AGA")||temp.equals("AGG"))
				aminoChain+="R";
			else if(temp.equals("TTT")||temp.equals("TTC"))
				aminoChain+="F";	
			else if(temp.equals("TGG"))
				aminoChain+="W"	;
			else if(temp.equals("ATG"))
				aminoChain+="M";//start codon?	
			else if(temp.equals("TGA")||temp.equals("TAA")||temp.equals("TAG"))
				aminoChain+="Stop";																																										
		}
		return aminoChain;
	}
	        
   //Converts single amino acid to possible DNA codon s
   public String[] convertToDNACodon(String amino){
		boolean converted = false;
		String[] DNACodon=new String[6];		
                String tempTemp =amino.substring(0,1);
                if(tempTemp.equals("I")){
                    DNACodon[0]="ATT";
                    DNACodon[1]="ATC";
                    DNACodon[2]="ATA";
                }else if(tempTemp.equals("L")){
                    DNACodon[0]="CTT";
                    DNACodon[1]="CTG";
                    DNACodon[2]="TTA";
                    DNACodon[3]="TTG";                    
                    DNACodon[4]="CTC";
                    DNACodon[5]="CTA";
                }else if(tempTemp.equals("V")){
                    DNACodon[0]="GTT"; 
                    DNACodon[1]= "GTC";
                    DNACodon[2]= "GTA";
                    DNACodon[3]="GTG";
                }else if(tempTemp.equals("F")){
                    DNACodon[0]= "TTT";
                    DNACodon[1]= "TTC";
                }else if(tempTemp.equals("M")){
                    DNACodon[0]= "ATG";
                }else if(tempTemp.equals("C")){
                    DNACodon[0]= "TGT";
                    DNACodon[1]= "TGC";
                }else if(tempTemp.equals("A")){
                    DNACodon[0]= "GCT";
                    DNACodon[1]= "GCC";
                    DNACodon[2]= "GCA";
                    DNACodon[3]= "GCG";
                }else if(tempTemp.equals("G")){
                    DNACodon[0]= "GGT"; 
                    DNACodon[1]= "GGC"; 
                    DNACodon[2]= "GGA"; 
                    DNACodon[3]= "GGG";
                }else if(tempTemp.equals("P")){
                    DNACodon[0]=  "CCG";                    
                    DNACodon[1]=  "CCT";
                    DNACodon[2]=  "CCA";
                    DNACodon[3]=  "CCC";
                }else if(tempTemp.equals("T")){
                    DNACodon[0]=  "ACA";
                    DNACodon[1]=  "ACG";
                    DNACodon[2]=  "ACT";
                    DNACodon[3]=  "ACC";
                }else if(tempTemp.equals("S")){
                    DNACodon[0]=  "AGC";
                    DNACodon[1]=  "TCA";
                    DNACodon[2]=  "TCT";
                    DNACodon[3]=  "TCC";
                    DNACodon[4]=  "AGT";
                    DNACodon[5]=  "TCG";
                }else if(tempTemp.equals("Y")){
                    DNACodon[0]=  "TAT";
                    DNACodon[1]=  "TAC";
                }else if(tempTemp.equals("W")){
                    DNACodon[0]=  "TGG";
                }else if(tempTemp.equals("Q")){
                    DNACodon[0]=  "CAA";
                    DNACodon[1]=  "CAG";
                }else if(tempTemp.equals("N")){
                    DNACodon[0]= "AAT"; 
                    DNACodon[1]= "AAC";
                }else if(tempTemp.equals("H")){
                    DNACodon[0]= "CAT"; 
                    DNACodon[1]= "CAC";
                }else if(tempTemp.equals("E")){
                    DNACodon[0]= "GAA"; 
                    DNACodon[1]= "GAG";
                }else if(tempTemp.equals("D")){
                    DNACodon[0]= "GAT"; 
                    DNACodon[1]= "GAC";
                }else if(tempTemp.equals("K")){
                    DNACodon[0]= "AAA"; 
                    DNACodon[1]= "AAG";
                }else if(tempTemp.equals("R")){
                    DNACodon[0]= "AGA";
                    DNACodon[1]= "CGC"; 
                    DNACodon[2]= "CGT"; 
                    DNACodon[3]= "CGG";
                    DNACodon[4]= "CGA"; 
                    DNACodon[5]= "AGG"; 
                }else if(tempTemp.equals("*")){
                    DNACodon[0]="TAA";
                    DNACodon[1]="TGA";
                    DNACodon[2]="TAG";
                }
                int logicalSize=0;
                for(int i=0; i<6; i++){
                    if(DNACodon[i]!=null){
                        logicalSize++;
                    }else
                        break;
                }
                if(logicalSize!=0){      
                       String[] tempDNACodon=new String[logicalSize];
                       for(int j=0;j<logicalSize;j++){
                           tempDNACodon[j]=DNACodon[j];
                       }
                       DNACodon=tempDNACodon;                
                }
                return DNACodon;
        }
        
   //Gets conservation of nucleotide/amino acid in gene
   public String findRelatedSNP(String aliFasta, int loc){
                boolean PSIC=false,PSICReplace=false;
                String PSICChange="";
                if(aliFasta.startsWith("PSIC")){
                    PSIC=true;
                    aliFasta=aliFasta.substring(4,aliFasta.length());
                }
                if(aliFasta.startsWith("Rep")){
                    PSICReplace=true;
                    PSICChange=aliFasta.substring(3,4).toUpperCase();
                    aliFasta=aliFasta.substring(4);
                }
                String report ="";
                aliFasta=aliFasta+ "                                                                                                   ";
                int length=f.getGene().getName().length();
		int index = aliFasta.indexOf(f.getGene().getName()+" ");
                String inCheck=aliFasta.substring(index+length,index+length+1);
                //while(!(aliFasta.substring(index+length,index+length+1).equals(" ")))
                //    index = aliFasta.indexOf(f.getGene().getName().toUpperCase(),index+length+1);
		int snpLoc=loc;
		int i=0;
		String nAtLoc ="";
		int totalCount=1;
		String[] reportString = new String[50];
		while(aliFasta.substring(index+length+i,index+length+1+i).equals(" ")){
			i++;
		}
		i+=length;
                int subCounter=0;
                int counter=0;
                main:
		while(index!=-1){
			String row=aliFasta.substring(index+i,index+i+aliFasta.substring(index+i).indexOf("\n"));
			int blankNums=0;
			String nucleotide2="";
			String nucleotide1="";
			String name = "";
			row=aliFasta.substring(index+i,index+i+aliFasta.substring(index+i).indexOf("\n")); 
			for(int j=0; j<row.length(); j++){
				if(!(row.substring(j,j+1).equals("-"))){
                                        counter++;
					subCounter++;
                                        if(counter==loc){  
                                            counter=j;
						j=1;
						nAtLoc=row.substring(counter,counter+1);
                                                if(PSICReplace)
                                                    nAtLoc=PSICChange;
						int rowTemp2=row.indexOf(" ");
						int rowTemp1=row.indexOf("\n");
						if(rowTemp1==rowTemp2&&rowTemp2==-1){
                                                    int p=row.length();
                                                    rowTemp2=row.length()+1;
						}
						reportString[0]=f.getGene().getName() + " " + nAtLoc;
						while(j>0){
                                                    if(aliFasta.substring(index+i+counter+(rowTemp2+i)*j, index+i+(rowTemp2+i)*j+counter+1).equals(" ")||aliFasta.substring(index+i+counter+(rowTemp2+i)*j, index+i+(rowTemp2+i)*j+counter+1).equals(".")||aliFasta.substring(index+i+counter+(rowTemp2+i)*j, index+i+(rowTemp2+i)*j+counter+1).equals("*")||aliFasta.substring(index+i+counter+(rowTemp2+i)*j, index+i+(rowTemp2+i)*j+counter+1).equals(":")){
							j=1;
							break;
                                		    }
					       	    nucleotide2 = aliFasta.substring(index+i+counter+(rowTemp2+i)*j,index+i+counter+(rowTemp2+i)*j+1);
					     	    name = aliFasta.substring(index+rowTemp2*j+i*j);//.indexOf(" "));
					  	    name = name.substring(0,name.indexOf(" "));
					 	    totalCount++;
					 	    reportString[totalCount-1]=name+" "+nucleotide2;
					 	    j++;
                                                }
						while(j>0){
                                                            try{
								if(aliFasta.substring(index+i+counter-rowTemp2*j-i*j, index+i+counter-rowTemp2*j-i*j+1).equals(" ")||aliFasta.substring(index+i+counter-rowTemp2*j-i*j, index+i+counter-rowTemp2*j-i*j+1).equals("*")||aliFasta.substring(index+i+counter-rowTemp2*j-i*j, index+i+counter-rowTemp2*j-i*j+1).equals("\n")||aliFasta.substring(index+i+counter-rowTemp2*j-i*j, index+i+counter-rowTemp2*j-i*j+1).equals(".")||aliFasta.substring(index+i+counter-rowTemp2*j-i*j, index+i+counter-rowTemp2*j-i*j+1).equals(":")){
									j=0;
									break;
								}
							}catch(Exception exe){
								break;
							}
							nucleotide1 = aliFasta.substring(index+i+counter-rowTemp2*j-i*j, index+i+counter-rowTemp2*j-i*j+1);
							try{
                                                            name = aliFasta.substring(index-rowTemp2*j-i*j);
                                                        }catch(StringIndexOutOfBoundsException exe){
                                                            System.out.println();
                                                        }
                                                        name=name.substring(0,name.indexOf(" "));
							totalCount++;
							reportString[totalCount-1]=name+" "+nucleotide1;
							j++;
						}
						break main;
					}
					if(j==row.length()-1){
						index=aliFasta.indexOf(f.getGene().getName()+" ", index+1);
                                                String y=aliFasta.substring(index+length,index+length+1);
                                                while(!(aliFasta.substring(index+length,index+length+1).equals(" ")))
                                                    index = aliFasta.indexOf(f.getGene().getName()+" ",index+length+1);
						j=-1;
                                                try{    
                                                    row=aliFasta.substring(index+i,index+i+aliFasta.substring(index+i).indexOf("\n"));
                                                }catch(Exception exe){
                                                    System.out.println();
                                                }
					}
				}
			}
			//int j=1;			
			index=aliFasta.indexOf(f.getGene().getName()+" ", index+1);
		}
		if(!PSIC)
                    return generateReport(reportString);
                List<String> temp=new ArrayList<>();
                for(String s: reportString){
                    if(s!=null)
                        temp.add(s.substring(s.length()-1));
                    else break;
                }
                return temp.toString();                    
                
	}
        
   //Starting point for calculation of PSIC algorithim
   public double PSIC(String paraFasta, String homoFasta, int loc, String replacement){
            int PSIC1, PSIC2;
            double replacementScore;
            if(paraFasta.indexOf(f.getGene().name)==-1)
                    paraFasta=null;
            List<String> aminosAtLoc= new ArrayList<>();
            String temp;
            if(paraFasta!=null){    
                paraFasta="PSIC"+paraFasta;
            }
            homoFasta="PSIC"+homoFasta;
            if(paraFasta!=null){
                temp=findRelatedSNP(paraFasta,loc);
                int repeatLoc=0;
                int counter=0;
                int index1=1;
                int index2=temp.indexOf(",");
                while(true){
                    if(counter!=0)
                        aminosAtLoc.add(temp.substring(index1,index2).trim());
                    counter++;
                    index1+=temp.substring(index1).indexOf(",")+1;
                    if(temp.substring(index2+1).indexOf(",")!=-1){
                        index2+=temp.substring(index2+1).indexOf(",")+1;
                    }else{
                        aminosAtLoc.add(temp.substring(index1,
                                temp.indexOf("]")).trim());
                        break;
                    }
                }               
            }
            temp=findRelatedSNP(homoFasta, loc);
            int counter=0;
            int index1=1;
            int index2=temp.indexOf(",");
            int realLoc=aminosAtLoc.size();
            while(true){
                aminosAtLoc.add(temp.substring(index1,index2).trim());
                index1+=temp.substring(index1).indexOf(",")+1;
                if(temp.substring(index2+1).indexOf(",")!=-1){
                    index2+=temp.substring(index2+1).indexOf(",")+1;
                }else{
                    aminosAtLoc.add(temp.substring(index1,
                        temp.indexOf("]")).trim());
                    break;
                }
            }
            int maxObs=0;
            for(int i=0;i<aminosAtLoc.size(); i++)
                if(aminosAtLoc.get(i).equals(aminosAtLoc.get(realLoc)))
                    maxObs++;
            List<String> distinct= new ArrayList<>();
            distinct.add(aminosAtLoc.get(realLoc));
            List<List<Integer>> distinctPos=getDistinct(distinct, aminosAtLoc);
            double aminoProb=aminoProb(distinct.get(0));
            if(distinctPos.size()==1)
                return Math.log(1/aminoProb)-calcEffObsRepl(1.0,this.aminoProb(replacement),1-aminoProb);
            double noAminoProb=1;
            for(int i=0; i<distinctPos.size();i++){
                if(aminosAtLoc.get(distinctPos.get(i).get(0)).equals(replacement)){
                    noAminoProb=i*-1;
                    break;
                }else
                    if(aminosAtLoc.get(distinctPos.get(i).get(0)).equals("-"))
                        continue;
                    else
                        noAminoProb-=aminoProb(aminosAtLoc.get(distinctPos.get(i).get(0)));           
            }
            return getProbability(paraFasta, homoFasta, distinctPos,noAminoProb,aminoProb(replacement),aminoProb);
        }
        
   //Calculated effect observations if conservation = 100% for PSIC algorithim
   public double calcEffObsRepl(double effObsSum, double aminoProb, double notObsAminoProb){
            return Math.log((0.3/(effObsSum+0.3)*aminoProb/notObsAminoProb)/aminoProb);
        }
        
   public List<List<Integer>> getDistinct(List<String> distinct, List<String> aminosAtLoc){
            outer:
            for(int i=0; i<aminosAtLoc.size(); i++){
                for(int j=0; j<distinct.size();j++){
                    if(aminosAtLoc.get(i).equals(distinct.get(j)))
                        continue outer;
                }
                distinct.add(aminosAtLoc.get(i));
            }
            List<List<Integer>> matchingSeqPos= new ArrayList<>();
            for(int i=0; i<distinct.size();i++){
                List<Integer> tempMatching=new ArrayList<>();
                for(int j=0;j<aminosAtLoc.size();j++)
                    if(aminosAtLoc.get(j).equals(distinct.get(i)))
                         tempMatching.add(j);
                matchingSeqPos.add(tempMatching);
            }
            return matchingSeqPos;
        }
        
  //Solves for p(a,j)/summation p(a,n) in PSIC algorithim
   public double getProbability(String paraFasta,String homoFasta,
                List<List<Integer>> distinctPos,
                double noAminoProb,double aminoSubsProb,double aminoRegProb){
            boolean aminoInAlignment=false;
            double effObsAtSubs=0;
            if(noAminoProb<0){
                noAminoProb*=-1;
                aminoInAlignment=true;
            }            
            double effObsAtPos=0;
            double effObsSum=0;
            double obsAminoProb=0;
            if(homoFasta.startsWith("Rep", 4)){
                homoFasta=homoFasta.substring(0,4)+homoFasta.substring(8);
                if(paraFasta!=null)
                    paraFasta=paraFasta.substring(0,4)+paraFasta.substring(8);
            }    
            int numPos=f.getGene().protFasta.substring(f.getGene().protFasta.indexOf("\n")).trim().length()+1;
            for(int i=0; i<distinctPos.size();i++){
                if(distinctPos.get(i).size()==1){
                    if(i==0)
                        effObsAtPos=1;
                    else if(aminoInAlignment&&i==(int)noAminoProb){
                        effObsAtSubs=1;
                        effObsSum+=1;
                    }else{
                        effObsSum+=1;
                    }
                    continue;
                }
                double obs1=0;
                List<String> aminosAtLoc=new ArrayList<>();
                int gapCounter=0;
                String amino="";
                posCounter:
                for(int k=1; k<numPos;k++){
                    aminosAtLoc=new ArrayList<>();
                    if(paraFasta!=null){
                        String temp=findRelatedSNP(paraFasta,k);
                        int counter=0;
                        int index1=1;
                        int index2=temp.indexOf(",");
                        int realLoc=0;
                        while(true){
                           if(counter!=0){
                                aminosAtLoc.add(temp.substring(index1,index2).trim());
                                if(aminosAtLoc.get(aminosAtLoc.size()-1).equals("-"))
                                    continue posCounter;                                  
                           }
                           counter++;                         
                           index1+=temp.substring(index1).indexOf(",")+1;
                           if(temp.substring(index2+1).indexOf(",")!=-1){
                               index2+=temp.substring(index2+1).indexOf(",")+1;
                           }else{
                             aminosAtLoc.add(temp.substring(index1,
                             temp.indexOf("]")).trim());
                             break;
                           }
                        }
                    }
                    String temp=findRelatedSNP(homoFasta,k);
                    int realLoc=aminosAtLoc.size();
                    int counter=0;
                    int index1=1;
                    int index2=temp.indexOf(",");            
                    while(true){
                        aminosAtLoc.add(temp.substring(index1,index2).trim());
                        if(aminosAtLoc.get(aminosAtLoc.size()-1).equals("-")){
                            gapCounter++;
                            continue posCounter;
                        }
                        index1+=temp.substring(index1).indexOf(",")+1;
                        if(temp.substring(index2+1).indexOf(",")!=-1){
                            index2+=temp.substring(index2+1).indexOf(",")+1;
                        }else{
                            aminosAtLoc.add(temp.substring(index1,
                                temp.indexOf("]")).trim());
                            break;
                        }
                    }
                    List<String> tempAminoAtLoc= new ArrayList<>();
                    for(int j=0; j<distinctPos.get(i).size();j++){
                       // try{
                            tempAminoAtLoc.add(aminosAtLoc.get(distinctPos.get(i).get(j)));
                       // }catch(Exception exe){
                       //     continue;
                       // }
                        if(j>0&&!(tempAminoAtLoc.get(j).equals(tempAminoAtLoc.get(j-1))))
                            break;
                        if(j==distinctPos.get(i).size()-1)
                            obs1++;
                    }
                    aminosAtLoc=new ArrayList<>();
                }
                obs1=obs1/(numPos-2-gapCounter);
                if(i==0){
                    effObsAtPos=solvePSIC(obs1,1.0,distinctPos.get(i).size());
                    effObsSum+=effObsAtPos;
                }else if(aminoInAlignment&&i==(int)noAminoProb){
                    effObsAtSubs=solvePSIC(obs1,1.0,distinctPos.get(i).size());
                    effObsSum+=effObsAtSubs;
                }else
                    effObsSum+=solvePSIC(obs1,1.0,distinctPos.get(i).size());
            } 
            double PSICScore1=Math.log(effObsAtPos/effObsSum/aminoRegProb);
            double PSICScoreSubs;
            if(!aminoInAlignment)
                PSICScoreSubs=Math.log((0.3/(effObsSum+0.3)*aminoSubsProb/noAminoProb)/aminoSubsProb);
            else
                PSICScoreSubs=Math.log((effObsAtSubs/(effObsSum)/aminoSubsProb));                
            return Math.abs(PSICScore1-PSICScoreSubs);
        }
        
   //Recursively solves psic algorithim for p(a,j)
   private double solvePSIC(double solution1, double numPosStart, double numPosEnd){
            double solution2=0;
            for(String s:aminoAcids){
                solution2+=Math.pow(aminoProb(s),(numPosStart+numPosEnd)/2);
            }
            if(Math.abs(solution2-solution1)<1e-4)
                return (numPosStart+numPosEnd)/2;
            else if(Math.abs(numPosStart-numPosEnd)<1e-6){
                return (numPosStart+numPosEnd)/2;
            }else if(solution2>solution1)
                return solvePSIC(solution1,(numPosStart+numPosEnd)/2, numPosEnd);
            else{
                return solvePSIC(solution1,numPosStart, (numPosStart+numPosEnd)/2);
            }
        }
        
   //Returns probability of amino acid occuring in human genome at given position    
   private double aminoProb(String amino){
            switch(amino.toUpperCase()){
                case "A":
                    return .085786;
                case "R":
                    return .045676;
                case "N":
                    return .047306;
                case "D":
                    return .058022;
                case "C":
                    return .018036;
                case "Q":
                    return .037722;
                case "E":
                    return .059724;
                case "G":
                    return .081155;
                case "H":
                    return .021639;
                case "I":
                    return .052944;
                case "L":
                    return .081155;
                case "K":
                    return .058717;
                case "M":
                    return .021109;
                case "F":
                    return .039944;
                case "P":
                    return .048178;
                case "S":
                    return .063047;
                case "T":
                    return .060835;
                case "W":
                    return .014256;
                case "Y":
                    return .036310;
                case "V":
                    return .068436;
            }
            return 1;
        }
        
   //Generates string report for relatedSNP method
   private String generateReport(String [] s){
		int logSize=0;
		String report ="";
		for(int i=0; i<s.length; i++){
			if(s[i]==null){
				double matches=1, notMatches=i-1;
				int[] notMIndex=new int[i];
				for(int k=1; k<i; k++){
					if(s[k].substring(s[k].length()-1).equals(s[0].substring(s[0].length()-1))){
						matches++;
						notMatches--;
					}else{
						notMIndex[k]=k;
					}
				}
				matches=matches/(i);
				notMatches=notMatches/(i);
				if(matches>0.89)
					report=s[0].substring(s[0].length()-1) + " in " + s[0].substring(0,s[0].indexOf(" ")) +" is completely conserved."+"("+(int)(matches*100)+"%)";
				else if(matches>.77){
					report=s[0].substring(s[0].length()-1) + " in " + s[0].substring(0,s[0].indexOf(" ")) +" is well conserved."+"("+(int)(matches*100)+"%)";
				}
				else if(matches>.70){
					report=s[0].substring(s[0].length()-1) + " in " + s[0].substring(0,s[0].indexOf(" ")) +" is semi-conserved."+"("+(int)(matches*100)+"%)";
				}
				else if(matches>.34){
					report=s[0].substring(s[0].length()-1) + " in " + s[0].substring(0,s[0].indexOf(" ")) +" is poorly conserved."+"("+(int)(matches*100)+"%)";
				} else {
					report=s[0].substring(s[0].length()-1) + " in " + s[0].substring(0,s[0].indexOf(" ")) +" is not conserved."+"("+(int)(matches*100)+"%)";
				}
				boolean scanN=false;		
				for(int k: notMIndex){
					if(k!=0){
						scanN=true;
						break;
					}
				}
				if(scanN){
					report = report + "\n      The following nucleotides were not conserved:\n"; 
					for(int k: notMIndex){
						if(k!=0)	
							if(!(s[k].substring(s[k].length()-1).equals("-")))
								report=report+"      "+s[k].substring(s[k].length()-1)+" in " +s[k].substring(0,s[k].indexOf(" "))+" instead of " + s[0].substring(s[0].length()-1)+".\n";
							else
								report = report + "      Deletion or lack of insertion in " + s[k].substring(0,s[k].indexOf(" "))+".\n";
					}
				}
				break;	
			}
		}
		return report;
	}
        
   @Deprecated
   private boolean similarParalogName(String name){
            String geneName=f.getGene().getName().toLowerCase();
            if(name.substring(0,name.indexOf(" ")).equals(geneName))
                return false;
            int limit=0;
            int noMatchCounter=0;
            if(name.length()<=geneName.length())
                limit=name.length();
            else
                limit=geneName.length();
            for(int i=0; i<limit;i++)
                if(!name.substring(i,i+1).equals(geneName.substring(i,i+1)))
                    noMatchCounter++;
            return noMatchCounter<3;
          
        }
        
   /*Determines similarity if paralogs based on e value before added. Replaces
    * similarParalogName method
    */
   private boolean similarParalog(String name){
            if(paralogText==null)
                return false;
            if(name.indexOf(" ")!=-1)
                name=name.substring(0,name.indexOf(" "));
            name=name.toLowerCase().trim();
            if(paralogText.indexOf(name)==-1)
                return false;
            String score=paralogText.substring(paralogText.indexOf(name));
            int delimiter=score.indexOf("row ");
            if(score.indexOf("row ")!=-1)
                score=score.substring(0,delimiter);
            if(score.indexOf(">0<")!=-1)
                return true;
            return false;
        }
        
   //Initializes page of similar Paralogs
   private void initializeSimilarParalog(){
            String geneName=f.getGene().getName().toLowerCase();
            java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);  
            java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
            class NoLogs implements IncorrectnessListener {
        	@Override
		public void notify(String arg0, Object arg1) {}
		}		
       	    WebClient webClient = new WebClient(BrowserVersion.CHROME);
	    webClient.setIncorrectnessListener(new NoLogs());
            HtmlPage page1;
            try{
                page1=webClient.getPage("http://genome.ucsc.edu/cgi-bin/hgNear");
            }catch(IOException | FailingHttpStatusCodeException exe){
                webClient.closeAllWindows();
                return;
            }
            try{
                HtmlSelect searchOption=(HtmlSelect)page1.getElementByName("near_order");
                HtmlOption blastP=searchOption.getOption(2);
                searchOption.setSelectedAttribute(blastP, true);
                HtmlForm form= page1.getFormByName("mainForm");
                HtmlInput input = form.getInputByName("near_search");
                input.setValueAttribute(geneName);
                HtmlSubmitInput go = form.getInputByName("submit");
                HtmlPage page2=go.click();
                String webContents=page2.asXml();
                if(webContents.indexOf("Simple Search Results")!=-1){
                    String link=webContents.substring(webContents.indexOf(
                                "     "+geneName.toUpperCase())-250,
                            webContents.indexOf("    "+geneName.toUpperCase())-1);
                    int startIndex=0;
                    int tempIndex=link.indexOf("/cgi");
                    String tempLink=link;
                    while(true){
                        int tempIndex2=tempIndex;
                        tempLink=link.substring(tempIndex+1);
                        tempIndex=tempLink.indexOf("/cgi");
                        startIndex=tempIndex2+tempIndex+1;
                        if(tempIndex==-1)
                            break;
                   }
                    link=link.substring(startIndex);
                    link="http://genome.ucsc.edu"+link.substring(0,link.indexOf("\""));
                    link=link.substring(link.indexOf("search=")+7);
                    input.setValueAttribute(link);
                    page2=go.click();                            
                }
                URLReader h=new URLReader();
                String s=page2.getUrl().toString();
                h.findStringinURL(page2.getUrl().toString(), "a");
                paralogText=h.getHTML().toLowerCase();
                webClient.closeAllWindows();
            }catch(Exception exe){
                webClient.closeAllWindows();
            }
        }
        
   //Adds sequence length numbers to clustalw alignment
   public String addAlignmentNumber(String alignment){
            alignment+="                                    ";
            String newAlignment=alignment;
            int numtypes=0;
            String[] names=new String[500];
            int index=0;
            for(int i=0; i<500;i++){
                String row=alignment.substring(index,index+alignment.substring(index).indexOf("\n"));
                if(row.indexOf("*")!=-1||row.indexOf(":")!=-1||row.indexOf(".")!=-1||row.indexOf("                        ")!=-1)
                    break;
                names[i]=row.substring(0,row.indexOf(" ")+1);
                index=alignment.indexOf(row)+row.length()+1;
            }
            for(int i=0; names[i]!=null;i++){
                int namesIndex=alignment.indexOf(names[i])+names[i].length();
                int counter=0;                
                while(true){
                   String row=alignment.substring(namesIndex);                    
                   String rowCheck=alignment.substring(namesIndex-names[i].length());
                   int newLineIndex=rowCheck.indexOf("\n");
                   boolean noNewLine=false;
                   if(newLineIndex!=-1)
                       rowCheck=rowCheck.substring(0,newLineIndex);
                   else
                       noNewLine=true;
                   int spaces=0;
                   for(int j=names[i].length(); j<50; j++){
                       try{
                            if(row.substring(j,j+1).equals(" "))
                                spaces++;
                            else
                               break;
                       }catch(Exception exe){
                           break;
                       }
                   }
                   if(!noNewLine)
                        try{
                            row=row.substring(spaces,row.indexOf("\n"));
                        } catch(Exception exe){
                            break;
                        }
                   else
                       try{
                        row=row.substring(spaces,spaces+row.substring(spaces).indexOf("             "));
                       }catch(Exception exe){
                           System.out.println(5);
                       }
                   String rowTemp=row.trim();
                   if(noNewLine)
                       rowCheck=rowCheck.substring(0,rowCheck.indexOf("                  ")).trim();                   
                   boolean blankRow=true;
                   for(int j=0; j<row.length(); j++)
                       if(!(row.substring(j,j+1).equals(" ")||row.substring(j,j+1).equals("-"))){
                           blankRow=false;
                           break;
                       }
                   if(!blankRow)    
                        for(int j=0; j<rowTemp.length(); j++)
                            if(!(rowTemp.substring(j,j+1).equals("-")))
                                counter++;
                   try{ 
                        String nameTemp=names[i].substring(0,names[i].length()-1);
                   }catch(Exception exe){}
                   if(!blankRow)
                        newAlignment=newAlignment.substring(0,newAlignment.indexOf(rowCheck)+names[i].length()+spaces)+row.substring(0,row.length())+"  "+counter+newAlignment.substring(newAlignment.indexOf(rowCheck)+rowCheck.length());
                   int newPosition=alignment.substring(namesIndex).indexOf(names[i]);
                   if(newPosition==-1||newPosition==0)
                       break;
                   String s=alignment.substring(newPosition);
                   namesIndex=namesIndex+newPosition+names[i].length();
                }
            }            
            return newAlignment.replace('|', ' ');
        }
        
   //Searches blat for amino acid and returns genome browser 
   public String getBrowserURL(String aaChain){
                java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);  
                java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
                class NoLogs implements IncorrectnessListener {
			@Override
			public void notify(String arg0, Object arg1) {}
		}		
		WebClient webClient = new WebClient(BrowserVersion.CHROME);
		webClient.setIncorrectnessListener(new NoLogs());
		HtmlPage page1;
		try{
			page1 = webClient.getPage("http://genome.ucsc.edu/cgi-bin/hgBlat?command=start");
		}catch(Exception exe){
                    webClient.closeAllWindows();
                    return null;
		}
		HtmlForm form = page1.getFormByName("mainForm");
		HtmlTextArea textField = form.getTextAreaByName("userSeq");
		HtmlSubmitInput button = form.getInputByName("Submit");
		textField.setText(aaChain);
        	HtmlPage page2;		
		try{
                    page2 = button.click();
		}catch(Exception exe){
                    webClient.closeAllWindows();
                    return null;
                }
                String url=page2.asXml();
                if(url.indexOf("Sorry, no matches")!=-1){
                    webClient.closeAllWindows();
                    return null;
                }
                try{    
                    url=url.substring(url.indexOf("cgi-bin/hgTracks?position="));
                }catch(StringIndexOutOfBoundsException exe){
                    webClient.closeAllWindows();
                    return null;
                }
                url="http://genome.ucsc.edu/"+url.substring(0,url.indexOf("\""));
                webClient.closeAllWindows();
                return url;
        }
}