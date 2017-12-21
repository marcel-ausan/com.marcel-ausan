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
import java.util.logging.Logger;
import com.ullink.oms.constants.ModelType;
import com.ullink.oms.extensions.api.data.service.UserDataService;
import com.ullink.oms.helpers.HelperRegistry;
import com.ullink.oms.model.Action;
import com.ullink.oms.model.User;
import com.ullink.oms.model.User.UserType;
import com.ullink.oms.workers.enrichment.impl.java.JavaActionEnricher;

public class UserCreation extends JavaActionEnricher
{

    private static UserDataService userHelper = (UserDataService) HelperRegistry.getInstance().getDataService(ModelType.user);

    static
    {
        createOrUpdateUser("admin", "admin", User.UserType.SYSTEM_ADMIN);
        createOrUpdateUser("ullink", "ullink", User.UserType.SALES);

    }

    private static void createOrUpdateUser(String userName, String password, UserType userType)
    {
        User user = new User();
        user.setId(userName);
        user.setPassword(tryToHashPassword(password));
        user.setApiRight(true);
        user.setAdminWebRight(true);
        user.setConfigManagerRight(true);
        user.setType(userType);
        user.setFailedLogins(0);
        user.setFailedLogins(0);
        user.setPasswordExpire(null);

        // user.setSupervisedUsers(getAllUsers());

        try
        {
            
            User existingUser = userHelper.get(userName, false);
            
            if (null == existingUser)
            {
                userHelper.create(user); // works on a 3.6 Odisys at least -- need to verify on earlier versions though
            }
            else
            {
                userHelper.update(user); // works on a 3.6 Odisys at least -- need to verify on earlier versions though
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

    }

}