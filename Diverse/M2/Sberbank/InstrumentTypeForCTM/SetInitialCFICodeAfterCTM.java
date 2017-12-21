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
 * The 7digits CFI code has been added in the data column of the instrument
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
import com.ullink.oms.middle.api.model.BlockTrade;
import com.ullink.oms.model.Action;
import com.ullink.oms.model.Instrument;
import com.ullink.oms.model.Order;
import com.ullink.oms.model.Event;
import com.ullink.oms.model.interfaces.Dataable;
import com.ullink.oms.model.tools.DataManager;
import com.ullink.oms.workers.enrichment.impl.java.JavaActionEnricher;
import com.ullink.oms.workers.enrichment.impl.java.JavaEventEnricher;
import com.ullink.ultools.log.Log;
import com.ullink.ultools.log.LogFactory;

public class SetInitialCFICodeAfterCTM extends JavaEventEnricher
{
    private static final Log logger = LogFactory.getLog("SetInitialCFICodeAfterCTM");

    public void doEnrich(Event event) throws Exception
    {
        logger.info("Event was generated for action type: " + event.getAction());

        if (event.getAction() instanceof ActionBlockTradeApprove) // ActionBlockTradeApprove
        {

            logger.info("Called action enrichment: SetInitialCFICodeAfterCTM");

            // defining the bt from BlockTrade.Approve action. It only contains the ID of the blocktrade
            ActionBlockTrade actionblocktrade = (ActionBlockTrade) event.getAction();
            BlockTrade btFromBlockTradeApprove = actionblocktrade.getBlockTrade();

            // creating service to be able to get the blocktrade from DB based on the ID from the BlockTrade.Approve message
            DataServiceHelper<BlockTrade> blockTradeDataService = new DataServiceHelper<BlockTrade>(ServerServices.getInstance().getDataServiceRegistry().getDataService(ModelType.valueOf(BlockTrade.class)));
            BlockTrade btFromDB = blockTradeDataService.get(btFromBlockTradeApprove.getId());

            if("omgeo-standard".equals(btFromDB.getAllocationMode())) 
            {
                Instrument instrument = InstrumentDataServiceImpl.getInstance().get(btFromDB.getInstrumentId());

                String initialCFICode = getDataTagValue(instrument, "CFICode6Digits");
                String CFICodeCTM = instrument.getType();

                setInitialCFICode(instrument, CFICodeCTM, initialCFICode);

                InstrumentDataServiceImpl.getInstance().update(instrument);
            }
            
        }
    }

    /*
     * This method replaces the 2nd character from CFICodeInstrument6Digits with the 2nd character from CFICode7Digits
     */
    private static void setInitialCFICode(Instrument instrument, String CFICodeCTM, String initialCFICode)
    {
        if (instrument == null || CFICodeCTM == null || initialCFICode == null)
        {
            logger.info("Instrument.type cannot be reverted.");
            return;
        }

        instrument.setType(initialCFICode);
        logger.info("Instrument.type has been reverted back to: " + instrument.getType());

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
