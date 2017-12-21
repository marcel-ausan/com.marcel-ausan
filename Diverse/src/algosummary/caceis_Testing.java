import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.BooleanUtils;
import com.ullink.ulbridge2.ULBridge;
import com.ullink.ulbridge2.ULMessage;
import com.ullink.ulbridge2.dao.BridgeSharedFileDAO;
import com.ullink.ulbridge2.dao.persistentobjects.route.Route;
import com.ullink.ulbridge2.dao.services.RouteServices;
import com.ullink.ulbridge2.dao.services.RouteServicesDAO;
import com.ullink.ulbridge2.dao.tables.route.RouteTable;
import com.ullink.ulbridge2.dao.transaction.TransactionServices;
import com.ullink.ulbridge2.dao.transaction.TransactionServicesDAO;
import com.ullink.ultools.NamingService;
import com.ullink.ultools.TraceFile;
import com.ullink.ultools.dao.core.api.Visitor;
import com.ullink.ultools.decimal.NumericHelper;
import com.ullink.ultools.tags.Tags;

public class enrichment_FIDESSA_NORMALIZATION
{

    private static final String enrichmentName = "FIDESSA_NORMALIZATION";
    private static final TraceFile trace = (TraceFile) NamingService.lookup(ULBridge.NAMING_TRACE);
    private static final RouteServices routeServices = new RouteServicesDAO((RouteTable) NamingService.lookup(BridgeSharedFileDAO.NAMING_ROUTE_TABLE));
    private static ULBridge ulbridge = (ULBridge) NamingService.lookup(ULBridge.NAMING_BRIDGE);
    private static final String OES_PLUGIN = "DC_FIDESSA_OES";
    private static final String AES_PLUGIN = "DC_FIDESSA_AES";
    TransactionServices transaction = new TransactionServicesDAO();

    public static Map<String, String> InstrumentTypeMap = new HashMap<String, String>()
    {
        {
            put("OC", "Option Call");
            put("OP", "Option Put");
            put("F", "Future");
            put("M", "Strategy");
            put("KT", "Strategy");
        }
    };

    private static List<String> TAG_TOCOPY = Arrays
        .asList("CFICODE", "CONTRACTMULTIPLIER", "CURRENCY", "EXCHANGECODE", "MATURITYDATE", "MATURITYMONTHYEAR", "RATIOQTY", "REFID", "REPORTID", "SECURITYDESC", "SECURITYEXCHANGE", "SECURITYTYPE", "SIDE", "STRIKEPRICE");

