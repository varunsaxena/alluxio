/*
 * Licensed to the University of California, Berkeley under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package tachyon.client.keyvalue;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import tachyon.Constants;
import tachyon.LocalTachyonClusterResource;
import tachyon.TachyonURI;
import tachyon.util.io.BufferUtils;
import tachyon.util.io.PathUtils;

/**
 * Integration tests for {@link KeyValueStores}.
 */
public final class KeyValueStoreIntegrationTest {
  private static final int BLOCK_SIZE = 512 * Constants.MB;
  private static final byte[] KEY1 = "key1".getBytes();
  private static final byte[] KEY2 = "key2_foo".getBytes();
  private static final byte[] VALUE1 = "value1".getBytes();
  private static final byte[] VALUE2 = "value2_bar".getBytes();
  private static KeyValueStores sKVStores;

  private KeyValueStoreWriter mWriter;
  private KeyValueStoreReader mReader;
  private TachyonURI mStoreUri;

  @ClassRule
  public static LocalTachyonClusterResource sLocalTachyonClusterResource =
      new LocalTachyonClusterResource(Constants.GB, Constants.KB, BLOCK_SIZE,
          /* ensure key-value service is turned on */
          Constants.KEY_VALUE_ENABLED, "true");

  @BeforeClass
  public static void beforeClass() throws Exception {
    sKVStores = KeyValueStores.Factory.create();
  }

  @Before
  public void before() throws Exception {
    mStoreUri = new TachyonURI(PathUtils.uniqPath());
  }

  /**
   * Tests creating and opening an empty store.
   *
   * @throws Exception if unexpected error happens
   */
  @Test
  public void createAndOpenEmptyStoreTest() throws Exception {
    mWriter = sKVStores.create(mStoreUri);
    Assert.assertNotNull(mWriter);
    mWriter.close();

    mReader = sKVStores.open(mStoreUri);
    Assert.assertNotNull(mReader);
    mReader.close();
  }

  /**
   * Tests creating and opening a store with one key.
   *
   * @throws Exception if unexpected error happens
   */
  @Test
  public void createAndOpenStoreWithOneKeyTest() throws Exception {
    mWriter = sKVStores.create(mStoreUri);
    mWriter.put(KEY1, VALUE1);
    mWriter.close();

    mReader = sKVStores.open(mStoreUri);
    Assert.assertArrayEquals(VALUE1, mReader.get(KEY1));
    Assert.assertNull(mReader.get(KEY2));
    mReader.close();
  }

  /**
   * Tests creating and opening a store with a number of key.
   *
   * @throws Exception if unexpected error happens
   */
  @Test
  public void createAndOpenStoreWithMultiKeysTest() throws Exception {
    final int numKeys = 100;
    final int keyLength = 4; // 64 Byte key
    final int valueLength = 5 * Constants.KB; // 5KB value
    mWriter = sKVStores.create(mStoreUri);
    for (int i = 0; i < numKeys; i ++) {
      byte[] key = BufferUtils.getIncreasingByteArray(i, keyLength);
      byte[] value = BufferUtils.getIncreasingByteArray(i, valueLength);
      mWriter.put(key, value);
    }
    mWriter.close();

    mReader = sKVStores.open(mStoreUri);
    for (int i = 0; i < numKeys; i ++) {
      byte[] key = BufferUtils.getIncreasingByteArray(i, keyLength);
      byte[] value = mReader.get(key);
      Assert.assertTrue(BufferUtils.equalIncreasingByteArray(i, valueLength, value));
    }
    Assert.assertNull(mReader.get(KEY1));
    Assert.assertNull(mReader.get(KEY2));
    mReader.close();
  }

  /**
   * Tests that an iterator for an empty store has no next elements.
   *
   * @throws Exception when any exception happens
   */
  @Test
  public void emptyStoreIteratorTest() throws Exception {
    mWriter = sKVStores.create(mStoreUri);
    mWriter.close();

    mReader = sKVStores.open(mStoreUri);
    KeyValueIterator iterator = mReader.iterator();
    Assert.assertFalse(iterator.hasNext());
  }

  /**
   * Tests that an iterator can correctly iterate over a store.
   *
   * @throws Exception when any exception happens
   */
  @Test
  public void iteratorTest() throws Exception {
    mWriter = sKVStores.create(mStoreUri);
    mWriter.put(KEY1, VALUE1);
    mWriter.put(KEY2, VALUE2);
    mWriter.close();

    mReader = sKVStores.open(mStoreUri);
    KeyValueIterator iterator = mReader.iterator();

    Assert.assertTrue(iterator.hasNext());
    KeyValuePair pair1 = iterator.next();
    Assert.assertTrue(iterator.hasNext());
    KeyValuePair pair2 = iterator.next();
    Assert.assertFalse(iterator.hasNext());

    if (pair1.getKey().equals(ByteBuffer.wrap(KEY2))) {
      KeyValuePair tmp = pair1;
      pair1 = pair2;
      pair2 = tmp;
    }
    Assert.assertArrayEquals(KEY1, BufferUtils.newByteArrayFromByteBuffer(pair1.getKey()));
    Assert.assertArrayEquals(VALUE1, BufferUtils.newByteArrayFromByteBuffer(pair1.getValue()));
    Assert.assertArrayEquals(KEY2, BufferUtils.newByteArrayFromByteBuffer(pair2.getKey()));
    Assert.assertArrayEquals(VALUE2, BufferUtils.newByteArrayFromByteBuffer(pair2.getValue()));
  }
}
