package simpledb.execution;

import javafx.scene.transform.Affine;
import simpledb.common.Type;
import simpledb.storage.*;

import java.security.KeyStore.Entry;
import java.util.*;
import simpledb.common.DbException;
import simpledb.transaction.TransactionAbortedException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    /**
     * Aggregate constructor
     *
     * @param gbfield     the 0-based index of the group-by field in the tuple, or
     *                    NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null
     *                    if there is no grouping
     * @param afield      the 0-based index of the aggregate field in the tuple
     * @param what        the aggregation operator
     */
    private int gbfield_;
    private Type gbfielType_;
    private int afield_;
    private Op what_;
    private Object map;
    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // TODO: some code goes here
        this.gbfield_ = gbfield;
        this.gbfielType_ = gbfieldtype;
        this.afield_ = afield;
        this.what_ = what;
        if (gbfield_ == Aggregator.NO_GROUPING) {
            map = new ArrayList<Integer>();
        }
        else {
            assert gbfielType_ != null;
            if (gbfielType_ == Type.INT_TYPE) {
                map = new HashMap<Integer, ArrayList<Integer>>();
            }
            else if (gbfielType_ == Type.STRING_TYPE) {
                map = new HashMap<String, ArrayList<Integer>>();
            }
        }
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor
     *
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // TODO: some code goes here
        if (gbfield_ == Aggregator.NO_GROUPING) {
            ((ArrayList<Integer>) map).add(((IntField) tup.getField(afield_)).getValue());
        }
        else if (gbfielType_ == Type.INT_TYPE) {
            Integer f1 = ((IntField) tup.getField(gbfield_)).getValue();
            Integer f2 = ((IntField) tup.getField(afield_)).getValue();
            if (((HashMap<Integer, ArrayList<Integer>>) map).containsKey(f1)) {
                ((HashMap<Integer, ArrayList<Integer>>) map).get(f1).add(f2);
            }
            else {
                ArrayList<Integer> a = new ArrayList<Integer>();
                a.add(f2);
                ((HashMap<Integer, ArrayList<Integer>>) map).put(f1, a);
            }
        }
        else if (gbfielType_ == Type.STRING_TYPE) {
            String f1 = ((StringField) tup.getField(gbfield_)).getValue();
            Integer f2 = ((IntField) tup.getField(afield_)).getValue();
            if (((HashMap<String, ArrayList<Integer>>) map).containsKey(f1)) {
                ((HashMap<String, ArrayList<Integer>>) map).get(f1).add(f2);
            }
            else {
                ArrayList<Integer> a = new ArrayList<Integer>();
                a.add(f2);
                ((HashMap<String, ArrayList<Integer>>) map).put(f1, a);
            }
        }
    }

    /**
     * Create a OpIterator over group aggregate results.
     *
     * @return a OpIterator whose tuples are the pair (groupVal, aggregateVal)
     *         if using group, or a single (aggregateVal) if no grouping. The
     *         aggregateVal is determined by the type of aggregate specified in
     *         the constructor.
     */
    public class IntAggOpIterator implements OpIterator {
        public ArrayList<Tuple> ret = new ArrayList<Tuple>();
        public Iterator<Tuple> itr;
        public IntAggOpIterator() {
            if (gbfielType_ == Type.INT_TYPE) {
                switch (what_) {
                    case COUNT:
                        for (Map.Entry<Integer, ArrayList<Integer>> e : ((HashMap<Integer, ArrayList<Integer>>) map).entrySet()) {
                            Tuple tup = new Tuple(getTupleDesc());
                            tup.setField(0, new IntField(e.getKey()));
                            tup.setField(1, new IntField(e.getValue().size()));
                            ret.add(tup);
                        }
                        break;
                    case SUM:
                        for (Map.Entry<Integer, ArrayList<Integer>> e : ((HashMap<Integer, ArrayList<Integer>>) map).entrySet()) {
                            int sum = 0;
                            Tuple tup = new Tuple(getTupleDesc());
                            tup.setField(0, new IntField(e.getKey()));
                            for (int i : e.getValue()) {
                                sum += i;
                            }
                            tup.setField(1, new IntField(sum));
                            ret.add(tup);
                        }
                        break;
                    case AVG:
                        for (Map.Entry<Integer, ArrayList<Integer>> e : ((HashMap<Integer, ArrayList<Integer>>) map).entrySet()) {
                            int avg = 0;
                            Tuple tup = new Tuple(getTupleDesc());
                            tup.setField(0, new IntField(e.getKey()));
                            for (int i : e.getValue()) {
                                avg += i;
                            }
                            tup.setField(1, new IntField(avg / e.getValue().size()));
                            ret.add(tup);
                        }
                        break;
                    case MIN:
                        for (Map.Entry<Integer, ArrayList<Integer>> e : ((HashMap<Integer, ArrayList<Integer>>) map).entrySet()) {
                            int min = Integer.MAX_VALUE;
                            Tuple tup = new Tuple(getTupleDesc());
                            tup.setField(0, new IntField(e.getKey()));
                            for (int i : e.getValue()) {
                                min = Math.min(min, i);
                            }
                            tup.setField(1, new IntField(min));
                            ret.add(tup);
                        }
                        break;
                    case MAX:
                        for (Map.Entry<Integer, ArrayList<Integer>> e : ((HashMap<Integer, ArrayList<Integer>>) map).entrySet()) {
                            int max = Integer.MIN_VALUE;
                            Tuple tup = new Tuple(getTupleDesc());
                            tup.setField(0, new IntField(e.getKey()));
                            for (int i : e.getValue()) {
                                max = Math.max(max, i);
                            }
                            tup.setField(1, new IntField(max));
                            ret.add(tup);
                        }
                        break;
                    case SUM_COUNT:
                        throw new NotImplementedException();
                    case SC_AVG:
                        throw new NotImplementedException();
                }
            }
            else if (gbfielType_ == Type.STRING_TYPE) {
                switch (what_) {
                    case COUNT:
                        for (Map.Entry<String, ArrayList<Integer>> e : ((HashMap<String, ArrayList<Integer>>) map).entrySet()) {
                            Tuple tup = new Tuple(getTupleDesc());
                            tup.setField(0, new StringField(e.getKey(), e.getKey().length()));
                            tup.setField(1, new IntField(e.getValue().size()));
                            ret.add(tup);
                        }
                        break;
                    case SUM:
                        for (Map.Entry<String, ArrayList<Integer>> e : ((HashMap<String, ArrayList<Integer>>) map).entrySet()) {
                            int sum = 0;
                            Tuple tup = new Tuple(getTupleDesc());
                            tup.setField(0, new StringField(e.getKey(), e.getKey().length()));
                            for (int i : e.getValue()) {
                                sum += i;
                            }
                            tup.setField(1, new IntField(sum));
                            ret.add(tup);
                        }
                        break;
                    case AVG:
                        for (Map.Entry<String, ArrayList<Integer>> e : ((HashMap<String, ArrayList<Integer>>) map).entrySet()) {
                            int avg = 0;
                            Tuple tup = new Tuple(getTupleDesc());
                            tup.setField(0, new StringField(e.getKey(), e.getKey().length()));
                            for (int i : e.getValue()) {
                                avg += i;
                            }
                            tup.setField(1, new IntField(avg / e.getValue().size()));
                            ret.add(tup);
                        }
                        break;
                    case MIN:
                        for (Map.Entry<String, ArrayList<Integer>> e : ((HashMap<String, ArrayList<Integer>>) map).entrySet()) {
                            int min = Integer.MAX_VALUE;
                            Tuple tup = new Tuple(getTupleDesc());
                            tup.setField(0, new StringField(e.getKey(), e.getKey().length()));
                            for (int i : e.getValue()) {
                                min = Math.min(min, i);
                            }
                            tup.setField(1, new IntField(min));
                            ret.add(tup);
                        }
                        break;
                    case MAX:
                        for (Map.Entry<String, ArrayList<Integer>> e : ((HashMap<String, ArrayList<Integer>>) map).entrySet()) {
                            int max = Integer.MIN_VALUE;
                            Tuple tup = new Tuple(getTupleDesc());
                            tup.setField(0, new StringField(e.getKey(), e.getKey().length()));
                            for (int i : e.getValue()) {
                                max = Math.max(max, i);
                            }
                            tup.setField(1, new IntField(max));
                            ret.add(tup);
                        }
                        break;
                    case SUM_COUNT:
                        throw new NotImplementedException();
                    case SC_AVG:
                        throw new NotImplementedException();
                }
            }
            else if (gbfield_ == Aggregator.NO_GROUPING) {
                Tuple tup = new Tuple(getTupleDesc());
                switch (what_) {
                    case COUNT:
                        tup.setField(0, new IntField(((ArrayList<Integer>) map).size()));
                        ret.add(tup);
                        break;
                    case SUM:
                        int sum = 0;
                        for (int e : ((ArrayList<Integer>) map)) {
                            sum += e;
                        }
                        tup.setField(0, new IntField(sum));
                        ret.add(tup);
                        break;
                    case AVG:
                        int avg = 0;
                        for (int e : ((ArrayList<Integer>) map)) {
                            avg += e;
                        }
                        tup.setField(0, new IntField(avg / ((ArrayList<Integer>) map).size()));
                        ret.add(tup);
                        break;
                    case MIN:
                        int min = Integer.MAX_VALUE;
                        for (int e : ((ArrayList<Integer>) map)) {
                            min = Math.min(min, e);
                        }
                        tup.setField(0, new IntField(min));
                        ret.add(tup);
                        break;
                    case MAX:
                        int max = Integer.MIN_VALUE;
                        for (int e : ((ArrayList<Integer>) map)) {
                            max = Math.max(max, e);
                        }
                        tup.setField(0, new IntField(max));
                        ret.add(tup);
                        break;
                    case SUM_COUNT:
                        throw new NotImplementedException();
                    case SC_AVG:
                        throw new NotImplementedException();
                }
            }
        }

        @Override
        public void open() throws DbException, TransactionAbortedException {
            itr = ret.iterator();
        }

        @Override
        public boolean hasNext() throws DbException, TransactionAbortedException {
            return itr.hasNext();
        }

        @Override
        public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
            return itr.next();
        }

        @Override
        public void rewind() throws DbException, TransactionAbortedException {
            itr = ret.iterator();
        }

        @Override
        public TupleDesc getTupleDesc() {
            if (gbfield_ == Aggregator.NO_GROUPING) {
                return new TupleDesc(new Type[]{Type.INT_TYPE});
            }
            else if (gbfielType_ == Type.INT_TYPE) {
                return new TupleDesc(new Type[]{Type.INT_TYPE, Type.INT_TYPE});
            }
            else {
                return new TupleDesc(new Type[]{Type.STRING_TYPE, Type.INT_TYPE});
            }
        }

        @Override 
        public void close() {
            itr = null;
        }

    }
    public OpIterator iterator() {
        // TODO: some code goes here
        return new IntAggOpIterator();
    }

}
