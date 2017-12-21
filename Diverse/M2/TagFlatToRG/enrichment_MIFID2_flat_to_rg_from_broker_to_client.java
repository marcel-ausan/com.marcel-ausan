//http://gitlab.ullink.lan/fasttrack/java-projects/tree/master/ulbridge/src/mifid2

import com.ullink.ulbridge2.ULMessage;
import com.ullink.ultools.tags.Tags;

import java.util.*;

public class enrichment_MIFID2_flat_to_rg_from_broker_to_client
{
    private static final Set<String> DelivertocompidList = new HashSet<>(Arrays
        .asList("1", "2", "3", "4"));

    private static final Map<String, String> TRDREGPUBLICATIONREASONS = new HashMap<String, String>()
    {
        {
            put("0", "TRDREGPUBLICATIONTYPE=pre-trade-transparency-waiver|TRDREGPUBLICATIONREASON=no-book-order-due-to-average-spread-price|");
            put("1", "TRDREGPUBLICATIONTYPE=pre-trade-transparency-waiver|TRDREGPUBLICATIONREASON=no-book-order-due-to-ref-price|");
            put("2", "TRDREGPUBLICATIONTYPE=pre-trade-transparency-waiver|TRDREGPUBLICATIONREASON=no-book-order-due-to-other-conditions|");
            put("3", "TRDREGPUBLICATIONTYPE=pre-trade-transparency-waiver|TRDREGPUBLICATIONREASON=no-public-price-due-to-ref-price|");
            put("4", "TRDREGPUBLICATIONTYPE=pre-trade-transparency-waiver|TRDREGPUBLICATIONREASON=no-public-price-due-to-illiquid|");
            put("5", "TRDREGPUBLICATIONTYPE=pre-trade-transparency-waiver|TRDREGPUBLICATIONREASON=no-public-price-due-to-order-size|");
            put("6", "TRDREGPUBLICATIONTYPE=post-trade-deferral|TRDREGPUBLICATIONREASON=deferral-due-to-large-in-scale|");
            put("7", "TRDREGPUBLICATIONTYPE=post-trade-deferral|TRDREGPUBLICATIONREASON=deferral-due-to-illiquid|");
            put("8", "TRDREGPUBLICATIONTYPE=post-trade-deferral|TRDREGPUBLICATIONREASON=deferral-due-to-size-specific|");
            put("9", "TRDREGPUBLICATIONTYPE=pre-trade-transparency-waiver|TRDREGPUBLICATIONREASON=no-public-price-due-to-large-in-scale|");
            put("10", "TRDREGPUBLICATIONTYPE=pre-trade-transparency-waiver|TRDREGPUBLICATIONREASON=no-public-price-due-to-order-hidden|");
        }
    };

    private static final Map<String, String> NOTRADEPRICECONDITIONS = new HashMap<String, String>()
    {
        {
            put("13", "TRADEPRICECONDITION=special-dividend|");
            put("14", "TRADEPRICECONDITION=price-improvement|");
            put("16", "TRADEPRICECONDITION=trade-exempted-from-trading-obligation|");
        }
    };

    private void processFIX20123(ULMessage ulm)
    {
        if (ulm.exist("FIX.20123"))
        {
            addPartyID(ulm, ulm.getString("FIX.20123"), "publishingintermediary", "mic");
            ulm.remove("FIX.20123");
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

    private void processFIX20063(ULMessage ulm)
    {
        if (ulm.exist("FIX.20063"))
        {
            addPartyID(ulm, ulm.getString("FIX.20063"), "systematicinternaliser", "mic");
            ulm.remove("FIX.20063");
        }
    }

    private void processFIX20073(ULMessage ulm)
    {
        if (ulm.exist("FIX.20073"))
        {
            addPartyID(ulm, ulm.getString("FIX.20073"), "executionvenue", "mic");
            ulm.remove("FIX.20073");
        }
    }

    private void processFIX20021(ULMessage ulm)
    {
        if (ulm.exist("FIX.20021"))
        {
            addPartyID(ulm, ulm.getString("FIX.20021"), "clearingorganization", "");
            ulm.remove("FIX.20021");
        }
    }

    private void processFIX20017(ULMessage ulm)
    {
        if (ulm.exist("FIX.20017"))
        {
            addPartyID(ulm, ulm.getString("FIX.20017"), "contrafirm", "");
            ulm.remove("FIX.20017");
        }
    }

    private void processFIX20022(ULMessage ulm)
    {
        if (ulm.exist("FIX.20022"))
        {
            addPartyID(ulm, ulm.getString("FIX.20022"), "exchange", "mic");
            ulm.remove("FIX.20022");
        }
    }

    private void processFIX8016(ULMessage ulm)
    {
        if (ulm.exist("FIX.8016"))
        {
            ulm.add("NOREGULATORYTRADEIDS", "1");
            ulm.add("NOREGULATORYTRADEIDS", "TRADEIDTYPE=trading-venue-transaction-identifier|TRADEID=" + ulm.getString("FIX.8016") + "|", 0);
            ulm.remove("FIX.8016");
        }
    }

    private void processFIX8014(ULMessage ulm)
    {
        if (ulm.exist("FIX.8014"))
        {
            String[] tradeFlags = ulm.getString("FIX.8014").split(" ");
            for (int i = 0; i < tradeFlags.length; i++)
            {
                ULMessage noTradePriceConditions = ULMessage.valueOf(NOTRADEPRICECONDITIONS.get(tradeFlags[i]));
                ulm.add("NOTRADEPRICECONDITIONS", noTradePriceConditions.toString(), i);
            }
            ulm.add("NOTRADEPRICECONDITIONS", tradeFlags.length);
            ulm.remove("FIX.8014");
        }
    }

    private void processFIX8013(ULMessage ulm)
    {
        if (ulm.exist("FIX.8013"))
        {
            String[] tradeFlags = ulm.getString("FIX.8013").split(" ");
            for (int i = 0; i < tradeFlags.length; i++)
            {
                ULMessage noTrdRegPublications = ULMessage.valueOf(TRDREGPUBLICATIONREASONS.get(tradeFlags[i]));
                ulm.add("NOTRDREGPUBLICATIONS", noTrdRegPublications.toString(), i);
            }
            ulm.add("NOTRDREGPUBLICATIONS", tradeFlags.length);
            ulm.remove("FIX.8013");
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
        if (partyIDSource != "")
            noPartyIDs.add(Tags.PARTYIDSOURCE, partyIDSource);
        ulm.add(Tags.NOPARTYIDS, noPartyIDs.toString(), nb);
        ulm.add(Tags.NOPARTYIDS, nb + 1);
    }

    public void doEnrichment(ULMessage ulm)
    {
        processFIX8013(ulm);
        processFIX8014(ulm);
        processFIX8016(ulm);

        processFIX20123(ulm);
        processFIX20001(ulm);
        processFIX20063(ulm);
        processFIX20073(ulm);
        processFIX20021(ulm);
        processFIX20017(ulm);
        processFIX20022(ulm);
    }
}
