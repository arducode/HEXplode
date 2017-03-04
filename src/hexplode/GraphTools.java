/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hexplode;

//import java.util.Iterator;
import it.unimi.dsi.fastutil.objects.*;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;
//import org.graphstream.graph.*;
//import org.graphstream.graph.implementations.SingleGraph;
import org.ujmp.core.*;
import org.jgrapht.*;
import org.jgrapht.graph.*;
//import stringenum.StringEnum;
import setgame.Game;
import it.unimi.dsi.fastutil.*;
import it.unimi.dsi.fastutil.ints.*;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.roaringbitmap.RoaringBitmap;

/**
 *
 * @author Marco
 */
public class GraphTools {
    /**
     *  Returns the adjacency matrix of the given graph 
     * @param g the graph whose to create the adjacency matrix
     * @return A Game object that represents the adjacency matrix of the graph
     */
    public static Matrix adjacencyMatrix(Graph<Integer, DefaultEdge> g) {
        Matrix res = Matrix.Factory.zeros(g.vertexSet().size(), g.vertexSet().size());
        g.vertexSet().stream().forEach((a) -> {
            g.vertexSet().stream().forEach((b) -> {
                if(g.containsEdge(a, b)) res.setAsBoolean(true, a, b);
                else res.setAsBoolean(false, a, b);
            });
        });
        return res;
    }
    /**
     * Returns a graph made from the given adjacency matrix
     * 
     * @param m a Game object that represents the adjacency matrix of the graph
     * @param directed a boolean to indicate whether the graph is directed or not - the method will not distinguish itself from the given Matrix
     * @return A Graph object made from the given adjacency matrix
     */
    public static Graph<Integer, DefaultEdge> fromMatrix(Matrix m, boolean directed) {
        if(m.isSquare()) {
            if(directed) {
                Graph<Integer, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);
                for(int i = 0; i < m.getRowCount(); i++) g.addVertex(i);
                for(long[] coord : m.allCoordinates()) {
                    if(m.getAsBoolean(coord)) {
                        g.addEdge((int) coord[0], (int) coord[1]);
                    }
                }
                return g;
            }
            else {
                Graph<Integer, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);
                for(int i = 0; i < m.getRowCount(); i++) g.addVertex(i);
                for(long[] coord : m.allCoordinates()) {
                    if(m.getAsBoolean(coord)) {
                        g.addEdge((int) coord[0], (int) coord[1]);
                        g.addEdge((int) coord[1], (int) coord[0]);
                    }
                }
                return g;
            }
        }
        return null;
    }
    
    public static boolean areNear(Graph g, int va, int vb) {
        return g.containsEdge(vb, va) || g.containsEdge(va, vb);
    }
    
    public static Set<Integer> getNeighbours(Graph<Integer, DefaultEdge> g, int v) {
        Set<DefaultEdge> edges = g.edgesOf(v);
        Set<Integer> neighbours = edges.stream().map((e) -> (g.getEdgeSource(e) == v ? g.getEdgeTarget(e) : g.getEdgeSource(e))).collect(Collectors.toSet());
        return neighbours;
    }
    
    public static Set<Integer> getGoodNeighbours(Graph<Integer, DefaultEdge> g, IntList visited, int end) {
        int last = visited.getInt(visited.size() - 1);
        if(areNear(g, last, end)) {
            Set<Integer> res = new HashSet<>();
            res.add(end);
            return res;
        }
        Set<Integer> ngbs = getNeighbours(g, last);
        Set<Integer> badNodes = new HashSet<>(visited);
        badNodes.remove(last);
        visited.stream().filter((i) -> (i != last)).forEach((i) -> {
            badNodes.addAll(getNeighbours(g, i));
        });
        ngbs.removeAll(badNodes);
        return ngbs;
    }
    
    public static ObjectList<IntList> absolutePaths(Graph<Integer, DefaultEdge> g, int start, int end) {
//        System.out.println(start + " " + end);
        PriorityQueue<IntList> scan = new ObjectArrayFIFOQueue<>();
        ObjectList<IntList> res = new ObjectArrayList<>();
        
        IntList f = new IntArrayList();
        f.add(start);
        scan.enqueue(f);
        
        while(!scan.isEmpty()) {
            IntList oldPath = scan.dequeue();
            Set<Integer> vxs = getGoodNeighbours(g, oldPath, end);
            vxs.stream().forEach((node) -> {
                IntList newPath = new IntArrayList(oldPath);
                newPath.add(node);
//                System.out.println(java.util.Arrays.toString(newPath.toIntArray()) + " " + node);
                if(node == end) res.add(newPath);
                else scan.enqueue(newPath);
            });
        }
        
        return res;
    }
    
    public static Game graphToGame(Graph<Integer, DefaultEdge> g, int start, int end) {
        ObjectList<IntList> paths = absolutePaths(g, start, end);
        ObjectList<RoaringBitmap> rows = new ObjectArrayList<>();
        paths.stream().forEach((path) -> {
            rows.add(RoaringBitmap.bitmapOf(path.toIntArray()));
        });
        Game res = new Game(paths.size(), g.vertexSet().size(), rows);
        IntList del = new IntArrayList();
        for(int i = 0; i < res.getPlayableElmsCount(); i++) {
            if(i == start || i == end) del.add(i);
            if(!res.isElmBelongingToAnyBlock(i)) del.add(i);
        }
        res.removeAllElms(del);
        return res;
    }
}