    public void doEnrichment(ULMessage ulm)
    {
        if (BooleanUtils.toBoolean(ulm.getString("TOBEFILTERED")))
            return;

        if ("allocation".equals(ulm.getString(Tags.MSGTYPE)))
        {
            //Change orderid in allocation to match the one in Odisys
            addCorrectSuffixtoClOrdId(ulm);

            //Add initial ULLINK.INSTRUMENTID
            addUllinkInstrumentId(ulm);

            //Add avg price in clearing account workaround for ONBRD-15391
            copyAvgPriceInClearingAccount(ulm);

            // Retrieve clientid from PARTYID
            addClientIdFromPartyIds(ulm);

            //Remove tag ORDERQTY for PODS-1860
            removeTagOrderQty(ulm);
        }
        else if ("executionreport".equals(ulm.getString(Tags.MSGTYPE)))
        {
            // Strategy handling => split the n-leg strat in n orders
            if (ulm.exist(Tags.MULTILEGREPORTINGTYPE) && "multileg".equals(ulm.getString(Tags.MULTILEGREPORTINGTYPE)))
            {
                if (ulm.exist("NOLEGSRETURN"))
                {

                    int nb_legs = ulm.getInt("NOLEGSRETURN");
                    info("nb_legs=" + nb_legs);
                    for (int i = 0; i < nb_legs; i++)
                    {
                        ULMessage current_leg = (ULMessage) ulm.clone();
                        ULMessage leg = ULMessage.valueOf(ulm.getString("NOLEGSRETURN", i));
                        info("get leg " + i + " =>" + leg.toString());
                        current_leg.add("MULTILEGREPORTINGTYPE", "new");

                        current_leg.add(Tags.SYMBOL, leg.getString("LEGEXCHANGECODE"));
                        current_leg.add(Tags.CLORDID, leg.getString("LEGREPORTID"));

                        current_leg.add("STRATEGYLEG", leg.getString(Tags.LEGREFID));
                        current_leg.add("STRATEGYSIDE", current_leg.getString(Tags.SIDE));
                        current_leg.add("CFI", leg.getString(Tags.LEGCFICODE));
                        current_leg.add("STRATEGYTYPE", ulm.exist("STRATEGYTYPE") ? ulm.getString("STRATEGYTYPE") : ulm.getString(Tags.CFICODE));

                        TAG_TOCOPY.stream().forEach(tag -> current_leg.add(tag, leg.getString("LEG" + tag)));

                        // manage ratio for strat on options
                        if (leg.exist(Tags.LEGRATIOQTY))
                        {
                            current_leg.add(Tags.ORDERQTY, leg.getBigDecimal(Tags.LEGRATIOQTY).multiply(current_leg.getBigDecimal(Tags.ORDERQTY)));
                        }

                        info("Current leg to push=" + current_leg.toString());
                        // Only father handling so route only from OES_PLUGIN
                        transaction.startTransaction();
                        ulbridge.routeMessage(OES_PLUGIN, "ODS_ReportFull_FIDESSA", current_leg);

                    }
                }
                ulm.add("TOBEFILTERED", "YES");
                return;
            }

            setReceiveMode(ulm);
            ManageSecurityAltId(ulm);
            setInstrumentType(ulm);

            // Retrieve ClientId Account TraderId
            retrieveClientIdAccountIdTraderId(ulm);

            if ("USd".equals(ulm.getString("CURRENCY")))
            {
                ulm.add("CURRENCY", "USC");
            }

            //Change CFI code to allow instrument resolution
            if ("future".equals(ulm.getString("SECURITYTYPE")) || ulm.getString(Tags.CFICODE).startsWith("F"))
            {
                ulm.add("CFICODE", "FXXXXX");
            }
            else if ("call".equals(ulm.getString("PUTORCALL")) || ulm.getString(Tags.CFICODE).startsWith("OC"))
            {
                ulm.add("CFICODE", "OCXXXX");
            }
            else if ("put".equals(ulm.getString("PUTORCALL")) || ulm.getString(Tags.CFICODE).startsWith("OP"))
            {
                ulm.add("CFICODE", "OPXXXX");
            }

            //XEUR received from Fidessa is SECURITYID=FESX_SU5Z5
            // referential in odisys is FESXU5-FESX-Z5
            if (("XEUR".equals(ulm.getString("EXDESTINATION")) || "IFUS".equals(ulm.getString("EXDESTINATION")) || "IFLL".equals(ulm.getString("EXDESTINATION")) || "IFLX".equals(ulm.getString("EXDESTINATION")) || "IFLO"
                .equals(ulm.getString("EXDESTINATION"))) && "multileg".equals(ulm.getString("MULTILEGREPORTINGTYPE")))
            {

                String symbol = ulm.getString("SECURITYID");
                String[] tab = symbol.split("_");

                if (tab != null && tab.length == 2)
                {
                    String leg1 = tab[0] + tab[1].substring(1, 3);
                    String leg2 = tab[0] + tab[1].substring(3, 5);

                    if (leg1 != null && leg2 != null)
                    {
                        ulm.add("SECURITYID", leg1 + "-" + leg2);
                    }
                }
            }
            else if ("XCME".equals(ulm.getString("EXDESTINATION")) && ulm.getString("SECURITYID") != null && "multileg".equals(ulm.getString("MULTILEGREPORTINGTYPE")))
            {
                //change GE_SGE_PK_01Y_Z5 to GE:PK 01Y Z5
                String securityId = ulm.getString("SECURITYID");

                if (securityId.contains("_SGE_"))
                {
                    ulm.add("SECURITYID", securityId.replace("_SGE_", ":").replace("_", " "));
                }

            }
            else if ("IFEU".equals(ulm.getString("EXDESTINATION")) && ulm.getString("SYMBOL") != null)
            {
                String symbol = ulm.getString("SYMBOL");

                if (symbol.endsWith(" Comdty"))
                {
                    ulm.add("SYMBOL", symbol.replace(" Comdty", ""));
                }
            }
            else if (ulm.getString("SECURITYID") != null && ulm.getString("SYMBOL") != null && "multileg".equals(ulm.getString("MULTILEGREPORTINGTYPE")))
            {
                String symbol = ulm.getString("SYMBOL");
                String securityId = ulm.getString("SECURITYID");

                if (securityId.startsWith(symbol + "_S"))
                {
                    ulm.add("SECURITYID", securityId.replace(symbol + "_S", ""));
                }
            }

            //Adding suffix to REPORT_PARENTORDER_CLORDID to match initial order ONBRD-15392
            if (ulm.getString("REPORT_PARENTORDER_CLORDID") != null)
            {
                String origParentOrderId = ulm.getString("REPORT_PARENTORDER_CLORDID");
                String newParentOrderId = ("DMA".equals(ulm.getString("FIDESSA_USERDISPLAYNAME"))) ? origParentOrderId : retrieveClOrdIdSuffix(origParentOrderId);
                ulm.add("REPORT_PARENTORDER_CLORDID", newParentOrderId);
            }

            //Change exec type for trade cancel as Fidessa not FIX compliant ONBRD-15393
            if ("executionreport".equals(ulm.getString(Tags.MSGTYPE)) && "EXEC_CXCL".equals(ulm.getString("CUSTOMEXECTYPE")))
            {
                ulm.add(Tags.EXECTYPE, "tradecancel");
            }

            if ("trade".equals(ulm.getString(Tags.EXECTYPE)) && "leg".equals(ulm.getString(Tags.MULTILEGREPORTINGTYPE)))
            {
                ulm.add("CLORDID", ulm.getString("LEGREPORTID"));
            }
        }
    }

