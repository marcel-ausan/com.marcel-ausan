package table;

import record.MyPersistedObject;
import utilities.ExtendedDataHelper;

import com.ullink.ulbridge2.ULMessage;
import com.ullink.ulbridge2.dao.tables.common.LongKeyBridgeDynamicTableDefinition;
import com.ullink.ultools.dao.core.api.exception.DAOUnexpectedException;
import com.ullink.ultools.dao.core.impl.file.FileCachedObject;
import com.ullink.ultools.dao.core.tools.DataBuffer;
import com.ullink.ultools.dao.core.tools.DataHelper;
import com.ullink.ultools.dao.core.tools.ReUsableCharBuffer;
import com.ullink.ultools.dao.core.tools.SharedByteBuffer;

public class MyTableDefinition extends LongKeyBridgeDynamicTableDefinition<MyPersistedObject>
{
    private static final byte MAGIC_0 = '0';

    public MyTableDefinition()
    {
        super();
    }

    @Override
    public void writeObject(SharedByteBuffer cs, FileCachedObject<Long, MyPersistedObject> object)
    {
        serialize0(DataHelper.writeByte(cs, MAGIC_0), object);
    }

    public static void serialize0(SharedByteBuffer cs, FileCachedObject<Long, MyPersistedObject> object)
    {
        MyPersistedObject myObject = object.getObj();
        if (myObject == null)
        {
            return;
        }

        DataHelper.writeString(cs, myObject.getField1());
        DataHelper.writeInteger(cs, myObject.getField2());
        ExtendedDataHelper.writeULMessage(cs, myObject.getField3());
    }

    @Override
    public MyPersistedObject readObject(Long id, DataBuffer cs)
    {
        byte magic = cs.getBuffer()[cs.getOffset()];
        cs.inc(1);
        switch (magic)
        {
            case MAGIC_0 :
                return deserialize0(id, cs);
            default :
                throw new DAOUnexpectedException("Invalid magic char [" + magic + "] in persistence, supported [" + MAGIC_0 + "]");
        }
    }

    public static MyPersistedObject deserialize0(Long id, DataBuffer cs)
    {
        ReUsableCharBuffer buffer = ReUsableCharBuffer.get();
        String field1 = DataHelper.getString(cs, buffer);
        int field2 = DataHelper.getInteger(cs);
        ULMessage field3 = ExtendedDataHelper.getULMessage(cs);

        MyPersistedObject myObject = new MyPersistedObject(field1, field2, field3);
        myObject.setId(id);
        return myObject;
    }

    @Override
    public FileCachedObject<Long, MyPersistedObject> loadObject(long fPos, Object... data)
    {
        Long key = (Long) data[0];
        updateIdGenerator(key.longValue());

        String field1 = (String) data[1];
        int field2 = ((Integer) data[2]).intValue();
        ULMessage field3 = (ULMessage) data[3];

        MyPersistedObject myObject = new MyPersistedObject(field1, field2, field3);
        myObject.setId(key);
        return createFileCachedObject(key, myObject, fPos);
    }

    @Override
    public Object[] unloadObject(FileCachedObject<Long, MyPersistedObject> cached)
    {
        if (cached != null && cached.getObjInternal() != null)
        {
            MyPersistedObject myObject = cached.getObjInternal();
            return new Object[]{cached.id, myObject.getField1(), Integer.valueOf(myObject.getField2()), myObject.getField3()};
        }
        return null;
    }
}