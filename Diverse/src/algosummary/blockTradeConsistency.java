import java.math.BigDecimal;
import java.math.MathContext;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import com.ullink.oms.ServiceRegistry;
import com.ullink.oms.ULOdisysApi;
import com.ullink.oms.apihelpers.model.customproperties.CustomPropertiesHelper;
import com.ullink.oms.apihelpers.model.instrument.InstrumentHelper;
import com.ullink.oms.apihelpers.model.instrument.InstrumentHelperFactory;
import com.ullink.oms.constants.ModelType;
import com.ullink.oms.extensions.api.data.dao.DAOException;
import com.ullink.oms.extensions.api.data.service.DataService;
import com.ullink.oms.extensions.apihelpers.util.service.ServerServices;
import com.ullink.oms.fees.api.model.CalcFee;
import com.ullink.oms.fees.api.model.charge.Charge;
import com.ullink.oms.fees.api.model.commission.Commission;
import com.ullink.oms.helpers.BusinessCalendarDataServiceImpl;
import com.ullink.oms.helpers.CalendarPhaseHelper;
import com.ullink.oms.helpers.ChangeRateDataServiceImpl;
import com.ullink.oms.helpers.ExchangeDataServiceImpl;
import com.ullink.oms.helpers.InstrumentDataServiceImpl;
import com.ullink.oms.helpers.OrderDataServiceImpl;
import com.ullink.oms.helpers.UserDataServiceImpl;
import com.ullink.oms.middle.api.actions.blocktrade.ActionBlockTrade;
import com.ullink.oms.middle.api.actions.blocktrade.ActionBlockTradeCreate;
import com.ullink.oms.middle.api.actions.blocktrade.ActionBlockTradeReplace;
import com.ullink.oms.middle.api.model.BlockConfirm;
import com.ullink.oms.middle.api.model.BlockTrade;
import com.ullink.oms.middle.api.model.Warehouse;
import com.ullink.oms.middle.api.model.enums.AgreedPriceType;
import com.ullink.oms.middle.api.model.ref.ConsiderationRef;
import com.ullink.oms.middle.apihelper.model.enums.AllocationLevelConstant;
import com.ullink.oms.middle.extension.common.services.SettlementDateService;
import com.ullink.oms.model.Action;
import com.ullink.oms.model.Exchange;
import com.ullink.oms.model.Instrument;
import com.ullink.oms.model.Order;
import com.ullink.oms.model.Quantity;
import com.ullink.oms.model.User;
import com.ullink.oms.model.calendar.Businesscalendar;
import com.ullink.oms.model.interfaces.Dataable;
import com.ullink.oms.model.interfaces.ObjectWithCustomProperties;
import com.ullink.oms.model.tools.DataManager;
import com.ullink.oms.osgi.common.ExtensionsGovernor;
import com.ullink.oms.transformer.XmlTransformer;
import com.ullink.oms.transformer.xpath.Condition;
import com.ullink.oms.transformer.xpath.ConditionHelper;

@SuppressWarnings("deprecation")
public class blockTradeConsistency
{
    private static Logger                logger                         = Logger.getLogger("blocktradeConsistency");
    private static SettlementDateService settlementDateService;
    private static final String          WHICH_MIDDLE                   = "MIDDLE";
    public static final List<String>     AUTHORIZED_CUSTOMPROP_TOADD    = Arrays.asList("FR_FTT_EXEMPT_CASE", "IT_FTT_EXEMPT_CASE", "ACCOUNTING_CCY", "SBIDATE", "BASKETID");
    public static final List<String>     TTFEXEMPTED_VALUES             = Arrays.asList("AV", "EXO_COMPTE_TECH", "EXO_PNA", "EXO_PSI", "EXO1_MARCHE_PRIM", "EXO2_LCH_EOC", "EXO3_TENUE_MARCH", "EXO4_CTR_LIQUID", "EXO5_INTRAGROUPE",
                                                                            "EXO6_CESSION_TEM",
                                                                            "EXO7_FCPE_SAL", "EXO8_EPARGNE_SAL", "EXO9_OBLIGATIONS");
    public static final List<String>     CUSTOMPROPERTIES_ORDER         = Arrays.asList("BUSINESSLINE", "PRESTATION", "OTC", "ULMIDDLE.ACTIVITY", "REGISTRAR", "EXASSTYPE", WHICH_MIDDLE, "REF_CLIENT", "REF_CI", "SRD", "BASKETID");
    public static final List<String>     IGNORE_CUSTOMPROP              = Arrays.asList("STP", "SAVE_TIME_ORIG");