    private void removeTagOrderQty(ULMessage ulm)
    {
        if (ulm.getString(Tags.NOORDERS) == null)
            return;

        int i;
        int nbNoOrders = Integer.parseInt(ulm.getString(Tags.NOORDERS));
        for (i = 0; i < nbNoOrders; i++)
        {
            ULMessage currentRptGroupIteration = ULMessage.valueOf(ulm.getString(Tags.NOORDERS, i));
            if (currentRptGroupIteration.getString(Tags.ORDERQTY) != null)
                currentRptGroupIteration.remove(Tags.ORDERQTY);

            ulm.add(Tags.NOORDERS, currentRptGroupIteration.toString(), i);
        }
    }

    private String retrieveClOrdIdSuffix(String clOrdId)
    {
        String correctClOrdId = null;
        Route route = null;

        //Check for correct iteration number as Fidessa send .i at the end of the ClOrdId
        int i = 1;
        route = getRoute(clOrdId + "." + Integer.toString(i));

        // by default, iteration=1
        if (route == null)
            return clOrdId + ".1";

        while (route != null)
        {
            i++;
            route = getRoute(clOrdId + "." + Integer.toString(i));
        }

        correctClOrdId = clOrdId + "." + Integer.toString(i - 1);

        //Check if order has been canceled
        if (getRoute(correctClOrdId + ".C") != null)
            correctClOrdId = correctClOrdId + ".C";

        return correctClOrdId;
    }

    private void addClientIdFromPartyIds(ULMessage ulm)
    {
        if (ulm.exist("NOPARTYIDS"))
        {
            int noPartyIDs = ulm.getInt("NOPARTYIDS");
            for (int i = 0; i < noPartyIDs; i++)
            {
                ULMessage nopartyid = ULMessage.valueOf(ulm.getString(Tags.NOPARTYIDS, i));
                if ("orderoriginationfirm".equals(nopartyid.getString("PARTYROLE")))
                {
                    ulm.add("ULLINK.CLIENTID", nopartyid.getString("PARTYID"));
                    return;
                }
            }
        }
    }

