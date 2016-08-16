/*GESPA  Copyright (C) 2013  Jay Khurana
 * See GESPA License for additional information.
 */
package DNAFinderMain;

import java.util.logging.Level;
import java.util.*;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.IncorrectnessListener; 

/**
 *
 * @author Jay Khurana
 */
public class SNP {
    private String phenotype;
    private String protChange;
    private String nucleoChangeCodon;
    private String infoURL;
    private int nucleoLoc;
    private int protLoc;
    private int attempts;
    private boolean multipleConfirmed;
    private Framework f;
    private String x;
    
    public SNP(Framework f){this.f=f;}
    
    public SNP(String p,String pC,String nC, int pr,int n, boolean m){
        phenotype=p;
        protChange=pC;
        nucleoChangeCodon=nC;
        protLoc=pr;
        nucleoLoc=n;
        multipleConfirmed=m;
        this.f=f;
    }
    
    public void setPhenotype(String p){
        phenotype=p;
    }

    public void setNucleoLoc(int n){
        nucleoLoc=n;
    }
    
    public void setInfoURL(String url){
        infoURL=url;
    }
    
    public void setProtLoc(int p){
        protLoc=p;
    }
    
    public void setProtChange(String p){
        protChange=p;
    }

    public void setNucleoChangeCodon(String n){
        nucleoChangeCodon=n;
    }
    
    public void setMultipleConfirmed(boolean b){
        multipleConfirmed = b;
    }
    
    public void setAttempts(int i){
        attempts=i;
    }
    
    public String getPhenotype(){
        return phenotype;
    }
    
    public String getInfoUrl(){
       return infoURL;
    }

    public int getNucleoLoc(){
        return nucleoLoc;
    }

    public int getProtLoc(){
        return protLoc;
    }
    
    public String getProtChange(){
        return protChange;
    }
    
    public String getNucleoChangeCodon(){
        return nucleoChangeCodon;
    }
    
    public boolean getMultipleConfirmed(){
        return multipleConfirmed;
    }
    
