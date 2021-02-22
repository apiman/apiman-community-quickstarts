package io.apiman.distro.es;

import io.apiman.common.es.util.EsConstants;
import org.apache.commons.lang.math.NumberUtils;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * ES with fixed port rather than randomised.
 *
 * @author Marc Savy {@literal <marc@rhymewithgravy.com>}
 */
public class FixedPortEsContainer extends ElasticsearchContainer {

    private static final int ELASTICSEARCH_DEFAULT_PORT = 9200;

    /**
     * Create an ES container. Do not provide the version, as this constructor will glue it on.
     *
     * @param dockerImageNameWithoutVersion the image name/URN without the version.
     */
    public FixedPortEsContainer(String dockerImageNameWithoutVersion) {
        super(dockerImageNameWithoutVersion + ":" + EsConstants.getEsVersion());
    }

    /**
     * Add a fixed exposed port range. Syntax is like 9200-9300.
     *
     * If a port the start of the range is unavailable, an alternative port will be used by the ES client(s).
     *
     * In many setups the range is unnecessary, and you can just put a single value.
     *
     * @param hostPortRange the host port range like 9200-9300
     */
    public FixedPortEsContainer withFixedExposedPortRange(String hostPortRange) {
        rangeToList(hostPortRange, ELASTICSEARCH_DEFAULT_PORT);
        return this;
    }

    /**
     * @see #withFixedExposedPortRange(String) 
     * @param hostPortRange the host port range like 9200-9300
     * @param startContainerRange where the internal container range starts (default 9200).
     */
    public FixedPortEsContainer withFixedExposedPortRange(String hostPortRange, int startContainerRange) {
        rangeToList(hostPortRange, startContainerRange);
        return this;
    }

    /**
     * As the default internal ports in the container do not need changing, we map the external port ranges
     * to the default internal ones in lockstep. This generates that staggered mapping.
     *
     * @param hostPortRange the host port range like 9200-9300
     * @param startContainerRange where the internal container range starts. Usually ELASTICSEARCH_DEFAULT_PORT.
     */
    private void rangeToList(String hostPortRange, int startContainerRange) {
        String[] split = hostPortRange.split("-");

        int start;
        int end;

        if (split.length == 2) {
            // Range like 1200-1250
            start = Integer.parseInt(split[0].trim());
            end = Integer.parseInt(split[1].trim());
        } else if (split.length == 1 && NumberUtils.isNumber(split[0])) {
            // Support single value rather than range.
            start = Integer.parseInt(split[0].trim());
            end = start;
        } else {
            throw new IllegalArgumentException("Port range " + hostPortRange + " specified was in "
                 + "unrecognised format. Must be in pattern: 9300-9302");
        }

        // Check whether numbers are sane
        if (start <= 0 || end <= 0) {
            throw new IllegalArgumentException("Port range must be positive integer");
        } else if (start > end) {
            throw new IllegalArgumentException("Port range start is larger than end (specified backwards?): "
                 + start + " > " + end);
        }

        // Map the offsets from user to container for the same length (e.g. 1100 -> 9200, 1101 -> 9201, etc)
        for (int i = 0; i < (end-start); i++) {
            super.addFixedExposedPort(
                 start + i,
                 startContainerRange + i
            );
        }
    }
}
