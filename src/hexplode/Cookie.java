/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hexplode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import it.unimi.dsi.fastutil.objects.*;

/**
 *
 * @author Marco
 */
public class Cookie {
    private final int card;
    private int ID = -1;
    private final ObjectList<Intersection> inters;
    
    public Cookie(int setCard, ObjectList<Intersection> intersections) {
        card = setCard;
        inters = intersections;
        sort();
    }
    
    public Cookie(int setCard, int setID, ObjectList<Intersection>  intersections) {
        card = setCard;
        ID = setID;
        inters = intersections;
        sort();
    }
    private void sort() {
        inters.sort((Intersection o1, Intersection o2) -> // <editor-fold defaultstate="collapsed" desc="Compare basing on the entries">
        {
            if(o1.getCard() > o2.getCard()) return 1;
            else if(o1.getCard() < o2.getCard()) return -1;
            else {
                if(o1.getSets().size() > o2.getSets().size()) return 1;
                else if(o1.getSets().size() < o2.getSets().size()) return -1;
                else {
                    int size = o1.getSets().size();
                    for(int i = 0; i < size; i++) {
                        if(o1.getSets().get(i)[0] > o2.getSets().get(i)[0]) return 1;
                        else if(o1.getSets().get(i)[0] < o2.getSets().get(i)[0]) return -1;
                        else {
                            if(o1.getSets().get(i)[1] > o2.getSets().get(i)[1]) return 1;
                            else if(o1.getSets().get(i)[1] < o2.getSets().get(i)[1]) return -1;
                        }
                    }
                    return 0;
                }
            }
        });
        // </editor-fold>
    }
    
    protected int getCard() {
        return card;
    }
    
    protected ObjectList<Intersection> getInters() {
        return inters;
    }
    
    public boolean matches(Cookie ref) {
        sort();
        if(card == ref.card && ID == ref.ID && inters.size() == ref.inters.size()) {
            for(int i = 0; i < inters.size(); i++) {
                if(!inters.get(i).matches(ref.inters.get(i))) {
                    return false;
                }
            }
            return true;
        }
        else return false;
    }
    
    @Override
    public String toString() {
        return "card:" + card + ", ID:" + ID + ", inters:" + Arrays.toString(inters.toArray(new Intersection[0]));
    }
}