    public static final List<String>     MANDATORY_RVA_CUSTOMPROPERTIES = Arrays.asList("PLACECOT", "PAYSCOT", "ul-core.primary-settlement-venue-id");

    private static String                CMCICS_CLIENTID                = "CMCICS";

    InstrumentHelper                     instrumentHelper               = new InstrumentHelperFactory(new ULOdisysApi(null).getLibraryVersion()).create();

    private SettlementDateService getSettlementService()
    {
        if (settlementDateService == null)
        {
            ExtensionsGovernor extensionsGovernor = ServiceRegistry.getInstance().getService(ExtensionsGovernor.class);
            settlementDateService = (SettlementDateService) extensionsGovernor.getRegisteredServices().get(SettlementDateService.class.getName());
        }
        return settlementDateService;
    }

    Map<ModelType, DataService> dataServices = new HashMap<ModelType, DataService>();

    private void initDataServices(ModelType modelType)
    {
        if (dataServices.get(modelType) == null)
            dataServices.put(modelType, ServerServices.getInstance().getDataServiceRegistry().getDataService(modelType));
    }

    public void doEnrich(Action action) throws Exception
    {
        logger.info("Enrichment starts");
        ActionBlockTrade actionblocktrade = (ActionBlockTrade) action;
        logger.info("/******/");
        logger.info("Enrichment will treat this action : " + XmlTransformer.transformActionToString(action));
        logger.info("/*****/");

        if (actionblocktrade.getBlockTrade() != null)
        {
            String current_user = actionblocktrade.getUserId(); //PATURERO
            User user = UserDataServiceImpl.getInstance().get(current_user); //PATURERO

            if (!user.isConsumerOfLicenseToken())
                return;

            initDataServices(BlockTrade.MODEL_TYPE);

            if (actionblocktrade instanceof ActionBlockTradeCreate)
            {
                // no check on revert netting
                if (actionblocktrade.getBlockTrade() != null
                    && actionblocktrade.getBlockTrade().getStates() != null
                    && (actionblocktrade.getBlockTrade().getStates().contains("revert-netting") || actionblocktrade.getBlockTrade().getStates().contains("-netted")))
                    return;

                // GB - ONBRD-7597 - Netted blocks are sent with a very limited action - below checks should have been beforehand.
                // <action id="act20150821-093451-424-00" userid="jerome" type="blocktrade.create" nettingrule="default_allocation_detail"><blocktrade><refs><md:ref type="blocktrade" refid="MID-N-386EN9G-00"/><md:ref type="blocktrade"
                // refid="MID-N-386Df6Q-00"/></refs></blocktrade></action>
                boolean isNettedBlock = btInRefs(actionblocktrade.getBlockTrade());
                if (isNettedBlock)
                    return;

                // Do not allow a bookout if the whole quantity is not in warehouse
                checkWareHouse(actionblocktrade.getBlockTrade());

                // Do not allow a bookout on MPEX order
                checkMPEX(actionblocktrade.getBlockTrade());

                // Do not allow a bookout on an instrument incomplete
                checkInstrumentRVA(actionblocktrade.getBlockTrade());

                // ONBRD-6949
                checkMsgFields(actionblocktrade);

                // ONBRD-5037 CMCICS - Middle project > TO DO - non existing account (PRE 30)
                // 1) It should not be possible to bookout if no account was selected
                // 2) Prevent user from choosing an account that does not exist in trading or bookout screen
                // => static data on client
                checkAllocationOnOfflineMode((ActionBlockTradeCreate) actionblocktrade);

                if (isFoUser(user))
                {
                    checkForcedCharges((ActionBlockTradeCreate) actionblocktrade);
                }

                // Les utilisateurs FO peuvent r�aliser un booking en laissant les champs � level � et � price reporting mode � vides.
                // Il y a des workflow dans lesquels il est n�cessaire de modifier ces champs, il ne faut laisser que les choix utiles. (level block & alloc ? price reporting mode : gross et net)
                checkPriceTypeOnCreate(((ActionBlockTradeCreate) actionblocktrade).getBlockTrade());

                checkApplicationLevel(actionblocktrade.getBlockTrade());
            }

            // ONBRD-4925 : Control if a country / exchange is not associated with a calendar
            checkCalendarOnExchange(getExchangeId(actionblocktrade.getBlockTrade()));

            if (isFoUser(user))
            {
                // ONBRD-5038 CMCICS - Middle project > TO DO - Do not allow trade date modification (PRE 31)
                checkTradeDate(actionblocktrade.getBlockTrade());
            }

            // ONBRD-4693 - CMCICS - Middle project > 12.5 RL date
            // ONBRD-6388 - (Major) Bloquer book out on order account si trade date = settlement date
            checkRLDate(actionblocktrade);

            // ONBRD-4927 CMCICS - Middle project > New - Popup to prevent user to bookout if EUR is not settlement currency and FX rate was not modified
            checkFXRate(actionblocktrade.getBlockTrade());

            // ONBRD-5519 [Cannot select a weekend when changing settlement date (DEP19)]
            checkSettlementDate(actionblocktrade.getBlockTrade());

            // Compte tenu des diff�rences de comportement, en fonction des donn�es, lors des modifications de block et pour un besoin
            // de coh�rence entre les informations � communes � des block et de leurs allocations, nous souhaitons emp�cher la modification
            // de block comportant des allocations non annul�es.
            if (actionblocktrade instanceof ActionBlockTradeReplace)
            {
                // no check on revert netting
                if (actionblocktrade.getBlockTrade() != null
                    && actionblocktrade.getBlockTrade().getStates() != null
                    && (actionblocktrade.getBlockTrade().getStates().contains("revert-netting") || actionblocktrade.getBlockTrade().getStates().contains("-netted")))
                    return;

                initDataServices(BlockConfirm.MODEL_TYPE);
                ActionBlockTradeReplace actionblocktradereplace = (ActionBlockTradeReplace) actionblocktrade;
                BlockTrade blocktrade = actionblocktradereplace.getBlockTrade();
                Condition getBlockTradeIdCondition = ConditionHelper.equal(BlockConfirm.Properties.BLOCKTRADEID, blocktrade.getId());
                Iterator<BlockConfirm> blockconfirms;
                if (blocktrade.getId() != null && (blockconfirms = dataServices.get(BlockConfirm.MODEL_TYPE).getAll(getBlockTradeIdCondition)) != null) // BlockConfirms related to blocktrade in DB
                {
                    while (blockconfirms.hasNext())
                    {
                        BlockConfirm blockconfirm = blockconfirms.next();
                        if (blockconfirm.getStates() != null && !blockconfirm.getStates().contains("cancelled") && !"omgeo-standard".equals(actionblocktrade.getBlockTrade().getAllocationMode()))
                        {
                            // one blockconfirm is not cancelled
                            throw new Exception("Modification of BlockTrade is not allowed : at least one allocation is not in cancelled state");
                        }
                    }
                }
            }

            // DEP-41 Custom prop control
            // if (actionblocktrade instanceof ActionBlockTradeReplace)
            checkCustomProperties(actionblocktrade.getBlockTrade());

            // Dans la proc�dure pour r�pondre en net, il est notifi� que l?agreed price type ne doit pas �tre changer.
            // Pouvez-vous donc griser ou emp�cher toutes modifications de ce champs. Une issue a �t� ouverte car la modification
            // de ce champs entraine de grosses erreur notamment lors de l?export BO.
            checkPriceType(actionblocktrade.getBlockTrade());

            // ONBRD-6597 - Workflow : (Blocker) Contr�le sur le client sur le bloc et l'alloc
            if (actionblocktrade.getBlockTrade().getAllocationMode() != null && !"omgeo-standard".equals(actionblocktrade.getBlockTrade().getAllocationMode()))
                checkClient(actionblocktrade);

            // ONBRD-5351 (to do) 13.6 lot size multiple
            checkLotSize(actionblocktrade.getBlockTrade());
        }
    }

