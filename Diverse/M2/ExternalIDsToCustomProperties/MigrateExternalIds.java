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
import com.ullink.oms.dao.schema.InstrumentTable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import com.ullink.oms.helpers.ClientDataServiceImpl;
import com.ullink.oms.model.Action;
import com.ullink.oms.model.Client;
import com.ullink.oms.workers.enrichment.impl.java.JavaActionEnricher;

public class MigrateExternalIds extends JavaActionEnricher
{
    static
    {
        Iterator<Client> clientIterator = ClientDataServiceImpl.getInstance().getAll();
        while (clientIterator.hasNext())
        {
            Client client = clientIterator.next();
            if (client.getExternalIds() != null)
            {
                Map<String, String> customProperties = client.getCustomProperties() == null ? new HashMap<String, String>() : client.getCustomProperties();
                for (Entry<String, String> externalId : client.getExternalIds().entrySet())
                {
                    customProperties.put(externalId.getKey(), externalId.getValue());
                }
                client.setCustomProperties(customProperties);
                ClientDataServiceImpl.getInstance().update(client);
            }
        }
    }

    @Override
    public void doEnrich(Action arg0) throws Exception
    {
    }

}