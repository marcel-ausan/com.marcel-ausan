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
import com.ullink.ulbridge2.ULMessage;
import com.ullink.ultools.tags.Tags;

public class enrichment_CFICodeMiFID2Resilience
{

    public void doEnrichment(ULMessage ulm) throws Exception
    {
        // retrieve the persisted CFICODE
        if (ulm.exist("#CFICODE") && ulm.exist(Tags.CFICODE) && !(ulm.getString("CFICODE").equals(ulm.getString("#CFICODE"))))
        {
            ulm.add(Tags.CFICODE, ulm.getString("#CFICODE"));
        }
        // persist cficode
        // change the cficode to old values
        else if (ulm.exist(Tags.CFICODE))
        {
            String cfiCode  = ulm.getString(Tags.CFICODE);
            ulm.add("#CFICODE", ulm.getString(Tags.CFICODE));
            if (cfiCode.startsWith("J"))
                ulm.add(Tags.CFICODE, "MMFXXX");
            if (cfiCode.startsWith("K"))
                ulm.add(Tags.CFICODE, "MRXXXX");
            if (cfiCode.startsWith("C") || cfiCode.startsWith("S") || cfiCode.startsWith("H") ||
                cfiCode.startsWith("I") || cfiCode.startsWith("L") || cfiCode.startsWith("T"))
                ulm.add(Tags.CFICODE, "MXXXXX");
        }
    }
}
