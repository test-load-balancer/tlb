package tlb.server.resources;

import org.apache.log4j.Logger;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;
import tlb.TlbConstants;
import tlb.server.repo.EntryRepoFactory;

import java.io.IOException;
import java.util.Map;

import static tlb.TlbConstants.Server.LISTING_VERSION;
import static tlb.TlbConstants.Server.REPO_FACTORY;
import static tlb.TlbConstants.Server.REQUEST_NAMESPACE;

/**
 * @understands is a single point of extension for all TLB resources which also packages commonly used helpers for other resources
 */
public abstract class TlbResource extends Resource {
    protected final Map<String, Object> reqAttrs;

    private static final Logger logger = Logger.getLogger(TlbResource.class.getName());

    public TlbResource(Context context, Request request, Response response) {
        super(context, request, response);
        getVariants().add(new Variant(MediaType.TEXT_PLAIN));
        reqAttrs = request.getAttributes();
        try {
            createRepos();
        } catch (Exception e) {
            logger.warn(String.format("Failed to get repo for '%s'", request.getOriginalRef().getPath()), e);
            throw new RuntimeException(e);
        }
    }

    protected abstract void createRepos() throws IOException, ClassNotFoundException;

    protected String strAttr(final String key) {
        return (String) reqAttrs.get(key);
    }

    protected EntryRepoFactory repoFactory() {
        return (EntryRepoFactory) getContext().getAttributes().get(REPO_FACTORY);
    }

    protected String reqNamespace() {
        return strAttr(REQUEST_NAMESPACE);
    }

    protected String reqVersion() {
        return strAttr(LISTING_VERSION);
    }
}