    private void checkWareHouse(BlockTrade blockTrade) throws Exception
    {
        initDataServices(Warehouse.MODEL_TYPE);
        Condition getwarehouse = ConditionHelper.equal(Warehouse.Properties.ORDERID, getOrder(blockTrade).getId());
        Iterator<Warehouse> all = dataServices.get(Warehouse.MODEL_TYPE).getAll(getwarehouse);
        if (all.hasNext())
        {
            Warehouse single = all.next();
            if (single.getQty().compareTo(blockTrade.getQty()) < 0)
                throw new Exception("All quantity is not in warehouse, please warehouse first");
        }
    }

    private void checkLotSize(BlockTrade blockTrade) throws Exception
    {
        // TODO Auto-generated method stub
        if (blockTrade.getInstrumentId() != null)
        {
            Instrument instrument = InstrumentDataServiceImpl.getInstance().get(blockTrade.getInstrumentId());
            if (instrument != null && instrumentHelper.isBond(instrument) && Quantity.Type.CONTRACTS.equals(instrument.getQuantityType()))
            {
                BigDecimal bookoutqty = blockTrade.getQty();
                BigDecimal instrumentfacevalue = instrument.getFaceValueOfIssue();
                MathContext mc = new MathContext(0);
                BigDecimal result = bookoutqty.remainder(instrumentfacevalue, mc);
                if (result.compareTo(BigDecimal.ZERO) != 0)
                    throw new Exception("Bookout quantity is not multiple of the face value of issue. Cannot bookout");
            }
        }

    }

