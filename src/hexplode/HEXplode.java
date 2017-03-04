/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hexplode;

import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import setgame.Game;
import textfile.WriteFile;

/**
 *
 * @author Marco
 */
public class HEXplode extends javax.swing.JFrame {
    double zoomSpeed = 0.95;
    
    int size = 2;
    boolean test = true;
    String path = System.getProperty("user.dir") + "\\Results.txt";
    String dbPath = System.getProperty("user.dir") + "\\datas.db";
    Graph board = genGraph(size);
    GameTools gt = new GameTools(dbPath);
    static IntList shortMove = new IntArrayList();
    static IntList cutMove = new IntArrayList();

    /**
     * Creates new form HEXplode
     */
    public HEXplode() {
        initComponents();
        pathField.setText(path);
    }
    
    public static int point(int i, int j, int size) {
        return i*size + j;
    }
    
    public Graph genGraph(int dim) {
        //generate the graph
        Graph<Integer, DefaultEdge> g = new SimpleGraph(DefaultEdge.class);
        for(int i = 0; i < dim; i++) {
            for(int j = 0; j < dim; j++) {
                int id = point(i, j, dim);
                g.addVertex(id);
//                g.getNode(id).addAttribute("ui.label", id);
            }
        }
        //add the two edges of the board
        g.addVertex(point(dim, 0, dim));
        g.addVertex(point(dim, 1, dim));
        //add the edges
        for(int i = 0; i < dim; i++) {
            for(int j = 0; j < dim; j++) {
                if(i == 0) g.addEdge(point(i, j, dim), point(dim, 0, dim));
                if(i == dim - 1) g.addEdge(point(i, j, dim), point(dim, 1, dim));
                if(i < dim - 1) g.addEdge(point(i, j, dim), point(i+1, j, dim));
                if(j < dim - 1) g.addEdge(point(i, j, dim), point(i, j+1, dim));
                if(i < dim - 1 && j < dim - 1) g.addEdge(point(i, j, dim), point(i+1, j+1, dim));
            }
        }
        return g;
    }
    