    private void setInstrumentType(ULMessage ulm)
    {
        String INSTRUMENTTYPE = ulm.getString("INSTRUMENTTYPE");

        if (!ulm.exist("CFI"))
            ulm.add("CFI", INSTRUMENTTYPE);

        String strategy_indicator = Character.toString(INSTRUMENTTYPE.charAt(1));
        String category_and_group = INSTRUMENTTYPE.substring(0, 2);
        String asset_indicator = Character.toString(INSTRUMENTTYPE.charAt(0));

        if ("KT".equals(category_and_group))
        {
            ulm.add("INSTRUMENTTYPE", InstrumentTypeMap.get(category_and_group));
        }
        else if ("M".equals(strategy_indicator))
        {
            ulm.add("INSTRUMENTTYPE", InstrumentTypeMap.get(strategy_indicator));
        }
        else if ("O".equals(asset_indicator))
        {
            ulm.remove("STRATEGYTYPE");
            ulm.add("INSTRUMENTTYPE", InstrumentTypeMap.get(INSTRUMENTTYPE.substring(0, 2)));
        }
        else
        {
            ulm.remove("STRATEGYTYPE");
            ulm.add("INSTRUMENTTYPE", InstrumentTypeMap.get(INSTRUMENTTYPE.substring(0, 1)));
        }
    }

    private void ManageSecurityAltId(ULMessage ulm)
    {
        if (ulm.getString(Tags.NOSECURITYALTID) == null)
            return;

        int nbNoSecurityAltId = Integer.parseInt(ulm.getString(Tags.NOSECURITYALTID));
        int i = 0;

        while (i < nbNoSecurityAltId)
        {
            ULMessage currentNoSecurityAltId = ULMessage.valueOf(ulm.getString(Tags.NOSECURITYALTID, i));

            if ("FIM".equals(currentNoSecurityAltId.getString(Tags.SECURITYALTIDSOURCE)) && currentNoSecurityAltId.getString(Tags.SECURITYALTID) != null)
            {
                ulm.add("STRATEGYID", currentNoSecurityAltId.getString(Tags.SECURITYALTID));
            }
            else if ("exchange-symbol".equals(currentNoSecurityAltId.getString(Tags.SECURITYALTIDSOURCE)) && currentNoSecurityAltId.getString(Tags.SECURITYALTID) != null)
            {
                ulm.add("EXCHANGECODE", currentNoSecurityAltId.getString(Tags.SECURITYALTID));
            }
            i++;
        }
    }

    private void setReceiveMode(ULMessage ulm)
    {
        /*
            8101 / WorkflowInd for ReceiveMode
        8101=DLA or 8101=CARE or 8101=ALGO : Electronic
        8101=FTW or 8101 absent : Manual"

         */


        /*
            8101 / WorkflowInd for HandleMode
        8101=DLA : Direct
        8101=CARE : Manual
        8101=ALGO : Automated
        8101=FTW or 8101 absent : Manual
         */

        String ReceiveMode;
        String HandleMode;
        String BusinessLine;
        // will be done odisys side BusinessLine;

        switch (ulm.exist("RECEIVEMODE") ? ulm.getString("RECEIVEMODE") : "Manual")
        {
            case "DLA":
                ReceiveMode = "Electronic";
                HandleMode = "Direct";
                BusinessLine = "Electronic";
                break;
            case "CARE":
                ReceiveMode = "Electronic";
                HandleMode = "Manual";
                BusinessLine = "Care";
                break;
            case "ALGO":
                ReceiveMode = "Electronic";
                HandleMode = "Automated";
                BusinessLine = "Electronic";
                break;
            case "FTW":
                ReceiveMode = "Manual";
                HandleMode = "Manual";
                BusinessLine = "Care";
                break;
            default:
                ReceiveMode = "Manual";
                HandleMode = "Manual";
                BusinessLine = "Care";
                break;
        }
        ulm.add("RECEIVEMODE", ReceiveMode);
        ulm.add("HANDMODE", HandleMode);
        ulm.add("BUSINESSLINE", BusinessLine);

    }

