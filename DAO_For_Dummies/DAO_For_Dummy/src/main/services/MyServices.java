package services;

import java.util.List;
import java.util.Set;

import record.MyPersistedObject;
import record.MyPersistedWritableObject;

import com.ullink.ultools.dao.core.api.Visitor;

public interface MyServices
{
    public void insertObject(MyPersistedObject myObject);

    public void updateObject(final MyPersistedWritableObject newObject);

    public void removeObject(String key);

    public MyPersistedObject selectByUniqueKey(String key);

    public Set<MyPersistedObject> selectAllByNonUniqueKey(String key);

    public void visitAll(Visitor<MyPersistedObject> visitor);

    public List<MyPersistedObject> getAll();

    public void clear();

    public void load();
}
