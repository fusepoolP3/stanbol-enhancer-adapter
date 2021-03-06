/*
 * Copyright 2014 reto.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.fusepool.enhancer.adapter.service;

import eu.fusepool.p3.vocab.TRANSFORMER;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.jaxrs.utils.TrailingSlash;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.web.viewable.RdfViewable;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.impl.ByteArraySource;

/**
 *
 * @author Rupert Westenthaler
 */
public class ChainWrapper {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private final Chain chain;
    private final Serializer serializer;
    private final EnhancementJobManager jobManager;

    ChainWrapper(Chain chain, Serializer serializer, EnhancementJobManager jobManager) {
        this.chain = chain;
        this.serializer = serializer;
        this.jobManager = jobManager;
    }
    
    @GET
    public RdfViewable getDescription(@Context final UriInfo uriInfo, 
            @HeaderParam("user-agent") String userAgent) throws Exception {
        TrailingSlash.enforceNotPresent(uriInfo);
        final String resourcePath = uriInfo.getAbsolutePath().toString();
        //The URI at which this service was accessed accessed, this will be the 
        //central serviceUri in the response
        final UriRef serviceUri = new UriRef(resourcePath);
        //the in memory graph to which the triples for the response are added
        final MGraph responseGraph = new IndexedMGraph();
        //This GraphNode represents the service within our result graph
        final GraphNode node = new GraphNode(serviceUri, responseGraph);
        //The triples will be added to the first graph of the union
        //i.e. to the in-memory responseGraph
        node.addProperty(RDF.type, TRANSFORMER.Transformer);
        for (String outputFormat : serializer.getSupportedFormats()) {
            node.addProperty(TRANSFORMER.supportedOutputFormat, new PlainLiteralImpl(outputFormat));
        }
        node.addProperty(TRANSFORMER.supportedInputFormat, new PlainLiteralImpl("*/*"));
        //What we return is the GraphNode we created with a template path
        return new RdfViewable("Transformer", node, Transformers.class);
    }
    
    @POST
    public ContentItem post(ContentItem contentItem) throws IOException {
        try {
            jobManager.enhanceContent(contentItem, chain);
        } catch (Exception e) {
            throw new WebApplicationException(e.getMessage(), e, 
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
        }
        return contentItem;
    }
    
}
