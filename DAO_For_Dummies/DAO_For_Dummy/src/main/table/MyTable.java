package table;

import record.MyPersistedObject;

import com.ullink.ulbridge2.dao.tables.common.BridgeDynamicTable;
import com.ullink.ultools.dao.core.api.NonUniqueIndex;
import com.ullink.ultools.dao.core.api.Transaction;
import com.ullink.ultools.dao.core.api.Transform;
import com.ullink.ultools.dao.core.api.UniqueIndex;

public interface MyTable extends BridgeDynamicTable<Long, MyPersistedObject>
{
    Object getTableLock();

    Long getKey(MyPersistedObject msg);

    UniqueIndex<String, Long, MyPersistedObject> getUniqueIndex();

    NonUniqueIndex<String, Long, MyPersistedObject> getNonUniqueIndex();

    void buildIndexes();

    void doTransform(Transaction trans, Long id, Transform<MyPersistedObject> t);
}