    private boolean btInRefs(BlockTrade blockTrade) throws Exception
    {
        if (blockTrade.getRefs() != null)
        {
            for (ConsiderationRef ref : blockTrade.getRefs())
            {
                if ("blocktrade".equals(ref.getType()))
                {
                    initDataServices(BlockTrade.MODEL_TYPE);
                    BlockTrade bt = (BlockTrade) dataServices.get(BlockTrade.MODEL_TYPE).get(ref.getRefId(), false);
                    if (bt != null && !"offline-noexport".equals(bt.getAllocationMode()))
                        throw new Exception("Netting on an already exported blocktrade is not allowed");
                    return true;
                }
            }
        }
        return false;
    }

    private void checkMsgFields(ActionBlockTrade actionblocktrade) throws Exception
    {
        // need to manage massive bookout
        // seems same message + one alloc
        if (actionblocktrade instanceof ActionBlockTradeCreate)
        {
            ActionBlockTradeCreate actionblocktradecreate = (ActionBlockTradeCreate) actionblocktrade;
            if (actionblocktradecreate.getBlockConfirm() != null
                && actionblocktradecreate.getBlockConfirm().size() == 1
                && actionblocktradecreate.getBlockTrade().getSettlementMsg() != null
                && actionblocktradecreate.getBlockConfirm().iterator().next().getSettlementMsg() != null
                && actionblocktradecreate.getBlockTrade().getSettlementMsg().equals(actionblocktradecreate.getBlockConfirm().iterator().next().getSettlementMsg()))
            {
                actionblocktrade.getBlockTrade().setSettlementMsg(null);
                return;
            }
        }

        if (actionblocktrade.getBlockTrade().getSettlementMsg() != null && !actionblocktrade.getBlockTrade().getSettlementMsg().isEmpty())
            throw new Exception("Settlement Message at block level should be empty");
        else
            actionblocktrade.getBlockTrade().setSettlementMsg(null);
    }

    private void checkPriceTypeOnCreate(BlockTrade blockTrade) throws Exception
    {
        if ((blockTrade.getPriceReportingMode() != null && blockTrade.getPriceReportingMode().isEmpty()) || (blockTrade.getPriceReportingMode() != null && blockTrade.getPriceReportingMode().equals(AgreedPriceType.NET_NET)))
            throw new Exception("Price Reporting Mode should be only set to gross or net");

        if ((blockTrade.getAllocationLevel() != null && blockTrade.getAllocationLevel().isEmpty()) || (blockTrade.getAllocationLevel() != null && (blockTrade.getAllocationLevel().equals(AllocationLevelConstant.NONE.toString()))))
            throw new Exception("Level should not be empty");
    }

