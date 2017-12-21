package services;

import java.util.List;
import java.util.Set;

import record.MyPersistedObject;
import record.MyPersistedWritableObject;
import utilities.MyConstants;

import com.ullink.ulbridge2.ULSqlLink;
import com.ullink.ultools.NamingService;
import com.ullink.ultools.dao.core.api.Visitor;

public class MyServicesSqlLink implements MyServices
{
    public static enum Column {
        FIELD1("VARCHAR(255)"), FIELD2("INTEGER(255)"), FIELD3("VARCHAR(255)"), ;

        final private String type;

        private Column(String pType)
        {
            type = pType;
        }

        public String type()
        {
            return type;
        }
    }

    public static final String[] COLUMNS = new String[]{Column.FIELD1.name(), Column.FIELD2.name(), Column.FIELD3.name(),};
    public static final String[] TYPES   = new String[]{Column.FIELD1.type(), Column.FIELD2.type(), Column.FIELD3.type(),};

    protected ULSqlLink          sqlLink = null;

    public MyServicesSqlLink()
    {
        sqlLink = (ULSqlLink) NamingService.lookup(MyConstants.NAMING_SQLLINK);
        sqlLink.executeCreate(MyConstants.MY_TABLE_NAME, COLUMNS, TYPES, false);
    }

    @Override
    public void insertObject(MyPersistedObject myObject)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void removeObject(String key)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public Set<MyPersistedObject> selectAllByNonUniqueKey(String key)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MyPersistedObject selectByUniqueKey(String key)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateObject(final MyPersistedWritableObject newObject)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void load()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void clear()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void visitAll(Visitor<MyPersistedObject> visitor)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public List<MyPersistedObject> getAll()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
