package simpledb.execution;

import simpledb.common.Type;
import simpledb.storage.Tuple;
import java.util.*;

import javax.print.DocFlavor.STRING;

import simpledb.storage.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import simpledb.common.DbException;
import simpledb.transaction.TransactionAbortedException;

/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    /**
     * Aggregate constructor
     *
     * @param gbfield     the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield      the 0-based index of the aggregate field in the tuple
     * @param what        aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */
    private int gbfield_;
    private Type gbfielType_;
    private int afield_;
    private Op what_;
    private Object map;

    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // TODO: some code goes here
        if (what != Op.COUNT) {
            throw new IllegalArgumentException("StringAggregator only support COUNT");
        }
        this.gbfield_ = gbfield;
        this.gbfielType_ = gbfieldtype;
        this.afield_ = afield;
        this.what_ = what;
        if (gbfield_ == Aggregator.NO_GROUPING) {
            map = new ArrayList<Field>();
        }
        else {
            assert gbfieldtype != null;
            map = new HashMap<Field, ArrayList<Field>>();
        }
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     *
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // TODO: some code goes here
        if (gbfield_ == Aggregator.NO_GROUPING) {
            ((ArrayList<Field>) map).add(tup.getField(afield_));
        }
        else {
            Field f1 = tup.getField(gbfield_);
            Field f2 = tup.getField(afield_);
            if (((HashMap<Field, ArrayList<Field>>) map).containsKey(f1)) {
                ((HashMap<Field, ArrayList<Field>>) map).get(f1).add(f2);
            }
            else {
                ArrayList<Field> a = new ArrayList<Field>();
                a.add(f2);
                ((HashMap<Field, ArrayList<Field>>) map).put(f1, a);
            }
        }
    }

    /**
     * Create a OpIterator over group aggregate results.
     *
     * @return a OpIterator whose tuples are the pair (groupVal,
     *         aggregateVal) if using group, or a single (aggregateVal) if no
     *         grouping. The aggregateVal is determined by the type of
     *         aggregate specified in the constructor.
     */
    public class StringAggOpIterator implements OpIterator {
        public ArrayList<Tuple> ret = new ArrayList<Tuple>();
        public Iterator<Tuple> itr;
        public StringAggOpIterator() {
            if (gbfield_ == Aggregator.NO_GROUPING) {
                Tuple tup = new Tuple(getTupleDesc());
                tup.setField(0, new IntField(((ArrayList<Integer>) map).size()));
                ret.add(tup);
            }
            else {
                for (Map.Entry<Field, ArrayList<Field>> e : ((HashMap<Field, ArrayList<Field>>) map).entrySet()) {
                    Tuple tup = new Tuple(getTupleDesc());
                    tup.setField(0, e.getKey());
                    tup.setField(1, new IntField(e.getValue().size()));
                    ret.add(tup);
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
                return new TupleDesc(new Type[]{gbfielType_});
            }
            else {
                return new TupleDesc(new Type[]{gbfielType_, Type.INT_TYPE});
            }
        }

        @Override 
        public void close() {
            itr = null;
        }

    }

    public OpIterator iterator() {
        // TODO: some code goes here
        return new StringAggOpIterator();
    }

}
