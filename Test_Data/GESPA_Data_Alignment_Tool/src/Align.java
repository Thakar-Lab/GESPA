/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package aligndata;

import java.nio.file.*;
import java.nio.charset.Charset;
import java.io.BufferedReader;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.*;

/**
 *
 * @author Jay Khurana
 */
public class Align extends javax.swing.JFrame {

    /**
     * Creates new form Align
     */
    public Align() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        currentProgLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(689, 417));
        setResizable(false);

        jButton1.setText("Batch Text");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("Cons. Report");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setText("Gene Info Panel");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setText("Align Datasets");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("GESPA Data Alignment Tool");

        currentProgLabel.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE)
                                .addGap(18, 18, 18)
                                .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, 141, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(currentProgLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 265, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addGap(18, 18, 18)
                        .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, 118, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, 118, Short.MAX_VALUE)
                        .addGap(80, 80, 80)))
                .addGap(53, 53, 53))
            .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 253, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(currentProgLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
                    .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
                    .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
                    .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        String text=retrieveText();        
        if(text!=null)
            source1=text;
        else
            return;
        int index1=0;
        int index2=source1.indexOf("\r");  
        while(true){
            List<String> tempList=new ArrayList<>();               
            String data="\t"+source1.substring(index1,index2);
            int index3=1;
            while(true){
                if(index3!=1)
                    index3=index3+data.substring(index3+1).indexOf("\t")+2;
                int index4=index3+data.substring(index3).indexOf("\t");
                boolean willBreak=false;
                if(index3==-1){
                    System.out.println();
                }
                if(data.substring(index3).indexOf("\t")==-1){
                    willBreak=true;
                    index4=data.length();
                }
                tempList.add(data.substring(index3,index4));                
                if(willBreak)
                    break;                
                if(index3==1)
                    index3=2;
            }
            s1List.add(tempList);
            tempList=null;
            index1=index2+1;
            index2=index1+source1.substring(index1).indexOf("\r");            
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        String text=retrieveText();        
        if(text!=null)
            source2=text;
        else
            return;
        int index1=0;
        int index2=source2.indexOf("\r");  
        while(true){
            List<String> tempList=new ArrayList<>();               
            String data="\t"+source2.substring(index1,index2);
            int index3=1;
            while(true){
                if(data.indexOf("FALSE")!=-1)
                    break;                 
                if(index3!=1)
                    index3=index3+data.substring(index3+1).indexOf("\t")+2;
                int index4=index3+data.substring(index3).indexOf("\t");
                boolean willBreak=false;
                if(index3==-1){
                    System.out.println();
                }
                if(data.substring(index3).indexOf("\t")==-1){
                    willBreak=true;
                    index4=data.length();
                }
                tempList.add(data.substring(index3,index4));                
                if(willBreak)
                    break;                
                if(index3==1)
                    index3=2;
            }
            if(!tempList.isEmpty())
                s2List.add(tempList);
            tempList=null;
            index1=index2+1;
            index2=index1+source2.substring(index1).indexOf("\r");         
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        String text=retrieveText();        
        if(text!=null)
            source3=text;
        else
            return;
        int index1=0;
        int index2=source3.indexOf("\r");  
        while(true){
            List<String> tempList=new ArrayList<>();               
            String data="\t"+source3.substring(index1,index2);
            int index3=1;
            while(true){               
                if(index3!=1)
                    index3=index3+data.substring(index3+1).indexOf("\t")+2;
                int index4=index3+data.substring(index3).indexOf("\t");
                boolean willBreak=false;
                if(index3==-1){
                    System.out.println();
                }
                if(data.substring(index3).indexOf("\t")==-1){
                    willBreak=true;
                    index4=data.length();
                }
                tempList.add(data.substring(index3,index4));                
                if(willBreak)
                    break;                
                if(index3==1)
                    index3=2;
            }
            s3List.add(tempList);
            tempList=null;
            index1=index2+1;
            index2=index1+source3.substring(index1).indexOf("\r");         
        }        
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        List<List<String>> temp1 = new ArrayList<>();
        List<List<String>> temp2 = new ArrayList<>();
        List<List<String>> temp3 = new ArrayList<>(); 
        List<List<String>> masterList = new ArrayList<>();
        currentProgLabel.setText("Aligning data. Please Wait.");
        for(int i=0; i<s1List.size();i++){
            temp1.add(s1List.get(i));
        }
        for(List<String> s: s2List){
            temp2.add(s);
        }
        for(List<String> s: s3List){
            temp3.add(s);
        }
        int removed1=0, removed2=0, removed3=0;
        for(int i=0; i<temp1.size();i++){
           List<String> masterTemp = new ArrayList<>();            
           if(temp1.get(i).size()==4)
               continue;
           String geneName=temp1.get(i).get(0);
           String SNP=temp1.get(i).get(1);
           for(int j=0; j<temp2.size();j++){
               if(temp2.get(j).get(1).indexOf(geneName)==-1)
                   continue;
               if(temp2.get(j).get(1).indexOf(SNP)==-1)
                   continue;
               for(String s: temp1.get(i)){
                   masterTemp.add(s);
               }
               for(String s:temp2.get(j)){
                   masterTemp.add(s);
               }
               for(int k=0; k<temp3.size();k++){
                   if(temp3.get(k).get(0).indexOf(geneName)==-1)
                        continue;
                   if(temp3.get(k).get(0).indexOf(SNP)==-1)
                        continue;
                   for(String s:temp3.get(k)){
                        masterTemp.add(s);
                   }
                   temp3.remove(k);
                   break;
               }
               temp2.remove(j);
               masterList.add(masterTemp);
               temp1.remove(i);
               i--;               
               break;
           }
        }
        for(List<String> s: masterList){
            String line="";
            for(String s2: s){
                line+=s2+"\t";
            }
            alignedText+=line+"\n";
        }
        jTextArea1.setText(alignedText);
        alignedText="";
        currentProgLabel.setText("");
        setResizable(true);
    }//GEN-LAST:event_jButton4ActionPerformed
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Align.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Align.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Align.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Align.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Align().setVisible(true);
            }
        });
    }
    
    public static String retrieveText(){
        currentProgLabel.setText("Reading file. Please wait.");
        JFileChooser selectBatch = new JFileChooser();
        FileFilter textOnly = new FileNameExtensionFilter("Text file", "txt");
        selectBatch.setFileFilter(textOnly);
        int fileChosen=selectBatch.showDialog(null,"Open File");
        String fileText="";
        if(fileChosen==1){
            currentProgLabel.setText("");
            return null;
        }
        BufferedReader fileReader; 
        Charset charset = Charset.forName("Cp1252");
        try{    
           fileReader = Files.newBufferedReader(selectBatch.getSelectedFile().toPath(), charset);
        }catch(IOException exe){
            JOptionPane.showMessageDialog(null,"Could not find file at specified"
                    + " location!","Warning",JOptionPane.WARNING_MESSAGE);
            return null;
        }
        String line;
        try{    
            while ((line = fileReader.readLine()) != null) {
                fileText+=(line+"\r");
            }
        }catch(IOException exe){ 
            currentProgLabel.setText("Error reading file. Please check file.");            
            JOptionPane.showMessageDialog(null,"Could not read from file at "
                    + "selected location!","Warning",JOptionPane.WARNING_MESSAGE);        
        }
        currentProgLabel.setText("");         
        return fileText;
    }
    
    private static String source1;
    private static String source2;
    private static String source3;
    private static String alignedText="";
    List<List<String>> s1List = new ArrayList<>(); 
    List<List<String>> s2List = new ArrayList<>(); 
    List<List<String>> s3List = new ArrayList<>();     
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private static javax.swing.JLabel currentProgLabel;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    // End of variables declaration//GEN-END:variables
}
