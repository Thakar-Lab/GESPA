/*GESPA  Copyright (C) 2013  Jay Khurana
 * See GESPA License for additional information.
 */
package DNAFinderMain;


/**
 *
 * @author Jay Khurana
 */

public class NCBIFinder{
    
   private URLReader entrezReader;
   
   public NCBIFinder(){
        entrezReader=new URLReader();
    }
    
   public String nucleoFinder(String id) throws Exception{     
      waitComplete();
      try{  
          for(int i=0; i<10;i++){
            try{
              entrezReader.findStringinURL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=nuccore&id="+id+"&rettype=fasta&retmode=xml", "");
            }catch(Exception exe){
                if(i==9)
                    throw exe;
                waitComplete();
                continue;
            }
            break;
          }
      }catch(Exception exe){
          throw exe;
      }
      String nucleoSeq=entrezReader.getHTML();
      nucleoSeq=nucleoSeq.substring(nucleoSeq.indexOf("TSeq_sequence>")+14);
      return nucleoSeq.substring(0,nucleoSeq.indexOf("</TSeq_sequence>"));
   }
   
   public String protFinder(String id) throws Exception{
      waitComplete();
      try{  
         for(int i=0; i<10;i++){  
            try{
                entrezReader.findStringinURL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=nuccore&id="+id+"&rettype=fasta&retmode=xml", "");
            }catch(Exception exe){
                if(i==9)
                    throw exe;
                waitComplete();
                continue;
            }
            break;
         }          
      }catch(Exception exe){
          throw exe;
      }
      String protSeq=entrezReader.getHTML();
      protSeq=protSeq.substring(protSeq.indexOf("TSeq_sequence>")+14);
      return protSeq.substring(0,protSeq.indexOf("</TSeq_sequence>"));                
   }

   public String getName(String id) throws Exception{
      waitComplete();
      try{  
          for(int i=0; i<10;i++){
            try{
              entrezReader.findStringinURL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=nuccore&id="+id+"&rettype=fasta&retmode=xml", "");
            }catch(Exception exe){
                if(i==9)
                    throw exe;
                waitComplete();
                continue;
            }
            break;
          }
      }catch(Exception exe){
          throw exe;
      }
      String rs=entrezReader.getHTML();
      String name=rs.substring(rs.indexOf("(")+1,rs.indexOf(")"))+"|";      
      if(rs.indexOf("transcript variant")!=-1){
          name+=("var|"+rs.substring(rs.indexOf("transcript variant")+19,
              rs.indexOf("transcript variant")
              +rs.substring(rs.indexOf("transcript variant")).indexOf(",")));
      }
      return name;
       /*RichSequence rs = null;
      GenbankRichSequenceDB grsdb = new GenbankRichSequenceDB();
      try{
            rs = grsdb.getRichSequence(id);
            String name=rs.getDescription().substring(rs.getDescription().indexOf("(")+1,rs.getDescription().indexOf(")"))+"|";
            String name2=rs.getDescription();
            if(rs.getDescription().indexOf("transcript variant")!=-1){
                name+=("var|"+rs.getDescription().substring(rs.getDescription().indexOf("transcript variant")+19,rs.getDescription().indexOf("transcript variant")+rs.getDescription().substring(rs.getDescription().indexOf("transcript variant")).indexOf(",")));
            }
            if(name.length()>19){
                name=name.substring(0,19);
            }
            return name;
      }catch(BioException be){
            be.printStackTrace();
      }
      return "";*/
   }
   
   public void waitComplete(){
        try{
            Thread.sleep(350);
        }catch(InterruptedException exe){}
        catch(Exception exe){
            throw exe;
        }
    }
   

}
