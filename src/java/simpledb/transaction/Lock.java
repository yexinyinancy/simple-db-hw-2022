package simpledb.transaction;

import simpledb.common.Permissions;
import simpledb.storage.PageId;

public class Lock {
    private Permissions permission_;
    private TransactionId transactionid_;

    public Lock(Permissions permission, TransactionId transactionid) {
        this.permission_ = permission;
        this.transactionid_ = transactionid;
    }
    public TransactionId getTransactionId() {
        return this.transactionid_;
    }
    public Permissions getPermission() {
        return this.permission_;
    }
    public void setTransactionId(TransactionId transactionid) {
        this.transactionid_ = transactionid;
    }
    public void setPermission(Permissions permission) {
        this.permission_ = permission;
    }

}
