//http://gitlab.ullink.lan/fasttrack/java-projects/tree/master/ulbridge/src/mifid2

import com.ullink.ulbridge2.ULMessage;
import com.ullink.ultools.tags.Tags;

import java.util.HashMap;
import java.util.Map;

public class enrichment_MIFID2_flat_to_rg_from_client_to_broker
{

    private static final Map<String, String> ORDERATTRIBUTES = new HashMap<String, String>()
    {
        {
            put("0", "AGGREGATEDCLIENT");
            put("1", "PENDINGCLIENTALLOCATION");
            put("2", "LIQUIDITYPROVISION");
            put("3", "RISKREDUCTION");
            put("4", "ALGORITHMICORDER");
            put("5", "SYSTEMATICINTERNALISER");
            put("6", "ALLEXECUTIONSSUBMITTEDTOAPA");
            put("7", "ORDEREXECUTIONINSTRUCTEDBYCLIENT");
            put("8", "LARGEINSCALE");
            put("9", "HIDDEN");
        }
    };

    private void processFIX8015(ULMessage ulm)
    {
        if (ulm.exist("FIX.8015"))
        {
            String[] tradeFlags = ulm.getString("FIX.8015").split(" ");
            for (int i = 0; i < tradeFlags.length; i++)
            {
                String OrderAttribute = ORDERATTRIBUTES.get(tradeFlags[i]);
                ulm.add(OrderAttribute, "yes");
            }
            ulm.remove("FIX.8015");
        }
    }

    private void processFIX20123(ULMessage ulm)
    {
        if (ulm.exist("FIX.20123"))
        {
            addPartyID(ulm, ulm.getString("FIX.20123"), "publishingintermediary", "mic");
            ulm.remove("FIX.20123");
        }
    }

    private void processFIX20072(ULMessage ulm)
    {
        if (ulm.exist("FIX.20072"))
        {
            addPartyID(ulm, ulm.getString("FIX.20072"), "publishingintermediary", "mic");
            ulm.remove("FIX.20072");
        }
    }
    
    private void processFIX20003(ULMessage ulm)
    {
        if (ulm.exist("FIX.20003"))
        {
            addPartyID(ulm, ulm.getString("FIX.20003"), "clientid", "legalentityidentifier");
            ulm.remove("FIX.20003");
        }
    }

    private void processFIX20013(ULMessage ulm)
    {
        if (ulm.exist("FIX.20013"))
        {
            String fix20013 = ulm.getString("FIX.20013");
            if (fix20013.equals("0") || fix20013.equals("NONE"))
                fix20013 = "0";
            if (fix20013.equals("1") || fix20013.equals("AGGR"))
                fix20013 = "1";
            if (fix20013.equals("2") || fix20013.equals("PNAL"))
                fix20013 = "2";

            addPartyID(ulm, fix20013, "orderoriginationfirm", "legalentityidentifier");
            ulm.remove("FIX.20013");
        }
    }

    private void processFIX20001(ULMessage ulm)
    {
        if (ulm.exist("FIX.20001"))
        {
            addPartyID(ulm, ulm.getString("FIX.20001"), "executingfirm", "legalentityidentifier");
            ulm.remove("FIX.20001");
        }
    }

    //20122 - as per FIX Community guildelines "this requires the sender and receiver of the message to pre-agree the party id source"
    //20012 - as per FIX Community guildelines "this requires the sender and receiver of the message to pre-agree the party id source"

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
        if (partyIDSource != "")
            noPartyIDs.add(Tags.PARTYIDSOURCE, partyIDSource);
        ulm.add(Tags.NOPARTYIDS, noPartyIDs.toString(), nb);
        ulm.add(Tags.NOPARTYIDS, nb + 1);
    }

    public void doEnrichment(ULMessage ulm)
    {
        processFIX8015(ulm);

        processFIX20123(ulm);
        processFIX20072(ulm);
        processFIX20003(ulm);
        processFIX20013(ulm);
        processFIX20001(ulm);
    }
}
