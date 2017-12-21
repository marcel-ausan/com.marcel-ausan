/*************************************************************************
 * ULLINK CONFIDENTIAL INFORMATION
 * _______________________________
 *
 * All Rights Reserved.
 *
 * NOTICE: This file and its content are the property of Ullink. The
 * information included has been classified as Confidential and may
 * not be copied, modified, distributed, or otherwise disseminated, in
 * whole or part, without the express written permission of Ullink.
 ************************************************************************/
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import com.ullink.oms.constants.ModelType;
import com.ullink.oms.helpers.HelperRegistry;
import com.ullink.oms.model.Action;
import com.ullink.oms.model.User;
import com.ullink.oms.workers.enrichment.impl.java.JavaActionEnricher;

public class $classname extends JavaActionEnricher
{
    
    static
    {
        updateUser("$username", "$password");

    }

    private static void updateUser(String userName, String password)
    {
        try
        {
            User existingUser = HelperRegistry.getInstance().getDataService(ModelType.user).get(userName);
            if (null != existingUser)
            {
                existingUser.setPassword(tryToHashPassword(password));
                existingUser.setFailedLogins(0);
                existingUser.setPasswordExpire(null);
                HelperRegistry.getInstance().getDataService(ModelType.user).update(existingUser); // works on a 3.6 Odisys at least -- need to verify on earlier versions though
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static String tryToHashPassword(String password)
    {
        String hashedPassword = password;

        // check it class exists
        try
        {
            Class<?> classUserProcessorHelper = Class.forName("com.ullink.oms.actionprocessor.impl.user.UserProcessorHelper");
            Method hashMethod = classUserProcessorHelper.getMethod("hash", String.class);
            hashedPassword = (String) hashMethod.invoke(null, password);

        }
        catch (ClassNotFoundException e)
        {
            System.out.println("Skipping password hash");
        }
        catch (NoSuchMethodException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (SecurityException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IllegalArgumentException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InvocationTargetException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return hashedPassword;

    }

    @Override
    public void doEnrich(Action arg0) throws Exception
    {
        // TODO Auto-generated method stub

    }

}
