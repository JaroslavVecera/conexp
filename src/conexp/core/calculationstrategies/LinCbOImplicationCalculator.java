package conexp.core.calculationstrategies;

import conexp.core.AttributeInformationSupplier;
import conexp.core.ConceptFactory;
import conexp.core.ContextFactory;
import conexp.core.ContextFactoryRegistry;
import conexp.core.DefaultBinaryRelationProcessor;
import conexp.core.Implication;
import conexp.core.ImplicationCalcStrategy;
import conexp.core.ImplicationSet;
import conexp.core.ModifiableSet;
import conexp.core.Set;
import util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinCbOImplicationCalculator
        extends DefaultBinaryRelationProcessor
        implements ImplicationCalcStrategy {

    private ImplicationSet basis;
    private List list;
    private Set emptyAttrSet;
    private boolean fail = false;

    public void tearDown() {
        super.tearDown();
        basis = null;
        list = null;
        emptyAttrSet = null;
        fail = false;
    }

    public void setImplications(ImplicationSet implSet) {
        basis = implSet;
    }

    public void calcImplications() {
        Initialize();
        LinCbOStep(emptyAttrSet, -1, emptyAttrSet, new HashMap());
    }

    private void Initialize() {
        fail = false;
        int attribCount = getAttributeCount();
        emptyAttrSet = ContextFactoryRegistry.createSet(attribCount);
        AttributeInformationSupplier ais = basis.getAttributesInformation();
        list = new ArrayList(attribCount);
        for (int i = 0; i < attribCount; i++) {
        	list.add(new ImplicationSet(ais));
        }
    }

    private void LinCbOStep(Set closingAttributes, int lastAddedAttr, Set newAttributes, Map prevCount) {
        Tuple tuple = LinearClosureRC(closingAttributes, lastAddedAttr, newAttributes, prevCount);
        if (tuple == null) {
            return;
        }
        ModifiableSet closure = tuple.closure;
        Map count = tuple.count;
        Set osa = getObjectsSharingAttributes(closure);
        Set intent = getAttributesSharingObjects(osa);
        if (intent.compare(closure) != Set.EQUAL)
        {
            Implication impl = new Implication(closure, substractSets(intent, closure), osa.elementCount());
            basis.addImplication(impl);
            for (int i = 0; i < closure.length(); i++) {
            	if (closure.in(i)) {
                    ((ImplicationSet)list.get(i)).addImplication(impl);
            	}
            }
            if (testCanonicity(closure, intent, lastAddedAttr)) {
                LinCbOStep(intent, lastAddedAttr, substractSets(intent, closure), count);
            }
        }
        else {
            for (int i = getAttributeCount() - 1; i > lastAddedAttr; i--) {
                if (!closure.in(i)) {
                    ModifiableSet iSet = emptyAttrSet.makeModifiableSetCopy();
                    iSet.put(i);
                    ModifiableSet union = intent.makeModifiableSetCopy();
                    union.or(iSet);
                    LinCbOStep(union, i, iSet, count);
                }
            }
        }
    }

    private Tuple LinearClosureRC(final Set closingAttributes, final int lastAddedAttr, Set newAttributes, final Map prevCount) {
        final ModifiableSet attributes = closingAttributes.makeModifiableSetCopy();
        final Map count = new HashMap(prevCount);
        final ModifiableSet z = newAttributes.makeModifiableSetCopy();
        basis.forEach(new ImplicationSet.ImplicationProcessor() {
            public void processImplication(Implication implication) {
                if (!prevCount.containsKey(implication)) {
                    ModifiableSet s = substractSets(implication.getPremise(), closingAttributes);
                    count.put(implication, s.elementCount());
                }
            }
        });
        fail = false;
        while (!z.isEmpty()) {
            int minAttrId = z.firstIn();
            z.remove(minAttrId);
            ((ImplicationSet)list.get(minAttrId)).forEach(new ImplicationSet.ImplicationProcessor() {
                public void processImplication(Implication implication) {
                    if (fail) {
                        return;
                    }
                    count.put(implication, (Integer)count.get(implication) - 1);
                    if ((Integer)count.get(implication) == 0) {
                        ModifiableSet add = substractSets(implication.getConclusion(), attributes);
                        if (add.firstIn() < lastAddedAttr) {
                            fail = true;
                            return;
                        }
                        attributes.or(add);
                        z.or(add);
                    }
                }
            });
        }
        if (fail) {
            return null;
        } 
        else {
            return new Tuple(attributes, count);
        }
    }

    private int getObjectCount() {
        return rel.getRowCount();
    }

    private int getAttributeCount() {
        return rel.getColCount();
    }

    private ModifiableSet substractSets(Set a, Set b) {
        ModifiableSet res = a.makeModifiableSetCopy();
        res.andNot(b);
        return res;
    }

    private ModifiableSet getObjectsSharingAttributes(Set attributes) {
        int objectCount = rel.getRowCount();
        int attributeCount = rel.getColCount();
        ModifiableSet objects = ContextFactoryRegistry.createSet(objectCount);
        
        for (int object = 0; object < objectCount; object++) {
            boolean insert = true;
            for (int attribute = 0; attribute < attributeCount; attribute++) {
                if (attributes.in(attribute) && !rel.getRelationAt(object, attribute)) {
                    insert = false;
                    break;
                }
            }
            if (insert) {
                objects.put(object);
            }
        }
        return objects;
    }

    private ModifiableSet getAttributesSharingObjects(Set objects) {
        int objectCount = rel.getRowCount();
        int attributeCount = rel.getColCount();
        ModifiableSet attributes = ContextFactoryRegistry.createSet(attributeCount);
        
        for (int attribute = 0; attribute < attributeCount; attribute++) {
            boolean insert = true;
            for (int object = 0; object < objectCount; object++) {
                if (objects.in(object) && !rel.getRelationAt(object, attribute)) {
                    insert = false;
                    break;
                }
            }
            if (insert) {
                attributes.put(attribute);
            }
        }
        return attributes;
    }

    private boolean testCanonicity(Set a, Set b, int upperBoundAttrId) {
        for (int i = 0; i <= upperBoundAttrId; i++) {
            if (a.in(i) != b.in(i)) {
                return false;
            }
        }
        return true;
    }

    private class Tuple { 
        public final ModifiableSet closure; 
        public final Map count;

        public Tuple(ModifiableSet cl, Map cnt) {
            closure = cl;
            count = cnt;
        }
    } 
}