    private void copyAvgPriceInClearingAccount(ULMessage ulm)
    {
        HashMap<String, AllocConsideration> allocConsiderationHashMap = new HashMap<String, AllocConsideration>();
        String allocalgo = ulm.getString("ALLOCALGO");

        int nbNoAllocs = Integer.parseInt(ulm.getString(Tags.NOALLOCS));
        int i = 0;
        while (i < nbNoAllocs)
        {
            ULMessage currentNoAlloc = ULMessage.valueOf(ulm.getString(Tags.NOALLOCS, i));

            // Doing this only if ALLOCALGO = 2 - Average Price
            // recompute average price for ALLOCALGO = 5 - Near Average

            if ("2".equals(allocalgo))
            {
                String allocAvgPx = currentNoAlloc.getString("ALLOCAVGPX");
                String allocid = currentNoAlloc.getString("INDIVIDUALALLOCID");

                int nbNoPartyIds = 0;
                if (currentNoAlloc.getString(Tags.NOPARTYIDS) != null)
                    nbNoPartyIds = Integer.parseInt(currentNoAlloc.getString(Tags.NOPARTYIDS));

                addNoPartyId(currentNoAlloc, allocid + "/" + allocAvgPx, "clearingaccount", nbNoPartyIds);
                currentNoAlloc.add(Tags.NOPARTYIDS, String.valueOf(nbNoPartyIds + 1));

                ulm.add(Tags.NOALLOCS, String.valueOf(currentNoAlloc), i);
            }
            else if ("5".equals(allocalgo))
            {

                String allocAcc = currentNoAlloc.getString(Tags.ALLOCACCOUNT);

                currentNoAlloc.add("ALLOC_CONSIDERATION", currentNoAlloc.getBigDecimal(Tags.ALLOCPRICE).multiply(currentNoAlloc.getBigDecimal(Tags.ALLOCQTY)));

                if (allocConsiderationHashMap.containsKey(allocAcc))
                {
                    allocConsiderationHashMap.get(allocAcc).addConsideration(currentNoAlloc.getBigDecimal("ALLOC_CONSIDERATION"));
                    allocConsiderationHashMap.get(allocAcc).addQuantity(currentNoAlloc.getBigDecimal(Tags.ALLOCQTY));
                }
                else
                {
                    allocConsiderationHashMap.put(allocAcc, new AllocConsideration(currentNoAlloc.getBigDecimal("ALLOC_CONSIDERATION"), currentNoAlloc.getBigDecimal(Tags.ALLOCQTY)));
                }
                ulm.add(Tags.NOALLOCS, String.valueOf(currentNoAlloc), i);
            }
            i++;
        }

        if ("5".equals(allocalgo))
        {
            i = 0;
            while (i < nbNoAllocs)
            {
                ULMessage currentNoAlloc = ULMessage.valueOf(ulm.getString(Tags.NOALLOCS, i));
                String allocAcc = currentNoAlloc.getString(Tags.ALLOCACCOUNT);

                int nbNoPartyIds = 0;
                if (currentNoAlloc.getString(Tags.NOPARTYIDS) != null)
                    nbNoPartyIds = Integer.parseInt(currentNoAlloc.getString(Tags.NOPARTYIDS));

                addNoPartyId(currentNoAlloc, "/" + allocConsiderationHashMap.get(allocAcc).average(), "clearingaccount", nbNoPartyIds);
                currentNoAlloc.add(Tags.NOPARTYIDS, String.valueOf(nbNoPartyIds + 1));
                currentNoAlloc.add("ALLOC_AVERAGE", allocConsiderationHashMap.get(allocAcc).average());

                ulm.add(Tags.NOALLOCS, String.valueOf(currentNoAlloc), i);
                i++;
            }
        }
    }

