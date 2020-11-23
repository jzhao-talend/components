// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.azurestorage.blob.runtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobStorageException;

import org.apache.avro.Schema;
import org.talend.components.api.component.runtime.SourceOrSink;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.api.exception.ComponentException;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.azurestorage.AzureStorageProvideConnectionProperties;
import org.talend.components.azurestorage.tazurestorageconnection.TAzureStorageConnectionProperties;
import org.talend.components.azurestorage.utils.AzureStorageUtils;
import org.talend.daikon.NamedThing;
import org.talend.daikon.SimpleNamedThing;
import org.talend.daikon.properties.ValidationResult;
import org.talend.daikon.properties.ValidationResult.Result;

public class AzureStorageSourceOrSink extends AzureStorageRuntime implements SourceOrSink {

    private static final long serialVersionUID = 1589394346101991075L;

    protected transient Schema schema;

    @Override
    public ValidationResult initialize(RuntimeContainer runtimeContainer, ComponentProperties properties) {
        ValidationResult validationResult = super.initialize(runtimeContainer, properties);
        if (runtimeContainer != null) {
            AzureStorageUtils.setApplicationVersion((String) runtimeContainer.getGlobalData(AzureStorageUtils.TALEND_PRODUCT_VERSION_GLOBAL_KEY));
            AzureStorageUtils.setComponentVersion((String) runtimeContainer.getGlobalData(AzureStorageUtils.TALEND_COMPONENT_VERSION_GLOBAL_KEY));
        }
        if (validationResult.getStatus() == ValidationResult.Result.ERROR) {
            return validationResult;
        }

        return ValidationResult.OK;
    }

    @Override
    public ValidationResult validate(RuntimeContainer runtimeContainer) {
        // Nothing to validate here
        return ValidationResult.OK;
    }

    @Override
    public Schema getEndpointSchema(RuntimeContainer container, String schemaName) throws IOException {
        return null;
    }

    public static ValidationResult validateConnection(AzureStorageProvideConnectionProperties properties) {
        AzureStorageSourceOrSink sos = new AzureStorageSourceOrSink();
        ValidationResult vr = sos.initialize(null, (ComponentProperties) properties);
        if (ValidationResult.Result.OK != vr.getStatus()) {
            return vr;
        }

        try {
            sos.getBlobServiceClient(null);
        } catch (Exception e) {
            return new ValidationResult(Result.ERROR, e.getLocalizedMessage());
        }

        return ValidationResult.OK;

    }

    public static List<NamedThing> getSchemaNames(RuntimeContainer container, TAzureStorageConnectionProperties properties)
            throws IOException {
        AzureStorageSourceOrSink sos = new AzureStorageSourceOrSink();
        sos.initialize(container, properties);
        return sos.getSchemaNames(container);
    }

    @Override
    public List<NamedThing> getSchemaNames(RuntimeContainer container) throws IOException {
        List<NamedThing> result = new ArrayList<>();
        try {
            BlobServiceClient client = getAzureConnection(container).getBlobServiceClient();
            for (BlobContainerItem c : client.listBlobContainers()) {
                result.add(new SimpleNamedThing(c.getName(), c.getName()));
            }
        } catch (BlobStorageException e) {
            throw new ComponentException(e);
        }
        return result;
    }

}
