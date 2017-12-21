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

import com.ullink.ulbridge2.*;
import com.ullink.ultools.tags.Tags;

/*
 * This enrichment was requested through ONBRD-15754 -> Primextend - Hidden orders ARLSEC
 */

public class enrichment_O_hidden_orders
{

    public void doEnrichment(ULMessage ulm)
    {
        if ("JPMULL".equals(ulm.getString(Tags.DELIVERTOCOMPID)) && "ALRSEC".equals(ulm.getString(Tags.ACCOUNT)) && "0".equals(ulm.getString(Tags.MAXFLOOR)))
        {
            if ("ARCA".equals(ulm.getString(Tags.EXDESTINATION)))
            {
                ulm.remove(Tags.MAXFLOOR);
                ulm.add(Tags.ORDTYPE, "limitorbetter");
                ulm.add(Tags.EXECINST, "5");
            }
            else if ("AMEX".equals(ulm.getString(Tags.EXDESTINATION)))
            {
                ulm.remove(Tags.MAXFLOOR);
                ulm.add(Tags.ORDTYPE, "limit");
                ulm.add(Tags.TIMEINFORCE, "day");
                ulm.add(Tags.EXECINST, "N");
            }
            else if ("NASDAQ".equals(ulm.getString(Tags.EXDESTINATION)))
            {
                ulm.remove(Tags.MAXFLOOR);
                ulm.add(Tags.EXECBROKER, "INET");
                //normalize-extra-tags = true for O_NYFIX_FIX42, so no cfb changes are needed
                ulm.add("FIX.9140", "N");
            }
            else if ("BATS".equals(ulm.getString(Tags.EXDESTINATION)) || "BYXX".equals(ulm.getString(Tags.EXDESTINATION)) || "EDGX".equals(ulm.getString(Tags.EXDESTINATION)) || "EDGA".equals(ulm.getString(Tags.EXDESTINATION)))
            {
                ulm.remove(Tags.MAXFLOOR);
                //normalize-extra-tags = true for O_NYFIX_FIX42, so no cfb changes are needed
                ulm.add("FIX.9479", "I");
            }
        }
    }
}