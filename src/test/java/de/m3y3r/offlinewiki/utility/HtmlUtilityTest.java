package de.m3y3r.offlinewiki.utility;

import junit.framework.Assert;

import org.junit.Test;

import de.m3y3r.offlinewiki.utility.HtmlUtility;

public class HtmlUtilityTest {

	@Test
	public void decodeEntityTest() {
		String result = HtmlUtility.decodeEntities("&amp;");
		Assert.assertEquals("&",result);
	}
}
