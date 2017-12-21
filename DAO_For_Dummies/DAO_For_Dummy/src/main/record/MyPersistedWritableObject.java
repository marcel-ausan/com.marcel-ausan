package record;

import com.ullink.ulbridge2.ULMessage;

public class MyPersistedWritableObject extends MyPersistedObject
{

    public MyPersistedWritableObject(String pField1, int pField2, ULMessage pField3)
    {
        super(pField1, pField2, pField3);
    }

    public void setField1(String pField1)
    {
        field1 = pField1;
    }

    public void setField2(int pField2)
    {
        field2 = pField2;
    }

    public void setField3(ULMessage pField3)
    {
        field3 = pField3;
    }
}
