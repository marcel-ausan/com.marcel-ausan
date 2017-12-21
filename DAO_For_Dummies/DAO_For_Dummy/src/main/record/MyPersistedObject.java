package record;

import com.ullink.ulbridge2.ULMessage;
import com.ullink.ultools.dao.core.impl.common.PersistableLongKey;

public class MyPersistedObject extends PersistableLongKey
{
    protected String    field1;
    protected int       field2;
    protected ULMessage field3;

    public MyPersistedObject(String pField1, int pField2, ULMessage pField3)
    {
        super(null);
        field1 = pField1;
        field2 = pField2;
        field3 = pField3;
    }

    public String getField1()
    {
        return field1;
    }

    public int getField2()
    {
        return field2;
    }

    public ULMessage getField3()
    {
        return field3;
    }
    
    @Override
    public MyPersistedWritableObject clone()
    {
        return new MyPersistedWritableObject(field1, field2, field3);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[field1=\"").append(field1);
        sb.append("\", field2=\"").append(field2);
        sb.append("\", field3=\"").append(field3);
        sb.append("\"]");
        return sb.toString();
    }
}