    private void checkClient(ActionBlockTrade actionblocktrade) throws Exception
    {

        String current_user = actionblocktrade.getUserId();
        User user = UserDataServiceImpl.getInstance().get(current_user);

        // if fo ou (mo et today)
        if (isFoUser(user) || (!isFoUser(user) && isToday(actionblocktrade.getBlockTrade())))
        {
            logger.info("Checking if client order must be block/alloc client or CMCICS");

            // client order must be block/alloc client or CMCICS
            BlockTrade blocktrade = actionblocktrade.getBlockTrade();
            BlockTrade bt = null;
            String clientblock;
            Order order;

            if (blocktrade.getClientId() == null || getOrder(blocktrade) == null)
                bt = (BlockTrade) dataServices.get(BlockTrade.MODEL_TYPE).get(blocktrade.getId(), false);

            if (blocktrade.getClientId() != null)
                clientblock = blocktrade.getClientId();
            else
                clientblock = bt.getClientId();

            if (getOrder(blocktrade) != null)
                order = getOrder(blocktrade);
            else
                order = getOrder(bt);

            String clientorder = order.getClientId();

            logger.info("clientorder=" + clientorder);
            logger.info("clientblock=" + clientblock);

            // Allow bookout Axcellerator
            if (clientorder == null && order.getClientSource() != null && "I_Marvel_FIX44_Axcellerator".equals(order.getClientSource()))
                return;

            if (!clientblock.equals(clientorder) && !CMCICS_CLIENTID.equals(clientblock))
                throw new Exception("Client on block should be the same as the client on the order or " + CMCICS_CLIENTID);

            if (actionblocktrade instanceof ActionBlockTradeCreate)
            {
                if (((ActionBlockTradeCreate) actionblocktrade).getBlockConfirm() != null)
                {
                    for (BlockConfirm bcinbt : ((ActionBlockTradeCreate) actionblocktrade).getBlockConfirm())
                    {
                        logger.info("clientalloc=" + bcinbt.getClientId());
                        if (bcinbt.getClientId() != null && !clientorder.equals(bcinbt.getClientId()) && !CMCICS_CLIENTID.equals(bcinbt.getClientId()))
                            throw new Exception("Client on allocation should be the same as the client on the order or " + CMCICS_CLIENTID);
                    }
                }
            }
        }
    }

    private boolean isToday(BlockTrade bt) throws Exception
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        String today = formatter.format(new Date());

        logger.info("today=" + today);
        logger.info("getLastExecution=" + getLastExecution(bt).substring(0, 8));

