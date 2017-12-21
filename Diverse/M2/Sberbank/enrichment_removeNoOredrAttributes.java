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
import java.util.ArrayList;
import java.util.Arrays;
import com.ullink.ulbridge2.ULMessage;
import com.ullink.ultools.tags.Tags;

public class enrichment_removeNoOrderAttributes
{

    public final ArrayList<String> TAGS_TO_BE_REMOVED = new ArrayList<String>(Arrays.asList("AGGREGATEDCLIENT", "PENDINGCLIENTALLOCATION", "LIQUIDITYPROVISION", "RISKREDUCTION", "ALGORITHMICORDER", "SYSTEMATICINTERNALISER",
        "ALLEXECUTIONSSUBMITTEDTOAPA", "ORDEREXECUTIONINSTRUCTEDBYCLIENT", "LARGEINSCALE", "HIDDEN"));

    public void doEnrichment(ULMessage ulm)
    {
        for (int i = 0; i < TAGS_TO_BE_REMOVED.size(); i++)
        {
            if (ulm.exist(TAGS_TO_BE_REMOVED.get(i)))
            {
                ulm.remove(TAGS_TO_BE_REMOVED.get(i));
            }
        }
    }
}
