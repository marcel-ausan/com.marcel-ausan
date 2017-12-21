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

public class enrichment_DummyNoPartyIDs
{

    /*
     * For the upcoming LSE new format go live (20th of Nov2017), below 4 legs should be added into the NoPartyIDs repeating group
     * *** this enrichment should only be a temporary workaround
     */
    public void doEnrichment(ULMessage ulm)
    {
        ulm.add("MIFID.CLIENTIDSHORTCODE", "dummy value");
        ulm.add("MIFID.TRADERGROUP", "dummy value");
        ulm.add("MIFID.EXECUTINGTRADER", "dummy value");
        ulm.add("MIFID.INVESTMENTDECISIONMAKER", "dummy value");
        
        if(ulm.exist("MIFID.CLIENTIDSHORTCODE"))
        {
            addPartyID(ulm, ulm.getString("MIFID.CLIENTIDSHORTCODE"), "clientid", "shortcodeidentifier");
        }
        if (ulm.exist("MIFID.EXECUTINGTRADER"))
        {
            addPartyId(ulm, ulm.getString("MIFID.EXECUTINGTRADER"), "executingtrader", "shortcodeidentifier");
        }
        if (ulm.exist("MIFID.TRADERGROUP"))
        {
            addPartyId(ulm, ulm.getString("MIFID.TRADERGROUP"), "deskid", "proprietary/customcode");
        }
        if (ulm.exist("MIFID.INVESTMENTDECISIONMAKER"))
        {
            addPartyId(ulm, ulm.getString("MIFID.INVESTMENTDECISIONMAKER"), "investmentdecisionmaker", "shortcodeidentifier");
        } 
    }
    
    private void addPartyID(ULMessage ulm, String partyID, String partyRole, String partyIDSource)
    {
        int nb;
        if (ulm.exist(Tags.NOPARTYIDS))
            nb = ulm.getInt(Tags.NOPARTYIDS);
        else
            nb = 0;
        ULMessage noPartyIDs = new ULMessage();
        noPartyIDs.add(Tags.PARTYID, partyID);
        noPartyIDs.add(Tags.PARTYROLE, partyRole);
        noPartyIDs.add(Tags.PARTYIDSOURCE, partyIDSource);
        ulm.add(Tags.NOPARTYIDS, noPartyIDs.toString(), nb);
        ulm.add(Tags.NOPARTYIDS, nb + 1);
    }
}
