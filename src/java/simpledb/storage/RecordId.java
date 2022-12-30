package simpledb.storage;

import java.io.Serializable;

/**
 * A RecordId is a reference to a specific tuple on a specific page of a
 * specific table.
 */
public class RecordId implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * Creates a new RecordId referring to the specified PageId and tuple
     * number.
     *
     * @param pid     the pageid of the page on which the tuple resides
     * @param tupleno the tuple number within the page.
     */
    private PageId pid;
    private int tupleno;

    public RecordId(PageId pid, int tupleno) {
        // TODO: some code goes here
        this.pid = pid;
        this.tupleno = tupleno;
    }

    /**
     * @return the tuple number this RecordId references.
     */
    public int getTupleNumber() {
        // TODO: some code goes here
        return this.tupleno;
    }

    /**
     * @return the page id this RecordId references.
     */
    public PageId getPageId() {
        // TODO: some code goes here
        return this.pid;
    }

    /**
     * Two RecordId objects are considered equal if they represent the same
     * tuple.
     *
     * @return True if this and o represent the same tuple
     */
    @Override
    public boolean equals(Object o) {
        // TODO: some code goes here
        if (o instanceof RecordId) {
            RecordId or = (RecordId) o;
            if (or.pid.equals(this.pid) && or.tupleno == this.tupleno) {
                return true;
            }
        }
        return false;
    }

    /**
     * You should implement the hashCode() so that two equal RecordId instances
     * (with respect to equals()) have the same hashCode().
     *
     * @return An int that is the same for equal RecordId objects.
     */
    @Override
    public int hashCode() {
        // TODO: some code goes here
        return String.valueOf(this.pid.hashCode()).concat(String.valueOf(this.tupleno)).hashCode();
    }

}