    @Deprecated
    public SNP[] getAllSNPs() throws Exception{
       java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);  
       java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
       class NoLogs implements IncorrectnessListener {
			@Override
			public void notify(String arg0, Object arg1) {}
       }
       String clinVarHTML="";
       boolean loop=true;
       String clinVarURL="http://www.ncbi.nlm.nih.gov/clinvar/?term="+f.getGene().getName()+"[gene]";
       WebClient webClient = new WebClient();
       HtmlPage page1;
       IncorrectnessListener a=webClient.getIncorrectnessListener();
       webClient.setIncorrectnessListener(new NoLogs());
       webClient.getOptions().setJavaScriptEnabled(false);
       try{    
           page1=webClient.getPage(clinVarURL);
       }catch(Exception exe){
           webClient.closeAllWindows();
           return null;
       }
       HtmlPage page2=page1;
       String p;
      try{
       try{     
            p=page1.asXml().substring(page1.asXml().indexOf("1 to ")+11,page1.asXml().indexOf("1 to ")+page1.asXml().substring(page1.asXml().indexOf("1 to ")).indexOf("<")).trim();
       }catch(StringIndexOutOfBoundsException exe){
           try{     
                p=page1.asXml().substring(page1.asXml().indexOf("Results: ")+9,page1.asXml().indexOf("Results: ")+page1.asXml().substring(page1.asXml().indexOf("Results: ")).indexOf("<")).trim();
           }catch(StringIndexOutOfBoundsException exe2){
               return null;
           }
       }
       int numResults=Integer.parseInt(p);
       int numResultsOrigin=numResults;
       SNP[] SNP = new SNP[numResults];
       snpLoop:       
       while(loop){
          String y=page2.asXml();
          x+=page2.asXml();
          List<HtmlAnchor> c=page2.getAnchors();
          for(int j=0; j<10; j++)    
              if(c.isEmpty())
                    c=page2.getAnchors();
              else
                  break;
          if(numResults<=20){
               //System.out.print("Last SNP reached");
               break;
          }
          if(c.isEmpty()){
                if(attempts<5)
                      throw new Exception();
                System.out.println("Unable to obtain all SNPs. Please try again later");
                f.setSaveToCloud(false);
                //JOptionPane.showMessageDialog(null, "Unable to obtain all SNPs. 
                //To obtain all clinical SNPs please try again later.", 
                //"Error", JOptionPane.ERROR_MESSAGE);
                break;
          }
          for(int q=0; q<c.size();q++)   {      
              String s=c.get(q).getTextContent();
              if(s.equals("Next >")){
                  try{    
                     numResults-=20;
                     page2=c.get(q).click();
                     break;
                  }catch(Exception exe){
                     break;
                  }
              }
              if(q==c.size()-1){
                  break snpLoop;
              }
         }
    }
    int index2=0;
    boolean notPathogenic=false;
    snpLoop:
    for(int i=0;i<numResultsOrigin;i++){
       index2=x.indexOf("classified by ",index2+1)+14;
       String s=x.substring(index2,index2+200);
       boolean multiple=false;
       if(s.indexOf("multiple")!=-1)
           multiple=true;
       int index3=0;
       try{ 
        index3=index2-950+x.substring(index2-950).indexOf(" c.");
       }catch(StringIndexOutOfBoundsException exe){
           numResultsOrigin--;
           continue;
         //  System.out.println();
       }
       String o=x.substring(index3);
       if(index3>index2){
           int tempCounter=1;
           while(index3>index2){
               //Problem AREA
               try{
                   index2=x.indexOf("classified by ",index2+1)+14;                   
                   numResultsOrigin--;
                   if(i>numResultsOrigin)
                       break snpLoop;
                   // index2=x.indexOf("classified by ",index2+1)+14;
//                    numResultsOrigin--;
               }catch(Exception exe){
                   numResultsOrigin--;
                   continue;
               } 
//              tempCounter++;
           }
       }
       try{
            s=x.substring(index3+1+x.substring(index3+1).indexOf("c."),index2+100);
       }catch(StringIndexOutOfBoundsException exe){
           numResultsOrigin--;
           continue;
       }
       s=" "+s;
       String url=x.substring(index3-50,index2);
       int counter=0;
       while(url.indexOf("/clinvar/")==-1){
           index3-=50;
           url=x.substring(index3,index2);
           counter++;
       }
       if(counter>50)
          url=null;
       else
           url="http://www.ncbi.nlm.nih.gov"+url.substring(url.indexOf("/clinvar/"),
                   url.indexOf("/clinvar/")+url.substring(url.indexOf("/clinvar/")).indexOf("\""));          
       int subIndex=s.indexOf(" ");
       String[] names = new String[3];
       for(int j=0; j<s.length();j++){
           String subSub=s.substring(subIndex,subIndex+1);
           if(!(subSub.equals(" ")||subSub.equals("\n"))){
               subIndex++;
               subSub=s.substring(subIndex,subIndex+1);
               String subSub2=s.substring(subIndex-1,subIndex+1);
               if(!(subSub.equals("/")||subSub2.equals("td")||subSub.equals("b")||subSub.equals("d")||subSub.equals("\r"))){
                   String temp=s.substring(subIndex-1,subIndex-1+s.substring(subIndex-1).indexOf("\r"));
                   if(temp.indexOf("NM_")!=-1){
                       break;
                   }
                   if(temp.indexOf(">")!=-1)
                       continue;
                   if((subSub2.equals("c."))||((temp.indexOf(".")==-1&&temp.indexOf(":")==-1||temp.indexOf("0")==-1))){
                     if(names[0]==null){
                         names[0]=temp;
                         subIndex+=names[0].length()+1;
                     }else if(names[1]==null){
                         try{
                            names[1]=temp;
                         }catch(Exception exe){
                             names[1]=temp+"...";
                         }
                         subIndex+=100;
                         subIndex=s.indexOf("t",subIndex+1);
                         break;
                     }
                   }else{
                       subIndex+=90;
                   }
               }else{
                   subIndex+=8;
               }
           }
           subIndex++;
       }
      
       if(!notPathogenic){
           try{ 
               if(names[0].indexOf("X")!=-1||names[0].indexOf("B")!=-1||
                       names[0].indexOf("Z")!=-1||names[0].indexOf("J")!=-1)
                   notPathogenic=true;
               else{
                  // :c.2794A&gt;C (p.Met932Leu)
                   SNP[i]=new SNP(null);
                   
                   int clinProtLoc=Integer.parseInt(names[0].substring(
                           names[0].indexOf("p.")+5,names[0].length()-4));
                   int clinNucleoLoc=Integer.parseInt(names[0].substring(
                           names[0].indexOf(".")+1,names[0].indexOf("&")-1));
                   
                   String aminoAtLoc=null;
                   try{
                        aminoAtLoc=Framework.aminoConvert
                           (names[0].substring(
                           names[0].indexOf("p.")+2,
                           names[0].indexOf("p.")+5));
                   }catch(Exception exe){}
                   String pChange=Framework.aminoConvert
                           (names[0].substring(names[0].length()-4,names[0].length()-1));
                   String nChange=names[0].substring(names[0].indexOf(";")+1,
                           names[0].indexOf(";")+2);
                   if(nChange==null||pChange==null||pChange.equals(aminoAtLoc))
                       throw new Exception();                   
                   SNP[i].setProtLoc(clinProtLoc);
                   SNP[i].setNucleoLoc(clinNucleoLoc);
                   SNP[i].setProtChange(pChange);
                   SNP[i].setPhenotype(names[1]);
                   SNP[i].setInfoURL(url);
                   SNP[i].setMultipleConfirmed(multiple);
               }
           }catch(Exception exe){
               notPathogenic=true;
           }
       }
       if(notPathogenic){
               i--;
               numResultsOrigin--;
               notPathogenic=false;
       }
   }                                          
    webClient.closeAllWindows();
    return SNP;
   }finally{
       webClient.closeAllWindows();
   }
}
    
    //Gets SNPs on clinvar for a gene determined through current Framework
    public SNP[] getAllSNPsNew() throws Exception{
        List<SNP> SNP = new ArrayList<>();
        URLReader SNPRead = new URLReader();
        //waitComplete();
        SNPRead.findStringinURL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=clinvar&term="
                + f.getGene().getName()+"[gene]&count=1080&RetMax=20&RetStart=0"
                + "&QueryKey=1"
                + "&WebEnv=NCID_1_36649974_130.14.18.34_9001_1386348760_356908530&usehistory=y","");
        x = SNPRead.getHTML();
        if(x.indexOf("Message>No items found.</Output")>0)
            return null;
        String queryKey=x.substring(x.indexOf("Key>")+4,x.indexOf("</Quer"));
        String webEnv=x.substring(x.indexOf("Env>")+4,x.indexOf("</Web"));
        //waitComplete();
        SNPRead.findStringinURL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=clinvar"
                + "&query_key="+queryKey
                + "&WebEnv="+webEnv, "");
        x=SNPRead.getHTML();
        int index1=x.indexOf(f.getGene().getName()+"):");
        String temp4=x.substring(index1-10,index1+500);
        while(true){
            String[] names = new String[3];
            if(x.substring(index1,index1+100).indexOf("c.")<0)
                names[0]=null;
            else{
                int testTrait=x.substring(index1).indexOf("trait_name");
                if(testTrait<0)
                    testTrait=x.length();
                names[0]=x.substring(index1-100,index1+testTrait+100).trim();
            }
            names[1]=x.substring(index1+4,index1+x.substring(index1).indexOf("<"));
            boolean noID=false;
            try{ 
                try{
                    int refOneIndex=index1+1+x.substring(index1).indexOf(">RCV"), refOneEnd=-1;
                    if(x.substring(refOneIndex,refOneIndex+100).contains("rcv"))
                        refOneEnd=refOneIndex+x.substring(refOneIndex).indexOf("<");
                    names[2]=x.substring(refOneIndex,refOneEnd);
                    if(x.substring(index1+10,refOneIndex).contains(f.getGene().getName())){
                        throw new Exception();
                    }
                }catch(Exception exe){
                    try{
                        int idStart=index1-150+x.substring(index1-150).indexOf("uid=")+5;
                        int idEnd=idStart+x.substring(idStart).indexOf("\"");
                        names[2]="variation/"+x.substring(idStart,idEnd);
                    }catch(Exception exe2){
                        names[2]="";
                        noID=true;
                    }
                }
                //names[2]=x.substring(index1-250).substring((x.substring(index1-250).indexOf("sion>")+5), x.substring(index1-250).indexOf("</acce")).trim();               
                boolean multiple=false;
                if(x.substring(index1,index1+3000).indexOf("by multiple")>0)
                    multiple=true;
                if(!names[1].contains("c.")||!names[1].contains("p."))
                    throw new Exception();
                else
                    names[1]=names[1].substring(names[1].indexOf("c."));
                if(names[1].indexOf("X")!=-1||names[1].indexOf("B")!=-1||
                       names[1].indexOf("Z")!=-1||names[1].indexOf("J")!=-1)
                   break;//names[0]=null;
               else{
                    int clinProtLoc=Integer.parseInt(names[1].substring(
                           names[1].indexOf("p.")+5,names[1].length()-4));
                   int clinNucleoLoc=Integer.parseInt(names[1].substring(
                           names[1].indexOf(".")+1,names[1].indexOf("&")-1));
                   
                   String aminoAtLoc=null;
                   try{
                        aminoAtLoc=Framework.aminoConvert
                           (names[1].substring(
                           names[1].indexOf("p.")+2,
                           names[1].indexOf("p.")+5));
                   }catch(Exception exe){}
                   String pChange=Framework.aminoConvert
                           (names[1].substring(names[1].length()-4,names[1].length()-1));
                   String nChange=names[1].substring(names[1].indexOf(";")+1,
                           names[1].indexOf(";")+2);
                   if(nChange==null||pChange==null||pChange.equals(aminoAtLoc))
                       throw new Exception();                   
                   SNP temp = new SNP(null);
                   temp.setProtLoc(clinProtLoc);
                   temp.setNucleoLoc(clinNucleoLoc);
                   temp.setProtChange(pChange);
                   String trait;
                   try{
                       int indexT=names[0].indexOf("trait_name>")+11;
                       trait=names[0].substring(indexT,indexT+names[0].substring(indexT).indexOf("<"));
                   }catch(Exception exe){
                       throw exe;
                   }
                   temp.setPhenotype(trait);
                   if(!noID)
                        temp.setInfoURL("http://www.ncbi.nlm.nih.gov/clinvar/"+names[2]+"/");
                   else
                       temp.setInfoURL(null);
                   temp.setMultipleConfirmed(multiple);
                   SNP.add(temp);
               }
           }catch(Exception exe){
               int index1Check=index1+500+x.substring(index1+501).indexOf(f.getGene().getName()+"):");
               if(index1Check<index1+500)
                   break;
               index1=index1Check;
               continue;
           }         
               int index1Check=index1+500+x.substring(index1+501).indexOf(f.getGene().getName()+"):");
               if(index1Check<index1+500)
                   break;
               index1=index1Check;
        }
        return SNP.toArray(new SNP[SNP.size()]); 
    }
    
    private void waitComplete(){
        try{
            Thread.sleep(350);
        }catch(InterruptedException exe){}
        catch(Exception exe){
            throw exe;
        }        
    }
        
}