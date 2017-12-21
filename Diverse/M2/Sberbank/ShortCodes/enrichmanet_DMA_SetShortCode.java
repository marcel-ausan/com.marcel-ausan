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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.ullink.oms.model.Client;
import com.ullink.ulbridge2.ULBridge;
import com.ullink.ulbridge2.ULMessage;
import com.ullink.ulbridge2.plugins.odisys.dataprovider.api.OdisysDataProvider;
import com.ullink.ulbridge2.plugins.odisys.dataprovider.api.ServiceKey;
import com.ullink.ulbridge2.plugins.odisys.dataprovider.api.services.OdisysDataService;
import com.ullink.ultools.NamingService;
import com.ullink.ultools.TraceFile;
import com.ullink.ultools.tags.Tags;

/*
 * MiFID2 requires short codes to be sent to the exchange.
 * This enrichment populates client and trader short code in ULMessage sent to DMA plugin
 */

public class enrichment_O_DMA_SetShortcode
{

    private static final String enrichmentName = "O_DMA_SetShortCode";
    private static final TraceFile trace = (TraceFile) NamingService.lookup(ULBridge.NAMING_TRACE);
    private static final OdisysDataProvider odisysDataProvider = (OdisysDataProvider) NamingService.lookup(OdisysDataProvider.NAMING_CTX_ID);
    private static final OdisysDataService<Client> clientCalendarDataService = odisysDataProvider.getDataService(ServiceKey.CLIENT);
   // private static final OdisysDataService<Trader> traderDataService = odisysDataProvider.getDataService(ServiceKey.TRADER);

    private static class ShortCode
    {
        /*
         * check if short code already exists in the message and UL should forward it to the exchange.
         * This function is used to determine if the code is already provided or not.
         */
        public static boolean isProvided(ULMessage ulm)
        {
            boolean shortcodeProvided = false;
            if (ulm.exist(Tags.NOPARTYIDS))
            {
                int noPartyIds = ulm.getInt(Tags.NOPARTYIDS);
                for (int i = 0; i < noPartyIds; i++)
                {
                    ULMessage leg = ULMessage.valueOf(ulm.getString("NOPARTYIDS", i));
                    String partyRole = leg.exist(Tags.PARTYROLE) ? leg.getString(Tags.PARTYROLE) : "";
                    String partyIdSource = leg.exist(Tags.PARTYIDSOURCE) ? leg.getString(Tags.PARTYIDSOURCE) : "";
                    if (partyRole.equals("clientid") && partyIdSource.equals("shortcodeidentifier"))
                    {
                        shortcodeProvided = true;
                    }
                }
            }
            return shortcodeProvided;
        }

        /*
         * setClientCode populates short codes for client based on the custom property on client in ODS.
         */
        public static void setClientCode(ULMessage ulm)
        {
            if (clientCalendarDataService != null)
            {
                String shortCode = null;
                Client client = clientCalendarDataService.get(ulm.getString("ULLINK.CLIENTID"));
                if (client.getCustomProperties() != null)
                    //shortcode custom property needs to be set on Client screen in UL Trader
                    shortCode = client.getCustomProperties().get("shortcode");
                if (shortCode != null)
                    addPartyId(ulm, shortCode, "shortcodeidentifier", "clientid", null);
            }
        }
    }

    public void doEnrichment(ULMessage ulm)
    {
        if(ShortCode.isProvided(ulm))
        {
            ShortCode.setClientCode(ulm);
        }
        if (ulm.exist("MIFID.TRADERSHORTCODE"))
        {
            addPartyId(ulm, ulm.getString("MIFID.TRADERSHORTCODE"), "shortcodeidentifier", "executingtrader", "natural-person");
        }
        if (ulm.exist("MIFID.TRADERGROUP"))
        {
            addPartyId(ulm, ulm.getString("MIFID.TRADERGROUP"), "proprietary/customcode", "deskid", null);
        }
        if (ulm.exist("MIFID.INVESTMENTDECISIONMAKER"))
        {
            addPartyId(ulm, ulm.getString("MIFID.INVESTMENTDECISIONMAKER"), "shortcodeidentifier", "investmentdecisionmaker", "natural-person");
        }
        
        
    }
    
    private static void investmentDecisionMakerExists(ULMessage ulm) {
        if (ulm.exist(Tags.NOPARTYIDS))
        {
            int noPartyIds = ulm.getInt(Tags.NOPARTYIDS);
            for (int i = 0; i < noPartyIds; i++)
            {
                ULMessage leg = ULMessage.valueOf(ulm.getString("NOPARTYIDS", i));
                if ("investmentdecisionmaker".equals(leg.getString(Tags.PARTYROLE)))
                {
                    ulm.add("MIFID.INVESTMENTDECISIONMAKER", leg.getString(Tags.PARTYID));
                    ULMessage emptyMsg = new ULMessage();
                    leg = emptyMsg;
                    ulm.add(Tags.NOPARTYIDS, leg.toString(), i);
                }
            }
        }
    }

    /*
     * Utility method to add PartyIds repeating group in ULMsg.
     */
    private static void addPartyId(ULMessage ulm, String id, String source, String role, String qualifier)
    {
        int nbRptGroup = 0;
        if (ulm.exist(Tags.NOPARTYIDS))
            nbRptGroup = ulm.getInt(Tags.NOPARTYIDS);

        ULMessage partyId = new ULMessage();
        partyId.add(Tags.PARTYID, id);
        if (source != null)
            partyId.add(Tags.PARTYIDSOURCE, source);
        if (qualifier != null)
            partyId.add("PARTYROLEQUALIFIER", qualifier);
        partyId.add(Tags.PARTYROLE, role);
        ulm.add(Tags.NOPARTYIDS, partyId.toString(), nbRptGroup++);
        ulm.add(Tags.NOPARTYIDS, nbRptGroup++);
    }

}
