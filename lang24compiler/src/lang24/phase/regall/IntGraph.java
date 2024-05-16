package lang24.phase.regall;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import lang24.data.mem.MemTemp;

public class IntGraph{
    public final HashMap<MemTemp, GNode> graph;
    
    

    
    public IntGraph(){
        this.graph = new HashMap<>();
    }

    //dodaj tiste, ki so na istem robu
    public void add(HashSet<MemTemp> edge){
        //kako?
        //poisci, ce obstaja, dodaj sosede, ce ne obstaja, ustvari novi node, dodaj sosede
        for(MemTemp mt : edge){
            HashSet<MemTemp> othersThanCurrent = new HashSet<>(edge);
            othersThanCurrent.remove(mt);

            GNode n = this.graph.get(mt);
            //if null, add new gnode, set neighbours
            if(n == null){
                n = new GNode(mt, this.graph);
                this.graph.put(mt, n);
            }
            n.neighbours.addAll(othersThanCurrent);
        }
    }

    //O(n), better than O(nlog(n)) (instead of sort and pick first, we find minimum (should visit each node at least once either way))
    //find node with minimum visible neighbours
    public GNode findMinVN(){
        GNode vrni = null;
        if(this.graph.isEmpty()){
            return vrni;
        }
        else return Collections.min(this.graph.values(), GNode.visibleNeighbourCountComparator());
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Interference graph:\n");
        for(MemTemp mt : this.graph.keySet()){
            StringBuilder sbNode = new StringBuilder();
            GNode node = this.graph.get(mt);
            sb.append(node.temp);
            sb.append(" ");
            sb.append("visible: ");
            sb.append(node.visible);
            sb.append(" ");
            sb.append(node.neighbours.size());
            sb.append(" ");
            sb.append(node.visibleNeighbours());
            sbNode.append(" -> [");
            boolean first = true;
            for(MemTemp nmt : node.neighbours){
                if(!first) sbNode.append(", ");
                else first = false;
                GNode nmt_node = this.graph.get(nmt);
                if(nmt_node.visible) sbNode.append(nmt);
                else{
                    sbNode.append("(");
                    sbNode.append(nmt);
                    sbNode.append(")");
                }
                
            }
            sbNode.append("] c=");
            sbNode.append(node.color);
            sbNode.append('\n');
            sb.append(sbNode.toString());
        }
        return sb.toString();
    }


}

//graph node
class GNode{
    //n count:
    public final HashMap<MemTemp, GNode> temp2node; //mogoce bi bil lahko static?
    public final HashSet<MemTemp> neighbours;
    public final MemTemp temp;
    public int color;
    public boolean visible;
    public boolean potentialSpill;

    public GNode(MemTemp mt, HashMap<MemTemp, GNode> graph){
        this.potentialSpill = false;
        this.temp2node = graph;
        this.color = -1; //-1 = no color
        this.visible = true;
        this.temp = mt;
        this.neighbours = new HashSet<>(); //create empty set
    }

    public int visibleNeighbours(){
        int vrni = 0;
        // if(!this.visible) return Integer.MAX_VALUE;
        for(MemTemp mt : this.neighbours){
            GNode gnei = this.temp2node.get(mt);
            //should not be null, because neighbours are added first
            if(gnei != null){
                if(gnei.visible) vrni = vrni+1;
            }
        }
        return vrni;
    }


    //compare by neighbour count
    public static Comparator<GNode> neighbourCountComparator(){
        return new Comparator<GNode>() {
            @Override
            public int compare(GNode first, GNode second){
                return first.neighbours.size() - second.neighbours.size();
            }
        };
    }

    public static Comparator<GNode> visibleNeighbourCountComparator(){
        return new Comparator<GNode>() {
            @Override
            public int compare(GNode first, GNode second){
                if(!first.visible) return 1; //if invisible, then return other
                if(!second.visible) return -1;
                return first.visibleNeighbours() - second.visibleNeighbours();
            }
        };
    }

    
    
    
}