package simpledb.execution;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Type;
import simpledb.storage.BufferPool;
import simpledb.storage.IntField;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

import java.io.IOException;

/**
 * The delete operator. Delete reads tuples from its child operator and removes
 * them from the table they belong to.
 */
public class Delete extends Operator {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor specifying the transaction that this delete belongs to as
     * well as the child to read from.
     *
     * @param t     The transaction this delete runs in
     * @param child The child operator from which to read tuples for deletion
     */
    private TransactionId t_;
    private OpIterator child_;
    private TupleDesc td = new TupleDesc(new Type[]{Type.INT_TYPE});
    private boolean f = false;

    public Delete(TransactionId t, OpIterator child) {
        // TODO: some code goes here
        this.t_ = t;
        this.child_ = child;
    }

    public TupleDesc getTupleDesc() {
        // TODO: some code goes here
        return td;
    }

    public void open() throws DbException, TransactionAbortedException {
        // TODO: some code goes here
        super.open();
        child_.open();
    }

    public void close() {
        // TODO: some code goes here
        child_.close();
        super.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // TODO: some code goes here
        close();
        open();
    }

    /**
     * Deletes tuples as they are read from the child operator. Deletes are
     * processed via the buffer pool (which can be accessed via the
     * Database.getBufferPool() method.
     *
     * @return A 1-field tuple containing the number of deleted records.
     * @see Database#getBufferPool
     * @see BufferPool#deleteTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // TODO: some code goes here
        int cnt = 0;
        while (child_.hasNext()) {
            Tuple t = child_.next();
            try {
                Database.getBufferPool().deleteTuple(t_, t);;
                cnt++;
            }
            catch (IOException e) {
                throw new DbException("Insert: Error: IOException when Inserting");
            }
        }
        if (!f) {
            Tuple ret = new Tuple(td);
            ret.setField(0, new IntField(cnt));
            f = true;
            return ret;
        }
        return null;
    }

    @Override
    public OpIterator[] getChildren() {
        // TODO: some code goes here
        return new OpIterator[]{child_};
    }

    @Override
    public void setChildren(OpIterator[] children) {
        // TODO: some code goes here
        child_ = children[0];
    }

}
