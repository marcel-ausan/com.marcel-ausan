import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import com.ullink.ulams.api.model.AlgoDefinition;
import com.ullink.ulbridge2.ULBridge;
import com.ullink.ulbridge2.ULMessage;
import com.ullink.ulbridge2.plugins.odisys.dataprovider.api.OdisysDataProvider;
import com.ullink.ulbridge2.plugins.odisys.dataprovider.api.services.AlgoDefinitionDataService;
import com.ullink.ulbridge2.plugins.odisys.dataprovider.api.services.OdisysDataMatcher;
import com.ullink.ultools.NamingService;
import com.ullink.ultools.TraceFile;
import com.ullink.ultools.fixatdl.common.core.Parameter;
import com.ullink.ultools.fixatdl.common.core.ParameterType;
import com.ullink.ultools.fixatdl.common.core.Strategies;
import com.ullink.ultools.fixatdl.common.core.Strategy;
import com.ullink.ultools.fixatdl.common.service.AtdlService;
import com.ullink.ultools.tags.Tags;

public class enrichment_ALGO_AMS_TO_FIX
{

    private static TraceFile trace = (TraceFile) NamingService.lookup(ULBridge.NAMING_TRACE);
    public static String enrichmentName = "ALGO_AMS_TO_FIX";
    public static String ALGOTAGPREFIX = "FIX.";
    public static OdisysDataProvider odisysDataProvider = null;
    public static AlgoDefinitionDataService algoDataService = null;
    public static AtdlService atdlParser = null;
    public static SimpleDateFormat ulmFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    public static SimpleDateFormat fixFormat = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");

    public void doEnrichment(ULMessage ulm)
    {

        if (odisysDataProvider == null)
        {
            odisysDataProvider = (OdisysDataProvider) NamingService.lookup(OdisysDataProvider.NAMING_CTX_ID);
            algoDataService = odisysDataProvider.getAlgoDefinitionDataService();
            atdlParser = new AtdlService();
        }

        String provider = ulm.getString(Tags.ULLINK.ALGOPROVIDERID);
        String algoFixId = ulm.getString(Tags.ULLINK.ALGOID);
        if (provider != null && algoFixId != null)
        {
            if (algoDataService != null)
            {
                Collection<AlgoDefinition> algos = algoDataService.select(new MatcherAlgoId(provider, algoFixId));

                if (algos != null && algos.size() == 1)
                {
                    AlgoDefinition algo = algos.iterator().next();
                    String fixAtdlDefinition = algo.getDefinition();
                    trace.add(enrichmentName, TraceFile.DEBUG, "AlgoDefinition " + algo.getName() + " / " + algo.getProvider() + " found.");
                    try
                    {

                        Strategies strategies = atdlParser.convertToStrategies(fixAtdlDefinition);
                        ArrayList<Strategy> strategiesList = strategies.getStrategyList();
                        if (strategiesList != null && strategiesList.size() == 1)
                        {
                            ulm.add(ALGOTAGPREFIX + algo.getStrategyIdentifierTag(), algo.getWireValue());
                            Strategy strategy = strategiesList.get(0);
                            ArrayList<Parameter> parameters = strategy.getParameterList();
                            if (ulm.exist(Tags.ULLINK.ALGOPARAMS))
                            {
                                ULMessage ulmParams = ULMessage.valueOf(ulm.getString(Tags.ULLINK.ALGOPARAMS));
                                for (Parameter parameter : parameters)
                                {
                                    String ulmParamValue = ulmParams.getString(parameter.getName());
                                    if (ulmParamValue != null)
                                    {
                                        if (ParameterType.UTCTIMESTAMP.equals(parameter.getType()))
                                        {
                                            Date d = ulmFormat.parse(ulmParamValue);
                                            ulmParamValue = fixFormat.format(d);
                                        }
                                        ulm.add(ALGOTAGPREFIX + parameter.getFixTag(), ulmParamValue);
                                    }

                                }
                            }

                        }
                        else
                        {
                            trace.add(enrichmentName, TraceFile.ERROR, "No or more than one definition found.");
                        }

                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                }
            }
        }

    }

    final class MatcherAlgoId implements OdisysDataMatcher<AlgoDefinition>
    {
        private final String brokerId;
        private final String algoFixId;

        MatcherAlgoId(String brokerId, String algoFixId)
        {
            super();
            this.brokerId = brokerId;
            this.algoFixId = algoFixId;
        }

        @Override
        public boolean matches(AlgoDefinition algo)
        {

            return (algo.getWireValue().equals(algoFixId) && algo.getProvider().equals(brokerId));
        }

        @Override
        public boolean isComplete(Collection<AlgoDefinition> algos)
        {
            return !algos.isEmpty();
        }

    }

}
