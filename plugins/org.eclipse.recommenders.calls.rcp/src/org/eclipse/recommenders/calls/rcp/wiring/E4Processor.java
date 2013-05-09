package org.eclipse.recommenders.calls.rcp.wiring;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.recommenders.calls.ICallModelProvider;

public class E4Processor {

    @PostConstruct
    public void postConstruct(IEclipseContext context) throws IOException {
        NullCallModelProvider value = new NullCallModelProvider();
        value.open();
        context.set(ICallModelProvider.class, value);
    }
}
