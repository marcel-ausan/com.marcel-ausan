//http://gitlab.ullink.lan/fasttrack/java-projects/tree/master/ulbridge/src/mifid2

import com.ullink.ulbridge2.ULMessage;
import com.ullink.ulmessage.helpers.ULMessageBuilder;
import com.ullink.ultools.tags.Tags;

public class enrichment_MIFID2_rg_to_flat_from_client_to_broker
{
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
                String partyRoleQualifier = leg.exist("PARTYROLEQUALIFIER") ? leg.getString("PARTYROLEQUALIFIER") : "";

                // note: 452 = 123 is not available in the mifid templates
                // 20123
                // PartyRole(452) = 123 + PartyIDSource(447)+ PartyID(448) = the APA's MIC ==>> 20123 = value from 448
                if (partyRole.equals("publishingintermediary") && partyIdSource.equals("mic"))
                {
                    ulm.add("FIX.20123", partyId);
                    ulm.add("FIX.20072", partyId);
                }
                // 20003
                // PartyRole(452) = 3 (Client ID) + PartyIDSource(447) = N (LEI)/P(Short Code)/O(National ID) =>> 20003 = value from partyID
                else if (partyRole.equals("clientid") && (partyIdSource.equals("legalentityidentifier") || partyIdSource.equals("shortcodeidentifier") || partyIdSource.equals("interimidentifier")))
                {
                    ulm.add("FIX.20003", partyId);
                }
                // 20013
                // PartyRole(452) = 13 (orderoriginationfirm) + PartyIDSource(447) = N (LEI)/P(Short Code)/O(National ID) =>> 20003 = value from partyID
                else if (partyRole.equals("orderoriginationfirm") && (partyIdSource.equals("legalentityidentifier") || partyIdSource.equals("shortcodeidentifier") || partyIdSource.equals("interimidentifier")))
                {
                    String fix20013 = partyId;
                    if (fix20013.equals("0") || fix20013.equals("NONE"))
                        fix20013 = "0";
                    if (fix20013.equals("1") || fix20013.equals("AGGR"))
                        fix20013 = "1";
                    if (fix20013.equals("2") || fix20013.equals("PNAL"))
                        fix20013 = "2";

                    ulm.add("FIX.20013", fix20013);
                }
                // 20001
                // PartyRole(452) = 1 + PartyIDSource(447) = N + PartyID(448) = "client1" ==>> 20001 = client1
                else if (partyRole.equals("executingfirm") && partyIdSource.equals("legalentityidentifier"))
                {
                    ulm.add("FIX.20001", partyId);
                }
                // 20122
                else if (partyRole.equals("investmentdecisionmaker")
                    &&
                    ((partyRoleQualifier.equals("natural-person") && (partyIdSource.equals("nationalidofnaturalperson") || partyIdSource.equals("shortcodeidentifier")))
                        ||
                        (partyRoleQualifier.equals("algorithm") && (partyIdSource.equals("proprietary/customcode") || partyIdSource.equals("shortcodeidentifier")))))
                {
                    ulm.add("FIX.20122", partyId);
                }
                // 20012
                else if (partyRole.equals("executingtrader")
                    &&
                    ((partyRoleQualifier.equals("natural-person") && (partyIdSource.equals("nationalidofnaturalperson") || partyIdSource.equals("shortcodeidentifier")))
                        ||
                        (partyRoleQualifier.equals("algorithm") && (partyIdSource.equals("proprietary/customcode") || partyIdSource.equals("shortcodeidentifier")))))
                {
                    ulm.add("FIX.20012", partyId);
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

    private void processORDERATTRIBUTE(ULMessage ulm)
    {
        String fix8015 = "";
        if (ulm.exist("AGGREGATEDCLIENT") && ulm.getString("AGGREGATEDCLIENT").equals("yes"))
            fix8015 += "0 ";
        if (ulm.exist("PENDINGCLIENTALLOCATION") && ulm.getString("PENDINGCLIENTALLOCATION").equals("yes"))
            fix8015 += "1 ";
        if (ulm.exist("LIQUIDITYPROVISION") && ulm.getString("LIQUIDITYPROVISION").equals("yes"))
            fix8015 += "2 ";
        if (ulm.exist("RISKREDUCTION") && ulm.getString("RISKREDUCTION").equals("yes"))
            fix8015 += "3 ";
        if (ulm.exist("ALGORITHMICORDER") && ulm.getString("ALGORITHMICORDER").equals("yes"))
            fix8015 += "4 ";
        if (ulm.exist("SYSTEMATICINTERNALISER") && ulm.getString("SYSTEMATICINTERNALISER").equals("yes"))
            fix8015 += "5 ";
        if (ulm.exist("ALLEXECUTIONSSUBMITTEDTOAPA") && ulm.getString("ALLEXECUTIONSSUBMITTEDTOAPA").equals("yes"))
            fix8015 += "6 ";
        if (ulm.exist("ORDEREXECUTIONINSTRUCTEDBYCLIENT") && ulm.getString("ORDEREXECUTIONINSTRUCTEDBYCLIENT").equals("yes"))
            fix8015 += "7 ";
        if (ulm.exist("LARGEINSCALE") && ulm.getString("LARGEINSCALE").equals("yes"))
            fix8015 += "8 ";
        if (ulm.exist("HIDDEN") && ulm.getString("HIDDEN").equals("yes"))
            fix8015 += "9 ";

        if (fix8015 != "")
        {
            fix8015 = fix8015.substring(0, fix8015.length() - 1);
            ulm.add("FIX.8015", fix8015);
        }
    }

    public void doEnrichment(ULMessage ulm)
    {
        processNOPARTYIDS(ulm);
        processORDERATTRIBUTE(ulm);
    }
}
