package tlb.server.resources.correctness;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import tlb.domain.Entry;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.SetRepo;

import java.io.IOException;

/**
 * @understands maintaining universal set for correctness checks
 */
public class UpdateUniversalSetResource extends SetResource {
    public UpdateUniversalSetResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    public void acceptRepresentation(Representation entity) throws ResourceException {
        if (! repo.isPrimed()) {
            synchronized (EntryRepoFactory.mutex(repo.getIdentifier())) {
                if (! repo.isPrimed()) {
                    repo.load(reqPayload(entity));
                    getResponse().setStatus(Status.SUCCESS_CREATED);
                    return;
                }
            }
        }
        SetRepo.OperationResult match = repo.tryMatching(reqPayload(entity));
        if (match.success) {
            getResponse().setStatus(Status.SUCCESS_OK);
        } else {
            getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT);
            getResponse().setEntity(new StringRepresentation(match.message));
        }
    }

    @Override
    protected Entry parseEntry(Representation entity) throws IOException {
        throw new UnsupportedOperationException("not implemented yet");
    }

}
