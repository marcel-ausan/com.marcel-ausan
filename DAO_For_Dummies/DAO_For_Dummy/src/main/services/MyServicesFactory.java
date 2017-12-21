package services;

import java.util.StringTokenizer;

import com.ullink.ulbridge2.ClassRevision;

public class MyServicesFactory
{
    public static MyServices createMyServices()
    {
        MyServices services;

        if (getMajorRevision() >= 3)
        {
            services = new MyServicesDAO();
        }
        else
        {
            services = new MyServicesSqlLink();
        }

        services.load();
        return services;
    }

    // check ULBridge's major version
    public static int getMajorRevision()
    {
        int rev = 0;
        String revision = ClassRevision.getRevision();
        if (revision.equals("NO INFORMATION"))
        {
            rev = 0;
        }
        StringTokenizer st = new StringTokenizer(revision, ".");
        rev = Integer.parseInt(st.nextToken());
        return rev;
    }
}
