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
 * 
 * Sberbank is using 7 digit CFI codes 
 * In order to work well with MiFID2 version of Ullink software, they have removed the 2nd character so that the CFI code will have 6 characters
 * The 7digits CFI code has been added in the data column of the instrument under key CFICode
 * However, the 2nd character was used to identify the instrument in CTM
 * For Sberbank to continue to use their setup with CTM we suggest a two fold solution:
 * ---> enrichment on blocktrade.approve to put the 2nd character from 7 digit CFI code in the instrument.type property --> SetCFICodeForCTM
 * ---> enrichment on event event.blocktrade after blocktrade.approve to put back the initial CFI code --> SetInitialCFICodeAfterCTM
 ************************************************************************/

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import com.ullink.oms.actions.ActionOrderCreate;
import com.ullink.oms.actions.ActionTradeCreate;
import com.ullink.oms.apihelpers.utils.type.StringHelper;
import com.ullink.oms.constants.ModelType;
import com.ullink.oms.extensions.apihelpers.data.service.DataServiceHelper;
import com.ullink.oms.extensions.apihelpers.util.service.ServerServices;
import com.ullink.oms.helpers.InstrumentDataServiceImpl;
import com.ullink.oms.helpers.OrderDataServiceImpl;
import com.ullink.oms.middle.api.actions.blocktrade.ActionBlockTrade;
import com.ullink.oms.middle.api.actions.blocktrade.ActionBlockTradeApprove;
import com.ullink.oms.middle.api.actions.blocktrade.ActionBlockTradeCreate;
import com.ullink.oms.middle.api.actions.blocktrade.ActionBlockTradeReplace;
import com.ullink.oms.middle.api.model.BlockTrade;
import com.ullink.oms.model.Action;
import com.ullink.oms.model.Instrument;
import com.ullink.oms.model.Order;
import com.ullink.oms.model.interfaces.Dataable;
import com.ullink.oms.model.tools.DataManager;
import com.ullink.oms.workers.enrichment.impl.java.JavaActionEnricher;
import com.ullink.ultools.log.Log;
import com.ullink.ultools.log.LogFactory;

public class SetCFICodeForCTM extends JavaActionEnricher
{
    private static final Log logger = LogFactory.getLog("SetCFICodeForSSI");

    public void doEnrich(Action action) throws Exception
    {
        logger.info("Action type: " + action.getType());
        // action.getModelType();
        if (action instanceof ActionBlockTradeApprove) // ActionBlockTradeApprove
        {
            logger.info("Called action enrichment: SetCFICodeForCTM");

            // defining the bt from BlockTrade.Approve action. It only contains the ID of the blocktrade
            ActionBlockTrade actionblocktrade = (ActionBlockTrade) action;
            BlockTrade btFromBlockTradeApprove = actionblocktrade.getBlockTrade();

            // creating service to be able to get the blocktrade from DB based on the ID from the BlockTrade.Approve message
            DataServiceHelper<BlockTrade> blockTradeDataService = new DataServiceHelper<BlockTrade>(ServerServices.getInstance().getDataServiceRegistry().getDataService(ModelType.valueOf(BlockTrade.class)));
            BlockTrade btFromDB = blockTradeDataService.get(btFromBlockTradeApprove.getId());

            if("omgeo-standard".equals(btFromDB.getAllocationMode())) 
            {
                Instrument instrument = InstrumentDataServiceImpl.getInstance().get(btFromDB.getInstrumentId());

                String CFICode7Digits = getDataTagValue(instrument, "CFICode");
                String CFICodeInstrument6Digits = instrument.getType();

                // adding the correct 6 digits CFI code in the datatags so that we can recover it later (when SetInitialCFICodeAfterCTM will be executed)
                addDataTag(instrument, "CFICode6Digits", CFICodeInstrument6Digits);

                setCFIForCTM(instrument, CFICode7Digits, CFICodeInstrument6Digits);
                logger.info("Datatags for instrument " + instrument.getId() + ": " + instrument.getData());
                InstrumentDataServiceImpl.getInstance().update(instrument);
            } 
        }
    }

    /*
     * This method replaces the 2nd character from CFICodeInstrument6Digits with the 2nd character from CFICode7Digits
     */
    private static void setCFIForCTM(Instrument instrument, String CFICode7Digits, String CFICodeInstrument6Digits)
    {
        if (instrument == null || CFICode7Digits == null || CFICodeInstrument6Digits == null)
        {
            logger.info("Instrument.type cannot be changed.");
            return;
        }

        String CFICodeCTM = CFICodeInstrument6Digits.replace(CFICodeInstrument6Digits.charAt(1), CFICode7Digits.charAt(1));
        instrument.setType(CFICodeCTM);
        logger.info("Instrument.type has been set to: " + instrument.getType());

    }

    private static String getDataTagValue(Dataable object, String key)
    {
        if (key != null)
        {
            Map<String, String> map = DataManager.toMap(object);
            if (map != null)
            {
                return map.get(key);
            }
        }

        return null;
    }

    private static void addDataTag(Dataable object, String key, String value)
    {
        if (key == null || value == null)
            return;

        String xmlResult = DataManager.setPersistedDataValue(object, key, value);
        if (xmlResult != null)
            object.setData(xmlResult);
        logger.info("Datatag " + key + "=" + value + " has been added to the " + object + ".");
    }

}
