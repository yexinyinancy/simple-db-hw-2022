package simpledb.execution;

import simpledb.storage.Field;
import simpledb.storage.Tuple;

import java.io.Serializable;

/**
 * JoinPredicate compares fields of two tuples using a predicate. JoinPredicate
 * is most likely used by the Join operator.
 */
public class JoinPredicate implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor -- create a new predicate over two fields of two tuples.
     *
     * @param field1 The field index into the first tuple in the predicate
     * @param field2 The field index into the second tuple in the predicate
     * @param op     The operation to apply (as defined in Predicate.Op); either
     *               Predicate.Op.GREATER_THAN, Predicate.Op.LESS_THAN,
     *               Predicate.Op.EQUAL, Predicate.Op.GREATER_THAN_OR_EQ, or
     *               Predicate.Op.LESS_THAN_OR_EQ
     * @see Predicate
     */
    private int field1_;
    private Predicate.Op op_;
    private int field2_;

    public JoinPredicate(int field1, Predicate.Op op, int field2) {
        // TODO: some code goes here
        this.field1_ = field1;
        this.op_ = op;
        this.field2_ = field2;
    }

    /**
     * Apply the predicate to the two specified tuples. The comparison can be
     * made through Field's compare method.
     *
     * @return true if the tuples satisfy the predicate.
     */
    public boolean filter(Tuple t1, Tuple t2) {
        // TODO: some code goes here
        return t1.getField(field1_).compare(op_, t2.getField(field2_));
    }

    public int getField1() {
        // TODO: some code goes here
        return field1_;
    }

    public int getField2() {
        // TODO: some code goes here
        return field2_;
    }

    public Predicate.Op getOperator() {
        // TODO: some code goes here
        return op_;
    }
}