    private class AllocConsideration
    {
        BigDecimal consideration;
        BigDecimal quantity;

        void addConsideration(BigDecimal b)
        {
            this.consideration = this.consideration.add(b);
        }

        void addQuantity(BigDecimal b)
        {
            this.quantity = this.quantity.add(b);
        }

        BigDecimal average()
        {
            return NumericHelper.divide(this.consideration, this.quantity);
        }

        AllocConsideration(BigDecimal cons, BigDecimal qty)
        {
            this.consideration = cons;
            this.quantity = qty;
        }
    }

    private void addUllinkInstrumentId(ULMessage ulm)
    {
        String clOrdId = getClOrdId(ulm);
        Route route = getRoute(clOrdId);
        if (route == null)
        {
            // ? strat mode :
            String fidessaOrderId = clOrdId.substring(0, clOrdId.indexOf("."));
            routeServices.visitAllFromSessionName("ODS_Reportfull_FIDESSA", new Visitor<Route>()
            {
                @Override
                public void visit(Route route)
                {

                    info("route=" + route.getOrigKey() + " getExchangeKey=" + route.getExchangeKey() + " ?=? " + fidessaOrderId);
                    if (route.getExchangeKey().equals(fidessaOrderId))
                    {
                        List<String> matching_tags = new ArrayList(Arrays.asList(Tags.CFICODE, Tags.MATURITYMONTHYEAR, Tags.SYMBOL, Tags.SIDE));

                        String CFICODE = route.getULMsg().getString(Tags.CFICODE);
                        String MATURITYMONTHYEAR = route.getULMsg().getString(Tags.MATURITYMONTHYEAR);
                        String SYMBOL = route.getULMsg().getString(Tags.SYMBOL);
                        String SIDE = route.getULMsg().getString(Tags.SIDE);
                        info("info in route =" + CFICODE + "/" + MATURITYMONTHYEAR + "/" + SYMBOL + "/" + SIDE);

                        if (ulm.exist(Tags.CFICODE) && ulm.getString(Tags.CFICODE).startsWith("O"))
                        {
                            matching_tags.add(Tags.STRIKEPRICE);
                            info("info in route strikeprice =" + route.getULMsg().getString(Tags.STRIKEPRICE));
                        }

                        if (matching_tags.stream()
                            .allMatch(tag -> route.getULMsg().getString(tag).equals(ulm.getString(tag))))
                        {
                            ulm.add(Tags.ULLINK.INSTRUMENTID, route.getULMsg().getString(Tags.ULLINK.INSTRUMENTID));

                            // replace CLORDID by leg orderid
                            ULMessage currentNoOrders = ULMessage.valueOf(ulm.getString(Tags.NOORDERS, 0));
                            currentNoOrders.add(Tags.CLORDID, route.getOrigKey());
                            ulm.add(Tags.NOORDERS, String.valueOf(currentNoOrders), 0);

                        }
                    }
                }
            });

            if (!ulm.exist(Tags.ULLINK.INSTRUMENTID))
            {
                warning("No route available, can't find ULLINK.INSTRUMENTID");
            }
        }
        else
        {
            String ullinkInstrumentId = searchTagInRoute(route, "ULLINK.INSTRUMENTID");
            if (!"".equals(ullinkInstrumentId))
                ulm.add(Tags.ULLINK.INSTRUMENTID, ullinkInstrumentId);
        }
    }

    private String getClOrdId(ULMessage ulm)
    {
        int nbNoOrders = Integer.parseInt(ulm.getString(Tags.NOORDERS));
        int i = 0;
        String newClOrdId = null;
        while (i < nbNoOrders)
        {
            ULMessage currentNoOrders = ULMessage.valueOf(ulm.getString(Tags.NOORDERS, i));
            if (currentNoOrders.getString(Tags.CLORDID) != null)
            {
                String currentClOrdId = currentNoOrders.getString(Tags.CLORDID);
                newClOrdId = currentClOrdId;
            }
            i++;
        }
        return newClOrdId;
    }