    public void displayGraph(Graph g) {
        JFrame f = new JFrame();
//        f.setLayout(new GridBagLayout());
        JGraphXAdapter<Integer, DefaultEdge> jgx = new JGraphXAdapter(g);
        jgx.getModel().beginUpdate();
        try {
            jgx.clearSelection(); 
            jgx.selectAll();
            Object[] cells = jgx.getSelectionCells();
            for (Object c : cells) {
                mxCell cell = (mxCell) c;
                mxGeometry geo = cell.getGeometry();
                if (cell.isVertex()) {
                    cell.setConnectable(false);
                    geo.setWidth(40);
                    geo.setHeight(40);
                }
                else{
                    cell.setStyle("orthogonalEdgeStyle");
                }
            }
            jgx.clearSelection(); 
        }
        finally
        {
            jgx.getModel().endUpdate();
        }
        
        mxGraphComponent mgc = new mxGraphComponent(jgx);
        mgc.getGraph().getStylesheet().getDefaultEdgeStyle().put(mxConstants.STYLE_NOLABEL, "1");
        mgc.getGraph().setAllowDanglingEdges(false);
        mgc.getGraph().setAllowLoops(false);
        mgc.getGraph().setCellsBendable(false);
        mgc.getGraph().setCellsCloneable(false);
        mgc.getGraph().setCellsDeletable(false);
        mgc.getGraph().setCellsDisconnectable(false);
        mgc.getGraph().setCellsEditable(false);
        mgc.getGraph().setCellsResizable(false);
        mgc.getGraph().setConnectableEdges(false);
        mgc.getGraph().setDropEnabled(false);
        mgc.getGraph().setSplitEnabled(false);
        
        MouseWheelListener[] listeners = mgc.getMouseWheelListeners();
        for (MouseWheelListener mwl : listeners) {
            mgc.removeMouseWheelListener(mwl);
        }
        mgc.addMouseWheelListener((MouseWheelEvent e) -> {
            if(e.isControlDown()) {
                e.getPreciseWheelRotation();
                mgc.setZoomFactor(mgc.getZoomFactor() * Math.pow(zoomSpeed, e.getPreciseWheelRotation()));
                mgc.zoomTo(mgc.getZoomFactor(), true);
                e.consume();
            }
            else {
                
            }
        });
        for (MouseWheelListener mwl : listeners) {
            mgc.addMouseWheelListener(mwl);
        }
        
        mxFastOrganicLayout layout = new mxFastOrganicLayout(jgx);
        layout.setMaxIterations(1000);
        layout.execute(jgx.getDefaultParent());
        
        f.add(mgc);
        f.setTitle("Hex board " + size + "x" + size);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.pack();
        f.setExtendedState(JFrame.MAXIMIZED_BOTH);
        f.setVisible(true);
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fc = new javax.swing.JFileChooser();
        bg = new javax.swing.ButtonGroup();
        stratLabel = new javax.swing.JLabel();
        pathField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();
        test1 = new javax.swing.JRadioButton();
        test2 = new javax.swing.JRadioButton();
        dimLabel = new javax.swing.JLabel();
        spinner = new javax.swing.JSpinner();
        showButton = new javax.swing.JButton();
        runButton = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("HEXplode - Hex solver");
        setResizable(false);

        stratLabel.setText("File with test results:");

        pathField.setEditable(false);

        browseButton.setText("Browse");
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        bg.add(test1);
        test1.setSelected(true);
        test1.setText("Test wether the Hex board with the central hexagon occupied is positive or not");
        test1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                test1ActionPerformed(evt);
            }
        });

        bg.add(test2);
        test2.setText("Find the Short strategy on the Hex board with the central hexagon occupied");
        test2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                test2ActionPerformed(evt);
            }
        });

        dimLabel.setText("Board dimension:");

        spinner.setModel(new javax.swing.SpinnerNumberModel(2, 2, null, 1));
        spinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerStateChanged(evt);
            }
        });

        showButton.setText("Show the graph");
        showButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showButtonActionPerformed(evt);
            }
        });

        runButton.setText("Run!");
        runButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runButtonActionPerformed(evt);
            }
        });

        jButton1.setText("Clear file");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(test1)
                            .addComponent(test2))
                        .addGap(0, 94, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(stratLabel)
                            .addComponent(dimLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pathField)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(spinner, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(showButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(browseButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton1)))))
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(188, 188, 188)
                .addComponent(runButton)
                .addGap(188, 188, 188))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(stratLabel)
                    .addComponent(pathField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseButton)
                    .addComponent(jButton1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dimLabel)
                    .addComponent(spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(showButton))
                .addGap(18, 18, 18)
                .addComponent(test1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(test2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(runButton)
                .addGap(16, 16, 16))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void test1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_test1ActionPerformed
        test = true;
    }//GEN-LAST:event_test1ActionPerformed

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        int retVal = fc.showSaveDialog(this);
        if(retVal == JFileChooser.APPROVE_OPTION) path = fc.getSelectedFile().getAbsolutePath();
        pathField.setText(path);
    }//GEN-LAST:event_browseButtonActionPerformed

    private void test2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_test2ActionPerformed
        test = false;
    }//GEN-LAST:event_test2ActionPerformed

    private void spinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerStateChanged
        size = (int) spinner.getValue();
    }//GEN-LAST:event_spinnerStateChanged

    private void showButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showButtonActionPerformed
        board = genGraph(size);
        displayGraph(board);
    }//GEN-LAST:event_showButtonActionPerformed

    private void runButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runButtonActionPerformed
        board = genGraph(size);
        Game bgame = GraphTools.graphToGame(board, size*size, size*size+1);
        //get to the current position
        //what are the moves already played by Short and Cut?
        //play at center
        if(size % 2 == 1) shortMove.add(point(size / 2, size / 2, size));
        else shortMove.add(point((size - 1) / 2, (size - 1) / 2, size));
        shortMove.stream().forEach((smove) -> {
            gt.secureElement(bgame, smove);
        });
        cutMove.stream().forEach((cmove) -> {
            gt.cutElement(bgame, cmove);
        });
        String result = "";
        result = result + bgame.toString() + "\n";
        if(test) {
            //run the test
            long RAMoffset = Runtime.getRuntime().freeMemory() / 1024;
            long time1 = System.currentTimeMillis();
            boolean answer = gt.isPositiveB(bgame, true, false);
            long time2 = System.currentTimeMillis();
            long RAMtotal = Runtime.getRuntime().totalMemory() / 1024;
            long RAMused = RAMoffset - (Runtime.getRuntime().freeMemory() / 1024);
            result = result + answer + "\n";
            result = result + (time2 - time1) + " ms" + "\n";
            result = result + (RAMused + " KB out of " + RAMtotal + " KB") + "\n";
            result = result + "- - - -" + "\n";
            
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(path, true));
                String[] words = result.split("\n");
                for (String word: words) {
                    writer.write(word);
                    writer.newLine();
                }
                writer.close();
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            } finally {
                try {
                    writer.close();
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                }
            }
        }
        else {
            //run the test
            long RAMoffset = Runtime.getRuntime().freeMemory() / 1024;
            long time1 = System.currentTimeMillis();
            Tree strategy = gt.isPositive(bgame, true, false);
            long time2 = System.currentTimeMillis();
            long RAMtotal = Runtime.getRuntime().totalMemory() / 1024;
            long RAMused = RAMoffset - (Runtime.getRuntime().freeMemory() / 1024);
            result = result + Arrays.deepToString(strategy.getList()) + "\n";
            result = result + (time2 - time1) + " ms" + "\n";
            result = result + (RAMused + " KB out of " + RAMtotal + " KB") + "\n";
            result = result + "- - - -" + "\n";
            
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(path, true));
                String[] words = result.split("\n");
                for (String word: words) {
                    writer.write(word);
                    writer.newLine();
                }
                writer.close();
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            } finally {
                try {
                    writer.close();
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                }
            }
        }
    }//GEN-LAST:event_runButtonActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(path, false));
                writer.write("");
                writer.close();
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            } finally {
                try {
                    writer.close();
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                }
            }
    }//GEN-LAST:event_jButton1ActionPerformed

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
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(HEXplode.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new HEXplode().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bg;
    private javax.swing.JButton browseButton;
    private javax.swing.JLabel dimLabel;
    private javax.swing.JFileChooser fc;
    private javax.swing.JButton jButton1;
    private javax.swing.JTextField pathField;
    private javax.swing.JButton runButton;
    private javax.swing.JButton showButton;
    private javax.swing.JSpinner spinner;
    private javax.swing.JLabel stratLabel;
    private javax.swing.JRadioButton test1;
    private javax.swing.JRadioButton test2;
    // End of variables declaration//GEN-END:variables
}
