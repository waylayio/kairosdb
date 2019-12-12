package org.kairosdb.testing;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class AssumingCassandraRule implements TestRule
{
	@Override
	public Statement apply(Statement base, Description description)
	{
		return new Statement()
		{
			@Override
			public void evaluate() throws Throwable
			{
				Assume.assumeNotNull(System.getenv("CASSANDRA_HOST"));
				base.evaluate();
			}
		};

	}
}