    private void addCorrectSuffixtoClOrdId(ULMessage ulm)
    {
        int nbNoOrders = Integer.parseInt(ulm.getString(Tags.NOORDERS));
        int i = 0;
        while (i < nbNoOrders)
        {
            ULMessage currentNoOrders = ULMessage.valueOf(ulm.getString(Tags.NOORDERS, i));

            if (currentNoOrders.getString(Tags.CLORDID) != null)
            {
                String currentClOrdId = currentNoOrders.getString(Tags.CLORDID);
                String newClOrdId = retrieveClOrdIdSuffix(currentClOrdId);
                currentNoOrders.add(Tags.CLORDID, newClOrdId);
                ulm.add(Tags.NOORDERS, String.valueOf(currentNoOrders), i);
            }
            i++;
        }
    }

    private void retrieveClientIdAccountIdTraderId(ULMessage ulm)
    {
        if (!ulm.getString(Tags.NOPARTYIDS).isEmpty())
        {
            int i = 0;
            int noPartyIds = Integer.parseInt(ulm.getString(Tags.NOPARTYIDS));

            while (i < noPartyIds)
            {
                String rptGroup = ulm.getString(Tags.NOPARTYIDS, i);
                ULMessage noPartyIdGroup = ULMessage.valueOf(rptGroup);

                if (noPartyIdGroup.getString(Tags.PARTYROLE) != null && "clientid".equals(noPartyIdGroup.getString(Tags.PARTYROLE)))
                {
                    String clientId = noPartyIdGroup.getString(Tags.PARTYID);
                    if (clientId != null)
                    {
                        ulm.add(Tags.CLIENTID, clientId);
                    }
                }

                // Username of the FIDESSA GTP user
                if (noPartyIdGroup.getString(Tags.PARTYROLE) != null && "orderoriginationtrader".equals(noPartyIdGroup.getString(Tags.PARTYROLE)))
                {
                    String traderId = noPartyIdGroup.getString(Tags.PARTYID);
                    if (traderId != null)
                    {
                        ulm.add("SALESTRADER", traderId);
                    }
                }

                //Mapping ULLINK.ACCOUNTID from repeating group NOPARTYIDS ONBRD-15577
                if (noPartyIdGroup.getString(Tags.PARTYROLE) != null && "customeraccount".equals(noPartyIdGroup.getString(Tags.PARTYROLE)))
                {
                    String account = noPartyIdGroup.getString(Tags.PARTYID);
                    if (account != null)
                    {
                        ulm.add(Tags.ULLINK.ACCOUNTID, account);
                    }
                }
                i++;
            }
        }
    }

    private Route getRoute(String clOrdId)
    {
        return routeServices.searchRouteFromOrigKey(clOrdId, "ODS_Reportfull_FIDESSA");
    }

    private Route getRouteFromExchangeKey(String key)
    {
        return routeServices.searchRouteFromExchangeKeyAndFromSessionName(key, "ODS_Reportfull_FIDESSA");
    }

    private String searchTagInRoute(com.ullink.ulbridge2.dao.persistentobjects.route.Route route, String tag)
    {
        String value = "";
        if (route != null)
        {
            ULMessage routeMessage = route.getULMsg();
            if (routeMessage.exist(tag))
            {
                value = routeMessage.getString(tag);
            }
        }
        return value;
    }

    private ULMessage addNoPartyId(ULMessage ulm, String partyid, String partyRole, int numberOfNoPartyIDs)
    {
        ULMessage noPartyIDs = new ULMessage();
        noPartyIDs.add(Tags.PARTYROLE, partyRole);
        noPartyIDs.add(Tags.PARTYID, partyid);
        ulm.add(Tags.NOPARTYIDS, noPartyIDs.toString(), numberOfNoPartyIDs);

        return ulm;
    }

    private void info(String message)
    {
        trace.add(enrichmentName, TraceFile.INFO, message);
    }

    private void warning(String message)
    {
        trace.add(enrichmentName, TraceFile.WARNING, message);
    }
}
