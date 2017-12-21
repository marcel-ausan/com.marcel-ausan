import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import record.MyPersistedObject;
import record.MyPersistedWritableObject;
import services.MyServices;
import services.MyServicesFactory;
import utilities.MyConstants;

import com.ullink.ulbridge2.ULMessage;
import com.ullink.ulbridge2.dao.BridgeSharedFileDAO;
import com.ullink.ultools.NamingService;
import com.ullink.ultools.dao.core.impl.file.FileDAO;
import com.ullink.ultools.dao.core.impl.file.shared.SharedFileDAO;
import com.ullink.ultools.prefs.SimplePreferences;

public class DAOTest
{
    private static MyServices myServices = null;

    @BeforeClass
    public static void startUpDAO()
    {
        SimplePreferences prefs = new SimplePreferences();
        File dir = new File("data");
        deleteDirectory(dir);
        dir.mkdir();

        prefs.put(FileDAO.Constants.DIR_KEY, dir.getAbsolutePath());
        prefs.put(SharedFileDAO.Constants.FILE_KEY, SharedFileDAO.Constants.Defaults.DEFAULT_FILE);
        BridgeSharedFileDAO dao = new BridgeSharedFileDAO(prefs);
        dao.init();
        NamingService.bind(MyConstants.NAMING_DAO, dao);

        myServices = MyServicesFactory.createMyServices();
    }

    protected static boolean deleteDirectory(File path)
    {
        if (path.exists())
        {
            for (File file : path.listFiles())
            {
                if (file.isDirectory())
                {
                    deleteDirectory(file);
                }
                else
                {
                    file.delete();
                }
            }
        }
        return path.delete();
    }

    public ULMessage createULMessage(String nonUniqueKey)
    {
        ULMessage ulm = new ULMessage();
        ulm.add(MyConstants.NON_UNIQUE_KEY, nonUniqueKey);
        ulm.add("Tag1", "1");
        ulm.add("Tag2", "2");
        ulm.add("Tag3", "3");
        ulm.add("Tag4", "4");
        ulm.add("Tag5", "5");
        return ulm;
    }

    @After
    public void cleanUpDAO()
    {
        myServices.clear();
    }

    @Test
    public void testInsert()
    {
        String uniqueKey = "testInsert1";
        myServices.insertObject(new MyPersistedObject(uniqueKey, 10, createULMessage("100")));

        List<MyPersistedObject> list = myServices.getAll();
        assertEquals(list.size(), 1);

        MyPersistedObject obj = list.get(0);
        assertEquals(obj.getField1(), uniqueKey);
        assertEquals(obj.getField2(), 10);
    }

    @Test
    public void testRemove()
    {
        String uniqueKey = "testRemove1";
        myServices.insertObject(new MyPersistedObject(uniqueKey, 10, createULMessage("100")));
        myServices.removeObject(uniqueKey);

        List<MyPersistedObject> list = myServices.getAll();
        assertEquals(list.size(), 0);
    }

    @Test
    public void testSelectAllByNonUniqueKey()
    {
        myServices.insertObject(new MyPersistedObject("testNonUnique1", 10, createULMessage("100")));
        myServices.insertObject(new MyPersistedObject("testNonUnique2", 10, createULMessage("100")));
        myServices.insertObject(new MyPersistedObject("testNonUnique3", 10, createULMessage("100")));
        myServices.insertObject(new MyPersistedObject("testNonUnique4", 10, createULMessage("200")));
        myServices.insertObject(new MyPersistedObject("testNonUnique5", 10, createULMessage("200")));
        myServices.insertObject(new MyPersistedObject("testNonUnique6", 10, createULMessage("300")));

        Set<MyPersistedObject> set = myServices.selectAllByNonUniqueKey("200");
        assertEquals(set.size(), 2);
    }

    @Test
    public void testSelectByUniqueKey()
    {
        myServices.insertObject(new MyPersistedObject("testUnique1", 10, createULMessage("100")));
        myServices.insertObject(new MyPersistedObject("testUnique2", 10, createULMessage("100")));
        myServices.insertObject(new MyPersistedObject("testUnique3", 10, createULMessage("100")));
        myServices.insertObject(new MyPersistedObject("testUnique4", 10, createULMessage("200")));
        myServices.insertObject(new MyPersistedObject("testUnique5", 10, createULMessage("200")));
        myServices.insertObject(new MyPersistedObject("testUnique6", 10, createULMessage("300")));

        MyPersistedObject obj = myServices.selectByUniqueKey("testUnique6");
        assertEquals(obj.getField1(), "testUnique6");
        assertEquals(obj.getField2(), 10);
        assertEquals(obj.getField3().getString(MyConstants.NON_UNIQUE_KEY), "300");
    }

    @Test
    public void testUpdate()
    {
        myServices.insertObject(new MyPersistedObject("testUpdate1", 10, createULMessage("100")));

        MyPersistedObject obj = myServices.selectByUniqueKey("testUpdate1");
        MyPersistedWritableObject writableObj = obj.clone();
        writableObj.setField2(20);
        writableObj.setField3(createULMessage("200"));
        myServices.updateObject(writableObj);

        List<MyPersistedObject> list = myServices.getAll();
        assertEquals(list.size(), 1);

        MyPersistedObject checkObj = list.get(0);
        assertEquals(checkObj.getField1(), "testUpdate1");
        assertEquals(checkObj.getField2(), 20);
        assertEquals(checkObj.getField3().getString(MyConstants.NON_UNIQUE_KEY), "200");
    }    
}