        if (today.equals(getLastExecution(bt).substring(0, 8)))
            return true;
        else
            return false;
    }

    private Boolean isFoUser(User user)
    {
        String profil = getCustomProperty(user, "PROFIL");
        return (profil == null || !"MO".equals(profil));
    }

    private void checkApplicationLevel(BlockTrade bt) throws Exception
    {
        if (bt != null && bt.getFees() != null)
        {
            for (CalcFee fee : bt.getFees())
            {
                if (fee.getFeeModelType().equals(Commission.TYPE))
                    if (fee.getFeeModelType() != null && fee.getFeeModelType().equals(Commission.TYPE) && fee.getOverrideState() != null && bt.getAllocationLevel() != null
                        && !bt.getAllocationLevel().equals(AllocationLevelConstant.BLOCK.toString()))
                        throw new Exception("Allocation Level should be set to block if commission has been forced");
            }
        }
    }

    private void checkForcedCharges(ActionBlockTradeCreate actionblocktrade) throws Exception
    {
        BlockTrade bt = actionblocktrade.getBlockTrade();
        if (bt != null && bt.getFees() != null)
        {
            for (CalcFee fee : bt.getFees())
            {
                if (fee.getFeeModelType() != null && fee.getFeeModelType().equals(Charge.TYPE) && fee.getOverrideState() != null)
                    throw new Exception("Charges should not be changed");
            }
        }
    }

    private void checkPriceType(BlockTrade blockTrade) throws Exception
    {
        if (blockTrade.getPriceAgreedType() != null && !blockTrade.getPriceAgreedType().equals(AgreedPriceType.GROSS))
        {
            throw new Exception("Agreed Price Type should be set to gross");
        }
    }

    private void checkAllocationOnOfflineMode(ActionBlockTradeCreate actionblocktradecreate) throws Exception
    {
        String mode = actionblocktradecreate.getBlockTrade().getAllocationMode();
        if (mode != null && mode.contains("offline"))
        {
            // allocation mandatory
            if (actionblocktradecreate.getBlockConfirm() == null || actionblocktradecreate.getBlockConfirm().size() < 1)
                throw new Exception("Missing allocations for an offline workflow");
        }
    }

    private void checkInstrumentRVA(BlockTrade blockTrade) throws Exception
    {
        Instrument instrument = InstrumentDataServiceImpl.getInstance().get(blockTrade.getInstrumentId());

        if (instrument == null || (instrument != null && instrument.getCustomProperties() == null))
            throw new Exception("Bookout not possible, the instrument is missing the booking information, check with RVA");

        if (instrument.getType() == null)
            throw new Exception("Bookout not possible, the instrument is missing the cifcode information, check with RVA");

        for (String customprop : MANDATORY_RVA_CUSTOMPROPERTIES)
        {
            if (!instrument.getCustomProperties().containsKey(customprop))
                throw new Exception("Bookout not possible, the instrument is missing the booking information, check with RVA");
        }
    }

    private void checkMPEX(BlockTrade blockTrade) throws Exception
    {
        if (blockTrade.getCustomProperties() == null
            || !blockTrade.getCustomProperties().containsKey(WHICH_MIDDLE)
            || !blockTrade.getCustomProperties().get(WHICH_MIDDLE).equals("ULLINK"))
            throw new Exception("Bookout an order for a client still on MPEX is not allowed");
    }

    private void checkCustomProperties(BlockTrade blocktrade) throws Exception
    {

        if (blocktrade.getCustomProperties() != null)
        {
            Map<String, String> action_customprop = blocktrade.getCustomProperties();
            Map<String, String> db_customprop = new HashMap<String, String>();

            List<String> all_keys = new ArrayList<String>();
            all_keys.addAll(action_customprop.keySet());

            BlockTrade bt;
            // BT in db
            if (blocktrade.getId() != null && (bt = (BlockTrade) dataServices.get(BlockTrade.MODEL_TYPE).get(blocktrade.getId(), false)) != null && bt.getCustomProperties() != null) // BlockTrade in DB
            {
                logger.info("BT in db...");
                all_keys.addAll(bt.getCustomProperties().keySet());
                db_customprop = bt.getCustomProperties();
            }
            else
            {
                logger.info("BT not in db, checking order...");
                Order order = getOrder(blocktrade);
                for (String property : CUSTOMPROPERTIES_ORDER)
                {
                    String data = getData(order, property);
                    if (data != null && !data.isEmpty())
                    {
                        logger.info(property + " = " + data);
                        all_keys.add(property);
                        db_customprop.put(property, data);
                    }

                }
            }

            for (String key : all_keys)
            {
                // key presente dans les 2 maps
                if (db_customprop.containsKey(key) && action_customprop.containsKey(key))
                {
                    if (!db_customprop.get(key).equals(action_customprop.get(key)))
                    {
                        logger.info("key modified " + key);
                        throw new Exception("Key " + key + " should not be modified");
                    }
                }
                // only in source => adding
                else if (action_customprop.containsKey(key) && !IGNORE_CUSTOMPROP.contains(key))
                {
                    if (!AUTHORIZED_CUSTOMPROP_TOADD.contains(key))
                        throw new Exception("Key " + key + " should not be added. Only " + AUTHORIZED_CUSTOMPROP_TOADD.toString() + " are authorized");
                    else if (action_customprop.get(key) != null && !action_customprop.get(key).isEmpty() && key.contains("FTT") && !TTFEXEMPTED_VALUES.contains(action_customprop.get(key)))
                        throw new Exception("value " + action_customprop.get(key) + " for key " + key + " does not exist. Existing ones : " + TTFEXEMPTED_VALUES.toString());
                }
                // only in db => suppress
                else if (db_customprop.containsKey(key))
                {
                    if (!AUTHORIZED_CUSTOMPROP_TOADD.contains(key))
                        throw new Exception("Key " + key + " should not be deleted");
                }
            }
        }
    }

    private Order getOrder(BlockTrade blockTrade)
    {
        if (blockTrade.getRefs() != null)
        {
            for (ConsiderationRef ref : blockTrade.getRefs())
            {
                if ("order".equals(ref.getType()))
                {
                    try
                    {
                        return OrderDataServiceImpl.getInstance().get(ref.getRefId());
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    private ConsiderationRef getOrderRef(BlockTrade blockTrade)
    {
        if (blockTrade.getRefs() != null)
        {
            for (ConsiderationRef ref : blockTrade.getRefs())
            {
                if ("order".equals(ref.getType()))
                {
                    try
                    {
                        return ref;
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    private String getData(Dataable data, String key)
    {
        Map<String, String> tags = DataManager.toMap(data);
        String value = null;
        if (tags != null && tags.containsKey(key))
        {
            value = tags.get(key);
        }

        return value;
    }

    private void checkSettlementDate(BlockTrade blocktrade) throws Exception
    {
        if (blocktrade.getTime() != null && blocktrade.getTime().getSettlementDate() != null)
        {
            String exchangeid = getExchangeId(blocktrade);

            if (isSettlementOff(exchangeid, getTimefromDate(blocktrade.getTime().getSettlementDate())))
                throw new Exception("Settlement Date is set to a holiday");
        }
    }

    private String getExchangeId(BlockTrade blocktrade) throws Exception
    {
        BlockTrade bt;
        if (blocktrade.getId() != null && (bt = (BlockTrade) dataServices.get(BlockTrade.MODEL_TYPE).get(blocktrade.getId(), false)) != null) // BlockTrade in DB
        {
            return bt.getExchangeId();
        }
        else
            return blocktrade.getExchangeId();
    }

    private void checkTradeDate(BlockTrade blocktrade) throws Exception
    {
        if (blocktrade.getTime() != null && blocktrade.getTime().getOrig() != null)
        {
            String save_orig = CustomPropertiesHelper.get(blocktrade, "SAVE_TIME_ORIG");
            String orig = blocktrade.getTime().getOrig();

            logger.info("orig=" + orig);
            logger.info("save_orig=" + save_orig);

            int i = (orig.length() < 17) ? 8 : 17;
            orig = orig.substring(0, i);
            save_orig = save_orig.substring(0, i);

            if (!orig.equals(save_orig))
            {
                throw new Exception("Trade Date modification is not allowed");
            }
        }
    }

    private String getLastExecution(BlockTrade blocktrade) throws Exception
    {
        BlockTrade bt;
        if (blocktrade.getId() != null && (bt = (BlockTrade) dataServices.get(BlockTrade.MODEL_TYPE).get(blocktrade.getId(), false)) != null) // BlockConfirm in DB
        {
            return bt.getTime().getLastExecution();
        }
        else
            return blocktrade.getTime().getLastExecution();
    }

    private void checkCalendarOnExchange(String exchangeid) throws Exception
    {
        if (exchangeid != null)
        {
            Exchange exchange = ExchangeDataServiceImpl.getInstance().get(exchangeid);
            if (exchange != null && (exchange.getBusinessCalendarId() == null || (exchange.getBusinessCalendarId() != null && BusinessCalendarDataServiceImpl.getInstance().get(exchange.getBusinessCalendarId()) == null)))
            {
                throw new Exception("No calendar defined on the exchange, cannot book-out");
            }
        }
    }

    public void checkRLDate(ActionBlockTrade actionblocktrade) throws Exception
    {
        String current_user = actionblocktrade.getUserId();
        User user = UserDataServiceImpl.getInstance().get(current_user);

        if (user.isConsumerOfLicenseToken())
        {
            String rldate = null;

            if (actionblocktrade.getBlockTrade() != null && actionblocktrade.getBlockTrade().getTime() != null)
                rldate = actionblocktrade.getBlockTrade().getTime().getSettlementDate();

            if (rldate != null)
            {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");

                String origTime = null;
                origTime = actionblocktrade.getBlockTrade().getTime().getOrig();
                if (origTime == null)
                {
                    // Need to get it from db
                    BlockTrade bt = (BlockTrade) dataServices.get(BlockTrade.MODEL_TYPE).get(actionblocktrade.getBlockTrade().getId(), false);
                    origTime = bt.getTime().getOrig();
                }
                if (origTime == null)
                    return;

                Date d = formatter.parse(origTime);
                String tradedate = com.ullink.oms.middle.apihelper.model.HelperBlock.getDateFromStamp(formatter.format(d));
                logger.info("Tradedate : " + tradedate + " from orig : " + origTime);

                // FO does not allowed to modify RL Date
                if (isFoUser(user))
                {
                    String instrumentid = actionblocktrade.getBlockTrade().getInstrumentId();
                    String givenRL = actionblocktrade.getBlockTrade().getTime().getSettlementDate();
                    String dbRL = getSettlementService().calculate(tradedate, instrumentid, getOrderRef(actionblocktrade.getBlockTrade()));
                    if (!dbRL.equals(givenRL))
                    {
                        dbRL = dbRL.substring(6, 8) + "/" + dbRL.substring(4, 6) + "/" + dbRL.substring(0, 4);
                        throw new Exception("Not allowed to modify RL Date. Please specify original RL Date " + dbRL + " and add a comment at destination to Middle Office");
                    }
                }

                // Bloquer book out on order account si trade date = settlement date
                // SimpleDateFormat formatter_yyyyMMdd = new SimpleDateFormat("yyyyMMdd");
                // String today = formatter_yyyyMMdd.format(new Date());
                if (tradedate.equals(rldate))
                    throw new Exception("The settlement date can not be equal to the trade date");

            }
        }
    }

    public void checkFXRate(BlockTrade blocktrade) throws Exception
    {
        if (blocktrade == null)
            return;

        String currency = blocktrade.getCurrency();
        String sett_currency = blocktrade.getSettlementCurrency();

        if (currency == null || sett_currency == null)
            return;
        if (currency.equals(sett_currency))
            return;
        BigDecimal fxrate = blocktrade.getSettlementFxRate();

        if (fxrate != null)
        {
            try
            {
                BigDecimal t = ChangeRateDataServiceImpl.getInstance().getDefaultRateValue(currency, sett_currency);
                if (fxrate.compareTo(t) == 0)
                    throw new Exception("FX rate was not modfied. Please enter the negociated FX rate");
            }
            catch (DAOException e)
            {
                logger.info("FX Rate not found, continue...");
            }
        }
    }

    public Boolean isSettlementOff(String exchangeId, long timeInMillis) throws Exception
    {
        Exchange exchange = ExchangeDataServiceImpl.getInstance().get(exchangeId);
        if (null != exchange)
        {
            String businessCalendarId = exchange.getBusinessCalendarId();
            if (businessCalendarId != null)
            {
                Businesscalendar businessCalendar = BusinessCalendarDataServiceImpl.getInstance().get(businessCalendarId);
                if (businessCalendar != null)
                {
                    String calendarId = businessCalendar.getSettlementCalendarId();
                    if (calendarId != null)
                    {
                        return Boolean.valueOf(CalendarPhaseHelper.getInstance().isCalendarOff(calendarId, timeInMillis));
                    }
                }
            }
        }
        return Boolean.FALSE;
    }

    public long getTimefromDate(String date) throws Exception
    {
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyyMMdd");
        Date settlementDate = new Date();
        settlementDate = formatDate.parse(date);
        return settlementDate.getTime();
    }

    private String getCustomProperty(ObjectWithCustomProperties o, String key)
    {
        if (o == null)
            return null;
        Map<String, String> customproperties = o.getCustomProperties();
        if (customproperties != null && customproperties.containsKey(key))
            return customproperties.get(key);
        else
            return null;
    }

}