package table;

import record.MyPersistedObject;
import utilities.MyConstants;

import com.ullink.ulbridge2.dao.tables.common.BridgeDynamicTableDefinition;
import com.ullink.ulbridge2.dao.tables.common.BridgeDynamicTableImpl;
import com.ullink.ultools.dao.core.api.NonUniqueIndex;
import com.ullink.ultools.dao.core.api.TableId;
import com.ullink.ultools.dao.core.api.TransactionManager;
import com.ullink.ultools.dao.core.api.UniqueIndex;
import com.ullink.ultools.dao.core.api.exception.DAOUnexpectedException;
import com.ullink.ultools.dao.core.impl.CachedObject;
import com.ullink.ultools.dao.core.impl.file.FileTableSink;

public class MySharedFileSyncTable extends BridgeDynamicTableImpl<Long, MyPersistedObject> implements MyTable
{
    protected final AbstractUniqueIndex<String>    myUniqueIndex;
    protected final AbstractNonUniqueIndex<String> myNonUniqueIndex;

    public MySharedFileSyncTable(TableId<Long, MyPersistedObject> id, BridgeDynamicTableDefinition<Long, MyPersistedObject> definition,
            TransactionManager manager, FileTableSink sink)
    {
        super(id, definition, manager, sink);

        myUniqueIndex = new AbstractUniqueIndex<String>()
        {
            @Override
            protected String getKey(CachedObject<MyPersistedObject> v)
            {
                if (v == null || v.getObj() == null)
                {
                    return null;
                }
                return v.getObj().getField1();
            }

            @Override
            public void notifyCreated(CachedObject<MyPersistedObject> v) throws NullPointerException
            {
                String key = getKey(v);
                if (index.get(key) != null)
                {
                    throw new DAOUnexpectedException((new StringBuilder()).append("Duplicate key in index: ").append(key).toString());
                }
                super.notifyCreated(v);
            }
        };

        myNonUniqueIndex = new AbstractNonUniqueIndex<String>()
        {
            @Override
            protected String getKey(CachedObject<MyPersistedObject> v)
            {
                if (v == null || v.getObj() == null)
                {
                    return null;
                }
                return v.getObj().getField3().getString(MyConstants.NON_UNIQUE_KEY);
            }
        };

        addIndex("myUniqueIndex", myUniqueIndex);
        addIndex("myNonUniqueIndex", myNonUniqueIndex);

    }

    public Object getTableLock()
    {
        return tableLock;
    }

    public Long getKey(MyPersistedObject msg)
    {
        return getDefinition().getId(msg);
    }

    public UniqueIndex<String, Long, MyPersistedObject> getUniqueIndex()
    {
        return myUniqueIndex;
    }

    public NonUniqueIndex<String, Long, MyPersistedObject> getNonUniqueIndex()
    {
        return myNonUniqueIndex;
    }
}
