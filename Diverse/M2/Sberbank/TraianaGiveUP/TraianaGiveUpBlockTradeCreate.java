import com.ullink.oms.workers.enrichment.impl.java.JavaReplyEnricher;
import java.util.logging.Logger;
import com.ullink.oms.helpers.BrokerDataServiceImpl;
import com.ullink.oms.helpers.OrderDataServiceImpl;
import com.ullink.oms.middle.api.actions.blocktrade.ActionBlockTrade;
import com.ullink.oms.middle.api.model.BlockConfirm;
import com.ullink.oms.middle.api.model.BlockTrade;
import com.ullink.oms.middle.api.model.ref.ConsiderationRef;
import com.ullink.oms.middle.api.replies.ReplyBlockTradePreview;
import com.ullink.oms.middle.api.requests.RequestBlockTradePreview;
import com.ullink.oms.middle.extension.actionprocessors.blockconfirm.BlockConfirmHelper;
import com.ullink.oms.model.Broker;
import com.ullink.oms.model.Order;
import com.ullink.oms.model.ReplyBase;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

public class TraianaGiveUpBlockTradeCreate extends JavaReplyEnricher
{
    public static final Logger logger = Logger.getLogger("traianaGiveUpBlockconfirmCreate");
    
    public void doEnrich(Action action) throws Exception
    {
        logger.log(Level.INFO, "Called action enrichment traianaGiveUpBlockconfirmCreate");
        logger.log(Level.INFO, "Action type is: " + action.getType());
        if ("blocktrade.create".equals(action.getType()))
        {
            ActionBlockTrade actionblocktrade = (ActionBlockTrade) action;
            BlockTrade bt = actionblocktrade.getBlockTrade();
            if (bt != null && bt.getGiveupBrokerId() != null)
            {
                logger.log(Level.INFO, "This is a giveUp Trade. GiveUpBrokerId: " + bt.getGiveupBrokerId());
                Map<String, String> isGiveUpBroker = new HashMap<String, String>();
                isGiveUpBroker.put("isGiveupBroker", "yes");
                logger.log(Level.INFO, "Created custom property key=value : isGiveupBroker=yes");
                bt.setCustomProperties(isGiveUpBroker);
                logger.log(Level.INFO, "Custom Properties on the blockTrade message after adding isGiveUPBroker: " + bt.getCustomProperties());
            }
        }
    }
}