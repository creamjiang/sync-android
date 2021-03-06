/**
 * Copyright (c) 2015 Cloudant, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.cloudant.sync.replication;

import com.cloudant.common.RequireRunningCouchDB;
import com.cloudant.mazha.Response;
import com.cloudant.sync.datastore.BasicDocumentRevision;
import com.cloudant.sync.datastore.ConflictResolver;
import com.cloudant.sync.datastore.DatastoreExtended;
import com.cloudant.sync.datastore.DocumentBodyFactory;
import com.cloudant.sync.datastore.DocumentRevision;
import com.cloudant.sync.datastore.MutableDocumentRevision;
import com.cloudant.sync.datastore.UnsavedStreamAttachment;
import com.cloudant.sync.util.TestUtils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tomblench on 14/11/2014.
 */

@Category(RequireRunningCouchDB.class)
public class AttachmentsConflictsTest extends ReplicationTestBase {

    // test that we can correctly pull attachments for a conflicted remote db
    @Test
    public void testConflictedAttachment() throws Exception {
        // create remote
        Map<String, Object> foo1 = new HashMap<String, Object>();
        foo1.put("_id", "doc-a");
        Map<String, Object> atts = new HashMap<String, Object>();
        Map<String, String> att = new HashMap<String, String>();
        foo1.put("_attachments", atts);
        atts.put("att1", att);
        att.put("content_type", "text/plain");
        // the string "hello" base64 encoded
        att.put("data", "aGVsbG8=");
        foo1.put("foo", "(from remoteDb)");
        Response response = remoteDb.create(foo1);

        // create local (1-rev and 2-rev)
        MutableDocumentRevision rev = new MutableDocumentRevision();
        rev.docId = "doc-a";
        rev.body = DocumentBodyFactory.create("{\"foo\": \"local\"}".getBytes());
        MutableDocumentRevision rev2 = this.datastore.createDocumentFromRevision(rev).mutableCopy();
        rev2.attachments.put("att1", new UnsavedStreamAttachment(
                        new ByteArrayInputStream("hello universe".getBytes()),
                        "att1",
                        "text/plain")
        );
        this.datastore.updateDocumentFromRevision(rev2);
        this.push();

        this.datastoreManager.deleteDatastore(this.datastore.getDatastoreName());
        this.datastore = (DatastoreExtended)this.datastoreManager.openDatastore("foo-bar-baz");
        this.pull();

        DocumentRevision gotRev = this.datastore.getDocument("doc-a");

        Assert.assertEquals(gotRev.getAttachments().size(), 1);
        // local one is guaranteed to be winner because its revision tree is longer
        Assert.assertEquals(gotRev.getBody().asMap().get("foo"), "local");
        Assert.assertFalse(TestUtils.streamsEqual(gotRev.getAttachments().get("att1").getInputStream(),
                new ByteArrayInputStream("hello".getBytes())));
        Assert.assertTrue(TestUtils.streamsEqual(gotRev.getAttachments().get("att1").getInputStream(),
                new ByteArrayInputStream("hello universe".getBytes())));
    }

    // test that we can correctly pull attachments for a resolved remote db
    @Test
    public void testResolvedAttachment() throws Exception {
        // create remote
        Map<String, Object> foo1 = new HashMap<String, Object>();
        foo1.put("_id", "doc-a");
        Map<String, Object> atts = new HashMap<String, Object>();
        Map<String, String> att = new HashMap<String, String>();
        foo1.put("_attachments", atts);
        atts.put("att1", att);
        att.put("content_type", "text/plain");
        // the string "hello" base64 encoded
        att.put("data", "aGVsbG8=");
        foo1.put("foo", "(from remoteDb)");
        Response response = remoteDb.create(foo1);

        // create local
        MutableDocumentRevision rev = new MutableDocumentRevision();
        rev.docId = "doc-a";
        rev.body = DocumentBodyFactory.create("{\"foo\": \"local\"}".getBytes());
        rev.attachments.put("att1", new UnsavedStreamAttachment(
                        new ByteArrayInputStream("hello universe".getBytes()),
                        "att1",
                        "text/plain")
        );
        this.datastore.createDocumentFromRevision(rev);
        this.pull();
        this.datastore.resolveConflictsForDocument("doc-a", new ConflictResolver() {
            @Override
            public DocumentRevision resolve(String docId, List<BasicDocumentRevision> conflicts) {
                return conflicts.get(0);
            }
        });
        this.push();
        this.pull();

        this.datastoreManager.deleteDatastore(this.datastore.getDatastoreName());
        this.datastore = (DatastoreExtended)this.datastoreManager.openDatastore("foo-bar-baz");
        this.pull();

        DocumentRevision gotRev = this.datastore.getDocument("doc-a");
        Assert.assertEquals(gotRev.getAttachments().size(), 1);
    }

    private void push() throws Exception {
        TestStrategyListener listener = new TestStrategyListener();
        BasicPushStrategy push = new BasicPushStrategy(this.createPushReplication());
        push.eventBus.register(listener);

        Thread t = new Thread(push);
        t.start();
        t.join();
        Assert.assertTrue(listener.finishCalled);
        Assert.assertFalse(listener.errorCalled);
    }

    private void pull() throws Exception {
        TestStrategyListener listener = new TestStrategyListener();
        BasicPullStrategy pull = new BasicPullStrategy(this.createPullReplication());
        pull.getEventBus().register(listener);

        Thread t = new Thread(pull);
        t.start();
        t.join();
        Assert.assertTrue(listener.finishCalled);
        Assert.assertFalse(listener.errorCalled);
    }

}
