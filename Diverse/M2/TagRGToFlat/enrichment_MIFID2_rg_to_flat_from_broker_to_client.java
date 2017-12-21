//http://gitlab.ullink.lan/fasttrack/java-projects/tree/master/ulbridge/src/mifid2

import com.ullink.ulbridge2.ULMessage;
import com.ullink.ulmessage.helpers.ULMessageBuilder;
import com.ullink.ultools.tags.Tags;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class enrichment_MIFID2_rg_to_flat_from_broker_to_client
{
    private static final Set<String> DelivertocompidList = new HashSet<>(Arrays
        .asList("1", "2", "3", "4"));

    private void processNOREGULATORYTRADEIDS(ULMessage ulm)
    {
        if (ulm.exist("NOREGULATORYTRADEIDS"))
        {
            int noRegulatoryTradeIds = ulm.getInt("NOREGULATORYTRADEIDS");
            ULMessageBuilder builder = new ULMessageBuilder();
            for (int i = 0; i < noRegulatoryTradeIds; i++)
            {
                ULMessage leg = ULMessage.valueOf(ulm.getString("NOREGULATORYTRADEIDS", i));
                String tradeIdType = leg.exist("TRADEIDTYPE") ? leg.getString("TRADEIDTYPE") : "";
                String tradeId = leg.exist("TRADEID") ? leg.getString("TRADEID") : "";
                // RegulatoryTradeIDType(1906) = 5 (Trading venue transaction identifier) ==>> 8016 = valueRegulatoryTradeID(1903)
                if (tradeIdType.equals("trading-venue-transaction-identifier"))
                {
                    ulm.add("FIX.8016", tradeId);
                }
                else
                {
                    builder.repeating("NOREGULATORYTRADEIDS", leg.toString());
                }
                ulm.remove("NOREGULATORYTRADEIDS[" + i + "]");
            }
            ulm.remove("NOREGULATORYTRADEIDS");
            ulm.addFromString(builder.toString());
        }

    }

    private void processNOPARTYIDS(ULMessage ulm)
    {
        if (ulm.exist(Tags.NOPARTYIDS))
        {
            int noPartyIds = ulm.getInt(Tags.NOPARTYIDS);
            ULMessageBuilder builder = new ULMessageBuilder();
            for (int i = 0; i < noPartyIds; i++)
            {
                ULMessage leg = ULMessage.valueOf(ulm.getString("NOPARTYIDS", i));
                String partyRole = leg.exist(Tags.PARTYROLE) ? leg.getString(Tags.PARTYROLE) : "";
                String partyIdSource = leg.exist(Tags.PARTYIDSOURCE) ? leg.getString(Tags.PARTYIDSOURCE) : "";
                String partyId = leg.exist(Tags.PARTYID) ? leg.getString(Tags.PARTYID) : "";

                // note: 452 = 123 is not available in the mifid templates
                // 20123
                // PartyRole(452) = 123 + PartyIDSource(447)+ PartyID(448) = the APA's MIC ==>> 20123 = value from 448
                if (partyRole.equals("publishingintermediary") && partyIdSource.equals("mic"))
                {
                    ulm.add("FIX.20123", partyId);
                    ulm.add("FIX.20072", partyId);
                }
                // 20001
                // PartyRole(452) = 1 + PartyIDSource(447) = N + PartyID(448) = "client1" ==>> 20001 =client1
                else if (partyRole.equals("executingfirm") && partyIdSource.equals("legalentityidentifier"))
                {
                    ulm.add("FIX.20001", partyId);
                }
                // 20063
                // PartyRole(452) = 63 + PartyIDSource(447) = G + PartyID(448) = "the mic" ==>> 20001 = "the mic"
                else if (partyRole.equals("systematicinternaliser") && partyIdSource.equals("mic"))
                {
                    ulm.add("FIX.20063", partyId);
                }
                // 20073
                // PartyRole(452) = 73 + PartyIDSource(447) = G + PartyID(448) = "the si mic" ==>> 20001 = "the si mic"
                else if (partyRole.equals("executionvenue") && partyIdSource.equals("mic"))
                {
                    ulm.add("FIX.20073", partyId);
                }
                // 20021
                // PartyRole(452) = 21 (Clearing organisation) ==>> 20021=448
                else if (partyRole.equals("clearingorganization"))
                {
                    ulm.add("FIX.20021", partyId);
                }
                // 20017
                // PartyRole(452) = 17 (Contra firm) ==>> 20017=448
                else if (partyRole.equals("contrafirm"))
                {
                    ulm.add("FIX.20017", partyId);
                }
                // 20022
                // PartyRole(452) = 22 (Exchange) + PartyIDSource(447) = G (MIC) + PartyID(448) = "thevenuemic" ==>> 20022 = thevenuemic
                else if (partyRole.equals("exchange") && partyIdSource.equals("mic"))
                {
                    ulm.add("FIX.20022", partyId);
                }
                else
                {
                    builder.repeating(Tags.NOPARTYIDS, leg.toString());
                }
                ulm.remove("NOPARTYIDS[" + i + "]");
            }
            ulm.remove("NOPARTYIDS");
            ulm.addFromString(builder.toString());
        }
    }

    private void processNOTRDREGPUBLICATIONS(ULMessage ulm)
    {
        if (ulm.exist("NOTRDREGPUBLICATIONS"))
        {
            int noTrdRegPublications = ulm.getInt("NOTRDREGPUBLICATIONS");
            ULMessageBuilder builder = new ULMessageBuilder();
            for (int i = 0; i < noTrdRegPublications; i++)
            {
                ULMessage leg = ULMessage.valueOf(ulm.getString("NOTRDREGPUBLICATIONS", i));
                String type = leg.exist("TRDREGPUBLICATIONTYPE") ? leg.getString("TRDREGPUBLICATIONTYPE") : "";
                String reasons = leg.exist("TRDREGPUBLICATIONREASON") ? leg.getString("TRDREGPUBLICATIONREASON") : "";

                // TrdRegPublicationType(2669) = 0 + TrdRegPublicationReason(2670) = 0 ==>> 8013 = 0(value from 2670)
                if (type.equals("pre-trade-transparency-waiver") && reasons.equals("no-book-order-due-to-average-spread-price"))
                {
                    ulm.add("FIX.8013", "0");
                }
                // TrdRegPublicationType(2669) = 0 + TrdRegPublicationReason(2670) =1 ==>> 8013 = 1(value from 2670)
                else if (type.equals("pre-trade-transparency-waiver") && reasons.equals("no-book-order-due-to-ref-price"))
                {
                    ulm.add("FIX.8013", "1");
                }
                // TrdRegPublicationType(2669) = 0 + TrdRegPublicationReason(2670) = 2 ==>> 8013=2(value from 2670)
                else if (type.equals("pre-trade-transparency-waiver") && reasons.equals("no-book-order-due-to-other-conditions"))
                {
                    ulm.add("FIX.8013", "2");
                }
                // TrdRegPublicationType(2669) = 0 + TrdRegPublicationReason(2670) = 3 ==>> 8013 = 3(value from 2670)
                else if (type.equals("pre-trade-transparency-waiver") && reasons.equals("no-public-price-due-to-ref-price"))
                {
                    ulm.add("FIX.8013", "3");
                }
                // TrdRegPublicationType(2669) = 0 + TrdRegPublicationReason(2670) = 4 ==>> 8013 = 4(value from 2670)
                else if (type.equals("pre-trade-transparency-waiver") && reasons.equals("no-public-price-due-to-illiquid"))
                {
                    ulm.add("FIX.8013", "4");
                }
                // TrdRegPublicationType(2669) = 0 + TrdRegPublicationReason(2670) = 5 ==>> 8013 = 5(value from 2670)
                else if (type.equals("pre-trade-transparency-waiver") && reasons.equals("no-public-price-due-to-order-size"))
                {
                    ulm.add("FIX.8013", "5");
                }
                // TrdRegPublicationType(2669) = 1 + TrdRegPublicationReason(2670) = 6 ==>> 8013 = 6(value from 2670)
                else if (type.equals("post-trade-deferral") && reasons.equals("deferral-due-to-large-in-scale"))
                {
                    ulm.add("FIX.8013", "6");
                }
                // TrdRegPublicationType(2669) = 1 + TrdRegPublicationReason(2670) = 7 ==>> 8013 = 7(value from 2670)
                else if (type.equals("post-trade-deferral") && reasons.equals("deferral-due-to-illiquid"))
                {
                    ulm.add("FIX.8013", "7");
                }
                // TrdRegPublicationType(2669) = 1 + TrdRegPublicationReason(2670) = 8 ==>> 8013 = 8(value from 2670)
                else if (type.equals("post-trade-deferral") && reasons.equals("deferral-due-to-size-specific"))
                {
                    ulm.add("FIX.8013", "8");
                }
                // TrdRegPublicationType(2669) = 0 + TrdRegPublicationReason(2670) = 9 ==>> 8013 = 9(value from 2670)
                else if (type.equals("pre-trade-transparency-waiver") && reasons.equals("no-public-price-due-to-large-in-scale"))
                {
                    ulm.add("FIX.8013", "9");
                }
                // TrdRegPublicationType(2669) = 0 + TrdRegPublicationReason(2670) = 10 ==>> 8013 = 10(value from 2670)
                else if (type.equals("pre-trade-transparency-waiver") && reasons.equals("no-public-price-due-to-order-hidden"))
                {
                    ulm.add("FIX.8013", "10");
                }
                else
                {
                    builder.repeating("NOTRDREGPUBLICATIONS", leg.toString());
                }
                ulm.remove("NOTRDREGPUBLICATIONS[" + i + "]");
            }
            ulm.remove("NOTRDREGPUBLICATIONS");
            ulm.addFromString(builder.toString());
        }

    }

    private void processNOTRADEPRICECONDITIONS(ULMessage ulm)
    {
        if (ulm.exist("NOTRADEPRICECONDITIONS"))
        {
            int noTrdRegPublications = ulm.getInt("NOTRADEPRICECONDITIONS");
            ULMessageBuilder builder = new ULMessageBuilder();
            for (int i = 0; i < noTrdRegPublications; i++)
            {
                ULMessage leg = ULMessage.valueOf(ulm.getString("NOTRADEPRICECONDITIONS", i));
                String priceConditions = leg.exist("TRADEPRICECONDITION") ? leg.getString("TRADEPRICECONDITION") : "";

                // TradePriceCondition(1839) ==>> 13
                if (priceConditions.equals("special-dividend"))
                {
                    ulm.add("FIX.8014", "13");
                }
                // TradePriceCondition(1839) ==>> 14
                else if (priceConditions.equals("price-improvement"))
                {
                    ulm.add("FIX.8014", "14");
                }
                // TradePriceCondition(1839) ==>> 16
                else if (priceConditions.equals("trade-exempted-from-trading-obligation"))
                {
                    ulm.add("FIX.8014", "16");
                }
                else
                {
                    builder.repeating("NOTRADEPRICECONDITIONS", leg.toString());
                }
                ulm.remove("NOTRADEPRICECONDITIONS[" + i + "]");
            }
            ulm.remove("NOTRADEPRICECONDITIONS");
            ulm.addFromString(builder.toString());
        }
    }

    public void doEnrichment(ULMessage ulm)
    {
        processNOPARTYIDS(ulm);
        processNOTRDREGPUBLICATIONS(ulm);
        processNOTRADEPRICECONDITIONS(ulm);
        processNOREGULATORYTRADEIDS(ulm);
    }
}
