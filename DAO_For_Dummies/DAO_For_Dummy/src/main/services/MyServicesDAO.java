package services;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import record.MyPersistedObject;
import record.MyPersistedWritableObject;
import table.MySharedFileSyncTable;
import table.MyTable;
import table.MyTableDefinition;
import utilities.MyConstants;

import com.ullink.ultools.NamingService;
import com.ullink.ultools.dao.core.api.Context;
import com.ullink.ultools.dao.core.api.TableId;
import com.ullink.ultools.dao.core.api.Transaction;
import com.ullink.ultools.dao.core.api.Transform;
import com.ullink.ultools.dao.core.api.Visitor;
import com.ullink.ultools.dao.core.impl.file.shared.SharedFileDAO;

public class MyServicesDAO implements MyServices
{
    protected final MyTable table;

    public MyServicesDAO()
    {
        SharedFileDAO dao = (SharedFileDAO) NamingService.lookup(MyConstants.NAMING_DAO);
        TableId<Long, MyPersistedObject> id = new TableId<Long, MyPersistedObject>(MyConstants.MY_TABLE_NAME, MyPersistedObject.class);
        table = dao.registerTable(id, MySharedFileSyncTable.class, new MyTableDefinition());
    }

    @Override
    public void insertObject(MyPersistedObject myObject)
    {
        table.create(myObject);
    }

    @Override
    public void removeObject(String key)
    {
        table.getUniqueIndex().remove(key);
    }

    @Override
    public Set<MyPersistedObject> selectAllByNonUniqueKey(String key)
    {
        return table.getNonUniqueIndex().getAll(key);
    }

    @Override
    public MyPersistedObject selectByUniqueKey(String key)
    {
        return table.getUniqueIndex().get(key);
    }

    @Override
    public void updateObject(final MyPersistedWritableObject newObject)
    {
        Long id = table.getUniqueIndex().getId(newObject.getField1());
        if (id == null)
        {
            System.err.println("Object not found for key = " + newObject.getField1());
            return;
        }

        Transaction trans = Context.getTransaction();
        if (trans.isIsolated())
        {
            throw new UnsupportedOperationException("Isolated transaction not supported");
        }

        table.doTransform(trans, id, new Transform<MyPersistedObject>()
        {
            public MyPersistedObject transform(MyPersistedObject origObject)
            {
                if (newObject.getField1().equals(origObject.getField1()) && newObject.getField2() == origObject.getField2()
                        && newObject.getField3().equals(origObject.getField3()))
                {
                    return origObject;
                }

                MyPersistedObject object = new MyPersistedObject(newObject.getField1(), newObject.getField2(), newObject.getField3());
                object.setId(origObject.getId());
                return object;
            }
        });
    }

    public void visitAll(Visitor<MyPersistedObject> visitor)
    {
        table.visitAll(visitor);
    }

    public List<MyPersistedObject> getAll()
    {
        final List<MyPersistedObject> list = new LinkedList<MyPersistedObject>();
        visitAll(new Visitor<MyPersistedObject>()
        {
            @Override
            public void visit(MyPersistedObject obj)
            {
                list.add(obj);
            }
        });
        return list;
    }

    public void clear()
    {
        table.truncate();
    }

    public void load()
    {
        table.buildIndexes();
    }
}
