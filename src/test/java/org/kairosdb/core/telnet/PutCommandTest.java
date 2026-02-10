/*
 * Copyright 2016 KairosDB Authors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.kairosdb.core.telnet;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.junit.Before;
import org.junit.Test;
import org.kairosdb.core.DataPoint;
import org.kairosdb.core.DataPointSet;
import org.kairosdb.core.KairosRootConfig;
import org.kairosdb.core.datapoints.DoubleDataPointFactoryImpl;
import org.kairosdb.core.datapoints.LongDataPointFactoryImpl;
import org.kairosdb.core.datastore.Datastore;
import org.kairosdb.core.datastore.DatastoreMetricQuery;
import org.kairosdb.core.datastore.QueryCallback;
import org.kairosdb.core.datastore.TagSet;
import org.kairosdb.core.exception.DatastoreException;
import org.kairosdb.eventbus.EventBusConfiguration;
import org.kairosdb.eventbus.FilterEventBus;
import org.kairosdb.eventbus.Subscribe;
import org.kairosdb.events.DataPointEvent;
import org.kairosdb.util.ValidationException;

import jakarta.annotation.Nullable;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class PutCommandTest
{
	private PutCommand m_command;
	private FakeDatastore m_datastore;

	@Before
	public void setup() throws DatastoreException
	{
		FilterEventBus eventBus = new FilterEventBus(new EventBusConfiguration(new KairosRootConfig()));
		m_datastore = new FakeDatastore();
		eventBus.register(m_datastore);

		m_command = new PutCommand(eventBus, "test", new LongDataPointFactoryImpl(),
				new DoubleDataPointFactoryImpl());
	}

	@Test
	public void test() throws DatastoreException, ValidationException
	{
		m_command.execute(new FakeChannel(), Arrays.asList("telnet", "MetricName", "12345678999", "789", "foo=bar", "fum=barfum"));

		assertThat(m_datastore.getSet().getName(), equalTo("MetricName"));
		assertThat(m_datastore.getSet().getTags().size(), equalTo(2));
		assertThat(m_datastore.getSet().getTags().get("foo"), equalTo("bar"));
		assertThat(m_datastore.getSet().getTags().get("fum"), equalTo("barfum"));
		assertThat(m_datastore.getSet().getDataPoints().get(0).getTimestamp(), equalTo(12345678999L));
		assertThat(m_datastore.getSet().getDataPoints().get(0).getLongValue(), equalTo(789L));
	}

	@Test
	public void test_metricName_empty_invalid() throws DatastoreException, ValidationException
	{
		try
		{
			m_command.execute(new FakeChannel(), Arrays.asList("telnet", "", "12345678999", "789", "foo=bar", "fum=barfum"));
			fail("ValidationException expected");
		}
		catch (DatastoreException e)
		{
			fail("ValidationException expected");
		}
		catch (ValidationException e)
		{
			assertThat(e.getMessage(), equalTo("metricName must not be empty."));
		}
	}

	@Test
	public void test_metricName_characters_valid() throws DatastoreException, ValidationException
	{
		m_command.execute(new FakeChannel(), Arrays.asList("telnet", "你好", "12345678999", "789", "foo=bar", "fum=barfum"));
	}

	@Test
	public void test_tagName_empty_invalid() throws DatastoreException, ValidationException
	{
		try
		{
			m_command.execute(new FakeChannel(), Arrays.asList("telnet", "metricName", "12345678999", "789", "foo=bar", "=barfum"));
			fail("ValidationException expected");
		}
		catch (DatastoreException e)
		{
			fail("ValidationException expected");
		}
		catch (ValidationException e)
		{
			assertThat(e.getMessage(), equalTo("tag[1].name must not be empty."));
		}
	}

	@Test
	public void test_tagName_characters_validColonTagName() throws DatastoreException, ValidationException
	{
		m_command.execute(new FakeChannel(), Arrays.asList("telnet", "metricName", "12345678999", "789", "foo=bar", "fum:fi=barfum"));
	}

	@Test
	public void test_tagValue_empty_invalid() throws DatastoreException, ValidationException
	{
		try
		{
			m_command.execute(new FakeChannel(), Arrays.asList("telnet", "metricName", "12345678999", "789", "foo=bar", "fum="));
			fail("ValidationException expected");
		}
		catch (DatastoreException e)
		{
			fail("ValidationException expected");
		}
		catch (ValidationException e)
		{
			assertThat(e.getMessage(), equalTo("tag[1] must be in the format 'name=value'."));
		}
	}

	@Test
	public void test_tagValue_characters_validColonTagValue() throws DatastoreException, ValidationException
	{
		m_command.execute(new FakeChannel(), Arrays.asList("telnet", "metricName", "12345678999", "789", "foo=bar", "fum=bar:fum"));
	}

	@Test
	public void test_tag_invalid() throws DatastoreException, ValidationException
	{
		try
		{
			m_command.execute(new FakeChannel(), Arrays.asList("telnet", "metricName", "12345678999", "789", "foo=bar", "fum-barfum"));
			fail("ValidationException expected");
		}
		catch (DatastoreException e)
		{
			fail("ValidationException expected");
		}
		catch (ValidationException e)
		{
			assertThat(e.getMessage(), equalTo("tag[1] must be in the format 'name=value'."));
		}
	}

	public static class FakeChannel implements Channel
	{
		@Override
		public ChannelId id()
		{
			return null;
		}

		@Override
		public EventLoop eventLoop()
		{
			return null;
		}

		@Override
		public Channel parent()
		{
			return null;
		}

		@Override
		public ChannelConfig config()
		{
			return null;
		}

		@Override
		public boolean isOpen()
		{
			return false;
		}

		@Override
		public boolean isRegistered()
		{
			return false;
		}

		@Override
		public boolean isActive()
		{
			return false;
		}

		@Override
		public ChannelMetadata metadata()
		{
			return null;
		}

		@Override
		public SocketAddress localAddress()
		{
			return null;
		}

		@Override
		public SocketAddress remoteAddress()
		{
			return null;
		}

		@Override
		public ChannelFuture closeFuture()
		{
			return null;
		}

		@Override
		public boolean isWritable()
		{
			return false;
		}

		@Override
		public long bytesBeforeUnwritable()
		{
			return 0;
		}

		@Override
		public long bytesBeforeWritable()
		{
			return 0;
		}

		@Override
		public Unsafe unsafe()
		{
			return null;
		}

		@Override
		public ChannelPipeline pipeline()
		{
			return null;
		}

		@Override
		public ByteBufAllocator alloc()
		{
			return null;
		}

		@Override
		public Channel read()
		{
			return this;
		}

		@Override
		public Channel flush()
		{
			return this;
		}

		@Override
		public ChannelFuture bind(SocketAddress localAddress)
		{
			return null;
		}

		@Override
		public ChannelFuture connect(SocketAddress remoteAddress)
		{
			return null;
		}

		@Override
		public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress)
		{
			return null;
		}

		@Override
		public ChannelFuture disconnect()
		{
			return null;
		}

		@Override
		public ChannelFuture close()
		{
			return null;
		}

		@Override
		public ChannelFuture deregister()
		{
			return null;
		}

		@Override
		public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise)
		{
			return null;
		}

		@Override
		public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise)
		{
			return null;
		}

		@Override
		public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
		{
			return null;
		}

		@Override
		public ChannelFuture disconnect(ChannelPromise promise)
		{
			return null;
		}

		@Override
		public ChannelFuture close(ChannelPromise promise)
		{
			return null;
		}

		@Override
		public ChannelFuture deregister(ChannelPromise promise)
		{
			return null;
		}

		@Override
		public ChannelFuture write(Object msg)
		{
			return null;
		}

		@Override
		public ChannelFuture write(Object msg, ChannelPromise promise)
		{
			return null;
		}

		@Override
		public ChannelFuture writeAndFlush(Object msg)
		{
			return null;
		}

		@Override
		public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise)
		{
			return null;
		}

		@Override
		public ChannelPromise newPromise()
		{
			return null;
		}

		@Override
		public ChannelProgressivePromise newProgressivePromise()
		{
			return null;
		}

		@Override
		public ChannelFuture newSucceededFuture()
		{
			return null;
		}

		@Override
		public ChannelFuture newFailedFuture(Throwable cause)
		{
			return null;
		}

		@Override
		public ChannelPromise voidPromise()
		{
			return null;
		}

		@Override
		public <T> Attribute<T> attr(AttributeKey<T> key)
		{
			return null;
		}

		@Override
		public <T> boolean hasAttr(AttributeKey<T> key)
		{
			return false;
		}

		@Override
		public int compareTo(@Nullable Channel o)
		{
			return 0;
		}
	}

	private static class FakeDatastore implements Datastore
	{
		private DataPointSet set;

		private DataPointSet getSet()
		{
			return set;
		}

		@Override
		public void close() throws InterruptedException, DatastoreException
		{
		}

		@Subscribe
		public void putDataPoint(DataPointEvent event) throws DatastoreException
		{
			if (set == null)
				set = new DataPointSet(event.getMetricName(), event.getTags(), Collections.<DataPoint>emptyList());

			set.addDataPoint(event.getDataPoint());
		}

		/*@Override
		public void putDataPoints(DataPointSet dps) throws DatastoreException
		{
			this.set = dps;
		}*/

		@Override
		public Iterable<String> getMetricNames(String prefix) throws DatastoreException
		{
			return null;
		}

		@Override
		public Iterable<String> getTagNames() throws DatastoreException
		{
			return null;
		}

		@Override
		public Iterable<String> getTagValues() throws DatastoreException
		{
			return null;
		}

		@Override
		public void queryDatabase(DatastoreMetricQuery query, QueryCallback queryCallback) throws DatastoreException
		{
		}

		@Override
		public void deleteDataPoints(DatastoreMetricQuery deleteQuery) throws DatastoreException
		{
		}

		@Override
		public TagSet queryMetricTags(DatastoreMetricQuery query) throws DatastoreException
		{
			return null;
		}

		@Override
		public void indexMetricTags(DatastoreMetricQuery query) throws DatastoreException
		{
		}

		@Override
		public long getMinTimeValue()
		{
			return Long.MIN_VALUE;
		}

		@Override
		public long getMaxTimeValue()
		{
			return Long.MAX_VALUE;
		}
	}
}