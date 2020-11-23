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
package org.talend.components.azurestorage.queue.runtime;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.models.QueueMessageItem;
import com.azure.storage.queue.models.QueueStorageException;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.IndexedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.components.api.component.runtime.Result;
import org.talend.components.api.component.runtime.WriteOperation;
import org.talend.components.api.component.runtime.WriterWithFeedback;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.api.exception.ComponentException;
import org.talend.components.azurestorage.queue.AzureStorageQueueProperties;
import org.talend.components.azurestorage.queue.tazurestoragequeueoutput.TAzureStorageQueueOutputProperties;
import org.talend.components.common.runtime.GenericIndexedRecordConverter;
import org.talend.daikon.avro.AvroUtils;
import org.talend.daikon.i18n.GlobalI18N;
import org.talend.daikon.i18n.I18nMessages;

public class AzureStorageQueueWriter implements WriterWithFeedback<Result, IndexedRecord, IndexedRecord> {

    private AzureStorageQueueWriteOperation wope;

    private AzureStorageQueueSink sink;

    private TAzureStorageQueueOutputProperties props;

    private RuntimeContainer runtime;

    private QueueClient queue;

    private Schema writeSchema;

    private Result result;

    private List<QueueMsg> messagesBuffer;

    private List<IndexedRecord> successfulWrites = new ArrayList<>();

    private static final int MAX_MSG_TO_ENQUEUE = 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStorageQueueWriter.class);

    private static final I18nMessages i18nMessages = GlobalI18N.getI18nMessageProvider()
            .getI18nMessages(AzureStorageQueueWriter.class);

    public AzureStorageQueueWriter(RuntimeContainer runtime, AzureStorageQueueWriteOperation ope) {
        super();
        this.wope = ope;
        this.runtime = runtime;
        this.sink = (AzureStorageQueueSink) getWriteOperation().getSink();
        this.props = (TAzureStorageQueueOutputProperties) this.sink.properties;

        this.messagesBuffer = new ArrayList<>();
    }

    @Override
    public void open(String uId) throws IOException {
        this.result = new Result(uId);
        if (writeSchema == null) {
            writeSchema = props.schema.schema.getValue();
            if (AvroUtils.isIncludeAllFields(writeSchema)) {
                // if design schema include dynamic,need to get schema from record
                writeSchema = null;
            }
        }
        try {
            queue = sink.getQueueItem(runtime, props.queueName.getValue());
            System.out.println("[open]");
        } catch (QueueStorageException e) {
            LOGGER.error(e.getLocalizedMessage());
            if (props.dieOnError.getValue()) {
                throw new ComponentException(e);
            }
        }
    }

    @Override
    public void write(Object object) throws IOException {
        String content;
        if (object == null) {
            return;
        }
        cleanWrites();
        result.totalCount++;
        if (writeSchema == null) {
            writeSchema = ((IndexedRecord) object).getSchema();
        }
        GenericIndexedRecordConverter factory = new GenericIndexedRecordConverter();
        factory.setSchema(writeSchema);
        IndexedRecord inputRecord = factory.convertToAvro((IndexedRecord) object);
        Field msgContent = writeSchema.getField(AzureStorageQueueProperties.FIELD_MESSAGE_CONTENT);
        int ttl = props.timeToLiveInSeconds.getValue();
        int visibility = props.initialVisibilityDelayInSeconds.getValue();

        if (msgContent == null) {
            LOGGER.error(i18nMessages.getMessage("error.VacantMessage"));
            if (props.dieOnError.getValue()) {
                throw new ComponentException(new Exception(i18nMessages.getMessage("error.VacantMessage")));
            }
        } else {
            content = (String) inputRecord.get(msgContent.pos());
            QueueMessageItem msg = new QueueMessageItem();
            msg.setMessageText(content);
            messagesBuffer.add(new QueueMsg(msg, ttl, visibility));
        }

        if (messagesBuffer.size() >= MAX_MSG_TO_ENQUEUE) {
            sendParallelMessages();
        }
    }

    @Override
    public Result close() throws IOException {
        sendParallelMessages();
        queue = null;
        return result;
    }

    private void sendParallelMessages() {
        messagesBuffer.parallelStream().forEach(new Consumer<QueueMsg>() {

            @Override
            public void accept(QueueMsg msg) {
                try {
                    queue.sendMessageWithResponse(msg.getMsg().getMessageText(),
                                                  Duration.ofSeconds(msg.getInitialVisibilityDelayInSeconds()),
                                                  Duration.ofSeconds(msg.getTimeToLiveInSeconds()),
                                                  Duration.ofSeconds(30),
                                                  null);
                    synchronized (this) {
                        result.successCount++;
                        IndexedRecord record = new Record(writeSchema);
                        record.put(0, msg.getMsg().getMessageText());
                        successfulWrites.add(record);
                    }
                } catch (QueueStorageException e) {
                    result.rejectCount++;
                    LOGGER.error(e.getLocalizedMessage());
                }
            }
        });

        messagesBuffer.clear();
    }

    @Override
    public WriteOperation<Result> getWriteOperation() {
        return wope;
    }

    @Override
    public Iterable<IndexedRecord> getSuccessfulWrites() {
        return Collections.unmodifiableList(successfulWrites);
    }

    @Override
    public Iterable<IndexedRecord> getRejectedWrites() {
        return Collections.emptyList();
    }

    @Override
    public void cleanWrites() {
        successfulWrites.clear();
    }
}
